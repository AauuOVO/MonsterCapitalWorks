package org.mcw.monstercapitalworks.manager;

import org.mcw.monstercapitalworks.model.SpawnerType;
import org.bukkit.entity.Player;

/**
 * 权限管理器 - 处理权限检查
 */
public class PermissionManager {
    private final org.mcw.monstercapitalworks.MonsterCapitalWorks plugin;

    public PermissionManager(org.mcw.monstercapitalworks.MonsterCapitalWorks plugin) {
        this.plugin = plugin;
    }

    /**
     * 检查玩家是否有创建刷怪笼的权限
     */
    public boolean canCreate(Player player, SpawnerType type) {
        if (type == SpawnerType.NORMAL) {
            return player.hasPermission("mcw.create.normal");
        } else {
            return player.hasPermission("mcw.create.premium");
        }
    }

    /**
     * 检查玩家是否有打开GUI的权限
     */
    public boolean canOpenGUI(Player player, SpawnerType type) {
        if (type == SpawnerType.NORMAL) {
            return player.hasPermission("mcw.gui.normal");
        } else {
            return player.hasPermission("mcw.gui.premium");
        }
    }

    /**
     * 检查玩家是否有购买权限
     */
    public boolean canPurchase(Player player) {
        return player.hasPermission("mcw.gui.purchase");
    }

    /**
     * 检查玩家是否有管理员权限
     */
    public boolean isAdmin(Player player) {
        return player.hasPermission("mcw.admin");
    }

    /**
     * 检查玩家是否可以绕过生成条件
     */
    public boolean canBypassConditions(Player player) {
        return player.hasPermission("mcw.bypass.conditions");
    }

    /**
     * 检查玩家是否可以绕过禁用限制
     */
    public boolean canBypassDisabled(Player player) {
        return player.hasPermission("mcw.bypass.disabled");
    }

    /**
     * 检查玩家是否可以绕过数量限制
     */
    public boolean canBypassLimits(Player player) {
        return player.hasPermission("mcw.bypass.limits");
    }

    /**
     * 获取玩家从权限组获得的额外刷怪笼数量
     * 优化：使用二分查找减少权限检查次数
     */
    public int getPermissionExtraLimit(Player player, SpawnerType type) {
        String prefix = type == SpawnerType.NORMAL ? "mcw.limit.normal.extra." : "mcw.limit.premium.extra.";
        
        // 使用二分查找优化性能
        int left = 0;
        int right = 100;
        int maxExtra = 0;
        
        while (left <= right) {
            int mid = (left + right) / 2;
            if (player.hasPermission(prefix + mid)) {
                maxExtra = mid;
                left = mid + 1; // 继续查找更大的值
            } else {
                right = mid - 1;
            }
        }
        
        return maxExtra;
    }
    
    /**
     * 获取玩家的刷怪笼总限制（基础+权限额外+购买额外）
     */
    public int getSpawnerLimit(Player player, SpawnerType type) {
        String typeKey = type.name().toLowerCase();
        
        // 从limits.yml获取基础限制
        int baseLimit = plugin.getConfigManager().getLimits().getInt(typeKey + ".base", 5);
        
        // 权限额外限制
        int permExtra = getPermissionExtraLimit(player, type);
        
        // 已购买额外限制
        int purchasedExtra = plugin.getDataManager().getPlayerData(player.getUniqueId())
                .getPurchasedLimit(type);
        
        return baseLimit + permExtra + purchasedExtra;
    }

    /**
     * 检查玩家是否有重载配置的权限
     */
    public boolean canReload(Player player) {
        return player.hasPermission("mcw.command.reload");
    }
}
