package org.mcw.monstercapitalworks;

import org.mcw.monstercapitalworks.command.MCWCommand;
import org.mcw.monstercapitalworks.hook.PlaceholderAPIHook;
import org.mcw.monstercapitalworks.listener.SpawnerListener;
import org.mcw.monstercapitalworks.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MonsterCapitalWorks extends JavaPlugin {
    
    private static org.mcw.monstercapitalworks.MonsterCapitalWorks instance;
    private ConfigManager configManager;
    private DataManager dataManager;
    private EconomyManager economyManager;
    private PermissionManager permissionManager;
    private SpawnerManager spawnerManager;
    private UpgradeManager upgradeManager;
    private GUIManager guiManager;
    private PlaceholderAPIHook placeholderHook;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("limits.yml", false);
        saveResource("entities_normal.yml", false);
        saveResource("entities_premium.yml", false);
        saveResource("normal_upgrades.yml", false);
        saveResource("premium_upgrades.yml", false);
        
        initializeManagers();
        registerCommands();
        registerEvents();
        
        if (getConfig().getBoolean("placeholderapi.enabled", true)) {
            setupPlaceholderAPI();
        }
        
        getLogger().info("MonsterCapitalWorks 已成功启动！");
    }

    @Override
    public void onDisable() {
        if (spawnerManager != null) {
            spawnerManager.shutdown();
        }
        if (dataManager != null) {
            dataManager.close();
        }
    }
    
    private void initializeManagers() {
        configManager = new ConfigManager(this);
        configManager.initialize();
        
        dataManager = new DataManager(this);
        dataManager.initialize();
        
        economyManager = new EconomyManager(this);
        if (getConfig().getBoolean("economy.enabled", true)) {
            economyManager.initialize();
        }
        
        permissionManager = new PermissionManager(this);
        
        spawnerManager = new SpawnerManager(this);
        spawnerManager.initialize();
        
        upgradeManager = new UpgradeManager(this);
        upgradeManager.initialize();
        
        guiManager = new GUIManager(this);
        guiManager.initialize();
    }
    
    private void registerCommands() {
        PluginCommand command = getCommand("mcw");
        if (command != null) {
            MCWCommand MCWCommand = new MCWCommand(this);
            command.setExecutor(MCWCommand);
            command.setTabCompleter(MCWCommand);
        }
    }
    
    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new SpawnerListener(this), this);
    }
    
    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderHook = new PlaceholderAPIHook(this);
            placeholderHook.register();
        }
    }
    
    public void reloadConfigs() {
        reloadConfig();
        configManager.reloadAll();
        upgradeManager.reload();
    }
    
    // Getters
    
    public static org.mcw.monstercapitalworks.MonsterCapitalWorks getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }
    
    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
}
