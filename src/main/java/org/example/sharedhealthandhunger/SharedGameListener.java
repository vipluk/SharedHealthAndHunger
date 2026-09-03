package org.example.sharedhealthandhunger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Zunifikowany listener zdarzeń gry dla SharedHealthAndHunger.
 * Koordynuje synchronizację życia, głodu, efektów mikstur oraz cykl życia graczy
 * w spójnej i wolnej od błędów architekturze.
 */
public class SharedGameListener implements Listener {

    private final Main plugin;

    private final Set<UUID> suppressHealth = new HashSet<>();
    private final Set<UUID> suppressFood = new HashSet<>();
    private final Set<UUID> suppressEffects = new HashSet<>();

    private long lastActionBarTime = 0L;
    private boolean isHandlingTeamDeath = false;

    public SharedGameListener(Main plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // 1. ZDROWIE, OBRAŻENIA I ZGONY
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfigManager().isEnabledHealth()) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        if (victim.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(victim)) return;
        if (suppressHealth.contains(victim.getUniqueId())) return;

        Player attacker = null;
        if (event instanceof EntityDamageByEntityEvent edbe && edbe.getDamager() instanceof Player p) {
            attacker = p;
        }

        double finalDamage = event.getFinalDamage();
        double currentHealth = victim.getHealth();
        double healthAfter = currentHealth - finalDamage;

        // Action Bar
        if (plugin.getConfigManager().isEnabledActionBar() && finalDamage > 0.05) {
            long now = System.currentTimeMillis();
            if (now - lastActionBarTime >= plugin.getConfigManager().getCooldownMs()) {
                lastActionBarTime = now;
                sendSharedActionBar(victim, finalDamage, Math.max(0.0, healthAfter));
            }
        }

        // Sprawdzenie śmiertelnych obrażeń
        if (healthAfter <= 0.0) {
            boolean hasTotem = (victim.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                    || victim.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING);

            if (hasTotem) {
                final Player fAttacker = attacker;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (victim.isOnline() && !victim.isDead()) {
                        syncHealthToOthers(victim, victim.getHealth(), fAttacker);
                    }
                }, 1L);
                return;
            }

            // Gracz nie ma totemu: pozwalamy, by PlayerDeathEvent wykonał się normalnie dla WorldReset
            return;
        }

        syncHealthToOthers(victim, healthAfter, attacker);

        if (finalDamage > 0.05) {
            applySharedPhysicalEffects(victim, attacker);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (!plugin.getConfigManager().isEnabledHealth()) return;
        if (!(event.getEntity() instanceof Player source)) return;
        if (suppressHealth.contains(source.getUniqueId())) return;
        if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(source)) return;

        double maxHealth = plugin.getConfigManager().getMaxHealth();
        double healthAfter = Math.min(maxHealth, source.getHealth() + event.getAmount());
        syncHealthToOthers(source, healthAfter, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isEnabledHealth()) return;
        if (isHandlingTeamDeath) return;

        Player dyingPlayer = event.getEntity();
        if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(dyingPlayer)) return;

        handleDeathLogic(dyingPlayer);
    }

    private void handleDeathLogic(Player dyingPlayer) {
        if (isHandlingTeamDeath) return;
        isHandlingTeamDeath = true;

        try {
            if (plugin.getConfigManager().isRespawnSpectator()) {
                plugin.getLanguageManager().broadcastFormatted("player-died", "{player}", dyingPlayer.getName());

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;
                    if (!p.equals(dyingPlayer)) {
                        p.setGameMode(GameMode.SPECTATOR);
                    }
                }
            } else {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.equals(dyingPlayer)) continue;
                    if (p.isDead()) continue;
                    if (p.getGameMode() == GameMode.SPECTATOR) continue;
                    if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;

                    p.setHealth(0.0);
                }
            }
        } finally {
            isHandlingTeamDeath = false;
        }
    }

    public void syncHealthToOthers(Player source, double targetHealth, Player attacker) {
        if (Bukkit.getOnlinePlayers().size() <= 1) return;

        double maxHealth = plugin.getConfigManager().getMaxHealth();
        double clamped = Math.max(0.0, Math.min(targetHealth, maxHealth));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(source.getUniqueId())) continue;
            if (p.isDead()) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;
            if (plugin.getConfigManager().isIgnoreAttacker() && attacker != null && p.equals(attacker)) continue;

            suppressHealth.add(p.getUniqueId());
            try {
                plugin.getCompatibilityManager().setMaxHealth(p, maxHealth);
                p.setHealth(clamped);
            } finally {
                suppressHealth.remove(p.getUniqueId());
            }
        }
    }

    private void applySharedPhysicalEffects(Player victim, Player attacker) {
        boolean hurtAnim = plugin.getConfigManager().isHurtAnimation();
        boolean knockback = plugin.getConfigManager().isKnockback();
        double kbStrength = plugin.getConfigManager().getKnockbackStrength();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(victim)) continue;
            if (p.isDead()) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;
            if (plugin.getConfigManager().isIgnoreAttacker() && attacker != null && p.equals(attacker)) continue;

            if (hurtAnim) {
                plugin.getCompatibilityManager().playHurtAnimation(p, p.getLocation().getYaw());
            }

            if (knockback && kbStrength > 0.0) {
                Vector dir = p.getLocation().getDirection().multiply(-kbStrength).setY(0.15);
                p.setVelocity(dir);
            }
        }
    }

    private void sendSharedActionBar(Player victim, double damage, double currentHp) {
        String damageStr = String.format("%.1f", damage);
        String currentHpStr = String.format("%.1f", currentHp);
        String maxHpStr = String.format("%.0f", plugin.getConfigManager().getMaxHealth());

        String msg = plugin.getLanguageManager().getRawMsg("actionbar-damage")
                .replace("{player}", victim.getName())
                .replace("{damage}", damageStr)
                .replace("{health}", currentHpStr)
                .replace("{max_health}", maxHpStr);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;
            plugin.getCompatibilityManager().sendActionBar(p, msg);
        }
    }

    // =========================================================================
    // 2. GŁÓD I NASYCENIE
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!plugin.getConfigManager().isEnabledFood()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(player)) return;
        if (suppressFood.contains(player.getUniqueId())) return;

        syncFoodToOthers(player, event.getFoodLevel(), player.getSaturation(), player.getExhaustion());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getConfigManager().isEnabledFood()) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(player)) return;

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
            if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;

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

    // =========================================================================
    // 3. EFEKTY MIKSTUR
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!plugin.getConfigManager().isEnabledEffects()) return;
        if (!(event.getEntity() instanceof Player source)) return;
        if (source.getGameMode() == GameMode.SPECTATOR) return;
        if (suppressEffects.contains(source.getUniqueId())) return;
        if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(source)) return;

        EntityPotionEffectEvent.Action action = event.getAction();
        PotionEffect newEffect = event.getNewEffect();
        PotionEffect oldEffect = event.getOldEffect();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!source.isOnline()) return;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(source)) continue;
                if (p.isDead()) continue;
                if (p.getGameMode() == GameMode.SPECTATOR) continue;
                if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;

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

    // =========================================================================
    // 4. CYKL ŻYCIA GRACZY (JOIN, QUIT, WORLD CHANGE, GAMEMODE, RESPAWN)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        double maxHealth = plugin.getConfigManager().getMaxHealth();

        if (plugin.getConfigManager().isEnabledHealth()) {
            plugin.getCompatibilityManager().setMaxHealth(player, maxHealth);
            plugin.getCompatibilityManager().applyHealthScaling(player, maxHealth);
        }

        if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(player)) {
            return;
        }

        if (shouldNewPlayerBeSpectator(player)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(plugin.getLanguageManager().getComponent("spectator-join"));
            return;
        }

        Player referencePlayer = findReferencePlayer(player);

        if (referencePlayer != null) {
            if (plugin.getConfigManager().isEnabledHealth()) {
                player.setHealth(Math.min(referencePlayer.getHealth(), maxHealth));
            }
            if (plugin.getConfigManager().isEnabledFood()) {
                player.setFoodLevel(referencePlayer.getFoodLevel());
                player.setSaturation(referencePlayer.getSaturation());
                player.setExhaustion(referencePlayer.getExhaustion());
            }
            if (plugin.getConfigManager().isEnabledEffects()) {
                for (PotionEffect effect : referencePlayer.getActivePotionEffects()) {
                    player.addPotionEffect(effect);
                }
            }
        } else {
            if (plugin.getConfigManager().isEnabledHealth()) {
                player.setHealth(maxHealth);
            }
            if (plugin.getConfigManager().isEnabledFood()) {
                player.setFoodLevel(20);
                player.setSaturation(5.0f);
                player.setExhaustion(0.0f);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getHungerTask().removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        boolean fromIgnored = plugin.getCompatibilityManager().isIgnoredWorld(event.getFrom());
        boolean toIgnored = plugin.getCompatibilityManager().isPlayerInIgnoredWorld(player);

        // Wyjście z Limbo do nowego świata gry
        if (fromIgnored && !toIgnored) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                double maxHp = plugin.getConfigManager().getMaxHealth();
                plugin.getCompatibilityManager().setMaxHealth(player, maxHp);
                plugin.getCompatibilityManager().applyHealthScaling(player, maxHp);

                Player ref = findReferencePlayer(player);
                if (ref != null) {
                    if (plugin.getConfigManager().isEnabledHealth()) player.setHealth(ref.getHealth());
                    if (plugin.getConfigManager().isEnabledFood()) {
                        player.setFoodLevel(ref.getFoodLevel());
                        player.setSaturation(ref.getSaturation());
                    }
                } else {
                    if (plugin.getConfigManager().isEnabledHealth()) player.setHealth(maxHp);
                    if (plugin.getConfigManager().isEnabledFood()) {
                        player.setFoodLevel(20);
                        player.setSaturation(5.0f);
                    }
                }
            }, 2L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.SURVIVAL) {
            Player player = event.getPlayer();
            if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(player)) return;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                double maxHp = plugin.getConfigManager().getMaxHealth();
                plugin.getCompatibilityManager().setMaxHealth(player, maxHp);
                plugin.getCompatibilityManager().applyHealthScaling(player, maxHp);

                Player ref = findReferencePlayer(player);
                if (ref != null) {
                    if (plugin.getConfigManager().isEnabledHealth()) player.setHealth(ref.getHealth());
                    if (plugin.getConfigManager().isEnabledFood()) {
                        player.setFoodLevel(ref.getFoodLevel());
                        player.setSaturation(ref.getSaturation());
                    }
                } else {
                    if (plugin.getConfigManager().isEnabledHealth()) player.setHealth(maxHp);
                    if (plugin.getConfigManager().isEnabledFood()) {
                        player.setFoodLevel(20);
                        player.setSaturation(5.0f);
                    }
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        double maxHp = plugin.getConfigManager().getMaxHealth();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            plugin.getCompatibilityManager().setMaxHealth(player, maxHp);
            plugin.getCompatibilityManager().applyHealthScaling(player, maxHp);

            if (plugin.getConfigManager().isRespawnSpectator()) {
                player.setGameMode(GameMode.SPECTATOR);
            } else {
                if (plugin.getConfigManager().isEnabledHealth()) player.setHealth(maxHp);
                if (plugin.getConfigManager().isEnabledFood()) {
                    player.setFoodLevel(20);
                    player.setSaturation(5.0f);
                }
            }
        }, 1L);
    }

    private boolean shouldNewPlayerBeSpectator(Player newPlayer) {
        if (!plugin.getConfigManager().isRespawnSpectator()) return false;

        long alivePlayersCount = 0;
        long totalOtherPlayers = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(newPlayer.getUniqueId())) continue;
            if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;

            totalOtherPlayers++;
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                alivePlayersCount++;
            }
        }

        return totalOtherPlayers > 0 && alivePlayersCount == 0;
    }

    private Player findReferencePlayer(Player exclude) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(exclude.getUniqueId())) continue;
            if (p.isDead()) continue;
            if (plugin.getCompatibilityManager().isPlayerInIgnoredWorld(p)) continue;
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                return p;
            }
        }
        return null;
    }

    public void clearAllSuppression() {
        suppressHealth.clear();
        suppressFood.clear();
        suppressEffects.clear();
    }
}
