package org.Aauu.aauuMobCapital.command;

import org.Aauu.aauuMobCapital.AauuMobCapital;
import org.Aauu.aauuMobCapital.model.Spawner;
import org.Aauu.aauuMobCapital.model.SpawnerType;
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
public class AMCCommand implements CommandExecutor, TabCompleter {
    
    private final AauuMobCapital plugin;
    
    public AMCCommand(AauuMobCapital plugin) {
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
                
            default:
                sender.sendMessage(plugin.getConfigManager().getMessage("error.unknown_command"));
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== Aauu Mob Capital ===");
        sender.sendMessage("§e/amc help §7- 帮助");
        sender.sendMessage("§e/amc reload §7- 重载配置");
        sender.sendMessage("§e/amc give <玩家> <类型> [实体] §7- 给予刷怪笼");
        sender.sendMessage("§e/amc info §7- 查看刷怪笼信息");
        sender.sendMessage("§e/amc list [玩家] §7- 列出刷怪笼");
        sender.sendMessage("§e/amc remove §7- 移除刷怪笼");
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("amc.admin.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
            return true;
        }
        
        plugin.reloadConfigs();
        sender.sendMessage(plugin.getConfigManager().getMessage("success.reloaded"));
        return true;
    }
    
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amc.admin.give")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§c用法: /amc give <玩家> <类型> [实体]");
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
     * @return 显示名称
     */
    private String getSpawnerDisplayName(SpawnerType type, EntityType entityType) {
        // 检查是否启用自定义名称
        if (!plugin.getConfig().getBoolean("spawner_names.enabled", true)) {
            return "§6" + type.name() + " 刷怪笼";
        }
        
        // 尝试获取自定义名称
        String key = type.name() + "_" + entityType.name();
        String customName = plugin.getConfig().getString("spawner_names.custom_names." + key);
        
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        
        // 使用默认格式
        String defaultFormat = plugin.getConfig().getString("spawner_names.default_format", "§6%type% 刷怪笼");
        return defaultFormat
                .replace("%type%", type.name())
                .replace("%entity%", getEntityDisplayName(entityType));
    }
    
    /**
     * 获取实体的显示名称（中文）
     * @param entityType 实体类型
     * @return 显示名称
     */
    private String getEntityDisplayName(EntityType entityType) {
        switch (entityType) {
            case ZOMBIE: return "僵尸";
            case SKELETON: return "骷髅";
            case CREEPER: return "苦力怕";
            case SPIDER: return "蜘蛛";
            case ENDERMAN: return "末影人";
            case COW: return "奶牛";
            case PIG: return "猪";
            case SHEEP: return "羊";
            case CHICKEN: return "鸡";
            case RABBIT: return "兔子";
            case BLAZE: return "烈焰人";
            case GHAST: return "恶魂";
            case IRON_GOLEM: return "铁傀儡";
            case WOLF: return "狼";
            default: return entityType.name();
        }
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
            sender.sendMessage("§c非AMC刷怪笼！");
            return true;
        }
        
        sender.sendMessage("§6§l=== 刷怪笼信息 ===");
        sender.sendMessage("§7类型: §e" + spawner.getType().name());
        sender.sendMessage("§7实体: §e" + spawner.getEntityType().name());
        sender.sendMessage("§7所有者: §e" + plugin.getServer().getOfflinePlayer(spawner.getOwner()).getName());
        sender.sendMessage("§7位置: §e" + formatLocation(loc));
        sender.sendMessage("§7延迟: §e" + spawner.getSpawnDelay());
        sender.sendMessage("§7数量: §e" + spawner.getSpawnCount());
        sender.sendMessage("§7范围: §e" + spawner.getActivationRange());
        sender.sendMessage("§7存储: §e" + spawner.getStoredSpawns() + "/" + spawner.getMaxStorage());
        return true;
    }
    
    private boolean handleList(CommandSender sender, String[] args) {
        Player target;
        
        if (args.length >= 2) {
            if (!sender.hasPermission("amc.admin.list.others")) {
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
        sender.sendMessage("§6§l=== " + target.getName() + " 的刷怪笼 ===");
        sender.sendMessage("§7总数: §e" + spawners.size());
        
        Map<SpawnerType, Long> typeCounts = new HashMap<>();
        for (Spawner spawner : spawners) {
            typeCounts.put(spawner.getType(), typeCounts.getOrDefault(spawner.getType(), 0L) + 1);
        }
        
        for (Map.Entry<SpawnerType, Long> entry : typeCounts.entrySet()) {
            sender.sendMessage("§7  " + entry.getKey().name() + ": §e" + entry.getValue());
        }
        return true;
    }
    
    private boolean handleRemove(CommandSender sender) {
        if (!sender.hasPermission("amc.admin.remove")) {
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
            sender.sendMessage("§c非AMC刷怪笼！");
            return true;
        }
        
        plugin.getSpawnerManager().removeSpawner(loc);
        target.setType(Material.AIR);
        sender.sendMessage(plugin.getConfigManager().getMessage("success.removed_spawner"));
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
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("list")) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(Arrays.asList("normal", "premium"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            for (EntityType type : EntityType.values()) {
                if (type.isSpawnable() && type.isAlive()) {
                    completions.add(type.name().toLowerCase());
                }
            }
        }
        
        // 过滤匹配的补全
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        
        return completions;
    }
}
