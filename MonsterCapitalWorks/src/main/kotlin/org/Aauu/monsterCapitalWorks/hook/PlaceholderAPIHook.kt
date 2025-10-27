package org.Aauu.monsterCapitalWorks.hook

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.Aauu.monsterCapitalWorks.MonsterCapitalWorks
import org.Aauu.monsterCapitalWorks.model.SpawnMode
import org.Aauu.monsterCapitalWorks.model.SpawnerType
import org.bukkit.entity.Player

class PlaceholderAPIHook(private val plugin: MonsterCapitalWorks) : PlaceholderExpansion() {
    
    override fun getIdentifier(): String = "mcw"
    
    override fun getAuthor(): String = plugin.description.authors.joinToString(", ")
    
    override fun getVersion(): String = plugin.description.version
    
    override fun persist(): Boolean = true
    
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) {
            return null
        }
        
        val spawner = plugin.getGUIManager().getSelectedSpawner(player.uniqueId)
        val data = plugin.getDataManager().getPlayerData(player.uniqueId)
        
        return when (params) {
            // 刷怪笼数量相关
            "spawners_total" -> {
                val spawners = plugin.getSpawnerManager().getPlayerSpawners(player.uniqueId)
                spawners.size.toString()
            }
            "spawners_normal" -> {
                plugin.getSpawnerManager().getPlayerSpawnerCount(player.uniqueId, SpawnerType.NORMAL).toString()
            }
            "spawners_premium" -> {
                plugin.getSpawnerManager().getPlayerSpawnerCount(player.uniqueId, SpawnerType.PREMIUM).toString()
            }
            
            // 刷怪笼限制相关
            "limit_normal" -> {
                plugin.getPermissionManager().getSpawnerLimit(player, SpawnerType.NORMAL).toString()
            }
            "limit_premium" -> {
                plugin.getPermissionManager().getSpawnerLimit(player, SpawnerType.PREMIUM).toString()
            }
            
            // 玩家数据相关
            "purchased_normal" -> {
                data.normalPurchasedLimit.toString()
            }
            "purchased_premium" -> {
                data.premiumPurchasedLimit.toString()
            }
            
            // 当前刷怪笼基本信息（需要打开GUI）
            "type" -> spawner?.type?.name ?: "N/A"
            "entity" -> spawner?.entityType?.name ?: "N/A"
            "location" -> spawner?.let { 
                String.format("%d, %d, %d", it.location.blockX, it.location.blockY, it.location.blockZ)
            } ?: "N/A"
            "world" -> spawner?.location?.world?.name ?: "N/A"
            "active" -> spawner?.let { if (it.active) "§a开启" else "§c关闭" } ?: "N/A"
            "status" -> spawner?.let { if (it.active) "§a激活" else "§c未激活" } ?: "N/A"
            "stored" -> spawner?.storedSpawns?.toString() ?: "N/A"
            "spawn_mode" -> spawner?.let { 
                if (it.spawnMode == SpawnMode.RANDOM) "§e随机模式" else "§b精确模式"
            } ?: "N/A"
            
            // 精确位置相关
            "precise_x" -> spawner?.let { String.format("%.1f", it.preciseX) } ?: "N/A"
            "precise_y" -> spawner?.let { String.format("%.1f", it.preciseY) } ?: "N/A"
            "precise_z" -> spawner?.let { String.format("%.1f", it.preciseZ) } ?: "N/A"
            "precise_pos" -> spawner?.let {
                String.format("§7X:§e%.1f §7Y:§e%.1f §7Z:§e%.1f", it.preciseX, it.preciseY, it.preciseZ)
            } ?: "§7未设置"
            
            // 升级等级相关
            "speed_level" -> spawner?.getUpgradeLevel("speed")?.toString() ?: "0"
            "count_level" -> spawner?.getUpgradeLevel("count")?.toString() ?: "0"
            "max_nearby_level" -> spawner?.getUpgradeLevel("max_nearby")?.toString() ?: "0"
            "range_level" -> spawner?.getUpgradeLevel("range")?.toString() ?: "0"
            "storage_level" -> spawner?.getUpgradeLevel("storage")?.toString() ?: "0"
            
            // 当前值相关
            "speed_value" -> spawner?.spawnDelay?.toString() ?: "N/A"
            "count_value" -> spawner?.spawnCount?.toString() ?: "N/A"
            "max_nearby_value" -> spawner?.maxNearbyEntities?.toString() ?: "N/A"
            "range_value" -> spawner?.activationRange?.toString() ?: "N/A"
            "storage_value" -> spawner?.maxStorage?.toString() ?: "N/A"
            
            // 下一级值相关
            "speed_next_value" -> spawner?.let { 
                plugin.getUpgradeManager().getNextUpgradeValue(it, "speed").toInt().toString()
            } ?: "N/A"
            "count_next_value" -> spawner?.let { 
                plugin.getUpgradeManager().getNextUpgradeValue(it, "count").toInt().toString()
            } ?: "N/A"
            "max_nearby_next_value" -> spawner?.let { 
                plugin.getUpgradeManager().getNextUpgradeValue(it, "max_nearby").toInt().toString()
            } ?: "N/A"
            "range_next_value" -> spawner?.let { 
                plugin.getUpgradeManager().getNextUpgradeValue(it, "range").toInt().toString()
            } ?: "N/A"
            "storage_next_value" -> spawner?.let { 
                plugin.getUpgradeManager().getNextUpgradeValue(it, "storage").toInt().toString()
            } ?: "N/A"
            
            // 升级费用相关
            "speed_cost" -> spawner?.let { 
                String.format("%.2f", plugin.getUpgradeManager().getUpgradeCost(it, "speed"))
            } ?: "N/A"
            "count_cost" -> spawner?.let { 
                String.format("%.2f", plugin.getUpgradeManager().getUpgradeCost(it, "count"))
            } ?: "N/A"
            "max_nearby_cost" -> spawner?.let { 
                String.format("%.2f", plugin.getUpgradeManager().getUpgradeCost(it, "max_nearby"))
            } ?: "N/A"
            "range_cost" -> spawner?.let { 
                String.format("%.2f", plugin.getUpgradeManager().getUpgradeCost(it, "range"))
            } ?: "N/A"
            "storage_cost" -> spawner?.let { 
                String.format("%.2f", plugin.getUpgradeManager().getUpgradeCost(it, "storage"))
            } ?: "N/A"
            
            // 购买费用相关
            "buy_1_cost_normal" -> String.format("%.2f", calculatePurchaseCostForDisplay(player, SpawnerType.NORMAL, 1))
            "buy_5_cost_normal" -> String.format("%.2f", calculatePurchaseCostForDisplay(player, SpawnerType.NORMAL, 5))
            "buy_10_cost_normal" -> String.format("%.2f", calculatePurchaseCostForDisplay(player, SpawnerType.NORMAL, 10))
            "buy_1_cost_premium" -> String.format("%.2f", calculatePurchaseCostForDisplay(player, SpawnerType.PREMIUM, 1))
            "buy_5_cost_premium" -> String.format("%.2f", calculatePurchaseCostForDisplay(player, SpawnerType.PREMIUM, 5))
            "buy_10_cost_premium" -> String.format("%.2f", calculatePurchaseCostForDisplay(player, SpawnerType.PREMIUM, 10))
            
            // 玩家经济相关
            "money" -> {
                val economy = plugin.getEconomyManager()
                if (economy.hasEconomy()) {
                    String.format("%.2f", economy.getBalance(player))
                } else {
                    "N/A"
                }
            }
            
            // 权限相关
            "has_admin" -> if (player.hasPermission("mcw.admin")) "§a是" else "§c否"
            "has_premium" -> if (player.hasPermission("mcw.use.premium")) "§a是" else "§c否"
            "has_precise" -> if (player.hasPermission("mcw.spawnmode.precise")) "§a是" else "§c否"
            
            // PlayerPoints积分相关
            "points" -> {
                val playerPointsHook = plugin.getPlayerPointsHook()
                if (playerPointsHook?.isAvailable() == true) {
                    playerPointsHook.getPoints(player).toString()
                } else {
                    "N/A"
                }
            }
            "points_formatted" -> {
                val playerPointsHook = plugin.getPlayerPointsHook()
                if (playerPointsHook?.isAvailable() == true) {
                    String.format("%,d", playerPointsHook.getPoints(player))
                } else {
                    "N/A"
                }
            }
            
            else -> null
        }
    }
    
    /**
     * 计算购买指定数量的费用（用于占位符显示）
     */
    private fun calculatePurchaseCostForDisplay(player: Player, type: SpawnerType, amount: Int): Double {
        val data = plugin.getDataManager().getPlayerData(player.uniqueId)
        val currentPurchased = data.getPurchasedLimit(type)
        val typePath = "limits.${type.name.lowercase()}"
        
        val basePrice = plugin.config.getDouble("$typePath.purchase.base_price", 1000.0)
        val priceMode = plugin.config.getString("$typePath.purchase.price_mode", "multiplier")
        val multiplier = plugin.config.getDouble("$typePath.purchase.price_multiplier", 1.2)
        
        var totalCost = 0.0
        
        if (priceMode.equals("fixed", ignoreCase = true)) {
            totalCost = basePrice * amount
        } else {
            for (i in 0 until amount) {
                val price = basePrice * Math.pow(multiplier, (currentPurchased + i).toDouble())
                totalCost += price
            }
        }
        
        return totalCost
    }
}
