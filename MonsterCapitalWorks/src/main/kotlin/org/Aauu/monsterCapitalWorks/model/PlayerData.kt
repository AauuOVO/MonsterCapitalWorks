package org.Aauu.monsterCapitalWorks.model

import org.bukkit.entity.EntityType
import java.util.*

/**
 * 玩家数据模型
 * 存储玩家的刷怪笼限制和解锁的实体类型
 */
data class PlayerData(
    val uuid: UUID,
    var normalPurchasedLimit: Int = 0,
    var premiumPurchasedLimit: Int = 0,
    val unlockedEntities: MutableMap<SpawnerType, MutableSet<EntityType>> = mutableMapOf()
) {
    
    init {
        // 初始化解锁实体集合
        unlockedEntities.putIfAbsent(SpawnerType.NORMAL, mutableSetOf())
        unlockedEntities.putIfAbsent(SpawnerType.PREMIUM, mutableSetOf())
    }
    
    /**
     * 获取指定类型的已购买限制
     */
    fun getPurchasedLimit(type: SpawnerType): Int = when (type) {
        SpawnerType.NORMAL -> normalPurchasedLimit
        SpawnerType.PREMIUM -> premiumPurchasedLimit
    }
    
    /**
     * 设置指定类型的已购买限制
     */
    fun setPurchasedLimit(type: SpawnerType, limit: Int) {
        val validLimit = limit.coerceAtLeast(0)
        when (type) {
            SpawnerType.NORMAL -> normalPurchasedLimit = validLimit
            SpawnerType.PREMIUM -> premiumPurchasedLimit = validLimit
        }
    }
    
    /**
     * 增加已购买限制
     */
    fun addPurchasedLimit(type: SpawnerType, amount: Int) {
        when (type) {
            SpawnerType.NORMAL -> normalPurchasedLimit = (normalPurchasedLimit + amount).coerceAtLeast(0)
            SpawnerType.PREMIUM -> premiumPurchasedLimit = (premiumPurchasedLimit + amount).coerceAtLeast(0)
        }
    }
    
    /**
     * 获取指定类型的解锁实体
     */
    fun getUnlockedEntities(type: SpawnerType): MutableSet<EntityType> =
        unlockedEntities.getOrPut(type) { mutableSetOf() }
    
    /**
     * 检查是否已解锁指定实体
     */
    fun hasUnlockedEntity(type: SpawnerType, entityType: EntityType): Boolean =
        unlockedEntities[type]?.contains(entityType) ?: false
    
    /**
     * 解锁实体
     */
    fun unlockEntity(type: SpawnerType, entityType: EntityType) {
        unlockedEntities.getOrPut(type) { mutableSetOf() }.add(entityType)
    }
    
    /**
     * 锁定实体
     */
    fun lockEntity(type: SpawnerType, entityType: EntityType) {
        unlockedEntities[type]?.remove(entityType)
    }
}
