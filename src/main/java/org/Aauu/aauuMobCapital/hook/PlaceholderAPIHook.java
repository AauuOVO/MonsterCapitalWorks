package org.Aauu.aauuMobCapital.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.Aauu.aauuMobCapital.AauuMobCapital;
import org.Aauu.aauuMobCapital.model.Spawner;
import org.Aauu.aauuMobCapital.model.SpawnerType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * PlaceholderAPI 扩展
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    
    private final AauuMobCapital plugin;
    
    public PlaceholderAPIHook(AauuMobCapital plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "amc";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // 刷怪笼数量相关
        if (params.equals("spawners_total")) {
            List<Spawner> spawners = plugin.getSpawnerManager().getPlayerSpawners(player.getUniqueId());
            return String.valueOf(spawners.size());
        }
        
        if (params.equals("spawners_normal")) {
            return String.valueOf(plugin.getSpawnerManager().getPlayerSpawnerCount(player.getUniqueId(), SpawnerType.NORMAL));
        }
        
        if (params.equals("spawners_premium")) {
            return String.valueOf(plugin.getSpawnerManager().getPlayerSpawnerCount(player.getUniqueId(), SpawnerType.PREMIUM));
        }
        
        // 刷怪笼限制相关
        if (params.equals("limit_normal")) {
            return String.valueOf(plugin.getPermissionManager().getSpawnerLimit(player, SpawnerType.NORMAL));
        }
        
        if (params.equals("limit_premium")) {
            return String.valueOf(plugin.getPermissionManager().getSpawnerLimit(player, SpawnerType.PREMIUM));
        }
        
        // 玩家数据相关
        if (params.equals("purchased_normal")) {
            return String.valueOf(plugin.getDataManager().getPlayerData(player.getUniqueId()).getPurchasedLimit(SpawnerType.NORMAL));
        }
        
        if (params.equals("purchased_premium")) {
            return String.valueOf(plugin.getDataManager().getPlayerData(player.getUniqueId()).getPurchasedLimit(SpawnerType.PREMIUM));
        }
        
        // 精确位置相关（需要从当前打开的刷怪笼获取）
        Spawner spawner = plugin.getGUIManager().getSelectedSpawner(player.getUniqueId());
        if (spawner != null) {
            if (params.equals("precise_x")) {
                return String.format("%.1f", spawner.getPreciseX());
            }
            if (params.equals("precise_y")) {
                return String.format("%.1f", spawner.getPreciseY());
            }
            if (params.equals("precise_z")) {
                return String.format("%.1f", spawner.getPreciseZ());
            }
        }
        
        return null;
    }
}
