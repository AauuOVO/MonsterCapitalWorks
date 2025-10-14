package org.mcw.monstercapitalworks.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 条件检查工具类
 * 用于检查刷怪笼的生成条件
 */
public class ConditionChecker {
    
    /**
     * 检查PAPI条件（conditions字段）
     * 使用PlaceholderAPI变量进行判断
     * 
     * @param player 玩家
     * @param conditions 条件列表
     * @return 是否满足所有条件
     */
    public static boolean checkPAPIConditions(Player player, List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true; // 没有条件则通过
        }
        
        for (String condition : conditions) {
            if (!evaluatePAPICondition(player, condition)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查Y坐标条件（spawn_conditions字段）
     * 插件内部判断刷怪笼位置的Y坐标
     * 
     * @param location 刷怪笼位置
     * @param spawnConditions 生成条件配置节
     * @return 是否满足Y坐标条件
     */
    public static boolean checkYConditions(Location location, ConfigurationSection spawnConditions) {
        if (spawnConditions == null) {
            return true; // 没有条件则通过
        }
        
        int y = location.getBlockY();
        
        // 检查最小Y坐标
        if (spawnConditions.contains("min_y")) {
            int minY = spawnConditions.getInt("min_y");
            if (y < minY) {
                return false;
            }
        }
        
        // 检查最大Y坐标
        if (spawnConditions.contains("max_y")) {
            int maxY = spawnConditions.getInt("max_y");
            if (y > maxY) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 评估单个PAPI条件
     * 
     * @param player 玩家
     * @param condition 条件字符串，格式如："%world_name% == world_nether"
     * @return 是否满足条件
     */
    private static boolean evaluatePAPICondition(Player player, String condition) {
        try {
            // 使用PlaceholderAPI解析变量
            String parsed = PlaceholderAPI.setPlaceholders(player, condition);
            
            // 支持的运算符
            if (parsed.contains("==")) {
                String[] parts = parsed.split("==");
                if (parts.length == 2) {
                    return parts[0].trim().equals(parts[1].trim());
                }
            } else if (parsed.contains("!=")) {
                String[] parts = parsed.split("!=");
                if (parts.length == 2) {
                    return !parts[0].trim().equals(parts[1].trim());
                }
            } else if (parsed.contains(">=")) {
                String[] parts = parsed.split(">=");
                if (parts.length == 2) {
                    double left = parseDouble(parts[0].trim());
                    double right = parseDouble(parts[1].trim());
                    return left >= right;
                }
            } else if (parsed.contains("<=")) {
                String[] parts = parsed.split("<=");
                if (parts.length == 2) {
                    double left = parseDouble(parts[0].trim());
                    double right = parseDouble(parts[1].trim());
                    return left <= right;
                }
            } else if (parsed.contains(">")) {
                String[] parts = parsed.split(">");
                if (parts.length == 2) {
                    double left = parseDouble(parts[0].trim());
                    double right = parseDouble(parts[1].trim());
                    return left > right;
                }
            } else if (parsed.contains("<")) {
                String[] parts = parsed.split("<");
                if (parts.length == 2) {
                    double left = parseDouble(parts[0].trim());
                    double right = parseDouble(parts[1].trim());
                    return left < right;
                }
            }
            
            return false;
        } catch (Exception e) {
            // 解析失败，返回false
            return false;
        }
    }
    
    /**
     * 尝试将字符串解析为double
     * 
     * @param str 字符串
     * @return double值，解析失败返回0
     */
    private static double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
