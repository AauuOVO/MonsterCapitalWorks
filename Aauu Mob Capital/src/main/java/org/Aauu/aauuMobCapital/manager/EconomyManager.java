package org.Aauu.aauuMobCapital.manager;

import net.milkbowl.vault.economy.Economy;
import org.Aauu.aauuMobCapital.AauuMobCapital;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.concurrent.CompletableFuture;

/**
 * 经济管理器 - 处理Vault经济系统集成
 */
public class EconomyManager {
    private final AauuMobCapital plugin;
    private Economy economy;

    public EconomyManager(AauuMobCapital plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化经济系统
     */
    public boolean initialize() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("未找到Vault插件，经济功能将不可用");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("未找到经济插件，经济功能将不可用");
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("经济系统初始化成功: " + economy.getName());
        return true;
    }

    /**
     * 检查经济系统是否可用
     */
    public boolean isEnabled() {
        return economy != null;
    }

    /**
     * 获取玩家余额
     */
    public double getBalance(OfflinePlayer player) {
        if (!isEnabled()) return 0;
        return economy.getBalance(player);
    }

    /**
     * 检查玩家是否有足够的钱
     */
    public boolean has(OfflinePlayer player, double amount) {
        if (!isEnabled()) return false;
        return economy.has(player, amount);
    }

    /**
     * 异步扣除玩家金钱
     */
    public CompletableFuture<Boolean> withdraw(OfflinePlayer player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isEnabled()) return false;
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        });
    }

    /**
     * 异步给予玩家金钱
     */
    public CompletableFuture<Boolean> deposit(OfflinePlayer player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isEnabled()) return false;
            return economy.depositPlayer(player, amount).transactionSuccess();
        });
    }

    /**
     * 格式化金钱显示
     */
    public String format(double amount) {
        if (!isEnabled()) return String.valueOf(amount);
        return economy.format(amount);
    }

    /**
     * 获取货币名称（单数）
     */
    public String currencyNameSingular() {
        if (!isEnabled()) return "元";
        return economy.currencyNameSingular();
    }

    /**
     * 获取货币名称（复数）
     */
    public String currencyNamePlural() {
        if (!isEnabled()) return "元";
        return economy.currencyNamePlural();
    }
}
