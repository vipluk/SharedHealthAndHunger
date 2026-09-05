> **⚠️ Compatibility Note:** This plugin is built for **Minecraft 1.21 - 26.2** (Paper, Purpur, Spigot). Tested and fully compatible with **WorldReset** and **Geyser Crossplay** (Bedrock Edition).

If you want to report a bug or suggest a new plugin, join my [Discord server](https://discord.gg/A7WVnYj3BP).

Also check my other plugin **[WorldReset](https://modrinth.com/plugin/worldreset)**

# ❤️ SharedHealthAndHunger v1.4

**Take on the challenge with your friends! Can you survive while sharing a single life bar?**

SharedHealthAndHunger is a plugin designed for “Shared Life” challenge. The main concept is simple: all players on the server share health, hunger, and potion effects. If one player takes damage, everyone feels it!

### ✨ Key Features in v1.4

* **❤️ Shared Health:** When one player takes damage, everyone loses HP. Every hit and tick of damage is synchronized with zero dropped hits.
* **🍗 Shared Hunger & Saturation:** Sprinting and eating affect the entire team's hunger and saturation bar.
* **🧪 Shared Effects:** Did someone drink a Speed potion, get poisoned or drink milk? Everyone gets the exact same effect change!
* **🛡️ Totem of Undying Support:** Totems now properly trigger and save the team without ending the game prematurely!
* **💥 Pain Feedback (No Armor Damage):** Teammates receive visual hurt effects (red flash and camera tilt via native `playHurtAnimation`) without damaging armor durability or rubberbanding!
* **🌍 WorldReset Integration (100%):** Automatically respects the `limbo` world! No health/hunger loss in Limbo, and deaths properly trigger WorldReset's automated resets (`/wr death`).
* **📱 Geyser Crossplay (Bedrock Support):** Bedrock players (consoles/phones/Win10) receive automatic heart scaling (`geyser-health-scale`) so the HUD never glitches with high health values.
* **💀 Spectator / Death Modes:** Switch between Game Over (Spectator mode) or simultaneous team elimination upon death.
* **📊 Action Bar:** Modern Adventure-powered action bar messages showing who took damage and current team health.
* **⚡ Metabolism Control:** Configurable hunger loss multiplier that works cleanly with vanilla exhaustion cycles.
* **🌍 Multi-Language:** Full support for **English** and **Polish** (`/sh language en/pl`).
* **📈 Server Metrics (bStats & FastStats):** Integrated anonymous server statistics via [bStats.org](https://bstats.org) and [FastStats.dev](https://faststats.dev) to compare telemetry accuracy and monitor active versions.

---

### ⚙️ Configuration & Modes

The plugin is fully configurable (`config.yml`):

* **Limbo / Ignored Worlds:** Define worlds (`ignored-worlds`) excluded from health and hunger synchronization.
* **Geyser Health Scaling:** Visually scale Bedrock hearts to 10 hearts (20 HP) even if `max-health` is 40.0 or 100.0.
* **Physical Feedback Control:** Toggle hurt animations, knockback, or attacker pain.
* **Max Health:** Set any amount of hearts for the team (e.g., 20.0 = 10 hearts, 40.0 = 20 hearts).
* **Toggles:** Every feature can be toggled on or off mid-game.

---

### 📜 Commands and Permissions

Main command: `/sharedhealth` or `/sh`

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/sh respawn` | Resets the game, heals players, clears effects, and teleports to spawn. | `sharedhealth.respawn` |
| `/sh reload` | Reloads configuration (`config.yml`) and language files. | `sharedhealth.reload` |
| `/sh sync` | Forces immediate team health and food synchronization. | `sharedhealth.sync` |
| `/sh language <en/pl>` | Changes the plugin language (English / Polish). | `sharedhealth.language` |
| `/sh set maxhealth <value>` | Sets the maximum health (e.g., 40.0 = 20 hearts). | `sharedhealth.set.maxhealth` |
| `/sh set hungermult <value>` | Sets the hunger loss multiplier (e.g., 2.0 = 2x faster). | `sharedhealth.set.hungermult` |
| **(All /sh set commands)** | **Grants access to change ALL numeric values.** | **`sharedhealth.set.*`** |
| `/sh toggle health` | Toggles health synchronization. | `sharedhealth.toggle.health` |
| `/sh toggle food` | Toggles hunger synchronization. | `sharedhealth.toggle.food` |
| `/sh toggle effects` | Toggles potion effect synchronization. | `sharedhealth.toggle.effects` |
| `/sh toggle actionbar` | Toggles action bar notifications. | `sharedhealth.toggle.actionbar` |
| `/sh toggle attackerfeeldamage` | Should the attacker feel pain when hitting others? | `sharedhealth.toggle.attackerfeeldamage` |
| `/sh toggle respawn` | Toggle Spectator mode upon death. | `sharedhealth.toggle.respawn` |
| **(All /sh toggle commands)** | **Grants access to toggle ALL options.** | **`sharedhealth.toggle.*`** |

**Wildcard Permission (Full Admin):** `sharedhealth.*`

---

### 🚀 Installation

1. Download the `.jar` file (`SharedHealthAndHunger-1.4.0.jar`).
2. Place it in the `/plugins/` folder of your server (Spigot/Paper/Purpur **1.21+ to 26.2**).
3. Restart the server.
4. Done! The configuration and language files will generate automatically.