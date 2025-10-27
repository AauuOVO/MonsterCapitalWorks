package org.Aauu.monsterCapitalWorks.util

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ConditionChecker(private val plugin: JavaPlugin) {
    private var papiHooked = false

    fun initialize() {
        papiHooked = plugin.server.pluginManager.getPlugin("PlaceholderAPI") != null
    }

    fun checkYCondition(y: Int, minY: Int, maxY: Int): Boolean {
        return y in minY..maxY
    }

    fun checkPapiCondition(player: Player, condition: String): Boolean {
        if (!papiHooked) return true

        return try {
            val parts = condition.split(" ")
            if (parts.size < 3) return true

            val placeholder = parts[0]
            val operator = parts[1]
            val value = parts[2]

            val actualValue = PlaceholderAPI.setPlaceholders(player, placeholder)

            when (operator) {
                "==" -> actualValue == value
                "!=" -> actualValue != value
                ">" -> actualValue.toDoubleOrNull()?.let { it > value.toDoubleOrNull() ?: 0.0 } ?: false
                "<" -> actualValue.toDoubleOrNull()?.let { it < value.toDoubleOrNull() ?: 0.0 } ?: false
                ">=" -> actualValue.toDoubleOrNull()?.let { it >= value.toDoubleOrNull() ?: 0.0 } ?: false
                "<=" -> actualValue.toDoubleOrNull()?.let { it <= value.toDoubleOrNull() ?: 0.0 } ?: false
                else -> true
            }
        } catch (e: Exception) {
            plugin.logger.warning("PAPI条件检查失败: ${e.message}")
            true
        }
    }

    fun checkYConditions(location: Location, spawnConditions: ConfigurationSection?): Boolean {
        if (spawnConditions == null) {
            return true
        }
        
        val y = location.blockY
        
        // 检查min_y和max_y条件
        val minY = spawnConditions.getInt("min_y", Int.MIN_VALUE)
        val maxY = spawnConditions.getInt("max_y", Int.MAX_VALUE)
        
        return y in minY..maxY
    }

    fun checkAllConditions(player: Player, conditions: List<String>): Boolean {
        for (condition in conditions) {
            if (!checkCondition(player, condition)) {
                return false
            }
        }
        return true
    }

    private fun checkCondition(player: Player, condition: String): Boolean {
        return when {
            condition.startsWith("y:") -> {
                val parts = condition.substring(2).split("-")
                if (parts.size == 2) {
                    val minY = parts[0].toIntOrNull() ?: 0
                    val maxY = parts[1].toIntOrNull() ?: 256
                    checkYCondition(player.location.blockY, minY, maxY)
                } else {
                    true
                }
            }
            condition.startsWith("papi:") -> {
                checkPapiCondition(player, condition.substring(5))
            }
            else -> {
                // 支持直接的PAPI条件格式："%placeholder% == value"
                checkPapiCondition(player, condition)
            }
        }
    }
}
