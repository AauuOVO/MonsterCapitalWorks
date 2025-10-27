package org.Aauu.monsterCapitalWorks.command.impl

import org.Aauu.monsterCapitalWorks.MonsterCapitalWorks
import org.Aauu.monsterCapitalWorks.command.Command
import org.Aauu.monsterCapitalWorks.command.CommandRegistry
import org.bukkit.command.CommandSender

/**
 * 帮助命令实现
 */
class HelpCommand(private val plugin: MonsterCapitalWorks) : Command {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        sender.sendMessage("§6§l=== Monster Capital Works ===")
        
        val commands = CommandRegistry.getRegisteredCommands()
        
        for ((name, command) in commands) {
            if (command.hasPermission(sender)) {
                sender.sendMessage("§e/mcw $name §7- ${command.getDescription()}")
            }
        }
        
        return true
    }
    
    override fun getPermission(): String? = null
    
    override fun getUsage(): String = "/mcw help"
    
    override fun getDescription(): String = "显示帮助信息"
}
