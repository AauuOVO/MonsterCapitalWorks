package org.mcw.monstercapitalworks.command

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import org.mcw.monstercapitalworks.model.SpawnerType

class MCWCommand(private val plugin: MonsterCapitalWorks) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        
        return when (args[0].lowercase()) {
            "help" -> { sendHelp(sender); true }
            "reload" -> handleReload(sender)
            "give" -> handleGive(sender, args)
            "info" -> handleInfo(sender)
            "list" -> handleList(sender, args)
            "remove" -> handleRemove(sender)
            "limit" -> handleLimit(sender, args)
            else -> { sender.sendMessage(plugin.configManager.getMessage("error.unknown_command")); true }
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
        }
    }
    
    private fun handleReload(sender: CommandSender): Boolean {
        if (!sender.hasPermission("mcw.admin.reload")) {
            sender.sendMessage(plugin.configManager.getMessage("error.no_permission"))
            return true
        }
        plugin.reloadConfigs()
        sender.sendMessage(plugin.configManager.getMessage("success.reloaded"))
        return true
    }
    
    private fun handleGive(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("mcw.admin.give")) {
            sender.sendMessage(plugin.configManager.getMessage("error.no_permission"))
            return true
        }
        if (args.size < 3) {
            sender.sendMessage("§c用法: /mcw give <玩家> <类型> [实体]")
            return true
        }
        
        val target = plugin.server.getPlayer(args[1]) ?: run {
            sender.sendMessage(plugin.configManager.getMessage("error.player_not_found"))
            return true
        }
        
        val type = try {
            SpawnerType.valueOf(args[2].uppercase())
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§c无效类型！")
            return true
        }
        
        val entityType = if (args.size >= 4) {
            try {
                EntityType.valueOf(args[3].uppercase())
            } catch (e: IllegalArgumentException) {
                sender.sendMessage("§c无效实体！")
                return true
            }
        } else {
            EntityType.ZOMBIE
        }
        
        val spawnerItem = ItemStack(Material.SPAWNER)
        val meta = spawnerItem.itemMeta
        if (meta != null) {
            meta.displayName = plugin.configManager.colorize("§6${type.name} 刷怪笼")
            meta.lore = listOf("§7类型: §e${type.name}", "§7实体: §e${entityType.name}")
            spawnerItem.itemMeta = meta
        }
        
        target.inventory.addItem(spawnerItem)
        sender.sendMessage("§a已给予 ${target.name} 刷怪笼！")
        return true
    }
    
    private fun handleInfo(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.configManager.getMessage("error.player_only"))
            return true
        }
        
        val target = sender.getTargetBlock(null, 5)
        if (target.type != Material.SPAWNER) {
            sender.sendMessage("§c请看向刷怪笼！")
            return true
        }
        
        val spawner = plugin.spawnerManager.getSpawner(target.location) ?: run {
            sender.sendMessage("§c非MCW刷怪笼！")
            return true
        }
        
        val placeholders = mapOf(
            "%type%" to spawner.type.name,
            "%entity%" to spawner.entityType.name,
            "%owner%" to (plugin.server.getOfflinePlayer(spawner.owner).name ?: "Unknown"),
            "%location%" to "${target.x.toInt()}, ${target.y.toInt()}, ${target.z.toInt()}",
            "%delay%" to spawner.spawnDelay.toString(),
            "%count%" to spawner.spawnCount.toString(),
            "%range%" to spawner.activationRange.toString(),
            "%stored%" to spawner.storedSpawns.toString(),
            "%max_storage%" to spawner.maxStorage.toString()
        )
        
        plugin.configManager.getMessageList("info", placeholders).forEach { sender.sendMessage(it) }
        return true
    }
    
    private fun handleList(sender: CommandSender, args: Array<String>): Boolean {
        val target = if (args.size >= 2) {
            if (!sender.hasPermission("mcw.admin.list.others")) {
                sender.sendMessage(plugin.configManager.getMessage("error.no_permission"))
                return true
            }
            plugin.server.getPlayer(args[1]) ?: run {
                sender.sendMessage(plugin.configManager.getMessage("error.player_not_found"))
                return true
            }
        } else {
            if (sender !is Player) {
                sender.sendMessage(plugin.configManager.getMessage("error.player_only"))
                return true
            }
            sender
        }
        
        val spawners = plugin.spawnerManager.getPlayerSpawners(target.uniqueId)
        sender.sendMessage("§6${target.name} 的刷怪笼 (共 ${spawners.size} 个)")
        spawners.forEach { spawner ->
            val loc = spawner.location
            sender.sendMessage("§7- ${spawner.type.name} ${spawner.entityType.name} @ ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}")
        }
        return true
    }
    
    private fun handleRemove(sender: CommandSender): Boolean {
        if (!sender.hasPermission("mcw.admin.remove")) {
            sender.sendMessage(plugin.configManager.getMessage("error.no_permission"))
            return true
        }
        if (sender !is Player) {
            sender.sendMessage(plugin.configManager.getMessage("error.player_only"))
            return true
        }
        
        val target = sender.getTargetBlock(null, 5)
        if (target.type != Material.SPAWNER) {
            sender.sendMessage("§c请看向刷怪笼！")
            return true
        }
        
        val spawner = plugin.spawnerManager.getSpawner(target.location) ?: run {
            sender.sendMessage("§c非MCW刷怪笼！")
            return true
        }
        
        plugin.spawnerManager.removeSpawner(target.location)
        target.type = Material.AIR
        sender.sendMessage(plugin.configManager.getMessage("success.removed_spawner"))
        return true
    }
    
    private fun handleLimit(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("mcw.admin.limit")) {
            sender.sendMessage(plugin.configManager.getMessage("error.no_permission"))
            return true
        }
        if (args.size < 5) {
            sender.sendMessage("§c用法: /mcw limit <set/add/remove> <玩家> <类型> <数量>")
            return true
        }
        
        val action = args[1].lowercase()
        val target = plugin.server.getPlayer(args[2]) ?: run {
            sender.sendMessage(plugin.configManager.getMessage("error.player_not_found"))
            return true
        }
        
        val type = try {
            SpawnerType.valueOf(args[3].uppercase())
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§c无效类型！")
            return true
        }
        
        val amount = try {
            args[4].toInt()
        } catch (e: NumberFormatException) {
            sender.sendMessage(plugin.configManager.getMessage("error.invalid_amount"))
            return true
        }
        
        val data = plugin.dataManager.getPlayerData(target.uniqueId)
        when (action) {
            "set" -> {
                data.setPurchasedLimit(type, amount)
                plugin.dataManager.savePlayerData(data)
                sender.sendMessage("§a已设置 ${target.name} 的 ${type.name} 额外限制为 $amount")
            }
            "add" -> {
                data.addPurchasedLimit(type, amount)
                plugin.dataManager.savePlayerData(data)
                sender.sendMessage("§a已增加 ${target.name} 的 ${type.name} 额外限制 $amount")
            }
            "remove" -> {
                val current = data.getPurchasedLimit(type)
                data.setPurchasedLimit(type, maxOf(0, current - amount))
                plugin.dataManager.savePlayerData(data)
                sender.sendMessage("§a已减少 ${target.name} 的 ${type.name} 额外限制 $amount")
            }
            else -> sender.sendMessage("§c无效操作！")
        }
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        val completions = mutableListOf<String>()
        
        when (args.size) {
            1 -> {
                completions.addAll(listOf("help", "reload", "give", "info", "list", "remove"))
                if (sender.hasPermission("mcw.admin")) completions.add("limit")
            }
            2 -> {
                when (args[0].lowercase()) {
                    "give", "list" -> plugin.server.onlinePlayers.forEach { completions.add(it.name) }
                    "limit" -> completions.addAll(listOf("set", "add", "remove"))
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "give" -> completions.addAll(listOf("normal", "premium"))
                    "limit" -> plugin.server.onlinePlayers.forEach { completions.add(it.name) }
                }
            }
            4 -> {
                when (args[0].lowercase()) {
                    "give" -> EntityType.values().filter { it.isSpawnable && it.isAlive }.forEach { completions.add(it.name.lowercase()) }
                    "limit" -> completions.addAll(listOf("normal", "premium"))
                }
            }
        }
        
        val input = args.last().lowercase()
        return completions.filter { it.lowercase().startsWith(input) }
    }
}
