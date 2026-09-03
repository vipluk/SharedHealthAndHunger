package org.example.sharedhealthandhunger;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

@SuppressWarnings("deprecation")
public class Main extends JavaPlugin implements Listener, TabCompleter {

    private final Set<UUID> suppressHealth = new HashSet<>();
    private final Set<UUID> suppressFood = new HashSet<>();
    private final Set<UUID> suppressEffects = new HashSet<>();

    private final Map<String, Long> lastSyncAt = new HashMap<>();
    private final Map<UUID, Float> lastExhaustionMap = new HashMap<>();

    private FileConfiguration langConfig;

    private boolean enabledHealth;
    private boolean enabledFood;
    private boolean enabledEffects;
    private boolean enabledActionBar;
    private boolean ignoreAttacker;

    private long cooldownMs;
    private double maxHealthConfig;
    private boolean respawnSpectator;
    private double hungerLossMultiplier;

    private static final double SYNC_DAMAGE_TICK = 0.0001;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        saveLanguageFiles();
        loadLanguage();

        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("sharedhealth")).setTabCompleter(this);

        startHungerTask();

        getLogger().info("SharedHealthAndHunger v6.4 (Granular Permissions) enabled.");
        applyMaxValuesToOnlinePlayers();
    }

    @Override
    public void onDisable() {
        getLogger().info("SharedHealthAndHunger disabled.");
    }

    private void loadConfigValues() {
        FileConfiguration cfg = getConfig();
        cfg.addDefault("language", "en");
        cfg.addDefault("enabled-health", true);
        cfg.addDefault("enabled-food", true);
        cfg.addDefault("enabled-effects", true);
        cfg.addDefault("enabled-actionbar", true);
        cfg.addDefault("ignore-attacker", false);
        cfg.addDefault("cooldown-ms", 100);
        cfg.addDefault("max-health", 20.0);
        cfg.addDefault("respawn-spectator", true);
        cfg.addDefault("hunger-loss-multiplier", 1.0);
        cfg.options().copyDefaults(true);
        saveConfig();

        enabledHealth = cfg.getBoolean("enabled-health");
        enabledFood = cfg.getBoolean("enabled-food");
        enabledEffects = cfg.getBoolean("enabled-effects");
        enabledActionBar = cfg.getBoolean("enabled-actionbar");
        ignoreAttacker = cfg.getBoolean("ignore-attacker");
        cooldownMs = cfg.getLong("cooldown-ms");
        maxHealthConfig = Math.max(1.0, Math.min(1024.0, cfg.getDouble("max-health")));
        respawnSpectator = cfg.getBoolean("respawn-spectator");
        hungerLossMultiplier = cfg.getDouble("hunger-loss-multiplier");
    }

    // --- SYSTEM JĘZYKOWY ---

    private void saveLanguageFiles() {
        if (!new File(getDataFolder(), "messages_en.yml").exists()) {
            saveResource("messages_en.yml", false);
        }
        if (!new File(getDataFolder(), "messages_pl.yml").exists()) {
            saveResource("messages_pl.yml", false);
        }
    }

    private void loadLanguage() {
        String lang = getConfig().getString("language", "en");
        String fileName = "messages_" + lang + ".yml";
        File langFile = new File(getDataFolder(), fileName);

        if (!langFile.exists()) {
            getLogger().warning("Language file " + fileName + " not found! Falling back to English.");
            langFile = new File(getDataFolder(), "messages_en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private String getMsg(String key) {
        if (langConfig == null) return key;
        String prefix = langConfig.getString("prefix", "");
        String msg = langConfig.getString(key, key);
        return (prefix + msg).replace("&", "§");
    }

    private String getRawMsg(String key) {
        if (langConfig == null) return key;
        return langConfig.getString(key, key).replace("&", "§");
    }

    // --- LOGIKA GRY ---

    private void applyMaxValuesToOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setMaxHealth(maxHealthConfig);
            p.setHealth(maxHealthConfig);
        }
    }

    private void startHungerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabledFood || hungerLossMultiplier == 1.0) return;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) continue;
                    if (p.isDead()) continue;

                    UUID id = p.getUniqueId();
                    float currentExhaustion = p.getExhaustion();
                    float lastExhaustion = lastExhaustionMap.getOrDefault(id, currentExhaustion);

                    float diff = currentExhaustion - lastExhaustion;

                    if (diff > 0) {
                        float extraExhaustion = (float) (diff * (hungerLossMultiplier - 1.0));
                        float newTotal = currentExhaustion + extraExhaustion;
                        if (newTotal > 40.0f) newTotal = 40.0f;

                        p.setExhaustion(newTotal);
                        lastExhaustionMap.put(id, newTotal);
                    } else {
                        lastExhaustionMap.put(id, currentExhaustion);
                    }
                }
            }
        }.runTaskTimer(this, 10L, 10L);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.SURVIVAL) {
            Player p = event.getPlayer();
            if (enabledHealth) {
                p.setMaxHealth(maxHealthConfig);
                p.setHealth(maxHealthConfig);
            }
            if (enabledFood) {
                p.setFoodLevel(20);
                p.setSaturation(5.0f);
                p.setExhaustion(0f);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!enabledHealth) return;
        if (!(event.getEntity() instanceof Player damagedPlayer)) return;

        if (damagedPlayer.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }

        if (suppressHealth.contains(damagedPlayer.getUniqueId())) return;

        double finalDamage = event.getFinalDamage();
        double healthAfter = Math.max(0.0, damagedPlayer.getHealth() - finalDamage);

        if (enabledActionBar && finalDamage > 0) {
            sendSharedActionBar(damagedPlayer, finalDamage);
        }

        if (healthAfter > 0) {
            syncHealthToOthers(damagedPlayer, healthAfter);
        }

        if (finalDamage > 0 && healthAfter > 0) {
            applySharedPhysicalEffects(damagedPlayer, event);
        }

        if (healthAfter <= 0) {
            event.setCancelled(true);
            damagedPlayer.setMaxHealth(maxHealthConfig);
            damagedPlayer.setHealth(maxHealthConfig);
            handleDeathLogic(damagedPlayer);
        }
    }

    private void sendSharedActionBar(Player victim, double damage) {
        String damageStr = String.format("%.1f", damage);
        String msg = getRawMsg("actionbar-damage")
                .replace("{player}", victim.getName())
                .replace("{damage}", damageStr);

        TextComponent textComponent = new TextComponent(msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, textComponent);
        }
    }

    @EventHandler
    public void onRegain(EntityRegainHealthEvent event) {
        if (!enabledHealth) return;
        if (!(event.getEntity() instanceof Player source)) return;
        if (suppressHealth.contains(source.getUniqueId())) return;

        double healthAfter = Math.min(maxHealthConfig, source.getHealth() + event.getAmount());
        syncHealthToOthers(source, healthAfter);
    }

    private void syncHealthToOthers(Player source, double targetHealth) {
        if (Bukkit.getOnlinePlayers().size() <= 1) return;
        if (isCooldownActive("health")) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(source.getUniqueId())) continue;
            if (p.isDead()) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;

            suppressHealth.add(p.getUniqueId());
            try {
                p.setMaxHealth(maxHealthConfig);
                p.setHealth(Math.max(0.0, Math.min(targetHealth, maxHealthConfig)));
            } finally {
                suppressHealth.remove(p.getUniqueId());
            }
        }
    }

    private void applySharedPhysicalEffects(Player damagedPlayer, EntityDamageEvent event) {
        Player attacker = null;
        if (event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof Player p) {
            attacker = p;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(damagedPlayer)) continue;
            if (ignoreAttacker && attacker != null && p.equals(attacker)) continue;
            if (p.isDead()) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;

            suppressHealth.add(p.getUniqueId());
            try {
                p.damage(SYNC_DAMAGE_TICK);
                Vector sharedKb = p.getLocation().getDirection().multiply(-0.2).setY(0.2);
                p.setVelocity(sharedKb);
            } catch (Exception ignored) {
            } finally {
                suppressHealth.remove(p.getUniqueId());
            }
        }
    }

    private void handleDeathLogic(Player dyingPlayer) {
        if (respawnSpectator) {
            String msg = getMsg("player-died").replace("{player}", dyingPlayer.getName());
            Bukkit.broadcastMessage(msg);

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setGameMode(GameMode.SPECTATOR);
            }
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setHealth(0);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!enabledHealth) return;
        handleDeathLogic(event.getEntity());
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!enabledFood) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (suppressFood.contains(player.getUniqueId())) return;

        syncFoodToOthers(player, event.getFoodLevel());
    }

    private void syncFoodToOthers(Player source, int targetFood) {
        if (Bukkit.getOnlinePlayers().size() <= 1) return;
        if (isCooldownActive("food")) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(source.getUniqueId())) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;

            suppressFood.add(p.getUniqueId());
            try {
                p.setFoodLevel(targetFood);
                p.setSaturation(source.getSaturation());
                p.setExhaustion(source.getExhaustion());
                lastExhaustionMap.put(p.getUniqueId(), source.getExhaustion());
            } finally {
                suppressFood.remove(p.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!enabledEffects) return;
        if (!(event.getEntity() instanceof Player source)) return;
        if (suppressEffects.contains(source.getUniqueId())) return;

        EntityPotionEffectEvent.Action action = event.getAction();
        PotionEffect newEffect = event.getNewEffect();
        PotionEffect oldEffect = event.getOldEffect();

        Bukkit.getScheduler().runTask(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(source)) continue;
                if (p.isDead()) continue;
                if (p.getGameMode() == GameMode.SPECTATOR) continue;

                suppressEffects.add(p.getUniqueId());
                try {
                    if (action == EntityPotionEffectEvent.Action.ADDED || action == EntityPotionEffectEvent.Action.CHANGED) {
                        if (newEffect != null) {
                            p.addPotionEffect(newEffect);
                        }
                    } else if (action == EntityPotionEffectEvent.Action.REMOVED || action == EntityPotionEffectEvent.Action.CLEARED) {
                        if (oldEffect != null) {
                            p.removePotionEffect(oldEffect.getType());
                        }
                    }
                } finally {
                    suppressEffects.remove(p.getUniqueId());
                }
            }
        });
    }

    private boolean shouldNewPlayerBeSpectator(Player newPlayer) {
        if (!respawnSpectator) return false;
        long alivePlayersCount = 0;
        long totalOtherPlayers = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(newPlayer.getUniqueId())) continue;
            totalOtherPlayers++;
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                alivePlayersCount++;
            }
        }
        return totalOtherPlayers > 0 && alivePlayersCount == 0;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player newPlayer = event.getPlayer();

        if (enabledHealth) {
            newPlayer.setMaxHealth(maxHealthConfig);
        }

        if (shouldNewPlayerBeSpectator(newPlayer)) {
            newPlayer.setGameMode(GameMode.SPECTATOR);
            newPlayer.sendMessage(getMsg("spectator-join"));
            return;
        } else {
            newPlayer.setGameMode(GameMode.SURVIVAL);
        }

        Player referencePlayer = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(newPlayer.getUniqueId())) continue;
            if (p.isDead()) continue;
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                referencePlayer = p;
                break;
            }
        }

        if (referencePlayer != null) {
            if (enabledHealth) newPlayer.setHealth(referencePlayer.getHealth());
            if (enabledFood) {
                newPlayer.setFoodLevel(referencePlayer.getFoodLevel());
                newPlayer.setSaturation(referencePlayer.getSaturation());
                newPlayer.setExhaustion(referencePlayer.getExhaustion());
            }
            if (enabledEffects) {
                for (PotionEffect effect : referencePlayer.getActivePotionEffects()) {
                    newPlayer.addPotionEffect(effect);
                }
            }
        } else {
            if (enabledHealth) newPlayer.setHealth(maxHealthConfig);
            if (enabledFood) {
                newPlayer.setFoodLevel(20);
                newPlayer.setSaturation(5.0f);
                newPlayer.setExhaustion(0f);
            }
        }
        lastExhaustionMap.put(newPlayer.getUniqueId(), newPlayer.getExhaustion());
    }

    private boolean isCooldownActive(String key) {
        long now = System.currentTimeMillis();
        long last = lastSyncAt.getOrDefault(key, 0L);
        if (now - last > cooldownMs) {
            lastSyncAt.put(key, now);
            return false;
        }
        return true;
    }

    private void syncAllToLowestHealth() {
        double minHealth = maxHealthConfig;
        boolean foundAny = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
            if (p.getHealth() < minHealth) {
                minHealth = p.getHealth();
            }
            foundAny = true;
        }
        if (foundAny) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
                p.setHealth(minHealth);
            }
            Bukkit.broadcastMessage(getMsg("sync-health").replace("{value}", String.valueOf((int)minHealth)));
        }
    }

    private void syncAllToLowestFood() {
        int minFood = 20;
        boolean foundAny = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
            if (p.getFoodLevel() < minFood) {
                minFood = p.getFoodLevel();
            }
            foundAny = true;
        }
        if (foundAny) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
                p.setFoodLevel(minFood);
            }
            Bukkit.broadcastMessage(getMsg("sync-food").replace("{value}", String.valueOf(minFood)));
        }
    }

    // --- KOMENDY (Z UPRAWNIENIAMI SZCZEGÓŁOWYMI) ---

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String mainArg = args[0].toLowerCase();

        switch (mainArg) {
            case "respawn" -> {
                if (!sender.hasPermission("sharedhealth.respawn")) {
                    sender.sendMessage(getMsg("no-permission").replace("{permission}", "sharedhealth.respawn"));
                    return true;
                }
                resetGame();
            }
            case "language" -> {
                if (!sender.hasPermission("sharedhealth.language")) {
                    sender.sendMessage(getMsg("no-permission").replace("{permission}", "sharedhealth.language"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(getMsg("usage-language"));
                    return true;
                }
                String newLang = args[1].toLowerCase();
                if (newLang.equals("en") || newLang.equals("pl")) {
                    getConfig().set("language", newLang);
                    saveConfig();
                    loadLanguage();
                    sender.sendMessage(getMsg("language-changed").replace("{lang}", newLang));
                } else {
                    sender.sendMessage(getMsg("language-invalid"));
                }
            }
            case "toggle" -> {
                if (args.length < 2) {
                    sendHelp(sender);
                    return true;
                }
                String subArg = args[1].toLowerCase();

                // SPRAWDZANIE SZCZEGÓŁOWYCH UPRAWNIEŃ
                String permRequired = "sharedhealth.toggle." + subArg;
                if (!sender.hasPermission(permRequired) && !sender.hasPermission("sharedhealth.toggle.*")) {
                    sender.sendMessage(getMsg("no-permission").replace("{permission}", permRequired));
                    return true;
                }

                boolean state = false;
                String optName = subArg;

                switch (subArg) {
                    case "health" -> {
                        enabledHealth = !enabledHealth;
                        getConfig().set("enabled-health", enabledHealth);
                        state = enabledHealth;
                        if (enabledHealth) syncAllToLowestHealth();
                    }
                    case "food" -> {
                        enabledFood = !enabledFood;
                        getConfig().set("enabled-food", enabledFood);
                        state = enabledFood;
                        if (enabledFood) syncAllToLowestFood();
                    }
                    case "effects" -> {
                        enabledEffects = !enabledEffects;
                        getConfig().set("enabled-effects", enabledEffects);
                        state = enabledEffects;
                    }
                    case "actionbar" -> {
                        enabledActionBar = !enabledActionBar;
                        getConfig().set("enabled-actionbar", enabledActionBar);
                        state = enabledActionBar;
                    }
                    case "attackerfeeldamage" -> {
                        ignoreAttacker = !ignoreAttacker;
                        getConfig().set("ignore-attacker", ignoreAttacker);
                        state = ignoreAttacker;
                    }
                    case "respawn" -> {
                        respawnSpectator = !respawnSpectator;
                        getConfig().set("respawn-spectator", respawnSpectator);
                        state = respawnSpectator;
                    }
                    default -> {
                        sendHelp(sender);
                        return true;
                    }
                }
                saveConfig();
                String key = state ? "toggle-on" : "toggle-off";
                sender.sendMessage(getMsg(key).replace("{option}", optName));
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(getMsg("usage-set"));
                    return true;
                }
                String subArg = args[1].toLowerCase();

                // SPRAWDZANIE SZCZEGÓŁOWYCH UPRAWNIEŃ
                String permRequired = "sharedhealth.set." + subArg;
                if (!sender.hasPermission(permRequired) && !sender.hasPermission("sharedhealth.set.*")) {
                    sender.sendMessage(getMsg("no-permission").replace("{permission}", permRequired));
                    return true;
                }

                try {
                    double value = Double.parseDouble(args[2]);

                    if (subArg.equals("maxhealth")) {
                        maxHealthConfig = value;
                        getConfig().set("max-health", maxHealthConfig);
                        applyMaxValuesToOnlinePlayers();
                        sender.sendMessage(getMsg("set-value").replace("{option}", "Max Health").replace("{value}", String.valueOf(value)));
                    } else if (subArg.equals("hungermult")) {
                        hungerLossMultiplier = value;
                        getConfig().set("hunger-loss-multiplier", hungerLossMultiplier);
                        sender.sendMessage(getMsg("set-value").replace("{option}", "Hunger Multiplier").replace("{value}", String.valueOf(value)));
                    } else {
                        sendHelp(sender);
                    }
                    saveConfig();
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMsg("invalid-number"));
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void resetGame() {
        suppressHealth.clear();
        suppressFood.clear();
        suppressEffects.clear();
        lastSyncAt.clear();
        lastExhaustionMap.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setGameMode(GameMode.SURVIVAL);
            p.setMaxHealth(maxHealthConfig);
            p.setHealth(maxHealthConfig);
            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            p.setExhaustion(0f);
            p.setFireTicks(0);
            p.setFallDistance(0);
            p.setVelocity(new Vector(0, 0, 0));

            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }

            Location spawn = p.getWorld().getSpawnLocation();
            p.teleport(spawn);
            lastExhaustionMap.put(p.getUniqueId(), 0f);
        }
        Bukkit.broadcastMessage(getMsg("game-reset"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(getMsg("prefix") + "Commands:");
        if (sender.hasPermission("sharedhealth.respawn")) sender.sendMessage(getMsg("usage-respawn"));
        if (sender.hasPermission("sharedhealth.language")) sender.sendMessage(getMsg("usage-language"));
        if (sender.hasPermission("sharedhealth.toggle.*") || sender.hasPermission("sharedhealth.toggle.health"))
            sender.sendMessage(getMsg("usage-toggle"));
        if (sender.hasPermission("sharedhealth.set.*") || sender.hasPermission("sharedhealth.set.maxhealth"))
            sender.sendMessage(getMsg("usage-set"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Wyświetlamy komendę tylko jeśli gracz ma uprawnienie do niej (lub do gwiazdki)
            if (hasPerm(sender, "sharedhealth.toggle")) completions.add("toggle");
            if (hasPerm(sender, "sharedhealth.set")) completions.add("set");
            if (sender.hasPermission("sharedhealth.respawn") || sender.hasPermission("sharedhealth.*")) completions.add("respawn");
            if (sender.hasPermission("sharedhealth.language") || sender.hasPermission("sharedhealth.*")) completions.add("language");
            return completions;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("toggle")) {
                // Podpowiadamy tylko opcje, do których gracz ma dostęp
                if (hasPerm(sender, "sharedhealth.toggle.health")) completions.add("health");
                if (hasPerm(sender, "sharedhealth.toggle.food")) completions.add("food");
                if (hasPerm(sender, "sharedhealth.toggle.effects")) completions.add("effects");
                if (hasPerm(sender, "sharedhealth.toggle.actionbar")) completions.add("actionbar");
                if (hasPerm(sender, "sharedhealth.toggle.attackerfeeldamage")) completions.add("attackerfeeldamage");
                if (hasPerm(sender, "sharedhealth.toggle.respawn")) completions.add("respawn");
                return completions;
            }
            if (args[0].equalsIgnoreCase("set")) {
                if (hasPerm(sender, "sharedhealth.set.maxhealth")) completions.add("maxhealth");
                if (hasPerm(sender, "sharedhealth.set.hungermult")) completions.add("hungermult");
                return completions;
            }
            if (args[0].equalsIgnoreCase("language") && (sender.hasPermission("sharedhealth.language") || sender.hasPermission("sharedhealth.*"))) {
                return List.of("en", "pl");
            }
        }
        return Collections.emptyList();
    }

    // Metoda pomocnicza do sprawdzania uprawnień (obsługuje gwiazdki)
    private boolean hasPerm(CommandSender sender, String permBase) {
        return sender.hasPermission(permBase) || sender.hasPermission(permBase + ".*") || sender.hasPermission("sharedhealth.*");
    }
}