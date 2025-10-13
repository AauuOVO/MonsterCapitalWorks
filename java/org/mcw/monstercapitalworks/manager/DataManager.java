package org.mcw.monstercapitalworks.manager;

import org.mcw.monstercapitalworks.model.PlayerData;
import org.mcw.monstercapitalworks.model.Spawner;
import org.mcw.monstercapitalworks.model.SpawnerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 数据管理器 - 处理SQLite数据库操作
 */
public class DataManager {
    private final org.mcw.monstercapitalworks.MonsterCapitalWorks plugin;
    private Connection connection;
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<Integer, Spawner> spawnerCache;

    public DataManager(org.mcw.monstercapitalworks.MonsterCapitalWorks plugin) {
        this.plugin = plugin;
        this.playerDataCache = new HashMap<>();
        this.spawnerCache = new HashMap<>();
    }
    
    /**
     * 标准化Location - 确保使用方块坐标（整数）
     * 这样可以避免浮点数精度问题导致的Location比较失败
     */
    private Location normalizeLocation(Location location) {
        return new Location(
            location.getWorld(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    /**
     * 初始化数据库连接
     */
    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "data/mcw.db");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            // 优化：添加SQLite性能优化参数
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath() + 
                "?journal_mode=WAL" +  // 使用WAL模式提高并发性能
                "&synchronous=NORMAL" + // 平衡性能和安全性
                "&cache_size=10000" +   // 增加缓存大小
                "&temp_store=MEMORY";   // 临时表存储在内存中
            connection = DriverManager.getConnection(url);
            
            // 启用外键约束
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            createTables();
            plugin.getLogger().info("数据库初始化成功");
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建数据库表
     */
    private void createTables() throws SQLException {
        String[] tables = {
            // 玩家数据表
            """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                normal_purchased_limit INTEGER DEFAULT 0,
                premium_purchased_limit INTEGER DEFAULT 0,
                created_at INTEGER DEFAULT 0,
                updated_at INTEGER DEFAULT 0
            )
            """,
            
            // 玩家解锁实体表
            """
            CREATE TABLE IF NOT EXISTS player_entities (
                player_uuid TEXT,
                entity_type TEXT,
                spawner_type TEXT,
                PRIMARY KEY (player_uuid, entity_type, spawner_type)
            )
            """,
            
            // 刷怪笼数据表
            """
            CREATE TABLE IF NOT EXISTS spawners (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                owner TEXT NOT NULL,
                type TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                world TEXT NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                stored_spawns INTEGER DEFAULT 0,
                last_spawn_time BIGINT DEFAULT 0,
                last_release_time BIGINT DEFAULT 0,
                active INTEGER DEFAULT 1,
                spawn_mode TEXT DEFAULT 'RANDOM',
                precise_x DOUBLE DEFAULT 0,
                precise_y DOUBLE DEFAULT 1,
                precise_z DOUBLE DEFAULT 0,
                spawn_delay INTEGER DEFAULT 100,
                spawn_count INTEGER DEFAULT 1,
                max_nearby_entities INTEGER DEFAULT 6,
                activation_range INTEGER DEFAULT 16,
                storage_enabled INTEGER DEFAULT 0,
                max_storage INTEGER DEFAULT 100
            )
            """,
            
            // 刷怪笼升级表
            """
            CREATE TABLE IF NOT EXISTS spawner_upgrades (
                spawner_id INTEGER,
                upgrade_type TEXT,
                level INTEGER DEFAULT 0,
                PRIMARY KEY (spawner_id, upgrade_type),
                FOREIGN KEY (spawner_id) REFERENCES spawners(id) ON DELETE CASCADE
            )
            """
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
        }
    }

    /**
     * 异步加载玩家数据
     */
    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (playerDataCache.containsKey(uuid)) {
                return playerDataCache.get(uuid);
            }

            try {
                PlayerData data = new PlayerData(uuid);

                // 加载购买的限制
                String sql = "SELECT normal_purchased_limit, premium_purchased_limit FROM players WHERE uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        data.setNormalPurchasedLimit(rs.getInt("normal_purchased_limit"));
                        data.setPremiumPurchasedLimit(rs.getInt("premium_purchased_limit"));
                    }
                }

                // 加载解锁的实体
                sql = "SELECT entity_type, spawner_type FROM player_entities WHERE player_uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        EntityType entityType = EntityType.valueOf(rs.getString("entity_type"));
                        SpawnerType spawnerType = SpawnerType.fromKey(rs.getString("spawner_type"));
                        data.unlockEntity(spawnerType, entityType);
                    }
                }

                playerDataCache.put(uuid, data);
                return data;
            } catch (SQLException e) {
                plugin.getLogger().severe("加载玩家数据失败: " + e.getMessage());
                return new PlayerData(uuid);
            }
        });
    }

    /**
     * 异步保存玩家数据
     */
    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 保存购买限制
                String sql = "INSERT OR REPLACE INTO players (uuid, normal_purchased_limit, premium_purchased_limit) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, data.getUuid().toString());
                    stmt.setInt(2, data.getNormalPurchasedLimit());
                    stmt.setInt(3, data.getPremiumPurchasedLimit());
                    stmt.executeUpdate();
                }

                // 删除旧的实体解锁记录
                sql = "DELETE FROM player_entities WHERE player_uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, data.getUuid().toString());
                    stmt.executeUpdate();
                }

                // 保存新的实体解锁记录
                sql = "INSERT INTO player_entities (player_uuid, entity_type, spawner_type) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    for (SpawnerType type : SpawnerType.values()) {
                        for (EntityType entityType : data.getUnlockedEntities(type)) {
                            stmt.setString(1, data.getUuid().toString());
                            stmt.setString(2, entityType.name());
                            stmt.setString(3, type.getKey());
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("保存玩家数据失败: " + e.getMessage());
            }
        });
    }

    /**
     * 同步保存刷怪笼数据
     */
    public void saveSpawner(Spawner spawner) {
        try {
            Location loc = normalizeLocation(spawner.getLocation());
            String sql;
            
            if (spawner.getId() == 0) {
                // 新刷怪笼
                sql = "INSERT INTO spawners (owner, type, entity_type, world, x, y, z, stored_spawns, last_spawn_time, last_release_time, active, spawn_mode, precise_x, precise_y, precise_z, spawn_delay, spawn_count, max_nearby_entities, activation_range, storage_enabled, max_storage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            } else {
                // 更新现有刷怪笼
                sql = "UPDATE spawners SET entity_type = ?, stored_spawns = ?, last_spawn_time = ?, last_release_time = ?, active = ?, spawn_mode = ?, precise_x = ?, precise_y = ?, precise_z = ?, spawn_delay = ?, spawn_count = ?, max_nearby_entities = ?, activation_range = ?, storage_enabled = ?, max_storage = ? WHERE id = ?";
            }

            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (spawner.getId() == 0) {
                    stmt.setString(1, spawner.getOwner().toString());
                    stmt.setString(2, spawner.getType().getKey());
                    stmt.setString(3, spawner.getEntityType().name());
                    stmt.setString(4, loc.getWorld().getName());
                    stmt.setDouble(5, loc.getX());
                    stmt.setDouble(6, loc.getY());
                    stmt.setDouble(7, loc.getZ());
                    stmt.setInt(8, spawner.getStoredSpawns());
                    stmt.setLong(9, spawner.getLastSpawnTime());
                    stmt.setLong(10, spawner.getLastReleaseTime());
                    stmt.setInt(11, spawner.isActive() ? 1 : 0);
                    stmt.setString(12, spawner.getSpawnMode().name());
                    stmt.setDouble(13, spawner.getPreciseX());
                    stmt.setDouble(14, spawner.getPreciseY());
                    stmt.setDouble(15, spawner.getPreciseZ());
                    stmt.setInt(16, spawner.getSpawnDelay());
                    stmt.setInt(17, spawner.getSpawnCount());
                    stmt.setInt(18, spawner.getMaxNearbyEntities());
                    stmt.setInt(19, spawner.getActivationRange());
                    stmt.setInt(20, spawner.isStorageEnabled() ? 1 : 0);
                    stmt.setInt(21, spawner.getMaxStorage());
                } else {
                    stmt.setString(1, spawner.getEntityType().name());
                    stmt.setInt(2, spawner.getStoredSpawns());
                    stmt.setLong(3, spawner.getLastSpawnTime());
                    stmt.setLong(4, spawner.getLastReleaseTime());
                    stmt.setInt(5, spawner.isActive() ? 1 : 0);
                    stmt.setString(6, spawner.getSpawnMode().name());
                    stmt.setDouble(7, spawner.getPreciseX());
                    stmt.setDouble(8, spawner.getPreciseY());
                    stmt.setDouble(9, spawner.getPreciseZ());
                    stmt.setInt(10, spawner.getSpawnDelay());
                    stmt.setInt(11, spawner.getSpawnCount());
                    stmt.setInt(12, spawner.getMaxNearbyEntities());
                    stmt.setInt(13, spawner.getActivationRange());
                    stmt.setInt(14, spawner.isStorageEnabled() ? 1 : 0);
                    stmt.setInt(15, spawner.getMaxStorage());
                    stmt.setInt(16, spawner.getId());
                }
                
                stmt.executeUpdate();

                int id = spawner.getId();
                if (id == 0) {
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        id = rs.getInt(1);
                        spawner.setId(id);
                    }
                }

                // 保存升级等级
                saveSpawnerUpgrades(id, spawner.getUpgradeLevels());
                
                spawnerCache.put(id, spawner);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存刷怪笼失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步保存刷怪笼数据
     */
    public CompletableFuture<Integer> saveSpawnerAsync(Spawner spawner) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Location loc = normalizeLocation(spawner.getLocation());
                String sql;
                
                if (spawner.getId() == 0) {
                    // 新刷怪笼
                    sql = "INSERT INTO spawners (owner, type, entity_type, world, x, y, z, stored_spawns, last_spawn_time, last_release_time, active, spawn_mode, precise_x, precise_y, precise_z, spawn_delay, spawn_count, max_nearby_entities, activation_range, storage_enabled, max_storage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                } else {
                    // 更新现有刷怪笼
                    sql = "UPDATE spawners SET entity_type = ?, stored_spawns = ?, last_spawn_time = ?, last_release_time = ?, active = ?, spawn_mode = ?, precise_x = ?, precise_y = ?, precise_z = ?, spawn_delay = ?, spawn_count = ?, max_nearby_entities = ?, activation_range = ?, storage_enabled = ?, max_storage = ? WHERE id = ?";
                }

                try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    if (spawner.getId() == 0) {
                        stmt.setString(1, spawner.getOwner().toString());
                        stmt.setString(2, spawner.getType().getKey());
                        stmt.setString(3, spawner.getEntityType().name());
                        stmt.setString(4, loc.getWorld().getName());
                        stmt.setDouble(5, loc.getX());
                        stmt.setDouble(6, loc.getY());
                        stmt.setDouble(7, loc.getZ());
                        stmt.setInt(8, spawner.getStoredSpawns());
                        stmt.setLong(9, spawner.getLastSpawnTime());
                        stmt.setLong(10, spawner.getLastReleaseTime());
                        stmt.setInt(11, spawner.isActive() ? 1 : 0);
                        stmt.setString(12, spawner.getSpawnMode().name());
                        stmt.setDouble(13, spawner.getPreciseX());
                        stmt.setDouble(14, spawner.getPreciseY());
                        stmt.setDouble(15, spawner.getPreciseZ());
                        stmt.setInt(16, spawner.getSpawnDelay());
                        stmt.setInt(17, spawner.getSpawnCount());
                        stmt.setInt(18, spawner.getMaxNearbyEntities());
                        stmt.setInt(19, spawner.getActivationRange());
                        stmt.setInt(20, spawner.isStorageEnabled() ? 1 : 0);
                        stmt.setInt(21, spawner.getMaxStorage());
                    } else {
                        stmt.setString(1, spawner.getEntityType().name());
                        stmt.setInt(2, spawner.getStoredSpawns());
                        stmt.setLong(3, spawner.getLastSpawnTime());
                        stmt.setLong(4, spawner.getLastReleaseTime());
                        stmt.setInt(5, spawner.isActive() ? 1 : 0);
                        stmt.setString(6, spawner.getSpawnMode().name());
                        stmt.setDouble(7, spawner.getPreciseX());
                        stmt.setDouble(8, spawner.getPreciseY());
                        stmt.setDouble(9, spawner.getPreciseZ());
                        stmt.setInt(10, spawner.getSpawnDelay());
                        stmt.setInt(11, spawner.getSpawnCount());
                        stmt.setInt(12, spawner.getMaxNearbyEntities());
                        stmt.setInt(13, spawner.getActivationRange());
                        stmt.setInt(14, spawner.isStorageEnabled() ? 1 : 0);
                        stmt.setInt(15, spawner.getMaxStorage());
                        stmt.setInt(16, spawner.getId());
                    }
                    
                    stmt.executeUpdate();

                    int id = spawner.getId();
                    if (id == 0) {
                        ResultSet rs = stmt.getGeneratedKeys();
                        if (rs.next()) {
                            id = rs.getInt(1);
                        }
                    }

                    // 保存升级等级
                    saveSpawnerUpgrades(id, spawner.getUpgradeLevels());
                    
                    spawnerCache.put(id, spawner);
                    return id;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("保存刷怪笼失败: " + e.getMessage());
                return 0;
            }
        });
    }

    /**
     * 保存刷怪笼升级等级
     */
    private void saveSpawnerUpgrades(int spawnerId, Map<String, Integer> upgrades) throws SQLException {
        // 删除旧记录
        String sql = "DELETE FROM spawner_upgrades WHERE spawner_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, spawnerId);
            stmt.executeUpdate();
        }

        // 插入新记录
        sql = "INSERT INTO spawner_upgrades (spawner_id, upgrade_type, level) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
                stmt.setInt(1, spawnerId);
                stmt.setString(2, entry.getKey());
                stmt.setInt(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * 同步加载所有刷怪笼
     */
    public List<Spawner> loadAllSpawners() {
        List<Spawner> spawners = new ArrayList<>();
        try {
            String sql = "SELECT * FROM spawners";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    UUID owner = UUID.fromString(rs.getString("owner"));
                    SpawnerType type = SpawnerType.fromKey(rs.getString("type"));
                    EntityType entityType = EntityType.valueOf(rs.getString("entity_type"));
                    
                    // 使用标准化的Location（方块坐标）
                    Location location = new Location(
                        Bukkit.getWorld(rs.getString("world")),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z")
                    );

                    Spawner spawner = new Spawner(id, owner, type, entityType, location);
                    
                    // 加载基本数据
                    spawner.setStoredSpawns(rs.getInt("stored_spawns"));
                    spawner.setLastSpawnTime(rs.getLong("last_spawn_time"));
                    spawner.setLastReleaseTime(rs.getLong("last_release_time"));
                    spawner.setActive(rs.getInt("active") == 1);
                    
                    // 加载生成模式和精确位置
                    String spawnModeStr = rs.getString("spawn_mode");
                    spawner.setSpawnMode(spawnModeStr != null ? Spawner.SpawnMode.valueOf(spawnModeStr) : Spawner.SpawnMode.RANDOM);
                    spawner.setPreciseX(rs.getDouble("precise_x"));
                    spawner.setPreciseY(rs.getDouble("precise_y"));
                    spawner.setPreciseZ(rs.getDouble("precise_z"));
                    
                    // 加载刷怪参数
                    spawner.setSpawnDelay(rs.getInt("spawn_delay"));
                    spawner.setSpawnCount(rs.getInt("spawn_count"));
                    spawner.setMaxNearbyEntities(rs.getInt("max_nearby_entities"));
                    spawner.setActivationRange(rs.getInt("activation_range"));
                    
                    // 加载存储相关
                    spawner.setStorageEnabled(rs.getInt("storage_enabled") == 1);
                    spawner.setMaxStorage(rs.getInt("max_storage"));

                    // 加载升级等级
                    loadSpawnerUpgrades(spawner);

                    spawners.add(spawner);
                    spawnerCache.put(id, spawner);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载刷怪笼失败: " + e.getMessage());
        }
        return spawners;
    }
    
    /**
     * 异步加载所有刷怪笼
     */
    public CompletableFuture<List<Spawner>> loadAllSpawnersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Spawner> spawners = new ArrayList<>();
            try {
                String sql = "SELECT * FROM spawners";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        UUID owner = UUID.fromString(rs.getString("owner"));
                        SpawnerType type = SpawnerType.fromKey(rs.getString("type"));
                        EntityType entityType = EntityType.valueOf(rs.getString("entity_type"));
                        
                        // 使用标准化的Location（方块坐标）
                        Location location = new Location(
                            Bukkit.getWorld(rs.getString("world")),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            rs.getInt("z")
                        );

                        Spawner spawner = new Spawner(id, owner, type, entityType, location);
                        
                        // 加载基本数据
                        spawner.setStoredSpawns(rs.getInt("stored_spawns"));
                        spawner.setLastSpawnTime(rs.getLong("last_spawn_time"));
                        spawner.setLastReleaseTime(rs.getLong("last_release_time"));
                        spawner.setActive(rs.getInt("active") == 1);
                        
                        // 加载生成模式和精确位置
                        String spawnModeStr = rs.getString("spawn_mode");
                        spawner.setSpawnMode(spawnModeStr != null ? Spawner.SpawnMode.valueOf(spawnModeStr) : Spawner.SpawnMode.RANDOM);
                        spawner.setPreciseX(rs.getDouble("precise_x"));
                        spawner.setPreciseY(rs.getDouble("precise_y"));
                        spawner.setPreciseZ(rs.getDouble("precise_z"));
                        
                        // 加载刷怪参数
                        spawner.setSpawnDelay(rs.getInt("spawn_delay"));
                        spawner.setSpawnCount(rs.getInt("spawn_count"));
                        spawner.setMaxNearbyEntities(rs.getInt("max_nearby_entities"));
                        spawner.setActivationRange(rs.getInt("activation_range"));
                        
                        // 加载存储相关
                        spawner.setStorageEnabled(rs.getInt("storage_enabled") == 1);
                        spawner.setMaxStorage(rs.getInt("max_storage"));

                        // 加载升级等级
                        loadSpawnerUpgrades(spawner);

                        spawners.add(spawner);
                        spawnerCache.put(id, spawner);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("加载刷怪笼失败: " + e.getMessage());
            }
            return spawners;
        });
    }

    /**
     * 加载刷怪笼升级等级
     */
    private void loadSpawnerUpgrades(Spawner spawner) throws SQLException {
        String sql = "SELECT upgrade_type, level FROM spawner_upgrades WHERE spawner_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, spawner.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                spawner.setUpgradeLevel(rs.getString("upgrade_type"), rs.getInt("level"));
            }
        }
    }

    /**
     * 同步删除刷怪笼（通过Location）
     */
    public void deleteSpawner(Location location) {
        try {
            Location normalizedLoc = normalizeLocation(location);
            String sql = "DELETE FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, normalizedLoc.getWorld().getName());
                stmt.setInt(2, normalizedLoc.getBlockX());
                stmt.setInt(3, normalizedLoc.getBlockY());
                stmt.setInt(4, normalizedLoc.getBlockZ());
                stmt.executeUpdate();
            }
            
            // 从缓存中移除
            spawnerCache.values().removeIf(s -> {
                Location spawnerLoc = normalizeLocation(s.getLocation());
                return spawnerLoc.equals(normalizedLoc);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("删除刷怪笼失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步删除刷怪笼（通过ID）
     */
    public void deleteSpawner(int id) {
        try {
            String sql = "DELETE FROM spawners WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            spawnerCache.remove(id);
        } catch (SQLException e) {
            plugin.getLogger().severe("删除刷怪笼失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步删除刷怪笼
     */
    public CompletableFuture<Void> deleteSpawnerAsync(int id) {
        return CompletableFuture.runAsync(() -> deleteSpawner(id));
    }

    /**
     * 同步获取玩家数据
     */
    public PlayerData getPlayerData(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }
        
        // 如果缓存中没有，同步加载
        try {
            PlayerData data = new PlayerData(uuid);

            // 加载购买的限制
            String sql = "SELECT normal_purchased_limit, premium_purchased_limit FROM players WHERE uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    data.setNormalPurchasedLimit(rs.getInt("normal_purchased_limit"));
                    data.setPremiumPurchasedLimit(rs.getInt("premium_purchased_limit"));
                }
            }

            // 加载解锁的实体
            sql = "SELECT entity_type, spawner_type FROM player_entities WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    EntityType entityType = EntityType.valueOf(rs.getString("entity_type"));
                    SpawnerType spawnerType = SpawnerType.fromKey(rs.getString("spawner_type"));
                    data.unlockEntity(spawnerType, entityType);
                }
            }

            playerDataCache.put(uuid, data);
            return data;
        } catch (SQLException e) {
            plugin.getLogger().severe("加载玩家数据失败: " + e.getMessage());
            return new PlayerData(uuid);
        }
    }
    
    /**
     * 获取玩家数据缓存
     */
    public PlayerData getPlayerDataCache(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    /**
     * 获取刷怪笼缓存
     */
    public Spawner getSpawnerCache(int id) {
        return spawnerCache.get(id);
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("关闭数据库连接失败: " + e.getMessage());
        }
    }
}
