package org.mcw.monstercapitalworks.manager

import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import java.util.concurrent.CompletableFuture

/**
 * 经济管理器 - 处理Vault经济系统集成
 */
class EconomyManager(private val plugin: MonsterCapitalWorks) {
    
    private var economy: Economy? = null

    /**
     * 初始化经济系统
     */
    fun initialize(): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            plugin.logger.warning("未找到Vault插件，经济功能将不可用")
            return false
        }

        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            plugin.logger.warning("未找到经济插件，经济功能将不可用")
            return false
        }

        economy = rsp.provider
        plugin.logger.info("经济系统初始化成功: ${economy?.name}")
        return true
    }

    /**
     * 检查经济系统是否可用
     */
    fun isEnabled(): Boolean = economy != null

    /**
     * 获取玩家余额
     */
    fun getBalance(player: OfflinePlayer): Double {
        if (!isEnabled()) return 0.0
        return economy!!.getBalance(player)
    }

    /**
     * 检查玩家是否有足够的钱
     */
    fun has(player: OfflinePlayer, amount: Double): Boolean {
        if (!isEnabled()) return false
        return economy!!.has(player, amount)
    }

    /**
     * 扣除玩家金钱（同步）
     */
    fun withdraw(player: OfflinePlayer, amount: Double): Boolean {
        if (!isEnabled()) return false
        return economy!!.withdrawPlayer(player, amount).transactionSuccess()
    }

    /**
     * 给予玩家金钱（同步）
     */
    fun deposit(player: OfflinePlayer, amount: Double): Boolean {
        if (!isEnabled()) return false
        return economy!!.depositPlayer(player, amount).transactionSuccess()
    }

    /**
     * 格式化金钱显示
     */
    fun format(amount: Double): String {
        if (!isEnabled()) return amount.toString()
        return economy!!.format(amount)
    }

    /**
     * 获取货币名称（单数）
     */
    fun currencyNameSingular(): String {
        if (!isEnabled()) return "元"
        return economy!!.currencyNameSingular()
    }

    /**
     * 获取货币名称（复数）
     */
    fun currencyNamePlural(): String {
        if (!isEnabled()) return "元"
        return economy!!.currencyNamePlural()
    }
}
