package org.Aauu.aauuMobCapital.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 升级路径模型
 */
public class UpgradePath {
    private final String key;
    private String name;
    private String description;
    private int maxLevel;
    private final Map<Integer, UpgradeLevel> levels;

    // 完整构造函数
    public UpgradePath(String key, String name, String description, int maxLevel) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.maxLevel = maxLevel;
        this.levels = new HashMap<>();
    }
    
    // 简化构造函数（仅key）
    public UpgradePath(String key) {
        this.key = key;
        this.name = key;
        this.description = "";
        this.maxLevel = 0;
        this.levels = new HashMap<>();
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Map<Integer, UpgradeLevel> getLevels() {
        return levels;
    }

    public UpgradeLevel getLevel(int level) {
        return levels.get(level);
    }

    public void addLevel(int level, UpgradeLevel upgradeLevel) {
        levels.put(level, upgradeLevel);
        // 更新最大等级
        if (level > maxLevel) {
            maxLevel = level;
        }
    }
    
    /**
     * 添加等级（简化版本）
     */
    public void addLevel(int level, double value, double cost) {
        UpgradeLevel upgradeLevel = new UpgradeLevel((int)cost, value);
        addLevel(level, upgradeLevel);
    }
    
    /**
     * 检查是否有指定等级
     */
    public boolean hasLevel(int level) {
        return levels.containsKey(level);
    }
    
    /**
     * 获取指定等级的费用
     */
    public double getCost(int level) {
        UpgradeLevel upgradeLevel = levels.get(level);
        return upgradeLevel != null ? upgradeLevel.getCost() : 0;
    }
    
    /**
     * 获取指定等级的值
     */
    public double getValue(int level) {
        UpgradeLevel upgradeLevel = levels.get(level);
        return upgradeLevel != null ? upgradeLevel.getValue() : 0;
    }

    /**
     * 升级等级数据
     */
    public static class UpgradeLevel {
        private final int cost;
        private final double value;
        private final Map<String, Integer> requiredUpgrades;

        public UpgradeLevel(int cost, double value) {
            this.cost = cost;
            this.value = value;
            this.requiredUpgrades = new HashMap<>();
        }

        public int getCost() {
            return cost;
        }

        public double getValue() {
            return value;
        }

        public Map<String, Integer> getRequiredUpgrades() {
            return requiredUpgrades;
        }

        public void addRequiredUpgrade(String upgradeKey, int level) {
            requiredUpgrades.put(upgradeKey, level);
        }
    }
}
