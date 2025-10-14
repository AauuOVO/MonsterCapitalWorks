package org.mcw.monstercapitalworks.command;

import org.mcw.monstercapitalworks.model.Spawner;
import org.mcw.monstercapitalworks.model.SpawnerType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 主命令处理器
 */
public class MCWCommand implements CommandExecutor, TabCompleter {
    
    private final org.mcw.monstercapitalworks.MonsterCapitalWorks plugin;
    
    public MCWCommand(org.mcw.monstercapitalworks.MonsterCapitalWorks plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                sendHelp(sender);
                return true;
                
            case "reload":
                return handleReload(sender);
                
            case "give":
                return handleGive(sender, args);
                
            case "info":
                return handleInfo(sender);
                
            case "list":
                return handleList(sender, args);
                
            case "remove":
                return handleRemove(sender);
                
            case "limit":
                return handleLimit(sender, args);
                
            default:
                sender.sendMessage(plugin.getConfigManager().getMessage("error.unknown_command"));
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== Monster Capital Works ===");
        sender.sendMessage("§e/mcw help §7- 显示帮助信息");
        sender.sendMessage("§e/mcw reload §7- 重载配置文件");
        sender.sendMessage("§e/mcw give <玩家> <类型> [实体] §7- 给予刷怪笼");
        sender.sendMessage("§e/mcw info §7- 查看刷怪笼信息");
        sender.sendMessage("§e/mcw list [玩家] §7- 列出刷怪笼详情");
        sender.sendMessage("§e/mcw remove §7- 移除刷怪笼");
        if (sender.hasPermission("mcw.admin")) {
            sender.sendMessage("§6§l=== 管理员命令 ===");
            sender.sendMessage("§e/mcw limit set <玩家> <类型> <数量> §7- 设置玩家额外限制");
            sender.sendMessage("§e/mcw limit add <玩家> <类型> <数量> §7- 增加玩家额外限制");
            sender.sendMessage("§e/mcw limit remove <玩家> <类型> <数量> §7- 减少玩家额外限制");
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("mcw.admin.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
            return true;
        }
        
        plugin.reloadConfigs();
        sender.sendMessage(plugin.getConfigManager().getMessage("success.reloaded"));
        return true;
    }
    
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mcw.admin.give")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§c用法: /mcw give <玩家> <类型> [实体]");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.player_not_found"));
            return true;
        }
        
        SpawnerType type;
        try {
            type = SpawnerType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c无效类型！");
            return true;
        }
        
        // 获取实体类型，如果未指定则使用配置文件中的默认值
        EntityType entityType;
        if (args.length >= 4) {
            try {
                entityType = EntityType.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§c无效实体！");
                return true;
            }
        } else {
            // 从配置文件读取默认实体
            String defaultEntityStr = plugin.getConfig().getString("default_entities." + type.name().toLowerCase(), "ZOMBIE");
            try {
                entityType = EntityType.valueOf(defaultEntityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                entityType = EntityType.ZOMBIE; // 如果配置错误，使用僵尸作为后备
                plugin.getLogger().warning("配置文件中的默认实体 " + defaultEntityStr + " 无效，使用 ZOMBIE 作为默认值");
            }
        }
        
        org.bukkit.inventory.ItemStack spawner = new org.bukkit.inventory.ItemStack(Material.SPAWNER);
        org.bukkit.inventory.meta.ItemMeta meta = spawner.getItemMeta();
        if (meta != null) {
            // 获取自定义名称
            String displayName = getSpawnerDisplayName(type, entityType);
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7类型: §e" + type.name());
            lore.add("§7实体: §e" + entityType.name());
            meta.setLore(lore);
            spawner.setItemMeta(meta);
        }
        
        target.getInventory().addItem(spawner);
        sender.sendMessage("§a已给予 " + target.getName() + " 刷怪笼！");
        return true;
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
    
    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        Block target = player.getTargetBlock(null, 5);
        
        if (target.getType() != Material.SPAWNER) {
            sender.sendMessage("§c请看向刷怪笼！");
            return true;
        }
        
        Location loc = target.getLocation();
        Spawner spawner = plugin.getSpawnerManager().getSpawner(loc);
        
        if (spawner == null) {
            sender.sendMessage("§c非MCW刷怪笼！");
            return true;
        }
        
        // 使用新的列表消息格式
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%type%", spawner.getType().name());
        placeholders.put("%entity%", spawner.getEntityType().name());
        placeholders.put("%owner%", plugin.getServer().getOfflinePlayer(spawner.getOwner()).getName());
        placeholders.put("%location%", formatLocation(loc));
        placeholders.put("%delay%", String.valueOf(spawner.getSpawnDelay()));
        placeholders.put("%count%", String.valueOf(spawner.getSpawnCount()));
        placeholders.put("%range%", String.valueOf(spawner.getActivationRange()));
        placeholders.put("%stored%", String.valueOf(spawner.getStoredSpawns()));
        placeholders.put("%max_storage%", String.valueOf(spawner.getMaxStorage()));
        
        List<String> messages = plugin.getConfigManager().getMessageList("info", placeholders);
        for (String message : messages) {
            sender.sendMessage(message);
        }
        
        return true;
    }
    
    private boolean handleList(CommandSender sender, String[] args) {
        Player target;
        
        if (args.length >= 2) {
            if (!sender.hasPermission("mcw.admin.list.others")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
                return true;
            }
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("error.player_not_found"));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("error.player_only"));
                return true;
            }
            target = (Player) sender;
        }
        
        List<Spawner> spawners = plugin.getSpawnerManager().getPlayerSpawners(target.getUniqueId());
        
        // 按类型统计
        Map<SpawnerType, List<Spawner>> spawnersByType = new HashMap<>();
        for (Spawner spawner : spawners) {
            spawnersByType.computeIfAbsent(spawner.getType(), k -> new ArrayList<>()).add(spawner);
        }
        
        // 显示标题
        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("%player%", target.getName());
        sender.sendMessage(plugin.getConfigManager().getMessage("list.header", headerPlaceholders));
        
        // 显示每种类型的统计和刷怪笼
        for (SpawnerType type : SpawnerType.values()) {
            List<Spawner> typeSpawners = spawnersByType.getOrDefault(type, new ArrayList<>());
            int placed = typeSpawners.size();
            int limit = plugin.getPermissionManager().getSpawnerLimit(target, type);
            
            // 显示类型标题和统计
            Map<String, String> typePlaceholders = new HashMap<>();
            typePlaceholders.put("%type%", type.name());
            typePlaceholders.put("%count%", String.valueOf(placed));
            typePlaceholders.put("%limit%", String.valueOf(limit));
            sender.sendMessage(plugin.getConfigManager().getMessage("list.type_header", typePlaceholders));
            
            // 显示每个刷怪笼的位置
            if (typeSpawners.isEmpty()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("list.no_spawners", new HashMap<>()));
            } else {
                for (Spawner spawner : typeSpawners) {
                    Location loc = spawner.getLocation();
                    Map<String, String> locPlaceholders = new HashMap<>();
                    locPlaceholders.put("%world%", loc.getWorld().getName());
                    locPlaceholders.put("%x%", String.valueOf(loc.getBlockX()));
                    locPlaceholders.put("%y%", String.valueOf(loc.getBlockY()));
                    locPlaceholders.put("%z%", String.valueOf(loc.getBlockZ()));
                    locPlaceholders.put("%entity%", spawner.getEntityType().name());
                    sender.sendMessage(plugin.getConfigManager().getMessage("list.spawner_location", locPlaceholders));
                }
            }
            sender.sendMessage(""); // 空行分隔
        }
        
        return true;
    }
    
    private boolean handleRemove(CommandSender sender) {
        if (!sender.hasPermission("mcw.admin.remove")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        Block target = player.getTargetBlock(null, 5);
        
        if (target.getType() != Material.SPAWNER) {
            sender.sendMessage("§c请看向刷怪笼！");
            return true;
        }
        
        Location loc = target.getLocation();
        Spawner spawner = plugin.getSpawnerManager().getSpawner(loc);
        
        if (spawner == null) {
            sender.sendMessage("§c非MCW刷怪笼！");
            return true;
        }
        
        plugin.getSpawnerManager().removeSpawner(loc);
        target.setType(Material.AIR);
        sender.sendMessage(plugin.getConfigManager().getMessage("success.removed_spawner"));
        return true;
    }
    
    private boolean handleLimit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mcw.admin.limit")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
            return true;
        }
        
        if (args.length < 5) {
            sender.sendMessage("§c用法: /mcw limit <set/add/remove> <玩家> <类型> <数量>");
            return true;
        }
        
        String action = args[1].toLowerCase();
        if (!action.equals("set") && !action.equals("add") && !action.equals("remove")) {
            sender.sendMessage("§c无效的操作！请使用 set、add 或 remove");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.player_not_found"));
            return true;
        }
        
        SpawnerType type;
        try {
            type = SpawnerType.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c无效类型！请使用 NORMAL 或 PREMIUM");
            return true;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[4]);
            if (action.equals("set") && amount < 0) {
                sender.sendMessage(plugin.getConfigManager().getMessage("error.invalid_amount"));
                return true;
            }
            if ((action.equals("add") || action.equals("remove")) && amount <= 0) {
                sender.sendMessage(plugin.getConfigManager().getMessage("error.invalid_amount"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.invalid_amount"));
            return true;
        }
        
        // 根据操作类型执行相应的逻辑
        String typeKey = type.name().toLowerCase();
        int maxPurchasable = plugin.getConfigManager().getLimits().getInt(typeKey + ".purchase.max_purchasable", 50);
        int currentLimit = plugin.getDataManager().getPlayerData(target.getUniqueId()).getPurchasedLimit(type);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", target.getName());
        placeholders.put("%type%", type.name());
        placeholders.put("%amount%", String.valueOf(amount));
        
        switch (action) {
            case "set":
                // 检查是否超过最大可购买数量
                if (amount > maxPurchasable) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("%max%", String.valueOf(maxPurchasable));
                    sender.sendMessage(plugin.getConfigManager().getMessage("error.cannot_exceed_max", errorPlaceholders));
                    return true;
                }
                
                // 设置限制
                plugin.getDataManager().getPlayerData(target.getUniqueId()).setPurchasedLimit(type, amount);
                plugin.getDataManager().savePlayerData(plugin.getDataManager().getPlayerData(target.getUniqueId()));
                sender.sendMessage(plugin.getConfigManager().getMessage("success.limit_set", placeholders));
                break;
                
            case "add":
                int newLimitAdd = currentLimit + amount;
                
                // 检查是否超过最大可购买数量
                if (newLimitAdd > maxPurchasable) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("%max%", String.valueOf(maxPurchasable));
                    sender.sendMessage(plugin.getConfigManager().getMessage("error.cannot_exceed_max", errorPlaceholders));
                    return true;
                }
                
                // 增加限制
                plugin.getDataManager().getPlayerData(target.getUniqueId()).addPurchasedLimit(type, amount);
                plugin.getDataManager().savePlayerData(plugin.getDataManager().getPlayerData(target.getUniqueId()));
                sender.sendMessage(plugin.getConfigManager().getMessage("success.limit_added", placeholders));
                break;
                
            case "remove":
                // 检查当前限制是否为0
                if (currentLimit == 0) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("%type%", type.name());
                    sender.sendMessage(plugin.getConfigManager().getMessage("error.no_limit_to_remove", errorPlaceholders));
                    return true;
                }
                
                // 计算实际可以减少的数量
                int actualRemoveAmount = Math.min(amount, currentLimit);
                int newLimitRemove = currentLimit - actualRemoveAmount;
                
                // 更新限制
                plugin.getDataManager().getPlayerData(target.getUniqueId()).setPurchasedLimit(type, newLimitRemove);
                plugin.getDataManager().savePlayerData(plugin.getDataManager().getPlayerData(target.getUniqueId()));
                
                // 根据实际删除的数量显示不同的消息
                Map<String, String> removePlaceholders = new HashMap<>();
                removePlaceholders.put("%player%", target.getName());
                removePlaceholders.put("%type%", type.name());
                removePlaceholders.put("%amount%", String.valueOf(actualRemoveAmount));
                removePlaceholders.put("%current%", String.valueOf(currentLimit));
                removePlaceholders.put("%new%", String.valueOf(newLimitRemove));
                
                if (actualRemoveAmount < amount) {
                    // 实际删除的数量少于请求的数量
                    sender.sendMessage(plugin.getConfigManager().getMessage("success.limit_removed_partial", removePlaceholders));
                } else {
                    // 完全按照请求的数量删除
                    sender.sendMessage(plugin.getConfigManager().getMessage("success.limit_removed", removePlaceholders));
                }
                break;
        }
        
        return true;
    }
    
    private String formatLocation(Location loc) {
        return String.format("%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "reload", "give", "info", "list", "remove"));
            if (sender.hasPermission("mcw.admin")) {
                completions.add("limit");
            }
        } else if (args.length == 2) {
            String cmd = args[0].toLowerCase();
            if (cmd.equals("give") || cmd.equals("list")) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (cmd.equals("limit")) {
                completions.addAll(Arrays.asList("set", "add", "remove"));
            }
        } else if (args.length == 3) {
            String cmd = args[0].toLowerCase();
            if (cmd.equals("give")) {
                completions.addAll(Arrays.asList("normal", "premium"));
            } else if (cmd.equals("limit")) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 4) {
            String cmd = args[0].toLowerCase();
            if (cmd.equals("give")) {
                for (EntityType type : EntityType.values()) {
                    if (type.isSpawnable() && type.isAlive()) {
                        completions.add(type.name().toLowerCase());
                    }
                }
            } else if (cmd.equals("limit")) {
                completions.addAll(Arrays.asList("normal", "premium"));
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("limit")) {
                completions.addAll(Arrays.asList("1", "5", "10", "20", "50"));
            }
        }
        
        // 过滤匹配的补全
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        
        return completions;
    }
}
