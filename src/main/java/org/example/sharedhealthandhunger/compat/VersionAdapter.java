package org.example.sharedhealthandhunger.compat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * Adapter zapewniający bezbłędną kompatybilność między wersjami Minecrafta od 1.21 aż po 26.2.
 * Dynamicznie obsługuje zmiany nazw atrybutów (GENERIC_MAX_HEALTH vs MAX_HEALTH),
 * natywne animacje bólu (playHurtAnimation) oraz Adventure API dla Action Bar.
 */
public final class VersionAdapter {

    private static final Attribute MAX_HEALTH_ATTR = resolveMaxHealthAttribute();

    private VersionAdapter() {}

    private static Attribute resolveMaxHealthAttribute() {
        // 1. Sprawdzenie rejestru 1.21.2+ / 26.x (minecraft:max_health)
        try {
            Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("max_health"));
            if (attr != null) return attr;
        } catch (Throwable ignored) {}

        // 2. Sprawdzenie rejestru 1.21.0 - 1.21.1 (minecraft:generic.max_health)
        try {
            Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.max_health"));
            if (attr != null) return attr;
        } catch (Throwable ignored) {}

        // 3. Sprawdzenie stałej Enum w 1.21.2+
        try {
            return Attribute.valueOf("MAX_HEALTH");
        } catch (Throwable ignored) {}

        // 4. Sprawdzenie stałej Enum w 1.21.0 - 1.21.1
        try {
            return Attribute.valueOf("GENERIC_MAX_HEALTH");
        } catch (Throwable ignored) {}

        return null;
    }

    public static double getMaxHealth(Player player) {
        if (MAX_HEALTH_ATTR != null) {
            AttributeInstance inst = player.getAttribute(MAX_HEALTH_ATTR);
            if (inst != null) return inst.getValue();
        }
        return player.getMaxHealth();
    }

    public static void setMaxHealth(Player player, double value) {
        if (MAX_HEALTH_ATTR != null) {
            AttributeInstance inst = player.getAttribute(MAX_HEALTH_ATTR);
            if (inst != null) {
                inst.setBaseValue(value);
                return;
            }
        }
        player.setMaxHealth(value);
    }

    /**
     * Odtwarza natywną animację zranienia (czerwony ekran i tilt kamery)
     * bez zadawania fałszywych obrażeń fizycznych, niszczenia pancerza czy desynchronizacji.
     */
    public static void playHurtAnimation(Player player, float yaw) {
        try {
            player.playHurtAnimation(yaw);
        } catch (Throwable ignored) {
            // Bezpieczny fallback dla nietypowych forków
        }
    }

    /**
     * Wysyła wiadomość Action Bar przy użyciu nowoczesnego Adventure Component,
     * gwarantując idealne wyświetlanie na Java Edition oraz na Bedrock Edition przez Geyser.
     */
    public static void sendActionBar(Player player, String legacyMessage) {
        try {
            Component comp = LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
            player.sendActionBar(comp);
        } catch (Throwable t) {
            try {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(legacyMessage));
            } catch (Throwable ignored) {}
        }
    }
}
