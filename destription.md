❤️ **If you enjoy this plugin, please consider leaving a like! It means a lot for me.** ❤️

> **⚠️ Compatibility Note:** This plugin is built for **Minecraft 1.21 - 26.2** (Paper, Purpur, Spigot). Tested and fully compatible with **[WorldReset](https://modrinth.com/plugin/worldreset)** and **[Geyser Crossplay](https://modrinth.com/plugin/geyser)**.

If you want to report a bug or suggest a new plugin, join my [Discord server](https://discord.gg/A7WVnYj3BP).

# ❤️ SharedHealthAndHunger

**Take on the challenge with your friends! Can you survive while sharing a single life bar?**

SharedHealthAndHunger is a plugin designed for the “Shared Life” challenge. The concept is simple: all players on the server share health, hunger, and potion effects. If one player takes damage, everyone feels it!

### ✨ Key Features

* **❤️ Shared Health:** When one player takes damage, everyone loses HP. Every hit and tick of damage is synchronized with zero dropped hits.
* **🍗 Shared Hunger & Saturation:** Sprinting and eating affect the entire team's hunger and saturation bar without desync.
* **🧪 Shared Effects:** Did someone drink a Speed potion, get poisoned, or drink milk? Everyone gets the exact same effect!
* **🛡️ Totem of Undying Support:** Totems held in hand or off-hand now trigger properly! Saves the player and restores team health and absorption hearts instead of ending the game prematurely.
* **💥 Pain Feedback (No Armor Damage):** Teammates receive visual hurt effects (red flash and camera tilt) without degrading armor durability or causing rubberbanding!
* **💀 Spectator / Death Modes:** Switch between Game Over (Spectator mode) or simultaneous team elimination upon death.
* **📊 Modern Action Bar:** Real-time action bar notifications showing who took damage and current team health across both Java and Bedrock editions.
* **⚡ Metabolism Control:** Configurable hunger loss multiplier that works cleanly with vanilla exhaustion cycles.
* **🌍 Multi-Language:** Full support for **English** and **Polish** (`/sh language en/pl`).
* **📈 Server Metrics (bStats & FastStats):** Integrated anonymous server statistics via [bStats.org](https://bstats.org) and [FastStats.dev](https://faststats.dev) to compare telemetry accuracy and monitor active versions.

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

1. Download the `.jar` file (`SharedHealthAndHunger-1.4.jar`).
2. Place it in the `/plugins/` folder of your server (Spigot/Paper/Purpur **1.21 - 26.2**).
3. Restart the server.
4. Done! The `config.yml` and language files will generate automatically.