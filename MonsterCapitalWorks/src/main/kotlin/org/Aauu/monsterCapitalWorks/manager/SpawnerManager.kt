package org.Aauu.monsterCapitalWorks.manager

import org.Aauu.monsterCapitalWorks.MonsterCapitalWorks
import org.Aauu.monsterCapitalWorks.model.Spawner
import org.Aauu.monsterCapitalWorks.model.SpawnerType
import org.Aauu.monsterCapitalWorks.model.SpawnMode
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 刷怪笼管理器 - 负责刷怪笼的创建、管理和操作
 */
class SpawnerManager(private val plugin: JavaPlugin) {
    private val spawners = ConcurrentHashMap<Location, Spawner>()
    private val playerSpawners = ConcurrentHashMap<UUID, MutableSet<Location>>()
    private val unloadedSpawnerIds = mutableSetOf<Int>() // 记录未加载的刷怪笼ID
    
    private val dataManager get() = (plugin as MonsterCapitalWorks).getDataManager()

    fun initialize() {
        try {
            val loadedSpawners = dataManager.loadSpawners().get()
            for (spawner in loadedSpawners) {
                spawners[spawner.normalizeLocation()] = spawner
                playerSpawners.computeIfAbsent(spawner.owner) { mutableSetOf() }.add(spawner.normalizeLocation())
            }
            plugin.logger.info("已加载 ${loadedSpawners.size} 个刷怪笼")
            
            // 如果有未加载的刷怪笼，定时重新尝试加载
            if (unloadedSpawnerIds.isNotEmpty()) {
                scheduleRetryLoadUnloadedSpawners()
            }
        } catch (e: Exception) {
            plugin.logger.severe("加载刷怪笼失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 设置未加载的刷怪笼ID列表
     */
    fun setUnloadedSpawnerIds(ids: Set<Int>) {
        unloadedSpawnerIds.clear()
        unloadedSpawnerIds.addAll(ids)
    }
    
    /**
     * 定时重新尝试加载未加载的刷怪笼
     */
    private fun scheduleRetryLoadUnloadedSpawners() {
        // 每30秒尝试一次加载未加载的刷怪笼
        org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            Runnable {
                if (unloadedSpawnerIds.isNotEmpty()) {
                    retryLoadUnloadedSpawners()
                }
            },
            600L, // 延迟30秒后开始
            600L  // 每30秒执行一次
        )
    }
    
    /**
     * 重新尝试加载未加载的刷怪笼
     */
    private fun retryLoadUnloadedSpawners() {
        val idsToRemove = mutableSetOf<Int>()
        
        for (spawnerId in unloadedSpawnerIds) {
            try {
                // 从数据库重新加载该刷怪笼
                val spawner = dataManager.loadSpawnerById(spawnerId)
                if (spawner != null) {
                    // 世界已加载，添加刷怪笼
                    spawners[spawner.normalizeLocation()] = spawner
                    playerSpawners.computeIfAbsent(spawner.owner) { mutableSetOf() }.add(spawner.normalizeLocation())
                    idsToRemove.add(spawnerId)
                    plugin.logger.info("成功加载之前未加载的刷怪笼 #$spawnerId")
                }
            } catch (e: Exception) {
                plugin.logger.warning("重新加载刷怪笼 #$spawnerId 失败: ${e.message}")
            }
        }
        
        // 移除已加载的刷怪笼ID
        unloadedSpawnerIds.removeAll(idsToRemove)
    }

    /**
     * 创建新的刷怪笼
     */
    fun createSpawner(
        owner: UUID,
        type: SpawnerType,
        entityType: EntityType,
        location: Location
    ): Spawner {
        val spawner = Spawner.create(
            location = location,
            owner = owner,
            type = type,
            entityType = entityType
        )

        // 应用配置文件的默认值
        applyDefaultValues(spawner)

        dataManager.saveSpawner(spawner)
        val normalizedLocation = spawner.normalizeLocation()
        spawners[normalizedLocation] = spawner
        playerSpawners.computeIfAbsent(owner) { mutableSetOf() }.add(normalizedLocation)

        return spawner
    }

    /**
     * 应用配置文件的默认值到刷怪笼
     */
    private fun applyDefaultValues(spawner: Spawner) {
        val configManager = (plugin as MonsterCapitalWorks).getConfigManager()
        val typeConfig = when (spawner.type) {
            SpawnerType.NORMAL -> configManager.getNormalConfig()
            SpawnerType.PREMIUM -> configManager.getPremiumConfig()
        } ?: return

        // 应用基础配置值
        spawner.spawnDelay = typeConfig.getInt("spawning.default_delay", 1000)
        spawner.spawnCount = typeConfig.getInt("spawning.default_count", 6)
        spawner.activationRange = typeConfig.getInt("spawning.default_range", 8)
        spawner.maxNearbyEntities = typeConfig.getInt("spawning.default_max_nearby", 5)
        
        spawner.maxStorage = typeConfig.getInt("storage.default_max", 
            typeConfig.getInt("spawning.default_storage", 5))
        
        spawner.storageEnabled = typeConfig.getBoolean("storage.enabled", true)
        
        // 应用升级效果
        (plugin as MonsterCapitalWorks).getUpgradeManager().applyUpgrades(spawner)
    }

    /**
     * 获取指定位置的刷怪笼
     */
    fun getSpawner(location: Location): Spawner? {
        val normalized = Location(
            location.world,
            location.blockX.toDouble(),
            location.blockY.toDouble(),
            location.blockZ.toDouble()
        ).apply {
            yaw = 0f
            pitch = 0f
        }
        return spawners[normalized]
    }

    /**
     * 获取指定位置的刷怪笼（别名方法）
     */
    fun getSpawnerAt(location: Location): Spawner? = getSpawner(location)

    /**
     * 移除指定位置的刷怪笼
     */
    fun removeSpawner(location: Location): Boolean {
        val normalized = Location(
            location.world,
            location.blockX.toDouble(),
            location.blockY.toDouble(),
            location.blockZ.toDouble()
        ).apply {
            yaw = 0f
            pitch = 0f
        }
        val spawner = spawners.remove(normalized) ?: return false
        
        playerSpawners[spawner.owner]?.remove(normalized)
        dataManager.deleteSpawner(spawner)
        return true
    }

    fun getPlayerSpawners(uuid: UUID): List<Spawner> {
        return playerSpawners[uuid]?.mapNotNull { spawners[it] } ?: emptyList()
    }

    fun getPlayerSpawnerCount(uuid: UUID, type: SpawnerType): Int {
        return getPlayerSpawners(uuid).count { it.type == type }
    }

    fun updateSpawner(spawner: Spawner) {
        (plugin as MonsterCapitalWorks).getDataManager().saveSpawner(spawner)
    }

    fun toggleSpawner(spawner: Spawner): Boolean {
        spawner.active = !spawner.active
        updateSpawner(spawner)
        return spawner.active
    }

    fun setSpawnMode(spawner: Spawner, mode: SpawnMode) {
        spawner.spawnMode = mode
        updateSpawner(spawner)
    }

    fun setPreciseLocation(spawner: Spawner, x: Double, y: Double, z: Double) {
        spawner.preciseX = x
        spawner.preciseY = y
        spawner.preciseZ = z
        updateSpawner(spawner)
    }

    fun upgradeSpawner(spawner: Spawner, upgradeType: String, newLevel: Int) {
        spawner.upgradeLevels[upgradeType] = newLevel
        updateSpawner(spawner)
    }

    fun getAllSpawners(): Collection<Spawner> {
        return spawners.values
    }

    fun getSpawnerCount(): Int {
        return spawners.size
    }

    fun shutdown() {
        // 关闭时的清理工作
        spawners.clear()
        playerSpawners.clear()
    }
}
