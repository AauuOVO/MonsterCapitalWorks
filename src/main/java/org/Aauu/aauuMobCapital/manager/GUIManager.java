package org.Aauu.aauuMobCapital.manager;

import org.Aauu.aauuMobCapital.AauuMobCapital;
import org.Aauu.aauuMobCapital.model.PlayerData;
import org.Aauu.aauuMobCapital.model.Spawner;
import org.Aauu.aauuMobCapital.model.SpawnerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager {
    
    private final AauuMobCapital plugin;
    private final Map<UUID, String> openGuis;
    private final Map<UUID, Spawner> selectedSpawners;
    
    public GUIManager(AauuMobCapital plugin) {
        this.plugin = plugin;
        this.openGuis = new HashMap<>();
        this.selectedSpawners = new HashMap<>();
    }
    
    public void initialize() {
    }
    
    public void openMainMenu(Player player, Spawner spawner) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig("main_menu");
        if (guiConfig == null) {
            return;
        }
        
        String title = colorize(guiConfig.getString("title", "刷怪笼控制面板"));
        int size = guiConfig.getInt("size", 27);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createGuiItem(itemSection, player, spawner);
                    int slot = itemSection.getInt("slot", 0);
                    inv.setItem(slot, item);
                }
            }
        }
        
        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), "main_menu");
        selectedSpawners.put(player.getUniqueId(), spawner);
    }
    
    public void openUpgradeMenu(Player player, Spawner spawner) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig("upgrade_menu");
        if (guiConfig == null) {
            return;
        }
        
        String title = colorize(guiConfig.getString("title", "升级刷怪笼"));
        int size = guiConfig.getInt("size", 54);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createGuiItem(itemSection, player, spawner);
                    int slot = itemSection.getInt("slot", 0);
                    inv.setItem(slot, item);
                }
            }
        }
        
        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), "upgrade_menu");
        selectedSpawners.put(player.getUniqueId(), spawner);
    }
    
    public void openEntityMenu(Player player, Spawner spawner) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig("entity_menu");
        if (guiConfig == null) {
            return;
        }
        
        String title = colorize(guiConfig.getString("title", "选择生物"));
        int size = guiConfig.getInt("size", 54);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        // 获取可用实体列表
        FileConfiguration entityConfig = spawner.getType() == SpawnerType.NORMAL 
                ? plugin.getConfigManager().getNormalEntities()
                : plugin.getConfigManager().getPremiumEntities();
        
        if (entityConfig != null) {
            ConfigurationSection entitiesSection = entityConfig.getConfigurationSection("entities");
            if (entitiesSection != null) {
                int slot = 0;
                for (String entityKey : entitiesSection.getKeys(false)) {
                    if (slot >= 45) break; // 留出空间给返回按钮
                    
                    ConfigurationSection entitySection = entitiesSection.getConfigurationSection(entityKey);
                    if (entitySection != null) {
                        try {
                            EntityType entityType = EntityType.valueOf(entityKey.toUpperCase());
                            ItemStack item = createEntityItem(entitySection, entityType, player);
                            inv.setItem(slot++, item);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("无效的实体类型: " + entityKey);
                        }
                    }
                }
            }
        }
        
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            ConfigurationSection backSection = itemsSection.getConfigurationSection("back");
            if (backSection != null) {
                ItemStack backItem = createGuiItem(backSection, player, spawner);
                int slot = backSection.getInt("slot", 49);
                inv.setItem(slot, backItem);
            }
        }
        
        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), "entity_menu");
        selectedSpawners.put(player.getUniqueId(), spawner);
    }
    
    public void openBuyLimitMenu(Player player, Spawner spawner) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig("buy_limit_menu");
        if (guiConfig == null) {
            return;
        }
        
        String title = colorize(guiConfig.getString("title", "购买数量上限"));
        int size = guiConfig.getInt("size", 27);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createGuiItem(itemSection, player, spawner);
                    int slot = itemSection.getInt("slot", 0);
                    inv.setItem(slot, item);
                }
            }
        }
        
        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), "buy_limit_menu");
        selectedSpawners.put(player.getUniqueId(), spawner);
    }
    
    public void openPrecisePosMenu(Player player, Spawner spawner) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig("precise_pos_menu");
        if (guiConfig == null) {
            return;
        }
        
        String title = colorize(guiConfig.getString("title", "精确位置设置"));
        int size = guiConfig.getInt("size", 27);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createGuiItem(itemSection, player, spawner);
                    int slot = itemSection.getInt("slot", 0);
                    inv.setItem(slot, item);
                }
            }
        }
        
        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), "precise_pos_menu");
        selectedSpawners.put(player.getUniqueId(), spawner);
    }
    
    private ItemStack createGuiItem(ConfigurationSection section, Player player, Spawner spawner) {
        String materialName = section.getString("material", "STONE");
        Material material;
        
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = section.getString("name", "");
            name = replacePlaceholders(name, player, spawner);
            meta.setDisplayName(colorize(name));
            
            List<String> lore = section.getStringList("lore");
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                line = replacePlaceholders(line, player, spawner);
                processedLore.add(colorize(line));
            }
            meta.setLore(processedLore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createEntityItem(ConfigurationSection section, EntityType entityType, Player player) {
        // 使用对应的怪物蛋材质
        Material material = getSpawnEggMaterial(entityType);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = section.getString("name", entityType.name());
            meta.setDisplayName(colorize(name));
            
            // 获取价格和解锁状态
            double price = section.getDouble("price", 0);
            boolean requireUnlock = section.getBoolean("require_unlock", true);
            boolean enabled = section.getBoolean("enabled", true);
            
            // 检查玩家是否已解锁
            boolean unlocked = !requireUnlock || hasUnlockedEntity(player, entityType);
            
            List<String> lore = section.getStringList("lore");
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(colorize(line));
            }
            
            // 添加价格和状态信息
            processedLore.add("");
            if (!enabled) {
                processedLore.add("§c✘ 已禁用");
            } else if (unlocked) {
                processedLore.add("§a✔ 已解锁");
            } else {
                processedLore.add("§e价格: §f" + String.format("%.2f", price));
                processedLore.add("§c✘ 未解锁");
                processedLore.add("§7点击购买解锁");
            }
            
            meta.setLore(processedLore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private Material getSpawnEggMaterial(EntityType entityType) {
        String eggName = entityType.name() + "_SPAWN_EGG";
        try {
            return Material.valueOf(eggName);
        } catch (IllegalArgumentException e) {
            // 如果没有对应的怪物蛋，返回默认材质
            return Material.SPAWNER;
        }
    }
    
    private boolean hasUnlockedEntity(Player player, EntityType entityType) {
        Spawner spawner = selectedSpawners.get(player.getUniqueId());
        if (spawner == null) {
            return false;
        }
        
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data != null && data.hasUnlockedEntity(spawner.getType(), entityType);
    }
    
    public void handleEntityClick(Player player, int slot) {
        Spawner spawner = selectedSpawners.get(player.getUniqueId());
        if (spawner == null) {
            return;
        }
        
        // 获取实体配置
        FileConfiguration entityConfig = spawner.getType() == SpawnerType.NORMAL 
                ? plugin.getConfigManager().getNormalEntities()
                : plugin.getConfigManager().getPremiumEntities();
        
        if (entityConfig == null) {
            return;
        }
        
        ConfigurationSection entitiesSection = entityConfig.getConfigurationSection("entities");
        if (entitiesSection == null) {
            return;
        }
        
        // 找到对应slot的实体
        int currentSlot = 0;
        for (String entityKey : entitiesSection.getKeys(false)) {
            if (currentSlot == slot) {
                ConfigurationSection entitySection = entitiesSection.getConfigurationSection(entityKey);
                if (entitySection != null) {
                    try {
                        EntityType entityType = EntityType.valueOf(entityKey.toUpperCase());
                        handleEntitySelection(player, spawner, entityType, entitySection);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§c无效的实体类型！");
                    }
                }
                return;
            }
            currentSlot++;
        }
    }
    
    private void handleEntitySelection(Player player, Spawner spawner, EntityType entityType, ConfigurationSection entityConfig) {
        boolean enabled = entityConfig.getBoolean("enabled", true);
        if (!enabled) {
            return;
        }
        
        boolean requireUnlock = entityConfig.getBoolean("require_unlock", true);
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        boolean unlocked = !requireUnlock || (data != null && data.hasUnlockedEntity(spawner.getType(), entityType));
        
        if (!unlocked) {
            double price = entityConfig.getDouble("price", 0);
            EconomyManager economy = plugin.getEconomyManager();
            
            if (economy != null && economy.isEnabled()) {
                if (economy.has(player, price)) {
                    economy.withdraw(player, price);
                    if (data != null) {
                        data.unlockEntity(spawner.getType(), entityType);
                        plugin.getDataManager().savePlayerData(data);
                    }
                    openEntityMenu(player, spawner);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("error.insufficient_funds"));
                }
            }
        } else {
            spawner.setEntityType(entityType);
            spawner.setStoredSpawns(0);
            plugin.getSpawnerManager().updateSpawnerEntity(spawner.getLocation(), entityType);
            plugin.getSpawnerManager().saveSpawner(spawner);
            player.closeInventory();
        }
    }
    
    private String replacePlaceholders(String text, Player player, Spawner spawner) {
        if (text == null || spawner == null) {
            return text;
        }
        
        UpgradeManager upgradeManager = plugin.getUpgradeManager();
        
        // 基本信息
        text = text.replace("%amc_type%", spawner.getType().name());
        text = text.replace("%amc_entity%", spawner.getEntityType().name());
        text = text.replace("%amc_location%", formatLocation(spawner.getLocation()));
        text = text.replace("%amc_active%", spawner.isStorageEnabled() ? "§a激活" : "§c未激活");
        text = text.replace("%amc_status%", spawner.isActive() ? "§a开启" : "§c关闭");
        text = text.replace("%amc_stored%", String.valueOf(spawner.getStoredSpawns()));
        
        // 生成模式信息
        String spawnModeText = spawner.getSpawnMode() == Spawner.SpawnMode.RANDOM ? "§e随机模式" : "§b精确模式";
        text = text.replace("%amc_spawn_mode%", spawnModeText);
        if (spawner.getSpawnMode() == Spawner.SpawnMode.PRECISE) {
            text = text.replace("%amc_precise_pos%", String.format("§7X:§e%.1f §7Y:§e%.1f §7Z:§e%.1f", 
                spawner.getPreciseX(), spawner.getPreciseY(), spawner.getPreciseZ()));
        } else {
            text = text.replace("%amc_precise_pos%", "§7未设置");
        }
        
        // 精确位置单独的占位符
        text = text.replace("%amc_precise_x%", String.format("%.1f", spawner.getPreciseX()));
        text = text.replace("%amc_precise_y%", String.format("%.1f", spawner.getPreciseY()));
        text = text.replace("%amc_precise_z%", String.format("%.1f", spawner.getPreciseZ()));
        
        // 升级等级
        text = text.replace("%amc_speed_level%", String.valueOf(spawner.getUpgradeLevel("speed")));
        text = text.replace("%amc_count_level%", String.valueOf(spawner.getUpgradeLevel("count")));
        text = text.replace("%amc_max_nearby_level%", String.valueOf(spawner.getUpgradeLevel("max_nearby")));
        text = text.replace("%amc_range_level%", String.valueOf(spawner.getUpgradeLevel("range")));
        text = text.replace("%amc_storage_level%", String.valueOf(spawner.getUpgradeLevel("storage")));
        
        // 当前值
        text = text.replace("%amc_speed_value%", String.valueOf(spawner.getSpawnDelay()));
        text = text.replace("%amc_count_value%", String.valueOf(spawner.getSpawnCount()));
        text = text.replace("%amc_max_nearby_value%", String.valueOf(spawner.getMaxNearbyEntities()));
        text = text.replace("%amc_range_value%", String.valueOf(spawner.getActivationRange()));
        text = text.replace("%amc_storage_value%", String.valueOf(spawner.getMaxStorage()));
        
        // 下一级值
        if (upgradeManager != null) {
            text = text.replace("%amc_speed_next_value%", String.valueOf((int)upgradeManager.getNextUpgradeValue(spawner, "speed")));
            text = text.replace("%amc_count_next_value%", String.valueOf((int)upgradeManager.getNextUpgradeValue(spawner, "count")));
            text = text.replace("%amc_max_nearby_next_value%", String.valueOf((int)upgradeManager.getNextUpgradeValue(spawner, "max_nearby")));
            text = text.replace("%amc_range_next_value%", String.valueOf((int)upgradeManager.getNextUpgradeValue(spawner, "range")));
            text = text.replace("%amc_storage_next_value%", String.valueOf((int)upgradeManager.getNextUpgradeValue(spawner, "storage")));
            
            // 升级费用
            text = text.replace("%amc_speed_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "speed")));
            text = text.replace("%amc_count_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "count")));
            text = text.replace("%amc_max_nearby_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "max_nearby")));
            text = text.replace("%amc_range_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "range")));
            text = text.replace("%amc_storage_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "storage")));
            
            // 前置需求信息
            text = text.replace("%amc_speed_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "speed"));
            text = text.replace("%amc_count_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "count"));
            text = text.replace("%amc_max_nearby_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "max_nearby"));
            text = text.replace("%amc_range_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "range"));
            text = text.replace("%amc_storage_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "storage"));
        }
        
        // 玩家数据
        if (player != null) {
            int placed = plugin.getSpawnerManager().getPlayerSpawnerCount(player.getUniqueId(), spawner.getType());
            int limit = plugin.getPermissionManager().getSpawnerLimit(player, spawner.getType());
            
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            int purchased = data != null ? data.getPurchasedLimit(spawner.getType()) : 0;
            
            // 基础限制和权限限制
            int baseLimit = plugin.getConfig().getInt("limits." + spawner.getType().name().toLowerCase() + ".base", 5);
            int permLimit = plugin.getPermissionManager().getPermissionLimit(player, spawner.getType());
            
            text = text.replace("%amc_placed%", String.valueOf(placed));
            text = text.replace("%amc_limit%", String.valueOf(limit));
            text = text.replace("%amc_purchased%", String.valueOf(purchased));
            text = text.replace("%amc_base_limit%", String.valueOf(baseLimit));
            text = text.replace("%amc_perm_limit%", String.valueOf(permLimit));
            
            // 购买费用占位符
            double buy1Cost = plugin.getConfig().getDouble("limits." + spawner.getType().name().toLowerCase() + ".buy_cost_per_slot", 1000.0);
            text = text.replace("%amc_buy_1_cost%", String.format("%.2f", buy1Cost * 1));
            text = text.replace("%amc_buy_5_cost%", String.format("%.2f", buy1Cost * 5));
            text = text.replace("%amc_buy_10_cost%", String.format("%.2f", buy1Cost * 10));
        }
        
        return text;
    }
    
    private String formatLocation(org.bukkit.Location location) {
        return String.format("%d, %d, %d", 
                location.getBlockX(), 
                location.getBlockY(), 
                location.getBlockZ());
    }
    
    private String colorize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('&', '§');
    }
    
    public String getOpenGui(UUID player) {
        return openGuis.get(player);
    }
    
    public Spawner getSelectedSpawner(UUID player) {
        return selectedSpawners.get(player);
    }
    
    public void closeGui(UUID player) {
        openGuis.remove(player);
        selectedSpawners.remove(player);
    }
    
    public void handleClick(Player player, int slot, String guiName, boolean isRightClick, boolean isShiftClick) {
        Spawner spawner = selectedSpawners.get(player.getUniqueId());
        if (spawner == null) {
            return;
        }
        
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig(guiName);
        if (guiConfig == null) {
            return;
        }
        
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            return;
        }
        
        // 查找点击的物品
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection != null && itemSection.getInt("slot", -1) == slot) {
                // 根据点击类型选择不同的动作列表
                List<String> actions;
                if (isRightClick && itemSection.contains("right_click_actions")) {
                    actions = itemSection.getStringList("right_click_actions");
                } else {
                    actions = itemSection.getStringList("actions");
                }
                executeActions(player, spawner, actions, isRightClick, isShiftClick);
                break;
            }
        }
    }
    
    private void executeActions(Player player, Spawner spawner, List<String> actions, boolean isRightClick, boolean isShiftClick) {
        for (String action : actions) {
            if (action.startsWith("open_gui:")) {
                String guiName = action.substring(9).trim();
                openGui(player, spawner, guiName);
            } else if (action.equals("close")) {
                player.closeInventory();
            } else if (action.startsWith("upgrade:")) {
                String upgradeName = action.substring(8).trim();
                plugin.getUpgradeManager().upgradeSpawner(player, spawner, upgradeName);
                // 刷新GUI
                String currentGui = openGuis.get(player.getUniqueId());
                if (currentGui != null) {
                    openGui(player, spawner, currentGui);
                }
            } else if (action.equals("toggle_active")) {
                spawner.setActive(!spawner.isActive());
                plugin.getSpawnerManager().saveSpawner(spawner);
                String currentGui = openGuis.get(player.getUniqueId());
                if (currentGui != null) {
                    openGui(player, spawner, currentGui);
                }
            } else if (action.equals("toggle_spawn_mode")) {
                if (player.hasPermission("amc.spawnmode.precise")) {
                    if (spawner.getSpawnMode() == Spawner.SpawnMode.RANDOM) {
                        spawner.setSpawnMode(Spawner.SpawnMode.PRECISE);
                    } else {
                        spawner.setSpawnMode(Spawner.SpawnMode.RANDOM);
                    }
                    plugin.getSpawnerManager().saveSpawner(spawner);
                    String currentGui = openGuis.get(player.getUniqueId());
                    if (currentGui != null) {
                        openGui(player, spawner, currentGui);
                    }
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
                }
            } else if (action.equals("set_precise_pos")) {
                if (player.hasPermission("amc.spawnmode.precise")) {
                    org.bukkit.Location playerLoc = player.getLocation();
                    org.bukkit.Location spawnerLoc = spawner.getLocation();
                    
                    // 计算相对偏移量
                    double offsetX = playerLoc.getX() - spawnerLoc.getX();
                    double offsetY = playerLoc.getY() - spawnerLoc.getY();
                    double offsetZ = playerLoc.getZ() - spawnerLoc.getZ();
                    
                    spawner.setPreciseX(offsetX);
                    spawner.setPreciseY(offsetY);
                    spawner.setPreciseZ(offsetZ);
                    spawner.setSpawnMode(Spawner.SpawnMode.PRECISE);
                    plugin.getSpawnerManager().saveSpawner(spawner);
                    String currentGui = openGuis.get(player.getUniqueId());
                    if (currentGui != null) {
                        openGui(player, spawner, currentGui);
                    }
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
                }
            } else if (action.startsWith("adjust_pos:")) {
                if (player.hasPermission("amc.spawnmode.precise")) {
                    String[] parts = action.substring(11).trim().split(" ");
                    if (parts.length >= 2) {
                        try {
                            String axis = parts[0];
                            double amount = Double.parseDouble(parts[1]);
                            
                            switch (axis.toLowerCase()) {
                                case "x":
                                    if (amount == 0) {
                                        spawner.setPreciseX(0);
                                    } else {
                                        spawner.setPreciseX(spawner.getPreciseX() + amount);
                                    }
                                    break;
                                case "y":
                                    if (amount == 0) {
                                        spawner.setPreciseY(0);
                                    } else {
                                        spawner.setPreciseY(spawner.getPreciseY() + amount);
                                    }
                                    break;
                                case "z":
                                    if (amount == 0) {
                                        spawner.setPreciseZ(0);
                                    } else {
                                        spawner.setPreciseZ(spawner.getPreciseZ() + amount);
                                    }
                                    break;
                                case "reset":
                                    spawner.setPreciseX(0);
                                    spawner.setPreciseY(0);
                                    spawner.setPreciseZ(0);
                                    break;
                            }
                            
                            spawner.setSpawnMode(Spawner.SpawnMode.PRECISE);
                            plugin.getSpawnerManager().saveSpawner(spawner);
                            String currentGui = openGuis.get(player.getUniqueId());
                            if (currentGui != null) {
                                openGui(player, spawner, currentGui);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
                }
            } else if (action.startsWith("purchase:")) {
                String[] parts = action.substring(9).trim().split(" ");
                if (parts.length >= 2 && parts[0].equals("limit")) {
                    try {
                        int amount = Integer.parseInt(parts[1]);
                        handlePurchaseLimit(player, spawner.getType(), amount);
                        String currentGui = openGuis.get(player.getUniqueId());
                        if (currentGui != null) {
                            openGui(player, spawner, currentGui);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
    
    private void handlePurchaseLimit(Player player, SpawnerType type, int amount) {
        double costPerSlot = plugin.getConfig().getDouble("limits." + type.name().toLowerCase() + ".buy_cost_per_slot", 1000.0);
        double totalCost = costPerSlot * amount;
        EconomyManager economy = plugin.getEconomyManager();
        
        if (economy == null || !economy.isEnabled()) {
            return;
        }
        
        if (!economy.has(player, totalCost)) {
            player.sendMessage(plugin.getConfigManager().getMessage("error.insufficient_funds"));
            return;
        }
        
        economy.withdraw(player, totalCost);
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            data.addPurchasedLimit(type, amount);
            plugin.getDataManager().savePlayerData(data);
            player.sendMessage(plugin.getConfigManager().getMessage("success.limit_purchased"));
        }
    }
    
    private void openGui(Player player, Spawner spawner, String guiName) {
        switch (guiName) {
            case "main_menu":
                openMainMenu(player, spawner);
                break;
            case "upgrade_menu":
                openUpgradeMenu(player, spawner);
                break;
            case "entity_menu":
                openEntityMenu(player, spawner);
                break;
            case "buy_limit_menu":
                openBuyLimitMenu(player, spawner);
                break;
            case "precise_pos_menu":
                openPrecisePosMenu(player, spawner);
                break;
        }
    }
}
