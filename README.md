# Safeguard

![Minecraft 1.21.11](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![MIT License](https://img.shields.io/badge/License-MIT-green)

**A client-side helper that watches your back in Minecraft survival.**

---

## Overview

Safeguard is a **client-side Fabric mod** that detects in-game dangers and helps you avoid them — through visual/audio
warnings, or optional automatic actions like pausing and disconnecting.

It does **not** introduce new items, mechanics, or dimensions. It does **not** make you invincible. Everything runs
locally on the client — no server-side support needed.

---

## Features

### Detections

All detections are **enabled by default** unless otherwise noted.

| Category       | Detection              | What it warns about                                                      |
|----------------|------------------------|--------------------------------------------------------------------------|
| 💥 Combat      | **Anti Creeper**       | Nearby creepers, showing distance and fuse countdown                     |
| 💥 Combat      | **Anti Ambush**        | Hostile mobs and invisible players around you                            |
| 💥 Combat      | **Projectile Tracker** | Projectiles heading your way (arrows, fireballs, etc.)                   |
| 🌍 Environment | **Anti Fall**          | Mining above caves/cliffs, dangerous falls, plus optional auto MLG       |
| 🌍 Environment | **Anti Suffocation**   | Being inside a wall or under falling blocks                              |
| 🌍 Environment | **Lava Detection**     | Lava near your mining path                                               |
| 🌍 Environment | **On Fire**            | When burning — also suggests items that can extinguish in your inventory |
| 💊 Status      | **Damage Detection**   | Taking damage (can trigger auto-pause / quit — off by default)           |
| 💊 Status      | **Low Health**         | Health dropping below thresholds (red vignette + optional auto actions)  |
| 💊 Status      | **Low Hunger**         | Hunger running low, with smart food recommendations                      |

### Protection Actions

Actions are triggered by detections. Each can be toggled on/off independently.

| Type       | Action                                      | Default |
|------------|---------------------------------------------|---------|
| 🛡️ Active  | **Auto Pause** (singleplayer only)          | ❌ off  |
| 🛡️ Active  | **Auto Quit**                               | ❌ off  |
| 🛡️ Active  | **Auto MLG** (water / slime placement)      | ✅ on   |
| 💬 Passive | **Action Bar Title** warning text           | ✅ on   |
| 💬 Passive | **Entity Outline** glowing highlight        | ✅ on   |
| 💬 Passive | **Block Highlight** through walls           | ✅ on   |
| 💬 Passive | **Sound Alert**                             | ✅ on   |
| 💬 Passive | **Red Vignette** (low-health screen effect) | ✅ on   |

---

## Configuration

Open the config screen via **Mod Menu** or `/safeguard screen`.

![Config Screen](images/en_us/config_screen.png)

Toggle detections and actions in a tree structure — disabling a category (e.g. "Combat")
disables everything under it. Config is saved to `.minecraft/config/safeguard.json`.

---

## Commands

```
/safeguard screen                        Open config screen
/safeguard detection <id> [state]        View or toggle a detection
/safeguard action <id> [state]           View or toggle an action
```

The command is purely client-side. It will not be sent to servers once parsed successfully. IDs use the format
`namespace:category/.../name`. The command provides **tab completion**.

---

## Dependencies

| Type        | Name                                              | Version   |
|-------------|---------------------------------------------------|-----------|
| Required    | [Fabric API](https://modrinth.com/mod/fabric-api) | ≥ 0.141.4 |
| Required    | [YACL](https://modrinth.com/mod/yacl)             | ≥ 3.8.2   |
| Recommended | [Mod Menu](https://modrinth.com/mod/modmenu)      | ≥ 17.0.0  |

---

## Feedback & Contributing

Bug reports and feature ideas are welcome on [GitHub Issues](https://github.com/yangguangMC/SafeGuard/issues).

Pull requests are appreciated! For the current project structure, please read the context documentation,
[`contexts/CONTEXT.md`](contexts/CONTEXT.md). To work with AI agents, add that documentation to your context, and add [
`.clinerules/AGENTS.md`](.clinerules/AGENTS.md) as one of the agent's rule files. After making modifications, please
update `CONTEXT.md`.

---

## License

MIT — see [LICENSE.txt](LICENSE.txt).
