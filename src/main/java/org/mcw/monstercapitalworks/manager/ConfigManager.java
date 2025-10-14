package org.mcw.monstercapitalworks.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private final org.mcw.monstercapitalworks.MonsterCapitalWorks plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;
    private final Map<String, FileConfiguration> guiConfigs;
    
    public ConfigManager(org.mcw.monstercapitalworks.MonsterCapitalWorks plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
        this.guiConfigs = new HashMap<>();
    }
    
    public void initialize() {
        loadConfig("messages");
        loadConfig("limits");
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
        
        String[] guiFiles = {
            "main_menu_normal.yml", 
            "main_menu_premium.yml", 
            "upgrade_menu.yml", 
            "entity_menu.yml", 
            "buy_limit_menu.yml", 
            "precise_pos_menu.yml"
        };
        
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
    
    public FileConfiguration getLimits() {
        return getConfig("limits");
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
        
        String prefix = messages.getString("prefix", "");
        String message = messages.getString(path, path);
        return colorize(prefix + message);
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        FileConfiguration messages = getMessages();
        if (messages == null) {
            return path;
        }
        
        String prefix = messages.getString("prefix", "");
        String message = messages.getString(path, path);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return colorize(prefix + message);
    }
    
    /**
     * 获取列表消息（不添加前缀）
     * @param path 消息路径
     * @param placeholders 占位符映射
     * @return 处理后的消息列表
     */
    public java.util.List<String> getMessageList(String path, Map<String, String> placeholders) {
        FileConfiguration messages = getMessages();
        if (messages == null) {
            return new java.util.ArrayList<>();
        }
        
        java.util.List<String> messageList = messages.getStringList(path);
        java.util.List<String> processedList = new java.util.ArrayList<>();
        
        for (String line : messageList) {
            if (placeholders != null) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    line = line.replace(entry.getKey(), entry.getValue());
                }
            }
            processedList.add(colorize(line));
        }
        
        return processedList;
    }
    
    /**
     * 将文本中的颜色代码转换为Minecraft格式
     * 支持RGB颜色格式: {#RRGGBB}
     * 支持传统颜色代码: &a, &b, &c 等
     * 
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    public String colorize(String text) {
        if (text == null) {
            return "";
        }
        
        // 处理 RGB 颜色格式 {#RRGGBB}
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
        java.util.regex.Matcher matcher = hexPattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            
            // 将每个十六进制字符转换为 §x§r§r§g§g§b§b 格式
            for (char c : hexColor.toCharArray()) {
                replacement.append("§").append(c);
            }
            
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        
        // 处理传统颜色代码 &
        return buffer.toString().replace('&', '§');
    }
    
    /**
     * 获取刷怪笼自定义名称
     * 
     * @param spawnerType 刷怪笼类型 (NORMAL/PREMIUM)
     * @param entityType 实体类型
     * @return 自定义名称，如果未配置则返回null
     */
    public String getSpawnerCustomName(String spawnerType, String entityType) {
        FileConfiguration config = getMainConfig();
        
        if (!config.getBoolean("spawner_names.enabled", false)) {
            return null;
        }
        
        // 检查是否有自定义名称配置
        String customNamePath = "spawner_names.types." + spawnerType + ".custom_names." + entityType;
        if (config.contains(customNamePath)) {
            String customName = config.getString(customNamePath);
            // 如果配置的值不为空，使用自定义名称
            if (customName != null && !customName.isEmpty()) {
                return colorize(customName);
            }
        }
        
        // 如果没有自定义名称或custom_names为空数组，使用默认实体格式
        String defaultFormat = config.getString("spawner_names.types." + spawnerType + ".default_entity_format");
        if (defaultFormat != null && !defaultFormat.isEmpty()) {
            return colorize(defaultFormat.replace("%entity%", entityType));
        }
        
        // 使用全局默认格式
        String globalFormat = config.getString("spawner_names.default_format", "{#FFD700}%type% 刷怪笼");
        String typeName = config.getString("spawner_names.types." + spawnerType + ".display_name", spawnerType);
        return colorize(globalFormat.replace("%type%", typeName).replace("%entity%", entityType));
    }
    
    /**
     * 获取刷怪蛋自定义名称
     * 
     * @param entityType 实体类型
     * @return 自定义名称，如果未配置则返回null
     */
    public String getSpawnEggCustomName(String entityType) {
        FileConfiguration config = getMainConfig();
        
        if (!config.getBoolean("entity_spawn_egg_names.enabled", false)) {
            return null;
        }
        
        String path = "entity_spawn_egg_names.custom_names." + entityType;
        if (config.contains(path)) {
            return colorize(config.getString(path));
        }
        
        // 使用默认格式
        String defaultFormat = config.getString("entity_spawn_egg_names.default_format", "&e%entity% &7刷怪蛋");
        return colorize(defaultFormat.replace("%entity%", entityType));
    }
}
