package org.Aauu.monsterCapitalWorks.manager

import org.Aauu.monsterCapitalWorks.MonsterCapitalWorks
import org.bukkit.entity.Player
import org.Aauu.monsterCapitalWorks.model.SpawnerType

class PermissionManager(private val plugin: MonsterCapitalWorks) {
    
    fun canPlaceSpawner(player: Player, type: SpawnerType): Boolean {
        return when (type) {
            SpawnerType.NORMAL -> player.hasPermission("mcw.place.normal")
            SpawnerType.PREMIUM -> player.hasPermission("mcw.place.premium")
        }
    }

    fun canUseSpawner(player: Player, type: SpawnerType): Boolean {
        return when (type) {
            SpawnerType.NORMAL -> player.hasPermission("mcw.use.normal")
            SpawnerType.PREMIUM -> player.hasPermission("mcw.use.premium")
        }
    }

    fun isAdmin(player: Player): Boolean {
        return player.hasPermission("mcw.admin") || player.isOp
    }

    fun canBypassConditions(player: Player): Boolean {
        return player.hasPermission("mcw.bypass.conditions")
    }

    fun canReload(player: Player): Boolean {
        return player.hasPermission("mcw.admin.reload")
    }

    fun canGive(player: Player): Boolean {
        return player.hasPermission("mcw.admin.give")
    }

    fun canRemove(player: Player): Boolean {
        return player.hasPermission("mcw.admin.remove")
    }

    fun getSpawnerLimit(player: Player, type: SpawnerType): Int {
        // 从对应的配置文件获取基础限制
        val config = when (type) {
            SpawnerType.NORMAL -> plugin.getConfigManager().getNormalConfig()
            SpawnerType.PREMIUM -> plugin.getConfigManager().getPremiumConfig()
        } ?: return 0
        
        val typePath = "${type.name}.limits"
        val baseLimit = config.getInt("$typePath.base", 5)
        
        // 检查权限组额外限制
        var additionalLimit = 0
        val permissions = player.effectivePermissions
        
        for (attachment in permissions) {
            val permission = attachment.permission
            if (permission.startsWith("mcw.limit.${type.name.lowercase()}.extra.")) {
                try {
                    val amount = permission.substringAfterLast(".").toInt()
                    if (amount > additionalLimit) {
                        additionalLimit = amount
                    }
                } catch (e: NumberFormatException) {
                    // 忽略无效的权限格式
                }
            }
        }
        
        // 获取玩家已购买的限制
        val playerData = plugin.getDataManager().getPlayerData(player.uniqueId)
        val purchasedLimit = playerData.getPurchasedLimit(type)
        
        return baseLimit + additionalLimit + purchasedLimit
    }

    /**
     * 检查玩家是否可以使用精确模式
     * Normal类型：默认允许（返回true）
     * Premium类型：需要 mcw.spawnmode.precise 权限
     */
    fun canUsePreciseMode(player: Player, type: SpawnerType): Boolean {
        return when (type) {
            SpawnerType.NORMAL -> {
                // Normal类型默认允许精确模式
                true
            }
            SpawnerType.PREMIUM -> {
                // Premium类型需要精确模式权限
                player.hasPermission("mcw.spawnmode.precise")
            }
        }
    }

    /**
     * 检查玩家是否有精确模式权限（通用检查）
     */
    fun hasPreciseModePermission(player: Player): Boolean {
        return player.hasPermission("mcw.spawnmode.precise")
    }
}
