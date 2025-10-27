package org.Aauu.monsterCapitalWorks.listener

import org.Aauu.monsterCapitalWorks.MonsterCapitalWorks
import org.Aauu.monsterCapitalWorks.model.Spawner
import org.Aauu.monsterCapitalWorks.model.SpawnerType
import org.Aauu.monsterCapitalWorks.util.ConditionChecker
import org.bukkit.Material
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
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class SpawnerListener(private val plugin: MonsterCapitalWorks) : Listener {
    
    @EventHandler(priority = EventPriority.HIGH)
    fun onSpawnerPlace(event: BlockPlaceEvent) {
        val block = event.blockPlaced
        if (block.type != Material.SPAWNER) {
            return
        }
        
        val player = event.player
        val item = event.itemInHand
        
        if (!isAMCSpawner(item)) {
            return
        }
        
        val type = getSpawnerType(item)
        val entityType = getEntityType(item)
        
        if (type == null || entityType == null) {
            event.isCancelled = true
            return
        }
        
        if (!player.hasPermission("mcw.place.${type.name.lowercase()}")) {
            event.isCancelled = true
            player.sendMessage(plugin.getConfigManager().getMessage("error.no_permission"))
            return
        }
        
        val placed = plugin.getSpawnerManager().getPlayerSpawnerCount(player.uniqueId, type)
        val limit = plugin.getPermissionManager().getSpawnerLimit(player, type)
        
        if (placed >= limit) {
            event.isCancelled = true
            val placeholders = mapOf(
                "%current%" to placed.toString(),
                "%limit%" to limit.toString()
            )
            player.sendMessage(plugin.getConfigManager().getMessage("error.spawner_limit_reached", placeholders))
            return
        }
        
        val loc = block.location
        
        // 检查spawn_conditions（Y坐标条件）
        if (!player.hasPermission("mcw.bypass.conditions")) {
            val entityConfig = if (type == SpawnerType.NORMAL) {
                plugin.getConfigManager().getNormalEntities()
            } else {
                plugin.getConfigManager().getPremiumEntities()
            }
            
            if (entityConfig != null) {
                val spawnConditions = entityConfig.getConfigurationSection(
                    "entities.${entityType.name}.spawn_conditions"
                )
                
                val conditionChecker = ConditionChecker(plugin)
                if (!conditionChecker.checkYConditions(loc, spawnConditions)) {
                    event.isCancelled = true
                    player.sendMessage(plugin.getConfigManager().getMessage("error.spawn_conditions_not_met"))
                    return
                }
            }
        }
        
        val spawner = plugin.getSpawnerManager().createSpawner(player.uniqueId, type, entityType, loc)
        
        val upgrades = getUpgradesFromItem(item)
        if (upgrades.isNotEmpty()) {
            for ((key, value) in upgrades) {
                spawner.setUpgradeLevel(key, value)
            }
            plugin.getUpgradeManager().applyUpgrades(spawner)
            // 确保升级数据同步到数据库
            plugin.getSpawnerManager().updateSpawner(spawner)
        }
        
        val cs = block.state as org.bukkit.block.CreatureSpawner
        cs.spawnedType = entityType
        cs.delay = spawner.spawnDelay
        cs.maxNearbyEntities = spawner.maxNearbyEntities
        cs.requiredPlayerRange = spawner.activationRange
        cs.spawnCount = spawner.spawnCount
        
        // 禁用原版刷怪笼生成
        if (plugin.config.getBoolean("spawner_control.disable_vanilla_spawning", true)) {
            // 设置一个极大的延迟来禁用原版生成
            cs.delay = Short.MAX_VALUE.toInt()
        }
        
        cs.update()
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun onSpawnerBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.SPAWNER) {
            return
        }
        
        val loc = block.location
        val spawner = plugin.getSpawnerManager().getSpawner(loc) ?: return
        
        val player = event.player
        
        if (spawner.owner != player.uniqueId && !player.hasPermission("mcw.admin.break")) {
            event.isCancelled = true
            player.sendMessage(plugin.getConfigManager().getMessage("error.not_owner"))
            return
        }
        
        plugin.getSpawnerManager().removeSpawner(loc)
        event.isDropItems = false
        val drop = createSpawnerItemWithUpgrades(spawner)
        loc.world?.dropItemNaturally(loc, drop)
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun onSpawnerInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (block.type != Material.SPAWNER) {
            return
        }
        
        val loc = block.location
        val spawner = plugin.getSpawnerManager().getSpawner(loc)
        
        if (spawner == null) {
            // 调试信息：刷怪笼未找到
            if (plugin.config.getBoolean("debug", false)) {
                plugin.logger.info("未找到刷怪笼: $loc")
                plugin.logger.info("已加载的刷怪笼数量: ${plugin.getSpawnerManager().getPlayerSpawners(event.player.uniqueId).size}")
            }
            return
        }
        
        val player = event.player
        
        // 处理 Shift+左键 掉落刷怪笼
        if (event.action == Action.LEFT_CLICK_BLOCK && player.isSneaking) {
            event.isCancelled = true
            
            // 检查是否是所有者或有管理员权限
            if (spawner.owner != player.uniqueId && !player.hasPermission("mcw.admin.pickup")) {
                player.sendMessage(plugin.getConfigManager().getMessage("error.not_your_spawner"))
                return
            }
            
            // 移除刷怪笼并掉落物品
            plugin.getSpawnerManager().removeSpawner(loc)
            block.type = Material.AIR
            
            // 创建带升级信息的刷怪笼物品
            val drop = createSpawnerItemWithUpgrades(spawner)
            loc.world?.dropItemNaturally(loc, drop)
            
            player.sendMessage(plugin.getConfigManager().getMessage("success.spawner_dropped"))
            return
        }
        
        // 处理右键打开GUI（支持右键和Shift+右键）
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            if (spawner.owner != player.uniqueId && !player.hasPermission("mcw.admin.use")) {
                event.isCancelled = true
                player.sendMessage(plugin.getConfigManager().getMessage("error.not_owner"))
                return
            }
            
            event.isCancelled = true
            plugin.getGUIManager().openMainMenu(player, spawner)
            return
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) {
            return
        }
        
        val player = event.whoClicked as Player
        val guiName = plugin.getGUIManager().getOpenGui(player.uniqueId) ?: return
        
        // 立即取消所有点击事件，防止物品被拿走或交换
        event.isCancelled = true
        
        val slot = event.rawSlot
        
        // 如果点击的是玩家背包区域，直接返回不处理（防止玩家操作自己的背包）
        if (slot < 0 || slot >= event.inventory.size) {
            return
        }
        
        // 额外检查：如果当前物品为空，也不处理
        val currentItem = event.currentItem ?: return
        if (currentItem.type == Material.AIR) {
            return
        }
        
        // 处理实体选择菜单
        if ("entity_menu" == guiName) {
            if (currentItem.type.name.endsWith("_SPAWN_EGG")) {
                plugin.getGUIManager().handleEntityClick(player, slot)
                return
            }
        }
        
        // 处理其他GUI点击
        val isRightClick = event.click.name.contains("RIGHT")
        val isShiftClick = event.isShiftClick
        plugin.getGUIManager().handleClick(player, slot, guiName, isRightClick, isShiftClick)
    }
    
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.player !is Player) {
            return
        }
        
        val player = event.player as Player
        plugin.getGUIManager().closeGui(player.uniqueId)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        // 检查生成原因是否来自刷怪笼
        if (event.spawnReason != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return
        }
        
        val block = event.location.block
        
        // 检查该位置是否刷怪笼
        val spawner = plugin.getSpawnerManager().getSpawner(block.location)
        
        if (spawner != null) {
            // 刷怪笼，取消原版生成
            event.isCancelled = true
            return
        }
        
        // 如果启用了禁用原版刷怪笼，则取消所有来自刷怪笼的生成
        if (plugin.config.getBoolean("spawner_control.disable_vanilla_spawning", true)) {
            event.isCancelled = true
        }
    }
    
    private fun isAMCSpawner(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.SPAWNER) {
            return false
        }
        
        val meta = item.itemMeta ?: return false
        if (!meta.hasLore()) {
            return false
        }
        
        val lore = meta.lore ?: return false
        if (lore.isEmpty()) {
            return false
        }
        
        // 检查lore中是否包含类型标识
        for (line in lore) {
            if (line.contains("类型:")) {
                return true
            }
        }
        
        return false
    }
    
    private fun getSpawnerType(item: ItemStack): SpawnerType? {
        val meta = item.itemMeta ?: return null
        if (!meta.hasLore()) {
            return null
        }
        
        val lore = meta.lore ?: return null
        
        for (line in lore) {
            if (line.contains("类型:")) {
                val typeStr = line.replace("§7类型: §e", "").trim()
                return try {
                    SpawnerType.valueOf(typeStr)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
        
        return null
    }
    
    private fun getEntityType(item: ItemStack): EntityType? {
        val meta = item.itemMeta ?: return null
        if (!meta.hasLore()) {
            return null
        }
        
        val lore = meta.lore ?: return null
        
        for (line in lore) {
            if (line.contains("实体:")) {
                val entityStr = line.replace("§7实体: §e", "").trim()
                return try {
                    EntityType.valueOf(entityStr)
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
            // 使用自定义名称
            val displayName = getSpawnerDisplayName(spawner.type, spawner.entityType)
            meta.setDisplayName(displayName)
            
            // 创建简化的lore，不显示升级信息
            val lore = mutableListOf<String>()
            lore.add("§7类型: §e${spawner.type.name}")
            lore.add("§7实体: §e${spawner.entityType.name}")
            
            // 隐藏升级信息，但仍然保存在NBT中
            // 升级信息将在getUpgradesFromItem中读取
            val upgrades = spawner.upgradeLevels
            if (upgrades.isNotEmpty()) {
                lore.add("")
                lore.add("§8升级等级:")
                for ((key, value) in upgrades) {
                    if (value > 0) {
                        lore.add("§8  $key: §e$value")
                    }
                }
            }
            
            meta.lore = lore
            item.itemMeta = meta
        }
        
        return item
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
    
    private fun getUpgradesFromItem(item: ItemStack): MutableMap<String, Int> {
        val upgrades = mutableMapOf<String, Int>()
        
        val meta = item.itemMeta ?: return upgrades
        if (!meta.hasLore()) {
            return upgrades
        }
        
        val lore = meta.lore ?: return upgrades
        
        var inUpgradeSection = false
        for (line in lore) {
            if (line.contains("升级等级:")) {
                inUpgradeSection = true
                continue
            }
            
            if (inUpgradeSection && line.contains(":")) {
                val cleaned = line.replace("§8  ", "").replace("§e", "").trim()
                val parts = cleaned.split(":")
                if (parts.size == 2) {
                    try {
                        val upgradeName = parts[0].trim()
                        val level = parts[1].trim().toInt()
                        upgrades[upgradeName] = level
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
        }
        
        return upgrades
    }
}
