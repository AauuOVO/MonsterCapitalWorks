package org.mcw.monstercapitalworks.model

import org.bukkit.Location
import org.bukkit.entity.EntityType
import java.util.*

/**
 * 刷怪笼数据模型
 */
class Spawner(
    var id: Int = 0,
    val owner: UUID,
    val type: SpawnerType,
    var entityType: EntityType,
    val location: Location,
    val upgradeLevels: MutableMap<String, Int> = mutableMapOf(),
    
    // 刷怪参数
    var spawnDelay: Int = 100,
    var spawnCount: Int = 1,
    var maxNearbyEntities: Int = 6,
    var activationRange: Int = 16,
    
    // 存储相关
    var storageEnabled: Boolean = false,
    var maxStorage: Int = 100,
    var storedSpawns: Int = 0,
    var lastSpawnTime: Long = System.currentTimeMillis(),
    var lastReleaseTime: Long = System.currentTimeMillis(),
    
    // 刷怪笼开关状态（true=开启刷怪，false=关闭刷怪仅存储）
    var active: Boolean = true,
    
    // 生成模式（RANDOM=随机位置，PRECISE=精确位置）
    var spawnMode: SpawnMode = SpawnMode.RANDOM,
    
    // 精确生成位置（相对于刷怪笼的偏移量）
    var preciseX: Double = 0.0,
    var preciseY: Double = 1.0,
    var preciseZ: Double = 0.0
) {
    /**
     * 生成模式枚举
     */
    enum class SpawnMode {
        RANDOM,  // 随机位置生成
        PRECISE  // 精确位置生成
    }

    fun getUpgradeLevel(upgradeType: String): Int = upgradeLevels.getOrDefault(upgradeType, 0)

    fun setUpgradeLevel(upgradeType: String, level: Int) {
        upgradeLevels[upgradeType] = level
    }

    fun getUpgradeLevels(): Map<String, Int> = upgradeLevels.toMap()

    fun addStoredSpawns(amount: Int) {
        storedSpawns = minOf(storedSpawns + amount, maxStorage)
    }

    fun removeStoredSpawns(amount: Int) {
        storedSpawns = maxOf(0, storedSpawns - amount)
    }
}
