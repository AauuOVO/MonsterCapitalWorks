package org.mcw.monstercapitalworks.model;

import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家数据模型
 */
public class PlayerData {
    private final UUID uuid;
    private int normalPurchasedLimit;
    private int premiumPurchasedLimit;
    private final Map<SpawnerType, Set<EntityType>> unlockedEntities;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.normalPurchasedLimit = 0;
        this.premiumPurchasedLimit = 0;
        this.unlockedEntities = new HashMap<>();
        this.unlockedEntities.put(SpawnerType.NORMAL, new HashSet<>());
        this.unlockedEntities.put(SpawnerType.PREMIUM, new HashSet<>());
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getNormalPurchasedLimit() {
        return normalPurchasedLimit;
    }

    public void setNormalPurchasedLimit(int normalPurchasedLimit) {
        this.normalPurchasedLimit = normalPurchasedLimit;
    }

    public int getPremiumPurchasedLimit() {
        return premiumPurchasedLimit;
    }

    public void setPremiumPurchasedLimit(int premiumPurchasedLimit) {
        this.premiumPurchasedLimit = premiumPurchasedLimit;
    }
    
    /**
     * 获取指定类型的已购买限制
     */
    public int getPurchasedLimit(SpawnerType type) {
        return type == SpawnerType.NORMAL ? normalPurchasedLimit : premiumPurchasedLimit;
    }
    
    /**
     * 设置指定类型的已购买限制
     */
    public void setPurchasedLimit(SpawnerType type, int limit) {
        if (type == SpawnerType.NORMAL) {
            this.normalPurchasedLimit = limit;
        } else {
            this.premiumPurchasedLimit = limit;
        }
    }
    
    /**
     * 增加已购买限制
     */
    public void addPurchasedLimit(SpawnerType type, int amount) {
        if (type == SpawnerType.NORMAL) {
            this.normalPurchasedLimit += amount;
        } else {
            this.premiumPurchasedLimit += amount;
        }
    }

    public Set<EntityType> getUnlockedEntities(SpawnerType type) {
        return unlockedEntities.get(type);
    }

    public boolean hasUnlockedEntity(SpawnerType type, EntityType entityType) {
        return unlockedEntities.get(type).contains(entityType);
    }

    public void unlockEntity(SpawnerType type, EntityType entityType) {
        unlockedEntities.get(type).add(entityType);
    }

    public void lockEntity(SpawnerType type, EntityType entityType) {
        unlockedEntities.get(type).remove(entityType);
    }
}
