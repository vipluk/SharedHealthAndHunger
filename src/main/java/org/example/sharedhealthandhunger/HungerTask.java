package org.example.sharedhealthandhunger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Odpowiada za precyzyjne przeliczanie mnożnika utraty głodu (hunger-loss-multiplier).
 * Prawidłowo respektuje waniliowy cykl wyczerpania (exhaustion 0.0 - 4.0),
 * zapobiegając desynchronizacjom i drżeniu ikon głodu na klientach Bedrock.
 */
public class HungerTask extends BukkitRunnable {

    private final Main plugin;
    private final Map<UUID, Float> lastExhaustionMap = new HashMap<>();

    public HungerTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().isEnabledFood()) return;
        double multiplier = plugin.getConfigManager().getHungerLossMultiplier();
        if (multiplier <= 1.0) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isOnline() || p.isDead()) continue;
            if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) continue;
            if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;

            UUID id = p.getUniqueId();
            float currentExhaustion = p.getExhaustion();
            float lastExhaustion = lastExhaustionMap.getOrDefault(id, currentExhaustion);

            float delta = currentExhaustion - lastExhaustion;

            if (delta > 0) {
                float extra = (float) (delta * (multiplier - 1.0));
                float newTotal = currentExhaustion + extra;

                if (newTotal >= 4.0f) {
                    int dropSteps = (int) (newTotal / 4.0f);
                    float remainder = newTotal % 4.0f;

                    float sat = p.getSaturation();
                    if (sat > 0) {
                        float satToDeduct = Math.min(sat, (float) dropSteps);
                        p.setSaturation(Math.max(0.0f, sat - satToDeduct));
                        dropSteps -= (int) satToDeduct;
                    }

                    if (dropSteps > 0 && p.getFoodLevel() > 0) {
                        p.setFoodLevel(Math.max(0, p.getFoodLevel() - dropSteps));
                    }

                    p.setExhaustion(remainder);
                    lastExhaustionMap.put(id, remainder);
                } else {
                    p.setExhaustion(newTotal);
                    lastExhaustionMap.put(id, newTotal);
                }
            } else {
                lastExhaustionMap.put(id, currentExhaustion);
            }
        }
    }

    public void removePlayer(UUID uuid) {
        lastExhaustionMap.remove(uuid);
    }

    public void clearAll() {
        lastExhaustionMap.clear();
    }
}
