package org.mcw.monstercapitalworks.manager;

import org.mcw.monstercapitalworks.model.PlayerData;
import org.mcw.monstercapitalworks.model.Spawner;
import org.mcw.monstercapitalworks.model.SpawnerType;
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
    
    private final org.mcw.monstercapitalworks.MonsterCapitalWorks plugin;
    private final Map<UUID, String> openGuis;
    private final Map<UUID, Spawner> selectedSpawners;
    
    public GUIManager(org.mcw.monstercapitalworks.MonsterCapitalWorks plugin) {
        this.plugin = plugin;
        this.openGuis = new HashMap<>();
        this.selectedSpawners = new HashMap<>();
    }
    
    public void initialize() {
    }
    
    public void openMainMenu(Player player, Spawner spawner) {
        // 根据刷怪笼类型选择不同的菜单
        String menuName = spawner.getType() == SpawnerType.NORMAL ? "main_menu_normal" : "main_menu_premium";
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig(menuName);
        if (guiConfig == null) {
            return;
        }
        
        String title = colorize(guiConfig.getString("title", "刷怪笼控制面板"));
        int size = guiConfig.getInt("size", 27);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            loadLayoutBasedGui(inv, guiConfig, player, spawner);
        } else {
            // 兼容旧的 items 格式
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
        }
        
        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), menuName);
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
        
        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            loadLayoutBasedGui(inv, guiConfig, player, spawner);
        } else {
            // 兼容旧的 items 格式
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
        
        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            loadLayoutBasedGui(inv, guiConfig, player, spawner);
        } else {
            // 兼容旧的 items 格式
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
        
        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            loadLayoutBasedGui(inv, guiConfig, player, spawner);
        } else {
            // 兼容旧的 items 格式
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
        }
        
        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), "precise_pos_menu");
        selectedSpawners.put(player.getUniqueId(), spawner);
    }
    
    private void loadLayoutBasedGui(Inventory inv, FileConfiguration guiConfig, Player player, Spawner spawner) {
        List<String> layout = guiConfig.getStringList("layout");
        ConfigurationSection iconsSection = guiConfig.getConfigurationSection("icons");
        
        if (layout.isEmpty() || iconsSection == null) {
            return;
        }
        
        int slot = 0;
        for (String row : layout) {
            for (char iconChar : row.toCharArray()) {
                String iconKey = String.valueOf(iconChar);
                ConfigurationSection iconSection = iconsSection.getConfigurationSection(iconKey);
                
                if (iconSection != null) {
                    ItemStack item = createGuiItem(iconSection, player, spawner);
                    inv.setItem(slot, item);
                }
                slot++;
            }
        }
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
            // 优先使用刷怪蛋自定义名称，如果没有则使用实体配置中的名称
            String customEggName = plugin.getConfigManager().getSpawnEggCustomName(entityType.name());
            String name;
            if (customEggName != null) {
                name = customEggName;
            } else {
                name = section.getString("name", entityType.name());
                name = colorize(name);
            }
            meta.setDisplayName(name);
            
            // 获取价格和解锁状态
            double price = section.getDouble("price", 0);
            boolean requireUnlock = section.getBoolean("require_unlock", true);
            boolean enabled = section.getBoolean("enabled", true);
            
            // 检查玩家是否已解锁
            boolean unlocked = !requireUnlock || hasUnlockedEntity(player, entityType);
            
            List<String> lore = section.getStringList("lore");
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                // 过滤掉包含"用刷怪蛋交互时"的lore行（隐藏flag信息）
                if (!line.contains("用刷怪蛋交互时") && !line.contains("spawn_egg_interact")) {
                    processedLore.add(colorize(line));
                }
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
            player.sendMessage("§c该生物已被禁用！");
            return;
        }
        
        // 检查spawn_conditions（Y坐标条件）- 只有在没有bypass权限时才检查
        if (!player.hasPermission("mcw.bypass.conditions")) {
            if (entityConfig.contains("spawn_conditions")) {
                org.bukkit.configuration.ConfigurationSection spawnConditions = 
                    entityConfig.getConfigurationSection("spawn_conditions");
                
                if (!org.mcw.monstercapitalworks.util.ConditionChecker.checkYConditions(
                        spawner.getLocation(), spawnConditions)) {
                    player.sendMessage("§c该生物不满足当前刷怪笼位置的Y坐标条件！");
                    
                    // 提供详细的错误信息
                    if (spawnConditions.contains("min_y")) {
                        int minY = spawnConditions.getInt("min_y");
                        player.sendMessage("§7需要: Y坐标 >= §e" + minY);
                    }
                    if (spawnConditions.contains("max_y")) {
                        int maxY = spawnConditions.getInt("max_y");
                        player.sendMessage("§7需要: Y坐标 <= §e" + maxY);
                    }
                    player.sendMessage("§7当前刷怪笼Y坐标: §e" + spawner.getLocation().getBlockY());
                    return;
                }
            }
        }
        
        // 检查spawn_condition条件（PAPI条件）
        if (entityConfig.contains("spawn_condition")) {
            String condition = entityConfig.getString("spawn_condition");
            if (!checkSpawnCondition(player, spawner.getLocation(), condition)) {
                player.sendMessage("§c当前环境不满足生成条件！");
                return;
            }
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
            player.sendMessage("§a已切换刷怪笼生物类型！");
        }
    }
    
    /**
     * 检查生成条件
     * @param player 玩家
     * @param location 刷怪笼位置
     * @param condition 条件字符串
     * @return 是否满足条件
     */
    private boolean checkSpawnCondition(Player player, Location location, String condition) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        
        // 替换PlaceholderAPI占位符
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            condition = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, condition);
            
            // 如果条件中包含刷怪笼位置相关的占位符，需要特殊处理
            // 例如：%world_biome% 需要在刷怪笼位置检查
            if (condition.contains("%world_biome%") || condition.contains("%biome%")) {
                // 获取刷怪笼位置的生物群系
                String biome = location.getBlock().getBiome().getKey().getKey().toLowerCase();
                condition = condition.replace("%world_biome%", biome);
                condition = condition.replace("%biome%", biome);
            }
            
            if (condition.contains("%world_name%")) {
                String worldName = location.getWorld().getName();
                condition = condition.replace("%world_name%", worldName);
            }
        }
        
        // 评估条件表达式
        return evaluateCondition(condition);
    }
    
    /**
     * 评估条件表达式
     * @param condition 条件字符串
     * @return 是否满足条件
     */
    private boolean evaluateCondition(String condition) {
        try {
            // 处理常见的比较运算符
            condition = condition.trim();
            
            // 处理 == 运算符
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();
                    return left.equalsIgnoreCase(right);
                }
            }
            
            // 处理 != 运算符
            if (condition.contains("!=")) {
                String[] parts = condition.split("!=");
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();
                    return !left.equalsIgnoreCase(right);
                }
            }
            
            // 处理 > 运算符
            if (condition.contains(">") && !condition.contains(">=")) {
                String[] parts = condition.split(">");
                if (parts.length == 2) {
                    try {
                        double left = Double.parseDouble(parts[0].trim());
                        double right = Double.parseDouble(parts[1].trim());
                        return left > right;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            
            // 处理 >= 运算符
            if (condition.contains(">=")) {
                String[] parts = condition.split(">=");
                if (parts.length == 2) {
                    try {
                        double left = Double.parseDouble(parts[0].trim());
                        double right = Double.parseDouble(parts[1].trim());
                        return left >= right;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            
            // 处理 < 运算符
            if (condition.contains("<") && !condition.contains("<=")) {
                String[] parts = condition.split("<");
                if (parts.length == 2) {
                    try {
                        double left = Double.parseDouble(parts[0].trim());
                        double right = Double.parseDouble(parts[1].trim());
                        return left < right;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            
            // 处理 <= 运算符
            if (condition.contains("<=")) {
                String[] parts = condition.split("<=");
                if (parts.length == 2) {
                    try {
                        double left = Double.parseDouble(parts[0].trim());
                        double right = Double.parseDouble(parts[1].trim());
                        return left <= right;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            
            // 如果没有运算符，尝试解析为布尔值
            return Boolean.parseBoolean(condition);
            
        } catch (Exception e) {
            plugin.getLogger().warning("无法评估条件: " + condition + " - " + e.getMessage());
            return false;
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
            int permLimit = plugin.getPermissionManager().getPermissionExtraLimit(player, spawner.getType());
            
            text = text.replace("%amc_placed%", String.valueOf(placed));
            text = text.replace("%amc_limit%", String.valueOf(limit));
            text = text.replace("%amc_purchased%", String.valueOf(purchased));
            text = text.replace("%amc_base_limit%", String.valueOf(baseLimit));
            text = text.replace("%amc_perm_limit%", String.valueOf(permLimit));
            
            // 购买费用占位符（使用新的计算方法）
            text = text.replace("%amc_buy_1_cost%", String.format("%.2f", calculatePurchaseCostForDisplay(player, spawner.getType(), 1)));
            text = text.replace("%amc_buy_5_cost%", String.format("%.2f", calculatePurchaseCostForDisplay(player, spawner.getType(), 5)));
            text = text.replace("%amc_buy_10_cost%", String.format("%.2f", calculatePurchaseCostForDisplay(player, spawner.getType(), 10)));
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
        
        // 处理 RGB 颜色格式 {#RRGGBB}
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
        java.util.regex.Matcher matcher = hexPattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            
            // 将每个十六进制字符转换为 §x§r§r§g§g§b§b 格式
            for (char c : hexColor.toCharArray()) {
                replacement.append("§").append(c);
            }
            
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        
        // 处理传统颜色代码 &
        return buffer.toString().replace('&', '§');
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
        
        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            List<String> layout = guiConfig.getStringList("layout");
            ConfigurationSection iconsSection = guiConfig.getConfigurationSection("icons");
            
            if (!layout.isEmpty() && iconsSection != null) {
                int currentSlot = 0;
                for (String row : layout) {
                    for (char iconChar : row.toCharArray()) {
                        if (currentSlot == slot) {
                            String iconKey = String.valueOf(iconChar);
                            ConfigurationSection iconSection = iconsSection.getConfigurationSection(iconKey);
                            
                            if (iconSection != null) {
                                List<String> actions;
                                if (isRightClick && iconSection.contains("right_click_actions")) {
                                    actions = iconSection.getStringList("right_click_actions");
                                } else {
                                    actions = iconSection.getStringList("actions");
                                }
                                executeActions(player, spawner, actions, isRightClick, isShiftClick);
                            }
                            return;
                        }
                        currentSlot++;
                    }
                }
            }
        } else {
            // 兼容旧的 items 格式
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
    }
    
    private void executeActions(Player player, Spawner spawner, List<String> actions, boolean isRightClick, boolean isShiftClick) {
        for (String action : actions) {
            if (action.startsWith("open_gui:")) {
                String guiName = action.substring(9).trim();
                openGui(player, spawner, guiName);
            } else if (action.equals("close")) {
                player.closeInventory();
            } else if (action.startsWith("command:")) {
                // 玩家身份执行命令
                String command = action.substring(8).trim();
                executePlayerCommand(player, command);
            } else if (action.startsWith("op:")) {
                // OP身份执行命令（临时授予OP）
                String command = action.substring(3).trim();
                executeOpCommand(player, command);
            } else if (action.startsWith("console:")) {
                // 控制台执行命令
                String command = action.substring(8).trim();
                executeConsoleCommand(player, command);
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
                if (player.hasPermission("mcw.spawnmode.precise")) {
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
                if (player.hasPermission("mcw.spawnmode.precise")) {
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
                if (player.hasPermission("mcw.spawnmode.precise")) {
                    String[] parts = action.substring(11).trim().split(" ");
                    if (parts.length >= 2) {
                        try {
                            String axis = parts[0];
                            double amount = Double.parseDouble(parts[1]);
                            int activationRange = spawner.getActivationRange();
                            boolean validPosition = true;
                            
                            switch (axis.toLowerCase()) {
                                case "x":
                                    if (amount == 0) {
                                        spawner.setPreciseX(0);
                                    } else {
                                        double newX = spawner.getPreciseX() + amount;
                                        if (Math.abs(newX) <= activationRange) {
                                            spawner.setPreciseX(newX);
                                        } else {
                                            validPosition = false;
                                            player.sendMessage("§c错误: X轴偏移不能超过激活范围 ±" + activationRange + " 格");
                                        }
                                    }
                                    break;
                                case "y":
                                    if (amount == 0) {
                                        spawner.setPreciseY(0);
                                    } else {
                                        double newY = spawner.getPreciseY() + amount;
                                        if (Math.abs(newY) <= activationRange) {
                                            spawner.setPreciseY(newY);
                                        } else {
                                            validPosition = false;
                                            player.sendMessage("§c错误: Y轴偏移不能超过激活范围 ±" + activationRange + " 格");
                                        }
                                    }
                                    break;
                                case "z":
                                    if (amount == 0) {
                                        spawner.setPreciseZ(0);
                                    } else {
                                        double newZ = spawner.getPreciseZ() + amount;
                                        if (Math.abs(newZ) <= activationRange) {
                                            spawner.setPreciseZ(newZ);
                                        } else {
                                            validPosition = false;
                                            player.sendMessage("§c错误: Z轴偏移不能超过激活范围 ±" + activationRange + " 格");
                                        }
                                    }
                                    break;
                                case "reset":
                                    spawner.setPreciseX(0);
                                    spawner.setPreciseY(0);
                                    spawner.setPreciseZ(0);
                                    break;
                            }
                            
                            if (validPosition) {
                                spawner.setSpawnMode(Spawner.SpawnMode.PRECISE);
                                plugin.getSpawnerManager().saveSpawner(spawner);
                            }
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
        String typePath = "limits." + type.name().toLowerCase();
        
        // 检查是否启用购买
        if (!plugin.getConfig().getBoolean(typePath + ".purchase.enabled", true)) {
            player.sendMessage("§c该类型刷怪笼不支持购买额外位置！");
            return;
        }
        
        // 获取玩家当前已购买数量
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return;
        }
        
        int currentPurchased = data.getPurchasedLimit(type);
        int maxPurchasable = plugin.getConfig().getInt(typePath + ".purchase.max_purchasable", 50);
        
        // 检查是否超过最大可购买数量
        if (currentPurchased + amount > maxPurchasable) {
            player.sendMessage("§c购买失败！最多只能购买 " + maxPurchasable + " 个额外位置，你已购买 " + currentPurchased + " 个。");
            return;
        }
        
        // 计算总费用
        double totalCost = calculatePurchaseCost(type, currentPurchased, amount);
        
        EconomyManager economy = plugin.getEconomyManager();
        if (economy == null || !economy.isEnabled()) {
            return;
        }
        
        if (!economy.has(player, totalCost)) {
            player.sendMessage(plugin.getConfigManager().getMessage("error.insufficient_funds"));
            return;
        }
        
        // 扣款并增加限制
        economy.withdraw(player, totalCost);
        data.addPurchasedLimit(type, amount);
        plugin.getDataManager().savePlayerData(data);
        
        player.sendMessage("§a成功购买 " + amount + " 个额外位置！花费: §e" + String.format("%.2f", totalCost));
    }
    
    /**
     * 计算购买费用
     * @param type 刷怪笼类型
     * @param currentPurchased 当前已购买数量
     * @param amount 要购买的数量
     * @return 总费用
     */
    private double calculatePurchaseCost(SpawnerType type, int currentPurchased, int amount) {
        String typePath = "limits." + type.name().toLowerCase() + ".purchase";
        
        double basePrice = plugin.getConfig().getDouble(typePath + ".base_price", 1000.0);
        String priceMode = plugin.getConfig().getString(typePath + ".price_mode", "multiplier");
        double multiplier = plugin.getConfig().getDouble(typePath + ".price_multiplier", 1.2);
        
        double totalCost = 0.0;
        
        if (priceMode.equalsIgnoreCase("fixed")) {
            // 固定价格模式：每个位置价格相同
            totalCost = basePrice * amount;
        } else {
            // 倍率递增模式：每次购买后价格递增
            for (int i = 0; i < amount; i++) {
                // 计算第 (currentPurchased + i + 1) 个位置的价格
                // 公式: basePrice * (multiplier ^ currentPurchased)
                double price = basePrice * Math.pow(multiplier, currentPurchased + i);
                totalCost += price;
            }
        }
        
        return totalCost;
    }
    
    /**
     * 计算购买指定数量的费用（用于占位符显示）
     * @param player 玩家
     * @param type 刷怪笼类型
     * @param amount 要购买的数量
     * @return 费用
     */
    private double calculatePurchaseCostForDisplay(Player player, SpawnerType type, int amount) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return 0.0;
        }
        
        int currentPurchased = data.getPurchasedLimit(type);
        return calculatePurchaseCost(type, currentPurchased, amount);
    }
    
    private void openGui(Player player, Spawner spawner, String guiName) {
        switch (guiName) {
            case "main_menu":
            case "main_menu_normal":
            case "main_menu_premium":
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
    
    /**
     * 以玩家身份执行命令
     * @param player 玩家
     * @param command 命令（不带斜杠）
     */
    private void executePlayerCommand(Player player, String command) {
        if (command == null || command.isEmpty()) {
            return;
        }
        
        try {
            // 替换占位符
            final String finalCommand = replacePlaceholders(command, player, selectedSpawners.get(player.getUniqueId()));
            
            // 在主线程执行命令
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.performCommand(finalCommand);
            });
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("玩家 " + player.getName() + " 执行命令: /" + finalCommand);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("执行玩家命令时出错: " + command);
            plugin.getLogger().warning(e.getMessage());
        }
    }
    
    /**
     * 以OP身份执行命令（临时授予OP权限）
     * 注意：此方法会临时授予玩家OP权限，执行命令后立即撤销
     * 建议优先使用console命令方式，此方式仅在必要时使用
     * @param player 玩家
     * @param command 命令（不带斜杠）
     */
    private void executeOpCommand(Player player, String command) {
        if (command == null || command.isEmpty()) {
            return;
        }
        
        try {
            // 替换占位符
            final String finalCommand = replacePlaceholders(command, player, selectedSpawners.get(player.getUniqueId()));
            
            // 在主线程执行
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean wasOp = player.isOp();
                
                try {
                    // 临时授予OP权限
                    if (!wasOp) {
                        player.setOp(true);
                    }
                    
                    // 执行命令
                    player.performCommand(finalCommand);
                    
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("玩家 " + player.getName() + " 以OP身份执行命令: /" + finalCommand);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("以OP身份执行命令时出错: " + finalCommand);
                    plugin.getLogger().warning(e.getMessage());
                } finally {
                    // 确保撤销OP权限（如果玩家原本不是OP）
                    if (!wasOp && player.isOp()) {
                        player.setOp(false);
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("准备OP命令时出错: " + command);
            plugin.getLogger().warning(e.getMessage());
        }
    }
    
    /**
     * 以控制台身份执行命令
     * 这是最安全和推荐的命令执行方式
     * @param player 玩家（用于占位符替换）
     * @param command 命令（不带斜杠）
     */
    private void executeConsoleCommand(Player player, String command) {
        if (command == null || command.isEmpty()) {
            return;
        }
        
        try {
            // 替换占位符
            String processedCommand = replacePlaceholders(command, player, selectedSpawners.get(player.getUniqueId()));
            
            // 替换玩家名称占位符
            processedCommand = processedCommand.replace("%player%", player.getName());
            processedCommand = processedCommand.replace("%player_name%", player.getName());
            processedCommand = processedCommand.replace("%player_uuid%", player.getUniqueId().toString());
            
            final String finalCommand = processedCommand;
            
            // 在主线程以控制台身份执行命令
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                    
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("控制台执行命令: /" + finalCommand);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("控制台执行命令时出错: " + finalCommand);
                    plugin.getLogger().warning(e.getMessage());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("准备控制台命令时出错: " + command);
            plugin.getLogger().warning(e.getMessage());
        }
    }
}
