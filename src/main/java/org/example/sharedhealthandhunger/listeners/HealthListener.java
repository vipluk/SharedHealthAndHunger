package org.example.sharedhealthandhunger.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;
import org.example.sharedhealthandhunger.Main;
import org.example.sharedhealthandhunger.compat.VersionAdapter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Odpowiada za precyzyjną synchronizację zdrowia, prawidłową obsługę Totemów Nieśmiertelności,
 * natywne animacje bólu (hurt animation) oraz koordynację zgonów z pluginem WorldReset.
 */
public class HealthListener implements Listener {

    private final Main plugin;
    private final Set<UUID> suppressHealth = new HashSet<>();
    private long lastActionBarTime = 0L;
    private boolean isHandlingTeamDeath = false;

    public HealthListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfigManager().isEnabledHealth()) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        // Ignoruj widzów i graczy w poczekalni Limbo
        if (victim.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(victim)) return;
        if (suppressHealth.contains(victim.getUniqueId())) return;

        Player attacker = null;
        if (event instanceof EntityDamageByEntityEvent edbe && edbe.getDamager() instanceof Player p) {
            attacker = p;
        }

        double finalDamage = event.getFinalDamage();
        double currentHealth = victim.getHealth();
        double healthAfter = currentHealth - finalDamage;

        // Wyświetlanie Action Bar
        if (plugin.getConfigManager().isEnabledActionBar() && finalDamage > 0.05) {
            long now = System.currentTimeMillis();
            if (now - lastActionBarTime >= plugin.getConfigManager().getCooldownMs()) {
                lastActionBarTime = now;
                sendSharedActionBar(victim, finalDamage, Math.max(0.0, healthAfter));
            }
        }

        // Sprawdź, czy obrażenia są śmiertelne
        if (healthAfter <= 0.0) {
            // Sprawdź, czy gracz posiada Totem Nieśmiertelności
            boolean hasTotem = (victim.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                    || victim.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING);

            if (hasTotem) {
                // Pozwól silnikowi gry zużyć totem!
                // W następnym ticku zsynchronizuj ocalone zdrowie i absorpcję
                final Player fAttacker = attacker;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (victim.isOnline() && !victim.isDead()) {
                        syncHealthToOthers(victim, victim.getHealth(), fAttacker);
                    }
                }, 1L);
                return;
            }

            // Gracz nie ma totemu - NIE anulujemy eventu!
            // Pozwalamy, aby PlayerDeathEvent wykonał się normalnie.
            // Dzięki temu:
            // 1. WorldReset poprawnie wykryje śmierć i zresetuje świat (/wr death).
            // 2. Statystyki zgonów i tablice wyników zostaną zaktualizowane.
            // 3. Po śmierci zadziała handleDeathLogic bez fałszywych pętli rekurencyjnych.
            return;
        }

        // Obrażenia nie są śmiertelne - synchronizujemy zdrowie zespołu
        syncHealthToOthers(victim, healthAfter, attacker);

        // Efekty fizyczne u sojuszników
        if (finalDamage > 0.05) {
            applySharedPhysicalEffects(victim, attacker);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (!plugin.getConfigManager().isEnabledHealth()) return;
        if (!(event.getEntity() instanceof Player source)) return;
        if (suppressHealth.contains(source.getUniqueId())) return;
        if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(source)) return;

        double maxHealth = plugin.getConfigManager().getMaxHealth();
        double healthAfter = Math.min(maxHealth, source.getHealth() + event.getAmount());
        syncHealthToOthers(source, healthAfter, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isEnabledHealth()) return;
        if (isHandlingTeamDeath) return;

        Player dyingPlayer = event.getEntity();
        if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(dyingPlayer)) return;

        handleDeathLogic(dyingPlayer);
    }

    private void handleDeathLogic(Player dyingPlayer) {
        if (isHandlingTeamDeath) return;
        isHandlingTeamDeath = true;

        try {
            if (plugin.getConfigManager().isRespawnSpectator()) {
                String msg = plugin.getLanguageManager().getMsg("player-died")
                        .replace("{player}", dyingPlayer.getName());
                Bukkit.broadcastMessage(msg);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(p)) continue;
                    if (!p.equals(dyingPlayer)) {
                        p.setGameMode(GameMode.SPECTATOR);
                    }
                }
            } else {
                // Zabij pozostałych graczy bez tworzenia rekurencji
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.equals(dyingPlayer)) continue;
                    if (p.isDead()) continue;
                    if (p.getGameMode() == GameMode.SPECTATOR) continue;
                    if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(p)) continue;

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
            if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(p)) continue;
            if (plugin.getConfigManager().isIgnoreAttacker() && attacker != null && p.equals(attacker)) continue;

            suppressHealth.add(p.getUniqueId());
            try {
                VersionAdapter.setMaxHealth(p, maxHealth);
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
            if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(p)) continue;
            if (plugin.getConfigManager().isIgnoreAttacker() && attacker != null && p.equals(attacker)) continue;

            // 1. Czerwony błysk i wstrząs kamery przez natywne API (bez niszczenia pancerza!)
            if (hurtAnim) {
                VersionAdapter.playHurtAnimation(p, p.getLocation().getYaw());
            }

            // 2. Lekki, bezpieczny odrzut (jeśli włączony)
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
            if (plugin.getWorldResetHook().isPlayerInIgnoredWorld(p)) continue;
            VersionAdapter.sendActionBar(p, msg);
        }
    }

    public void clearSuppression() {
        suppressHealth.clear();
    }
}
