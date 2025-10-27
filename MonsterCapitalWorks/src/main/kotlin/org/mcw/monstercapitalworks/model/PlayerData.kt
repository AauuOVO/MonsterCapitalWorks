package org.mcw.monstercapitalworks.model

import org.bukkit.entity.EntityType
import java.util.*

/**
 * 玩家数据模型
 */
data class PlayerData(
    val uuid: UUID,
    var normalPurchasedLimit: Int = 0,
    var premiumPurchasedLimit: Int = 0,
    private val unlockedEntities: MutableMap<SpawnerType, MutableSet<EntityType>> = mutableMapOf(
        SpawnerType.NORMAL to mutableSetOf(),
        SpawnerType.PREMIUM to mutableSetOf()
    )
) {
    /**
     * 获取指定类型的已购买限制
     */
    fun getPurchasedLimit(type: SpawnerType): Int =
        if (type == SpawnerType.NORMAL) normalPurchasedLimit else premiumPurchasedLimit

    /**
     * 设置指定类型的已购买限制
     */
    fun setPurchasedLimit(type: SpawnerType, limit: Int) {
        when (type) {
            SpawnerType.NORMAL -> normalPurchasedLimit = limit
            SpawnerType.PREMIUM -> premiumPurchasedLimit = limit
        }
    }

    /**
     * 增加已购买限制
     */
    fun addPurchasedLimit(type: SpawnerType, amount: Int) {
        when (type) {
            SpawnerType.NORMAL -> normalPurchasedLimit += amount
            SpawnerType.PREMIUM -> premiumPurchasedLimit += amount
        }
    }

    fun getUnlockedEntities(type: SpawnerType): Set<EntityType> =
        unlockedEntities[type] ?: emptySet()

    fun hasUnlockedEntity(type: SpawnerType, entityType: EntityType): Boolean =
        unlockedEntities[type]?.contains(entityType) ?: false

    fun unlockEntity(type: SpawnerType, entityType: EntityType) {
        unlockedEntities[type]?.add(entityType)
    }

    fun lockEntity(type: SpawnerType, entityType: EntityType) {
        unlockedEntities[type]?.remove(entityType)
    }
}
