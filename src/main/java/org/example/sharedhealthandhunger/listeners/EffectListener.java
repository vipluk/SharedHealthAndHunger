package org.example.sharedhealthandhunger.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.example.sharedhealthandhunger.Main;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Odpowiada za czystą i wolną od wyścigów synchronizację efektów mikstur.
 * Obsługuje dodawanie, usuwanie oraz picie mleka (czyszczenie efektów).
 */
public class EffectListener implements Listener {

    private final Main plugin;
    private final Set<UUID> suppressEffects = new HashSet<>();

    public EffectListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!plugin.getConfigManager().isEnabledEffects()) return;
        if (!(event.getEntity() instanceof Player source)) return;
        if (source.getGameMode() == GameMode.SPECTATOR) return;
        if (suppressEffects.contains(source.getUniqueId())) return;
        if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(source)) return;

        EntityPotionEffectEvent.Action action = event.getAction();
        PotionEffect newEffect = event.getNewEffect();
        PotionEffect oldEffect = event.getOldEffect();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!source.isOnline()) return;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(source)) continue;
                if (p.isDead()) continue;
                if (p.getGameMode() == GameMode.SPECTATOR) continue;
                if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(p)) continue;

                suppressEffects.add(p.getUniqueId());
                try {
                    if (action == EntityPotionEffectEvent.Action.ADDED || action == EntityPotionEffectEvent.Action.CHANGED) {
                        if (newEffect != null) {
                            p.addPotionEffect(newEffect);
                        }
                    } else if (action == EntityPotionEffectEvent.Action.REMOVED) {
                        if (oldEffect != null) {
                            p.removePotionEffect(oldEffect.getType());
                        }
                    } else if (action == EntityPotionEffectEvent.Action.CLEARED) {
                        if (oldEffect != null) {
                            p.removePotionEffect(oldEffect.getType());
                        } else {
                            for (PotionEffect pe : p.getActivePotionEffects()) {
                                p.removePotionEffect(pe.getType());
                            }
                        }
                    }
                } finally {
                    suppressEffects.remove(p.getUniqueId());
                }
            }
        });
    }

    public void clearSuppression() {
        suppressEffects.clear();
    }
}
