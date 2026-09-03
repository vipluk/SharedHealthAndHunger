package org.example.sharedhealthandhunger.lang;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.example.sharedhealthandhunger.Main;

import java.io.File;

/**
 * Odpowiada za wielojęzyczność pluginu (English / Polski),
 * wczytywanie plików messages_*.yml, parsowanie kolorów i prefiksów.
 */
public class LanguageManager {

    private final Main plugin;
    private FileConfiguration langConfig;
    private String currentLanguage = "en";

    public LanguageManager(Main plugin) {
        this.plugin = plugin;
    }

    public void init() {
        saveLanguageFiles();
        loadLanguage();
    }

    public void saveLanguageFiles() {
        if (!new File(plugin.getDataFolder(), "messages_en.yml").exists()) {
            plugin.saveResource("messages_en.yml", false);
        }
        if (!new File(plugin.getDataFolder(), "messages_pl.yml").exists()) {
            plugin.saveResource("messages_pl.yml", false);
        }
    }

    public void loadLanguage() {
        currentLanguage = plugin.getConfig().getString("language", "en").toLowerCase();
        String fileName = "messages_" + currentLanguage + ".yml";
        File langFile = new File(plugin.getDataFolder(), fileName);

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + fileName + " not found! Falling back to English.");
            currentLanguage = "en";
            langFile = new File(plugin.getDataFolder(), "messages_en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public void setLanguage(String lang) {
        currentLanguage = lang.toLowerCase();
        plugin.getConfig().set("language", currentLanguage);
        plugin.saveConfig();
        loadLanguage();
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    @SuppressWarnings("deprecation")
    public String getMsg(String key) {
        if (langConfig == null) return key;
        String prefix = langConfig.getString("prefix", "&e[SharedHealth] &7");
        String msg = langConfig.getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    @SuppressWarnings("deprecation")
    public String getRawMsg(String key) {
        if (langConfig == null) return key;
        String msg = langConfig.getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
