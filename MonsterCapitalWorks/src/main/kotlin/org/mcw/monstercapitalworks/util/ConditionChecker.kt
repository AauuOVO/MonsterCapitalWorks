package org.mcw.monstercapitalworks.util

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

/**
 * 条件检查工具类
 * 用于检查刷怪笼的生成条件
 */
object ConditionChecker {
    /**
     * 检查PAPI条件（conditions字段）
     * 使用PlaceholderAPI变量进行判断
     *
     * @param player 玩家
     * @param conditions 条件列表
     * @return 是否满足所有条件
     */
    fun checkPAPIConditions(player: Player, conditions: List<String>?): Boolean {
        if (conditions.isNullOrEmpty()) {
            return true // 没有条件则通过
        }

        return conditions.all { evaluatePAPICondition(player, it) }
    }

    /**
     * 检查Y坐标条件（spawn_conditions字段）
     * 插件内部判断刷怪笼位置的Y坐标
     *
     * @param location 刷怪笼位置
     * @param spawnConditions 生成条件配置节
     * @return 是否满足Y坐标条件
     */
    fun checkYConditions(location: Location, spawnConditions: ConfigurationSection?): Boolean {
        if (spawnConditions == null) {
            return true // 没有条件则通过
        }

        val y = location.blockY

        // 检查最小Y坐标
        if (spawnConditions.contains("min_y")) {
            val minY = spawnConditions.getInt("min_y")
            if (y < minY) {
                return false
            }
        }

        // 检查最大Y坐标
        if (spawnConditions.contains("max_y")) {
            val maxY = spawnConditions.getInt("max_y")
            if (y > maxY) {
                return false
            }
        }

        return true
    }

    /**
     * 评估单个PAPI条件
     *
     * @param player 玩家
     * @param condition 条件字符串，格式如："%world_name% == world_nether"
     * @return 是否满足条件
     */
    private fun evaluatePAPICondition(player: Player, condition: String): Boolean {
        return try {
            // 使用PlaceholderAPI解析变量
            val parsed = PlaceholderAPI.setPlaceholders(player, condition)

            // 支持的运算符（按优先级排序，避免子字符串匹配问题）
            when {
                parsed.contains(">=") -> {
                    val parts = parsed.split(">=", limit = 2)
                    parts.size == 2 && parseDouble(parts[0].trim()) >= parseDouble(parts[1].trim())
                }
                parsed.contains("<=") -> {
                    val parts = parsed.split("<=", limit = 2)
                    parts.size == 2 && parseDouble(parts[0].trim()) <= parseDouble(parts[1].trim())
                }
                parsed.contains("!=") -> {
                    val parts = parsed.split("!=", limit = 2)
                    parts.size == 2 && parts[0].trim() != parts[1].trim()
                }
                parsed.contains("==") -> {
                    val parts = parsed.split("==", limit = 2)
                    parts.size == 2 && parts[0].trim() == parts[1].trim()
                }
                parsed.contains(">") -> {
                    val parts = parsed.split(">", limit = 2)
                    parts.size == 2 && parseDouble(parts[0].trim()) > parseDouble(parts[1].trim())
                }
                parsed.contains("<") -> {
                    val parts = parsed.split("<", limit = 2)
                    parts.size == 2 && parseDouble(parts[0].trim()) < parseDouble(parts[1].trim())
                }
                else -> false
            }
        } catch (e: Exception) {
            // 解析失败，返回false
            false
        }
    }

    /**
     * 尝试将字符串解析为double
     *
     * @param str 字符串
     * @return double值，解析失败返回0
     */
    private fun parseDouble(str: String): Double {
        return try {
            str.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }
}
