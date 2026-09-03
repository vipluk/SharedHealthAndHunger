package org.example.sharedhealthandhunger.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.example.sharedhealthandhunger.Main;
import org.example.sharedhealthandhunger.compat.GeyserHook;
import org.example.sharedhealthandhunger.compat.VersionAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Obsługa komendy /sharedhealth (alias /sh) wraz z kompletnym TabCompleterem,
 * granularnymi uprawnieniami oraz nowymi komendami /sh reload i /sh sync.
 */
public class SharedHealthCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public SharedHealthCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String mainArg = args[0].toLowerCase();

        switch (mainArg) {
            case "respawn" -> {
                if (!hasPerm(sender, "sharedhealth.respawn")) {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("no-permission")
                            .replace("{permission}", "sharedhealth.respawn"));
                    return true;
                }
                plugin.resetGame();
            }
            case "reload" -> {
                if (!hasPerm(sender, "sharedhealth.reload")) {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("no-permission")
                            .replace("{permission}", "sharedhealth.reload"));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(plugin.getLanguageManager().getMsg("reload-success"));
            }
            case "sync" -> {
                if (!hasPerm(sender, "sharedhealth.sync")) {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("no-permission")
                            .replace("{permission}", "sharedhealth.sync"));
                    return true;
                }
                plugin.syncAllTeamStats();
                double hp = plugin.getConfigManager().getMaxHealth();
                sender.sendMessage(plugin.getLanguageManager().getMsg("sync-success")
                        .replace("{health}", String.format("%.1f", hp))
                        .replace("{food}", "20"));
            }
            case "language" -> {
                if (!hasPerm(sender, "sharedhealth.language")) {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("no-permission")
                            .replace("{permission}", "sharedhealth.language"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("usage-language"));
                    return true;
                }
                String newLang = args[1].toLowerCase();
                if (newLang.equals("en") || newLang.equals("pl")) {
                    plugin.getLanguageManager().setLanguage(newLang);
                    sender.sendMessage(plugin.getLanguageManager().getMsg("language-changed")
                            .replace("{lang}", newLang));
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("language-invalid"));
                }
            }
            case "toggle" -> {
                if (args.length < 2) {
                    sendHelp(sender);
                    return true;
                }
                String subArg = args[1].toLowerCase();
                String permRequired = "sharedhealth.toggle." + subArg;

                if (!hasPerm(sender, permRequired) && !hasPerm(sender, "sharedhealth.toggle.*")) {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("no-permission")
                            .replace("{permission}", permRequired));
                    return true;
                }

                boolean state;
                switch (subArg) {
                    case "health" -> {
                        state = !plugin.getConfigManager().isEnabledHealth();
                        plugin.getConfigManager().setEnabledHealth(state);
                        if (state) plugin.syncAllToLowestHealth();
                    }
                    case "food" -> {
                        state = !plugin.getConfigManager().isEnabledFood();
                        plugin.getConfigManager().setEnabledFood(state);
                        if (state) plugin.syncAllToLowestFood();
                    }
                    case "effects" -> {
                        state = !plugin.getConfigManager().isEnabledEffects();
                        plugin.getConfigManager().setEnabledEffects(state);
                    }
                    case "actionbar" -> {
                        state = !plugin.getConfigManager().isEnabledActionBar();
                        plugin.getConfigManager().setEnabledActionBar(state);
                    }
                    case "attackerfeeldamage" -> {
                        state = !plugin.getConfigManager().isIgnoreAttacker();
                        plugin.getConfigManager().setIgnoreAttacker(state);
                    }
                    case "respawn" -> {
                        state = !plugin.getConfigManager().isRespawnSpectator();
                        plugin.getConfigManager().setRespawnSpectator(state);
                    }
                    default -> {
                        sendHelp(sender);
                        return true;
                    }
                }

                String key = state ? "toggle-on" : "toggle-off";
                sender.sendMessage(plugin.getLanguageManager().getMsg(key).replace("{option}", subArg));
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("usage-set"));
                    return true;
                }
                String subArg = args[1].toLowerCase();
                String permRequired = "sharedhealth.set." + subArg;

                if (!hasPerm(sender, permRequired) && !hasPerm(sender, "sharedhealth.set.*")) {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("no-permission")
                            .replace("{permission}", permRequired));
                    return true;
                }

                try {
                    double value = Double.parseDouble(args[2]);

                    if (subArg.equals("maxhealth")) {
                        plugin.getConfigManager().setMaxHealth(value);
                        plugin.applyMaxValuesToOnlinePlayers();
                        sender.sendMessage(plugin.getLanguageManager().getMsg("set-value")
                                .replace("{option}", "Max Health")
                                .replace("{value}", String.valueOf(value)));
                    } else if (subArg.equals("hungermult")) {
                        plugin.getConfigManager().setHungerLossMultiplier(value);
                        sender.sendMessage(plugin.getLanguageManager().getMsg("set-value")
                                .replace("{option}", "Hunger Multiplier")
                                .replace("{value}", String.valueOf(value)));
                    } else {
                        sendHelp(sender);
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getLanguageManager().getMsg("invalid-number"));
                }
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMsg("prefix") + "Commands:");
        if (hasPerm(sender, "sharedhealth.respawn")) sender.sendMessage(plugin.getLanguageManager().getMsg("usage-respawn"));
        if (hasPerm(sender, "sharedhealth.reload")) sender.sendMessage(plugin.getLanguageManager().getMsg("usage-reload"));
        if (hasPerm(sender, "sharedhealth.sync")) sender.sendMessage(plugin.getLanguageManager().getMsg("usage-sync"));
        if (hasPerm(sender, "sharedhealth.language")) sender.sendMessage(plugin.getLanguageManager().getMsg("usage-language"));
        if (hasPerm(sender, "sharedhealth.toggle.*") || hasPerm(sender, "sharedhealth.toggle.health"))
            sender.sendMessage(plugin.getLanguageManager().getMsg("usage-toggle"));
        if (hasPerm(sender, "sharedhealth.set.*") || hasPerm(sender, "sharedhealth.set.maxhealth"))
            sender.sendMessage(plugin.getLanguageManager().getMsg("usage-set"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (hasPerm(sender, "sharedhealth.toggle")) completions.add("toggle");
            if (hasPerm(sender, "sharedhealth.set")) completions.add("set");
            if (hasPerm(sender, "sharedhealth.respawn")) completions.add("respawn");
            if (hasPerm(sender, "sharedhealth.reload")) completions.add("reload");
            if (hasPerm(sender, "sharedhealth.sync")) completions.add("sync");
            if (hasPerm(sender, "sharedhealth.language")) completions.add("language");
            return filterPrefix(completions, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("toggle")) {
                if (hasPerm(sender, "sharedhealth.toggle.health")) completions.add("health");
                if (hasPerm(sender, "sharedhealth.toggle.food")) completions.add("food");
                if (hasPerm(sender, "sharedhealth.toggle.effects")) completions.add("effects");
                if (hasPerm(sender, "sharedhealth.toggle.actionbar")) completions.add("actionbar");
                if (hasPerm(sender, "sharedhealth.toggle.attackerfeeldamage")) completions.add("attackerfeeldamage");
                if (hasPerm(sender, "sharedhealth.toggle.respawn")) completions.add("respawn");
                return filterPrefix(completions, args[1]);
            }
            if (args[0].equalsIgnoreCase("set")) {
                if (hasPerm(sender, "sharedhealth.set.maxhealth")) completions.add("maxhealth");
                if (hasPerm(sender, "sharedhealth.set.hungermult")) completions.add("hungermult");
                return filterPrefix(completions, args[1]);
            }
            if (args[0].equalsIgnoreCase("language") && hasPerm(sender, "sharedhealth.language")) {
                completions.add("en");
                completions.add("pl");
                return filterPrefix(completions, args[1]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }

    private boolean hasPerm(CommandSender sender, String permBase) {
        return sender.hasPermission(permBase) || sender.hasPermission(permBase + ".*") || sender.hasPermission("sharedhealth.*");
    }
}
