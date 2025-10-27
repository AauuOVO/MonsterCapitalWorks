package org.Aauu.monsterCapitalWorks.manager


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.Aauu.monsterCapitalWorks.async.AsyncTaskManager
import org.Aauu.monsterCapitalWorks.model.PlayerData
import org.Aauu.monsterCapitalWorks.model.Spawner
import org.Aauu.monsterCapitalWorks.model.SpawnerType
import org.Aauu.monsterCapitalWorks.model.SpawnMode
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import org.Aauu.monsterCapitalWorks.MonsterCapitalWorks
import java.io.File
import java.sql.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

/**
 * 数据管理器 - 支持SQLite和MySQL数据库
 * 使用连接池优化性能
 */
class DataManager(private val plugin: JavaPlugin) {
    private lateinit var dataSource: HikariDataSource
    private val spawners = ConcurrentHashMap<Location, Spawner>()
    private val playerSpawners = ConcurrentHashMap<UUID, MutableSet<Location>>()
    private val playerData = ConcurrentHashMap<UUID, PlayerData>()
    
    var databaseType: DatabaseType = DatabaseType.SQLITE
        private set

    enum class DatabaseType {
        SQLITE, MYSQL
    }

    fun initialize() {
        try {
            val config = plugin.config
            databaseType = when (config.getString("database.type", "sqlite")?.lowercase()) {
                "mysql" -> DatabaseType.MYSQL
                else -> DatabaseType.SQLITE
            }

            dataSource = when (databaseType) {
                DatabaseType.MYSQL -> createMySQLDataSource(config)
                DatabaseType.SQLITE -> createSQLiteDataSource(config)
            }

            createTables()
            plugin.logger.info("数据库连接成功 - 类型: $databaseType")
        } catch (e: Exception) {
            plugin.logger.severe("数据库初始化失败: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("数据库初始化失败", e)
        }
    }

    private fun createMySQLDataSource(config: org.bukkit.configuration.file.FileConfiguration): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.Aauu.monsterCapitalWorks.libs.mysql.cj.jdbc.Driver"
            
            val host = config.getString("database.mysql.host", "localhost")
            val port = config.getInt("database.mysql.port", 3306)
            val database = config.getString("database.mysql.database", "mcw_database")
            val username = config.getString("database.mysql.username", "root")
            val password = config.getString("database.mysql.password", "password")
            
            jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            this.username = username
            this.password = password

            // 连接池配置
            maximumPoolSize = config.getInt("database.mysql.pool.maximum_pool_size", 10)
            minimumIdle = config.getInt("database.mysql.pool.minimum_idle", 5)
            connectionTimeout = config.getLong("database.mysql.pool.connection_timeout", 30000)
            idleTimeout = config.getLong("database.mysql.pool.idle_timeout", 600000)
            maxLifetime = config.getLong("database.mysql.pool.max_lifetime", 1800000)

            // 性能优化配置
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
            addDataSourceProperty("cacheResultSetMetadata", "true")
            addDataSourceProperty("cacheServerConfiguration", "true")
            addDataSourceProperty("elideSetAutoCommits", "true")
            addDataSourceProperty("maintainTimeStats", "false")
        }

        return HikariDataSource(hikariConfig)
    }

    private fun createSQLiteDataSource(config: org.bukkit.configuration.file.FileConfiguration): HikariDataSource {
        val dataFolder = plugin.dataFolder
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        val dbFile = File(dataFolder, config.getString("database.sqlite.file", "data/mcw.db"))
        
        // 确保数据库文件的父目录存在
        val dbParentDir = dbFile.parentFile
        if (dbParentDir != null && !dbParentDir.exists()) {
            dbParentDir.mkdirs()
        }
        
        val url = "jdbc:sqlite:${dbFile.absolutePath}"

        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.sqlite.JDBC"
            jdbcUrl = url
            maximumPoolSize = 1 // SQLite不支持多连接
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
        }

        return HikariDataSource(hikariConfig)
    }

    private fun createTables() {
        val statements = when (databaseType) {
            DatabaseType.MYSQL -> getMySQLTableStatements()
            DatabaseType.SQLITE -> getSQLiteTableStatements()
        }

        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    for (sql in statements) {
                        try {
                            statement.execute(sql)
                            plugin.logger.fine("成功创建表: ${sql.substring(0, sql.indexOf('(').coerceAtLeast(0))}...")
                        } catch (e: SQLException) {
                            e.message?.contains("already exists")?.let {
                                if (!it == true) {
                                    plugin.logger.warning("创建表失败: $sql, 错误: ${e.message}")
                                }
                            }
                        }
                    }
                }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun getMySQLTableStatements(): List<String> {
        return listOf(
            """
            CREATE TABLE IF NOT EXISTS spawners (
                id INT AUTO_INCREMENT PRIMARY KEY,
                owner VARCHAR(36) NOT NULL,
                type VARCHAR(20) NOT NULL,
                entity_type VARCHAR(50) NOT NULL,
                world VARCHAR(100) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                stored_spawns INT DEFAULT 0,
                stored_spawns_released INT DEFAULT 0,  -- 新增字段
                last_spawn_time BIGINT DEFAULT 0,
                last_release_time BIGINT DEFAULT 0,
                active TINYINT(1) DEFAULT 1,
                spawn_mode VARCHAR(20) DEFAULT 'RANDOM',
                precise_x DOUBLE DEFAULT 0,
                precise_y DOUBLE DEFAULT 1,
                precise_z DOUBLE DEFAULT 0,
                spawn_delay INT DEFAULT 100,
                spawn_count INT DEFAULT 1,
                max_nearby_entities INT DEFAULT 6,
                activation_range INT DEFAULT 16,
                storage_enabled TINYINT(1) DEFAULT 0,
                max_storage INT DEFAULT 100,
                created_at BIGINT DEFAULT 0,
                updated_at BIGINT DEFAULT 0,
                INDEX idx_owner (owner),
                INDEX idx_location (world, x, y, z),
                INDEX idx_active (active)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS players (
                uuid VARCHAR(36) PRIMARY KEY,
                normal_purchased_limit INT DEFAULT 0,
                premium_purchased_limit INT DEFAULT 0,
                created_at BIGINT DEFAULT 0,
                updated_at BIGINT DEFAULT 0
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS spawner_upgrades (
                spawner_id INT,
                upgrade_type VARCHAR(50) NOT NULL,
                level INT DEFAULT 0,
                PRIMARY KEY (spawner_id, upgrade_type),
                FOREIGN KEY (spawner_id) REFERENCES spawners(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS player_unlocked_entities (
                uuid VARCHAR(36),
                spawner_type VARCHAR(20) NOT NULL,
                entity_type VARCHAR(50) NOT NULL,
                PRIMARY KEY (uuid, spawner_type, entity_type),
                FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """.trimIndent()
        )
    }

    private fun getSQLiteTableStatements(): List<String> {
        return listOf(
            """
            CREATE TABLE IF NOT EXISTS spawners (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                owner TEXT NOT NULL,
                type TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                world TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                stored_spawns INTEGER DEFAULT 0,
                stored_spawns_released INTEGER DEFAULT 0,  -- 新增字段
                last_spawn_time INTEGER DEFAULT 0,
                last_release_time INTEGER DEFAULT 0,
                active INTEGER DEFAULT 1,
                spawn_mode TEXT DEFAULT 'RANDOM',
                precise_x REAL DEFAULT 0,
                precise_y REAL DEFAULT 1,
                precise_z REAL DEFAULT 0,
                spawn_delay INTEGER DEFAULT 100,
                spawn_count INTEGER DEFAULT 1,
                max_nearby_entities INTEGER DEFAULT 6,
                activation_range INTEGER DEFAULT 16,
                storage_enabled INTEGER DEFAULT 0,
                max_storage INTEGER DEFAULT 100,
                created_at INTEGER DEFAULT 0,
                updated_at INTEGER DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                normal_purchased_limit INTEGER DEFAULT 0,
                premium_purchased_limit INTEGER DEFAULT 0,
                created_at INTEGER DEFAULT 0,
                updated_at INTEGER DEFAULT 0
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS spawner_upgrades (
                spawner_id INTEGER,
                upgrade_type TEXT NOT NULL,
                level INTEGER DEFAULT 0,
                PRIMARY KEY (spawner_id, upgrade_type),
                FOREIGN KEY (spawner_id) REFERENCES spawners(id) ON DELETE CASCADE
            )
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS player_unlocked_entities (
                uuid TEXT,
                spawner_type TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                PRIMARY KEY (uuid, spawner_type, entity_type),
                FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    fun saveSpawner(spawner: Spawner): CompletableFuture<Void> {
        return AsyncTaskManager.runAsync("SaveSpawner", timeout = 30000) {
            try {
                dataSource.connection.use { connection ->
                    val currentTime = System.currentTimeMillis()
                    
                    if (spawner.id == 0) {
                        // INSERT语句
                        val insertSql = """
                            INSERT INTO spawners (owner, type, entity_type, world, x, y, z, stored_spawns, 
                            stored_spawns_released, last_spawn_time, last_release_time, active, spawn_mode, 
                            precise_x, precise_y, precise_z, spawn_delay, spawn_count, max_nearby_entities, 
                            activation_range, storage_enabled, max_storage, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent()

                        connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS).use { ps ->
                            ps.setString(1, spawner.owner.toString())
                            ps.setString(2, spawner.type.name)
                            ps.setString(3, spawner.entityType.name)
                            ps.setString(4, spawner.location.world?.name ?: "world")
                            ps.setDouble(5, spawner.location.x)
                            ps.setDouble(6, spawner.location.y)
                            ps.setDouble(7, spawner.location.z)
                            ps.setInt(8, spawner.storedSpawns)
                            ps.setInt(9, spawner.storedSpawnsReleased)
                            ps.setLong(10, spawner.lastSpawnTime)
                            ps.setLong(11, spawner.lastReleaseTime)
                            ps.setInt(12, if (spawner.active) 1 else 0)
                            ps.setString(13, spawner.spawnMode.name)
                            ps.setDouble(14, spawner.preciseX)
                            ps.setDouble(15, spawner.preciseY)
                            ps.setDouble(16, spawner.preciseZ)
                            ps.setInt(17, spawner.spawnDelay)
                            ps.setInt(18, spawner.spawnCount)
                            ps.setInt(19, spawner.maxNearbyEntities)
                            ps.setInt(20, spawner.activationRange)
                            ps.setInt(21, if (spawner.storageEnabled) 1 else 0)
                            ps.setInt(22, spawner.maxStorage)
                            ps.setLong(23, currentTime)
                            ps.setLong(24, currentTime)

                            ps.executeUpdate()

                            ps.generatedKeys.use { rs ->
                                if (rs.next()) {
                                    spawner.id = rs.getInt(1)
                                }
                            }
                        }
                    } else {
                        // UPDATE语句
                        val updateSql = """
                            UPDATE spawners SET owner=?, type=?, entity_type=?, world=?, x=?, y=?, z=?, 
                            stored_spawns=?, stored_spawns_released=?, last_spawn_time=?, last_release_time=?, 
                            active=?, spawn_mode=?, precise_x=?, precise_y=?, precise_z=?, spawn_delay=?, 
                            spawn_count=?, max_nearby_entities=?, activation_range=?, storage_enabled=?, 
                            max_storage=?, updated_at=? WHERE id=?
                        """.trimIndent()

                        connection.prepareStatement(updateSql).use { ps ->
                            ps.setString(1, spawner.owner.toString())
                            ps.setString(2, spawner.type.name)
                            ps.setString(3, spawner.entityType.name)
                            ps.setString(4, spawner.location.world?.name ?: "world")
                            ps.setDouble(5, spawner.location.x)
                            ps.setDouble(6, spawner.location.y)
                            ps.setDouble(7, spawner.location.z)
                            ps.setInt(8, spawner.storedSpawns)
                            ps.setInt(9, spawner.storedSpawnsReleased)
                            ps.setLong(10, spawner.lastSpawnTime)
                            ps.setLong(11, spawner.lastReleaseTime)
                            ps.setInt(12, if (spawner.active) 1 else 0)
                            ps.setString(13, spawner.spawnMode.name)
                            ps.setDouble(14, spawner.preciseX)
                            ps.setDouble(15, spawner.preciseY)
                            ps.setDouble(16, spawner.preciseZ)
                            ps.setInt(17, spawner.spawnDelay)
                            ps.setInt(18, spawner.spawnCount)
                            ps.setInt(19, spawner.maxNearbyEntities)
                            ps.setInt(20, spawner.activationRange)
                            ps.setInt(21, if (spawner.storageEnabled) 1 else 0)
                            ps.setInt(22, spawner.maxStorage)
                            ps.setLong(23, currentTime)
                            ps.setInt(24, spawner.id)

                            ps.executeUpdate()
                        }
                    }

                    // 保存升级数据
                    saveUpgrades(connection, spawner)
                }
            } catch (e: SQLException) {
                plugin.logger.severe("保存刷怪笼失败: ${e.message}")
                throw RuntimeException("保存刷怪笼失败", e)
            }
            Unit // 返回Unit而不是null
        }.thenApply { null } // 转换为CompletableFuture<Void>
    }

    private fun saveUpgrades(connection: Connection, spawner: Spawner) {
        connection.prepareStatement("DELETE FROM spawner_upgrades WHERE spawner_id = ?").use { deletePs ->
            deletePs.setInt(1, spawner.id)
            deletePs.executeUpdate()
        }

        if (spawner.upgradeLevels.isNotEmpty()) {
            connection.prepareStatement(
                "INSERT INTO spawner_upgrades (spawner_id, upgrade_type, level) VALUES (?, ?, ?)"
            ).use { insertPs ->
                for ((upgradeType, level) in spawner.upgradeLevels) {
                    insertPs.setInt(1, spawner.id)
                    insertPs.setString(2, upgradeType)
                    insertPs.setInt(3, level)
                    insertPs.addBatch()
                }
                insertPs.executeBatch()
            }
        }
    }

    fun loadSpawners(): CompletableFuture<List<Spawner>> {
        return AsyncTaskManager.runAsync("LoadSpawners", timeout = 60000) {
            val spawnerList = mutableListOf<Spawner>()
            val failedSpawners = mutableListOf<Int>() // 记录加载失败的刷怪笼ID
            try {
                dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT * FROM spawners").use { rs ->
                            while (rs.next()) {
                                try {
                                    val worldName = rs.getString("world")
                                    val world = plugin.server.getWorld(worldName)
                                    
                                    // 检查世界是否已加载
                                    if (world == null) {
                                        val spawnerId = rs.getInt("id")
                                        plugin.logger.warning("刷怪笼 #$spawnerId 所在世界 '$worldName' 未加载，将跳过此刷怪笼")
                                        failedSpawners.add(spawnerId)
                                        continue
                                    }
                                    
                                    val spawner = Spawner(
                                        id = rs.getInt("id"),
                                        owner = UUID.fromString(rs.getString("owner")),
                                        type = SpawnerType.valueOf(rs.getString("type")),
                                        entityType = EntityType.valueOf(rs.getString("entity_type")),
                                        location = Location(
                                            world,
                                            rs.getDouble("x"),
                                            rs.getDouble("y"),
                                            rs.getDouble("z")
                                        ),
                                        spawnDelay = rs.getInt("spawn_delay"),
                                        spawnCount = rs.getInt("spawn_count"),
                                        maxNearbyEntities = rs.getInt("max_nearby_entities"),
                                        activationRange = rs.getInt("activation_range"),
                                        storageEnabled = rs.getInt("storage_enabled") == 1,
                                        maxStorage = rs.getInt("max_storage"),
                                        storedSpawns = rs.getInt("stored_spawns"),
                                        storedSpawnsReleased = rs.getInt("stored_spawns_released"), // 新增字段
                                        lastSpawnTime = rs.getLong("last_spawn_time"),
                                        lastReleaseTime = rs.getLong("last_release_time"),
                                        active = rs.getInt("active") == 1,
                                        spawnMode = SpawnMode.valueOf(rs.getString("spawn_mode")),
                                        preciseX = rs.getDouble("precise_x"),
                                        preciseY = rs.getDouble("precise_y"),
                                        preciseZ = rs.getDouble("precise_z")
                                    )

                                    // 加载升级数据
                                    loadUpgrades(connection, spawner)
                                    
                                    // 应用升级效果（确保升级后的值正确计算）
                                    (plugin as? MonsterCapitalWorks)?.getUpgradeManager()?.applyUpgrades(spawner)
                                    spawnerList.add(spawner)
                                    spawners[spawner.normalizeLocation()] = spawner
                                    playerSpawners.computeIfAbsent(spawner.owner) { mutableSetOf() }.add(spawner.normalizeLocation())
                                } catch (e: Exception) {
                                    plugin.logger.warning("加载单个刷怪笼失败: ${e.message}")
                                    // 继续加载其他刷怪笼
                                }
                            }
                        }
                    }
                }
                plugin.logger.info("成功加载 ${spawnerList.size} 个刷怪笼")
                if (failedSpawners.isNotEmpty()) {
                    plugin.logger.warning("有 ${failedSpawners.size} 个刷怪笼因为世界未加载而被跳过，ID: $failedSpawners")
                    // 通知SpawnerManager有未加载的刷怪笼
                    (plugin as? MonsterCapitalWorks)?.getSpawnerManager()?.setUnloadedSpawnerIds(failedSpawners.toSet())
                }
            } catch (e: SQLException) {
                plugin.logger.severe("加载刷怪笼失败: ${e.message}")
                throw RuntimeException("加载刷怪笼失败", e)
            }
            spawnerList
        }
    }
    
    /**
     * 根据ID加载单个刷怪笼
     */
    fun loadSpawnerById(spawnerId: Int): Spawner? {
        return try {
            dataSource.connection.use { connection ->
                connection.prepareStatement("SELECT * FROM spawners WHERE id = ?").use { ps ->
                    ps.setInt(1, spawnerId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val worldName = rs.getString("world")
                            val world = plugin.server.getWorld(worldName)
                            
                            if (world == null) {
                                plugin.logger.warning("刷怪笼 #$spawnerId 所在世界 '$worldName' 仍未加载")
                                return@use null
                            }
                            
                            val spawner = Spawner(
                                id = rs.getInt("id"),
                                owner = UUID.fromString(rs.getString("owner")),
                                type = SpawnerType.valueOf(rs.getString("type")),
                                entityType = EntityType.valueOf(rs.getString("entity_type")),
                                location = Location(
                                    world,
                                    rs.getDouble("x"),
                                    rs.getDouble("y"),
                                    rs.getDouble("z")
                                ),
                                spawnDelay = rs.getInt("spawn_delay"),
                                spawnCount = rs.getInt("spawn_count"),
                                maxNearbyEntities = rs.getInt("max_nearby_entities"),
                                activationRange = rs.getInt("activation_range"),
                                storageEnabled = rs.getInt("storage_enabled") == 1,
                                maxStorage = rs.getInt("max_storage"),
                                        storedSpawns = rs.getInt("stored_spawns"),
                                        storedSpawnsReleased = rs.getInt("stored_spawns_released"),
                                        lastSpawnTime = rs.getLong("last_spawn_time"),
                                        lastReleaseTime = rs.getLong("last_release_time"),
                                active = rs.getInt("active") == 1,
                                spawnMode = SpawnMode.valueOf(rs.getString("spawn_mode")),
                                preciseX = rs.getDouble("precise_x"),
                                preciseY = rs.getDouble("precise_y"),
                                preciseZ = rs.getDouble("precise_z")
                            )
                            
                            // 加载升级数据
                            loadUpgrades(connection, spawner)
                            
                            // 应用升级效果（确保升级后的值正确计算）
                            (plugin as? MonsterCapitalWorks)?.getUpgradeManager()?.applyUpgrades(spawner)
                            spawner
                        } else {
                            null
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("加载刷怪笼 #$spawnerId 失败: ${e.message}")
            null
        }
    }

    private fun loadUpgrades(connection: Connection, spawner: Spawner) {
        connection.prepareStatement("SELECT * FROM spawner_upgrades WHERE spawner_id = ?").use { ps ->
            ps.setInt(1, spawner.id)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    spawner.upgradeLevels[rs.getString("upgrade_type")] = rs.getInt("level")
                }
            }
        }
    }

    fun deleteSpawner(spawner: Spawner): CompletableFuture<Void> {
        return AsyncTaskManager.runAsync("DeleteSpawner", timeout = 30000) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("DELETE FROM spawners WHERE id = ?").use { ps ->
                        ps.setInt(1, spawner.id)
                        ps.executeUpdate()
                    }
                }
                spawners.remove(spawner.normalizeLocation())
                playerSpawners[spawner.owner]?.remove(spawner.normalizeLocation())
            } catch (e: SQLException) {
                plugin.logger.severe("删除刷怪笼失败: ${e.message}")
                throw RuntimeException("删除刷怪笼失败", e)
            }
            Unit // 返回Unit而不是null
        }.thenApply { null } // 转换为CompletableFuture<Void>
    }

    fun getSpawnerAt(location: Location): Spawner? {
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

    fun getPlayerSpawners(uuid: UUID): Set<Location> {
        return playerSpawners[uuid]?.toSet() ?: emptySet()
    }

    fun savePlayerData(data: PlayerData): CompletableFuture<Void> {
        return AsyncTaskManager.runAsync("SavePlayerData", timeout = 30000) {
            try {
                dataSource.connection.use { connection ->
                    val currentTime = System.currentTimeMillis()
                    connection.prepareStatement(
                        """
                        INSERT ${if (databaseType == DatabaseType.MYSQL) "" else "OR REPLACE"} INTO players 
                        (uuid, normal_purchased_limit, premium_purchased_limit, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?)
                        ${if (databaseType == DatabaseType.MYSQL) "ON DUPLICATE KEY UPDATE normal_purchased_limit=VALUES(normal_purchased_limit), premium_purchased_limit=VALUES(premium_purchased_limit), updated_at=VALUES(updated_at)" else ""}
                        """.trimIndent()
                    ).use { ps ->
                        ps.setString(1, data.uuid.toString())
                        ps.setInt(2, data.normalPurchasedLimit)
                        ps.setInt(3, data.premiumPurchasedLimit)
                        ps.setLong(4, currentTime)
                        ps.setLong(5, currentTime)
                        ps.executeUpdate()
                    }

                    // 保存解锁的实体
                    saveUnlockedEntities(connection, data)
                }
            } catch (e: SQLException) {
                plugin.logger.severe("保存玩家数据失败: ${e.message}")
                throw RuntimeException("保存玩家数据失败", e)
            }
            Unit // 返回Unit而不是null
        }.thenApply { null } // 转换为CompletableFuture<Void>
    }

    private fun saveUnlockedEntities(connection: Connection, data: PlayerData) {
        connection.prepareStatement("DELETE FROM player_unlocked_entities WHERE uuid = ?").use { deletePs ->
            deletePs.setString(1, data.uuid.toString())
            deletePs.executeUpdate()
        }

        if (data.unlockedEntities.isNotEmpty()) {
            connection.prepareStatement(
                "INSERT INTO player_unlocked_entities (uuid, spawner_type, entity_type) VALUES (?, ?, ?)"
            ).use { insertPs ->
                for ((spawnerType, entities) in data.unlockedEntities) {
                    for (entity in entities) {
                        insertPs.setString(1, data.uuid.toString())
                        insertPs.setString(2, spawnerType.name)
                        insertPs.setString(3, entity.name)
                        insertPs.addBatch()
                    }
                }
                insertPs.executeBatch()
            }
        }
    }

    fun getPlayerData(uuid: UUID): PlayerData {
        return playerData.getOrPut(uuid) { loadPlayerData(uuid) }
    }

    fun loadPlayerData(uuid: UUID): PlayerData {
        val data = PlayerData(uuid)
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement("SELECT * FROM players WHERE uuid = ?").use { ps ->
                    ps.setString(1, uuid.toString())
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            data.normalPurchasedLimit = rs.getInt("normal_purchased_limit")
                            data.premiumPurchasedLimit = rs.getInt("premium_purchased_limit")
                        }
                    }
                }

                // 加载解锁的实体
                loadUnlockedEntities(connection, data)
            }
        } catch (e: SQLException) {
            plugin.logger.severe("加载玩家数据失败: ${e.message}")
        }
        return data
    }

    private fun loadUnlockedEntities(connection: Connection, data: PlayerData) {
        connection.prepareStatement("SELECT * FROM player_unlocked_entities WHERE uuid = ?").use { ps ->
            ps.setString(1, data.uuid.toString())
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val spawnerType = SpawnerType.valueOf(rs.getString("spawner_type"))
                    val entityType = EntityType.valueOf(rs.getString("entity_type"))
                    data.unlockedEntities.computeIfAbsent(spawnerType) { mutableSetOf() }.add(entityType)
                }
            }
        }
    }

    fun close() {
        try {
            if (::dataSource.isInitialized) {
                dataSource.close()
            }
            plugin.logger.info("数据库连接已关闭")
        } catch (e: Exception) {
            plugin.logger.severe("关闭数据库连接失败: ${e.message}")
        }
    }

}
