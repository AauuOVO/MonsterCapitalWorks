package org.Aauu.aauuMobCapital.manager;

import org.Aauu.aauuMobCapital.AauuMobCapital;
import org.Aauu.aauuMobCapital.model.Spawner;
import org.Aauu.aauuMobCapital.model.SpawnerType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刷怪笼管理器 - 管理所有刷怪笼的生成和存储
 */
public class SpawnerManager {
    
    private final AauuMobCapital plugin;
    private final Map<Location, Spawner> spawners;
    private final Map<UUID, Set<Location>> playerSpawners;
    private BukkitRunnable spawnerTask;
    
    public SpawnerManager(AauuMobCapital plugin) {
        this.plugin = plugin;
        this.spawners = new ConcurrentHashMap<>();
        this.playerSpawners = new ConcurrentHashMap<>();
    }
    
    /**
     * 初始化管理器
     */
    public void initialize() {
        // 从数据库加载所有刷怪笼
        loadAllSpawners();
        
        // 启动刷怪笼处理任务
        startSpawnerTask();
        
        plugin.getLogger().info("刷怪笼管理器已初始化，加载了 " + spawners.size() + " 个刷怪笼");
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        if (spawnerTask != null) {
            spawnerTask.cancel();
        }
        
        // 保存所有刷怪笼数据
        saveAllSpawners();
        
        spawners.clear();
        playerSpawners.clear();
    }
    
    /**
     * 从数据库加载所有刷怪笼
     */
    private void loadAllSpawners() {
        List<Spawner> loadedSpawners = plugin.getDataManager().loadAllSpawners();
        
        for (Spawner spawner : loadedSpawners) {
            spawners.put(spawner.getLocation(), spawner);
            
            // 添加到玩家刷怪笼映射
            playerSpawners.computeIfAbsent(spawner.getOwner(), k -> new HashSet<>())
                    .add(spawner.getLocation());
        }
    }
    
    /**
     * 保存所有刷怪笼
     */
    private void saveAllSpawners() {
        for (Spawner spawner : spawners.values()) {
            plugin.getDataManager().saveSpawner(spawner);
        }
    }
    
    /**
     * 启动刷怪笼处理任务
     */
    private void startSpawnerTask() {
        int interval = plugin.getConfig().getInt("performance.spawner_tick_interval", 20);
        int maxPerTick = plugin.getConfig().getInt("performance.max_spawners_per_tick", 10);
        boolean async = plugin.getConfig().getBoolean("performance.async_processing", true);
        
        spawnerTask = new BukkitRunnable() {
            private final Iterator<Spawner>[] iterators = new Iterator[]{spawners.values().iterator()};
            private int processed = 0;
            
            @Override
            public void run() {
                processed = 0;
                
                // 如果迭代器用完了，重新创建
                if (!iterators[0].hasNext()) {
                    iterators[0] = spawners.values().iterator();
                }
                
                // 处理一批刷怪笼
                while (iterators[0].hasNext() && processed < maxPerTick) {
                    Spawner spawner = iterators[0].next();
                    processSpawner(spawner);
                    processed++;
                }
            }
        };
        
        if (async) {
            spawnerTask.runTaskTimerAsynchronously(plugin, interval, interval);
        } else {
            spawnerTask.runTaskTimer(plugin, interval, interval);
        }
    }
    
    /**
     * 处理单个刷怪笼
     */
    private void processSpawner(Spawner spawner) {
        // 在主线程中执行所有需要访问世界数据的操作
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // 检查刷怪笼是否仍然存在
            Location loc = spawner.getLocation();
            if (loc.getWorld() == null) {
                return;
            }
            
            Block block = loc.getBlock();
            if (block.getType() != Material.SPAWNER) {
                // 刷怪笼已被破坏,移除
                removeSpawner(loc);
                return;
            }
            
            // 检查是否有玩家在激活范围内
            int range = spawner.getActivationRange();
            boolean hasPlayer = loc.getWorld().getNearbyEntities(loc, range, range, range)
                    .stream()
                    .anyMatch(entity -> entity instanceof Player);
            
            if (!hasPlayer) {
                // 没有玩家，只有在刷怪笼关闭时才累积存储
                if (!spawner.isActive() && spawner.isStorageEnabled()) {
                    long now = System.currentTimeMillis();
                    long lastSpawn = spawner.getLastSpawnTime();
                    long delay = spawner.getSpawnDelay() * 50L; // tick转毫秒
                    
                    if (now - lastSpawn >= delay) {
                        spawner.addStoredSpawns(1);
                        spawner.setLastSpawnTime(now);
                    }
                }
                return;
            }
            
            // 有玩家在范围内
            if (spawner.isActive()) {
                // 刷怪笼开启，正常生成
                long now = System.currentTimeMillis();
                long lastSpawn = spawner.getLastSpawnTime();
                long delay = spawner.getSpawnDelay() * 50L;
                
                if (now - lastSpawn >= delay) {
                    // 检查附近实体数量
                    EntityType entityType = spawner.getEntityType();
                    long nearbyCount = loc.getWorld().getNearbyEntities(loc, range, range, range)
                            .stream()
                            .filter(entity -> entity.getType() == entityType)
                            .count();
                    
                    if (nearbyCount < spawner.getMaxNearbyEntities()) {
                        // 可以生成
                        spawnEntities(spawner);
                        spawner.setLastSpawnTime(now);
                    }
                }
                
                // 释放存储的生成
                if (spawner.getStoredSpawns() > 0) {
                    releaseStoredSpawns(spawner);
                }
            } else {
                // 刷怪笼关闭，仅存储
                if (spawner.isStorageEnabled()) {
                    long now = System.currentTimeMillis();
                    long lastSpawn = spawner.getLastSpawnTime();
                    long delay = spawner.getSpawnDelay() * 50L;
                    
                    if (now - lastSpawn >= delay) {
                        spawner.addStoredSpawns(1);
                        spawner.setLastSpawnTime(now);
                    }
                }
            }
        });
    }
    
    private void spawnEntities(Spawner spawner) {
        Location loc = spawner.getLocation();
        EntityType type = spawner.getEntityType();
        int count = spawner.getSpawnCount();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < count; i++) {
                Location spawnLoc;
                
                if (spawner.getSpawnMode() == Spawner.SpawnMode.PRECISE) {
                    // 精确模式：使用设定的偏移量
                    spawnLoc = loc.clone().add(
                        spawner.getPreciseX(),
                        spawner.getPreciseY(),
                        spawner.getPreciseZ()
                    );
                } else {
                    // 随机模式：在刷怪笼周围随机位置生成
                    double offsetX = (Math.random() - 0.5) * 4;
                    double offsetY = Math.random() * 2;
                    double offsetZ = (Math.random() - 0.5) * 4;
                    spawnLoc = loc.clone().add(offsetX, offsetY, offsetZ);
                }
                
                loc.getWorld().spawnEntity(spawnLoc, type);
            }
        });
    }
    
    /**
     * 释放存储的生成
     */
    private void releaseStoredSpawns(Spawner spawner) {
        int releaseInterval = plugin.getConfig().getInt("spawning.storage.release_interval", 20);
        long now = System.currentTimeMillis();
        long lastRelease = spawner.getLastReleaseTime();
        
        if (now - lastRelease >= releaseInterval * 50L) {
            if (spawner.getStoredSpawns() > 0) {
                spawnEntities(spawner);
                spawner.removeStoredSpawns(1);
                spawner.setLastReleaseTime(now);
            }
        }
    }
    
    public Spawner createSpawner(Location location, UUID owner, SpawnerType type, EntityType entityType) {
        Spawner spawner = new Spawner(location, owner, type, entityType);
        
        spawner.setSpawnDelay(plugin.getConfig().getInt("spawning.default_spawn_delay", 100));
        spawner.setSpawnCount(plugin.getConfig().getInt("spawning.default_spawn_count", 1));
        spawner.setMaxNearbyEntities(plugin.getConfig().getInt("spawning.default_max_nearby_entities", 6));
        spawner.setActivationRange(plugin.getConfig().getInt("spawning.default_activation_range", 16));
        
        if (plugin.getConfig().getBoolean("spawning.storage.enabled", true)) {
            spawner.setStorageEnabled(true);
            spawner.setMaxStorage(plugin.getConfig().getInt("spawning.storage.default_max_storage", 100));
        }
        
        spawners.put(location, spawner);
        playerSpawners.computeIfAbsent(owner, k -> new HashSet<>()).add(location);
        plugin.getDataManager().saveSpawner(spawner);
        
        // 彻底禁用原版刷怪笼生成
        disableVanillaSpawner(location, entityType);
        
        return spawner;
    }
    
    /**
     * 彻底禁用原版刷怪笼生成
     */
    private void disableVanillaSpawner(Location location, EntityType entityType) {
        Block block = location.getBlock();
        if (block.getType() == Material.SPAWNER) {
            CreatureSpawner cs = (CreatureSpawner) block.getState();
            cs.setSpawnedType(entityType);
            cs.setMaxSpawnDelay(Integer.MAX_VALUE); // 先设置最大延迟
            cs.setMinSpawnDelay(Integer.MAX_VALUE); // 再设置最小延迟
            cs.setDelay(Integer.MAX_VALUE); // 设置当前延迟为最大值
            cs.setSpawnCount(0); // 生成数量设为0
            cs.setMaxNearbyEntities(0); // 最大附近实体设为0
            cs.setRequiredPlayerRange(0); // 激活范围设为0
            cs.setSpawnRange(0); // 生成范围设为0
            cs.update(true, false); // 强制更新
        }
    }
    
    /**
     * 移除刷怪笼
     */
    public void removeSpawner(Location location) {
        Spawner spawner = spawners.remove(location);
        if (spawner != null) {
            Set<Location> locations = playerSpawners.get(spawner.getOwner());
            if (locations != null) {
                locations.remove(location);
            }
            plugin.getDataManager().deleteSpawner(location);
        }
    }
    
    /**
     * 获取刷怪笼
     */
    public Spawner getSpawner(Location location) {
        return spawners.get(location);
    }
    
    /**
     * 获取玩家的所有刷怪笼
     */
    public List<Spawner> getPlayerSpawners(UUID player) {
        Set<Location> locations = playerSpawners.get(player);
        if (locations == null) {
            return new ArrayList<>();
        }
        
        List<Spawner> result = new ArrayList<>();
        for (Location loc : locations) {
            Spawner spawner = spawners.get(loc);
            if (spawner != null) {
                result.add(spawner);
            }
        }
        return result;
    }
    
    /**
     * 获取玩家指定类型的刷怪笼数量
     */
    public int getPlayerSpawnerCount(UUID player, SpawnerType type) {
        return (int) getPlayerSpawners(player).stream()
                .filter(s -> s.getType() == type)
                .count();
    }
    
    /**
     * 检查位置是否是刷怪笼
     */
    public boolean isSpawner(Location location) {
        return spawners.containsKey(location);
    }
    
    public void updateSpawnerEntity(Location location, EntityType entityType) {
        Spawner spawner = spawners.get(location);
        if (spawner != null) {
            spawner.setEntityType(entityType);
            
            // 彻底禁用原版刷怪笼生成
            disableVanillaSpawner(location, entityType);
            
            plugin.getDataManager().saveSpawner(spawner);
        }
    }
    
    /**
     * 保存单个刷怪笼
     */
    public void saveSpawner(Spawner spawner) {
        plugin.getDataManager().saveSpawner(spawner);
    }
}
