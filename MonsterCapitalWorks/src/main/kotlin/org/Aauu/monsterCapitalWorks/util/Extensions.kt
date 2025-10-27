package org.Aauu.monsterCapitalWorks.util

import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

/**
 * Bukkit扩展函数集合
 * 提供常用的Kotlin风格API封装
 */

/**
 * 检查位置是否安全（用于生成实体）
 * 注意：此函数已废弃，请使用带EntityType参数的版本
 */
@Deprecated("使用isSafeSpawnFor(EntityType)替代", ReplaceWith("isSafeSpawnFor(entityType)"))
fun Location.isSafeSpawn(): Boolean {
    val block = this.block
    val blockAbove = this.clone().add(0.0, 1.0, 0.0).block
    
    // 只检查生成位置和上方不是固体方块
    return !block.type.isSolid && !blockAbove.type.isSolid
}

/**
 * 检查位置对特定实体类型是否安全
 */
fun Location.isSafeSpawnFor(entityType: EntityType): Boolean {
    val block = this.block
    val blockAbove = this.clone().add(0.0, 1.0, 0.0).block
    
    // 只检查生成位置和上方不是固体方块
    // 允许在空中生成，不限制下方必须有支撑
    return !block.type.isSolid && !blockAbove.type.isSolid
}

/**
 * 判断是否为飞行生物
 */
fun EntityType.isFlying(): Boolean = when (this) {
    EntityType.BAT,
    EntityType.GHAST,
    EntityType.ENDER_DRAGON,
    EntityType.WITHER,
    EntityType.PARROT,
    EntityType.BEE,
    EntityType.ALLAY,
    EntityType.VEX,
    EntityType.PHANTOM -> true
    else -> false
}

/**
 * 玩家消息扩展 - 发送带颜色的消息
 */
fun Player.msg(text: String) {
    sendMessage("§a$text")
}

/**
 * 玩家错误消息扩展
 */
fun Player.errorMsg(text: String) {
    sendMessage("§c$text")
}

/**
 * 玩家警告消息扩展
 */
fun Player.warnMsg(text: String) {
    sendMessage("§e$text")
}

/**
 * 玩家信息消息扩展
 */
fun Player.infoMsg(text: String) {
    sendMessage("§b$text")
}
