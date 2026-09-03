package org.example.sharedhealthandhunger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Zunifikowany menedżer kompatybilności pluginu.
 * Odpowiada za:
 * 1. Uniwersalne atrybuty zdrowia i animacje bólu (Minecraft 1.21 - 26.2).
 * 2. Pełną integrację z Geyser Crossplay / Floodgate (skalowanie serc na Bedrock).
 * 3. 100% kompatybilność z WorldReset (izolacja poczekalni Limbo).
 * 4. Wyświetlanie Action Bar w nowoczesnym standardzie Adventure API bez przestarzałego BungeeCorda.
 */
public class CompatibilityManager {

    private final Main plugin;
    private static final Attribute MAX_HEALTH_ATTR = resolveMaxHealthAttribute();

    // --- WorldReset ---
    private final Set<String> ignoredWorlds = new HashSet<>();
    private boolean worldResetPresent = false;

    // --- Geyser / Floodgate ---
    private static boolean floodgatePresent = false;
    private static boolean geyserPresent = false;
    private static Method floodgateIsBedrockMethod = null;
    private static Object floodgateApiInstance = null;
    private static Method geyserIsBedrockMethod = null;
    private static Object geyserApiInstance = null;

    static {
        initFloodgate();
        initGeyser();
    }

    public CompatibilityManager(Main plugin) {
        this.plugin = plugin;
        checkWorldResetPlugin();
    }

    // =========================================================================
    // 1. WERSJE MINECRAFT 1.21 - 26.2 (ATRYBUTY I ANIMACJE)
    // =========================================================================

    private static Attribute resolveMaxHealthAttribute() {
        try {
            Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("max_health"));
            if (attr != null) return attr;
        } catch (Throwable ignored) {}

        try {
            Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.max_health"));
            if (attr != null) return attr;
        } catch (Throwable ignored) {}

        try {
            return Attribute.valueOf("MAX_HEALTH");
        } catch (Throwable ignored) {}

        try {
            return Attribute.valueOf("GENERIC_MAX_HEALTH");
        } catch (Throwable ignored) {}

        return null;
    }

    public double getMaxHealth(Player player) {
        if (MAX_HEALTH_ATTR != null) {
            AttributeInstance inst = player.getAttribute(MAX_HEALTH_ATTR);
            if (inst != null) return inst.getValue();
        }
        return 20.0;
    }

    public void setMaxHealth(Player player, double value) {
        if (MAX_HEALTH_ATTR != null) {
            AttributeInstance inst = player.getAttribute(MAX_HEALTH_ATTR);
            if (inst != null) {
                inst.setBaseValue(value);
            }
        }
    }

    public void playHurtAnimation(Player player, float yaw) {
        try {
            player.playHurtAnimation(yaw);
        } catch (Throwable ignored) {}
    }

    public void sendActionBar(Player player, String message) {
        Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        player.sendActionBar(comp);
    }

    // =========================================================================
    // 2. GEYSER CROSSPLAY & FLOODGATE (BEDROCK EDITION)
    // =========================================================================

    private static void initFloodgate() {
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = floodgateApiClass.getMethod("getInstance");
            floodgateApiInstance = getInstance.invoke(null);
            floodgateIsBedrockMethod = floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class);
            floodgatePresent = true;
        } catch (Throwable ignored) {
            floodgatePresent = false;
        }
    }

    private static void initGeyser() {
        try {
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Method apiMethod = geyserApiClass.getMethod("api");
            geyserApiInstance = apiMethod.invoke(null);
            geyserIsBedrockMethod = geyserApiClass.getMethod("isBedrockPlayer", UUID.class);
            geyserPresent = true;
        } catch (Throwable ignored) {
            geyserPresent = false;
        }
    }

    public boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
        if (!plugin.getConfigManager().isGeyserCompatibility()) return false;

        UUID uuid = player.getUniqueId();

        if (floodgatePresent && floodgateIsBedrockMethod != null && floodgateApiInstance != null) {
            try {
                Object result = floodgateIsBedrockMethod.invoke(floodgateApiInstance, uuid);
                if (result instanceof Boolean bool && bool) return true;
            } catch (Throwable ignored) {}
        }

        if (geyserPresent && geyserIsBedrockMethod != null && geyserApiInstance != null) {
            try {
                Object result = geyserIsBedrockMethod.invoke(geyserApiInstance, uuid);
                if (result instanceof Boolean bool && bool) return true;
            } catch (Throwable ignored) {}
        }

        String name = player.getName();
        return name.startsWith(".") || name.startsWith("*");
    }

    public void applyHealthScaling(Player player, double currentMaxHealth) {
        if (!plugin.getConfigManager().isGeyserCompatibility()) return;
        if (!plugin.getConfigManager().isGeyserHealthScale()) return;

        if (isBedrockPlayer(player)) {
            if (currentMaxHealth > 20.0) {
                player.setHealthScaled(true);
                player.setHealthScale(20.0);
            } else {
                player.setHealthScaled(false);
            }
        }
    }

    // =========================================================================
    // 3. WORLDRESET & ŚWIATY IGNOROWANE (LIMBO)
    // =========================================================================

    public void checkWorldResetPlugin() {
        Plugin p = Bukkit.getPluginManager().getPlugin("WorldReset");
        worldResetPresent = (p != null && p.isEnabled());
    }

    public boolean isWorldResetPresent() {
        return worldResetPresent;
    }

    public void updateIgnoredWorlds(List<String> worlds) {
        ignoredWorlds.clear();
        if (plugin.getConfigManager().isWorldresetCompatibility()) {
            ignoredWorlds.add("limbo");
        }
        if (worlds != null) {
            for (String w : worlds) {
                if (w != null && !w.trim().isEmpty()) {
                    ignoredWorlds.add(w.trim().toLowerCase());
                }
            }
        }
    }

    public boolean isIgnoredWorld(World world) {
        if (world == null) return false;
        return ignoredWorlds.contains(world.getName().toLowerCase());
    }

    public boolean isPlayerInIgnoredWorld(Player player) {
        if (player == null || !player.isOnline()) return true;
        return isIgnoredWorld(player.getWorld());
    }
}
