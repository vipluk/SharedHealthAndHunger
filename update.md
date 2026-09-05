# SharedHealthAndHunger v1.4 — Update Notes

Version 1.4 is a major overhaul focusing on gameplay reliability, bedrock crossplay support, complete native integration with the [WorldReset](https://modrinth.com/plugin/worldreset) plugin, and future-proof compatibility across Minecraft 1.21 up to 26.2.

---

## 🛡️ Totem of Undying Support & Death Rework

In previous versions, `EntityDamageEvent` was cancelled prematurely upon receiving fatal damage (`healthAfter <= 0`), which completely broke Minecraft's vanilla resurrection mechanics.

### What changed:
* **Totem of Undying Activation:** Players holding a Totem of Undying in their main hand or off-hand now trigger the vanilla totem animation, sound, and resurrection effects normally.
* **Team Health Restoration:** Instead of killing teammates or terminating the session, the plugin captures the restored health and absorption hearts and synchronizes them to the entire team.
* **Loop-Safe Death Handling:** If a player dies without a Totem, `PlayerDeathEvent` is allowed to fire naturally. This eliminates recursive `StackOverflowError` loops when `respawn-spectator` is disabled and guarantees that death-tracking plugins can process the event.

---

## 🌍 WorldReset Integration (100% Native)

Full coordination with the **WorldReset** speedrun/challenge plugin:

* **Limbo World Isolation:** Players waiting in the `limbo` world during world generation are completely excluded from damage and hunger synchronization via `ignored-worlds: ["limbo"]`.
* **Automated Reset Triggering (`/wr death`):** Because `PlayerDeathEvent` now fires cleanly, WorldReset immediately detects player death, updates its `records.yml` database, updates scoreboard death counters, and initiates the automated reset.
* **Seamless World Transition:** When players leave Limbo and arrive at the spawn point of the newly generated world, their health and hunger are safely synchronized without accidental deaths or stat loss.

---

## 📱 Geyser Crossplay & Bedrock HUD Scaling

Minecraft Bedrock clients (joining via GeyserMC / Floodgate on mobile devices, Nintendo Switch, PlayStation, Xbox, and Windows 10) experience visual health bar glitches when `max-health` is increased beyond vanilla limits (e.g. 40.0 or 100.0 HP).

* **Automatic Health Scaling (`geyser-health-scale: true`):** When team max health exceeds 20.0 HP (10 hearts), Bedrock players automatically receive `setHealthScale(20.0)`. Their on-screen health bar remains a clean 10 hearts that scale proportionally, preventing HUD overflow.
* **Three-Tier Detection:** Bedrock players are identified through Floodgate API, Geyser API, or standard prefix detection (`.` or `*`).
* **Adventure Component Action Bar:** Action bar notifications are rendered as Adventure Components, which Geyser translates to Bedrock without missing formatting codes or garbled symbols.

---

## 💥 Armor-Safe Physical Feedback (`playHurtAnimation`)

In version 1.3, simulating shared pain across teammates was done by executing `p.damage(0.0001)`. This introduced several side effects:
* Armor durability was degraded on every shared hit, rapidly destroying expensive armor.
* The Thorns enchantment caused reciprocal recoil damage loops.
* Invulnerability frames (i-frames) were repeatedly reset, causing rubberbanding and anti-cheat false positives.

### Solution in v1.4:
Teammate feedback now uses the native packet-level `player.playHurtAnimation(yaw)`. Players see the camera shake and red flash without receiving artificial damage ticks or losing armor durability. Knockback can be independently tuned or disabled in `config.yml`.

---

## 🍗 Hunger Exhaustion & Saturation Synchronizer

The hunger drain task has been redesigned:
* **Vanilla 4.0 Exhaustion Boundaries:** Instead of naive subtraction, `HungerTask` mirrors vanilla exhaustion math (`0.0f - 4.0f`).
* **Saturation Deduction:** Saturation points are properly deducted before food points drop, matching vanilla behavior.
* **Instant Eating Sync:** Added `PlayerItemConsumeEvent` listening so eating food instantly synchronizes saturation to all teammates.

---

## 📈 Telemetry & Server Metrics (bStats & FastStats)

Integrated anonymous server telemetry using both **bStats** and **FastStats** to collect deployment insights, active installations, and platform demographics:
* **bStats Integration:** Configured with plugin ID `33872`. Relocated under `org.example.sharedhealthandhunger.bstats` to ensure zero classpath collisions with other plugins.
* **FastStats SDK:** Integrated official FastStats Bukkit SDK (token: `f7130edb41bc7dad6a1f697e74f54001`) with graceful startup (`context.ready()`) and shutdown (`context.shutdown()`) hooks. Relocated under `org.example.sharedhealthandhunger.faststats`.
* **Dual Telemetry Benchmark:** Both services run in parallel to benchmark data collection reliability, reporting latencies, and platform breakdown accuracy.

---

## 📜 Commands & Permissions

Main command aliases: `/sharedhealth` or `/sh`

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/sh respawn` | Resets the game, heals players, clears effects, and teleports everyone to spawn. | `sharedhealth.respawn` | OP |
| `/sh reload` | Reloads `config.yml` and language files without restarting the server. | `sharedhealth.reload` | OP |
| `/sh sync` | Forces immediate team health, food, and saturation synchronization. | `sharedhealth.sync` | OP |
| `/sh language <en/pl>` | Changes the active plugin language. | `sharedhealth.language` | OP |
| `/sh toggle health` | Toggles shared health synchronization. | `sharedhealth.toggle.health` | OP |
| `/sh toggle food` | Toggles shared hunger synchronization. | `sharedhealth.toggle.food` | OP |
| `/sh toggle effects` | Toggles shared potion effect synchronization. | `sharedhealth.toggle.effects` | OP |
| `/sh toggle actionbar` | Toggles action bar notifications. | `sharedhealth.toggle.actionbar` | OP |
| `/sh toggle attackerfeeldamage` | Toggles whether attackers feel recoil pain when hitting teammates. | `sharedhealth.toggle.attackerfeeldamage` | OP |
| `/sh toggle respawn` | Toggles between Spectator mode on death and team elimination. | `sharedhealth.toggle.respawn` | OP |
| `/sh set maxhealth <value>` | Sets the maximum health for the team (e.g. 40.0 = 20 hearts). | `sharedhealth.set.maxhealth` | OP |
| `/sh set hungermult <value>` | Sets the hunger exhaustion multiplier (e.g. 2.0 = 2x faster). | `sharedhealth.set.hungermult` | OP |

**Wildcard Permissions:**
* `sharedhealth.*` — Grants access to all plugin commands.
* `sharedhealth.toggle.*` — Grants access to all toggle subcommands.
* `sharedhealth.set.*` — Grants access to all value configuration subcommands.

---

## ⚙️ Configuration Additions (`config.yml`)

```yaml
# Compatibility with WorldReset plugin (protects the "limbo" waiting world)
worldreset-compatibility: true

# Worlds excluded from health and food synchronization
ignored-worlds:
  - "limbo"

# Geyser & Floodgate crossplay support (Bedrock Edition)
geyser-compatibility: true

# Scales Bedrock health HUD to 10 visual hearts if max-health > 20
geyser-health-scale: true

# Physical feedback settings when a teammate takes damage
physical-feedback:
  hurt-animation: true
  knockback: true
  knockback-strength: 0.2
```

---

## 📝 Translation Keys Added

Added in `messages_en.yml` and `messages_pl.yml`:
* `reload-success` — Message displayed when reloading configuration via `/sh reload`.
* `sync-success` — Message displayed when manually synchronizing team stats via `/sh sync`.
* `usage-reload` — Help entry for `/sh reload`.
* `usage-sync` — Help entry for `/sh sync`.

---

## ⚡ Technical Architecture Overview

The plugin architecture was refactored into 7 high-performance, modular classes in `org.example.sharedhealthandhunger`:

```
org.example.sharedhealthandhunger/
├── Main.java                 # Lifecycle orchestration, game reset, team stat sync
├── SharedGameListener.java   # Unified listener: health, damage, totems, hunger, effects, lifecycle
├── SharedHealthCommand.java  # Command executor and tab completer (/sh)
├── CompatibilityManager.java # Version adapter (1.21-26.2), Geyser/Floodgate hook, WorldReset hook
├── ConfigManager.java        # Configuration accessor, validator, and mutator
├── LanguageManager.java      # Adventure-powered bilingual translation manager
└── HungerTask.java           # Periodic exhaustion and hunger rate calculator
```

* **Compilation:** Target Java 21, base Paper API `1.21-R0.1-SNAPSHOT`.
* **Code Health:** 0 compiler errors, 0 deprecation warnings, 0 unused classes.
