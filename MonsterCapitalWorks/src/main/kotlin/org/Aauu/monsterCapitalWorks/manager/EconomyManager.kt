package org.Aauu.monsterCapitalWorks.manager

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import net.milkbowl.vault.economy.Economy

/**
 * 经济管理器 - 封装Vault经济系统
 * 提供类型安全的经济操作API
 */
class EconomyManager(private val plugin: JavaPlugin) {
    private var economy: Economy? = null

    fun initialize(): Boolean {
        val registration = plugin.server.servicesManager.getRegistration(Economy::class.java)
        return registration?.let {
            economy = it.provider
            plugin.logger.info("成功连接到Vault经济系统")
            true
        } ?: run {
            plugin.logger.warning("未找到Vault经济插件，经济功能将被禁用")
            false
        }
    }

    fun hasEconomy(): Boolean = economy != null

    fun getBalance(player: Player): Double = 
        economy?.getBalance(player) ?: 0.0

    fun withdraw(player: Player, amount: Double): Boolean =
        economy?.let { eco ->
            eco.withdrawPlayer(player, amount).transactionSuccess()
        } ?: false

    fun deposit(player: Player, amount: Double): Boolean =
        economy?.let { eco ->
            eco.depositPlayer(player, amount).transactionSuccess()
        } ?: false

    fun has(player: Player, amount: Double): Boolean =
        economy?.has(player, amount) ?: false

    fun format(amount: Double): String =
        economy?.format(amount) ?: amount.toString()
}
