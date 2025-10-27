package org.mcw.monstercapitalworks.manager

import org.bukkit.entity.Player
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import org.mcw.monstercapitalworks.model.SpawnerType

/**
 * 权限管理器 - 处理权限检查
 */
class PermissionManager(private val plugin: MonsterCapitalWorks) {

    /**
     * 检查玩家是否有创建刷怪笼的权限
     */
    fun canCreate(player: Player, type: SpawnerType): Boolean {
        return if (type == SpawnerType.NORMAL) {
            player.hasPermission("mcw.create.normal")
        } else {
            player.hasPermission("mcw.create.premium")
        }
    }

    /**
     * 检查玩家是否有打开GUI的权限
     */
    fun canOpenGUI(player: Player, type: SpawnerType): Boolean {
        return if (type == SpawnerType.NORMAL) {
            player.hasPermission("mcw.gui.normal")
        } else {
            player.hasPermission("mcw.gui.premium")
        }
    }

    /**
     * 检查玩家是否有购买权限
     */
    fun canPurchase(player: Player): Boolean {
        return player.hasPermission("mcw.gui.purchase")
    }

    /**
     * 检查玩家是否有管理员权限
     */
    fun isAdmin(player: Player): Boolean {
        return player.hasPermission("mcw.admin")
    }

    /**
     * 检查玩家是否可以绕过生成条件
     */
    fun canBypassConditions(player: Player): Boolean {
        return player.hasPermission("mcw.bypass.conditions")
    }

    /**
     * 检查玩家是否可以绕过禁用限制
     */
    fun canBypassDisabled(player: Player): Boolean {
        return player.hasPermission("mcw.bypass.disabled")
    }

    /**
     * 检查玩家是否可以绕过数量限制
     */
    fun canBypassLimits(player: Player): Boolean {
        return player.hasPermission("mcw.bypass.limits")
    }

    /**
     * 获取玩家从权限组获得的额外刷怪笼数量
     * 优化：使用二分查找减少权限检查次数
     */
    fun getPermissionExtraLimit(player: Player, type: SpawnerType): Int {
        val prefix = if (type == SpawnerType.NORMAL) "mcw.limit.normal.extra." else "mcw.limit.premium.extra."
        
        // 使用二分查找优化性能
        var left = 0
        var right = 100
        var maxExtra = 0
        
        while (left <= right) {
            val mid = (left + right) / 2
            if (player.hasPermission(prefix + mid)) {
                maxExtra = mid
                left = mid + 1 // 继续查找更大的值
            } else {
                right = mid - 1
            }
        }
        
        return maxExtra
    }
    
    /**
     * 获取玩家的刷怪笼总限制（基础+权限额外+购买额外）
     */
    fun getSpawnerLimit(player: Player, type: SpawnerType): Int {
        val typeKey = type.name.lowercase()
        
        // 从limits.yml获取基础限制
        val baseLimit = plugin.configManager.getConfig("limits")?.getInt("$typeKey.base", 5) ?: 5
        
        // 权限额外限制
        val permExtra = getPermissionExtraLimit(player, type)
        
        // 已购买额外限制
        val playerData = plugin.dataManager.getPlayerDataCache(player.uniqueId)
        val purchasedExtra = playerData?.getPurchasedLimit(type) ?: 0
        
        return baseLimit + permExtra + purchasedExtra
    }

    /**
     * 检查玩家是否有重载配置的权限
     */
    fun canReload(player: Player): Boolean {
        return player.hasPermission("mcw.command.reload")
    }
}
