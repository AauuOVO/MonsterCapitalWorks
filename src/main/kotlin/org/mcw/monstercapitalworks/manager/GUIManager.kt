package org.mcw.monstercapitalworks.manager

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import org.mcw.monstercapitalworks.model.PlayerData
import org.mcw.monstercapitalworks.model.Spawner
import org.mcw.monstercapitalworks.model.SpawnerType
import java.util.*

/**
 * GUI管理器 - 管理所有菜单界面
 */
class GUIManager(private val plugin: MonsterCapitalWorks) {
    
    private val openGuis = mutableMapOf<UUID, String>()
    private val selectedSpawners = mutableMapOf<UUID, Spawner>()
    
    fun initialize() {
        // GUI管理器初始化
        plugin.logger.info("GUI管理器已初始化")
    }
    
    fun openMainMenu(player: Player, spawner: Spawner) {
        val menuName = if (spawner.type == SpawnerType.NORMAL) "main_menu_normal" else "main_menu_premium"
        val guiConfig = plugin.configManager.getGuiConfig(menuName) ?: return
        
        val title = plugin.configManager.colorize(guiConfig.getString("title", "刷怪笼控制面板"))
        val size = guiConfig.getInt("size", 27)
        
        val inv = Bukkit.createInventory(null, size, title)
        loadGuiItems(inv, guiConfig, player, spawner)
        
        player.openInventory(inv)
        openGuis[player.uniqueId] = menuName
        selectedSpawners[player.uniqueId] = spawner
    }
    
    fun openUpgradeMenu(player: Player, spawner: Spawner) {
        val guiConfig = plugin.configManager.getGuiConfig("upgrade_menu") ?: return
        
        val title = plugin.configManager.colorize(guiConfig.getString("title", "升级刷怪笼"))
        val size = guiConfig.getInt("size", 54)
        
        val inv = Bukkit.createInventory(null, size, title)
        loadGuiItems(inv, guiConfig, player, spawner)
        
        player.openInventory(inv)
        openGuis[player.uniqueId] = "upgrade_menu"
        selectedSpawners[player.uniqueId] = spawner
    }
    
    fun openEntityMenu(player: Player, spawner: Spawner) {
        val guiConfig = plugin.configManager.getGuiConfig("entity_menu") ?: return
        
        val title = plugin.configManager.colorize(guiConfig.getString("title", "选择生物") ?: "选择生物")
        val size = guiConfig.getInt("size", 54)
        
        val inv = Bukkit.createInventory(null, size, title)
        
        val entityConfig = if (spawner.type == SpawnerType.NORMAL) {
            plugin.configManager.getNormalEntities()
        } else {
            plugin.configManager.getPremiumEntities()
        }
        
        if (entityConfig != null) {
            val entitiesSection = entityConfig.getConfigurationSection("entities")
            if (entitiesSection != null) {
                var slot = 0
                for (entityKey in entitiesSection.getKeys(false)) {
                    if (slot >= 45) break
                    
                    val entitySection = entitiesSection.getConfigurationSection(entityKey)
                    if (entitySection != null) {
                        try {
                            val entityType = EntityType.valueOf(entityKey.uppercase())
                            val item = createEntityItem(entitySection, entityType, player, spawner)
                            inv.setItem(slot++, item)
                        } catch (e: IllegalArgumentException) {
                            plugin.logger.warning("无效的实体类型: $entityKey")
                        }
                    }
                }
            }
        }
        
        player.openInventory(inv)
        openGuis[player.uniqueId] = "entity_menu"
        selectedSpawners[player.uniqueId] = spawner
    }
    
    fun openBuyLimitMenu(player: Player, spawner: Spawner) {
        val guiConfig = plugin.configManager.getGuiConfig("buy_limit_menu") ?: return
        
        val title = plugin.configManager.colorize(guiConfig.getString("title", "购买数量上限"))
        val size = guiConfig.getInt("size", 27)
        
        val inv = Bukkit.createInventory(null, size, title)
        loadGuiItems(inv, guiConfig, player, spawner)
        
        player.openInventory(inv)
        openGuis[player.uniqueId] = "buy_limit_menu"
        selectedSpawners[player.uniqueId] = spawner
    }
    
    fun openPrecisePosMenu(player: Player, spawner: Spawner) {
        val guiConfig = plugin.configManager.getGuiConfig("precise_pos_menu") ?: return
        
        val title = plugin.configManager.colorize(guiConfig.getString("title", "精确位置设置"))
        val size = guiConfig.getInt("size", 27)
        
        val inv = Bukkit.createInventory(null, size, title)
        loadGuiItems(inv, guiConfig, player, spawner)
        
        player.openInventory(inv)
        openGuis[player.uniqueId] = "precise_pos_menu"
        selectedSpawners[player.uniqueId] = spawner
    }
    
    private fun loadGuiItems(inv: Inventory, guiConfig: FileConfiguration, player: Player, spawner: Spawner) {
        val itemsSection = guiConfig.getConfigurationSection("items") ?: return
        
        for (key in itemsSection.getKeys(false)) {
            val itemSection = itemsSection.getConfigurationSection(key) ?: continue
            val item = createGuiItem(itemSection, player, spawner)
            val slot = itemSection.getInt("slot", 0)
            inv.setItem(slot, item)
        }
    }
    
    private fun createGuiItem(section: org.bukkit.configuration.ConfigurationSection, player: Player, spawner: Spawner): ItemStack {
        val materialName = section.getString("material", "STONE") ?: "STONE"
        val material = try {
            Material.valueOf(materialName)
        } catch (e: IllegalArgumentException) {
            Material.STONE
        }
        
        val item = ItemStack(material)
        val meta = item.itemMeta
        
        if (meta != null) {
            val displayName = section.getString("name", "") ?: ""
            val processedName = replacePlaceholders(displayName, player, spawner)
            meta.displayName = plugin.configManager.colorize(processedName)
            
            val lore = section.getStringList("lore").map { line ->
                plugin.configManager.colorize(replacePlaceholders(line, player, spawner))
            }
            meta.lore = lore
            
            item.itemMeta = meta
        }
        
        return item
    }
    
    private fun createEntityItem(section: org.bukkit.configuration.ConfigurationSection, entityType: EntityType, player: Player, spawner: Spawner): ItemStack {
        val material = try {
            Material.valueOf("${entityType.name}_SPAWN_EGG")
        } catch (e: IllegalArgumentException) {
            Material.SPAWNER
        }
        
        val item = ItemStack(material)
        val meta = item.itemMeta
        
        if (meta != null) {
            val customEggName = plugin.configManager.getSpawnEggCustomName(entityType.name)
            val displayName = customEggName ?: plugin.configManager.colorize(section.getString("name", entityType.name) ?: entityType.name)
            meta?.displayName = displayName
            
            val data = plugin.dataManager.getPlayerData(player.uniqueId)
            val unlocked = !section.getBoolean("require_unlock", true) || data.hasUnlockedEntity(spawner.type, entityType)
            
            val lore = mutableListOf<String>()
            lore.addAll(section.getStringList("lore").map { plugin.configManager.colorize(it) })
            lore.add("")
            
            if (!section.getBoolean("enabled", true)) {
                lore.add("§c✘ 已禁用")
            } else if (unlocked) {
                lore.add("§a✔ 已解锁")
            } else {
                val price = section.getDouble("price", 0.0)
                lore.add("§e价格: §f${String.format("%.2f", price)}")
                lore.add("§c✘ 未解锁")
            }
            
            meta.lore = lore
            item.itemMeta = meta
        }
        
        return item
    }
    
    fun handleEntityClick(player: Player, slot: Int) {
        val spawner = selectedSpawners[player.uniqueId] ?: return
        
        val entityConfig = if (spawner.type == SpawnerType.NORMAL) {
            plugin.configManager.getNormalEntities()
        } else {
            plugin.configManager.getPremiumEntities()
        } ?: return
        
        val entitiesSection = entityConfig.getConfigurationSection("entities") ?: return
        
        var currentSlot = 0
        for (entityKey in entitiesSection.getKeys(false)) {
            if (currentSlot == slot) {
                val entitySection = entitiesSection.getConfigurationSection(entityKey)
                if (entitySection != null) {
                    try {
                        val entityType = EntityType.valueOf(entityKey.uppercase())
                        handleEntitySelection(player, spawner, entityType, entitySection)
                    } catch (e: IllegalArgumentException) {
                        player.sendMessage("§c无效的实体类型！")
                    }
                }
                return
            }
            currentSlot++
        }
    }
    
    private fun handleEntitySelection(player: Player, spawner: Spawner, entityType: EntityType, entityConfig: org.bukkit.configuration.ConfigurationSection) {
        if (!entityConfig.getBoolean("enabled", true)) {
            player.sendMessage("§c该生物已被禁用！")
            return
        }
        
        val requireUnlock = entityConfig.getBoolean("require_unlock", true)
        val data = plugin.dataManager.getPlayerData(player.uniqueId)
        val unlocked = !requireUnlock || data.hasUnlockedEntity(spawner.type, entityType)
        
        if (!unlocked) {
            val price = entityConfig.getDouble("price", 0.0)
            val economy = plugin.economyManager
            
            if (economy.isEnabled()) {
                if (economy.has(player, price)) {
                    economy.withdraw(player, price)
                    data.unlockEntity(spawner.type, entityType)
                    plugin.dataManager.savePlayerData(data)
                    openEntityMenu(player, spawner)
                } else {
                    player.sendMessage(plugin.configManager.getMessage("error.insufficient_funds"))
                }
            }
        } else {
            spawner.entityType = entityType
            spawner.storedSpawns = 0
            plugin.spawnerManager.updateSpawnerEntity(spawner.location, entityType)
            plugin.spawnerManager.saveSpawner(spawner)
            player.closeInventory()
            player.sendMessage("§a已切换刷怪笼生物类型！")
        }
    }
    
    fun handleClick(player: Player, slot: Int, guiName: String, isRightClick: Boolean, isShiftClick: Boolean) {
        val spawner = selectedSpawners[player.uniqueId] ?: return
        val guiConfig = plugin.configManager.getGuiConfig(guiName) ?: return
        
        val itemsSection = guiConfig.getConfigurationSection("items") ?: return
        
        for (key in itemsSection.getKeys(false)) {
            val itemSection = itemsSection.getConfigurationSection(key)
            if (itemSection != null && itemSection.getInt("slot", -1) == slot) {
                val actions = itemSection.getStringList("actions")
                executeActions(player, spawner, actions)
                break
            }
        }
    }
    
    private fun executeActions(player: Player, spawner: Spawner, actions: List<String>) {
        for (action in actions) {
            when {
                action.startsWith("open_gui:") -> {
                    val guiName = action.substring(9).trim()
                    openGui(player, spawner, guiName)
                }
                action == "close" -> player.closeInventory()
                action.startsWith("upgrade:") -> {
                    val upgradeName = action.substring(8).trim()
                    plugin.upgradeManager.upgradeSpawner(player, spawner, upgradeName)
                    val currentGui = openGuis[player.uniqueId]
                    if (currentGui != null) openGui(player, spawner, currentGui)
                }
                action == "toggle_active" -> {
                    spawner.active = !spawner.active
                    plugin.spawnerManager.saveSpawner(spawner)
                    val currentGui = openGuis[player.uniqueId]
                    if (currentGui != null) openGui(player, spawner, currentGui)
                }
            }
        }
    }
    
    private fun openGui(player: Player, spawner: Spawner, guiName: String) {
        when (guiName) {
            "main_menu", "main_menu_normal", "main_menu_premium" -> openMainMenu(player, spawner)
            "upgrade_menu" -> openUpgradeMenu(player, spawner)
            "entity_menu" -> openEntityMenu(player, spawner)
            "buy_limit_menu" -> openBuyLimitMenu(player, spawner)
            "precise_pos_menu" -> openPrecisePosMenu(player, spawner)
        }
    }
    
    private fun replacePlaceholders(text: String, player: Player, spawner: Spawner): String {
        var result = text
        result = result.replace("%amc_type%", spawner.type.name)
        result = result.replace("%amc_entity%", spawner.entityType.name)
        result = result.replace("%amc_status%", if (spawner.active) "§a开启" else "§c关闭")
        result = result.replace("%amc_stored%", spawner.storedSpawns.toString())
        result = result.replace("%amc_spawn_delay%", spawner.spawnDelay.toString())
        result = result.replace("%amc_spawn_count%", spawner.spawnCount.toString())
        result = result.replace("%amc_max_nearby%", spawner.maxNearbyEntities.toString())
        result = result.replace("%amc_activation_range%", spawner.activationRange.toString())
        result = result.replace("%amc_max_storage%", spawner.maxStorage.toString())
        
        val placed = plugin.spawnerManager.getPlayerSpawnerCount(player.uniqueId, spawner.type)
        val limit = plugin.permissionManager.getSpawnerLimit(player, spawner.type)
        result = result.replace("%amc_placed%", placed.toString())
        result = result.replace("%amc_limit%", limit.toString())
        
        return result
    }
    
    fun getOpenGui(player: UUID): String? = openGuis[player]
    
    fun getSelectedSpawner(player: UUID): Spawner? = selectedSpawners[player]
    
    fun closeGui(player: UUID) {
        openGuis.remove(player)
        selectedSpawners.remove(player)
    }
}
