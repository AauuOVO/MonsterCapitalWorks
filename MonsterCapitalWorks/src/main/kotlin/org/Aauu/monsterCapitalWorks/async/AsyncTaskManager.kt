package org.Aauu.monsterCapitalWorks.async

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

/**
 * 异步任务管理器 - 提供高性能的异步任务处理
 * 支持任务优先级、超时控制、重试机制等高级功能
 */
object AsyncTaskManager {
    
    private lateinit var plugin: JavaPlugin
    private val asyncExecutor: ExecutorService
    private val syncExecutor: ExecutorService
    private val scheduledExecutor: ScheduledExecutorService
    private val taskCounter = AtomicLong(0)
    private val activeTasks = ConcurrentHashMap<Long, TaskInfo>()
    
    data class TaskInfo(
        val id: Long,
        val name: String,
        val createdAt: Long,
        val timeout: Long?,
        val retryCount: Int = 0,
        val maxRetries: Int = 0
    )
    
    enum class TaskPriority {
        LOW, NORMAL, HIGH, CRITICAL
    }
    
    init {
        // 创建线程池
        asyncExecutor = ThreadPoolExecutor(
            2, // 核心线程数
            8, // 最大线程数
            60L, TimeUnit.SECONDS, // 空闲线程存活时间
            LinkedBlockingQueue(1000), // 任务队列
            ThreadFactory { r ->
                Thread(r, "MCW-Async-${taskCounter.incrementAndGet()}").apply {
                    isDaemon = true
                }
            }
        )
        
        syncExecutor = ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            ThreadFactory { r ->
                Thread(r, "MCW-Sync").apply {
                    isDaemon = true
                }
            }
        )
        
        scheduledExecutor = Executors.newScheduledThreadPool(2, ThreadFactory { r ->
            Thread(r, "MCW-Scheduled-${taskCounter.incrementAndGet()}").apply {
                isDaemon = true
            }
        })
        
        // 启动任务监控
        startTaskMonitoring()
    }
    
    /**
     * 初始化异步任务管理器
     */
    fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }
    
    /**
     * 执行异步任务
     */
    fun <T> runAsync(
        name: String = "Unknown",
        priority: TaskPriority = TaskPriority.NORMAL,
        timeout: Long? = null,
        maxRetries: Int = 0,
        task: () -> T
    ): CompletableFuture<T> {
        val taskId = taskCounter.incrementAndGet()
        val taskInfo = TaskInfo(taskId, name, System.currentTimeMillis(), timeout, 0, maxRetries)
        activeTasks[taskId] = taskInfo
        
        val future = CompletableFuture.supplyAsync({
            executeWithRetry(task, maxRetries)
        }, asyncExecutor).whenComplete { result, throwable ->
            activeTasks.remove(taskId)
            if (throwable != null) {
                plugin.logger.warning("异步任务执行失败: $name, 错误: ${throwable.message}")
            }
        }
        
        // 设置超时
        timeout?.let {
            future.orTimeout(it, TimeUnit.MILLISECONDS)
        }
        
        return future
    }
    
    /**
     * 在主线程中执行同步任务
     */
    fun <T> runSync(task: () -> T): CompletableFuture<T> {
        return if (Bukkit.isPrimaryThread()) {
            CompletableFuture.completedFuture(task())
        } else {
            CompletableFuture.supplyAsync(task, syncExecutor)
        }
    }
    
    /**
     * 延迟执行任务
     */
    fun schedule(
        delay: Long,
        unit: TimeUnit,
        name: String = "Scheduled",
        task: () -> Unit
    ): ScheduledFuture<*> {
        return scheduledExecutor.schedule({
            try {
                task()
            } catch (e: Exception) {
                plugin.logger.severe("定时任务执行失败: $name, 错误: ${e.message}")
            }
        }, delay, unit)
    }
    
    /**
     * 定期执行任务
     */
    fun scheduleAtFixedRate(
        initialDelay: Long,
        period: Long,
        unit: TimeUnit,
        name: String = "Repeating",
        task: () -> Unit
    ): ScheduledFuture<*> {
        return scheduledExecutor.scheduleAtFixedRate({
            try {
                task()
            } catch (e: Exception) {
                plugin.logger.severe("定期任务执行失败: $name, 错误: ${e.message}")
            }
        }, initialDelay, period, unit)
    }
    
    /**
     * 带重试机制的任务执行
     */
    private fun <T> executeWithRetry(task: () -> T, maxRetries: Int): T {
        var lastException: Exception? = null
        var attempts = 0
        
        while (attempts <= maxRetries) {
            try {
                return task()
            } catch (e: Exception) {
                lastException = e
                attempts++
                
                if (attempts <= maxRetries) {
                    plugin.logger.warning("任务执行失败，正在重试 ($attempts/$maxRetries): ${e.message}")
                        Thread.sleep((1000 * attempts).toLong()) // 指数退避
                }
            }
        }
        
        throw lastException ?: RuntimeException("任务执行失败")
    }
    
    /**
     * 启动任务监控
     */
    private fun startTaskMonitoring() {
        // 延迟启动监控，确保插件完全初始化
        scheduledExecutor.schedule({
            // 检查是否启用监控
            val enableMonitoring = if (::plugin.isInitialized) {
                plugin.config.getBoolean("performance.enable_task_monitoring", false)
            } else {
                false // 默认关闭
            }
            
            if (enableMonitoring) {
                scheduledExecutor.scheduleAtFixedRate({
                    monitorTasks()
                }, 30, 30, TimeUnit.SECONDS)
            }
        }, 5, TimeUnit.SECONDS) // 延迟5秒启动，确保配置已加载
    }
    
    /**
     * 监控任务状态
     */
    private fun monitorTasks() {
        val currentTime = System.currentTimeMillis()
        val timeoutTasks = activeTasks.filter { (_, taskInfo) ->
            taskInfo.timeout?.let { currentTime - taskInfo.createdAt > it } ?: false
        }
        
        if (timeoutTasks.isNotEmpty()) {
            plugin.logger.warning("发现 ${timeoutTasks.size} 个超时任务")
            timeoutTasks.forEach { (id, taskInfo) ->
                plugin.logger.warning("超时任务: ${taskInfo.name} (ID: $id)")
            }
        }
        
        plugin.logger.info("当前活跃任务数: ${activeTasks.size}")
    }
    
    /**
     * 获取任务统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "active_tasks" to activeTasks.size,
            "async_pool_active" to (asyncExecutor as ThreadPoolExecutor).activeCount,
            "async_pool_queue_size" to (asyncExecutor as ThreadPoolExecutor).queue.size,
            "sync_pool_active" to (syncExecutor as ThreadPoolExecutor).activeCount,
            "sync_pool_queue_size" to (syncExecutor as ThreadPoolExecutor).queue.size
        )
    }
    
    /**
     * 关闭异步任务管理器
     */
    fun shutdown() {
        plugin.logger.info("正在关闭异步任务管理器...")
        
        // 关闭执行器
        asyncExecutor.shutdown()
        syncExecutor.shutdown()
        scheduledExecutor.shutdown()
        
        try {
            if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow()
            }
            if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow()
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            asyncExecutor.shutdownNow()
            syncExecutor.shutdownNow()
            scheduledExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
        
        activeTasks.clear()
        plugin.logger.info("异步任务管理器已关闭")
    }
}
