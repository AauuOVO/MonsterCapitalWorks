package org.Aauu.monsterCapitalWorks.command

import org.bukkit.command.CommandSender
import java.util.concurrent.ConcurrentHashMap

/**
 * 命令注册器 - 管理所有子命令
 */
object CommandRegistry {
    
    private val commands = ConcurrentHashMap<String, Command>()
    
    /**
     * 注册命令
     */
    fun registerCommand(name: String, command: Command) {
        commands[name.lowercase()] = command
    }
    
    /**
     * 获取命令
     */
    fun getCommand(name: String): Command? {
        return commands[name.lowercase()]
    }
    
    /**
     * 执行命令
     */
    fun executeCommand(sender: CommandSender, name: String, args: Array<String>): Boolean {
        val command = getCommand(name) ?: return false
        
        if (!command.hasPermission(sender)) {
            sender.sendMessage("§c你没有权限执行此命令！")
            return true
        }
        
        return command.execute(sender, args)
    }
    
    /**
     * 获取所有已注册的命令
     */
    fun getRegisteredCommands(): Map<String, Command> {
        return commands.toMap()
    }
    
    /**
     * 获取命令补全建议
     */
    fun getTabCompletions(args: Array<String>): List<String> {
        val completions = mutableListOf<String>()
        
        when (args.size) {
            1 -> {
                // 补全子命令名称
                val input = args[0].lowercase()
                completions.addAll(commands.keys.filter { it.startsWith(input) })
            }
            else -> {
                // 委托给具体命令处理
                val command = getCommand(args[0])
                if (command is TabCompletable) {
                    completions.addAll(command.getTabCompletions(args))
                }
            }
        }
        
        return completions
    }
    
    /**
     * 可补全命令接口
     */
    interface TabCompletable {
        fun getTabCompletions(args: Array<String>): List<String>
    }
}
