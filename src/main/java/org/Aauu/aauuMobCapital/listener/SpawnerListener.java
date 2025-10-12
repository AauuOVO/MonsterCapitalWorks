package org.Aauu.aauuMobCapital.listener;

import org.Aauu.aauuMobCapital.AauuMobCapital;
import org.Aauu.aauuMobCapital.model.Spawner;
import org.Aauu.aauuMobCapital.model.SpawnerType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerListener implements Listener {
    
    private final AauuMobCapital plugin;
    
    public SpawnerListener(AauuMobCapital plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.SPAWNER) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        if (!isAMCSpawner(item)) {
            return;
        }
        
        SpawnerType type = getSpawnerType(item);
        EntityType entityType = getEntityType(item);
        
        if (type == null || entityType == null) {
            event.setCancelled(true);
            return;
        }
        
        if (!player.hasPermission("amc.place." + type.name().toLowerCase())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
            return;
        }
        
        int placed = plugin.getSpawnerManager().getPlayerSpawnerCount(player.getUniqueId(), type);
        int limit = plugin.getPermissionManager().getSpawnerLimit(player, type);
        
        if (placed >= limit) {
            event.setCancelled(true);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%limit%", String.valueOf(limit));
            player.sendMessage(plugin.getConfigManager().getMessage("error.spawner_limit_reached", placeholders));
            return;
        }
        
        Location loc = block.getLocation();
        Spawner spawner = plugin.getSpawnerManager().createSpawner(loc, player.getUniqueId(), type, entityType);
        
        Map<String, Integer> upgrades = getUpgradesFromItem(item);
        if (!upgrades.isEmpty()) {
            for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
                spawner.setUpgradeLevel(entry.getKey(), entry.getValue());
            }
            plugin.getUpgradeManager().applyUpgrades(spawner);
            plugin.getSpawnerManager().saveSpawner(spawner);
        }
        
        org.bukkit.block.CreatureSpawner cs = (org.bukkit.block.CreatureSpawner) block.getState();
        cs.setSpawnedType(entityType);
        cs.setDelay(spawner.getSpawnDelay());
        cs.setMaxNearbyEntities(spawner.getMaxNearbyEntities());
        cs.setRequiredPlayerRange(spawner.getActivationRange());
        cs.setSpawnCount(spawner.getSpawnCount());
        cs.update();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) {
            return;
        }
        
        Location loc = block.getLocation();
        Spawner spawner = plugin.getSpawnerManager().getSpawner(loc);
        
        if (spawner == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        if (!spawner.getOwner().equals(player.getUniqueId()) && !player.hasPermission("amc.admin.break")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("error.not_owner"));
            return;
        }
        
        plugin.getSpawnerManager().removeSpawner(loc);
        event.setDropItems(false);
        ItemStack drop = createSpawnerItemWithUpgrades(spawner);
        loc.getWorld().dropItemNaturally(loc, drop);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) {
            return;
        }
        
        Location loc = block.getLocation();
        Spawner spawner = plugin.getSpawnerManager().getSpawner(loc);
        
        if (spawner == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        if (!spawner.getOwner().equals(player.getUniqueId()) && !player.hasPermission("amc.admin.use")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("error.not_owner"));
            return;
        }
        
        event.setCancelled(true);
        plugin.getGUIManager().openMainMenu(player, spawner);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String guiName = plugin.getGUIManager().getOpenGui(player.getUniqueId());
        
        if (guiName == null) {
            return;
        }
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        
        if ("entity_menu".equals(guiName)) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType().name().endsWith("_SPAWN_EGG")) {
                plugin.getGUIManager().handleEntityClick(player, slot);
                return;
            }
        }
        
        plugin.getGUIManager().handleClick(player, slot, guiName, event.isRightClick(), event.isShiftClick());
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        plugin.getGUIManager().closeGui(player.getUniqueId());
    }
    
    private boolean isAMCSpawner(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }
        
        // 检查lore中是否包含类型标识
        for (String line : lore) {
            if (line.contains("类型:")) {
                return true;
            }
        }
        
        return false;
    }
    
    private SpawnerType getSpawnerType(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            return null;
        }
        
        for (String line : lore) {
            if (line.contains("类型:")) {
                String typeStr = line.replace("§7类型: §e", "").trim();
                try {
                    return SpawnerType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        
        return null;
    }
    
    private EntityType getEntityType(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            return null;
        }
        
        for (String line : lore) {
            if (line.contains("实体:")) {
                String entityStr = line.replace("§7实体: §e", "").trim();
                try {
                    return EntityType.valueOf(entityStr);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        
        return null;
    }
    
    private ItemStack createSpawnerItemWithUpgrades(Spawner spawner) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6" + spawner.getType().name() + " 刷怪笼");
            
            // 创建lore，包含升级信息
            java.util.ArrayList<String> lore = new java.util.ArrayList<>();
            lore.add("§7类型: §e" + spawner.getType().name());
            lore.add("§7实体: §e" + spawner.getEntityType().name());
            lore.add("");
            lore.add("§7升级等级:");
            
            Map<String, Integer> upgrades = spawner.getUpgradeLevels();
            if (!upgrades.isEmpty()) {
                for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
                    if (entry.getValue() > 0) {
                        lore.add("§8  " + entry.getKey() + ": §e" + entry.getValue());
                    }
                }
            } else {
                lore.add("§8  无升级");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private Map<String, Integer> getUpgradesFromItem(ItemStack item) {
        Map<String, Integer> upgrades = new HashMap<>();
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return upgrades;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            return upgrades;
        }
        
        boolean inUpgradeSection = false;
        for (String line : lore) {
            if (line.contains("升级等级:")) {
                inUpgradeSection = true;
                continue;
            }
            
            if (inUpgradeSection && line.contains(":")) {
                String cleaned = line.replace("§8  ", "").replace("§e", "").trim();
                String[] parts = cleaned.split(":");
                if (parts.length == 2) {
                    try {
                        String upgradeName = parts[0].trim();
                        int level = Integer.parseInt(parts[1].trim());
                        upgrades.put(upgradeName, level);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        
        return upgrades;
    }
}
