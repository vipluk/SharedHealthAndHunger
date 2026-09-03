package org.example.sharedhealthandhunger.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.example.sharedhealthandhunger.Main;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Odpowiada za precyzyjną synchronizację poziomu głodu (food level) oraz nasycenia (saturation).
 * Uwzględnia zdarzenia spożywania posiłków (PlayerItemConsumeEvent), aby nasycenie z jedzenia
 * było wiernie przekazywane całemu zespołowi.
 */
public class FoodListener implements Listener {

    private final Main plugin;
    private final Set<UUID> suppressFood = new HashSet<>();

    public FoodListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!plugin.getConfigManager().isEnabledFood()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(player)) return;
        if (suppressFood.contains(player.getUniqueId())) return;

        syncFoodToOthers(player, event.getFoodLevel(), player.getSaturation(), player.getExhaustion());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getConfigManager().isEnabledFood()) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(player)) return;

        // Po spożyciu jedzenia nasycenie aktualizuje się pod koniec ticku
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                syncFoodToOthers(player, player.getFoodLevel(), player.getSaturation(), player.getExhaustion());
            }
        }, 1L);
    }

    public void syncFoodToOthers(Player source, int targetFood, float saturation, float exhaustion) {
        if (Bukkit.getOnlinePlayers().size() <= 1) return;

        int clampedFood = Math.max(0, Math.min(20, targetFood));
        float clampedSat = Math.max(0.0f, Math.min(20.0f, saturation));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(source.getUniqueId())) continue;
            if (p.isDead()) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(p)) continue;

            suppressFood.add(p.getUniqueId());
            try {
                p.setFoodLevel(clampedFood);
                p.setSaturation(clampedSat);
                p.setExhaustion(exhaustion);
            } finally {
                suppressFood.remove(p.getUniqueId());
            }
        }
    }

    public void clearSuppression() {
        suppressFood.clear();
    }
}
