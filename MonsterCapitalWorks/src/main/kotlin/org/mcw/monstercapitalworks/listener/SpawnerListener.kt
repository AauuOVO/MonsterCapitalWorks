package org.mcw.monstercapitalworks.listener

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import org.mcw.monstercapitalworks.model.Spawner
import org.mcw.monstercapitalworks.model.SpawnerType
import org.mcw.monstercapitalworks.util.ConditionChecker

class SpawnerListener(private val plugin: MonsterCapitalWorks) : Listener {
    
    @EventHandler(priority = EventPriority.HIGH)
    fun onSpawnerPlace(event: BlockPlaceEvent) {
        val block = event.blockPlaced
        if (block.type != Material.SPAWNER) return
        
        val player = event.player
        val item = event.itemInHand
        
        if (!isAMCSpawner(item)) return
        
        val type = getSpawnerType(item)
        val entityType = getEntityType(item)
        
        if (type == null || entityType == null) {
            event.isCancelled = true
            player.sendMessage("§c刷怪笼数据损坏！")
            return
        }
        
        if (!player.hasPermission("mcw.place.${type.name.lowercase()}")) {
            event.isCancelled = true
            player.sendMessage(plugin.configManager.getMessage("error.no_permission"))
            return
        }
        
        val placed = plugin.spawnerManager.getPlayerSpawnerCount(player.uniqueId, type)
        val limit = plugin.permissionManager.getSpawnerLimit(player, type)
        
        if (placed >= limit) {
            event.isCancelled = true
            player.sendMessage("§c已达到刷怪笼数量限制！当前: $placed/$limit")
            return
        }
        
        val loc = block.location
        
        // 检查Y坐标条件
        if (!player.hasPermission("mcw.bypass.conditions")) {
            val entityConfig = if (type == SpawnerType.NORMAL) {
                plugin.configManager.getNormalEntities()
            } else {
                plugin.configManager.getPremiumEntities()
            }
            
            if (entityConfig != null) {
                val spawnConditions = entityConfig.getConfigurationSection("entities.${entityType.name}.spawn_conditions")
                if (!ConditionChecker.checkYConditions(loc, spawnConditions)) {
                    event.isCancelled = true
                    player.sendMessage("§c该生物不满足当前位置的Y坐标条件！")
                    return
                }
            }
        }
        
        val spawner = plugin.spawnerManager.createSpawner(loc, player.uniqueId, type, entityType)
        
        val upgrades = getUpgradesFromItem(item)
        if (upgrades.isNotEmpty()) {
            for ((upgradeName, level) in upgrades) {
                spawner.setUpgradeLevel(upgradeName, level)
            }
            plugin.upgradeManager.applyUpgrades(spawner)
            plugin.spawnerManager.saveSpawner(spawner)
        }
        
        val cs = block.state as CreatureSpawner
        cs.spawnedType = entityType
        cs.delay = spawner.spawnDelay
        cs.maxNearbyEntities = spawner.maxNearbyEntities
        cs.requiredPlayerRange = spawner.activationRange
        cs.spawnCount = spawner.spawnCount
        cs.update()
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun onSpawnerBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.SPAWNER) return
        
        val loc = block.location
        val spawner = plugin.spawnerManager.getSpawner(loc) ?: return
        
        val player = event.player
        
        if (spawner.owner != player.uniqueId && !player.hasPermission("mcw.admin.break")) {
            event.isCancelled = true
            player.sendMessage(plugin.configManager.getMessage("error.not_owner"))
            return
        }
        
        plugin.spawnerManager.removeSpawner(loc)
        event.isDropItems = false
        val drop = createSpawnerItemWithUpgrades(spawner)
        loc.world!!.dropItemNaturally(loc, drop)
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun onSpawnerInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.type != Material.SPAWNER) return
        
        val loc = block.location
        val spawner = plugin.spawnerManager.getSpawner(loc) ?: return
        
        val player = event.player
        
        // Shift+左键 掉落刷怪笼
        if (event.action == Action.LEFT_CLICK_BLOCK && player.isSneaking) {
            event.isCancelled = true
            
            if (spawner.owner != player.uniqueId && !player.hasPermission("mcw.admin.pickup")) {
                player.sendMessage(plugin.configManager.getMessage("error.not_your_spawner"))
                return
            }
            
            plugin.spawnerManager.removeSpawner(loc)
            block.type = Material.AIR
            
            val drop = createSpawnerItemWithUpgrades(spawner)
            loc.world!!.dropItemNaturally(loc, drop)
            
            player.sendMessage(plugin.configManager.getMessage("success.spawner_dropped"))
            return
        }
        
        // 右键打开GUI
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        
        if (spawner.owner != player.uniqueId && !player.hasPermission("mcw.admin.use")) {
            event.isCancelled = true
            player.sendMessage(plugin.configManager.getMessage("error.not_owner"))
            return
        }
        
        event.isCancelled = true
        plugin.guiManager.openMainMenu(player, spawner)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) return
        
        val player = event.whoClicked as Player
        val guiName = plugin.guiManager.getOpenGui(player.uniqueId) ?: return
        
        event.isCancelled = true
        
        val slot = event.rawSlot
        if (slot < 0 || slot >= event.inventory.size) return
        
        val currentItem = event.currentItem ?: return
        if (currentItem.type == Material.AIR) return
        
        if (guiName == "entity_menu" && currentItem.type.name.endsWith("_SPAWN_EGG")) {
            plugin.guiManager.handleEntityClick(player, slot)
            return
        }
        
        plugin.guiManager.handleClick(player, slot, guiName, event.isRightClick, event.isShiftClick)
    }
    
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.player !is Player) return
        plugin.guiManager.closeGui((event.player as Player).uniqueId)
    }
    
    private fun isAMCSpawner(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.SPAWNER) return false
        val meta = item.itemMeta ?: return false
        val lore = meta.lore ?: return false
        return lore.any { it.contains("类型:") }
    }
    
    private fun getSpawnerType(item: ItemStack): SpawnerType? {
        val meta = item.itemMeta ?: return null
        val lore = meta.lore ?: return null
        for (line in lore) {
            if (line.contains("类型:")) {
                val typeStr = line.replace(Regex("§[0-9a-fk-or]"), "").replace("类型:", "").trim()
                return try {
                    SpawnerType.valueOf(typeStr.uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
        return null
    }
    
    private fun getEntityType(item: ItemStack): EntityType? {
        val meta = item.itemMeta ?: return null
        val lore = meta.lore ?: return null
        for (line in lore) {
            if (line.contains("实体:")) {
                val entityStr = line.replace(Regex("§[0-9a-fk-or]"), "").replace("实体:", "").trim()
                return try {
                    EntityType.valueOf(entityStr.uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
        return null
    }
    
    private fun createSpawnerItemWithUpgrades(spawner: Spawner): ItemStack {
        val item = ItemStack(Material.SPAWNER)
        val meta = item.itemMeta
        
        if (meta != null) {
            meta.displayName = plugin.configManager.colorize("§6${spawner.type.name} 刷怪笼")
            
            val lore = mutableListOf<String>()
            lore.add("§7类型: §e${spawner.type.name}")
            lore.add("§7实体: §e${spawner.entityType.name}")
            
            val upgrades = spawner.upgradeLevels
            if (upgrades.isNotEmpty()) {
                lore.add("")
                lore.add("§8升级等级:")
                for ((upgradeName, level) in upgrades) {
                    if (level > 0) {
                        lore.add("§8  $upgradeName: §e$level")
                    }
                }
            }
            
            meta.lore = lore
            item.itemMeta = meta
        }
        
        return item
    }
    
    private fun getUpgradesFromItem(item: ItemStack): Map<String, Int> {
        val upgrades = mutableMapOf<String, Int>()
        val meta = item.itemMeta ?: return upgrades
        val lore = meta.lore ?: return upgrades
        
        var inUpgradeSection = false
        for (line in lore) {
            if (line.contains("升级等级:")) {
                inUpgradeSection = true
                continue
            }
            
            if (inUpgradeSection && line.contains(":")) {
                val cleaned = line.replace("§8  ", "").replace("§e", "").trim()
                val colonIndex = cleaned.lastIndexOf(":")
                if (colonIndex > 0) {
                    try {
                        val upgradeName = cleaned.substring(0, colonIndex).trim()
                        val level = cleaned.substring(colonIndex + 1).trim().toInt()
                        upgrades[upgradeName] = level
                    } catch (e: NumberFormatException) {
                        // 忽略
                    }
                }
            }
        }
        
        return upgrades
    }
}
