package org.mcw.monstercapitalworks.model;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 刷怪笼数据模型
 */
public class Spawner {
    private int id;
    private final UUID owner;
    private final SpawnerType type;
    private EntityType entityType;
    private final Location location;
    private final Map<String, Integer> upgradeLevels;
    
    // 刷怪参数
    private int spawnDelay;
    private int spawnCount;
    private int maxNearbyEntities;
    private int activationRange;
    
    // 存储相关
    private boolean storageEnabled;
    private int maxStorage;
    private int storedSpawns;
    private long lastSpawnTime;
    private long lastReleaseTime;
    
    // 刷怪笼开关状态（true=开启刷怪，false=关闭刷怪仅存储）
    private boolean active;
    
    // 生成模式（RANDOM=随机位置，PRECISE=精确位置）
    private SpawnMode spawnMode;
    
    // 精确生成位置（相对于刷怪笼的偏移量）
    private double preciseX;
    private double preciseY;
    private double preciseZ;

    // 带ID的构造函数（从数据库加载时使用）
    public Spawner(int id, UUID owner, SpawnerType type, EntityType entityType, Location location) {
        this.id = id;
        this.owner = owner;
        this.type = type;
        this.entityType = entityType;
        this.location = location;
        this.upgradeLevels = new HashMap<>();
        this.storedSpawns = 0;
        this.lastSpawnTime = System.currentTimeMillis();
        this.lastReleaseTime = System.currentTimeMillis();
        this.active = true;
        this.spawnMode = SpawnMode.RANDOM;
        this.preciseX = 0;
        this.preciseY = 1;
        this.preciseZ = 0;
        
        // 默认值
        this.spawnDelay = 100;
        this.spawnCount = 1;
        this.maxNearbyEntities = 6;
        this.activationRange = 16;
        this.storageEnabled = false;
        this.maxStorage = 100;
    }
    
    // 不带ID的构造函数（新创建时使用）
    public Spawner(Location location, UUID owner, SpawnerType type, EntityType entityType) {
        this(0, owner, type, entityType, location);
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    public UUID getOwner() {
        return owner;
    }

    public SpawnerType getType() {
        return type;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public Location getLocation() {
        return location;
    }

    public Map<String, Integer> getUpgradeLevels() {
        return upgradeLevels;
    }

    public int getUpgradeLevel(String upgradeType) {
        return upgradeLevels.getOrDefault(upgradeType, 0);
    }

    public void setUpgradeLevel(String upgradeType, int level) {
        upgradeLevels.put(upgradeType, level);
    }

    // 刷怪参数的getter和setter
    public int getSpawnDelay() {
        return spawnDelay;
    }

    public void setSpawnDelay(int spawnDelay) {
        this.spawnDelay = spawnDelay;
    }

    public int getSpawnCount() {
        return spawnCount;
    }

    public void setSpawnCount(int spawnCount) {
        this.spawnCount = spawnCount;
    }

    public int getMaxNearbyEntities() {
        return maxNearbyEntities;
    }

    public void setMaxNearbyEntities(int maxNearbyEntities) {
        this.maxNearbyEntities = maxNearbyEntities;
    }

    public int getActivationRange() {
        return activationRange;
    }

    public void setActivationRange(int activationRange) {
        this.activationRange = activationRange;
    }

    // 存储相关的getter和setter
    public boolean isStorageEnabled() {
        return storageEnabled;
    }

    public void setStorageEnabled(boolean storageEnabled) {
        this.storageEnabled = storageEnabled;
    }

    public int getMaxStorage() {
        return maxStorage;
    }

    public void setMaxStorage(int maxStorage) {
        this.maxStorage = maxStorage;
    }

    public int getStoredSpawns() {
        return storedSpawns;
    }

    public void setStoredSpawns(int storedSpawns) {
        this.storedSpawns = Math.min(storedSpawns, maxStorage);
    }

    public void addStoredSpawns(int amount) {
        this.storedSpawns = Math.min(this.storedSpawns + amount, maxStorage);
    }
    
    public void removeStoredSpawns(int amount) {
        this.storedSpawns = Math.max(0, this.storedSpawns - amount);
    }

    public long getLastSpawnTime() {
        return lastSpawnTime;
    }

    public void setLastSpawnTime(long lastSpawnTime) {
        this.lastSpawnTime = lastSpawnTime;
    }
    
    public long getLastReleaseTime() {
        return lastReleaseTime;
    }
    
    public void setLastReleaseTime(long lastReleaseTime) {
        this.lastReleaseTime = lastReleaseTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    // 生成模式相关
    public SpawnMode getSpawnMode() {
        return spawnMode;
    }
    
    public void setSpawnMode(SpawnMode spawnMode) {
        this.spawnMode = spawnMode;
    }
    
    public double getPreciseX() {
        return preciseX;
    }
    
    public void setPreciseX(double preciseX) {
        this.preciseX = preciseX;
    }
    
    public double getPreciseY() {
        return preciseY;
    }
    
    public void setPreciseY(double preciseY) {
        this.preciseY = preciseY;
    }
    
    public double getPreciseZ() {
        return preciseZ;
    }
    
    public void setPreciseZ(double preciseZ) {
        this.preciseZ = preciseZ;
    }
    
    /**
     * 生成模式枚举
     */
    public enum SpawnMode {
        RANDOM,  // 随机位置生成
        PRECISE  // 精确位置生成
    }
}
