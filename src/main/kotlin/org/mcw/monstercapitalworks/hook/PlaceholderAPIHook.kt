package org.mcw.monstercapitalworks.hook

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import org.mcw.monstercapitalworks.model.SpawnerType

/**
 * PlaceholderAPI 扩展
 */
class PlaceholderAPIHook(private val plugin: MonsterCapitalWorks) : PlaceholderExpansion() {
    
    override fun getIdentifier(): String = "mcw"
    
    override fun getAuthor(): String = plugin.description.authors.joinToString(", ")
    
    override fun getVersion(): String = plugin.description.version
    
    override fun persist(): Boolean = true
    
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) {
            return ""
        }
        
        return when (params) {
            // 刷怪笼数量相关
            "spawners_total" -> {
                val spawners = plugin.spawnerManager.getPlayerSpawners(player.uniqueId)
                spawners.size.toString()
            }
            "spawners_normal" -> {
                plugin.spawnerManager.getPlayerSpawnerCount(player.uniqueId, SpawnerType.NORMAL).toString()
            }
            "spawners_premium" -> {
                plugin.spawnerManager.getPlayerSpawnerCount(player.uniqueId, SpawnerType.PREMIUM).toString()
            }
            // 刷怪笼限制相关
            "limit_normal" -> {
                plugin.permissionManager.getSpawnerLimit(player, SpawnerType.NORMAL).toString()
            }
            "limit_premium" -> {
                plugin.permissionManager.getSpawnerLimit(player, SpawnerType.PREMIUM).toString()
            }
            // 玩家数据相关
            "purchased_normal" -> {
                plugin.dataManager.getPlayerData(player.uniqueId).getPurchasedLimit(SpawnerType.NORMAL).toString()
            }
            "purchased_premium" -> {
                plugin.dataManager.getPlayerData(player.uniqueId).getPurchasedLimit(SpawnerType.PREMIUM).toString()
            }
            // 精确位置相关（需要从当前打开的刷怪笼获取）
            "precise_x" -> {
                val spawner = plugin.guiManager.getSelectedSpawner(player.uniqueId)
                if (spawner != null) {
                    String.format("%.1f", spawner.preciseX)
                } else {
                    null
                }
            }
            "precise_y" -> {
                val spawner = plugin.guiManager.getSelectedSpawner(player.uniqueId)
                if (spawner != null) {
                    String.format("%.1f", spawner.preciseY)
                } else {
                    null
                }
            }
            "precise_z" -> {
                val spawner = plugin.guiManager.getSelectedSpawner(player.uniqueId)
                if (spawner != null) {
                    String.format("%.1f", spawner.preciseZ)
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
