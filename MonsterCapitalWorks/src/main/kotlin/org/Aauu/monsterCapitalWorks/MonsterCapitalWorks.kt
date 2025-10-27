package org.Aauu.monsterCapitalWorks

import org.Aauu.monsterCapitalWorks.async.AsyncTaskManager
import org.Aauu.monsterCapitalWorks.cache.CacheManager
import org.Aauu.monsterCapitalWorks.command.CommandRegistry
import org.Aauu.monsterCapitalWorks.command.MCWCommand
import org.Aauu.monsterCapitalWorks.command.impl.HelpCommand
import org.Aauu.monsterCapitalWorks.di.DependencyInjector
import org.Aauu.monsterCapitalWorks.hook.PlaceholderAPIHook
import org.Aauu.monsterCapitalWorks.hook.PlayerPointsHook
import org.Aauu.monsterCapitalWorks.listener.ListenerManager
import org.Aauu.monsterCapitalWorks.listener.SpawnerListener
import org.Aauu.monsterCapitalWorks.manager.*
import org.Aauu.monsterCapitalWorks.model.SpawnMode
import org.Aauu.monsterCapitalWorks.util.isSafeSpawnFor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.PluginCommand
import org.bukkit.entity.LivingEntity
import org.bukkit.plugin.java.JavaPlugin

class MonsterCapitalWorks : JavaPlugin() {
    
    companion object {
        @JvmStatic
        lateinit var instance: MonsterCapitalWorks
            private set
    }
    
    private var placeholderHook: PlaceholderAPIHook? = null
    private var playerPointsHook: PlayerPointsHook? = null

    override fun onEnable() {
        instance = this
        
        try {
            // 初始化核心系统
            initializeCoreSystems()
            
            // 保存默认配置文件
            saveDefaultResources()
            
            // 初始化依赖注入
            initializeDependencyInjection()
            
            // 注册命令和事件
            registerCommands()
            registerEvents()
            
            // 设置钩子
            setupHooks()
            
            // 启动刷怪笼生成任务
            startSpawnerTask()
            
            logger.info("MonsterCapitalWorks v${description.version} 已成功启动！")
            logger.info("数据库类型: ${getDataManager().databaseType}")
            logger.info("已注册 ${CommandRegistry.getRegisteredCommands().size} 个命令")
            logger.info("已注册 ${ListenerManager.getAllListeners().size} 个事件监听器")
            
        } catch (e: Exception) {
            logger.severe("插件启动失败: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        try {
            // 关闭异步任务管理器
            AsyncTaskManager.shutdown()
            
            // 关闭缓存管理器
            CacheManager.shutdown()
            
            // 关闭依赖注入服务
            DependencyInjector.shutdownServices()
            
            // 注销所有监听器
            ListenerManager.unregisterAll()
            
            logger.info("MonsterCapitalWorks 已安全关闭")
        } catch (e: Exception) {
            logger.severe("插件关闭时发生错误: ${e.message}")
        }
    }
    
    /**
     * 初始化核心系统
     */
    private fun initializeCoreSystems() {
        CacheManager
        AsyncTaskManager.initialize(this)
        ListenerManager.initialize(this)
    }
    
    /**
     * 初始化依赖注入
     */
    private fun initializeDependencyInjection() {
        DependencyInjector.initialize(this)
        DependencyInjector.initializeServices()
    }
    
    /**
     * 保存默认配置文件
     */
    private fun saveDefaultResources() {
        val resources = listOf(
            "config.yml",
            "messages.yml",
            "spawner/normal_config.yml",
            "spawner/premium_config.yml",
            "spawner/entity_spawn_egg.yml",
            "entities/normal_entities.yml",
            "entities/premium_entities.yml",
            "upgrades/normal_upgrades.yml",
            "upgrades/premium_upgrades.yml",
            "gui/main_menu_normal.yml",
            "gui/main_menu_premium.yml",
            "gui/entity_menu.yml",
            "gui/upgrade_menu.yml",
            "gui/buy_limit_menu.yml",
            "gui/pos_menu.yml"
        )
        
        for (resource in resources) {
            saveResource(resource, false)
        }
    }
    
    /**
     * 注册命令
     */
    private fun registerCommands() {
        // 注册基础命令
        CommandRegistry.registerCommand("help", HelpCommand(this))
        
        // 注册主命令执行器
        val command: PluginCommand? = getCommand("mcw")
        if (command != null) {
            val mcwCommand = MCWCommand(this)
            command.setExecutor(mcwCommand)
            command.tabCompleter = mcwCommand
        }
    }
    
    /**
     * 注册事件监听器
     */
    private fun registerEvents() {
        // 注册刷怪笼监听器
        ListenerManager.registerListener(SpawnerListener(this))
    }
    
    /**
     * 设置钩子
     */
    private fun setupHooks() {
        // 设置PlaceholderAPI钩子
        if (config.getBoolean("placeholderapi.enabled", true)) {
            setupPlaceholderAPI()
        }
        
        // 初始化PlayerPoints钩子
        playerPointsHook = PlayerPointsHook(this)
    }
    
    /**
     * 设置PlaceholderAPI钩子
     */
    private fun setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderHook = PlaceholderAPIHook(this)
            placeholderHook?.register()
        }
    }
    
    /**
     * 启动刷怪笼生成任务
     */
    private fun startSpawnerTask() {
        val tickInterval = config.getInt("performance.spawner_tick_interval", 20)
        val maxSpawnersPerTick = config.getInt("performance.max_spawners_per_tick", 10)
        val asyncProcessing = config.getBoolean("performance.async_processing", true)
        
        // 刷怪笼处理必须在主线程执行，因为需要调用Bukkit API
        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            processSpawners(maxSpawnersPerTick)
        }, 0L, tickInterval.toLong())
    }
    
    /**
     * 处理刷怪笼生成
     */
    private fun processSpawners(maxSpawnersPerTick: Int) {
        val currentTime = System.currentTimeMillis()
        val spawners = getSpawnerManager().getAllSpawners()
        var processedCount = 0
        
        for (spawner in spawners) {
            if (processedCount >= maxSpawnersPerTick) break
            
            // 处理激活刷怪笼的生成
            if (spawner.active) {
                // 检查是否到了生成时间
                if (currentTime - spawner.lastSpawnTime >= spawner.spawnDelay * 50L) {
                    trySpawn(spawner)
                    processedCount++
                }
            }
            
            // 处理关闭刷怪笼的存储
            if (!spawner.active && spawner.storageEnabled) {
                processStorageAdd(spawner, currentTime)
            }
            
            // 处理存储释放（只有激活的刷怪笼才释放）
            processStorageRelease(spawner)
        }
    }

    /**
     * 尝试生成实体
     */
    private fun trySpawn(spawner: org.Aauu.monsterCapitalWorks.model.Spawner) {
        val world = spawner.location.world ?: return
        
        // 检查附近实体数量（无论是否激活都要检查）
        // 附近实体数量判定范围为激活范围的立方体范围内
        val nearbyEntities = world.getNearbyEntities(spawner.location, spawner.activationRange.toDouble(), spawner.activationRange.toDouble(), spawner.activationRange.toDouble())
            .filterIsInstance<LivingEntity>()
            .filter { it.type == spawner.entityType }
        
        if (nearbyEntities.count() >= spawner.maxNearbyEntities) {
            return // 附近已有太多同类实体
        }
        
        // 无论刷怪笼是否激活，都尝试生成生物（但只在激活时真正生成）
        // 这样可以确保即使刷怪笼关闭，也能正常生成生物（如果需要的话）
        val spawnLocation = if (spawner.spawnMode == SpawnMode.PRECISE) {
            // 精确模式：使用设定的精确位置，但确保在激活范围内
            val targetX = spawner.location.x + spawner.preciseX
            val targetY = spawner.location.y + spawner.preciseY
            val targetZ = spawner.location.z + spawner.preciseZ
            
            // 确保不超出激活范围（防止bug）
            val clampedX = Math.max(spawner.location.x - spawner.activationRange, 
                                 Math.min(spawner.location.x + spawner.activationRange, targetX))
            val clampedY = Math.max(spawner.location.y - spawner.activationRange, 
                                 Math.min(spawner.location.y + spawner.activationRange, targetY))
            val clampedZ = Math.max(spawner.location.z - spawner.activationRange, 
                                 Math.min(spawner.location.z + spawner.activationRange, targetZ))
            
            Location(world, clampedX, clampedY, clampedZ)
        } else {
            // 随机模式：在刷怪笼激活范围内随机生成
            val range = spawner.activationRange.toDouble()
            val randomX = (Math.random() * 2 - 1) * range // -range到range的随机偏移
            val randomY = (Math.random() * 2 - 1) * range // Y轴也在范围内随机
            val randomZ = (Math.random() * 2 - 1) * range
            Location(
                world,
                spawner.location.x + randomX,
                spawner.location.y + randomY,
                spawner.location.z + randomZ
            )
        }
        
        // 刷怪笼开启时，正常生成生物
        if (spawner.active) {
            // 检查生成位置是否安全
            if (!spawnLocation.isSafeSpawnFor(spawner.entityType)) {
                return
            }
            
            // 生成生物
            val entity = world.spawnEntity(spawnLocation, spawner.entityType) ?: return
        }
        
        // 只有激活的刷怪笼才更新生成时间
        if (spawner.active) {
            spawner.lastSpawnTime = System.currentTimeMillis()
        }
    }
    
    /**
     * 处理存储添加（关闭的刷怪笼）
     */
    private fun processStorageAdd(spawner: org.Aauu.monsterCapitalWorks.model.Spawner, currentTime: Long) {
        // 检查是否应该添加新的存储生物（按tick间隔）
        val storageInterval = config.getInt("performance.storage_add_interval", 20) // 默认每20tick添加一个生物
        
        // 检查是否到了添加时间
        if (currentTime - spawner.lastReleaseTime >= storageInterval * 50L) {
            // 添加一个生物到存储中
            if (spawner.storedSpawns < spawner.maxStorage) {
                spawner.addStoredSpawns(1)
                spawner.lastReleaseTime = currentTime
                // 确保存储数据同步到数据库
                getSpawnerManager().updateSpawner(spawner)
            }
        }
    }
    
    /**
     * 处理存储释放
     */
    private fun processStorageRelease(spawner: org.Aauu.monsterCapitalWorks.model.Spawner) {
        // 只有存储功能启用且刷怪笼激活时才释放存储的生物
        if (!spawner.storageEnabled || !spawner.active || spawner.storedSpawns <= 0) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        // 从主配置文件读取存储释放设置，而不是从刷怪笼配置文件
        val releaseInterval = config.getInt("performance.storage_release_interval", 100)
        val releaseAmount = config.getInt("performance.storage_release_amount", 1)
        
        // 检查是否到了释放时间
        if (currentTime - spawner.lastReleaseTime >= releaseInterval * 50L) {
            val world = spawner.location.world ?: return
            
            // 检查附近实体数量
            // 附近实体数量判定范围为激活范围的立方体范围内
            val nearbyEntities = world.getNearbyEntities(spawner.location, spawner.activationRange.toDouble(), spawner.activationRange.toDouble(), spawner.activationRange.toDouble())
                .filterIsInstance<LivingEntity>()
                .filter { it.type == spawner.entityType }
            
            val availableSpace = spawner.maxNearbyEntities - nearbyEntities.count()
            val actualReleaseAmount = Math.min(releaseAmount, Math.min(availableSpace, spawner.storedSpawns))
            
            if (actualReleaseAmount > 0) {
                // 释放存储的生物
                for (i in 0 until actualReleaseAmount) {
                    val spawnLocation = if (spawner.spawnMode == SpawnMode.PRECISE) {
                        // 精确模式：使用设定的精确位置，但确保在激活范围内
                        val targetX = spawner.location.x + spawner.preciseX
                        val targetY = spawner.location.y + spawner.preciseY
                        val targetZ = spawner.location.z + spawner.preciseZ
                        
                        // 确保不超出激活范围（防止bug）
                        val clampedX = Math.max(spawner.location.x - spawner.activationRange, 
                                             Math.min(spawner.location.x + spawner.activationRange, targetX))
                        val clampedY = Math.max(spawner.location.y - spawner.activationRange, 
                                             Math.min(spawner.location.y + spawner.activationRange, targetY))
                        val clampedZ = Math.max(spawner.location.z - spawner.activationRange, 
                                             Math.min(spawner.location.z + spawner.activationRange, targetZ))
                        
                        Location(world, clampedX, clampedY, clampedZ)
                    } else {
                        // 随机模式：在刷怪笼激活范围内随机生成
                        val range = spawner.activationRange.toDouble()
                        val randomX = (Math.random() * 2 - 1) * range
                        val randomY = (Math.random() * 2 - 1) * range
                        val randomZ = (Math.random() * 2 - 1) * range
                        Location(
                            world,
                            spawner.location.x + randomX,
                            spawner.location.y + randomY,
                            spawner.location.z + randomZ
                        )
                    }
                    
                    // 检查生成位置是否安全
                    if (spawnLocation.isSafeSpawnFor(spawner.entityType)) {
                        world.spawnEntity(spawnLocation, spawner.entityType)
                        spawner.removeStoredSpawns(1)
                    }
                }
                
                spawner.lastReleaseTime = currentTime
                getSpawnerManager().updateSpawner(spawner)
            }
        }
    }
    
    
    /**
     * 重载配置
     */
    fun reloadConfigs() {
        reloadConfig()
        getConfigManager().reloadAll()
        getUpgradeManager().reload()
        
        // 清理相关缓存
        CacheManager.remove("config:*")
    }
    
    // 便捷的getter方法 - 使用依赖注入
    fun getConfigManager(): ConfigManager = DependencyInjector.getService(ConfigManager::class.java)
    fun getDataManager(): DataManager = DependencyInjector.getService(DataManager::class.java)
    fun getEconomyManager(): EconomyManager = DependencyInjector.getService(EconomyManager::class.java)
    fun getPermissionManager(): PermissionManager = DependencyInjector.getService(PermissionManager::class.java)
    fun getSpawnerManager(): SpawnerManager = DependencyInjector.getService(SpawnerManager::class.java)
    fun getUpgradeManager(): UpgradeManager = DependencyInjector.getService(UpgradeManager::class.java)
    fun getGUIManager(): GUIManager = DependencyInjector.getService(GUIManager::class.java)
    fun getPlayerPointsHook(): PlayerPointsHook? = playerPointsHook
}
