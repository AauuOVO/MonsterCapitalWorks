package org.mcw.monstercapitalworks.manager;

import org.mcw.monstercapitalworks.model.Spawner;
import org.mcw.monstercapitalworks.model.SpawnerType;
import org.mcw.monstercapitalworks.model.UpgradePath;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * 升级管理器 - 管理刷怪笼升级系统
 */
public class UpgradeManager {
    
    private final org.mcw.monstercapitalworks.MonsterCapitalWorks plugin;
    private final Map<String, UpgradePath> normalUpgrades;
    private final Map<String, UpgradePath> premiumUpgrades;
    
    public UpgradeManager(org.mcw.monstercapitalworks.MonsterCapitalWorks plugin) {
        this.plugin = plugin;
        this.normalUpgrades = new HashMap<>();
        this.premiumUpgrades = new HashMap<>();
    }
    
    /**
     * 初始化管理器
     */
    public void initialize() {
        loadUpgrades();
        plugin.getLogger().info("升级管理器已初始化");
    }
    
    /**
     * 加载所有升级配置
     */
    private void loadUpgrades() {
        ConfigManager configManager = plugin.getConfigManager();
        
        // 加载普通升级
        FileConfiguration normalConfig = configManager.getNormalUpgrades();
        if (normalConfig != null) {
            loadUpgradesFromConfig(normalConfig, normalUpgrades);
        }
        
        // 加载付费升级
        FileConfiguration premiumConfig = configManager.getPremiumUpgrades();
        if (premiumConfig != null) {
            loadUpgradesFromConfig(premiumConfig, premiumUpgrades);
        }
        
        plugin.getLogger().info("已加载 " + normalUpgrades.size() + " 个普通升级路径");
        plugin.getLogger().info("已加载 " + premiumUpgrades.size() + " 个付费升级路径");
    }
    
    /**
     * 从配置文件加载升级
     */
    private void loadUpgradesFromConfig(FileConfiguration config, Map<String, UpgradePath> upgradeMap) {
        ConfigurationSection upgradesSection = config.getConfigurationSection("upgrades");
        if (upgradesSection == null) {
            return;
        }
        
        for (String key : upgradesSection.getKeys(false)) {
            ConfigurationSection upgradeSection = upgradesSection.getConfigurationSection(key);
            if (upgradeSection == null) {
                continue;
            }
            
            UpgradePath path = new UpgradePath(key);
            
            // 加载升级等级
            ConfigurationSection levelsSection = upgradeSection.getConfigurationSection("levels");
            if (levelsSection != null) {
                for (String levelKey : levelsSection.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelKey);
                        ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
                        
                        if (levelSection != null) {
                            double value = levelSection.getDouble("value", 0);
                            double cost = levelSection.getDouble("cost", 0);
                            
                            path.addLevel(level, value, cost);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("无效的升级等级: " + levelKey);
                    }
                }
            }
            
            upgradeMap.put(key, path);
        }
    }
    
    /**
     * 获取升级路径
     */
    public UpgradePath getUpgradePath(SpawnerType type, String upgradeName) {
        Map<String, UpgradePath> upgrades = type == SpawnerType.NORMAL ? normalUpgrades : premiumUpgrades;
        return upgrades.get(upgradeName);
    }
    
    /**
     * 升级刷怪笼
     */
    public boolean upgradeSpawner(Player player, Spawner spawner, String upgradeName) {
        UpgradePath path = getUpgradePath(spawner.getType(), upgradeName);
        if (path == null) {
            player.sendMessage("§c升级失败：未找到升级路径！");
            return false;
        }
        
        // 获取当前等级
        int currentLevel = spawner.getUpgradeLevel(upgradeName);
        int nextLevel = currentLevel + 1;
        int maxLevel = path.getMaxLevel();
        
        // 检查是否已达到最大等级
        if (currentLevel >= maxLevel) {
            player.sendMessage("§c该升级已达到最大等级！无法继续升级。");
            return false;
        }
        
        // 检查是否可以升级到下一级
        if (!path.hasLevel(nextLevel)) {
            player.sendMessage("§c该升级已达到最大等级！无法继续升级。");
            return false;
        }
        
        // 检查前置条件
        if (!checkRequiredUpgrades(player, spawner, upgradeName, nextLevel)) {
            player.sendMessage("§c升级失败：不满足前置条件！");
            String requiredInfo = getRequiredUpgradesInfo(spawner, upgradeName);
            if (!requiredInfo.isEmpty()) {
                player.sendMessage("§7需要：");
                player.sendMessage(requiredInfo);
            }
            return false;
        }
        
        // 获取升级费用
        double cost = path.getCost(nextLevel);
        
        // 检查经济
        EconomyManager economy = plugin.getEconomyManager();
        if (economy != null && economy.isEnabled()) {
            if (!economy.has(player, cost)) {
                player.sendMessage(plugin.getConfigManager().getMessage("error.insufficient_funds"));
                return false;
            }
            
            // 扣除费用
            economy.withdraw(player, cost);
        }
        
        // 执行升级
        spawner.setUpgradeLevel(upgradeName, nextLevel);
        applyUpgrade(spawner, upgradeName, nextLevel, path);
        
        // 保存刷怪笼
        plugin.getSpawnerManager().saveSpawner(spawner);
        
        player.sendMessage(plugin.getConfigManager().getMessage("success.upgraded"));
        
        return true;
    }
    
    /**
     * 应用升级效果
     */
    private void applyUpgrade(Spawner spawner, String upgradeName, int level, UpgradePath path) {
        double value = path.getValue(level);
        
        switch (upgradeName) {
            case "speed":
                // 速度升级 - 减少生成延迟
                spawner.setSpawnDelay((int) value);
                break;
                
            case "count":
                // 数量升级 - 增加生成数量
                spawner.setSpawnCount((int) value);
                break;
                
            case "max_nearby":
                // 最大附近实体升级
                spawner.setMaxNearbyEntities((int) value);
                break;
                
            case "range":
                // 激活范围升级
                spawner.setActivationRange((int) value);
                break;
                
            case "storage":
                // 存储升级
                spawner.setMaxStorage((int) value);
                break;
                
            default:
                plugin.getLogger().warning("未知的升级类型: " + upgradeName);
                break;
        }
    }
    
    /**
     * 应用所有升级效果（用于恢复刷怪笼时）
     */
    public void applyUpgrades(Spawner spawner) {
        Map<String, Integer> upgrades = spawner.getUpgradeLevels();
        
        for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
            String upgradeName = entry.getKey();
            int level = entry.getValue();
            
            if (level > 0) {
                UpgradePath path = getUpgradePath(spawner.getType(), upgradeName);
                if (path != null && path.hasLevel(level)) {
                    applyUpgrade(spawner, upgradeName, level, path);
                }
            }
        }
    }
    
    /**
     * 获取升级费用
     */
    public double getUpgradeCost(Spawner spawner, String upgradeName) {
        UpgradePath path = getUpgradePath(spawner.getType(), upgradeName);
        if (path == null) {
            return 0;
        }
        
        int currentLevel = spawner.getUpgradeLevel(upgradeName);
        int nextLevel = currentLevel + 1;
        
        return path.getCost(nextLevel);
    }
    
    /**
     * 获取升级后的值
     */
    public double getUpgradeValue(Spawner spawner, String upgradeName, int level) {
        UpgradePath path = getUpgradePath(spawner.getType(), upgradeName);
        if (path == null) {
            return 0;
        }
        
        return path.getValue(level);
    }
    
    /**
     * 获取当前升级值
     */
    public double getCurrentUpgradeValue(Spawner spawner, String upgradeName) {
        int level = spawner.getUpgradeLevel(upgradeName);
        return getUpgradeValue(spawner, upgradeName, level);
    }
    
    /**
     * 获取下一级升级值
     */
    public double getNextUpgradeValue(Spawner spawner, String upgradeName) {
        int nextLevel = spawner.getUpgradeLevel(upgradeName) + 1;
        return getUpgradeValue(spawner, upgradeName, nextLevel);
    }
    
    /**
     * 检查是否可以升级
     */
    public boolean canUpgrade(Spawner spawner, String upgradeName) {
        UpgradePath path = getUpgradePath(spawner.getType(), upgradeName);
        if (path == null) {
            return false;
        }
        
        int currentLevel = spawner.getUpgradeLevel(upgradeName);
        int nextLevel = currentLevel + 1;
        
        return path.hasLevel(nextLevel);
    }
    
    /**
     * 获取最大等级
     */
    public int getMaxLevel(SpawnerType type, String upgradeName) {
        UpgradePath path = getUpgradePath(type, upgradeName);
        if (path == null) {
            return 0;
        }
        
        return path.getMaxLevel();
    }
    
    /**
     * 检查前置升级条件（静默检查，不发送消息）
     */
    private boolean checkRequiredUpgrades(Player player, Spawner spawner, String upgradeName, int nextLevel) {
        // 获取配置文件
        FileConfiguration config = spawner.getType() == SpawnerType.NORMAL 
                ? plugin.getConfigManager().getNormalUpgrades()
                : plugin.getConfigManager().getPremiumUpgrades();
        
        if (config == null) {
            return true;
        }
        
        // 获取该等级的前置条件
        String path = "upgrades." + upgradeName + ".levels." + nextLevel;
        ConfigurationSection levelSection = config.getConfigurationSection(path);
        
        if (levelSection == null) {
            return true;
        }
        
        ConfigurationSection requiredSection = levelSection.getConfigurationSection("required_upgrades");
        if (requiredSection == null) {
            return true;
        }
        
        // 检查每个前置条件
        for (String requiredUpgrade : requiredSection.getKeys(false)) {
            int requiredLevel = requiredSection.getInt(requiredUpgrade);
            int currentLevel = spawner.getUpgradeLevel(requiredUpgrade);
            
            if (currentLevel < requiredLevel) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取升级的前置需求信息（用于GUI显示）
     */
    public String getRequiredUpgradesInfo(Spawner spawner, String upgradeName) {
        int nextLevel = spawner.getUpgradeLevel(upgradeName) + 1;
        
        FileConfiguration config = spawner.getType() == SpawnerType.NORMAL 
                ? plugin.getConfigManager().getNormalUpgrades()
                : plugin.getConfigManager().getPremiumUpgrades();
        
        if (config == null) {
            return "";
        }
        
        String path = "upgrades." + upgradeName + ".levels." + nextLevel;
        ConfigurationSection levelSection = config.getConfigurationSection(path);
        
        if (levelSection == null) {
            return "";
        }
        
        ConfigurationSection requiredSection = levelSection.getConfigurationSection("required_upgrades");
        if (requiredSection == null) {
            return "";
        }
        
        StringBuilder info = new StringBuilder();
        for (String requiredUpgrade : requiredSection.getKeys(false)) {
            int requiredLevel = requiredSection.getInt(requiredUpgrade);
            int currentLevel = spawner.getUpgradeLevel(requiredUpgrade);
            
            String upgradePath = "upgrades." + requiredUpgrade + ".name";
            String upgradeDisplayName = config.getString(upgradePath, requiredUpgrade);
            upgradeDisplayName = upgradeDisplayName.replace('&', '§');
            
            if (currentLevel < requiredLevel) {
                info.append("§c✘ ").append(upgradeDisplayName).append(" §e").append(requiredLevel).append("§c级 §7(当前: §e").append(currentLevel).append("§7)\n");
            } else {
                info.append("§a✔ ").append(upgradeDisplayName).append(" §e").append(requiredLevel).append("§a级\n");
            }
        }
        
        return info.toString().trim();
    }
    
    /**
     * 重载升级配置
     */
    public void reload() {
        normalUpgrades.clear();
        premiumUpgrades.clear();
        loadUpgrades();
    }
}
