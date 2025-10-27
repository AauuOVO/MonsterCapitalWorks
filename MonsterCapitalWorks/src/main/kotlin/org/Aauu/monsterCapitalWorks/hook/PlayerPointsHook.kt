package org.Aauu.monsterCapitalWorks.hook

import org.Aauu.monsterCapitalWorks.MonsterCapitalWorks
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

/**
 * PlayerPoints插件钩子
 * 提供PlayerPoints积分系统的支持
 */
class PlayerPointsHook(private val plugin: MonsterCapitalWorks) {
    
    private var playerPointsPlugin: Plugin? = null
    private var playerPointsAPI: Any? = null
    
    init {
        setupPlayerPoints()
    }
    
    /**
     * 初始化PlayerPoints插件连接
     */
    private fun setupPlayerPoints() {
        try {
            playerPointsPlugin = Bukkit.getPluginManager().getPlugin("PlayerPoints")
            
            if (playerPointsPlugin?.isEnabled == true) {
                // 获取PlayerPoints的API类
                val clazz = Class.forName("org.black_ixx.playerpoints.PlayerPoints")
                val method = clazz.getMethod("getAPI")
                playerPointsAPI = method.invoke(playerPointsPlugin)
                
                plugin.logger.info("成功连接到PlayerPoints插件")
            } else {
                // 检查配置中是否启用了PlayerPoints
                if (plugin.config.getBoolean("playerpoints.enabled", false)) {
                    plugin.logger.warning("§c[警告] 配置中启用了 PlayerPoints 功能，但未找到PlayerPoints插件")
                    plugin.logger.warning("§c请在config.yml中的playerpoints.enabled设置为false，或安装PlayerPoints插件")
                    plugin.logger.warning("§c如果需要使用PlayerPoints功能，请：")
                    plugin.logger.warning("§c1. 下载并安装PlayerPoints插件")
                    plugin.logger.warning("§c2. 重启服务器")
                    plugin.logger.warning("§c3. 在config.yml中设置 playerpoints.enabled: true")
                } else {
                    plugin.logger.info("未找到PlayerPoints插件，功能将不可用")
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("连接PlayerPoints插件时出错: ${e.message}")
            playerPointsPlugin = null
            playerPointsAPI = null
        }
    }
    
    /**
     * 检查PlayerPoints是否可用
     */
    fun isAvailable(): Boolean {
        return playerPointsPlugin?.isEnabled == true && playerPointsAPI != null
    }
    
    /**
     * 获取玩家的积分
     */
    fun getPoints(player: org.bukkit.entity.Player): Int {
        if (!isAvailable()) return 0
        
        return try {
            val api = playerPointsAPI ?: return 0
            val apiClass = api.javaClass
            val method = apiClass.getMethod("look", org.bukkit.OfflinePlayer::class.java)
            val result = method.invoke(api, player)
            
            // PlayerPoints返回的是Integer或Long
            when (result) {
                is Int -> result
                is Long -> result.toInt()
                else -> 0
            }
        } catch (e: Exception) {
            plugin.logger.warning("获取玩家积分时出错: ${e.message}")
            0
        }
    }
    
    /**
     * 设置玩家的积分
     */
    fun setPoints(player: org.bukkit.entity.Player, amount: Int): Boolean {
        if (!isAvailable()) return false
        
        return try {
            val api = playerPointsAPI ?: return false
            val apiClass = api.javaClass
            val method = apiClass.getMethod("set", org.bukkit.OfflinePlayer::class.java, Int::class.java)
            method.invoke(api, player, amount)
            true
        } catch (e: Exception) {
            plugin.logger.warning("设置玩家积分时出错: ${e.message}")
            false
        }
    }
    
    /**
     * 给玩家增加积分
     */
    fun givePoints(player: org.bukkit.entity.Player, amount: Int): Boolean {
        if (!isAvailable()) return false
        
        return try {
            val api = playerPointsAPI ?: return false
            val apiClass = api.javaClass
            val method = apiClass.getMethod("give", org.bukkit.OfflinePlayer::class.java, Int::class.java)
            method.invoke(api, player, amount)
            true
        } catch (e: Exception) {
            plugin.logger.warning("给予玩家积分时出错: ${e.message}")
            false
        }
    }
    
    /**
     * 扣除玩家的积分
     */
    fun takePoints(player: org.bukkit.entity.Player, amount: Int): Boolean {
        if (!isAvailable()) return false
        
        return try {
            val api = playerPointsAPI ?: return false
            val apiClass = api.javaClass
            val method = apiClass.getMethod("take", org.bukkit.OfflinePlayer::class.java, Int::class.java)
            method.invoke(api, player, amount)
            true
        } catch (e: Exception) {
            plugin.logger.warning("扣除玩家积分时出错: ${e.message}")
            false
        }
    }
    
    /**
     * 检查玩家是否有足够的积分
     */
    fun hasPoints(player: org.bukkit.entity.Player, amount: Int): Boolean {
        return getPoints(player) >= amount
    }
    
    /**
     * 获取PlayerPoints插件的版本信息
     */
    fun getPlayerPointsVersion(): String {
        return if (isAvailable()) {
            playerPointsPlugin?.description?.version ?: "未知"
        } else {
            "未安装"
        }
    }
}
