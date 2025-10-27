package org.Aauu.monsterCapitalWorks.model

import org.bukkit.Location
import org.bukkit.entity.EntityType
import java.util.*

/**
 * 刷怪笼数据模型
 * 使用data class提供自动生成的equals、hashCode、toString等方法
 */
data class Spawner(
    var id: Int = 0,
    val owner: UUID,
    val type: SpawnerType,
    var entityType: EntityType,
    val location: Location,
    val upgradeLevels: MutableMap<String, Int> = mutableMapOf(),
    
    // 生成配置
    var spawnDelay: Int = 200,
    var spawnCount: Int = 6,
    var maxNearbyEntities: Int = 5,
    var activationRange: Int = 8,
    
    // 存储配置
    var storageEnabled: Boolean = true,
    var maxStorage: Int = 5,
    var storedSpawns: Int = 0,
    var storedSpawnsReleased: Int = 0,
    var lastSpawnTime: Long = System.currentTimeMillis(),
    var lastReleaseTime: Long = System.currentTimeMillis(),
    
    // 状态配置
    var active: Boolean = true,
    var spawnMode: SpawnMode = SpawnMode.RANDOM,
    var preciseX: Double = 0.0,
    var preciseY: Double = 1.0,
    var preciseZ: Double = 0.0
) {
    
    init {
        // 验证并修正配置值
        maxNearbyEntities = maxNearbyEntities.coerceAtLeast(1)
        activationRange = activationRange.coerceAtLeast(1)
        spawnDelay = spawnDelay.coerceAtLeast(1)
        spawnCount = spawnCount.coerceAtLeast(1)
        maxStorage = maxStorage.coerceAtLeast(1)
    }
    
    /**
     * 标准化位置 - 移除yaw和pitch，只保留方块坐标
     */
    fun normalizeLocation(): Location = Location(
        location.world,
        location.blockX.toDouble(),
        location.blockY.toDouble(),
        location.blockZ.toDouble()
    ).apply {
        yaw = 0f
        pitch = 0f
    }
    
    /**
     * 获取升级等级
     */
    fun getUpgradeLevel(upgradeType: String): Int =
        upgradeLevels.getOrDefault(upgradeType, 0)
    
    /**
     * 设置升级等级
     */
    fun setUpgradeLevel(upgradeType: String, level: Int) {
        upgradeLevels[upgradeType] = level.coerceAtLeast(0)
    }
    
    /**
     * 更新存储数量（带上限检查）
     */
    fun updateStoredSpawns(amount: Int) {
        storedSpawns = amount.coerceIn(0, maxStorage)
    }
    
    /**
     * 增加存储数量
     */
    fun addStoredSpawns(amount: Int) {
        storedSpawns = (storedSpawns + amount).coerceIn(0, maxStorage)
    }
    
    /**
     * 减少存储数量
     */
    fun removeStoredSpawns(amount: Int) {
        storedSpawns = (storedSpawns - amount).coerceAtLeast(0)
    }
    
    companion object {
        /**
         * 创建新的刷怪笼实例
         */
        fun create(
            location: Location,
            owner: UUID,
            type: SpawnerType,
            entityType: EntityType
        ): Spawner = Spawner(
            id = 0,
            owner = owner,
            type = type,
            entityType = entityType,
            location = location
        )
    }
}
