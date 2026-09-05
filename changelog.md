# Changelog - SharedHealthAndHunger v1.4

### 🚀 New Features
* 🛡️ **Totem of Undying Support:** Totems held in the main hand or off-hand now trigger properly! When a player takes fatal damage, the totem resurrects them and synchronizes the restored health and golden hearts to the whole team instead of ending the game prematurely.
* 🌍 **WorldReset Integration:** Full native compatibility with [WorldReset](https://modrinth.com/plugin/worldreset):
  * Players waiting in the Limbo lobby are automatically protected from shared damage and hunger loss.
  * Death detection cleanly triggers WorldReset's automated resets (`/wr death`) and records tracking.
  * Team health and hunger are seamlessly restored when entering a freshly generated world.
* 📱 **Bedrock & Geyser Crossplay Support:** Full compatibility for players joining from consoles, mobile devices, and Windows 10:
  * Automatic heart scaling (`geyser-health-scale: true`): When team max health exceeds 10 hearts (20 HP), Bedrock players receive visual scaling to keep the health bar clean and prevent it from overflowing the screen.
  * Automatically detects Bedrock players via Floodgate or Geyser.
* 💥 **Visual Hurt Feedback (No Armor Damage):** Teammates now see and feel pain feedback (camera tilt and red flash) when a partner takes damage, without damaging armor durability or canceling invulnerability frames.
* 🍗 **Accurate Hunger & Eating Sync:** Overhauled hunger synchronization to match vanilla mechanics. Eating food now instantly syncs hunger and saturation across all teammates without jitter or desync.
* 📊 **Enhanced Action Bar Notifications:** Action bar messages showing who took damage and current team health now render smoothly across both Java and Bedrock editions.
* 📈 **bStats & FastStats Metrics Integration:** Added anonymous server metrics via [bStats.org](https://bstats.org) and [FastStats.dev](https://faststats.dev) to monitor active installations and Minecraft versions. Both services are included side-by-side to compare their telemetry data and results.

### ⚙️ New Commands & Configuration
* 🔄 **Reload Command (`/sh reload`):** Reloads configuration and translations on the fly without needing to restart the server (`sharedhealth.reload`).
* ⚡ **Manual Sync Command (`/sh sync`):** Instantly synchronizes health, hunger, and saturation across all teammates (`sharedhealth.sync`).
* 🌐 **Ignored Worlds:** Easily exclude lobby, hub, or minigame worlds from shared damage and hunger in `config.yml`.
* 🎛️ **Customizable Physical Feedback:** Configure whether teammates receive hurt screen tilt, knockback, or attacker feedback independently in `config.yml`.

### 🐛 Fixes & Improvements
* 🛑 **Fixed Infinite Death Loop:** Fixed an issue where dying with spectator mode disabled could crash the server or trap players in an endless death loop.
* 🎯 **Fixed Rapid Combat Hits:** Fixed an issue where rapid attacks and combo hits were silently dropped or ignored. All damage is now reliably shared.
* 🛡️ **Fixed Armor Degradation:** Teammates no longer lose durability on chestplates, leggings, and boots when a partner takes environmental damage like falling, fire, or lava.
* ⚔️ **Fair Gamemode Switching:** Players switching from Creative or Spectator back to Survival no longer get a free full heal; health and food properly synchronize to the current team state.
* 🌐 **Minecraft 1.21 – 26.2 Compatibility:** Updated for the latest Minecraft and Paper versions with improved overall stability and performance.
