package org.mcw.monstercapitalworks.listener;

import org.mcw.monstercapitalworks.model.Spawner;
import org.mcw.monstercapitalworks.model.SpawnerType;
import org.mcw.monstercapitalworks.util.ConditionChecker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
    
    private final org.mcw.monstercapitalworks.MonsterCapitalWorks plugin;
    
    public SpawnerListener(org.mcw.monstercapitalworks.MonsterCapitalWorks plugin) {
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
        
        if (!player.hasPermission("mcw.place." + type.name().toLowerCase())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
            return;
        }
        
        int placed = plugin.getSpawnerManager().getPlayerSpawnerCount(player.getUniqueId(), type);
        int limit = plugin.getPermissionManager().getSpawnerLimit(player, type);
        
        if (placed >= limit) {
            event.setCancelled(true);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%current%", String.valueOf(placed));
            placeholders.put("%limit%", String.valueOf(limit));
            player.sendMessage(plugin.getConfigManager().getMessage("error.spawner_limit_reached", placeholders));
            return;
        }
        
        Location loc = block.getLocation();
        
        // 检查spawn_conditions（Y坐标条件）
        if (!player.hasPermission("mcw.bypass.conditions")) {
            FileConfiguration entityConfig = type == SpawnerType.NORMAL ? 
                plugin.getConfigManager().getNormalEntities() : 
                plugin.getConfigManager().getPremiumEntities();
            
            if (entityConfig != null) {
                ConfigurationSection spawnConditions = entityConfig.getConfigurationSection(
                    "entities." + entityType.name() + ".spawn_conditions");
                
                if (!ConditionChecker.checkYConditions(loc, spawnConditions)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getMessage("error.spawn_conditions_not_met"));
                    return;
                }
            }
        }
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
        
        if (!spawner.getOwner().equals(player.getUniqueId()) && !player.hasPermission("mcw.admin.break")) {
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
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) {
            return;
        }
        
        Location loc = block.getLocation();
        Spawner spawner = plugin.getSpawnerManager().getSpawner(loc);
        
        if (spawner == null) {
            // 调试信息：刷怪笼未找到
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("未找到刷怪笼: " + loc);
                plugin.getLogger().info("已加载的刷怪笼数量: " + plugin.getSpawnerManager().getPlayerSpawners(event.getPlayer().getUniqueId()).size());
            }
            return;
        }
        
        Player player = event.getPlayer();
        
        // 处理 Shift+左键 掉落刷怪笼
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.isSneaking()) {
            event.setCancelled(true);
            
            // 检查是否是所有者或有管理员权限
            if (!spawner.getOwner().equals(player.getUniqueId()) && !player.hasPermission("mcw.admin.pickup")) {
                player.sendMessage(plugin.getConfigManager().getMessage("error.not_your_spawner"));
                return;
            }
            
            // 移除刷怪笼并掉落物品
            plugin.getSpawnerManager().removeSpawner(loc);
            block.setType(Material.AIR);
            
            // 创建带升级信息的刷怪笼物品
            ItemStack drop = createSpawnerItemWithUpgrades(spawner);
            loc.getWorld().dropItemNaturally(loc, drop);
            
            player.sendMessage(plugin.getConfigManager().getMessage("success.spawner_dropped"));
            return;
        }
        
        // 处理右键打开GUI
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        if (!spawner.getOwner().equals(player.getUniqueId()) && !player.hasPermission("mcw.admin.use")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("error.not_owner"));
            return;
        }
        
        event.setCancelled(true);
        plugin.getGUIManager().openMainMenu(player, spawner);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String guiName = plugin.getGUIManager().getOpenGui(player.getUniqueId());
        
        if (guiName == null) {
            return;
        }
        
        // 立即取消所有点击事件，防止物品被拿走或交换
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        // 如果点击的是玩家背包区域，直接返回不处理（防止玩家操作自己的背包）
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        
        // 额外检查：如果当前物品为空，也不处理
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType() == Material.AIR) {
            return;
        }
        
        // 处理实体选择菜单
        if ("entity_menu".equals(guiName)) {
            if (currentItem.getType().name().endsWith("_SPAWN_EGG")) {
                plugin.getGUIManager().handleEntityClick(player, slot);
                return;
            }
        }
        
        // 处理其他GUI点击
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
            // 使用自定义名称
            String displayName = getSpawnerDisplayName(spawner.getType(), spawner.getEntityType());
            meta.setDisplayName(displayName);
            
            // 创建简化的lore，不显示升级信息
            java.util.ArrayList<String> lore = new java.util.ArrayList<>();
            lore.add("§7类型: §e" + spawner.getType().name());
            lore.add("§7实体: §e" + spawner.getEntityType().name());
            
            // 隐藏升级信息，但仍然保存在NBT中
            // 升级信息将在getUpgradesFromItem中读取
            Map<String, Integer> upgrades = spawner.getUpgradeLevels();
            if (!upgrades.isEmpty()) {
                lore.add("");
                lore.add("§8升级等级:");
                for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
                    if (entry.getValue() > 0) {
                        lore.add("§8  " + entry.getKey() + ": §e" + entry.getValue());
                    }
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 获取刷怪笼的显示名称
     * @param type 刷怪笼类型
     * @param entityType 实体类型
     * @return 显示名称（已应用颜色代码）
     */
    private String getSpawnerDisplayName(SpawnerType type, EntityType entityType) {
        // 使用ConfigManager获取自定义名称（已包含颜色代码处理）
        String customName = plugin.getConfigManager().getSpawnerCustomName(type.name(), entityType.name());
        
        if (customName != null) {
            return customName;
        }
        
        // 如果ConfigManager返回null，使用简单的默认格式
        return plugin.getConfigManager().colorize("§6" + type.name() + " 刷怪笼");
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
