package org.Aauu.monsterCapitalWorks.cache

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 缓存管理器 - 提供高性能的缓存功能
 * 支持过期时间、自动清理等高级功能
 */
object CacheManager {
    
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private val lock = ReentrantReadWriteLock()
    
    data class CacheEntry(
        val value: Any,
        val createdAt: Long = System.currentTimeMillis(),
        val expireAfter: Long? = null,
        val accessCount: Long = 0
    ) {
        fun isExpired(): Boolean {
            return expireAfter?.let { System.currentTimeMillis() - createdAt > it } ?: false
        }
    }
    
    init {
        // 启动定期清理任务，每5分钟清理一次过期缓存
        executor.scheduleAtFixedRate({
            cleanExpiredEntries()
        }, 5, 5, TimeUnit.MINUTES)
    }
    
    /**
     * 存储缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param expireAfter 过期时间（毫秒），null表示永不过期
     */
    fun put(key: String, value: Any, expireAfter: Long? = null) {
        lock.write {
            cache[key] = CacheEntry(value, System.currentTimeMillis(), expireAfter)
        }
    }
    
    /**
     * 获取缓存
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期则返回null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        return lock.read {
            val entry = cache[key]
            if (entry == null || entry.isExpired()) {
                null
            } else {
                // 更新访问计数
                @Suppress("UNCHECKED_CAST")
                val result = entry.value as T
                lock.write {
                    cache[key] = entry.copy(accessCount = entry.accessCount + 1)
                }
                result
            }
        }
    }
    
    /**
     * 获取或计算缓存
     * 如果缓存不存在，则通过提供的函数计算值并缓存
     */
    inline fun <T> getOrPut(key: String, expireAfter: Long? = null, defaultValue: () -> T): T {
        return get(key) ?: run {
            val value = defaultValue()
            @Suppress("UNCHECKED_CAST")
            put(key, value as Any, expireAfter)
            value
        }
    }
    
    /**
     * 检查缓存是否存在且未过期
     */
    fun contains(key: String): Boolean {
        return lock.read {
            val entry = cache[key]
            entry != null && !entry.isExpired()
        }
    }
    
    /**
     * 删除缓存
     */
    fun remove(key: String): Boolean {
        return lock.write {
            cache.remove(key) != null
        }
    }
    
    /**
     * 清空所有缓存
     */
    fun clear() {
        lock.write {
            cache.clear()
        }
    }
    
    /**
     * 清理过期的缓存条目
     */
    fun cleanExpiredEntries() {
        lock.write {
            val expiredKeys = cache.filter { it.value.isExpired() }.keys
            expiredKeys.forEach { cache.remove(it) }
            
            if (expiredKeys.isNotEmpty()) {
                println("[CacheManager] 清理了 ${expiredKeys.size} 个过期缓存条目")
            }
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getStats(): Map<String, Any> {
        return lock.read {
            val totalEntries = cache.size
            val expiredEntries = cache.count { it.value.isExpired() }
            val totalAccessCount = cache.values.sumOf { it.accessCount }
            
            mapOf(
                "total_entries" to totalEntries,
                "expired_entries" to expiredEntries,
                "valid_entries" to (totalEntries - expiredEntries),
                "total_access_count" to totalAccessCount
            )
        }
    }
    
    /**
     * 获取所有缓存键
     */
    fun getKeys(): Set<String> {
        return lock.read {
            cache.keys.toSet()
        }
    }
    
    /**
     * 关闭缓存管理器
     */
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
        clear()
    }
    
    /**
     * 创建带命名空间的缓存键
     */
    fun createKey(namespace: String, vararg parts: String): String {
        return "$namespace:${parts.joinToString(":")}"
    }
}
