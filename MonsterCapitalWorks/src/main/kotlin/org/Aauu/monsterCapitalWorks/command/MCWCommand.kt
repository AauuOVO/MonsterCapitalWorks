package org.Aauu.monsterCapitalWorks.command

import org.Aauu.monsterCapitalWorks.MonsterCapitalWorks
import org.Aauu.monsterCapitalWorks.model.Spawner
import org.Aauu.monsterCapitalWorks.model.SpawnerType
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class MCWCommand(private val plugin: MonsterCapitalWorks) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        val subCommand = args[0].lowercase()
        
        return when (subCommand) {
            "help" -> {
                sendHelp(sender)
                true
            }
            
            "reload" -> handleReload(sender)
            
            "give" -> handleGive(sender, args)
            
            "info" -> handleInfo(sender)
            
            "list" -> handleList(sender, args)
            
            "remove" -> handleRemove(sender)
            
            "limit" -> handleLimit(sender, args)
            
            else -> {
                sender.sendMessage(plugin.getConfigManager().getMessage("error.unknown_command"))
                true
            }
        }
    }
    
    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== Monster Capital Works ===")
        sender.sendMessage("§e/mcw help §7- 显示帮助信息")
        sender.sendMessage("§e/mcw reload §7- 重载配置文件")
        sender.sendMessage("§e/mcw give <玩家> <类型> [实体] §7- 给予刷怪笼")
        sender.sendMessage("§e/mcw info §7- 查看刷怪笼信息")
        sender.sendMessage("§e/mcw list [玩家] §7- 列出刷怪笼详情")
        sender.sendMessage("§e/mcw remove §7- 移除刷怪笼")
        if (sender.hasPermission("mcw.admin")) {
            sender.sendMessage("§6§l=== 管理员命令 ===")
            sender.sendMessage("§e/mcw limit set <玩家> <类型> <数量> §7- 设置玩家额外限制")
            sender.sendMessage("§e/mcw limit add <玩家> <类型> <数量> §7- 增加玩家额外限制")
            sender.sendMessage("§e/mcw limit remove <玩家> <类型> <数量> §7- 减少玩家额外限制")
        }
    }
    
    private fun handleReload(sender: CommandSender): Boolean {
        if (!sender.hasPermission("mcw.admin.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"))
            return true
        }
        
        plugin.reloadConfigs()
        sender.sendMessage(plugin.getConfigManager().getMessage("success.reloaded"))
        return true
    }
    
    private fun handleGive(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mcw.admin.give")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"))
            return true
        }
        
        if (args.size < 3) {
            sender.sendMessage("§c用法: /mcw give <玩家> <类型> [实体]")
            return true
        }
        
        val target = Bukkit.getPlayer(args[1])
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.player_not_found"))
            return true
        }
        
        val type = try {
            SpawnerType.valueOf(args[2].uppercase())
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§c无效类型！")
            return true
        }
        
        // 获取实体类型，如果未指定则使用配置文件中的默认值
        val entityType = if (args.size >= 4) {
            try {
                EntityType.valueOf(args[3].uppercase())
            } catch (e: IllegalArgumentException) {
                sender.sendMessage("§c无效实体！")
                return true
            }
        } else {
            // 从配置文件读取默认实体
            val defaultEntityStr = plugin.getConfig()?.getString("default_entities.${type.name.lowercase()}", "ZOMBIE") ?: "ZOMBIE"
            try {
                EntityType.valueOf(defaultEntityStr.uppercase())
            } catch (e: IllegalArgumentException) {
                EntityType.ZOMBIE // 如果配置错误，使用僵尸作为后备
            }
        }
        
        val spawner = ItemStack(Material.SPAWNER)
        val meta = spawner.itemMeta
        if (meta != null) {
            // 获取自定义名称
            val displayName = getSpawnerDisplayName(type, entityType)
            meta.setDisplayName(displayName)
            
            val lore = mutableListOf<String>()
            lore.add("§7类型: §e${type.name}")
            lore.add("§7实体: §e${entityType.name}")
            meta.lore = lore
            spawner.itemMeta = meta
        }
        
        target.inventory.addItem(spawner)
        sender.sendMessage("§a已给予 ${target.name} 刷怪笼！")
        return true
    }
    
    /**
     * 获取刷怪笼的显示名称
     */
    private fun getSpawnerDisplayName(type: SpawnerType, entityType: EntityType): String {
        // 使用ConfigManager获取自定义名称（已包含颜色代码处理）
        val customName = plugin.getConfigManager().getSpawnerCustomName(type.name, entityType.name)
        
        if (customName != null) {
            return customName
        }
        
        // 如果ConfigManager返回null，使用简单的默认格式
        return plugin.getConfigManager().colorize("§6${type.name} 刷怪笼")
    }
    
    private fun handleInfo(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.player_only"))
            return true
        }
        
        val target = sender.getTargetBlock(null, 5)
        
        if (target.type != Material.SPAWNER) {
            sender.sendMessage("§c请看向刷怪笼！")
            return true
        }
        
        val loc = target.location
        val spawner = plugin.getSpawnerManager().getSpawner(loc)
        
        if (spawner == null) {
            sender.sendMessage("§c非MCW刷怪笼！")
            return true
        }
        
        // 使用新的列表消息格式
        val placeholders = mutableMapOf<String, String>()
        placeholders["%type%"] = spawner.type.name
        placeholders["%entity%"] = spawner.entityType.name
        placeholders["%owner%"] = Bukkit.getOfflinePlayer(spawner.owner).name ?: "未知"
        placeholders["%location%"] = formatLocation(loc)
        placeholders["%delay%"] = spawner.spawnDelay.toString()
        placeholders["%count%"] = spawner.spawnCount.toString()
        placeholders["%range%"] = spawner.activationRange.toString()
        placeholders["%stored%"] = spawner.storedSpawns.toString()
        placeholders["%max_storage%"] = spawner.maxStorage.toString()
        
        val messages = plugin.getConfigManager().getMessageList("info", placeholders)
        for (message in messages) {
            sender.sendMessage(message)
        }
        
        return true
    }
    
    private fun handleList(sender: CommandSender, args: Array<out String>): Boolean {
        val target = if (args.size >= 2) {
            if (!sender.hasPermission("mcw.admin.list.others")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"))
                return true
            }
            Bukkit.getPlayer(args[1])
        } else {
            if (sender !is Player) {
                sender.sendMessage(plugin.getConfigManager().getMessage("error.player_only"))
                return true
            }
            sender
        }
        
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.player_not_found"))
            return true
        }
        
        val spawners = plugin.getSpawnerManager().getPlayerSpawners(target.uniqueId)
        
        // 按类型统计
        val spawnersByType = spawners.groupBy { it.type }
        
        // 显示标题
        val headerPlaceholders = mapOf("%player%" to target.name)
        sender.sendMessage(plugin.getConfigManager().getMessage("list.header", headerPlaceholders))
        
        // 显示每种类型的统计和刷怪笼
        for (type in SpawnerType.values()) {
            val typeSpawners = spawnersByType[type] ?: emptyList()
            val placed = typeSpawners.size
            val limit = plugin.getPermissionManager().getSpawnerLimit(target, type)
            
            // 显示类型标题和统计
            val typePlaceholders = mapOf(
                "%type%" to type.name,
                "%count%" to placed.toString(),
                "%limit%" to limit.toString()
            )
            sender.sendMessage(plugin.getConfigManager().getMessage("list.type_header", typePlaceholders))
            
            // 显示每个刷怪笼的位置
            if (typeSpawners.isEmpty()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("list.no_spawners", emptyMap()))
            } else {
                for (spawner in typeSpawners) {
                    val loc = spawner.location
                    val worldName = loc.world?.name ?: "unknown"
                    val locPlaceholders = mapOf(
                        "%world%" to worldName,
                        "%x%" to loc.blockX.toString(),
                        "%y%" to loc.blockY.toString(),
                        "%z%" to loc.blockZ.toString(),
                        "%entity%" to spawner.entityType.name
                    )
                    sender.sendMessage(plugin.getConfigManager().getMessage("list.spawner_location", locPlaceholders))
                }
            }
            sender.sendMessage("") // 空行分隔
        }
        
        return true
    }
    
    private fun handleRemove(sender: CommandSender): Boolean {
        if (!sender.hasPermission("mcw.admin.remove")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"))
            return true
        }
        
        if (sender !is Player) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.player_only"))
            return true
        }
        
        val target = sender.getTargetBlock(null, 5)
        
        if (target.type != Material.SPAWNER) {
            sender.sendMessage("§c请看向刷怪笼！")
            return true
        }
        
        val loc = target.location
        val spawner = plugin.getSpawnerManager().getSpawner(loc)
        
        if (spawner == null) {
            sender.sendMessage("§c非MCW刷怪笼！")
            return true
        }
        
        plugin.getSpawnerManager().removeSpawner(loc)
        target.type = Material.AIR
        sender.sendMessage(plugin.getConfigManager().getMessage("success.removed_spawner"))
        return true
    }
    
    private fun handleLimit(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mcw.admin.limit")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"))
            return true
        }
        
        if (args.size < 5) {
            sender.sendMessage("§c用法: /mcw limit <set/add/remove> <玩家> <类型> <数量>")
            return true
        }
        
        val action = args[1].lowercase()
        if (action !in setOf("set", "add", "remove")) {
            sender.sendMessage("§c无效的操作！请使用 set、add 或 remove")
            return true
        }
        
        val target = Bukkit.getPlayer(args[2])
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.player_not_found"))
            return true
        }
        
        val type = try {
            SpawnerType.valueOf(args[3].uppercase())
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§c无效类型！请使用 NORMAL 或 PREMIUM")
            return true
        }
        
        val amount = try {
            args[4].toInt()
        } catch (e: NumberFormatException) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.invalid_amount"))
            return true
        }
        
        if (action == "set" && amount < 0) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.invalid_amount"))
            return true
        }
        if ((action == "add" || action == "remove") && amount <= 0) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error.invalid_amount"))
            return true
        }
        
        // 根据操作类型执行相应的逻辑
        val config = when (type) {
            SpawnerType.NORMAL -> plugin.getConfigManager().getNormalConfig()
            SpawnerType.PREMIUM -> plugin.getConfigManager().getPremiumConfig()
        } ?: return true
        
        // 对于付费刷怪笼，不限制最大可购买数量（通过指令管理）
        // 对于普通刷怪笼，使用默认限制
        val maxPurchasable = if (type == SpawnerType.PREMIUM) {
            Int.MAX_VALUE // 付费刷怪笼无限制
        } else {
            config.getInt("economy.max_purchasable", 50) // 普通刷怪笼限制
        }
        val currentLimit = plugin.getDataManager().getPlayerData(target.uniqueId).getPurchasedLimit(type)
        
        val placeholders = mapOf(
            "%player%" to target.name,
            "%type%" to type.name,
            "%amount%" to amount.toString()
        )
        
        when (action) {
            "set" -> {
                // 检查是否超过最大可购买数量
                if (amount > maxPurchasable) {
                    val errorPlaceholders = mapOf("%max%" to maxPurchasable.toString())
                    sender.sendMessage(plugin.getConfigManager().getMessage("error.cannot_exceed_max", errorPlaceholders))
                    return true
                }
                
                // 设置限制
                plugin.getDataManager().getPlayerData(target.uniqueId).setPurchasedLimit(type, amount)
                plugin.getDataManager().savePlayerData(plugin.getDataManager().getPlayerData(target.uniqueId))
                sender.sendMessage(plugin.getConfigManager().getMessage("success.limit_set", placeholders))
            }
            
            "add" -> {
                val newLimitAdd = currentLimit + amount
                
                // 检查是否超过最大可购买数量
                if (newLimitAdd > maxPurchasable) {
                    val errorPlaceholders = mapOf("%max%" to maxPurchasable.toString())
                    sender.sendMessage(plugin.getConfigManager().getMessage("error.cannot_exceed_max", errorPlaceholders))
                    return true
                }
                
                // 增加限制
                plugin.getDataManager().getPlayerData(target.uniqueId).addPurchasedLimit(type, amount)
                plugin.getDataManager().savePlayerData(plugin.getDataManager().getPlayerData(target.uniqueId))
                sender.sendMessage(plugin.getConfigManager().getMessage("success.limit_added", placeholders))
            }
            
            "remove" -> {
                // 检查当前限制是否为0
                if (currentLimit == 0) {
                    val errorPlaceholders = mapOf("%type%" to type.name)
                    sender.sendMessage(plugin.getConfigManager().getMessage("error.no_limit_to_remove", errorPlaceholders))
                    return true
                }
                
                // 计算实际可以减少的数量
                val actualRemoveAmount = minOf(amount, currentLimit)
                val newLimitRemove = currentLimit - actualRemoveAmount
                
                // 更新限制
                plugin.getDataManager().getPlayerData(target.uniqueId).setPurchasedLimit(type, newLimitRemove)
                plugin.getDataManager().savePlayerData(plugin.getDataManager().getPlayerData(target.uniqueId))
                
                // 根据实际删除的数量显示不同的消息
                val removePlaceholders = mapOf(
                    "%player%" to target.name,
                    "%type%" to type.name,
                    "%amount%" to actualRemoveAmount.toString(),
                    "%current%" to currentLimit.toString(),
                    "%new%" to newLimitRemove.toString()
                )
                
                if (actualRemoveAmount < amount) {
                    // 实际删除的数量少于请求的数量
                    sender.sendMessage(plugin.getConfigManager().getMessage("success.limit_removed_partial", removePlaceholders))
                } else {
                    // 完全按照请求的数量删除
                    sender.sendMessage(plugin.getConfigManager().getMessage("success.limit_removed", removePlaceholders))
                }
            }
        }
        
        return true
    }
    
    private fun formatLocation(loc: Location): String {
        return String.format("%d, %d, %d", loc.blockX, loc.blockY, loc.blockZ)
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        val completions = mutableListOf<String>()
        
        when (args.size) {
            1 -> {
                completions.addAll(listOf("help", "reload", "give", "info", "list", "remove"))
                if (sender.hasPermission("mcw.admin")) {
                    completions.add("limit")
                }
            }
            2 -> {
                val cmd = args[0].lowercase()
                when (cmd) {
                    "give", "list" -> {
                        for (player in Bukkit.getOnlinePlayers()) {
                            completions.add(player.name)
                        }
                    }
                    "limit" -> {
                        completions.addAll(listOf("set", "add", "remove"))
                    }
                }
            }
            3 -> {
                val cmd = args[0].lowercase()
                when (cmd) {
                    "give" -> {
                        completions.addAll(listOf("normal", "premium"))
                    }
                    "limit" -> {
                        for (player in Bukkit.getOnlinePlayers()) {
                            completions.add(player.name)
                        }
                    }
                }
            }
            4 -> {
                val cmd = args[0].lowercase()
                when (cmd) {
                    "give" -> {
                        for (type in EntityType.values()) {
                            if (type.isSpawnable && type.isAlive) {
                                completions.add(type.name.lowercase())
                            }
                        }
                    }
                    "limit" -> {
                        completions.addAll(listOf("normal", "premium"))
                    }
                }
            }
            5 -> {
                if (args[0].equals("limit", ignoreCase = true)) {
                    completions.addAll(listOf("1", "5", "10", "20", "50"))
                }
            }
        }
        
        // 过滤匹配的补全
        val input = args[args.size - 1].lowercase()
        return completions.filter { it.lowercase().startsWith(input) }
    }
}
