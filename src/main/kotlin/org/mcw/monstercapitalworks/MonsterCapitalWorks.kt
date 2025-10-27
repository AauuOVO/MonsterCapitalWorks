package org.mcw.monstercapitalworks

import org.bukkit.plugin.java.JavaPlugin
import org.mcw.monstercapitalworks.command.MCWCommand
import org.mcw.monstercapitalworks.hook.PlaceholderAPIHook
import org.mcw.monstercapitalworks.listener.SpawnerListener
import org.mcw.monstercapitalworks.manager.*
import java.io.File

class MonsterCapitalWorks : JavaPlugin() {
    
    companion object {
        private var instance: MonsterCapitalWorks? = null
        
        fun getInstance(): MonsterCapitalWorks? = instance
    }
    
    lateinit var configManager: ConfigManager
    lateinit var dataManager: DataManager
    lateinit var economyManager: EconomyManager
    lateinit var permissionManager: PermissionManager
    lateinit var spawnerManager: SpawnerManager
    lateinit var upgradeManager: UpgradeManager
    lateinit var guiManager: GUIManager
    private var placeholderHook: PlaceholderAPIHook? = null

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        
        // 只在文件不存在时才生成配置文件，避免每次重启都覆盖已有配置
        saveResourceIfNotExists("messages.yml")
        saveResourceIfNotExists("limits.yml")
        saveResourceIfNotExists("entities_normal.yml")
        saveResourceIfNotExists("entities_premium.yml")
        saveResourceIfNotExists("normal_upgrades.yml")
        saveResourceIfNotExists("premium_upgrades.yml")
        
        initializeManagers()
        registerCommands()
        registerEvents()
        
        if (config.getBoolean("placeholderapi.enabled", true)) {
            setupPlaceholderAPI()
        }
        
        logger.info("MonsterCapitalWorks 已成功启动！")
    }
    
    /**
     * 只在文件不存在时才保存资源文件
     */
    private fun saveResourceIfNotExists(resourcePath: String) {
        val file = File(dataFolder, resourcePath)
        if (!file.exists()) {
            saveResource(resourcePath, false)
        }
    }

    override fun onDisable() {
        if (::spawnerManager.isInitialized) spawnerManager.shutdown()
        if (::dataManager.isInitialized) dataManager.close()
    }
    
    private fun initializeManagers() {
        configManager = ConfigManager(this)
        configManager.initialize()
        
        dataManager = DataManager(this)
        dataManager.initialize()
        
        economyManager = EconomyManager(this)
        if (config.getBoolean("economy.enabled", true)) {
            economyManager.initialize()
        }
        
        permissionManager = PermissionManager(this)
        
        spawnerManager = SpawnerManager(this)
        spawnerManager.initialize()
        
        upgradeManager = UpgradeManager(this)
        upgradeManager.initialize()
        
        guiManager = GUIManager(this)
        guiManager.initialize()
    }
    
    private fun registerCommands() {
        val command = getCommand("mcw")
        if (command != null) {
            val mcwCommand = MCWCommand(this)
            command.setExecutor(mcwCommand)
            command.setTabCompleter(mcwCommand)
        }
    }
    
    private fun registerEvents() {
        server.pluginManager.registerEvents(SpawnerListener(this), this)
    }
    
    private fun setupPlaceholderAPI() {
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            placeholderHook = PlaceholderAPIHook(this)
            placeholderHook!!.register()
        }
    }
    
    fun reloadConfigs() {
        reloadConfig()
        configManager.reloadAll()
        upgradeManager.reload()
    }
}
