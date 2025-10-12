package org.Aauu.aauuMobCapital.manager;

import org.Aauu.aauuMobCapital.AauuMobCapital;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private final AauuMobCapital plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;
    private final Map<String, FileConfiguration> guiConfigs;
    
    public ConfigManager(AauuMobCapital plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
        this.guiConfigs = new HashMap<>();
    }
    
    public void initialize() {
        loadConfig("messages");
        loadConfig("entities_normal");
        loadConfig("entities_premium");
        loadConfig("normal_upgrades");
        loadConfig("premium_upgrades");
        
        loadGuiConfigs();
    }
    
    private void loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        if (!file.exists()) {
            plugin.saveResource(name + ".yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(name, config);
        configFiles.put(name, file);
    }
    
    private void loadGuiConfigs() {
        File guiFolder = new File(plugin.getDataFolder(), "gui");
        if (!guiFolder.exists()) {
            guiFolder.mkdirs();
        }
        
        String[] guiFiles = {"main_menu.yml", "upgrade_menu.yml", "entity_menu.yml", "buy_limit_menu.yml", "precise_pos_menu.yml"};
        
        for (String fileName : guiFiles) {
            File file = new File(guiFolder, fileName);
            if (!file.exists()) {
                try (InputStream in = plugin.getResource("gui/" + fileName)) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("无法创建GUI配置文件: " + fileName);
                }
            }
            
            if (file.exists()) {
                String guiName = fileName.replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                guiConfigs.put(guiName, config);
            }
        }
    }
    
    public FileConfiguration getConfig(String name) {
        return configs.getOrDefault(name, null);
    }
    
    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }
    
    public FileConfiguration getMessages() {
        return getConfig("messages");
    }
    
    public FileConfiguration getGuis() {
        return getConfig("guis");
    }
    
    public FileConfiguration getGuiConfig(String guiName) {
        return guiConfigs.getOrDefault(guiName, null);
    }
    
    public FileConfiguration getNormalEntities() {
        return getConfig("entities_normal");
    }
    
    public FileConfiguration getPremiumEntities() {
        return getConfig("entities_premium");
    }
    
    public FileConfiguration getNormalUpgrades() {
        return getConfig("normal_upgrades");
    }
    
    public FileConfiguration getPremiumUpgrades() {
        return getConfig("premium_upgrades");
    }
    
    public void saveConfig(String name) {
        FileConfiguration config = configs.get(name);
        File file = configFiles.get(name);
        
        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("无法保存配置文件: " + name);
                e.printStackTrace();
            }
        }
    }
    
    public void reloadAll() {
        plugin.reloadConfig();
        
        for (String name : configs.keySet()) {
            File file = configFiles.get(name);
            if (file != null && file.exists()) {
                configs.put(name, YamlConfiguration.loadConfiguration(file));
            }
        }
        
        guiConfigs.clear();
        loadGuiConfigs();
    }
    
    public String getMessage(String path) {
        FileConfiguration messages = getMessages();
        if (messages == null) {
            return path;
        }
        
        String message = messages.getString(path, path);
        return colorize(message);
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return message;
    }
    
    private String colorize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('&', '§');
    }
}
