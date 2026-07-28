# SafeGuard 项目上下文文档

> **最后编辑于**: 2026-07-28
> **目标读者**: 不了解此项目的开发者 / AI 助手
> **目的**: 快速了解项目结构、模块职责、依赖关系和数据流转

**注意：** 本文档仅代表本项目 **目前的状态**，不对未来发展方向做出任何硬性的规定，同时必须及时更新来保证符合本项目的最新情况。

---

## 1. 项目概述

**SafeGuard** 是一个面向 Minecraft 1.21.11 的 **Fabric 客户端模组**，专注于原版生存及其衍生场景。核心使命是：
**通过主动接管或被动提醒的方式，保护玩家免受游戏内各种危险情境的伤害**——包括但不限于摔落、岩浆、窒息、怪物偷袭、玩家偷袭（PVP）、受到伤害等。

所有检测与保护动作均在 **客户端本地完成**，不依赖服务端支持。不做玩法层面的改动，不引入新物品/新机制/新维度。

| 属性           | 值                          |
|----------------|-----------------------------|
| Mod ID         | `safeguard`                 |
| Mod 名称       | `Safe Guard`                |
| 包名           | `top.yangguangmc.safeguard` |
| 作者           | `yangguangMC`               |
| 许可           | MIT                         |
| 环境           | 客户端 (client)             |
| Java 版本      | 21                          |
| Minecraft 版本 | 1.21.11                     |
| Fabric Loader  | 0.19.3                      |
| Fabric API     | 0.141.4                     |

### 关键外部依赖

| 依赖                               | 版本                 | 用途              |
|------------------------------------|----------------------|-------------------|
| `fabric-api`                       | 0.141.4+1.21.11      | Fabric 基础 API   |
| `yet_another_config_lib_v3` (YACL) | 3.8.2+1.21.11-fabric | 配置 GUI 生成框架 |
| `modmenu`                          | 17.0.0               | Mod Menu 集成     |

---

## 2. 项目目录结构

```
Safe Guard/
├── build.gradle                          # Gradle 构建脚本
├── settings.gradle                       # Gradle 设置
├── gradle.properties                     # 构建变量
├── src/main/
│   ├── java/top/yangguangmc/safeguard/
│   │   ├── SafeGuard.java                # 模组入口 (ClientModInitializer)
│   │   ├── ModContext.java               # 全局上下文记录
│   │   ├── ConfigManager.java            # 配置序列化管理
│   │   ├── SafeGuardCommand.java         # 客户端命令
│   │   ├── SafeGuardModMenuApiImpl.java  # ModMenu API 实现
│   │   ├── gui/screen/
│   │   │   └── ConfigScreen.java         # YACL 配置界面
│   │   ├── injection/mixin/
│   │   │   ├── ClientPlayerEntityMixin.java   # 注入 tick()
│   │   │   ├── EntityRendererMixin.java       # 注入渲染状态
│   │   │   ├── GameRendererMixin.java         # 注入 close() 事件
│   │   │   ├── InGameHudMixin.java            # 注入晕影颜色
│   │   │   ├── KeyBindingAccessor.java        # Accessor Mixin
│   │   │   └── LivingEntityMixin.java         # 注入 onDamaged()
│   │   ├── protection/
│   │   │   ├── ProtectionManager.java     # 保护功能总管理器
│   │   │   ├── CategoryDefinition.java    # 分类默认状态定义
│   │   │   ├── SwitchTreeItem.java        # 树节点接口
│   │   │   ├── SwitchTreeNode.java        # 树状开关容器
│   │   │   ├── detection/
│   │   │   │   ├── Detection.java              # 检测项抽象基类
│   │   │   │   ├── AntiCreeperDetection.java   # 防苦力怕
│   │   │   │   ├── AntiFallDetection.java      # 防摔落+MLG
│   │   │   │   ├── AntiAmbushDetection.java    # 防偷袭
│   │   │   │   ├── ProjectileTrackerDetection.java # 弹射物追踪
│   │   │   │   ├── AntiSuffocationDetection.java   # 防窒息
│   │   │   │   ├── DamageDetection.java           # 伤害检测
│   │   │   │   ├── LavaDetection.java             # 岩浆检测
│   │   │   │   └── LowHealthDetection.java        # 低血量检测
│   │   │   ├── action/
│   │   │   │   ├── Action.java             # 保护动作基类
│   │   │   │   ├── PauseAction.java        # 自动暂停
│   │   │   │   ├── QuitAction.java         # 自动退出
│   │   │   │   ├── OutlineAction.java      # 实体轮廓高亮
│   │   │   │   ├── BlockOutlineAction.java # 方块轮廓高亮
│   │   │   │   ├── PlaySoundAction.java    # 播放音效
│   │   │   │   └── RedVignetteAction.java  # 红色晕影
│   │   │   └── event/
│   │   │       ├── ClientPlayerTickEvents.java  # Tick事件
│   │   │       ├── EntityDamagedEvents.java     # 实体受伤事件
│   │   │       ├── GameRendererCloseEvent.java  # GameRenderer 关闭事件
│   │   │       └── GatedEvent.java              # 门控事件包装器
│   │   └── util/
│   │       ├── FilledThroughWallsRenderer.java  # 方块填充渲染器
│   │       └── Utils.java                  # 工具类
│   └── resources/
│       ├── fabric.mod.json                # 模组元数据
│       ├── safeguard.mixins.json          # Mixin配置
│       └── assets/safeguard/lang/
│           ├── en_us.json                 # 英语翻译
│           └── zh_cn.json                 # 中文翻译
├── backup/         # 备份文件
└── contexts/       # 对项目的描述、约定等，以及项目常见依赖的反编译、反混淆后的源码
    ├── AGENTS.md       # AI 助手必读的开发规范等
    ├── CONTEXT.md      # 用于快速了解项目结构的介绍文档
    └── ROADMAP.md      # 项目路线图与发展规划
```

---

## 3. 各文件/模块职责一览

### 3.1 入口 & 生命周期

| 文件                           | 职责                                                                                                                                                                                             | 关键依赖                                                                         |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `SafeGuard.java`               | **模组主入口**，实现 `ClientModInitializer`。创建 `ProtectionManager`、`ConfigManager`、`FilledThroughWallsRenderer`，组装 `ModContext`，注册命令和配置界面，处理配置加载/保存生命周期。         | `ProtectionManager`, `ConfigManager`, `ModContext`, `FilledThroughWallsRenderer` |
| `ModContext.java`              | **全局上下文记录 (record)**，持有 `SafeGuard` 实例、`ProtectionManager`、`ConfigManager`、`FilledThroughWallsRenderer` 引用。定义常量 `MOD_NAME`=`SafeGuard`、`MOD_ID`=`safeguard`、Toast 类型。 | 被几乎所有模块引用                                                               |
| `ConfigManager.java`           | **配置管理器**。负责将检测项/动作的树状开关状态及绑定关系保存为 JSON 并加载。`trySave()` 支持备份恢复：写入失败时先备份原文件再重试。                                                            | `ProtectionManager`, `SwitchTreeNode`, YACL, Gson                                |
| `SafeGuardModMenuApiImpl.java` | **Mod Menu 集成**。实现 `ModMenuApi`，提供配置界面工厂方法 → `ConfigScreen::create`。                                                                                                            | `ConfigScreen`, ModMenu API                                                      |

### 3.2 命令系统 & GUI

| 文件                    | 职责                                                                                                                                                                                        | 关键依赖                                                  |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| `SafeGuardCommand.java` | **客户端命令** `/safeguard`。子命令: `screen`(打开配置)、`detection <id> [state]`(查看/设置检测项)、`action <id> [state]`(查看/设置动作)。提供 ID 自动补全。注册采用静态 `init(ctx)` 模式。 | `ModContext`, `ProtectionManager`, Brigadier              |
| `ConfigScreen.java`     | **YACL 配置界面**。三个配置分类：检测项开关(从 detectionRoot 树)、动作开关(从 actionRoot 树)、链接配置(检测项↔动作绑定)。静态 `init(ctx)` 持有上下文。                                      | `ModContext`, `ProtectionManager`, `SwitchTreeNode`, YACL |

### 3.3 Mixin 注入层

| 文件                           | 职责                                                                                                                  | 注入目标                              |
|--------------------------------|-----------------------------------------------------------------------------------------------------------------------|---------------------------------------|
| `ClientPlayerEntityMixin.java` | 在 `tick()` 头部注入回调，触发 `START_TICK` 事件——**所有检测项的帧循环入口**。                                        | `ClientPlayerEntity.tick()` HEAD      |
| `EntityRendererMixin.java`     | 在 `updateRenderState()` 设置 `outlineColor` 后注入，用 `OutlineAction` 覆盖轮廓颜色，实现高亮。                      | `updateRenderState()` outlineColor 后 |
| `GameRendererMixin.java`       | 在 `close()` 方法 RETURN 处注入回调，触发 `GameRendererCloseEvent`——供 `FilledThroughWallsRenderer` 等清理 GPU 资源。 | `GameRenderer.close()` RETURN         |
| `KeyBindingAccessor.java`      | **Accessor Mixin**，暴露 `KeyBinding.boundKey` 私有字段，供 `Utils.simulatePress()` 用。                              | `KeyBinding.boundKey`                 |
| `InGameHudMixin.java`          | 在 `renderVignetteOverlay()` 中注入，通过 `@ModifyVariable` 修改晕影颜色，将原色与红色按 `RedVignetteAction.progress` 混合，实现血量越低晕影越红的效果。 | `InGameHud.renderVignetteOverlay()` INVOKE 前 |
| `LivingEntityMixin.java`       | 在 `onDamaged()` 头部注入回调，触发 `EntityDamagedEvents.PRE` 事件——供 `DamageDetection` 等检测伤害。                 | `LivingEntity.onDamaged()` HEAD       |

### 3.4 保护系统核心

| 文件                          | 职责                                                                                                                                                                                                                                                                                                                       |
|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ProtectionManager.java`      | **总管理器**。持有 `protections`(Map<Detection,Collection<Action>>)、`detectionRoot`/`actionRoot` 两棵 SwitchTreeNode 树。构造函数中预定义 `active/afk` 分类为默认关闭。提供 `predefineActionCategory()`/`predefineDetectionCategory()` API 供第三方扩展。`init()` 注册全部 8 个检测项，注册时调用 `detection.init(ctx)`。 |
| `CategoryDefinition.java`     | **分类默认状态定义** (record)。声明树中枝干节点的默认启用状态（Identifier + defaultEnabled）。供 `ProtectionManager.predefineXxxCategory()` 使用，第三方扩展通过创建此类实例声明自定义分类。                                                                                                                               |
| `SwitchTreeItem.java`         | **树节点接口**。定义 `getId()` 和 `isEnabledByDefault()`。Detection 和 Action 都实现此接口。`isEnabledByDefault()` 现仅作为约定保留，默认状态由树的 `defaultEnabled` 管理。                                                                                                                                                |
| `SwitchTreeNode.java`         | **树状开关容器**。Identifier ID(/分隔层级)、enabled 状态、defaultEnabled 默认状态、父子引用。`isEffectivelyEnabled()`(级联检查)、`addOrGetNode()`(动态添加)、`predefineCategory()`(预定义分类默认值)、`setEnabled()` 触发 `notifyLeafDescendants()` 通知所有叶节点。根节点持有 nodeMap 实现 O(1) 查找。                    |
| `GameRendererCloseEvent.java` | **GameRenderer 关闭事件**。供 `FilledThroughWallsRenderer` 等组件在 GameRenderer 关闭时清理 GPU 资源。`GameRendererMixin` 在 `GameRenderer.close()` RETURN 处触发此事件。                                                                                                                                                  |
| `GatedEvent.java`             | **门控事件包装器**。在 Fabric Event 上叠加"按所有者挂起/恢复"能力。内部维护 `Map<Object,List<T>>` + `Set<Object>`，`listen(owner,listener)` 注册、`suspend/resume(owner)` 控制。纯 lambda + Supplier 实现，零反射。                                                                                                        |

### 3.5 检测项 (Detection)

所有检测项继承 `Detection`，构造时声明绑定的 Action 列表，并通过 `listen()` 声明事件监听。 基类通过 `GatedEvent` 自动管理
suspend/resume，子类无需手动检查启用状态。触发动作使用 `tryExecuteAction()` 自动双重开关检查。
与保护动作不同，检测项是"事实单例"的，可以被使用一个
`Identifier` 从 `ProtectionManager` 那里获取到唯一的实例， 一个检测项有且仅有一个 ID 与之一一对应。 由于种种原因，检测项和保护动作不带有
`enabled` 字段。原因见下方 5.4 节。

| 文件                              | ID                             | 职责                                                                                                                                                                                                                                                                     | 绑定的 Action                                                  |
|-----------------------------------|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| `Detection.java`                  | —                              | 抽象基类。ID、`boundActions`(Map<Action,Boolean>)、绑定管理。`listen()`(声明门控事件监听)、`tryExecuteAction()`(自动双重开关包装)、`applyActiveState()`(供 ProtectionManager 调用)。                                                                                     | -                                                              |
| `AntiCreeperDetection.java`       | `combat/anti_creeper`          | **防苦力怕**。8格内检测最近苦力怕，显示距离/引信倒计时到 ActionBar；引信激活播放音效；2/3距离内触发暂停/退出。                                                                                                                                                           | ActionBarTitleAction, PlaySoundAction, PauseAction, QuitAction |
| `AntiFallDetection.java`          | `environment/anti_fall`        | **防摔落**。三个子功能：(1)防挖掘坠落(准星对准脚下方块+挖掘时检查下方8格)；(2)已坠落保护(fallDistance>1.5+下方不安全→暂停/退出)；(3)**MLG自动落地水**(模拟下落轨迹→自动放水/黏液块)。                                                                                    | ActionBarTitleAction, PauseAction, QuitAction, MLGAction       |
| `AntiAmbushDetection.java`        | `combat/anti_ambush`           | **防偷袭**。每5帧检查16格内敌对生物/隐身玩家，ActionBar显示数量+名称+方向，OutlineAction 高亮不可见实体。                                                                                                                                                                | ActionBarTitleAction, OutlineAction                            |
| `ProjectileTrackerDetection.java` | `combat/projectile_tracker`    | **弹射物追踪**。检测飞向玩家的弹射物(箭/火球等)，角度偏差<10°时 ActionBar 警告+发射者信息。                                                                                                                                                                              | ActionBarTitleAction                                           |
| `AntiSuffocationDetection.java`   | `environment/anti_suffocation` | **防窒息**。三个子功能：(1)窒息检测(玩家 isInsideWall 时显示窒息方块名称)；(2)上方坠落方块检测(检查头顶方块是否可坠落)；(3)挖掘判断(通过 `Utils.hasDestroyIntention()` 检测挖掘头顶方块的意图)。                                                                         | ActionBarTitleAction                                           |
| `DamageDetection.java`            | `status/damage`                | **伤害检测**。监听 `EntityDamagedEvents.GATED_PRE`，当玩家受到伤害时自动暂停/退出游戏。                                                                                                                                                                                  | PauseAction, QuitAction                                        |
| `LavaDetection.java`              | `environment/lava`             | **岩浆检测**。每5帧检查一次，当玩家有挖掘意图时，以玩家为中心扫描岩浆方块（主世界5×5×5，下界通过 `EnvironmentAttributes.FAST_LAVA_GAMEPLAY` 判定后扩展为9×9×9）。ActionBar 显示最近岩浆的距离与方向，BlockOutlineAction 高亮所有发现的岩浆方块。禁用时自动清除方块高亮。 | ActionBarTitleAction, BlockOutlineAction                       |
| `LowHealthDetection.java`         | `status/low_health`            | **低血量检测**。每帧根据玩家当前生命值计算 0~1 的混合进度 delta：低于 minRateThreshold(默认10%最大生命值，下限4) 时为 1，高于 maxRateThreshold(默认30%最大生命值，上限20) 时为 0，之间线性插值。delta 传给 RedVignetteAction 控制红色晕影强度；低于 maxHealthThreshold 时触发暂停/退出。 | RedVignetteAction, PauseAction, QuitAction                     |

### 3.6 保护动作 (Action)

所有保护动作继承 `Action`，可以放在 `top.yangguangmc.safeguard.protection.action` 包内，也可以作为检测项的内部类，关键在于它与绑定它的检测项的耦合程度。
与检测项不同，保护动作 **不是**"事实单例"的，一个 `Identifier` 可能对应不止一个保护动作对象，多个保护动作实例可以对应同一个
ID，并因此具有共享的配置和开关状态。但对于保护动作的 ID **对于单个检测项来说** 是唯一的、一一对应的。

| 文件                      | ID                            | 职责                                                                                                                                                                  | 默认启用 |
|---------------------------|-------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `Action.java`             | —                             | 抽象基类。ID、`parent`(所属Detection)、`modContext`。`getStateNode()`获取树中开关节点。`createSoundInstance()` 工厂方法供子类使用。                                   | —        |
| `PauseAction.java`        | `active/afk/pause`            | **自动暂停**。`pause(MinecraftClient client)` 接收 client 参数。仅单人游戏可用；多人游戏Toast提示+自关闭。打开 GameMenuScreen，播放音效，执行后 `setEnabled(false)`。 | **否**   |
| `QuitAction.java`         | `active/afk/quit`             | **自动退出**。`quit(MinecraftClient client)` 接收 client 参数。disconnect()断开连接，播放音效，Toast提示，执行后 `setEnabled(false)`。                                | **否**   |
| `OutlineAction.java`      | `passive/other/outline`       | **实体轮廓高亮**。静态 ConcurrentHashMap 维护 UUID→剩余tick/颜色。世界tick递减，归零移除。EntityRendererMixin 渲染时读取覆盖 outlineColor。                           | 是       |
| `BlockOutlineAction.java` | `passive/other/block_outline` | **方块轮廓高亮**。通过 `FilledThroughWallsRenderer` 的标签隔离机制渲染立方体。以所属检测项 ID 为 tag 调用 `addBox()`，添加前自动 `clearByTag()` 确保不残留旧数据。    | 是       |
| `PlaySoundAction.java`    | `passive/other/play_sound`    | **间隔播放音效**。支持设置音效/音高/间隔(tick)。`setPlaying()` 控制状态，`tick()` 检查计时。                                                                          | 是       |
| `RedVignetteAction.java`  | `passive/other/red_vignette`  | **红色晕影**。持有静态 `progress` 字段 (0~1)。`LowHealthDetection` 每帧调用 `setProgress(delta)` 更新混合进度，`InGameHudMixin` 读取 `getProgress()` 将原晕影颜色与红色插值，血量越低晕影越红。 | 是       |

> **注**: `ActionBarTitleAction` 是各 Detection 的内部类 (ID统一为 `passive/hud/action_bar_title`)，通过
> `client.inGameHud.setOverlayMessage()` 在 ActionBar 显示警告信息。`MLGAction` 是 `AntiFallDetection` 的内部类 (ID为
> `active/other/mlg`)。

### 3.7 工具类

| 文件                              | 职责                                                                                                                                                                                                                                                                                                                                                                 |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `FilledThroughWallsRenderer.java` | **方块填充渲染器**。通过 Fabric `WorldRenderEvents.BEFORE_TRANSLUCENT` 注册渲染，在指定位置绘制填充立方体。支持深度穿透（`DepthTestFunction.NO_DEPTH_TEST`）。使用 **标签隔离** 机制（`Map<String, List<BoxRenderState>>`），各调用方通过 `addBox(tag, ...)` 添加、`clearByTag(tag)` 清除，方块持久化渲染直到显式清除。监听 `GameRendererCloseEvent` 释放 GPU 资源。 |
| `Utils.java`                      | **工具类**。提供 `simulatePress()`(模拟按键)、`directionIndicator()`(方向指示器)、`hasDestroyIntention()`(判断玩家是否有挖掘意图) 等静态方法。                                                                                                                                                                                                                       |

---

## 4. 核心依赖关系图

```
                         SafeGuard (ClientModInitializer)
                           │ 创建并注入
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                   ModContext                                      │
│  record: (instance, protectionManager, configMgr,                │
│           filledThroughWallsRenderer)                             │
│  常量: MOD_NAME=SafeGuard, MOD_ID=safeguard, Toast类型            │
└──────┬──────────────────────┬────────────────────┬───────────────┘
       │                      │                    │
       ▼                      ▼                    ▼
┌──────────────┐    ┌──────────────────┐   ┌────────────────────────┐
│ConfigManager │    │ProtectionManager │   │FilledThroughWallsRenderer│
│ save()/load()│    │ protections:     │   │  addBox(tag,...)        │
│  ↕ JSON文件  │    │  Map<Det,Act[]>  │   │  clearByTag(tag)        │
│              │    │ detectionRoot:   │   │  标签隔离 / 深度穿透渲染 │
│ 依赖: Gson,  │    │  SwitchTreeNode  │   │  监听 GameRendererClose  │
│  YACL,       │    │ actionRoot:      │   └────────────────────────┘
│  SwitchTree  │    │  SwitchTreeNode  │
└──────────────┘    └──────┬───────────┘
                           │ 注册 Detection
                           ▼
            ┌──────────────────────────────┐
            │    Detection    (检测项)      │
            │  boundActions: Map<Act,Bool> │
            └──────────┬───────────────────┘
                       │ 触发
                       ▼
            ┌──────────────────────────────┐
            │     Action  多个 (保护动作)    │
            │  PauseAction, QuitAction,    │
            │  OutlineAction, PlaySound,   │
            │  BlockOutlineAction,         │
            │  RedVignetteAction,          │
            │  ActionBarTitleAction,       │
            │  MLGAction                   │
            └──────────────────────────────┘
```

---

## 5. 数据流转详解

### 5.1 启动初始化

```
Minecraft 加载模组 → SafeGuard.onInitializeClient()
  ├─ new ProtectionManager() → 空 Map + 两棵空树
  ├─ new ConfigManager()
  ├─ new FilledThroughWallsRenderer()
  ├─ new ModContext(this, pm, cm, ftr) → 封装为 record
  ├─ configManager.init(ctx) → 持有 ctx
  ├─ protectionManager.init(ctx)
  │   ├─ register(AntiCreeperDetection)       → 添加 detection/action 节点到树
  │   ├─ register(AntiFallDetection)          → 添加 detection/action 节点到树
  │   ├─ register(ProjectileTrackerDetection) → 添加节点
  │   ├─ register(AntiAmbushDetection)        → 添加节点
  │   ├─ register(AntiSuffocationDetection)   → 添加节点
  │   ├─ register(LavaDetection)              → 添加节点
  │   ├─ register(DamageDetection)            → 添加节点
  │   └─ register(LowHealthDetection)         → 添加节点
  ├─ configManager.tryLoad() → 从 JSON 加载配置
  ├─ ClientLifecycleEvents.CLIENT_STOPPING → configManager.trySave()
  ├─ ConfigScreen.init(ctx) → 静态持有 ctx
  ├─ SafeGuardCommand.init(ctx) → 注册 /safeguard 命令
  └─ filledThroughWallsRenderer.init() → 注册 WorldRenderEvents + GameRendererCloseEvent
```

### 5.2 运行时检测

检测项通过 `listen()` 声明事件监听。基类使用 `GatedEvent` 自动门控 —— 当检测项被禁用时，其监听器自动挂起，不再接收事件，handler
中无需任何 `if (isEnabled())` 检查。

#### 5.2.1 每帧 Tick 检测

```
ClientPlayerEntity.tick() [Minecraft原生]
  │
  └─ Mixin: ClientPlayerEntityMixin [HEAD注入]
       └─ ClientPlayerTickEvents.START_TICK.fire(client, world, player)
            │
            ├─ AntiCreeperDetection: 遍历 CreeperEntity
            │   ├─ 距离≤8格 → ActionBar警告(距离+倒计时)
            │   ├─ 引信激活 → PlaySoundAction
            │   └─ 距离≤2/3×8格 → QuitAction.quit(client) / PauseAction.pause(client)
            │
            ├─ AntiFallDetection:
            │   ├─ 防挖掘坠落: 准星对脚下方块+挖掘? → checkSafety(8格) → ActionBar
            │   ├─ 已坠落保护: fallDistance>1.5+下方不安全 → PauseAction.pause(client) / QuitAction.quit(client)
            │   └─ MLG自动落地水: 模拟轨迹→自动放水/黏液块
            │
            ├─ ProjectileTrackerDetection: 弹射物速度指向玩家(角度<10°)? → ActionBar
            │
            ├─ AntiAmbushDetection: 每5帧, 16格内敌人/隐身玩家 → ActionBar+Outline
            │
            ├─ AntiSuffocationDetection:
            │   ├─ 窒息检测: player.isInsideWall() → 显示窒息方块 → ActionBar
            │   ├─ 上方坠落方块: 检查头顶 Falling 方块 → ActionBar
            │   └─ 挖掘判断: Utils.hasDestroyIntention() → ActionBar
            │
            ├─ LavaDetection: 每5帧, Utils.hasDestroyIntention()?
            │   ├─ 有意图 → 扫描岩浆 (主世界5×5×5/下界9×9×9)
            │   │   ├─ 有岩浆 → ActionBar(最近距离+方向) + BlockOutlineAction(高亮)
            │   │   └─ 无岩浆 → clearByTag(id) 清除高亮
            │   └─ 无意图 → clearByTag(id) 清除高亮
            │
            └─ LowHealthDetection: 每帧
                ├─ 血量 < minRateThreshold → delta=1 (红色晕影满)
                ├─ 血量 > maxRateThreshold → delta=0 (无效果)
                ├─ 之间 → 线性插值 delta → RedVignetteAction.setProgress(delta)
                └─ 血量 < maxHealthThreshold → PauseAction + QuitAction
```

#### 5.2.2 实体伤害检测

```
LivingEntity.onDamaged() [Minecraft原生]
  │
  └─ Mixin: LivingEntityMixin [HEAD注入]
       └─ EntityDamagedEvents.PRE.fire(victim, source)
            │
            └─ DamageDetection: 受害者是玩家?
                └─ 是 → PauseAction.pause(client) + QuitAction.quit(client)
```

#### 5.2.3 渲染相关

```
EntityRenderer.updateRenderState() [Minecraft原生]
  └─ Mixin: EntityRendererMixin [outlineColor设置后]
       └─ OutlineAction.getOutline(entityUUID)!=0 → 覆盖 outlineColor → 高亮

WorldRenderEvents.BEFORE_TRANSLUCENT [Fabric事件]
  └─ FilledThroughWallsRenderer.extractAndDraw()
       ├─ 提取阶段: 遍历 taggedStates (Map<String,List>) → 构建顶点缓冲
       └─ 绘制阶段: upload + draw (深度穿透渲染)

GameRenderer.close() [Minecraft原生]
  └─ Mixin: GameRendererMixin [RETURN注入]
       └─ GameRendererCloseEvent.CALLBACK → FilledThroughWallsRenderer.close() 释放GPU资源

InGameHud.renderVignetteOverlay() [Minecraft原生]
  └─ Mixin: InGameHudMixin [INVOKE前注入]
       └─ RedVignetteAction.getProgress()>0 → 将晕影颜色与红色按 progress 混合 → 低血量红色晕影
```

### 5.3 配置界面

```
ConfigScreen.create(parent)
  ├─ Category "检测项": detectionRoot → 每个节点 → Option<Boolean>
  │     名称: i18n("detection.<ns>.<path>") + 缩进
  │     绑定: node.isEnabled() / node.setEnabled()
  ├─ Category "保护动作": actionRoot → 每个节点 → Option<Boolean>
  │     名称: i18n("action.<ns>.<path>") + 缩进
  │     绑定: node.isEnabled() / node.setEnabled()
  └─ Category "链接": 遍历叶检测节点→boundActions→TickBox
        绑定: isBindingEnabled() / setBindingEnabled()
```

### 5.4 启用状态配置相关

#### 5.4.1 SwitchTreeNode 树结构

由于需要实现的需求是
"一个检测项或保护动作的分类路径上的任何一个节点都有独立的开关状态，仅当这所有祖先节点节点均为'开'时该节点才'有效启用'
"，所以使用树结构来储存所有检测项、保护动作的开关状态。 这样一来，树的枝干节点即为分类，叶节点即为检测项或保护动作本身。

```
isEffectivelyEnabled() = 自身.enabled AND 所有祖先.enabled (递归到根)
unmodifiableView() → 只读视图 (禁止 addOrGetNode, 允许 setEnabled)
```

目前使用两棵树，分别保存检测项和保护动作的启用状态。 检测项树（使用 `ProtectionManager#getDetectionStatesRoot` 获取）：

```
Root (null)
├── safeguard:combat                      [枝干节点]
│   ├── safeguard:combat/anti_creeper             [叶]
│   ├── safeguard:combat/projectile_tracker       [叶]
│   └── safeguard:combat/anti_ambush              [叶]
├── safeguard:environment                 [枝干节点]
│   ├── safeguard:environment/anti_fall           [叶]
│   ├── safeguard:environment/anti_suffocation    [叶]
│   └── safeguard:environment/lava               [叶]
└── safeguard:status                      [枝干节点]
    ├── safeguard:status/damage                    [叶]
    └── safeguard:status/low_health               [叶]
```

保护动作树（使用 `ProtectionManager#getActionStatesRoot` 获取）：

```
Root (null)
├── safeguard:active                [枝干节点]
│   ├── safeguard:active/afk        [枝干节点]
│   │   ├── safeguard:active/afk/pause  [叶]
│   │   └── safeguard:active/afk/quit   [叶]
│   └── safeguard:active/other      [枝干节点]
│       └── safeguard:active/other/mlg  [叶]
└── safeguard:passive               [枝干节点]
    ├── safeguard:passive/hud       [枝干节点]
    │   └── safeguard:passive/hud/action_bar_title  [叶]
        └── safeguard:passive/other     [枝干节点]
            ├── safeguard:passive/other/outline        [叶]
            ├── safeguard:passive/other/block_outline  [叶]
            ├── safeguard:passive/other/play_sound     [叶]
            └── safeguard:passive/other/red_vignette   [叶]
```

---

## 6. 关键设计模式与约定

- **Mixin 注入**: 在 Minecraft 原生代码中非侵入式注入钩子
- **门控事件 (GatedEvent)**: 在 Fabric Event 之上叠加按所有者挂起/恢复能力，检测项声明 `listen(event, handler)`
  后基类自动管理启用状态
- **事件驱动**: Fabric Event API 实现自定义事件，检测项注册监听
- **树状开关**: 自定义 `SwitchTreeNode` 层级开关，支持 `defaultEnabled` 默认值 + `isEffectivelyEnabled()` 级联检查
- **分类定义 (CategoryDefinition)**: record 类型声明枝干节点默认状态，保护管理器公开 API 供第三方扩展
- **不可变视图**: `unmodifiableView()` 返回内部类 `Unmodifiable`，对外隐藏写操作
- **自定义渲染管线**: `FilledThroughWallsRenderer` 使用自定义 `RenderPipeline` 实现深度穿透的方块填充渲染
- **标签隔离渲染**: `FilledThroughWallsRenderer` 使用 `Map<String, List<BoxRenderState>>` 按标签隔离存储方块。各调用方通过
  `addBox(tag, ...)` 添加、`clearByTag(tag)` 清除，不同来源的方块互不干扰。方块持久化渲染（不再每帧丢弃），由调用方在扫描前显式清除旧数据或在禁用时清理

### 命名约定

- **Identifier 路径**: `namespace:category/subcategory/leaf`，`/` 表示层级
    - 检测项: `safeguard:combat/anti_creeper`、`safeguard:environment/lava`、`safeguard:status/damage`、`safeguard:status/low_health`
    - 动作: `safeguard:active/afk/pause`、`safeguard:passive/other/block_outline`、`safeguard:passive/other/red_vignette`
    - 分类: `combat`(战斗)、`environment`(环境)、`status`(状态)、`active/afk`(主动)、`passive/hud`(HUD)、`passive/other`
      (其他)
- **翻译键**: `detection.<ns>.<path>`、`action.<ns>.<path>` (`/`→`.`)
- **默认启用**: 由树节点的 `defaultEnabled` 决定，分类级别可通过 `CategoryDefinition` 控制（如 `active/afk` 默认 false）

---

## 7. 技术栈

| 层级       | 技术                           |
|------------|--------------------------------|
| 语言       | Java 21                        |
| 构建       | Gradle + Fabric Loom 1.17      |
| 模组加载   | Fabric Loader 0.19.3           |
| 字节码注入 | Mixin 0.8+ (SpongePowered ASM) |
| 配置 GUI   | YACL 3.8.2                     |
| 模组菜单   | ModMenu 17.0.0                 |
| JSON       | Gson                           |
| 日志       | SLF4J                          |
| 命令       | Brigadier + Fabric Command API |

---

## 8. 已知问题

1. **README.md 内容不全**: "它能做什么"等章节标注 TODO

---

## 9. 开发指南

1. **导入**: IntelliJ IDEA 打开 `build.gradle`，等待 Gradle 同步
2. **运行**: 使用 `Minecraft_Client` run configuration
3. **添加检测项**: 在 `protection/detection/` 下建类继承 `Detection`，构造器中 `super(path, actions...)` +
   `listen(GATED_START_TICK, this::onHandler)`（或监听其他门控事件），触发动作使用
   `tryExecuteAction(actionClass, consumer)`。无需手动检查启用状态或重写
   `init()`。最后在 `ProtectionManager.init()` 调用 `register()`
4. **添加动作**: 在 `protection/action/` 下（或检测项的内部）建类继承 `Action`，实现具体逻辑。如果是方块高亮类动作，使用
   `modContext.filledThroughWallsRenderer().addBox(tag, ...)` 并按需在扫描前 `clearByTag(tag)`
5. **添加翻译**: 在 `zh_cn.json`/`en_us.json` 添加 `detection.*`/`action.*` 键