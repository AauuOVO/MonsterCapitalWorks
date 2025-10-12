package org.Aauu.aauuMobCapital.manager;

import org.Aauu.aauuMobCapital.AauuMobCapital;
import org.Aauu.aauuMobCapital.model.SpawnerType;
import org.bukkit.entity.Player;

/**
 * 权限管理器 - 处理权限检查
 */
public class PermissionManager {
    private final AauuMobCapital plugin;

    public PermissionManager(AauuMobCapital plugin) {
        this.plugin = plugin;
    }

    /**
     * 检查玩家是否有创建刷怪笼的权限
     */
    public boolean canCreate(Player player, SpawnerType type) {
        if (type == SpawnerType.NORMAL) {
            return player.hasPermission("amc.create.normal");
        } else {
            return player.hasPermission("amc.create.premium");
        }
    }

    /**
     * 检查玩家是否有打开GUI的权限
     */
    public boolean canOpenGUI(Player player, SpawnerType type) {
        if (type == SpawnerType.NORMAL) {
            return player.hasPermission("amc.gui.normal");
        } else {
            return player.hasPermission("amc.gui.premium");
        }
    }

    /**
     * 检查玩家是否有购买权限
     */
    public boolean canPurchase(Player player) {
        return player.hasPermission("amc.gui.purchase");
    }

    /**
     * 检查玩家是否有管理员权限
     */
    public boolean isAdmin(Player player) {
        return player.hasPermission("amc.admin");
    }

    /**
     * 检查玩家是否可以绕过生成条件
     */
    public boolean canBypassConditions(Player player) {
        return player.hasPermission("amc.bypass.conditions");
    }

    /**
     * 检查玩家是否可以绕过禁用限制
     */
    public boolean canBypassDisabled(Player player) {
        return player.hasPermission("amc.bypass.disabled");
    }

    /**
     * 检查玩家是否可以绕过数量限制
     */
    public boolean canBypassLimits(Player player) {
        return player.hasPermission("amc.bypass.limits");
    }

    /**
     * 获取玩家从权限组获得的额外刷怪笼数量
     */
    public int getPermissionLimit(Player player, SpawnerType type) {
        String prefix = type == SpawnerType.NORMAL ? "amc.limit.normal." : "amc.limit.premium.";
        
        int maxLimit = 0;
        for (int i = 1; i <= 100; i++) {
            if (player.hasPermission(prefix + i)) {
                maxLimit = Math.max(maxLimit, i);
            }
        }
        
        return maxLimit;
    }
    
    /**
     * 获取玩家的刷怪笼总限制（基础+权限+购买）
     */
    public int getSpawnerLimit(Player player, SpawnerType type) {
        // 基础限制
        int baseLimit = type == SpawnerType.NORMAL 
                ? plugin.getConfig().getInt("normal_spawner.base_limit", 5)
                : plugin.getConfig().getInt("premium_spawner.base_limit", 2);
        
        // 权限限制
        int permLimit = getPermissionLimit(player, type);
        
        // 已购买限制
        int purchasedLimit = plugin.getDataManager().getPlayerData(player.getUniqueId())
                .getPurchasedLimit(type);
        
        return baseLimit + permLimit + purchasedLimit;
    }

    /**
     * 检查玩家是否有重载配置的权限
     */
    public boolean canReload(Player player) {
        return player.hasPermission("amc.command.reload");
    }

    /**
     * 检查玩家是否有编辑GUI的权限
     */
    public boolean canEditGUI(Player player) {
        return player.hasPermission("amc.gui.edit");
    }
}
