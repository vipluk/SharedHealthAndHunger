package org.example.sharedhealthandhunger;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Zarządza konfiguracją pluginu (config.yml).
 */
public class ConfigManager {

    private final Main plugin;

    private boolean enabledHealth;
    private boolean enabledFood;
    private boolean enabledEffects;
    private boolean enabledActionBar;
    private boolean ignoreAttacker;
    private long cooldownMs;
    private double maxHealth;
    private boolean respawnSpectator;
    private double hungerLossMultiplier;

    private boolean worldresetCompatibility;
    private List<String> ignoredWorlds = new ArrayList<>();

    private boolean geyserCompatibility;
    private boolean geyserHealthScale;

    private boolean hurtAnimation;
    private boolean knockback;
    private double knockbackStrength;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadConfigValues() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        cfg.addDefault("language", "en");
        cfg.addDefault("enabled-health", true);
        cfg.addDefault("enabled-food", true);
        cfg.addDefault("enabled-effects", true);
        cfg.addDefault("enabled-actionbar", true);
        cfg.addDefault("ignore-attacker", false);
        cfg.addDefault("cooldown-ms", 100L);
        cfg.addDefault("max-health", 20.0);
        cfg.addDefault("respawn-spectator", true);
        cfg.addDefault("hunger-loss-multiplier", 1.0);

        cfg.addDefault("worldreset-compatibility", true);
        List<String> defaultIgnoredWorlds = new ArrayList<>();
        defaultIgnoredWorlds.add("limbo");
        cfg.addDefault("ignored-worlds", defaultIgnoredWorlds);

        cfg.addDefault("geyser-compatibility", true);
        cfg.addDefault("geyser-health-scale", true);

        cfg.addDefault("physical-feedback.hurt-animation", true);
        cfg.addDefault("physical-feedback.knockback", true);
        cfg.addDefault("physical-feedback.knockback-strength", 0.2);

        cfg.options().copyDefaults(true);
        plugin.saveConfig();

        enabledHealth = cfg.getBoolean("enabled-health", true);
        enabledFood = cfg.getBoolean("enabled-food", true);
        enabledEffects = cfg.getBoolean("enabled-effects", true);
        enabledActionBar = cfg.getBoolean("enabled-actionbar", true);
        ignoreAttacker = cfg.getBoolean("ignore-attacker", false);
        cooldownMs = cfg.getLong("cooldown-ms", 100L);
        maxHealth = Math.max(1.0, Math.min(1024.0, cfg.getDouble("max-health", 20.0)));
        respawnSpectator = cfg.getBoolean("respawn-spectator", true);
        hungerLossMultiplier = cfg.getDouble("hunger-loss-multiplier", 1.0);

        worldresetCompatibility = cfg.getBoolean("worldreset-compatibility", true);
        ignoredWorlds = cfg.getStringList("ignored-worlds");
        if (ignoredWorlds.isEmpty()) {
            ignoredWorlds = new ArrayList<>(defaultIgnoredWorlds);
        }

        geyserCompatibility = cfg.getBoolean("geyser-compatibility", true);
        geyserHealthScale = cfg.getBoolean("geyser-health-scale", true);

        hurtAnimation = cfg.getBoolean("physical-feedback.hurt-animation", true);
        knockback = cfg.getBoolean("physical-feedback.knockback", true);
        knockbackStrength = cfg.getDouble("physical-feedback.knockback-strength", 0.2);
    }

    public boolean isEnabledHealth() { return enabledHealth; }
    public void setEnabledHealth(boolean enabledHealth) {
        this.enabledHealth = enabledHealth;
        plugin.getConfig().set("enabled-health", enabledHealth);
        plugin.saveConfig();
    }

    public boolean isEnabledFood() { return enabledFood; }
    public void setEnabledFood(boolean enabledFood) {
        this.enabledFood = enabledFood;
        plugin.getConfig().set("enabled-food", enabledFood);
        plugin.saveConfig();
    }

    public boolean isEnabledEffects() { return enabledEffects; }
    public void setEnabledEffects(boolean enabledEffects) {
        this.enabledEffects = enabledEffects;
        plugin.getConfig().set("enabled-effects", enabledEffects);
        plugin.saveConfig();
    }

    public boolean isEnabledActionBar() { return enabledActionBar; }
    public void setEnabledActionBar(boolean enabledActionBar) {
        this.enabledActionBar = enabledActionBar;
        plugin.getConfig().set("enabled-actionbar", enabledActionBar);
        plugin.saveConfig();
    }

    public boolean isIgnoreAttacker() { return ignoreAttacker; }
    public void setIgnoreAttacker(boolean ignoreAttacker) {
        this.ignoreAttacker = ignoreAttacker;
        plugin.getConfig().set("ignore-attacker", ignoreAttacker);
        plugin.saveConfig();
    }

    public long getCooldownMs() { return cooldownMs; }

    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = Math.max(1.0, Math.min(1024.0, maxHealth));
        plugin.getConfig().set("max-health", this.maxHealth);
        plugin.saveConfig();
    }

    public boolean isRespawnSpectator() { return respawnSpectator; }
    public void setRespawnSpectator(boolean respawnSpectator) {
        this.respawnSpectator = respawnSpectator;
        plugin.getConfig().set("respawn-spectator", respawnSpectator);
        plugin.saveConfig();
    }

    public double getHungerLossMultiplier() { return hungerLossMultiplier; }
    public void setHungerLossMultiplier(double hungerLossMultiplier) {
        this.hungerLossMultiplier = hungerLossMultiplier;
        plugin.getConfig().set("hunger-loss-multiplier", hungerLossMultiplier);
        plugin.saveConfig();
    }

    public boolean isWorldresetCompatibility() { return worldresetCompatibility; }
    public List<String> getIgnoredWorlds() { return ignoredWorlds; }

    public boolean isGeyserCompatibility() { return geyserCompatibility; }
    public boolean isGeyserHealthScale() { return geyserHealthScale; }

    public boolean isHurtAnimation() { return hurtAnimation; }
    public boolean isKnockback() { return knockback; }
    public double getKnockbackStrength() { return knockbackStrength; }
}
