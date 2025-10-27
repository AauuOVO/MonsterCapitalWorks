package org.mcw.monstercapitalworks.manager

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import org.mcw.monstercapitalworks.model.Spawner
import org.mcw.monstercapitalworks.model.SpawnerType
import org.mcw.monstercapitalworks.model.UpgradePath

/**
 * 升级管理器 - 管理刷怪笼升级系统
 */
class UpgradeManager(private val plugin: MonsterCapitalWorks) {
    
    private val normalUpgrades = mutableMapOf<String, UpgradePath>()
    private val premiumUpgrades = mutableMapOf<String, UpgradePath>()
    
    /**
     * 初始化管理器
     */
    fun initialize() {
        loadUpgrades()
        plugin.logger.info("升级管理器已初始化")
    }
    
    /**
     * 加载所有升级配置
     */
    private fun loadUpgrades() {
        val configManager = plugin.configManager
        
        // 加载普通升级
        val normalConfig = configManager.getNormalUpgrades()
        if (normalConfig != null) {
            loadUpgradesFromConfig(normalConfig, normalUpgrades)
        }
        
        // 加载付费升级
        val premiumConfig = configManager.getPremiumUpgrades()
        if (premiumConfig != null) {
            loadUpgradesFromConfig(premiumConfig, premiumUpgrades)
        }
        
        plugin.logger.info("已加载 ${normalUpgrades.size} 个普通升级路径")
        plugin.logger.info("已加载 ${premiumUpgrades.size} 个付费升级路径")
    }
    
    /**
     * 从配置文件加载升级
     */
    private fun loadUpgradesFromConfig(config: FileConfiguration, upgradeMap: MutableMap<String, UpgradePath>) {
        val upgradesSection = config.getConfigurationSection("upgrades") ?: return
        
        for (key in upgradesSection.getKeys(false)) {
            val upgradeSection = upgradesSection.getConfigurationSection(key) ?: continue
            
            val path = UpgradePath(key)
            
            // 加载升级等级
            val levelsSection = upgradeSection.getConfigurationSection("levels")
            if (levelsSection != null) {
                for (levelKey in levelsSection.getKeys(false)) {
                    try {
                        val level = levelKey.toInt()
                        val levelSection = levelsSection.getConfigurationSection(levelKey)
                        
                        if (levelSection != null) {
                            val value = levelSection.getDouble("value", 0.0)
                            val cost = levelSection.getDouble("cost", 0.0)
                            
                            path.addLevel(level, value, cost)
                        }
                    } catch (e: NumberFormatException) {
                        plugin.logger.warning("无效的升级等级: $levelKey")
                    }
                }
            }
            
            upgradeMap[key] = path
        }
    }
    
    /**
     * 获取升级路径
     */
    fun getUpgradePath(type: SpawnerType, upgradeName: String): UpgradePath? {
        val upgrades = if (type == SpawnerType.NORMAL) normalUpgrades else premiumUpgrades
        return upgrades[upgradeName]
    }
    
    /**
     * 升级刷怪笼
     */
    fun upgradeSpawner(player: Player, spawner: Spawner, upgradeName: String): Boolean {
        val path = getUpgradePath(spawner.type, upgradeName)
        if (path == null) {
            player.sendMessage("§c升级失败：未找到升级路径！")
            return false
        }
        
        // 获取当前等级
        val currentLevel = spawner.getUpgradeLevel(upgradeName)
        val nextLevel = currentLevel + 1
        val maxLevel = path.maxLevel
        
        // 检查是否已达到最大等级
        if (currentLevel >= maxLevel || !path.hasLevel(nextLevel)) {
            player.sendMessage("§c该升级已达到最大等级！无法继续升级。")
            return false
        }
        
        // 检查前置条件
        if (!checkRequiredUpgrades(player, spawner, upgradeName, nextLevel)) {
            player.sendMessage("§c升级失败：不满足前置条件！")
            val requiredInfo = getRequiredUpgradesInfo(spawner, upgradeName)
            if (requiredInfo.isNotEmpty()) {
                player.sendMessage("§7需要：")
                player.sendMessage(requiredInfo)
            }
            return false
        }
        
        // 获取升级费用
        val cost = path.getCost(nextLevel)
        
        // 检查经济
        val economy = plugin.economyManager
        if (economy != null && economy.isEnabled()) {
            if (!economy.has(player, cost)) {
                player.sendMessage(plugin.configManager.getMessage("error.insufficient_funds"))
                return false
            }
            
            // 扣除费用
            economy.withdraw(player, cost)
        }
        
        // 执行升级
        spawner.setUpgradeLevel(upgradeName, nextLevel)
        applyUpgrade(spawner, upgradeName, nextLevel, path)
        
        // 保存刷怪笼
        plugin.spawnerManager.saveSpawner(spawner)
        
        player.sendMessage(plugin.configManager.getMessage("success.upgraded"))
        
        return true
    }
    
    /**
     * 应用升级效果
     */
    private fun applyUpgrade(spawner: Spawner, upgradeName: String, level: Int, path: UpgradePath) {
        val value = path.getValue(level)
        
        when (upgradeName) {
            "speed" -> {
                // 速度升级 - 减少生成延迟
                spawner.spawnDelay = value.toInt()
            }
            "count" -> {
                // 数量升级 - 增加生成数量
                spawner.spawnCount = value.toInt()
            }
            "max_nearby" -> {
                // 最大附近实体升级
                spawner.maxNearbyEntities = value.toInt()
            }
            "range" -> {
                // 激活范围升级
                spawner.activationRange = value.toInt()
            }
            "storage" -> {
                // 存储升级
                spawner.maxStorage = value.toInt()
            }
            else -> {
                plugin.logger.warning("未知的升级类型: $upgradeName")
            }
        }
    }
    
    /**
     * 应用所有升级效果（用于恢复刷怪笼时）
     */
    fun applyUpgrades(spawner: Spawner) {
        val upgrades = spawner.getUpgradeLevels()
        
        for ((upgradeName, level) in upgrades) {
            if (level > 0) {
                val path = getUpgradePath(spawner.type, upgradeName)
                if (path != null && path.hasLevel(level)) {
                    applyUpgrade(spawner, upgradeName, level, path)
                }
            }
        }
    }
    
    /**
     * 获取升级费用
     */
    fun getUpgradeCost(spawner: Spawner, upgradeName: String): Double {
        val path = getUpgradePath(spawner.type, upgradeName) ?: return 0.0
        
        val currentLevel = spawner.getUpgradeLevel(upgradeName)
        val nextLevel = currentLevel + 1
        
        return path.getCost(nextLevel)
    }
    
    /**
     * 获取升级后的值
     */
    fun getUpgradeValue(spawner: Spawner, upgradeName: String, level: Int): Double {
        val path = getUpgradePath(spawner.type, upgradeName) ?: return 0.0
        
        return path.getValue(level)
    }
    
    /**
     * 获取当前升级值
     */
    fun getCurrentUpgradeValue(spawner: Spawner, upgradeName: String): Double {
        val level = spawner.getUpgradeLevel(upgradeName)
        return getUpgradeValue(spawner, upgradeName, level)
    }
    
    /**
     * 获取下一级升级值
     */
    fun getNextUpgradeValue(spawner: Spawner, upgradeName: String): Double {
        val nextLevel = spawner.getUpgradeLevel(upgradeName) + 1
        return getUpgradeValue(spawner, upgradeName, nextLevel)
    }
    
    /**
     * 检查是否可以升级
     */
    fun canUpgrade(spawner: Spawner, upgradeName: String): Boolean {
        val path = getUpgradePath(spawner.type, upgradeName) ?: return false
        
        val currentLevel = spawner.getUpgradeLevel(upgradeName)
        val nextLevel = currentLevel + 1
        
        return path.hasLevel(nextLevel)
    }
    
    /**
     * 获取最大等级
     */
    fun getMaxLevel(type: SpawnerType, upgradeName: String): Int {
        val path = getUpgradePath(type, upgradeName) ?: return 0
        
        return path.maxLevel
    }
    
    /**
     * 检查前置升级条件（静默检查，不发送消息）
     */
    private fun checkRequiredUpgrades(player: Player, spawner: Spawner, upgradeName: String, nextLevel: Int): Boolean {
        // 获取配置文件
        val config = if (spawner.type == SpawnerType.NORMAL) {
            plugin.configManager.getNormalUpgrades()
        } else {
            plugin.configManager.getPremiumUpgrades()
        } ?: return true
        
        // 获取该等级的前置条件
        val path = "upgrades.$upgradeName.levels.$nextLevel"
        val levelSection = config.getConfigurationSection(path) ?: return true
        
        val requiredSection = levelSection.getConfigurationSection("required_upgrades") ?: return true
        
        // 检查每个前置条件
        for (requiredUpgrade in requiredSection.getKeys(false)) {
            val requiredLevel = requiredSection.getInt(requiredUpgrade)
            val currentLevel = spawner.getUpgradeLevel(requiredUpgrade)
            
            if (currentLevel < requiredLevel) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * 获取升级的前置需求信息（用于GUI显示）
     */
    fun getRequiredUpgradesInfo(spawner: Spawner, upgradeName: String): String {
        val nextLevel = spawner.getUpgradeLevel(upgradeName) + 1
        
        val config = if (spawner.type == SpawnerType.NORMAL) {
            plugin.configManager.getNormalUpgrades()
        } else {
            plugin.configManager.getPremiumUpgrades()
        } ?: return ""
        
        val path = "upgrades.$upgradeName.levels.$nextLevel"
        val levelSection = config.getConfigurationSection(path) ?: return ""
        
        val requiredSection = levelSection.getConfigurationSection("required_upgrades") ?: return ""
        
        val info = StringBuilder()
        for (requiredUpgrade in requiredSection.getKeys(false)) {
            val requiredLevel = requiredSection.getInt(requiredUpgrade)
            val currentLevel = spawner.getUpgradeLevel(requiredUpgrade)
            
            val upgradePath = "upgrades.$requiredUpgrade.name"
            var upgradeDisplayName = config.getString(upgradePath) ?: requiredUpgrade
            upgradeDisplayName = upgradeDisplayName.replace('&', '§')
            
            if (currentLevel < requiredLevel) {
                info.append("§c✘ ").append(upgradeDisplayName).append(" §e").append(requiredLevel)
                    .append("§c级 §7(当前: §e").append(currentLevel).append("§7)\n")
            } else {
                info.append("§a✔ ").append(upgradeDisplayName).append(" §e").append(requiredLevel).append("§a级\n")
            }
        }
        
        return info.toString().trim()
    }
    
    /**
     * 重载升级配置
     */
    fun reload() {
        normalUpgrades.clear()
        premiumUpgrades.clear()
        loadUpgrades()
    }
}
