package org.example.sharedhealthandhunger.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Moduł zapewniający 100% kompatybilność z Geyser Crossplay oraz Floodgate (Minecraft Bedrock).
 * Wykrywa graczy Bedrock i automatycznie dostosowuje skalowanie serc (Health Scale),
 * aby pasek życia na telefonach i konsolach nie ulegał błędom graficznym.
 */
public final class GeyserHook {

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

    private GeyserHook() {}

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

    /**
     * Sprawdza, czy gracz dołączył z Minecraft Bedrock Edition (przez Floodgate lub Geyser).
     */
    public static boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
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

        // Fallback na prefiks nazwy (domyślny w Geyserze i Floodgate, np. .Gracz lub *Gracz)
        String name = player.getName();
        return name.startsWith(".") || name.startsWith("*");
    }

    /**
     * Stosuje wizualne skalowanie serc dla graczy Bedrock (10 serc = 20 HP) przy niestandardowym max-health.
     * Zapobiega to nakładaniu się i wychodzeniu pasków serc poza ekran na urządzeniach mobilnych i konsolach.
     */
    public static void applyHealthScaling(Player player, boolean enabled, double currentMaxHealth) {
        if (!enabled) return;

        if (isBedrockPlayer(player)) {
            if (currentMaxHealth > 20.0) {
                player.setHealthScaled(true);
                player.setHealthScale(20.0);
            } else {
                player.setHealthScaled(false);
            }
        }
    }
}
