# Changelog - SharedHealthAndHunger v1.4

### ✨ New Features
* 🛡️ **Totem of Undying Support:** Totems held in main hand or off-hand now trigger properly! When a player suffers fatal damage with a Totem of Undying, the vanilla resurrection effect occurs, saves the player, and synchronizes the restored health and absorption hearts to the entire team instead of ending the game prematurely.
* 🌍 **WorldReset Integration (100% Native):** Full synchronization coordination with the [WorldReset](https://modrinth.com/plugin/worldreset) plugin:
  * Automatically isolates the `limbo` waiting world (`ignored-worlds`). Players waiting in Limbo do not take shared damage, lose hunger, or affect active games.
  * Allows `PlayerDeathEvent` to fire normally so WorldReset's automated death resets (`/wr death`) and records tracking (`records.yml`) trigger without hitch.
  * Seamless state restoration: team health, food, and saturation are cleanly restored when players leave Limbo and enter a freshly generated world.
* 📱 **Geyser Crossplay & Bedrock Support:** Full compatibility for players joining from phones, consoles, and Windows 10 via GeyserMC & Floodgate:
  * Automatic heart scaling (`geyser-health-scale: true`): When team max health exceeds 20 HP (10 hearts), Bedrock clients receive visual scaling (`setHealthScale(20.0)`) to prevent the health HUD from glitching or overflowing the screen.
  * Native Bedrock detection supporting Floodgate API, Geyser API, and username prefix fallbacks.
* 💥 **Armor-Safe Hurt Animation:** Replaced artificial damaging (`p.damage(0.0001)`) with native `player.playHurtAnimation(yaw)`. Teammates receive visual pain feedback (camera tilt and red flash) without degrading armor durability, triggering Thorns enchantment recoil, or resetting invulnerability frames.
* 🍗 **Exhaustion & Saturation Rework:** Completely overhauled `HungerTask` to accurately respect vanilla exhaustion boundaries (`0.0f - 4.0f`). Eliminates hunger bar desynchronization and jitter on Bedrock clients. Added `PlayerItemConsumeEvent` handling to instantly synchronize food saturation.
* 📊 **Pure Adventure Action Bar:** Modern Adventure Component implementation replacing legacy BungeeCord chat APIs. Colors, bold tags, and damage formatting render reliably across both Java and Bedrock editions.

### 📜 New Commands & Configuration
* 🔄 **`/sh reload`:** Reloads `config.yml` and language files (`messages_en.yml`, `messages_pl.yml`) without server restarts. (Permission: `sharedhealth.reload`).
* ⚡ **`/sh sync`:** Forces an immediate team-wide health, food, and saturation alignment to full capacity. (Permission: `sharedhealth.sync`).
* ⚙️ **Configurable Physical Feedback:** Independent toggles in `config.yml` for `physical-feedback.hurt-animation`, `physical-feedback.knockback`, and `physical-feedback.knockback-strength`.
* 🌐 **Ignored Worlds List:** `ignored-worlds: ["limbo"]` allows server owners to exclude custom lobby, hub, or minigame worlds from shared damage and hunger.

### 🐛 Fixed Bugs & Critical Improvements
* 🛑 **Fixed Infinite Death Loop Recursion:** Fixed fatal `StackOverflowError` caused by cancelling `EntityDamageEvent` and recursively invoking `p.damage()` on dead players when `respawn-spectator` was disabled.
* 🎯 **Fixed Dropped Combat Damage Hits:** Removed the rigid 100ms damage cooldown that silently discarded legitimate rapid hits. Loop-protection now relies on thread-safe atomic entity suppression sets.
* 🛡️ **Fixed Armor Degradation Bug:** Teammates no longer lose durability on chestplates, leggings, and boots when a partner takes environmental damage (fall, lava, fire).
* ⚔️ **Fair Gamemode Switching:** Players switching from Creative or Spectator back to Survival no longer get a free full heal; health and food automatically sync to the current team state.

### ⚡ Technical & Architecture
* 📦 **Streamlined 7-Class Architecture:** Consolidated all listeners and platform handlers into cohesive classes in `org.example.sharedhealthandhunger` ([Main](file:///c:/Users/vipluk/.gemini/antigravity-ide/scratch/SharedHealthAndHunger/src/main/java/org/example/sharedhealthandhunger/Main.java), [SharedGameListener](file:///c:/Users/vipluk/.gemini/antigravity-ide/scratch/SharedHealthAndHunger/src/main/java/org/example/sharedhealthandhunger/SharedGameListener.java), [SharedHealthCommand](file:///c:/Users/vipluk/.gemini/antigravity-ide/scratch/SharedHealthAndHunger/src/main/java/org/example/sharedhealthandhunger/SharedHealthCommand.java), [CompatibilityManager](file:///c:/Users/vipluk/.gemini/antigravity-ide/scratch/SharedHealthAndHunger/src/main/java/org/example/sharedhealthandhunger/CompatibilityManager.java), [ConfigManager](file:///c:/Users/vipluk/.gemini/antigravity-ide/scratch/SharedHealthAndHunger/src/main/java/org/example/sharedhealthandhunger/ConfigManager.java), [LanguageManager](file:///c:/Users/vipluk/.gemini/antigravity-ide/scratch/SharedHealthAndHunger/src/main/java/org/example/sharedhealthandhunger/LanguageManager.java), [HungerTask](file:///c:/Users/vipluk/.gemini/antigravity-ide/scratch/SharedHealthAndHunger/src/main/java/org/example/sharedhealthandhunger/HungerTask.java)).
* 🛡️ **Zero Compiler Warnings & Zero Deprecated APIs:** 100% clean compilation on Java 21 with Paper API 1.21-R0.1. Completely eliminated `net.md_5.bungee.*`, `org.bukkit.ChatColor`, and `Bukkit.broadcastMessage`.
* 🌐 **Minecraft 1.21 – 26.2 Compatibility:** Dynamic attribute resolution supporting both `minecraft:generic.max_health` (1.21 - 1.21.1) and `minecraft:max_health` (1.21.2 - 26.2+).
