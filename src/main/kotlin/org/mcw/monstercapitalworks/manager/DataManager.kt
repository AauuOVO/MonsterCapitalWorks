package org.mcw.monstercapitalworks.manager

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import org.mcw.monstercapitalworks.model.PlayerData
import org.mcw.monstercapitalworks.model.Spawner
import org.mcw.monstercapitalworks.model.SpawnerType
import java.io.File
import java.sql.*
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 数据管理器 - 处理SQLite数据库操作
 */
class DataManager(private val plugin: MonsterCapitalWorks) {
    
    private var connection: Connection? = null
    private val playerDataCache = mutableMapOf<UUID, PlayerData>()
    private val spawnerCache = mutableMapOf<Int, Spawner>()

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
     * 初始化数据库连接
     */
    fun initialize() {
        try {
            val dataFolder = plugin.dataFolder
            if (!dataFolder.exists()) {
                dataFolder.mkdirs()
            }

            val dbFile = File(dataFolder, "data/mcw.db")
            if (!dbFile.parentFile.exists()) {
                dbFile.parentFile.mkdirs()
            }

            // 优化：添加SQLite性能优化参数
            val url = "jdbc:sqlite:${dbFile.absolutePath}" +
                "?journal_mode=WAL" +  // 使用WAL模式提高并发性能
                "&synchronous=NORMAL" + // 平衡性能和安全性
                "&cache_size=10000" +   // 增加缓存大小
                "&temp_store=MEMORY"    // 临时表存储在内存中
            connection = DriverManager.getConnection(url)
            
            // 启用外键约束
            connection!!.createStatement().use { stmt ->
                stmt.execute("PRAGMA foreign_keys = ON")
            }

            createTables()
            plugin.logger.info("数据库初始化成功")
        } catch (e: SQLException) {
            plugin.logger.severe("数据库初始化失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 创建数据库表
     */
    private fun createTables() {
        val tables = arrayOf(
            // 玩家数据表
            """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                normal_purchased_limit INTEGER DEFAULT 0,
                premium_purchased_limit INTEGER DEFAULT 0,
                created_at INTEGER DEFAULT 0,
                updated_at INTEGER DEFAULT 0
            )
            """.trimIndent(),
            
            // 玩家解锁实体表
            """
            CREATE TABLE IF NOT EXISTS player_entities (
                player_uuid TEXT,
                entity_type TEXT,
                spawner_type TEXT,
                PRIMARY KEY (player_uuid, entity_type, spawner_type)
            )
            """.trimIndent(),
            
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
            """.trimIndent(),
            
            // 刷怪笼升级表
            """
            CREATE TABLE IF NOT EXISTS spawner_upgrades (
                spawner_id INTEGER,
                upgrade_type TEXT,
                level INTEGER DEFAULT 0,
                PRIMARY KEY (spawner_id, upgrade_type),
                FOREIGN KEY (spawner_id) REFERENCES spawners(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        connection!!.createStatement().use { stmt ->
            for (sql in tables) {
                stmt.execute(sql)
            }
        }
    }

    /**
     * 异步加载玩家数据
     */
    fun loadPlayerData(uuid: UUID): CompletableFuture<PlayerData> {
        return CompletableFuture.supplyAsync {
            if (playerDataCache.containsKey(uuid)) {
                return@supplyAsync playerDataCache[uuid]!!
            }

            try {
                val data = PlayerData(uuid)

                // 加载购买的限制
                var sql = "SELECT normal_purchased_limit, premium_purchased_limit FROM players WHERE uuid = ?"
                connection!!.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        data.normalPurchasedLimit = rs.getInt("normal_purchased_limit")
                        data.premiumPurchasedLimit = rs.getInt("premium_purchased_limit")
                    }
                }

                // 加载解锁的实体
                sql = "SELECT entity_type, spawner_type FROM player_entities WHERE player_uuid = ?"
                connection!!.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                while (rs.next()) {
                    val entityType = EntityType.valueOf(rs.getString("entity_type"))
                    val spawnerTypeStr = rs.getString("spawner_type")
                    val spawnerType = if (spawnerTypeStr != null) SpawnerType.fromKey(spawnerTypeStr) else null
                    if (spawnerType != null) {
                        data.unlockEntity(spawnerType, entityType)
                    }
                }
                }

                playerDataCache[uuid] = data
                data
            } catch (e: SQLException) {
                plugin.logger.severe("加载玩家数据失败: ${e.message}")
                PlayerData(uuid)
            }
        }
    }

    /**
     * 异步保存玩家数据
     */
    fun savePlayerData(data: PlayerData): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            try {
            // 保存购买限制
            var sql = "INSERT OR REPLACE INTO players (uuid, normal_purchased_limit, premium_purchased_limit) VALUES (?, ?, ?)"
            connection!!.prepareStatement(sql).use { stmt ->
                stmt.setString(1, data.uuid.toString())
                stmt.setInt(2, data.getPurchasedLimit(SpawnerType.NORMAL))
                stmt.setInt(3, data.getPurchasedLimit(SpawnerType.PREMIUM))
                stmt.executeUpdate()
            }

                // 删除旧的实体解锁记录
                sql = "DELETE FROM player_entities WHERE player_uuid = ?"
                connection!!.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, data.uuid.toString())
                    stmt.executeUpdate()
                }

                // 保存新的实体解锁记录
                sql = "INSERT INTO player_entities (player_uuid, entity_type, spawner_type) VALUES (?, ?, ?)"
                connection!!.prepareStatement(sql).use { stmt ->
                    for (type in SpawnerType.values()) {
                        for (entityType in data.getUnlockedEntities(type)) {
                            stmt.setString(1, data.uuid.toString())
                            stmt.setString(2, entityType.name)
                            stmt.setString(3, type.key)
                            stmt.addBatch()
                        }
                    }
                    stmt.executeBatch()
                }
            } catch (e: SQLException) {
                plugin.logger.severe("保存玩家数据失败: ${e.message}")
            }
        }
    }

    /**
     * 同步保存刷怪笼数据
     */
    fun saveSpawner(spawner: Spawner) {
        try {
            val loc = normalizeLocation(spawner.location)
            val sql: String
            
            if (spawner.id == 0) {
                // 新刷怪笼
                sql = "INSERT INTO spawners (owner, type, entity_type, world, x, y, z, stored_spawns, last_spawn_time, last_release_time, active, spawn_mode, precise_x, precise_y, precise_z, spawn_delay, spawn_count, max_nearby_entities, activation_range, storage_enabled, max_storage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            } else {
                // 更新现有刷怪笼
                sql = "UPDATE spawners SET entity_type = ?, stored_spawns = ?, last_spawn_time = ?, last_release_time = ?, active = ?, spawn_mode = ?, precise_x = ?, precise_y = ?, precise_z = ?, spawn_delay = ?, spawn_count = ?, max_nearby_entities = ?, activation_range = ?, storage_enabled = ?, max_storage = ? WHERE id = ?"
            }

            connection!!.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                if (spawner.id == 0) {
                    stmt.setString(1, spawner.owner.toString())
                    stmt.setString(2, spawner.type.key)
                    stmt.setString(3, spawner.entityType.name)
                    stmt.setString(4, loc.world!!.name)
                    stmt.setDouble(5, loc.x)
                    stmt.setDouble(6, loc.y)
                    stmt.setDouble(7, loc.z)
                    stmt.setInt(8, spawner.storedSpawns)
                    stmt.setLong(9, spawner.lastSpawnTime)
                    stmt.setLong(10, spawner.lastReleaseTime)
                    stmt.setInt(11, if (spawner.active) 1 else 0)
                    stmt.setString(12, spawner.spawnMode.name)
                    stmt.setDouble(13, spawner.preciseX)
                    stmt.setDouble(14, spawner.preciseY)
                    stmt.setDouble(15, spawner.preciseZ)
                    stmt.setInt(16, spawner.spawnDelay)
                    stmt.setInt(17, spawner.spawnCount)
                    stmt.setInt(18, spawner.maxNearbyEntities)
                    stmt.setInt(19, spawner.activationRange)
                    stmt.setInt(20, if (spawner.storageEnabled) 1 else 0)
                    stmt.setInt(21, spawner.maxStorage)
                } else {
                    stmt.setString(1, spawner.entityType.name)
                    stmt.setInt(2, spawner.storedSpawns)
                    stmt.setLong(3, spawner.lastSpawnTime)
                    stmt.setLong(4, spawner.lastReleaseTime)
                    stmt.setInt(5, if (spawner.active) 1 else 0)
                    stmt.setString(6, spawner.spawnMode.name)
                    stmt.setDouble(7, spawner.preciseX)
                    stmt.setDouble(8, spawner.preciseY)
                    stmt.setDouble(9, spawner.preciseZ)
                    stmt.setInt(10, spawner.spawnDelay)
                    stmt.setInt(11, spawner.spawnCount)
                    stmt.setInt(12, spawner.maxNearbyEntities)
                    stmt.setInt(13, spawner.activationRange)
                    stmt.setInt(14, if (spawner.storageEnabled) 1 else 0)
                    stmt.setInt(15, spawner.maxStorage)
                    stmt.setInt(16, spawner.id)
                }
                
                stmt.executeUpdate()

                var id = spawner.id
                if (id == 0) {
                    val rs = stmt.generatedKeys
                    if (rs.next()) {
                        id = rs.getInt(1)
                        spawner.id = id
                    }
                }

                // 保存升级等级
                saveSpawnerUpgrades(id, spawner.upgradeLevels)
                
                spawnerCache[id] = spawner
            }
        } catch (e: SQLException) {
            plugin.logger.severe("保存刷怪笼失败: ${e.message}")
        }
    }
    
    /**
     * 异步保存刷怪笼数据
     */
    fun saveSpawnerAsync(spawner: Spawner): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val loc = normalizeLocation(spawner.location)
                val sql: String
                
                if (spawner.id == 0) {
                    // 新刷怪笼
                    sql = "INSERT INTO spawners (owner, type, entity_type, world, x, y, z, stored_spawns, last_spawn_time, last_release_time, active, spawn_mode, precise_x, precise_y, precise_z, spawn_delay, spawn_count, max_nearby_entities, activation_range, storage_enabled, max_storage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                } else {
                    // 更新现有刷怪笼
                    sql = "UPDATE spawners SET entity_type = ?, stored_spawns = ?, last_spawn_time = ?, last_release_time = ?, active = ?, spawn_mode = ?, precise_x = ?, precise_y = ?, precise_z = ?, spawn_delay = ?, spawn_count = ?, max_nearby_entities = ?, activation_range = ?, storage_enabled = ?, max_storage = ? WHERE id = ?"
                }

                connection!!.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                    if (spawner.id == 0) {
                        stmt.setString(1, spawner.owner.toString())
                        stmt.setString(2, spawner.type.key)
                        stmt.setString(3, spawner.entityType.name)
                        stmt.setString(4, loc.world!!.name)
                        stmt.setDouble(5, loc.x)
                        stmt.setDouble(6, loc.y)
                        stmt.setDouble(7, loc.z)
                        stmt.setInt(8, spawner.storedSpawns)
                        stmt.setLong(9, spawner.lastSpawnTime)
                        stmt.setLong(10, spawner.lastReleaseTime)
                        stmt.setInt(11, if (spawner.active) 1 else 0)
                        stmt.setString(12, spawner.spawnMode.name)
                        stmt.setDouble(13, spawner.preciseX)
                        stmt.setDouble(14, spawner.preciseY)
                        stmt.setDouble(15, spawner.preciseZ)
                        stmt.setInt(16, spawner.spawnDelay)
                        stmt.setInt(17, spawner.spawnCount)
                        stmt.setInt(18, spawner.maxNearbyEntities)
                        stmt.setInt(19, spawner.activationRange)
                        stmt.setInt(20, if (spawner.storageEnabled) 1 else 0)
                        stmt.setInt(21, spawner.maxStorage)
                    } else {
                        stmt.setString(1, spawner.entityType.name)
                        stmt.setInt(2, spawner.storedSpawns)
                        stmt.setLong(3, spawner.lastSpawnTime)
                        stmt.setLong(4, spawner.lastReleaseTime)
                        stmt.setInt(5, if (spawner.active) 1 else 0)
                        stmt.setString(6, spawner.spawnMode.name)
                        stmt.setDouble(7, spawner.preciseX)
                        stmt.setDouble(8, spawner.preciseY)
                        stmt.setDouble(9, spawner.preciseZ)
                        stmt.setInt(10, spawner.spawnDelay)
                        stmt.setInt(11, spawner.spawnCount)
                        stmt.setInt(12, spawner.maxNearbyEntities)
                        stmt.setInt(13, spawner.activationRange)
                        stmt.setInt(14, if (spawner.storageEnabled) 1 else 0)
                        stmt.setInt(15, spawner.maxStorage)
                        stmt.setInt(16, spawner.id)
                    }
                    
                    stmt.executeUpdate()

                    var id = spawner.id
                    if (id == 0) {
                        val rs = stmt.generatedKeys
                        if (rs.next()) {
                            id = rs.getInt(1)
                        }
                    }

                    // 保存升级等级
                    saveSpawnerUpgrades(id, spawner.upgradeLevels)
                    
                    spawnerCache[id] = spawner
                    id
                }
            } catch (e: SQLException) {
                plugin.logger.severe("保存刷怪笼失败: ${e.message}")
                0
            }
        }
    }

    /**
     * 保存刷怪笼升级等级
     */
    private fun saveSpawnerUpgrades(spawnerId: Int, upgrades: Map<String, Int>) {
        try {
            // 删除旧记录
            var sql = "DELETE FROM spawner_upgrades WHERE spawner_id = ?"
            connection!!.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, spawnerId)
                stmt.executeUpdate()
            }

            // 插入新记录
            sql = "INSERT INTO spawner_upgrades (spawner_id, upgrade_type, level) VALUES (?, ?, ?)"
            connection!!.prepareStatement(sql).use { stmt ->
                for ((upgradeType, level) in upgrades) {
                    stmt.setInt(1, spawnerId)
                    stmt.setString(2, upgradeType)
                    stmt.setInt(3, level)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        } catch (e: SQLException) {
            plugin.logger.severe("保存刷怪笼升级失败: ${e.message}")
        }
    }

    /**
     * 同步加载所有刷怪笼
     */
    fun loadAllSpawners(): List<Spawner> {
        val spawners = mutableListOf<Spawner>()
        try {
            val sql = "SELECT * FROM spawners"
            connection!!.createStatement().use { stmt ->
                val rs = stmt.executeQuery(sql)
                
                while (rs.next()) {
                    val id = rs.getInt("id")
                    val owner = UUID.fromString(rs.getString("owner"))
                    val type = SpawnerType.fromKey(rs.getString("type"))
                    val entityType = EntityType.valueOf(rs.getString("entity_type"))
                    
                    // 使用标准化的Location（方块坐标）
                    val location = Location(
                        Bukkit.getWorld(rs.getString("world")),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z")
                    )

                    val spawner = Spawner(id, owner, type, entityType, location)
                    
                    // 加载基本数据
                    spawner.storedSpawns = rs.getInt("stored_spawns")
                    spawner.lastSpawnTime = rs.getLong("last_spawn_time")
                    spawner.lastReleaseTime = rs.getLong("last_release_time")
                    spawner.active = rs.getInt("active") == 1
                    
                    // 加载生成模式和精确位置
                    val spawnModeStr = rs.getString("spawn_mode")
                    spawner.spawnMode = if (spawnModeStr != null) Spawner.SpawnMode.valueOf(spawnModeStr) else Spawner.SpawnMode.RANDOM
                    spawner.preciseX = rs.getDouble("precise_x")
                    spawner.preciseY = rs.getDouble("precise_y")
                    spawner.preciseZ = rs.getDouble("precise_z")
                    
                    // 加载刷怪参数
                    spawner.spawnDelay = rs.getInt("spawn_delay")
                    spawner.spawnCount = rs.getInt("spawn_count")
                    spawner.maxNearbyEntities = rs.getInt("max_nearby_entities")
                    spawner.activationRange = rs.getInt("activation_range")
                    
                    // 加载存储相关
                    spawner.storageEnabled = rs.getInt("storage_enabled") == 1
                    spawner.maxStorage = rs.getInt("max_storage")

                    // 加载升级等级
                    loadSpawnerUpgrades(spawner)

                    spawners.add(spawner)
                    spawnerCache[id] = spawner
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("加载刷怪笼失败: ${e.message}")
        }
        return spawners
    }
    
    /**
     * 异步加载所有刷怪笼
     */
    fun loadAllSpawnersAsync(): CompletableFuture<List<Spawner>> {
        return CompletableFuture.supplyAsync { loadAllSpawners() }
    }

    /**
     * 加载刷怪笼升级等级
     */
    private fun loadSpawnerUpgrades(spawner: Spawner) {
        try {
            val sql = "SELECT upgrade_type, level FROM spawner_upgrades WHERE spawner_id = ?"
            connection!!.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, spawner.id)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    spawner.setUpgradeLevel(rs.getString("upgrade_type"), rs.getInt("level"))
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("加载刷怪笼升级失败: ${e.message}")
        }
    }

    /**
     * 同步删除刷怪笼（通过Location）
     */
    fun deleteSpawner(location: Location) {
        try {
            val normalizedLoc = normalizeLocation(location)
            val sql = "DELETE FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?"
            connection!!.prepareStatement(sql).use { stmt ->
                stmt.setString(1, normalizedLoc.world!!.name)
                stmt.setInt(2, normalizedLoc.blockX)
                stmt.setInt(3, normalizedLoc.blockY)
                stmt.setInt(4, normalizedLoc.blockZ)
                stmt.executeUpdate()
            }
            
            // 从缓存中移除
            spawnerCache.values.removeIf { s ->
                val spawnerLoc = normalizeLocation(s.location)
                spawnerLoc == normalizedLoc
            }
        } catch (e: SQLException) {
            plugin.logger.severe("删除刷怪笼失败: ${e.message}")
        }
    }
    
    /**
     * 同步删除刷怪笼（通过ID）
     */
    fun deleteSpawner(id: Int) {
        try {
            val sql = "DELETE FROM spawners WHERE id = ?"
            connection!!.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                stmt.executeUpdate()
            }
            spawnerCache.remove(id)
        } catch (e: SQLException) {
            plugin.logger.severe("删除刷怪笼失败: ${e.message}")
        }
    }
    
    /**
     * 异步删除刷怪笼
     */
    fun deleteSpawnerAsync(id: Int): CompletableFuture<Void> {
        return CompletableFuture.runAsync { deleteSpawner(id) }
    }

    /**
     * 同步获取玩家数据
     */
    fun getPlayerData(uuid: UUID): PlayerData {
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache[uuid]!!
        }
        
        // 如果缓存中没有，同步加载
        return try {
            val data = PlayerData(uuid)

            // 加载购买的限制
            var sql = "SELECT normal_purchased_limit, premium_purchased_limit FROM players WHERE uuid = ?"
            connection!!.prepareStatement(sql).use { stmt ->
                stmt.setString(1, uuid.toString())
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    data.normalPurchasedLimit = rs.getInt("normal_purchased_limit")
                    data.premiumPurchasedLimit = rs.getInt("premium_purchased_limit")
                }
            }

            // 加载解锁的实体
            sql = "SELECT entity_type, spawner_type FROM player_entities WHERE player_uuid = ?"
            connection!!.prepareStatement(sql).use { stmt ->
                stmt.setString(1, uuid.toString())
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val entityType = EntityType.valueOf(rs.getString("entity_type"))
                    val spawnerTypeStr = rs.getString("spawner_type")
                    val spawnerType = if (spawnerTypeStr != null) SpawnerType.fromKey(spawnerTypeStr) else null
                    if (spawnerType != null) {
                        data.unlockEntity(spawnerType, entityType)
                    }
                }
            }

            playerDataCache[uuid] = data
            data
        } catch (e: SQLException) {
            plugin.logger.severe("加载玩家数据失败: ${e.message}")
            PlayerData(uuid)
        }
    }
    
    /**
     * 获取玩家数据缓存
     */
    fun getPlayerDataCache(uuid: UUID): PlayerData? = playerDataCache[uuid]

    /**
     * 获取刷怪笼缓存
     */
    fun getSpawnerCache(id: Int): Spawner? = spawnerCache[id]

    /**
     * 关闭数据库连接
     */
    fun close() {
        try {
            if (connection != null && !connection!!.isClosed) {
                connection!!.close()
            }
        } catch (e: SQLException) {
            plugin.logger.severe("关闭数据库连接失败: ${e.message}")
        }
    }
}
