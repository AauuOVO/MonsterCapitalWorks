package org.Aauu.monsterCapitalWorks.di

import org.Aauu.monsterCapitalWorks.MonsterCapitalWorks
import org.Aauu.monsterCapitalWorks.manager.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 依赖注入容器 - 管理所有插件实例的生命周期
 * 使用单例模式确保全局唯一性
 */
object DependencyInjector {
    
    private val services = ConcurrentHashMap<Class<*>, Any>()
    private val pluginInstances = ConcurrentHashMap<String, Any>()
    private lateinit var plugin: MonsterCapitalWorks
    
    /**
     * 初始化依赖注入容器
     */
    fun initialize(plugin: MonsterCapitalWorks) {
        this.plugin = plugin
        registerCoreServices()
    }
    
    /**
     * 注册核心服务
     */
    private fun registerCoreServices() {
        // 注册管理器实例
        registerService(ConfigManager::class.java, ConfigManager(plugin))
        registerService(DataManager::class.java, DataManager(plugin))
        registerService(EconomyManager::class.java, EconomyManager(plugin))
        registerService(PermissionManager::class.java, PermissionManager(plugin))
        registerService(SpawnerManager::class.java, SpawnerManager(plugin))
        registerService(UpgradeManager::class.java, UpgradeManager(plugin, getService(ConfigManager::class.java)))
        registerService(GUIManager::class.java, GUIManager(plugin))
    }
    
    /**
     * 注册服务
     */
    fun <T> registerService(serviceClass: Class<T>, instance: T) {
        @Suppress("UNCHECKED_CAST")
        services[serviceClass] = instance as Any
    }
    
    /**
     * 获取服务实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getService(serviceClass: Class<T>): T {
        return services[serviceClass] as? T 
            ?: throw IllegalStateException("服务 ${serviceClass.simpleName} 未注册")
    }
    
    /**
     * 注册插件实例
     */
    fun registerPlugin(name: String, instance: Any) {
        pluginInstances[name] = instance
    }
    
    /**
     * 获取插件实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getPlugin(name: String): T {
        return pluginInstances[name] as? T 
            ?: throw IllegalStateException("插件 $name 未注册")
    }
    
    /**
     * 初始化所有服务
     */
    fun initializeServices() {
        // 按照依赖顺序初始化服务
        // 1. 首先初始化ConfigManager
        (services[ConfigManager::class.java] as? ConfigManager)?.initialize()
        
        // 2. 然后初始化DataManager（依赖ConfigManager）
        (services[DataManager::class.java] as? DataManager)?.initialize()
        
        // 3. 初始化EconomyManager
        (services[EconomyManager::class.java] as? EconomyManager)?.let { service ->
            if (plugin.config.getBoolean("economy.enabled", true)) {
                service.initialize()
            }
        }
        
        // 4. 初始化SpawnerManager（依赖DataManager）
        (services[SpawnerManager::class.java] as? SpawnerManager)?.initialize()
        
        // 5. 初始化UpgradeManager
        (services[UpgradeManager::class.java] as? UpgradeManager)?.initialize()
        
        // 6. 最后初始化GUIManager
        (services[GUIManager::class.java] as? GUIManager)?.initialize()
    }
    
    /**
     * 关闭所有服务
     */
    fun shutdownServices() {
        services.values.forEach { service ->
            when (service) {
                is SpawnerManager -> service.shutdown()
                is DataManager -> service.close()
            }
        }
        services.clear()
        pluginInstances.clear()
    }
    
    /**
     * 检查服务是否已注册
     */
    fun isServiceRegistered(serviceClass: Class<*>): Boolean {
        return services.containsKey(serviceClass)
    }
    
    /**
     * 获取所有已注册的服务
     */
    fun getRegisteredServices(): Set<Class<*>> {
        return services.keys.toSet()
    }
}
