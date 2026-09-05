package org.example.sharedhealthandhunger;

import dev.faststats.bukkit.BukkitContext;
import dev.faststats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.Objects;

/**
 * Główna klasa pluginu SharedHealthAndHunger v1.4.
 * Zunifikowana architektura o wysokiej wydajności, w 100% kompatybilna z
 * WorldReset, Geyser Crossplay oraz wersjami Minecraft 1.21 - 26.2.
 */
public class Main extends JavaPlugin {

    private final BukkitContext fastStatsContext = new BukkitContext.Factory(this, "f7130edb41bc7dad6a1f697e74f54001")
            .metrics(Metrics.Factory::create)
            .create();

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private CompatibilityManager compatibilityManager;
    private SharedGameListener gameListener;
    private HungerTask hungerTask;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadConfigValues();

        languageManager = new LanguageManager(this);
        languageManager.init();

        compatibilityManager = new CompatibilityManager(this);
        compatibilityManager.updateIgnoredWorlds(configManager.getIgnoredWorlds());

        gameListener = new SharedGameListener(this);
        Bukkit.getPluginManager().registerEvents(gameListener, this);

        SharedHealthCommand cmd = new SharedHealthCommand(this);
        Objects.requireNonNull(getCommand("sharedhealth")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("sharedhealth")).setTabCompleter(cmd);

        hungerTask = new HungerTask(this);
        hungerTask.runTaskTimer(this, 10L, 10L);

        applyMaxValuesToOnlinePlayers();

        // FastStats Metrics
        if (fastStatsContext != null) {
            fastStatsContext.ready();
        }

        // bStats Metrics
        int bStatsPluginId = 33872;
        new org.bstats.bukkit.Metrics(this, bStatsPluginId);

        getLogger().info("SharedHealthAndHunger v1.4 enabled (Unified Architecture, WorldReset & Geyser ready).");
    }

    @Override
    public void onDisable() {
        if (fastStatsContext != null) {
            fastStatsContext.shutdown();
        }
        if (hungerTask != null) {
            hungerTask.cancel();
        }
        getLogger().info("SharedHealthAndHunger disabled.");
    }

    public void reloadPlugin() {
        configManager.loadConfigValues();
        languageManager.loadLanguage();
        compatibilityManager.updateIgnoredWorlds(configManager.getIgnoredWorlds());
        applyMaxValuesToOnlinePlayers();
    }

    public void applyMaxValuesToOnlinePlayers() {
        double maxHp = configManager.getMaxHealth();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (compatibilityManager.isPlayerInIgnoredWorld(p)) continue;

            compatibilityManager.setMaxHealth(p, maxHp);
            p.setHealth(maxHp);
            compatibilityManager.applyHealthScaling(p, maxHp);
        }
    }

    public void resetGame() {
        gameListener.clearAllSuppression();
        hungerTask.clearAll();

        double maxHp = configManager.getMaxHealth();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (compatibilityManager.isPlayerInIgnoredWorld(p)) continue;

            p.setGameMode(GameMode.SURVIVAL);
            compatibilityManager.setMaxHealth(p, maxHp);
            p.setHealth(maxHp);
            compatibilityManager.applyHealthScaling(p, maxHp);

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

        languageManager.broadcast("game-reset");
    }

    public void syncAllTeamStats() {
        double maxHp = configManager.getMaxHealth();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (compatibilityManager.isPlayerInIgnoredWorld(p)) continue;

            compatibilityManager.setMaxHealth(p, maxHp);
            p.setHealth(maxHp);
            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            compatibilityManager.applyHealthScaling(p, maxHp);
        }
    }

    public void syncAllToLowestHealth() {
        double minHealth = configManager.getMaxHealth();
        boolean foundAny = false;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
            if (compatibilityManager.isPlayerInIgnoredWorld(p)) continue;

            if (p.getHealth() < minHealth) {
                minHealth = p.getHealth();
            }
            foundAny = true;
        }

        if (foundAny) {
            double finalMin = minHealth;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
                if (compatibilityManager.isPlayerInIgnoredWorld(p)) continue;

                p.setHealth(finalMin);
            }
            languageManager.broadcastFormatted("sync-health", "{value}", String.format("%.1f", finalMin));
        }
    }

    public void syncAllToLowestFood() {
        int minFood = 20;
        boolean foundAny = false;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
            if (compatibilityManager.isPlayerInIgnoredWorld(p)) continue;

            if (p.getFoodLevel() < minFood) {
                minFood = p.getFoodLevel();
            }
            foundAny = true;
        }

        if (foundAny) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
                if (compatibilityManager.isPlayerInIgnoredWorld(p)) continue;

                p.setFoodLevel(minFood);
            }
            languageManager.broadcastFormatted("sync-food", "{value}", String.valueOf(minFood));
        }
    }

    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public CompatibilityManager getCompatibilityManager() { return compatibilityManager; }
    public HungerTask getHungerTask() { return hungerTask; }
}