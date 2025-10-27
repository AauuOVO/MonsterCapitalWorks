package org.Aauu.monsterCapitalWorks.manager

import org.Aauu.monsterCapitalWorks.model.PlayerData
import org.Aauu.monsterCapitalWorks.model.Spawner
import org.Aauu.monsterCapitalWorks.model.SpawnerType
import org.Aauu.monsterCapitalWorks.model.SpawnMode
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class GUIManager(private val plugin: JavaPlugin) {
    private val openGuis = mutableMapOf<UUID, String>()
    private val selectedSpawners = mutableMapOf<UUID, Spawner>()

    fun initialize() {
        // 初始化GUI系统
    }

    fun openMainMenu(player: Player, spawner: Spawner) {
        val menuName = if (spawner.type == SpawnerType.NORMAL) "main_menu_normal" else "main_menu_premium"
        val guiConfig = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.let {
            it.getConfigManager().getGuiConfig(menuName)
        } ?: return

        val title = colorize(guiConfig.getString("title", "刷怪笼控制面板") ?: "刷怪笼控制面板")
        val size = guiConfig.getInt("size", 27)

        val inv = Bukkit.createInventory(null, size, title)

        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            loadLayoutBasedGui(inv, guiConfig, player, spawner)
        } else {
            // 兼容旧的 items 格式
            val itemsSection = guiConfig.getConfigurationSection("items")
            if (itemsSection != null) {
                for (key in itemsSection.getKeys(false)) {
                    val itemSection = itemsSection.getConfigurationSection(key)
                    if (itemSection != null) {
                        val item = createGuiItem(itemSection, player, spawner)
                        val slot = itemSection.getInt("slot", 0)
                        inv.setItem(slot, item)
                    }
                }
            }
        }

        player.openInventory(inv)
        openGuis[player.uniqueId] = menuName
        selectedSpawners[player.uniqueId] = spawner
    }

    fun openUpgradeMenu(player: Player, spawner: Spawner) {
        val guiConfig = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.let {
            it.getConfigManager().getGuiConfig("upgrade_menu")
        } ?: return

        val title = colorize(guiConfig.getString("title", "升级刷怪笼") ?: "升级刷怪笼")
        val size = guiConfig.getInt("size", 54)

        val inv = Bukkit.createInventory(null, size, title)

        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            loadLayoutBasedGui(inv, guiConfig, player, spawner)
        } else {
            // 兼容旧的 items 格式
            val itemsSection = guiConfig.getConfigurationSection("items")
            if (itemsSection != null) {
                for (key in itemsSection.getKeys(false)) {
                    val itemSection = itemsSection.getConfigurationSection(key)
                    if (itemSection != null) {
                        val item = createGuiItem(itemSection, player, spawner)
                        val slot = itemSection.getInt("slot", 0)
                        inv.setItem(slot, item)
                    }
                }
            }
        }

        player.openInventory(inv)
        openGuis[player.uniqueId] = "upgrade_menu"
        selectedSpawners[player.uniqueId] = spawner
    }

    fun openEntityMenu(player: Player, spawner: Spawner) {
        val guiConfig = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.let {
            it.getConfigManager().getGuiConfig("entity_menu")
        } ?: return

        val title = colorize(guiConfig.getString("title", "选择生物") ?: "选择生物")
        val size = guiConfig.getInt("size", 54)

        val inv = Bukkit.createInventory(null, size, title)

        // 获取可用实体列表
        val entityConfig = if (spawner.type == SpawnerType.NORMAL) {
            (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getConfigManager()?.getNormalEntities()
        } else {
            (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getConfigManager()?.getPremiumEntities()
        }

        if (entityConfig != null) {
            val entitiesSection = entityConfig.getConfigurationSection("entities")
            if (entitiesSection != null) {
                var slot = 0
                for (entityKey in entitiesSection.getKeys(false)) {
                    if (slot >= 45) break // 留出空间给返回按钮

                    val entitySection = entitiesSection.getConfigurationSection(entityKey)
                    if (entitySection != null) {
                        try {
                            val entityType = EntityType.valueOf(entityKey.uppercase())
                            val item = createEntityItem(entitySection, entityType, player)
                            inv.setItem(slot++, item)
                        } catch (e: IllegalArgumentException) {
                            plugin.logger.warning("无效的实体类型: $entityKey")
                        }
                    }
                }
            }
        }

        val itemsSection = guiConfig.getConfigurationSection("items")
        if (itemsSection != null) {
            val backSection = itemsSection.getConfigurationSection("back")
            if (backSection != null) {
                val backItem = createGuiItem(backSection, player, spawner)
                val slot = backSection.getInt("slot", 49)
                inv.setItem(slot, backItem)
            }
        }

        player.openInventory(inv)
        openGuis[player.uniqueId] = "entity_menu"
        selectedSpawners[player.uniqueId] = spawner
    }

    fun openBuyLimitMenu(player: Player, spawner: Spawner) {
        // 检查是否为付费刷怪笼，如果是则提示只能通过指令管理
        if (spawner.type == SpawnerType.PREMIUM) {
            player.sendMessage("§c付费刷怪笼只能通过指令增加放置上限！")
            player.sendMessage("§7使用: §e/mcw limit set <玩家> premium <数量>")
            return
        }

        val guiConfig = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.let {
            it.getConfigManager().getGuiConfig("buy_limit_menu")
        } ?: return

        val title = colorize(guiConfig.getString("title", "购买数量上限") ?: "购买数量上限")
        val size = guiConfig.getInt("size", 27)

        val inv = Bukkit.createInventory(null, size, title)

        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            loadLayoutBasedGui(inv, guiConfig, player, spawner)
        } else {
            // 兼容旧的 items 格式
            val itemsSection = guiConfig.getConfigurationSection("items")
            if (itemsSection != null) {
                for (key in itemsSection.getKeys(false)) {
                    val itemSection = itemsSection.getConfigurationSection(key)
                    if (itemSection != null) {
                        val item = createGuiItem(itemSection, player, spawner)
                        val slot = itemSection.getInt("slot", 0)
                        inv.setItem(slot, item)
                    }
                }
            }
        }

        player.openInventory(inv)
        openGuis[player.uniqueId] = "buy_limit_menu"
        selectedSpawners[player.uniqueId] = spawner
    }

    fun openPrecisePosMenu(player: Player, spawner: Spawner) {
        // 检查精确模式权限 - 只检查是否有权限，不阻止打开菜单
        val permissionManager = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getPermissionManager()
        if (permissionManager != null && !permissionManager.canUsePreciseMode(player, spawner.type)) {
            player.sendMessage("§c你没有权限使用精确模式！")
            if (spawner.type == SpawnerType.PREMIUM) {
                player.sendMessage("§7需要权限: §emcw.spawnmode.precise")
            }
            // 仍然允许打开菜单，但会在操作时再次检查权限
        }

        // 优先尝试加载pos_menu.yml，如果不存在则使用precise_pos_menu.yml
        val configManager = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getConfigManager()
        val guiConfig = configManager?.let {
            // 先尝试pos_menu.yml
            it.getGuiConfig("pos_menu") ?: it.getGuiConfig("precise_pos_menu")
        } ?: return

        val menuName = if (configManager.getGuiConfig("pos_menu") != null) "pos_menu" else "precise_pos_menu"

        val title = colorize(guiConfig.getString("title", "精确位置设置") ?: "精确位置设置")
        val size = guiConfig.getInt("size", 27)

        val inv = Bukkit.createInventory(null, size, title)

        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            loadLayoutBasedGui(inv, guiConfig, player, spawner)
        } else {
            // 兼容旧的 items 格式
            val itemsSection = guiConfig.getConfigurationSection("items")
            if (itemsSection != null) {
                for (key in itemsSection.getKeys(false)) {
                    val itemSection = itemsSection.getConfigurationSection(key)
                    if (itemSection != null) {
                        val item = createGuiItem(itemSection, player, spawner)
                        val slot = itemSection.getInt("slot", 0)
                        inv.setItem(slot, item)
                    }
                }
            }
        }

        player.openInventory(inv)
        openGuis[player.uniqueId] = menuName
        selectedSpawners[player.uniqueId] = spawner
    }

    private fun loadLayoutBasedGui(inv: Inventory, guiConfig: FileConfiguration, player: Player, spawner: Spawner) {
        val layout = guiConfig.getStringList("layout")
        val iconsSection = guiConfig.getConfigurationSection("icons")

        if (layout.isEmpty() || iconsSection == null) {
            return
        }

        var slot = 0
        for (row in layout) {
            for (iconChar in row.toCharArray()) {
                val iconKey = iconChar.toString()
                val iconSection = iconsSection.getConfigurationSection(iconKey)

                if (iconSection != null) {
                    val item = createGuiItem(iconSection, player, spawner)
                    inv.setItem(slot, item)
                }
                slot++
            }
        }
    }

    private fun createGuiItem(section: ConfigurationSection, player: Player, spawner: Spawner): ItemStack {
        val materialName = section.getString("material", "STONE") ?: "STONE"
        val material = try {
            Material.valueOf(materialName)
        } catch (e: IllegalArgumentException) {
            Material.STONE
        }

        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        val name = section.getString("name", "") ?: ""
        meta.setDisplayName(colorize(replacePlaceholders(name, player, spawner)))

        val lore = section.getStringList("lore")
        val processedLore = mutableListOf<String>()

        // 处理lore中的占位符
        for (loreLine in lore) {
            val processedLine = replacePlaceholders(loreLine, player, spawner)
            processedLore.add(colorize(processedLine))
        }

        meta.lore = processedLore
        item.itemMeta = meta
        return item
    }

    private fun createEntityItem(section: ConfigurationSection, entityType: EntityType, player: Player): ItemStack {
        // 使用对应的怪物蛋材质
        val material = getSpawnEggMaterial(entityType)
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        // 获取实体显示名称 - 优先级：entity_spawn_egg.yml > entities配置 > 默认英文名
        val displayName = resolveEntityDisplayName(entityType, section)
        meta.setDisplayName(colorize(displayName))

        // 获取价格和解锁状态
        val price = section.getDouble("price", 0.0)
        val pointPrice = section.getInt("point", 0)
        val requireUnlock = section.getBoolean("require_unlock", true)
        val enabled = section.getBoolean("enabled", true)

        // 检查玩家是否已解锁
        val unlocked = !requireUnlock || hasUnlockedEntity(player, entityType)

        val lore = section.getStringList("lore")
        val processedLore = mutableListOf<String>()
        for (line in lore) {
            // 过滤掉包含"用刷怪蛋交互时"的lore行（隐藏flag信息）
            if (!line.contains("用刷怪蛋交互时") && !line.contains("spawn_egg_interact")) {
                processedLore.add(colorize(line))
            }
        }

        // 添加价格和状态信息
        processedLore.add("")
        if (!enabled) {
            processedLore.add("§c✘ 已禁用")
        } else if (unlocked) {
            processedLore.add("§a✔ 已解锁")
        } else {
            // 双重支付系统显示
            if (pointPrice > 0) {
                processedLore.add("§e金币价格: §f${String.format("%.2f", price)}")
                processedLore.add("§b积分价格: §f${pointPrice}")
            } else {
                processedLore.add("§e价格: §f${String.format("%.2f", price)}")
            }
            processedLore.add("§c✘ 未解锁")
            processedLore.add("§7点击购买解锁")
        }

        meta.lore = processedLore
        item.itemMeta = meta
        return item
    }


    private fun getSpawnEggMaterial(entityType: EntityType): Material {
        val eggName = "${entityType.name}_SPAWN_EGG"
        return try {
            Material.valueOf(eggName)
        } catch (e: IllegalArgumentException) {
            // 如果没有对应的怪物蛋，返回默认材质
            Material.SPAWNER
        }
    }

    private fun hasUnlockedEntity(player: Player, entityType: EntityType): Boolean {
        val spawner = selectedSpawners[player.uniqueId] ?: return false
        val data = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getDataManager()?.getPlayerData(player.uniqueId)
        
        // 检查该实体类型是否在对应的刷怪笼类型中已解锁
        return data != null && data.hasUnlockedEntity(spawner.type, entityType)
    }

    fun handleEntityClick(player: Player, slot: Int) {
        val spawner = selectedSpawners[player.uniqueId] ?: return

        // 获取实体配置
        val entityConfig = if (spawner.type == SpawnerType.NORMAL) {
            (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getConfigManager()?.getNormalEntities()
        } else {
            (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getConfigManager()?.getPremiumEntities()
        }

        if (entityConfig == null) {
            return
        }

        val entitiesSection = entityConfig.getConfigurationSection("entities")
        if (entitiesSection == null) {
            return
        }

        // 找到对应slot的实体
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

    private fun handleEntitySelection(player: Player, spawner: Spawner, entityType: EntityType, entityConfig: ConfigurationSection) {
        val enabled = entityConfig.getBoolean("enabled", true)
        if (!enabled) {
            player.sendMessage("§c该生物已被禁用！")
            return
        }

        // 检查spawn_conditions（Y坐标条件）- 只有在没有bypass权限时才检查
        if (!player.hasPermission("mcw.bypass.conditions")) {
            if (entityConfig.contains("spawn_conditions")) {
                val spawnConditions = entityConfig.getConfigurationSection("spawn_conditions")
                
                if (spawnConditions != null) {
                    val conditionChecker = org.Aauu.monsterCapitalWorks.util.ConditionChecker(plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)
                    if (!conditionChecker.checkYConditions(spawner.location, spawnConditions)) {
                        player.sendMessage("§c该生物不满足当前刷怪笼位置的Y坐标条件！")

                        // 提供详细的错误信息
                        if (spawnConditions.contains("min_y")) {
                            val minY = spawnConditions.getInt("min_y")
                            player.sendMessage("§7需要: Y坐标 >= §e$minY")
                        }
                        if (spawnConditions.contains("max_y")) {
                            val maxY = spawnConditions.getInt("max_y")
                            player.sendMessage("§7需要: Y坐标 <= §e$maxY")
                        }
                        player.sendMessage("§7当前刷怪笼Y坐标: §e${spawner.location.blockY}")
                        return
                    }
                }
            }
        }

        // 检查spawn_condition条件（PAPI条件）
        if (entityConfig.contains("spawn_condition")) {
            val condition = entityConfig.getString("spawn_condition")
            if (!checkSpawnCondition(player, spawner.location, condition)) {
                player.sendMessage("§c当前环境不满足生成条件！")
                return
            }
        }

        val requireUnlock = entityConfig.getBoolean("require_unlock", true)
        val data = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getDataManager()?.getPlayerData(player.uniqueId)
        val unlocked = !requireUnlock || (data != null && data.hasUnlockedEntity(spawner.type, entityType))

        if (!unlocked) {
            val price = entityConfig.getDouble("price", 0.0)
            val pointPrice = entityConfig.getInt("point", 0)
            
            // 获取PlayerPoints和Economy管理器
            val playerPointsHook = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getPlayerPointsHook()
            val economy = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getEconomyManager()

            when {
                // 双重支付：同时需要金币和积分
                pointPrice > 0 && price > 0.0 -> {
                    val hasEnoughPoints = playerPointsHook?.isAvailable() == true && 
                                        playerPointsHook.hasPoints(player, pointPrice)
                    val hasEnoughMoney = economy != null && economy.hasEconomy() && 
                                       economy.has(player, price)
                    
                    when {
                        // 两者都足够
                        hasEnoughPoints && hasEnoughMoney -> {
                            player.sendMessage("§c该生物需要同时支付金币和积分！")
                            player.sendMessage("§7金币需求: §f${String.format("%.2f", price)}")
                            player.sendMessage("§7积分需求: §f$pointPrice")
                            player.sendMessage("§7请确保你有足够的金币和积分")
                        }
                        // 只有积分足够
                        hasEnoughPoints && !hasEnoughMoney -> {
                            player.sendMessage("§c该生物需要同时支付金币和积分！")
                            player.sendMessage("§7金币需求: §f${String.format("%.2f", price)}")
                            player.sendMessage("§7积分需求: §f$pointPrice")
                            player.sendMessage("§c你的金币不足！需要: §f${String.format("%.2f", price)}")
                        }
                        // 只有金币足够
                        !hasEnoughPoints && hasEnoughMoney -> {
                            player.sendMessage("§c该生物需要同时支付金币和积分！")
                            player.sendMessage("§7金币需求: §f${String.format("%.2f", price)}")
                            player.sendMessage("§7积分需求: §f$pointPrice")
                            player.sendMessage("§c你的积分不足！需要: §f$pointPrice")
                        }
                        // 两者都不足
                        else -> {
                            player.sendMessage("§c该生物需要同时支付金币和积分！")
                            player.sendMessage("§7金币需求: §f${String.format("%.2f", price)}")
                            player.sendMessage("§7积分需求: §f$pointPrice")
                            player.sendMessage("§c你的金币和积分都不足！")
                        }
                    }
                }
                // 只有积分支付
                pointPrice > 0 && price == 0.0 -> {
                    if (playerPointsHook?.isAvailable() == true && playerPointsHook.hasPoints(player, pointPrice)) {
                        playerPointsHook.takePoints(player, pointPrice)
                        if (data != null) {
                            data.unlockEntity(spawner.type, entityType)
                            (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getDataManager().savePlayerData(data)
                        }
                        openEntityMenu(player, spawner)
                        player.sendMessage("§a成功解锁 ${entityType.name}！花费: §f$pointPrice 积分")
                    } else {
                        player.sendMessage("§c积分不足！需要: §f$pointPrice")
                        if (playerPointsHook?.isAvailable() != true) {
                            player.sendMessage("§7提示: 需要安装PlayerPoints插件才能使用积分支付")
                        }
                    }
                }
                // 只有金币支付
                pointPrice == 0 && price > 0.0 -> {
                    if (economy != null && economy.hasEconomy() && economy.has(player, price)) {
                        economy.withdraw(player, price)
                        if (data != null) {
                            data.unlockEntity(spawner.type, entityType)
                            (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getDataManager().savePlayerData(data)
                        }
                        openEntityMenu(player, spawner)
                        player.sendMessage("§a成功解锁 ${entityType.name}！花费: §f${String.format("%.2f", price)}")
                    } else {
                        player.sendMessage("§c金币不足！需要: §f${String.format("%.2f", price)}")
                        if (economy == null || !economy.hasEconomy()) {
                            player.sendMessage("§7提示: 需要安装Vault插件才能使用金钱支付")
                        }
                    }
                }
                // 免费解锁
                else -> {
                    if (data != null) {
                        data.unlockEntity(spawner.type, entityType)
                        (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getDataManager().savePlayerData(data)
                    }
                    openEntityMenu(player, spawner)
                    player.sendMessage("§a成功解锁 ${entityType.name}！")
                }
            }
        } else {
            spawner.entityType = entityType
            spawner.updateStoredSpawns(0) // 清空存储的生物
            spawner.storedSpawnsReleased = 0 // 重置已释放的生物数量
            spawner.lastReleaseTime = System.currentTimeMillis() // 重置释放时间
            
            // 更新Bukkit刷怪笼的显示
            val block = spawner.location.block
            if (block.type == Material.SPAWNER) {
                val cs = block.state as org.bukkit.block.CreatureSpawner
                cs.spawnedType = entityType
                cs.delay = spawner.spawnDelay
                cs.maxNearbyEntities = spawner.maxNearbyEntities
                cs.requiredPlayerRange = spawner.activationRange
                cs.spawnCount = spawner.spawnCount
                cs.update()
            }
            
            (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getSpawnerManager().updateSpawner(spawner)
            player.closeInventory()
            player.sendMessage("§a已切换刷怪笼生物类型！")
        }
    }

    /**
     * 检查生成条件
     */
    private fun checkSpawnCondition(player: Player, location: Location, condition: String?): Boolean {
        if (condition == null || condition.isEmpty()) {
            return true
        }

        // 替换PlaceholderAPI占位符
        var processedCondition = condition
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            processedCondition = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, condition)

            // 如果条件中包含刷怪笼位置相关的占位符，需要特殊处理
            // 例如：%world_biome% 需要在刷怪笼位置检查
            if (condition.contains("%world_biome%") || condition.contains("%biome%")) {
                // 获取刷怪笼位置的生物群系
                val biome = location.block.biome.key.key.lowercase()
                processedCondition = processedCondition.replace("%world_biome%", biome)
                processedCondition = processedCondition.replace("%biome%", biome)
            }

            if (condition.contains("%world_name%")) {
                val worldName = location.world.name
                processedCondition = processedCondition.replace("%world_name%", worldName)
            }
        }

        // 评估条件表达式
        return evaluateCondition(processedCondition)
    }

    /**
     * 评估条件表达式
     */
    private fun evaluateCondition(condition: String): Boolean {
        return try {
            // 处理常见的比较运算符
            val trimmedCondition = condition.trim()

            // 处理 == 运算符
            if (trimmedCondition.contains("==")) {
                val parts = trimmedCondition.split("==")
                if (parts.size == 2) {
                    val left = parts[0].trim()
                    val right = parts[1].trim()
                    return left.equals(right, ignoreCase = true)
                }
            }

            // 处理 != 运算符
            if (trimmedCondition.contains("!=")) {
                val parts = trimmedCondition.split("!=")
                if (parts.size == 2) {
                    val left = parts[0].trim()
                    val right = parts[1].trim()
                    return !left.equals(right, ignoreCase = true)
                }
            }

            // 处理 > 运算符
            if (trimmedCondition.contains(">") && !trimmedCondition.contains(">=")) {
                val parts = trimmedCondition.split(">")
                if (parts.size == 2) {
                    try {
                        val left = parts[0].trim().toDouble()
                        val right = parts[1].trim().toDouble()
                        return left > right
                    } catch (e: NumberFormatException) {
                        return false
                    }
                }
            }

            // 处理 >= 运算符
            if (trimmedCondition.contains(">=")) {
                val parts = trimmedCondition.split(">=")
                if (parts.size == 2) {
                    try {
                        val left = parts[0].trim().toDouble()
                        val right = parts[1].trim().toDouble()
                        return left >= right
                    } catch (e: NumberFormatException) {
                        return false
                    }
                }
            }

            // 处理 < 运算符
            if (trimmedCondition.contains("<") && !trimmedCondition.contains("<=")) {
                val parts = trimmedCondition.split("<")
                if (parts.size == 2) {
                    try {
                        val left = parts[0].trim().toDouble()
                        val right = parts[1].trim().toDouble()
                        return left < right
                    } catch (e: NumberFormatException) {
                        return false
                    }
                }
            }

            // 处理 <= 运算符
            if (trimmedCondition.contains("<=")) {
                val parts = trimmedCondition.split("<=")
                if (parts.size == 2) {
                    try {
                        val left = parts[0].trim().toDouble()
                        val right = parts[1].trim().toDouble()
                        return left <= right
                    } catch (e: NumberFormatException) {
                        return false
                    }
                }
            }

            // 如果没有运算符，尝试解析为布尔值
            return trimmedCondition.toBoolean()

        } catch (e: Exception) {
            plugin.logger.warning("无法评估条件: $condition - ${e.message}")
            false
        }
    }

    private fun replacePlaceholders(text: String, player: Player, spawner: Spawner?): String {
        if (text == null || spawner == null) {
            return text
        }

        val upgradeManager = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getUpgradeManager()

        var result = text

        // 基本信息
        result = result.replace("%mcw_type%", spawner.type.name)
        result = result.replace("%mcw_entity%", spawner.entityType.name)
        result = result.replace("%mcw_location%", formatLocation(spawner.location))
        result = result.replace("%mcw_active%", if (spawner.active) "§a激活" else "§c未激活")
        result = result.replace("%mcw_status%", if (spawner.active) "§a开启" else "§c关闭")
        result = result.replace("%mcw_stored%", spawner.storedSpawns.toString())

        // 生成模式信息
        val spawnModeText = if (spawner.spawnMode == SpawnMode.RANDOM) "§e随机模式" else "§b精确模式"
        result = result.replace("%mcw_spawn_mode%", spawnModeText)
        if (spawner.spawnMode == SpawnMode.PRECISE) {
            result = result.replace("%mcw_precise_pos%", String.format("§7X:§e%.1f §7Y:§e%.1f §7Z:§e%.1f", 
                spawner.preciseX, spawner.preciseY, spawner.preciseZ))
        } else {
            result = result.replace("%mcw_precise_pos%", "§7未设置")
        }

        // 精确位置单独的占位符
        result = result.replace("%mcw_precise_x%", String.format("%.1f", spawner.preciseX))
        result = result.replace("%mcw_precise_y%", String.format("%.1f", spawner.preciseY))
        result = result.replace("%mcw_precise_z%", String.format("%.1f", spawner.preciseZ))

        // 升级等级
        result = result.replace("%mcw_speed_level%", spawner.getUpgradeLevel("speed").toString())
        result = result.replace("%mcw_count_level%", spawner.getUpgradeLevel("count").toString())
        result = result.replace("%mcw_max_nearby_level%", spawner.getUpgradeLevel("max_nearby").toString())
        result = result.replace("%mcw_range_level%", spawner.getUpgradeLevel("range").toString())
        result = result.replace("%mcw_storage_level%", spawner.getUpgradeLevel("storage").toString())

        // 当前值
        result = result.replace("%mcw_speed_value%", spawner.spawnDelay.toString())
        result = result.replace("%mcw_count_value%", spawner.spawnCount.toString())
        result = result.replace("%mcw_max_nearby_value%", spawner.maxNearbyEntities.toString())
        result = result.replace("%mcw_range_value%", spawner.activationRange.toString())
        result = result.replace("%mcw_storage_value%", spawner.maxStorage.toString())
        
        // 已存储数量
        result = result.replace("%mcw_stored%", spawner.storedSpawns.toString())

        // 下一级值
        if (upgradeManager != null) {
            result = result.replace("%mcw_speed_next_value%", upgradeManager.getNextUpgradeValue(spawner, "speed").toInt().toString())
            result = result.replace("%mcw_count_next_value%", upgradeManager.getNextUpgradeValue(spawner, "count").toInt().toString())
            result = result.replace("%mcw_max_nearby_next_value%", upgradeManager.getNextUpgradeValue(spawner, "max_nearby").toInt().toString())
            result = result.replace("%mcw_range_next_value%", upgradeManager.getNextUpgradeValue(spawner, "range").toInt().toString())
            result = result.replace("%mcw_storage_next_value%", upgradeManager.getNextUpgradeValue(spawner, "storage").toInt().toString())

            // 升级费用
            result = result.replace("%mcw_speed_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "speed")))
            result = result.replace("%mcw_count_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "count")))
            result = result.replace("%mcw_max_nearby_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "max_nearby")))
            result = result.replace("%mcw_range_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "range")))
            result = result.replace("%mcw_storage_cost%", String.format("%.2f", upgradeManager.getUpgradeCost(spawner, "storage")))

            // 前置需求信息
            result = result.replace("%mcw_speed_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "speed"))
            result = result.replace("%mcw_count_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "count"))
            result = result.replace("%mcw_max_nearby_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "max_nearby"))
            result = result.replace("%mcw_range_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "range"))
            result = result.replace("%mcw_storage_required%", upgradeManager.getRequiredUpgradesInfo(spawner, "storage"))
        }

        // 玩家数据
        val spawnerManager = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getSpawnerManager()
        val permissionManager = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getPermissionManager()
        val dataManager = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getDataManager()
        val economy = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getEconomyManager()
        
        if (spawnerManager != null && permissionManager != null && dataManager != null) {
            val placed = spawnerManager.getPlayerSpawnerCount(player.uniqueId, spawner.type)
            val limit = permissionManager.getSpawnerLimit(player, spawner.type)
            val playerData = dataManager.getPlayerData(player.uniqueId)
            val purchased = playerData?.getPurchasedLimit(spawner.type) ?: 0

            // 基础限制和权限限制
            val baseLimit = (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).config.getInt("limits.${spawner.type.name.lowercase()}.base", 5)
            val permLimit = permissionManager.getSpawnerLimit(player, spawner.type) - baseLimit

            result = result.replace("%mcw_placed%", placed.toString())
            result = result.replace("%mcw_limit%", limit.toString())
            result = result.replace("%mcw_purchased%", purchased.toString())
            result = result.replace("%mcw_base_limit%", baseLimit.toString())
            result = result.replace("%mcw_perm_limit%", permLimit.toString())

            // 购买费用占位符（使用新的计算方法）
            result = result.replace("%mcw_buy_1_cost%", String.format("%.2f", calculatePurchaseCostForDisplay(player, spawner.type, 1)))
            result = result.replace("%mcw_buy_5_cost%", String.format("%.2f", calculatePurchaseCostForDisplay(player, spawner.type, 5)))
            result = result.replace("%mcw_buy_10_cost%", String.format("%.2f", calculatePurchaseCostForDisplay(player, spawner.type, 10)))
            
            // 添加类型特定的占位符
            val typeName = spawner.type.name.lowercase()
            result = result.replace("%mcw_limit_${typeName}%", limit.toString())
            result = result.replace("%mcw_purchased_${typeName}%", purchased.toString())
            result = result.replace("%mcw_spawners_${typeName}%", placed.toString())
            result = result.replace("%mcw_buy_1_cost_${typeName}%", String.format("%.2f", calculatePurchaseCostForDisplay(player, spawner.type, 1)))
            result = result.replace("%mcw_buy_5_cost_${typeName}%", String.format("%.2f", calculatePurchaseCostForDisplay(player, spawner.type, 5)))
            result = result.replace("%mcw_buy_10_cost_${typeName}%", String.format("%.2f", calculatePurchaseCostForDisplay(player, spawner.type, 10)))
            
            // 玩家金钱占位符
            if (economy != null && economy.hasEconomy()) {
                val balance = economy.getBalance(player)
                result = result.replace("%mcw_money%", String.format("%.2f", balance))
            } else {
                result = result.replace("%mcw_money%", "§c经济系统未启用")
            }
        }

        return result
    }

    private fun formatLocation(location: Location): String {
        return String.format("%d, %d, %d", 
                location.blockX, 
                location.blockY, 
                location.blockZ)
    }

    private fun colorize(text: String): String {
        if (text == null) {
            return ""
        }

        // 处理 RGB 颜色格式 {#RRGGBB}
        val hexPattern = java.util.regex.Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}")
        val matcher = hexPattern.matcher(text)
        val buffer = StringBuffer()

        while (matcher.find()) {
            val hexColor = matcher.group(1)
            val replacement = StringBuilder("§x")

            // 将每个十六进制字符转换为 §x§r§r§g§g§b§b 格式
            for (c in hexColor.toCharArray()) {
                replacement.append("§").append(c)
            }

            matcher.appendReplacement(buffer, replacement.toString())
        }
        matcher.appendTail(buffer)

        // 处理传统颜色代码 &
        return buffer.toString().replace('&', '§')
    }

    fun getOpenGui(player: UUID): String? = openGuis[player]

    fun getSelectedSpawner(player: UUID): Spawner? = selectedSpawners[player]

    fun closeGui(player: UUID) {
        openGuis.remove(player)
        selectedSpawners.remove(player)
    }

    fun handleClick(player: Player, slot: Int, guiName: String, isRightClick: Boolean = false, isShiftClick: Boolean = false) {
        val spawner = selectedSpawners[player.uniqueId] ?: return

        val guiConfig = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getConfigManager()?.getGuiConfig(guiName) ?: return

        // 支持新的 layout/icons 格式
        if (guiConfig.contains("layout") && guiConfig.contains("icons")) {
            val layout = guiConfig.getStringList("layout")
            val iconsSection = guiConfig.getConfigurationSection("icons")

            if (!layout.isEmpty() && iconsSection != null) {
                var currentSlot = 0
                for (row in layout) {
                    for (iconChar in row.toCharArray()) {
                        if (currentSlot == slot) {
                            val iconKey = iconChar.toString()
                            val iconSection = iconsSection.getConfigurationSection(iconKey)

                            if (iconSection != null) {
                                val actions = if (isRightClick && iconSection.contains("right_click_actions")) {
                                    iconSection.getStringList("right_click_actions")
                                } else {
                                    iconSection.getStringList("actions")
                                }
                                executeActions(player, spawner, actions, isRightClick, isShiftClick)
                            }
                            return
                        }
                        currentSlot++
                    }
                }
            }
        } else {
            // 兼容旧的 items 格式
            val itemsSection = guiConfig.getConfigurationSection("items")
            if (itemsSection == null) {
                return
            }

            // 查找点击的物品
            for (key in itemsSection.getKeys(false)) {
                val itemSection = itemsSection.getConfigurationSection(key)
                if (itemSection != null && itemSection.getInt("slot", -1) == slot) {
                    // 根据点击类型选择不同的动作列表
                    val actions = if (isRightClick && itemSection.contains("right_click_actions")) {
                        itemSection.getStringList("right_click_actions")
                    } else {
                        itemSection.getStringList("actions")
                    }
                    executeActions(player, spawner, actions, isRightClick, isShiftClick)
                    break
                }
            }
        }
    }

    private fun executeActions(player: Player, spawner: Spawner, actions: List<String>, isRightClick: Boolean, isShiftClick: Boolean) {
        for (action in actions) {
            when {
                action.startsWith("open_gui:") -> {
                    val guiName = action.substring(9).trim()
                    openGui(player, spawner, guiName)
                }
                action == "close" -> {
                    player.closeInventory()
                }
                action.startsWith("command:") -> {
                    // 玩家身份执行命令
                    val command = action.substring(8).trim()
                    val finalCommand = replacePlaceholders(command, player, spawner)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        player.performCommand(finalCommand)
                    })
                }
                action.startsWith("op:") -> {
                    // OP身份执行命令（临时授予OP）
                    val command = action.substring(3).trim()
                    executeOpCommand(player, command)
                }
                action.startsWith("console:") -> {
                    // 控制台执行命令
                    val command = action.substring(8).trim()
                    executeConsoleCommand(player, command)
                }
                action.startsWith("upgrade:") -> {
                    val upgradeName = action.substring(8).trim()
                    (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getUpgradeManager().upgradeSpawner(player, spawner, upgradeName)
                    // 刷新GUI
                    val currentGui = openGuis[player.uniqueId]
                    if (currentGui != null) {
                        openGui(player, spawner, currentGui)
                    }
                }
                action == "toggle_active" -> {
                    spawner.active = !spawner.active
                    (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getSpawnerManager().updateSpawner(spawner)
                    val currentGui = openGuis[player.uniqueId]
                    if (currentGui != null) {
                        openGui(player, spawner, currentGui)
                    }
                }
                action == "toggle_spawn_mode" -> {
                    // 检查精确模式权限
                    val permissionManager = (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getPermissionManager()
                    if (spawner.spawnMode == SpawnMode.RANDOM && !permissionManager.canUsePreciseMode(player, spawner.type)) {
                        player.sendMessage("§c你没有权限使用精确模式！")
                        if (spawner.type == SpawnerType.PREMIUM) {
                            player.sendMessage("§7需要权限: §emcw.spawnmode.precise")
                        }
                        return
                    }
                    
                    spawner.spawnMode = if (spawner.spawnMode == SpawnMode.RANDOM) SpawnMode.PRECISE else SpawnMode.RANDOM
                    (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getSpawnerManager().updateSpawner(spawner)
                    val currentGui = openGuis[player.uniqueId]
                    if (currentGui != null) {
                        openGui(player, spawner, currentGui)
                    }
                }
                action == "set_precise_pos" -> {
                    if (player.hasPermission("mcw.spawnmode.precise")) {
                        val playerLoc = player.location
                        val spawnerLoc = spawner.location

                        // 计算从刷怪笼到玩家的绝对坐标偏移
                        // 这样设置后，怪物将在玩家当前位置生成
                        val offsetX = playerLoc.x - spawnerLoc.x
                        val offsetY = playerLoc.y - spawnerLoc.y
                        val offsetZ = playerLoc.z - spawnerLoc.z

                        spawner.preciseX = offsetX
                        spawner.preciseY = offsetY
                        spawner.preciseZ = offsetZ
                        spawner.spawnMode = SpawnMode.PRECISE
                        (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getSpawnerManager().updateSpawner(spawner)
                        
                        player.sendMessage("§a已设置精确生成位置！")
                        player.sendMessage("§7怪物将在以下位置生成:")
                        player.sendMessage("§7坐标: §e${String.format("%.1f", playerLoc.x)}, ${String.format("%.1f", playerLoc.y)}, ${String.format("%.1f", playerLoc.z)}")
                        player.sendMessage("§7相对刷怪笼偏移: §eX=${String.format("%.1f", offsetX)}, Y=${String.format("%.1f", offsetY)}, Z=${String.format("%.1f", offsetZ)}")
                        
                        val currentGui = openGuis[player.uniqueId]
                        if (currentGui != null) {
                            openGui(player, spawner, currentGui)
                        }
                    } else {
                        player.sendMessage((plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getConfigManager().getMessage("error.no_permission"))
                    }
                }
                action.startsWith("adjust_pos:") -> {
                    if (player.hasPermission("mcw.spawnmode.precise")) {
                        val parts = action.substring(11).trim().split(" ")
                        if (parts.size >= 2) {
                            try {
                                val axis = parts[0]
                                val amount = parts[1].toDouble()
                                val activationRange = spawner.activationRange
                                var validPosition = true

                                when (axis.lowercase()) {
                                    "x" -> {
                                        if (amount == 0.0) {
                                            spawner.preciseX = 0.0
                                        } else {
                                            val newX = spawner.preciseX + amount
                                            if (Math.abs(newX) <= activationRange) {
                                                spawner.preciseX = newX
                                            } else {
                                                validPosition = false
                                                player.sendMessage("§c错误: X轴偏移不能超过激活范围 ±$activationRange 格")
                                            }
                                        }
                                    }
                                    "y" -> {
                                        if (amount == 0.0) {
                                            spawner.preciseY = 0.0
                                        } else {
                                            val newY = spawner.preciseY + amount
                                            if (Math.abs(newY) <= activationRange) {
                                                spawner.preciseY = newY
                                            } else {
                                                validPosition = false
                                                player.sendMessage("§c错误: Y轴偏移不能超过激活范围 ±$activationRange 格")
                                            }
                                        }
                                    }
                                    "z" -> {
                                        if (amount == 0.0) {
                                            spawner.preciseZ = 0.0
                                        } else {
                                            val newZ = spawner.preciseZ + amount
                                            if (Math.abs(newZ) <= activationRange) {
                                                spawner.preciseZ = newZ
                                            } else {
                                                validPosition = false
                                                player.sendMessage("§c错误: Z轴偏移不能超过激活范围 ±$activationRange 格")
                                            }
                                        }
                                    }
                                    "reset" -> {
                                        spawner.preciseX = 0.0
                                        spawner.preciseY = 0.0
                                        spawner.preciseZ = 0.0
                                    }
                                }

                                if (validPosition) {
                                    spawner.spawnMode = SpawnMode.PRECISE
                                    (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getSpawnerManager().updateSpawner(spawner)
                                }
                                val currentGui = openGuis[player.uniqueId]
                                if (currentGui != null) {
                                    openGui(player, spawner, currentGui)
                                }
                            } catch (ignored: NumberFormatException) {
                            }
                        }
                    } else {
                        player.sendMessage((plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getConfigManager().getMessage("error.no_permission"))
                    }
                }
                action.startsWith("purchase:") -> {
                    val parts = action.substring(9).trim().split(" ")
                    if (parts.size >= 2 && parts[0] == "limit") {
                        try {
                            val amount = parts[1].toInt()
                            // 检查是否允许购买
                            if (spawner.type == SpawnerType.PREMIUM) {
                                player.sendMessage("§c付费刷怪笼只能通过指令增加放置上限！")
                                return
                            }
                            handlePurchaseLimit(player, spawner.type, amount)
                            val currentGui = openGuis[player.uniqueId]
                            if (currentGui != null) {
                                openGui(player, spawner, currentGui)
                            }
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
        }
    }

    private fun handlePurchaseLimit(player: Player, type: SpawnerType, amount: Int) {
        // 从对应的配置文件获取购买设置
        val config = when (type) {
            SpawnerType.NORMAL -> (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getConfigManager().getNormalConfig()
            SpawnerType.PREMIUM -> (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getConfigManager().getPremiumConfig()
        } ?: return

        val typePath = "${type.name}.limits"

        // 付费刷怪笼只能通过指令管理，普通刷怪笼默认启用购买
        if (type == SpawnerType.PREMIUM) {
            player.sendMessage("§c付费刷怪笼只能通过指令增加放置上限！")
            return
        }

        // 获取玩家当前已购买数量
        val data = (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getDataManager().getPlayerData(player.uniqueId) ?: return
        val currentPurchased = data.getPurchasedLimit(type)
        // 对于付费刷怪笼，不限制最大可购买数量（通过指令管理）
        // 对于普通刷怪笼，使用默认限制
        val maxPurchasable = if (type == SpawnerType.PREMIUM) {
            Int.MAX_VALUE // 付费刷怪笼无限制
        } else {
            config.getInt("economy.max_purchasable", 50) // 普通刷怪笼限制
        }

        // 检查是否超过最大可购买数量
        if (currentPurchased + amount > maxPurchasable) {
            player.sendMessage("§c购买失败！最多只能购买 $maxPurchasable 个额外位置，你已购买 $currentPurchased 个。")
            return
        }

        // 计算总费用
        val totalCost = calculatePurchaseCost(type, currentPurchased, amount)

        // 优先使用PlayerPoints，如果没有则使用Vault经济
        val playerPointsHook = (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getPlayerPointsHook()
        val economy = (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getEconomyManager()
        
        when {
            // PlayerPoints可用且有足够积分
            playerPointsHook?.isAvailable() == true && playerPointsHook.hasPoints(player, totalCost.toInt()) -> {
                playerPointsHook.takePoints(player, totalCost.toInt())
                data.addPurchasedLimit(type, amount)
                (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getDataManager().savePlayerData(data)
                player.sendMessage("§a成功购买 $amount 个额外位置！花费: §e${totalCost.toInt()} 积分")
            }
            // 使用Vault经济
            economy != null && economy.hasEconomy() && economy.has(player, totalCost) -> {
                economy.withdraw(player, totalCost)
                data.addPurchasedLimit(type, amount)
                (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getDataManager().savePlayerData(data)
                player.sendMessage("§a成功购买 $amount 个额外位置！花费: §e${String.format("%.2f", totalCost)}")
            }
            // 都不可用
            else -> {
                player.sendMessage("§c购买失败！没有可用的支付方式。")
                if (playerPointsHook?.isAvailable() != true) {
                    player.sendMessage("§7提示: 需要安装PlayerPoints插件才能使用积分支付")
                }
                if (economy == null || !economy.hasEconomy()) {
                    player.sendMessage("§7提示: 需要安装Vault插件才能使用金钱支付")
                }
            }
        }
    }

    /**
     * 计算购买费用
     */
    private fun calculatePurchaseCost(type: SpawnerType, currentPurchased: Int, amount: Int): Double {
        // 从对应的配置文件获取购买设置
        val config = when (type) {
            SpawnerType.NORMAL -> (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getConfigManager().getNormalConfig()
            SpawnerType.PREMIUM -> (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getConfigManager().getPremiumConfig()
        } ?: return 0.0

        val typePath = "${type.name}.economy"

        val basePrice = config.getDouble("$typePath.base_price", 1000.0)
        val priceMode = config.getString("$typePath.price_mode", "multiplier")
        val multiplier = config.getDouble("$typePath.price_multiplier", 1.2)

        var totalCost = 0.0

        if (priceMode.equals("fixed", ignoreCase = true)) {
            // 固定价格模式：每个位置价格相同
            totalCost = basePrice * amount
        } else {
            // 倍率递增模式：每次购买后价格递增
            for (i in 0 until amount) {
                // 计算第 (currentPurchased + i + 1) 个位置的价格
                // 公式: basePrice * (multiplier ^ currentPurchased)
                val price = basePrice * Math.pow(multiplier, (currentPurchased + i).toDouble())
                totalCost += price
            }
        }

        return totalCost
    }

    /**
     * 计算购买指定数量的费用（用于占位符显示）
     */
    private fun calculatePurchaseCostForDisplay(player: Player, type: SpawnerType, amount: Int): Double {
        val data = (plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).getDataManager().getPlayerData(player.uniqueId)
        if (data == null) {
            return 0.0
        }

        val currentPurchased = data.getPurchasedLimit(type)
        return calculatePurchaseCost(type, currentPurchased, amount)
    }

    private fun openGui(player: Player, spawner: Spawner, guiName: String) {
        // 处理动态菜单名称（包含占位符）
        val processedGuiName = replacePlaceholders(guiName, player, spawner)
        
        when {
            processedGuiName.startsWith("main_menu_") -> openMainMenu(player, spawner)
            processedGuiName == "upgrade_menu" -> openUpgradeMenu(player, spawner)
            processedGuiName == "entity_menu" -> openEntityMenu(player, spawner)
            processedGuiName == "buy_limit_menu" -> openBuyLimitMenu(player, spawner)
            processedGuiName == "pos_menu" -> openPrecisePosMenu(player, spawner)
            else -> {
                // 兼容旧的固定菜单名称
                when (guiName) {
                    "main_menu_normal", "main_menu_premium" -> openMainMenu(player, spawner)
                    "upgrade_menu" -> openUpgradeMenu(player, spawner)
                    "entity_menu" -> openEntityMenu(player, spawner)
                    "buy_limit_menu" -> openBuyLimitMenu(player, spawner)
                    "pos_menu" -> openPrecisePosMenu(player, spawner)
                }
            }
        }
    }

    /**
     * 解析实体显示名称
     * 优先级：entities配置的name字段 > entity_spawn_egg.yml > 默认英文名
     */
    private fun resolveEntityDisplayName(entityType: EntityType, section: ConfigurationSection?): String {
        // 优先级1：从entities配置的name字段读取（最高优先级）
        section?.getString("name")?.let { customName ->
            return customName
        }
        
        // 优先级2：从entity_spawn_egg.yml的entity_names中读取
        val configManager = (plugin as? org.Aauu.monsterCapitalWorks.MonsterCapitalWorks)?.getConfigManager()
        if (configManager != null) {
            val entitySpawnEggConfig = configManager.getEntitySpawnEggNames()
            entitySpawnEggConfig?.getString("entity_names.${entityType.name}")?.let { eggName ->
                return eggName
            }
        }
        
        // 优先级3：使用默认英文名
        return entityType.name
    }

    /**
     * 以OP身份执行命令（临时授予OP权限）
     */
    private fun executeOpCommand(player: Player, command: String) {
        if (command.isEmpty()) {
            return
        }

        try {
            // 替换占位符
            val finalCommand = replacePlaceholders(command, player, selectedSpawners[player.uniqueId])

            // 在主线程执行
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val wasOp = player.isOp

                try {
                    // 临时授予OP权限
                    if (!wasOp) {
                        player.isOp = true
                    }

                    // 执行命令
                    player.performCommand(finalCommand)

                    if ((plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).config.getBoolean("debug", false)) {
                        plugin.logger.info("玩家 ${player.name} 以OP身份执行命令: /$finalCommand")
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("以OP身份执行命令时出错: $finalCommand")
                    plugin.logger.warning(e.message)
                } finally {
                    // 确保撤销OP权限（如果玩家原本不是OP）
                    if (!wasOp && player.isOp) {
                        player.isOp = false
                    }
                }
            })
        } catch (e: Exception) {
            plugin.logger.warning("准备OP命令时出错: $command")
            plugin.logger.warning(e.message)
        }
    }

    /**
     * 以控制台身份执行命令
     */
    private fun executeConsoleCommand(player: Player, command: String) {
        if (command.isEmpty()) {
            return
        }

        try {
            // 替换占位符
            var processedCommand = replacePlaceholders(command, player, selectedSpawners[player.uniqueId])

            // 替换玩家名称占位符
            processedCommand = processedCommand.replace("%player%", player.name)
            processedCommand = processedCommand.replace("%player_name%", player.name)
            processedCommand = processedCommand.replace("%player_uuid%", player.uniqueId.toString())

            val finalCommand = processedCommand

            // 在主线程以控制台身份执行命令
            Bukkit.getScheduler().runTask(plugin, Runnable {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand)

                    if ((plugin as org.Aauu.monsterCapitalWorks.MonsterCapitalWorks).config.getBoolean("debug", false)) {
                        plugin.logger.info("控制台执行命令: /$finalCommand")
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("控制台执行命令时出错: $finalCommand")
                    plugin.logger.warning(e.message)
                }
            })
        } catch (e: Exception) {
            plugin.logger.warning("准备控制台命令时出错: $command")
            plugin.logger.warning(e.message)
        }
    }
}
