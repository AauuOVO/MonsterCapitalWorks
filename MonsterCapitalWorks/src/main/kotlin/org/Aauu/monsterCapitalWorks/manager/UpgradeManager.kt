package org.Aauu.monsterCapitalWorks.manager

import org.Aauu.monsterCapitalWorks.model.Spawner
import org.Aauu.monsterCapitalWorks.model.SpawnerType
import org.Aauu.monsterCapitalWorks.model.UpgradePath
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class UpgradeManager(
    private val plugin: JavaPlugin,
    private val configManager: ConfigManager
) {
    private val normalUpgrades = mutableMapOf<String, UpgradePath>()
    private val premiumUpgrades = mutableMapOf<String, UpgradePath>()

    fun initialize() {
        loadUpgrades()
    }

    private fun loadUpgrades() {
        loadUpgradesFromFile("upgrades/normal_upgrades.yml", normalUpgrades)
        loadUpgradesFromFile("upgrades/premium_upgrades.yml", premiumUpgrades)
    }

    private fun loadUpgradesFromFile(fileName: String, upgradeMap: MutableMap<String, UpgradePath>) {
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) {
            plugin.saveResource(fileName, false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val upgradesSection = config.getConfigurationSection("upgrades") ?: return

        for (key in upgradesSection.getKeys(false)) {
            val section = upgradesSection.getConfigurationSection(key) ?: continue
            
            // 检查是否为新的配置格式（包含levels）
            if (section.contains("levels")) {
                val displayName = section.getString("name", key) ?: key
                val levelsSection = section.getConfigurationSection("levels") ?: continue
                val maxLevel = levelsSection.getKeys(false).size
                val costs = mutableListOf<Double>()
                val values = mutableListOf<Any>()
                val requiredUpgrades = mutableMapOf<Int, Map<String, Int>>()
                
                // 按级别排序
                val sortedLevels = levelsSection.getKeys(false).sortedBy { it.toIntOrNull() ?: 0 }
                
                for (levelKey in sortedLevels) {
                    val levelSection = levelsSection.getConfigurationSection(levelKey) ?: continue
                    costs.add(levelSection.getDouble("cost", 0.0))
                    values.add(levelSection.get("value") ?: 0)
                    
                    // 解析前置升级要求
                    if (levelSection.contains("required_upgrades")) {
                        val requiredSection = levelSection.getConfigurationSection("required_upgrades")
                        if (requiredSection != null) {
                            val requiredMap = mutableMapOf<String, Int>()
                            for (requiredKey in requiredSection.getKeys(false)) {
                                requiredMap[requiredKey] = requiredSection.getInt(requiredKey, 0)
                            }
                            requiredUpgrades[levelKey.toIntOrNull() ?: 0] = requiredMap
                        }
                    }
                }
                
                upgradeMap[key] = UpgradePath(key, displayName, maxLevel, costs, values, requiredUpgrades)
            } else {
                // 兼容旧的配置格式
                val displayName = section.getString("display_name", key) ?: key
                val maxLevel = section.getInt("max_level", 10)
                val costs = section.getDoubleList("costs")
                val values = (section.getList("values") ?: emptyList<Any>()) as List<Any>

                upgradeMap[key] = UpgradePath(key, displayName, maxLevel, costs, values)
            }
        }
    }

    fun getUpgrades(type: SpawnerType): Map<String, UpgradePath> {
        return when (type) {
            SpawnerType.NORMAL -> normalUpgrades
            SpawnerType.PREMIUM -> premiumUpgrades
        }
    }

    fun getUpgrade(type: SpawnerType, upgradeType: String): UpgradePath? {
        return getUpgrades(type)[upgradeType]
    }

    fun getUpgradeCost(type: SpawnerType, upgradeType: String, level: Int): Double {
        val upgrade = getUpgrade(type, upgradeType) ?: return 0.0
        return if (level > 0 && level <= upgrade.costs.size) {
            upgrade.costs[level - 1]
        } else {
            0.0
        }
    }

    fun canUpgrade(spawner: Spawner, upgradeType: String): Boolean {
        val upgrade = getUpgrade(spawner.type, upgradeType) ?: return false
        val currentLevel = spawner.upgradeLevels[upgradeType] ?: 0
        return currentLevel < upgrade.maxLevel
    }

    fun getNextLevel(spawner: Spawner, upgradeType: String): Int {
        return (spawner.upgradeLevels[upgradeType] ?: 0) + 1
    }

    fun getUpgradeValue(type: SpawnerType, upgradeType: String, level: Int): Any? {
        val upgrade = getUpgrade(type, upgradeType) ?: return null
        return if (level > 0 && level <= upgrade.values.size) {
            upgrade.values[level - 1]
        } else {
            null
        }
    }

    fun applyUpgrade(spawner: Spawner, upgradeType: String) {
        val currentLevel = spawner.upgradeLevels[upgradeType] ?: 0
        val newLevel = currentLevel + 1

        // 直接调用applyUpgrades来重新计算所有升级值
        // 这样确保所有升级都基于基础值正确计算
        spawner.upgradeLevels[upgradeType] = newLevel
        applyUpgrades(spawner)
    }

    fun applyUpgrades(spawner: Spawner) {
        // 获取基础配置值
        val configManager = this.configManager
        val typeConfig = if (spawner.type == SpawnerType.NORMAL) {
            configManager.getNormalConfig()
        } else {
            configManager.getPremiumConfig()
        }
        
        // 获取基础值
        val baseDelay = typeConfig?.getInt("spawning.default_delay", 200) ?: 200
        val baseCount = typeConfig?.getInt("spawning.default_count", 6) ?: 6
        val baseMaxNearby = typeConfig?.getInt("spawning.default_max_nearby", 5) ?: 5
        val baseRange = typeConfig?.getInt("spawning.default_range", 8) ?: 8
        
        // 存储配置：优先使用storage.default_max，如果没有则使用spawning.default_storage
        val baseStorage = typeConfig?.getInt("storage.default_max", 
            typeConfig?.getInt("spawning.default_storage", 5) ?: 5) ?: 5
        
        // 重置为基础值
        spawner.spawnDelay = baseDelay
        spawner.spawnCount = baseCount
        spawner.maxNearbyEntities = baseMaxNearby
        spawner.activationRange = baseRange
        spawner.maxStorage = baseStorage
        
        // 应用升级
        for ((upgradeType, level) in spawner.upgradeLevels) {
            if (level <= 0) continue
            
            val upgrade = getUpgrade(spawner.type, upgradeType) ?: continue
            
            when (upgradeType) {
                "spawn_delay", "speed" -> {
                    // 速度升级：直接设置升级值而不是累加
                    val value = getUpgradeValue(spawner.type, upgradeType, level)
                    if (value is Number) {
                        spawner.spawnDelay = maxOf(20, value.toInt()) // 直接设置升级值，最小延迟为20tick
                    }
                }
                "spawn_count", "count" -> {
                    // 生成数量升级：基础值 + (升级等级 × 增加量)
                    val increment = getUpgradeValue(spawner.type, upgradeType, 1)
                    if (increment is Number) {
                        spawner.spawnCount = baseCount + (level * increment.toInt()).toInt()
                    }
                }
                "max_nearby_entities", "max_nearby" -> {
                    // 最大附近实体数升级：基础值 + (升级等级 × 增加量)
                    val increment = getUpgradeValue(spawner.type, upgradeType, 1)
                    if (increment is Number) {
                        spawner.maxNearbyEntities = baseMaxNearby + (level * increment.toInt()).toInt()
                    }
                }
                "activation_range", "range" -> {
                    // 激活范围升级：基础值 + (升级等级 × 增加量)
                    val increment = getUpgradeValue(spawner.type, upgradeType, 1)
                    if (increment is Number) {
                        spawner.activationRange = baseRange + (level * increment.toInt()).toInt()
                    }
                }
                "max_storage", "storage" -> {
                    // 最大存储升级：基础值 + (升级等级 × 增加量)
                    val increment = getUpgradeValue(spawner.type, upgradeType, 1)
                    if (increment is Number) {
                        spawner.maxStorage = baseStorage + (level * increment.toInt()).toInt()
                    }
                }
            }
        }
    }

    fun getUpgradeCost(spawner: Spawner, upgradeType: String): Double {
        val upgradePath = getUpgrade(spawner.type, upgradeType) ?: return 0.0
        val currentLevel = spawner.getUpgradeLevel(upgradeType)
        return upgradePath.getCost(currentLevel)
    }

    fun getNextUpgradeValue(spawner: Spawner, upgradeType: String): Double {
        val currentLevel = spawner.getUpgradeLevel(upgradeType)
        val nextLevel = currentLevel + 1
        val value = getUpgradeValue(spawner.type, upgradeType, nextLevel)
        return when (value) {
            is Number -> value.toDouble()
            else -> 0.0
        }
    }

    fun getRequiredUpgradesInfo(spawner: Spawner, upgradeType: String): String {
        val upgrade = getUpgrade(spawner.type, upgradeType) ?: return "无前置需求"
        val currentLevel = spawner.getUpgradeLevel(upgradeType)
        val nextLevel = currentLevel + 1
        
        // 检查下一级别是否有前置要求
        val requiredUpgrades = upgrade.requiredUpgrades[nextLevel] ?: return "无前置需求"
        
        if (requiredUpgrades.isEmpty()) {
            return "无前置需求"
        }
        
        val requirements = mutableListOf<String>()
        for ((requiredType, requiredLevel) in requiredUpgrades) {
            val currentRequiredLevel = spawner.getUpgradeLevel(requiredType)
            val requiredUpgrade = getUpgrade(spawner.type, requiredType)
            val displayName = requiredUpgrade?.displayName ?: requiredType
            
            if (currentRequiredLevel >= requiredLevel) {
                requirements.add("§a$displayName §7(§2已满足§7)")
            } else {
                requirements.add("§c$displayName §7需要等级 §e$requiredLevel §7(当前: §e$currentRequiredLevel§7)")
            }
        }
        
        return "§7前置需求: " + requirements.joinToString("§7, ")
    }

    /**
     * 检查是否满足升级的前置要求
     */
    fun meetsRequirements(spawner: Spawner, upgradeType: String): Boolean {
        val upgrade = getUpgrade(spawner.type, upgradeType) ?: return false
        val currentLevel = spawner.getUpgradeLevel(upgradeType)
        val nextLevel = currentLevel + 1
        
        // 检查下一级别是否有前置要求
        val requiredUpgrades = upgrade.requiredUpgrades[nextLevel] ?: return true
        
        // 检查每个前置要求是否满足
        for ((requiredType, requiredLevel) in requiredUpgrades) {
            val currentRequiredLevel = spawner.getUpgradeLevel(requiredType)
            if (currentRequiredLevel < requiredLevel) {
                return false // 不满足某个前置要求
            }
        }
        
        return true // 所有前置要求都满足
    }

    fun upgradeSpawner(player: org.bukkit.entity.Player, spawner: Spawner, upgradeType: String) {
        // 升级功能默认启用，无需检查 enabled 配置
        
        if (!canUpgrade(spawner, upgradeType)) {
            player.sendMessage("§c该升级已达到最大等级！")
            return
        }

        // 检查前置要求
        if (!meetsRequirements(spawner, upgradeType)) {
            player.sendMessage("§c不满足前置升级要求！")
            player.sendMessage(getRequiredUpgradesInfo(spawner, upgradeType))
            return
        }

        val cost = getUpgradeCost(spawner, upgradeType)
        val economy = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getEconomyManager()
        
        if (economy != null && economy.hasEconomy() && cost > 0) {
            if (!economy.has(player, cost)) {
                player.sendMessage("§c余额不足！需要: §e$cost")
                return
            }
            economy.withdraw(player, cost)
        }

        applyUpgrade(spawner, upgradeType)
        (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getSpawnerManager()?.updateSpawner(spawner)
        player.sendMessage("§a升级成功！")
    }

    fun reload() {
        normalUpgrades.clear()
        premiumUpgrades.clear()
        loadUpgrades()
    }
}
