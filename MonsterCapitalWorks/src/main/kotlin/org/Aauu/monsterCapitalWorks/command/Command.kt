package org.Aauu.monsterCapitalWorks.command

import org.bukkit.command.CommandSender

/**
 * 命令接口 - 实现命令模式
 */
interface Command {
    /**
     * 执行命令
     * @param sender 命令发送者
     * @param args 命令参数
     * @return 是否执行成功
     */
    fun execute(sender: CommandSender, args: Array<String>): Boolean
    
    /**
     * 获取命令权限
     */
    fun getPermission(): String?
    
    /**
     * 获取命令用法
     */
    fun getUsage(): String
    
    /**
     * 获取命令描述
     */
    fun getDescription(): String
    
    /**
     * 检查发送者是否有权限执行此命令
     */
    fun hasPermission(sender: CommandSender): Boolean {
        val permission = getPermission()
        return permission == null || sender.hasPermission(permission)
    }
}
