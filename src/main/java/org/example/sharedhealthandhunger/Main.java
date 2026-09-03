package org.example.sharedhealthandhunger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.example.sharedhealthandhunger.commands.SharedHealthCommand;
import org.example.sharedhealthandhunger.compat.GeyserHook;
import org.example.sharedhealthandhunger.compat.VersionAdapter;
import org.example.sharedhealthandhunger.compat.WorldResetHook;
import org.example.sharedhealthandhunger.config.ConfigManager;
import org.example.sharedhealthandhunger.lang.LanguageManager;
import org.example.sharedhealthandhunger.listeners.EffectListener;
import org.example.sharedhealthandhunger.listeners.FoodListener;
import org.example.sharedhealthandhunger.listeners.HealthListener;
import org.example.sharedhealthandhunger.listeners.PlayerListener;
import org.example.sharedhealthandhunger.tasks.HungerTask;

import java.util.Objects;

/**
 * Główna klasa pluginu SharedHealthAndHunger v1.4.
 * Integruje moduły zdrowia, głodu, mikstur, WorldReset oraz Geyser Crossplay.
 */
public class Main extends JavaPlugin {

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private WorldResetHook worldResetHook;
    private HungerTask hungerTask;

    private HealthListener healthListener;
    private FoodListener foodListener;
    private EffectListener effectListener;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        // Inicjalizacja konfiguracji i języka
        configManager = new ConfigManager(this);
        configManager.loadConfigValues();

        languageManager = new LanguageManager(this);
        languageManager.init();

        // Inicjalizacja integracji z WorldReset
        worldResetHook = new WorldResetHook();
        worldResetHook.updateIgnoredWorlds(configManager.getIgnoredWorlds());

        // Inicjalizacja listenerów
        healthListener = new HealthListener(this);
        foodListener = new FoodListener(this);
        effectListener = new EffectListener(this);
        playerListener = new PlayerListener(this);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(healthListener, this);
        pm.registerEvents(foodListener, this);
        pm.registerEvents(effectListener, this);
        pm.registerEvents(playerListener, this);

        // Rejestracja komend
        SharedHealthCommand commandHandler = new SharedHealthCommand(this);
        Objects.requireNonNull(getCommand("sharedhealth")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("sharedhealth")).setTabCompleter(commandHandler);

        // Uruchomienie taska głodu
        hungerTask = new HungerTask(this);
        hungerTask.runTaskTimer(this, 10L, 10L);

        // Zastosowanie początkowych wartości dla graczy online
        applyMaxValuesToOnlinePlayers();

        getLogger().info("SharedHealthAndHunger v1.4 enabled (WorldReset, Geyser & 1.21-26.2 compatible).");
    }

    @Override
    public void onDisable() {
        if (hungerTask != null) {
            hungerTask.cancel();
        }
        getLogger().info("SharedHealthAndHunger disabled.");
    }

    public void reloadPlugin() {
        configManager.loadConfigValues();
        languageManager.loadLanguage();
        worldResetHook.updateIgnoredWorlds(configManager.getIgnoredWorlds());
        applyMaxValuesToOnlinePlayers();
    }

    public void applyMaxValuesToOnlinePlayers() {
        double maxHp = configManager.getMaxHealth();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (worldResetHook.isPlayerInIgnoredWorld(p)) continue;

            VersionAdapter.setMaxHealth(p, maxHp);
            p.setHealth(maxHp);
            GeyserHook.applyHealthScaling(p, configManager.isGeyserHealthScale(), maxHp);
        }
    }

    public void resetGame() {
        healthListener.clearSuppression();
        foodListener.clearSuppression();
        effectListener.clearSuppression();
        hungerTask.clearAll();

        double maxHp = configManager.getMaxHealth();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (worldResetHook.isPlayerInIgnoredWorld(p)) continue;

            p.setGameMode(GameMode.SURVIVAL);
            VersionAdapter.setMaxHealth(p, maxHp);
            p.setHealth(maxHp);
            GeyserHook.applyHealthScaling(p, configManager.isGeyserHealthScale(), maxHp);

            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            p.setExhaustion(0.0f);
            p.setFireTicks(0);
            p.setFallDistance(0);
            p.setVelocity(new Vector(0, 0, 0));

            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }

            Location spawn = p.getWorld().getSpawnLocation();
            p.teleport(spawn);
        }

        Bukkit.broadcastMessage(languageManager.getMsg("game-reset"));
    }

    public void syncAllTeamStats() {
        double maxHp = configManager.getMaxHealth();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (worldResetHook.isPlayerInIgnoredWorld(p)) continue;

            VersionAdapter.setMaxHealth(p, maxHp);
            p.setHealth(maxHp);
            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            GeyserHook.applyHealthScaling(p, configManager.isGeyserHealthScale(), maxHp);
        }
    }

    public void syncAllToLowestHealth() {
        double minHealth = configManager.getMaxHealth();
        boolean foundAny = false;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
            if (worldResetHook.isPlayerInIgnoredWorld(p)) continue;

            if (p.getHealth() < minHealth) {
                minHealth = p.getHealth();
            }
            foundAny = true;
        }

        if (foundAny) {
            double finalMin = minHealth;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
                if (worldResetHook.isPlayerInIgnoredWorld(p)) continue;

                p.setHealth(finalMin);
            }
            Bukkit.broadcastMessage(languageManager.getMsg("sync-health")
                    .replace("{value}", String.format("%.1f", finalMin)));
        }
    }

    public void syncAllToLowestFood() {
        int minFood = 20;
        boolean foundAny = false;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
            if (worldResetHook.isPlayerInIgnoredWorld(p)) continue;

            if (p.getFoodLevel() < minFood) {
                minFood = p.getFoodLevel();
            }
            foundAny = true;
        }

        if (foundAny) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
                if (worldResetHook.isPlayerInIgnoredWorld(p)) continue;

                p.setFoodLevel(minFood);
            }
            Bukkit.broadcastMessage(languageManager.getMsg("sync-food")
                    .replace("{value}", String.valueOf(minFood)));
        }
    }

    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public WorldResetHook getWorldResetHook() { return worldResetHook; }
    public HungerTask getHungerTask() { return hungerTask; }
}