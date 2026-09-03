package org.example.sharedhealthandhunger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Odpowiada za wielojęzyczność pluginu (English / Polski).
 * Wykorzystuje w 100% nowoczesne Paper Adventure API, eliminując przestarzały ChatColor.
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

    public Component getComponent(String key) {
        if (langConfig == null) return Component.text(key);
        String prefix = langConfig.getString("prefix", "&e[SharedHealth] &7");
        String msg = langConfig.getString(key, key);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg);
    }

    public Component getRawComponent(String key) {
        if (langConfig == null) return Component.text(key);
        String msg = langConfig.getString(key, key);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    public String getMsg(String key) {
        return LegacyComponentSerializer.legacySection().serialize(getComponent(key));
    }

    public String getRawMsg(String key) {
        return LegacyComponentSerializer.legacySection().serialize(getRawComponent(key));
    }

    public void sendMessage(CommandSender sender, String key) {
        sender.sendMessage(getComponent(key));
    }

    public void broadcast(String key) {
        Bukkit.broadcast(getComponent(key));
    }

    public void broadcastFormatted(String key, String target, String replacement) {
        String raw = langConfig != null ? langConfig.getString(key, key) : key;
        String prefix = langConfig != null ? langConfig.getString("prefix", "&e[SharedHealth] &7") : "";
        String text = (prefix + raw).replace(target, replacement);
        Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }
}
