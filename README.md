> **⚠️ Compatibility Note:** This plugin is built for **Minecraft 1.21.5 Purpur**. I tested it on Purpur 1.21.5 and Spigot 1.21.1. It should work on 1.21.1-1.21.5 Spigot/Purpur/Paper/Bukkit. 

If you want to report a bug or suggest a new plugin, join my [Discord server](https://discord.gg/A7WVnYj3BP).

Also check my other plugin **[WorldReset](https://modrinth.com/plugin/worldreset)**

# ❤️ SharedHealthAndHunger

**Take on the challenge with your friends! Can you survive while sharing a single life bar?**

SharedHealthAndHunger is a plugin designed for “Shared Life” challenge. The main concept is simple: all players on the server share health, hunger, and potion effects. If one player takes damage, everyone feels it!

### ✨ Key Features

* **❤️ Shared Health:** When one player takes damage, everyone loses HP.
* **🍗 Shared Hunger:** Sprinting and eating affect the entire team's hunger bar.
* **🧪 Shared Effects:** Did someone drink a Speed potion or eat a Pufferfish? Everyone gets the same effect!
* **💥 Physical Damage Feedback:** When a player takes damage, teammates also receive a visual "hurt" effect (red screen) and slight knockback.
* **💀 Spectator Mode:** If health drops to zero, the game ends, and all players are automatically moved to Spectator mode.
* **📊 Action Bar:** Notifications about who took damage and how much HP was lost appear above the hotbar.
* **⚡ Metabolism Control:** Configurable hunger drain speed (e.g., make hunger drop 2x faster).
* **🌍 Multi-Language:** Full support for **English** and **Polish** (changeable via command).

---

### ⚙️ Configuration & Modes

The plugin is fully configurable. You can customize the gameplay to your needs:

* **Physical Feedback Control:** You can decide if the attacker should receive physical feedback (knockback/pain) when hitting a teammate. (option `attackerfeeldamage`).
* **Max Health:** Set any amount of hearts for the team (e.g., 10, 20, or even 100 hearts).
* **Toggles:** Every feature (health sync, hunger sync, effects) can be toggled on or off mid-game.

---

### 📜 Commands and Permissions

Main command: `/sharedhealth` or `/sh`

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/sh respawn` | Resets the game, heals players, clears effects, and teleports to spawn. | `sharedhealth.respawn` |
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

1.  Download the `.jar` file.
2.  Place it in the `/plugins/` folder of your server (Spigot/Paper/Purpur **1.21.5**).
3.  Restart the server.
4.  Done! The `config.yml` file will generate automatically.