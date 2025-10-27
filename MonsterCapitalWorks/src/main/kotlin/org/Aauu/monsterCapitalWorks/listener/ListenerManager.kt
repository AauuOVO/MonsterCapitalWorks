package org.Aauu.monsterCapitalWorks.listener

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * 事件监听器管理器 - 统一管理所有事件监听器
 */
object ListenerManager {
    
    private val listeners = ConcurrentHashMap<String, Any>()
    private lateinit var plugin: JavaPlugin
    
    /**
     * 初始化监听器管理器
     */
    fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }
    
    /**
     * 注册事件监听器
     */
    fun registerListener(listener: Any) {
        val listenerName = listener.javaClass.simpleName
        
        try {
            Bukkit.getPluginManager().registerEvents(listener as org.bukkit.event.Listener, plugin)
            listeners[listenerName] = listener
            plugin.logger.info("已注册事件监听器: $listenerName")
        } catch (e: Exception) {
            plugin.logger.severe("注册事件监听器失败: $listenerName, 错误: ${e.message}")
        }
    }
    
    /**
     * 注销事件监听器
     */
    fun unregisterListener(name: String) {
        listeners.remove(name)?.let {
            plugin.logger.info("已注销事件监听器: $name")
        }
    }
    
    /**
     * 获取已注册的监听器
     */
    fun getListener(name: String): Any? {
        return listeners[name]
    }
    
    /**
     * 获取所有已注册的监听器
     */
    fun getAllListeners(): Map<String, Any> {
        return listeners.toMap()
    }
    
    /**
     * 注销所有监听器
     */
    fun unregisterAll() {
        listeners.clear()
        plugin.logger.info("已注销所有事件监听器")
    }
}
