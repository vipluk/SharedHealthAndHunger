package org.example.sharedhealthandhunger.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.example.sharedhealthandhunger.Main;
import org.example.sharedhealthandhunger.compat.GeyserHook;
import org.example.sharedhealthandhunger.compat.VersionAdapter;

/**
 * Obsługuje dołączanie graczy, zmiany trybów gry, przejścia między światami (np. wyjście z Limbo w WorldReset)
 * oraz automatyczne skalowanie serc dla graczy Bedrock (Geyser).
 */
public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        double maxHealth = plugin.getConfigManager().getMaxHealth();

        if (plugin.getConfigManager().isEnabledHealth()) {
            VersionAdapter.setMaxHealth(player, maxHealth);
            GeyserHook.applyHealthScaling(player, plugin.getConfigManager().isGeyserHealthScale(), maxHealth);
        }

        // Jeśli gracz dołącza do ignorowanego świata (np. Limbo), nie ingerujemy
        if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(player)) {
            return;
        }

        if (shouldNewPlayerBeSpectator(player)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(plugin.getLanguageManager().getMsg("spectator-join"));
            return;
        }

        // Pobierz gracza referencyjnego z aktywnego świata gry (nie z Limbo)
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
        boolean fromIgnored = plugin.getWorldResetHook().isIgnoredWorld(event.getFrom());
        boolean toIgnored = plugin.getWorldResetHook().isPlayerInIgnoredWorld(player);

        // Gracz opuścił Limbo i wszedł do nowego świata gry (np. po resecie WorldReset)
        if (fromIgnored && !toIgnored) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                double maxHp = plugin.getConfigManager().getMaxHealth();
                VersionAdapter.setMaxHealth(player, maxHp);
                GeyserHook.applyHealthScaling(player, plugin.getConfigManager().isGeyserHealthScale(), maxHp);

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
            if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(player)) return;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                double maxHp = plugin.getConfigManager().getMaxHealth();
                VersionAdapter.setMaxHealth(player, maxHp);
                GeyserHook.applyHealthScaling(player, plugin.getConfigManager().isGeyserHealthScale(), maxHp);

                // Zamiast darmowego pełnego uleczenia, synchronizuj z obecnym stanem zespołu
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

            VersionAdapter.setMaxHealth(player, maxHp);
            GeyserHook.applyHealthScaling(player, plugin.getConfigManager().isGeyserHealthScale(), maxHp);

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
            if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(p)) continue;

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
            if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(p)) continue;
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                return p;
            }
        }
        return null;
    }
}
