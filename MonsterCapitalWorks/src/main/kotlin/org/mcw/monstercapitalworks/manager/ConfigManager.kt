package org.mcw.monstercapitalworks.manager

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.mcw.monstercapitalworks.MonsterCapitalWorks
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.regex.Pattern

/**
 * 配置管理器 - 处理YAML配置文件加载和颜色代码转换
 */
class ConfigManager(private val plugin: MonsterCapitalWorks) {
    
    private val configs = mutableMapOf<String, FileConfiguration>()
    private val configFiles = mutableMapOf<String, File>()
    private val guiConfigs = mutableMapOf<String, FileConfiguration>()
    
    fun initialize() {
        loadConfig("messages")
        loadConfig("limits")
        loadConfig("entities_normal")
        loadConfig("entities_premium")
        loadConfig("normal_upgrades")
        loadConfig("premium_upgrades")
        
        loadGuiConfigs()
    }
    
    private fun loadConfig(name: String) {
        val file = File(plugin.dataFolder, "$name.yml")
        if (!file.exists()) {
            plugin.saveResource("$name.yml", false)
        }
        
        val config = YamlConfiguration.loadConfiguration(file)
        configs[name] = config
        configFiles[name] = file
    }
    
    private fun loadGuiConfigs() {
        val guiFolder = File(plugin.dataFolder, "gui")
        if (!guiFolder.exists()) {
            guiFolder.mkdirs()
        }
        
        val guiFiles = arrayOf(
            "main_menu_normal.yml",
            "main_menu_premium.yml",
            "upgrade_menu.yml",
            "entity_menu.yml",
            "buy_limit_menu.yml",
            "precise_pos_menu.yml"
        )
        
        for (fileName in guiFiles) {
            val file = File(guiFolder, fileName)
            if (!file.exists()) {
                try {
                    plugin.getResource("gui/$fileName")?.use { input ->
                        Files.copy(input, file.toPath())
                    }
                } catch (e: IOException) {
                    plugin.logger.warning("无法创建GUI配置文件: $fileName")
                }
            }
            
            if (file.exists()) {
                val guiName = fileName.replace(".yml", "")
                val config = YamlConfiguration.loadConfiguration(file)
                guiConfigs[guiName] = config
            }
        }
    }
    
    fun getConfig(name: String): FileConfiguration? = configs[name]
    
    fun getMainConfig(): FileConfiguration = plugin.config
    
    fun getMessages(): FileConfiguration? = getConfig("messages")
    
    fun getLimits(): FileConfiguration? = getConfig("limits")
    
    fun getGuiConfig(guiName: String): FileConfiguration? = guiConfigs[guiName]
    
    fun getNormalEntities(): FileConfiguration? = getConfig("entities_normal")
    
    fun getPremiumEntities(): FileConfiguration? = getConfig("entities_premium")
    
    fun getNormalUpgrades(): FileConfiguration? = getConfig("normal_upgrades")
    
    fun getPremiumUpgrades(): FileConfiguration? = getConfig("premium_upgrades")
    
    fun saveConfig(name: String) {
        val config = configs[name]
        val file = configFiles[name]
        
        if (config != null && file != null) {
            try {
                config.save(file)
            } catch (e: IOException) {
                plugin.logger.severe("无法保存配置文件: $name")
                e.printStackTrace()
            }
        }
    }
    
    fun reloadAll() {
        plugin.reloadConfig()
        
        for (name in configs.keys) {
            val file = configFiles[name]
            if (file != null && file.exists()) {
                configs[name] = YamlConfiguration.loadConfiguration(file)
            }
        }
        
        guiConfigs.clear()
        loadGuiConfigs()
    }
    
    fun getMessage(path: String): String {
        val messages = getMessages() ?: return path
        
        val prefix = messages.getString("prefix", "")
        val message = messages.getString(path, path)
        return colorize(prefix + message)
    }
    
    fun getMessage(path: String, placeholders: Map<String, String>? = null): String {
        val messages = getMessages() ?: return path
        
        val prefix = messages.getString("prefix", "")
        var message = messages.getString(path, path)
        
        if (placeholders != null) {
            for ((key, value) in placeholders) {
                message = message.replace(key, value)
            }
        }
        
        return colorize(prefix + message)
    }
    
    /**
     * 获取列表消息（不添加前缀）
     */
    fun getMessageList(path: String, placeholders: Map<String, String>? = null): List<String> {
        val messages = getMessages() ?: return emptyList()
        
        val messageList = messages.getStringList(path)
        val processedList = mutableListOf<String>()
        
        for (line in messageList) {
            var processedLine = line
            if (placeholders != null) {
                for ((key, value) in placeholders) {
                    processedLine = processedLine.replace(key, value)
                }
            }
            processedList.add(colorize(processedLine))
        }
        
        return processedList
    }
    
    /**
     * 将文本中的颜色代码转换为Minecraft格式
     * 支持RGB颜色格式: {#RRGGBB}
     * 支持传统颜色代码: &a, &b, &c 等
     */
    fun colorize(text: String?): String {
        if (text == null) {
            return ""
        }
        
        // 处理 RGB 颜色格式 {#RRGGBB}
        val hexPattern = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}")
        val matcher = hexPattern.matcher(text)
        val buffer = StringBuffer()
        
        while (matcher.find()) {
            val hexColor = matcher.group(1)
            val replacement = StringBuilder("§x")
            
            // 将每个十六进制字符转换为 §x§r§r§g§g§b§b 格式
            for (c in hexColor.toCharArray()) {
                replacement.append("§").append(c)
            }
            
            matcher.appendReplacement(buffer, replacement.toString())
        }
        matcher.appendTail(buffer)
        
        // 处理传统颜色代码 &
        return buffer.toString().replace('&', '§')
    }
    
    /**
     * 获取刷怪笼自定义名称
     */
    fun getSpawnerCustomName(spawnerType: String, entityType: String): String? {
        val config = getMainConfig()
        
        if (!config.getBoolean("spawner_names.enabled", false)) {
            return null
        }
        
        // 检查是否有自定义名称配置
        val customNamePath = "spawner_names.types.$spawnerType.custom_names.$entityType"
        if (config.contains(customNamePath)) {
            val customName = config.getString(customNamePath)
            // 如果配置的值不为空，使用自定义名称
            if (customName != null && customName.isNotEmpty()) {
                return colorize(customName)
            }
        }
        
        // 如果没有自定义名称或custom_names为空数组，使用默认实体格式
        val defaultFormat = config.getString("spawner_names.types.$spawnerType.default_entity_format")
        if (defaultFormat != null && defaultFormat.isNotEmpty()) {
            return colorize(defaultFormat.replace("%entity%", entityType))
        }
        
        // 使用全局默认格式
        val globalFormat = config.getString("spawner_names.default_format", "{#FFD700}%type% 刷怪笼") ?: "{#FFD700}%type% 刷怪笼"
        val typeName = config.getString("spawner_names.types.$spawnerType.display_name", spawnerType) ?: spawnerType
        return colorize(globalFormat.replace("%type%", typeName).replace("%entity%", entityType))
    }
    
    /**
     * 获取刷怪蛋自定义名称
     */
    fun getSpawnEggCustomName(entityType: String): String? {
        val config = getMainConfig()
        
        if (!config.getBoolean("entity_spawn_egg_names.enabled", false)) {
            return null
        }
        
        val path = "entity_spawn_egg_names.custom_names.$entityType"
        if (config.contains(path)) {
            val customName = config.getString(path)
            if (customName != null) {
                return colorize(customName)
            }
        }
        
        // 使用默认格式
        val defaultFormat = config.getString("entity_spawn_egg_names.default_format", "&e%entity% &7刷怪蛋") ?: "&e%entity% &7刷怪蛋"
        return colorize(defaultFormat.replace("%entity%", entityType))
    }
}
