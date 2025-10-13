package org.Aauu.aauuMobCapital;

import org.Aauu.aauuMobCapital.command.AMCCommand;
import org.Aauu.aauuMobCapital.hook.PlaceholderAPIHook;
import org.Aauu.aauuMobCapital.listener.SpawnerListener;
import org.Aauu.aauuMobCapital.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AauuMobCapital extends JavaPlugin {
    
    private static AauuMobCapital instance;
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
        PluginCommand command = getCommand("amc");
        if (command != null) {
            AMCCommand amcCommand = new AMCCommand(this);
            command.setExecutor(amcCommand);
            command.setTabCompleter(amcCommand);
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
    
    public static AauuMobCapital getInstance() {
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
