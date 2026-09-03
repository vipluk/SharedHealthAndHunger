package org.example.sharedhealthandhunger.compat;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Moduł zapewniający 100% kompatybilność z pluginem WorldReset.
 * Izoluje świat Limbo (brak niepożądanej synchronizacji obrażeń czy głodu w poczekalni),
 * wspiera procedurę resetu mapy i koordynuje zgranie zdarzeń zgonów (/wr death).
 */
public final class WorldResetHook {

    private final Set<String> ignoredWorlds = new HashSet<>();
    private boolean worldResetPresent = false;

    public WorldResetHook() {
        checkWorldResetPlugin();
    }

    public void updateIgnoredWorlds(List<String> worldsFromConfig) {
        ignoredWorlds.clear();
        if (worldsFromConfig != null) {
            for (String w : worldsFromConfig) {
                if (w != null && !w.trim().isEmpty()) {
                    ignoredWorlds.add(w.trim().toLowerCase());
                }
            }
        }
        // Świat Limbo z WorldReset jest zawsze ignorowany
        ignoredWorlds.add("limbo");
    }

    public void checkWorldResetPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldReset");
        worldResetPresent = (plugin != null && plugin.isEnabled());
    }

    public boolean isWorldResetPresent() {
        return worldResetPresent;
    }

    /**
     * Sprawdza, czy dany świat powinien być ignorowany przez mechanizmy synchronizacji
     * (np. świat Limbo poczekalni w WorldReset).
     */
    public boolean isIgnoredWorld(World world) {
        if (world == null) return false;
        return ignoredWorlds.contains(world.getName().toLowerCase());
    }

    /**
     * Sprawdza, czy gracz przebywa w ignorowanym świecie.
     */
    public boolean isPlayerInIgnoredWorld(Player player) {
        if (player == null || !player.isOnline()) return true;
        return isIgnoredWorld(player.getWorld());
    }
}
