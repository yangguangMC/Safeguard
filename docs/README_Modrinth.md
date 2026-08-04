# README.md Modrinth 迁移流程

从 README.md 开始，进行如下操作：

- 将 `## Feedback & Contributing` 后一段文字简化
- 移除 `## License`
- 移除文章内嵌的 Icon
-

替换所有引用的仓库内容的链接 （[必须是原始的链接](https://raw.githubusercontent.com/yangguangMC/Safeguard/master/images)）

- 替换脚注

**下方这条分割线及其上方的所有内容不应该出现于模组发布页面！**
**如果你在模组发布页读到了这条消息，那么这一定是一个错误，请你提醒开发者！**

---

# Safeguard

**A client-side helper that watches your back in Minecraft survival.**

---

## Overview

Safeguard is a **client-side Fabric mod** that detects in-game dangers and helps you avoid them — through visual/audio
warnings, or optional automatic actions like pausing and disconnecting.

It does **not** introduce new items, mechanics, or dimensions. It does **not** make you invincible. It simply tries its
best to warn and protect you in case of danger. Everything runs locally on the client — no server-side support needed.

---

## Beta Notice

Safeguard is currently in **beta testing** and ships as an **MVP (Minimum Viable Product)** — it covers a broad range of
common dangers, but there is still plenty of room to grow. **Many more features and refinements are planned** for future
updates, such as per-item configuration options, new detection types (equipment durability warnings, potion effect HUD,
recommending optimal torch placement), and advanced features like projectile trajectory visualization.

Your feedback during this beta phase is especially valuable. If you encounter bugs, have ideas for improvement, or want
to suggest a new detection, please [report them on GitHub Issues](https://github.com/yangguangMC/Safeguard/issues)!

## Features

All protections are organized in a **tree-structured toggle system** — each detection and action belongs to a category
hierarchy (e.g. `Combat → Anti Creeper`). Disabling a category disables everything under it, giving you precise control
without drowning in options. The **AFK category** is off by default, so aggressive actions like auto-pause and auto-quit
won't interfere with normal gameplay — just enable it when you step away.

### Detections

All detections are **enabled by default** unless otherwise noted.

| Category       | Detection              | What it warns about                                                                                                |
|----------------|------------------------|--------------------------------------------------------------------------------------------------------------------|
| 💥 Combat      | **Anti Creeper**       | Nearby creepers, showing distance and fuse countdown                                                               |
| 💥 Combat      | **Anti Ambush**        | Hostile mobs and invisible players around you                                                                      |
| 💥 Combat      | **Projectile Tracker** | Projectiles heading your way (arrows, fireballs, etc.)                                                             |
| 🌍 Environment | **Anti Fall**          | Mining above caves/cliffs, dangerous falls, plus optional auto MLG                                                 |
| 🌍 Environment | **Anti Suffocation**   | Being inside a wall or under falling blocks (triggers a notification indicating how many falling blocks are there) |
| 🌍 Environment | **Lava Detection**     | Lava near your mining path                                                                                         |
| 🌍 Environment | **On Fire**            | When burning — also suggests items that can extinguish in your inventory                                           |
| 💊 Status      | **Damage Detection**   | Taking damage (triggers auto-pause / quit — but AFK category is off by default)                                    |
| 💊 Status      | **Low Health**         | Health dropping below thresholds (red vignette + optional auto actions)                                            |
| 💊 Status      | **Low Hunger**         | Hunger running low, with smart food recommendations                                                                |

### Protection Actions

Actions are triggered by detections. Each can be toggled on/off independently.

| Type             | Action                                      | Default    |
|------------------|---------------------------------------------|------------|
| 🛡️ Active/AFK    | **Auto Pause** [2]                          | ✅ on [1]  |
| 🛡️ Active/AFK    | **Auto Quit** [2]                           | ❌ off [3] |
| 🛡️ Active/Other  | **Auto MLG** (water / slime placement)      | ❌ off     |
| 💬 Passive/HUD   | **Action Bar Title** warning text           | ✅ on      |
| 💬 Passive/HUD   | **Red Vignette** (low-health screen effect) | ✅ on      |
| 💬 Passive/Other | **Entity Outline** glowing highlight        | ✅ on      |
| 💬 Passive/Other | **Block Highlight** through walls           | ✅ on      |
| 💬 Passive/Other | **Sound Alert**                             | ✅ on      |

Notes:

[^1]: However, it is blocked by the AFK category, which is off by default.

[^2]: The two actions will disable the AFK category when triggered (to prevent being triggered repeatedly).

[^3]: Auto Pause does not apply in multiplayer. If you want protection on a server, enable Auto Quit manually.

---

## Playing on servers

Safeguard is a client-side mod — most features work entirely on your local game client and do not affect server
gameplay. A few features (notably, for example, **Auto MLG**) send interaction packets to the server, which strict
anti-cheat systems could potentially flag.

As a general rule, **check with server admins** before using any client mods in multiplayer; use at your own risk.

---

## Screenshots

### Anti Creeper

![Anti Creeper](https://raw.githubusercontent.com/yangguangMC/Safeguard/master/images/anti_creeper.png)

### Lava Detection

![Lava Detection](https://raw.githubusercontent.com/yangguangMC/Safeguard/master/images/lava_detection.png)

### Low Hunger

![Low Hunger](https://raw.githubusercontent.com/yangguangMC/Safeguard/master/images/low_hunger.png)

Intelligently recommends food based on your current hunger and saturation level, preventing waste and providing maximum
efficiency.

---

## Configuration

Open the config screen via **Mod Menu** or `/safeguard screen`.

![Config Screen](https://raw.githubusercontent.com/yangguangMC/Safeguard/master/images/config_screen.png)

Toggle detections and actions in a tree structure — disabling a category (e.g. "Combat")
disables everything under it. Config is saved to `.minecraft/config/safeguard.json`.

---

## Commands

```
/safeguard screen                        Open config screen
/safeguard detection <id> [state]        View or toggle a detection
/safeguard action <id> [state]           View or toggle an action
```

It is purely client-side and is never sent to the server. IDs use the format
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

Bug reports and feature ideas are welcome on [GitHub Issues](https://github.com/yangguangMC/Safeguard/issues).

For more information, please view our [GitHub Repository](https://github.com/yangguangMC/Safeguard).
