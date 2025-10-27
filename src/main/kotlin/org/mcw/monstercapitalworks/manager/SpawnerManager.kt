package org.mcw.monstercapitalworks.manager

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import org.mcw.monstercapitalworks.model.Spawner
import org.mcw.monstercapitalworks.model.SpawnerType
import org.mcw.monstercapitalworks.util.ConditionChecker
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 刷怪笼管理器 - 管理所有刷怪笼的生成和存储
 */
class SpawnerManager(private val plugin: MonsterCapitalWorks) {
    
    private val spawners = ConcurrentHashMap<Location, Spawner>()
    private val playerSpawners = ConcurrentHashMap<UUID, MutableSet<Location>>()
    private var spawnerTask: BukkitRunnable? = null
    
    /**
     * 标准化Location - 确保使用方块坐标（整数）
     * 这样可以避免浮点数精度问题导致的Location比较失败
     */
    private fun normalizeLocation(location: Location): Location {
        return Location(
            location.world,
            location.blockX.toDouble(),
            location.blockY.toDouble(),
            location.blockZ.toDouble()
        ).apply {
            yaw = 0f
            pitch = 0f
        }
    }
    
    /**
     * 初始化管理器
     */
    fun initialize() {
        // 延迟加载刷怪笼，等待所有世界加载完成
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            loadAllSpawners()
            plugin.logger.info("刷怪笼管理器已初始化，加载了 ${spawners.size} 个刷怪笼")
        }, 20L) // 延迟1秒（20 ticks）
        
        // 启动刷怪笼处理任务
        startSpawnerTask()
    }
    
    /**
     * 关闭管理器
     */
    fun shutdown() {
        spawnerTask?.cancel()
        
        // 保存所有刷怪笼数据
        saveAllSpawners()
        
        spawners.clear()
        playerSpawners.clear()
    }
    
    /**
     * 从数据库加载所有刷怪笼
     */
    private fun loadAllSpawners() {
        val loadedSpawners = plugin.dataManager.loadAllSpawners()
        
        for (spawner in loadedSpawners) {
            val normalizedLoc = normalizeLocation(spawner.location)
            spawners[normalizedLoc] = spawner
            
            // 添加到玩家刷怪笼映射
            playerSpawners.computeIfAbsent(spawner.owner) { mutableSetOf() }.add(normalizedLoc)
            
            // 重要：禁用原版刷怪笼生成机制
            // 这样重启后刷怪笼才能被正确识别为 MCW 刷怪笼
            // 需要延迟执行，因为世界可能还没有完全加载
            val loc = normalizedLoc
            val entityType = spawner.entityType
            plugin.server.scheduler.runTask(plugin, Runnable {
                if (loc.world != null) {
                    disableVanillaSpawner(loc, entityType)
                }
            })
        }
    }
    
    /**
     * 保存所有刷怪笼
     */
    private fun saveAllSpawners() {
        for (spawner in spawners.values) {
            plugin.dataManager.saveSpawner(spawner)
        }
    }
    
    /**
     * 启动刷怪笼处理任务
     */
    private fun startSpawnerTask() {
        val interval = plugin.config.getInt("performance.spawner_tick_interval", 20)
        val maxPerTick = plugin.config.getInt("performance.max_spawners_per_tick", 10)
        val async = plugin.config.getBoolean("performance.async_processing", true)
        
        spawnerTask = object : BukkitRunnable() {
            private var iterator = spawners.values.iterator()
            private var processed = 0
            
            override fun run() {
                processed = 0
                
                // 如果迭代器用完了，重新创建
                if (!iterator.hasNext()) {
                    iterator = spawners.values.iterator()
                }
                
                // 处理一批刷怪笼
                while (iterator.hasNext() && processed < maxPerTick) {
                    val spawner = iterator.next()
                    processSpawner(spawner)
                    processed++
                }
            }
        }
        
        if (async) {
            spawnerTask!!.runTaskTimerAsynchronously(plugin, interval.toLong(), interval.toLong())
        } else {
            spawnerTask!!.runTaskTimer(plugin, interval.toLong(), interval.toLong())
        }
    }
    
    /**
     * 处理单个刷怪笼
     */
    private fun processSpawner(spawner: Spawner) {
        // 在主线程中执行所有需要访问世界数据的操作
        plugin.server.scheduler.runTask(plugin, Runnable process@{
            // 检查刷怪笼是否仍然存在
            val loc = spawner.location
            if (loc.world == null) {
                return@process
            }
            
            val block = loc.block
            if (block.type != Material.SPAWNER) {
                // 刷怪笼已被破坏,移除
                removeSpawner(loc)
                return@process
            }
            
            // 检查是否有玩家在激活范围内 - 优化：使用getNearbyPlayers而不是getNearbyEntities
            val range = spawner.activationRange
            val hasPlayer = loc.world!!.getNearbyPlayers(loc, range.toDouble()).isNotEmpty()
            
            if (!hasPlayer) {
                // 没有玩家，只有在刷怪笼关闭时才累积存储
                if (!spawner.active && spawner.storageEnabled) {
                    val now = System.currentTimeMillis()
                    val lastSpawn = spawner.lastSpawnTime
                    val delay = spawner.spawnDelay * 50L // tick转毫秒
                    
                    if (now - lastSpawn >= delay) {
                        spawner.addStoredSpawns(1)
                        spawner.lastSpawnTime = now
                    }
                }
                return@process
            }
            
            // 有玩家在范围内
            if (spawner.active) {
                // 刷怪笼开启，正常生成
                val now = System.currentTimeMillis()
                val lastSpawn = spawner.lastSpawnTime
                val delay = spawner.spawnDelay * 50L
                
                if (now - lastSpawn >= delay) {
                    // 优化：检查附近实体数量 - 使用getNearbyEntities但只计数特定类型
                    val entityType = spawner.entityType
                    val nearbyCount = loc.world!!.getNearbyEntities(loc, range.toDouble(), range.toDouble(), range.toDouble())
                        .count { entity -> entity.type == entityType }
                    
                    if (nearbyCount < spawner.maxNearbyEntities) {
                        // 可以生成
                        spawnEntities(spawner)
                        spawner.lastSpawnTime = now
                    }
                }
                
                // 释放存储的生成
                if (spawner.storedSpawns > 0) {
                    releaseStoredSpawns(spawner)
                }
            } else {
                // 刷怪笼关闭，仅存储
                if (spawner.storageEnabled) {
                    val now = System.currentTimeMillis()
                    val lastSpawn = spawner.lastSpawnTime
                    val delay = spawner.spawnDelay * 50L
                    
                    if (now - lastSpawn >= delay) {
                        spawner.addStoredSpawns(1)
                        spawner.lastSpawnTime = now
                    }
                }
            }
        })
    }
    
    private fun spawnEntities(spawner: Spawner) {
        val loc = spawner.location
        val type = spawner.entityType
        val count = spawner.spawnCount
        
        plugin.server.scheduler.runTask(plugin, Runnable spawn@{
            // 检查conditions（PAPI条件）
            // 获取附近的玩家来进行PAPI变量解析
            val nearbyPlayers = loc.world!!.getNearbyPlayers(loc, spawner.activationRange.toDouble())
            if (nearbyPlayers.isNotEmpty()) {
                val nearestPlayer = nearbyPlayers.first()
                
                // 检查是否有绕过权限
                if (!nearestPlayer.hasPermission("mcw.bypass.conditions")) {
                    val entityConfig = if (spawner.type == SpawnerType.NORMAL) {
                        plugin.configManager.getNormalEntities()
                    } else {
                        plugin.configManager.getPremiumEntities()
                    }
                    
                    if (entityConfig != null) {
                        val conditions = entityConfig.getStringList("entities.${type.name}.conditions")
                        
                        // 如果不满足PAPI条件，则不生成
                        if (!ConditionChecker.checkPAPIConditions(nearestPlayer, conditions)) {
                            return@spawn
                        }
                    }
                }
            }
            
            // 满足条件，开始生成
            for (i in 0 until count) {
                val spawnLoc = if (spawner.spawnMode == Spawner.SpawnMode.PRECISE) {
                    // 精确模式：使用设定的偏移量
                    loc.clone().add(
                        spawner.preciseX,
                        spawner.preciseY,
                        spawner.preciseZ
                    )
                } else {
                    // 随机模式：在刷怪笼周围随机位置生成
                    val offsetX = (Math.random() - 0.5) * 4
                    val offsetY = Math.random() * 2
                    val offsetZ = (Math.random() - 0.5) * 4
                    loc.clone().add(offsetX, offsetY, offsetZ)
                }
                
                loc.world!!.spawnEntity(spawnLoc, type)
            }
        })
    }
    
    /**
     * 释放存储的生成
     */
    private fun releaseStoredSpawns(spawner: Spawner) {
        val releaseInterval = plugin.config.getInt("spawning.storage.release_interval", 20)
        val now = System.currentTimeMillis()
        val lastRelease = spawner.lastReleaseTime
        
        if (now - lastRelease >= releaseInterval * 50L) {
            if (spawner.storedSpawns > 0) {
                spawnEntities(spawner)
                spawner.removeStoredSpawns(1)
                spawner.lastReleaseTime = now
            }
        }
    }
    
    fun createSpawner(location: Location, owner: UUID, type: SpawnerType, entityType: EntityType): Spawner {
        val normalizedLoc = normalizeLocation(location)
        val spawner = Spawner(id = 0, owner = owner, type = type, entityType = entityType, location = normalizedLoc)
        
        spawner.spawnDelay = plugin.config.getInt("spawning.default_spawn_delay", 100)
        spawner.spawnCount = plugin.config.getInt("spawning.default_spawn_count", 1)
        spawner.maxNearbyEntities = plugin.config.getInt("spawning.default_max_nearby_entities", 6)
        spawner.activationRange = plugin.config.getInt("spawning.default_activation_range", 16)
        
        if (plugin.config.getBoolean("spawning.storage.enabled", true)) {
            spawner.storageEnabled = true
            spawner.maxStorage = plugin.config.getInt("spawning.storage.default_max_storage", 100)
        }
        
        spawners[normalizedLoc] = spawner
        playerSpawners.computeIfAbsent(owner) { mutableSetOf() }.add(normalizedLoc)
        plugin.dataManager.saveSpawner(spawner)
        
        // 彻底禁用原版刷怪笼生成
        disableVanillaSpawner(normalizedLoc, entityType)
        
        return spawner
    }
    
    /**
     * 彻底禁用原版刷怪笼生成
     */
    private fun disableVanillaSpawner(location: Location, entityType: EntityType) {
        val block = location.block
        if (block.type == Material.SPAWNER) {
            val cs = block.state as CreatureSpawner
            cs.spawnedType = entityType
            cs.maxSpawnDelay = Int.MAX_VALUE // 先设置最大延迟
            cs.minSpawnDelay = Int.MAX_VALUE // 再设置最小延迟
            cs.delay = Int.MAX_VALUE // 设置当前延迟为最大值
            cs.spawnCount = 0 // 生成数量设为0
            cs.maxNearbyEntities = 0 // 最大附近实体设为0
            cs.requiredPlayerRange = 0 // 激活范围设为0
            cs.spawnRange = 0 // 生成范围设为0
            cs.update(true, false) // 强制更新
        }
    }
    
    /**
     * 移除刷怪笼
     */
    fun removeSpawner(location: Location) {
        val normalizedLoc = normalizeLocation(location)
        val spawner = spawners.remove(normalizedLoc)
        if (spawner != null) {
            val locations = playerSpawners[spawner.owner]
            locations?.remove(normalizedLoc)
            plugin.dataManager.deleteSpawner(normalizedLoc)
        }
    }
    
    /**
     * 获取刷怪笼
     */
    fun getSpawner(location: Location): Spawner? {
        val normalizedLoc = normalizeLocation(location)
        
        // 首先尝试直接获取
        var spawner = spawners[normalizedLoc]
        
        // 如果直接获取失败，尝试通过坐标匹配（解决世界对象不同的问题）
        if (spawner == null && normalizedLoc.world != null) {
            val worldName = normalizedLoc.world!!.name
            val x = normalizedLoc.blockX
            val y = normalizedLoc.blockY
            val z = normalizedLoc.blockZ
            
            for ((loc, s) in spawners) {
                if (loc.world != null &&
                    loc.world!!.name == worldName &&
                    loc.blockX == x &&
                    loc.blockY == y &&
                    loc.blockZ == z) {
                    spawner = s
                    
                    if (plugin.config.getBoolean("debug", false)) {
                        plugin.logger.info("通过坐标匹配找到刷怪笼: $loc")
                    }
                    break
                }
            }
        }
        
        // 调试信息
        if (spawner == null && plugin.config.getBoolean("debug", false)) {
            plugin.logger.info("查找刷怪笼失败:")
            plugin.logger.info("  原始位置: $location")
            plugin.logger.info("  标准化位置: $normalizedLoc")
            plugin.logger.info("  世界名称: ${normalizedLoc.world?.name ?: "null"}")
            plugin.logger.info("  已加载的刷怪笼位置:")
            for (loc in spawners.keys) {
                if (loc.world != null) {
                    plugin.logger.info("    - 世界: ${loc.world!!.name}, 坐标: (${loc.blockX}, ${loc.blockY}, ${loc.blockZ})")
                }
            }
        }
        
        return spawner
    }
    
    /**
     * 获取玩家的所有刷怪笼
     */
    fun getPlayerSpawners(player: UUID): List<Spawner> {
        val locations = playerSpawners[player] ?: return emptyList()
        
        val result = mutableListOf<Spawner>()
        for (loc in locations) {
            val spawner = spawners[loc]
            if (spawner != null) {
                result.add(spawner)
            }
        }
        return result
    }
    
    /**
     * 获取玩家指定类型的刷怪笼数量
     */
    fun getPlayerSpawnerCount(player: UUID, type: SpawnerType): Int {
        return getPlayerSpawners(player).count { s -> s.type == type }
    }
    
    /**
     * 检查位置是否是刷怪笼
     */
    fun isSpawner(location: Location): Boolean {
        val normalizedLoc = normalizeLocation(location)
        return spawners.containsKey(normalizedLoc)
    }
    
    fun updateSpawnerEntity(location: Location, entityType: EntityType) {
        val normalizedLoc = normalizeLocation(location)
        val spawner = spawners[normalizedLoc]
        if (spawner != null) {
            spawner.entityType = entityType
            
            // 彻底禁用原版刷怪笼生成
            disableVanillaSpawner(normalizedLoc, entityType)
            
            plugin.dataManager.saveSpawner(spawner)
        }
    }
    
    /**
     * 保存单个刷怪笼
     */
    fun saveSpawner(spawner: Spawner) {
        plugin.dataManager.saveSpawner(spawner)
    }
}
