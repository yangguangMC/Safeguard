# Safeguard 项目上下文文档

> **最后编辑于**: 2026-07-29
> **目标读者**: 不了解此项目的开发者 / AI 助手
> **目的**: 快速了解项目结构、模块职责、依赖关系和数据流转

**注意：** 本文档仅代表本项目 **目前的状态**，不对未来发展方向做出任何硬性的规定，同时必须及时更新来保证符合本项目的最新情况。

---

## 1. 项目概述

**Safeguard** 是一个面向 Minecraft 1.21.11 的 **Fabric 客户端模组**，专注于原版生存及其衍生场景。核心使命是：
**通过主动接管或被动提醒的方式，保护玩家免受游戏内各种危险情境的伤害**——包括但不限于摔落、岩浆、窒息、怪物偷袭、玩家偷袭（PVP）、受到伤害等。

所有检测与保护动作均在 **客户端本地完成**，不依赖服务端支持。不做玩法层面的改动，不引入新物品/新机制/新维度。

| 属性           | 值                          |
|----------------|-----------------------------|
| Mod ID         | `safeguard`                 |
| Mod 名称       | `Safeguard`                 |
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
Safeguard/
├── build.gradle                          # Gradle 构建脚本
├── settings.gradle                       # Gradle 设置
├── gradle.properties                     # 构建变量
├── src/main/
│   ├── java/top/yangguangmc/safeguard/
│   │   ├── Safeguard.java                # 模组入口 (ClientModInitializer)
│   │   ├── ModContext.java               # 全局上下文记录
│   │   ├── ConfigManager.java            # 配置序列化管理
│   │   ├── SafeguardCommand.java         # 客户端命令
│   │   ├── SafeguardModMenuApiImpl.java  # ModMenu API 实现
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
│   │   │   ├── GlobalProtectionConditions.java # 全局保护前置条件
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
│   │   │   │   ├── LowHealthDetection.java        # 低血量检测
│   │   │   │   ├── LowHungerDetection.java        # 饥饿检测
│   │   │   │   └── OnFireDetection.java           # 着火检测
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
    ├── minecraft-sources/                  # 项目依赖的 Minecraft 反编译、反混淆后的源码，可多阅读
    ├── yet-another-config-lib-sources/     # 项目依赖的 YACL 提供的源码
    ├── CONTEXT.md      # 用于快速了解项目结构的介绍文档
    └── ROADMAP.md      # 项目路线图与发展规划
```

---

## 3. 各文件/模块职责一览

### 3.1 入口 & 生命周期

| 文件                           | 职责                                                                                                                                                                                             | 关键依赖                                                                         |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `Safeguard.java`               | **模组主入口**，实现 `ClientModInitializer`。创建 `ProtectionManager`、`ConfigManager`、`FilledThroughWallsRenderer`，组装 `ModContext`，注册命令和配置界面，处理配置加载/保存生命周期。         | `ProtectionManager`, `ConfigManager`, `ModContext`, `FilledThroughWallsRenderer` |
| `ModContext.java`              | **全局上下文记录 (record)**，持有 `Safeguard` 实例、`ProtectionManager`、`ConfigManager`、`FilledThroughWallsRenderer` 引用。定义常量 `MOD_NAME`=`Safeguard`、`MOD_ID`=`safeguard`、Toast 类型。 | 被几乎所有模块引用                                                               |
| `ConfigManager.java`           | **配置管理器**。负责将检测项/动作的树状开关状态及绑定关系保存为 JSON 并加载。`trySave()` 支持备份恢复：写入失败时先备份原文件再重试。                                                            | `ProtectionManager`, `SwitchTreeNode`, YACL, Gson                                |
| `SafeguardModMenuApiImpl.java` | **Mod Menu 集成**。实现 `ModMenuApi`，提供配置界面工厂方法 → `ConfigScreen::create`。                                                                                                            | `ConfigScreen`, ModMenu API                                                      |

### 3.2 命令系统 & GUI

| 文件                    | 职责                                                                                                                                                                                        | 关键依赖                                                  |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| `SafeguardCommand.java` | **客户端命令** `/safeguard`。子命令: `screen`(打开配置)、`detection <id> [state]`(查看/设置检测项)、`action <id> [state]`(查看/设置动作)。提供 ID 自动补全。注册采用静态 `init(ctx)` 模式。 | `ModContext`, `ProtectionManager`, Brigadier              |
| `ConfigScreen.java`     | **YACL 配置界面**。三个配置分类：检测项开关(从 detectionRoot 树)、动作开关(从 actionRoot 树)、链接配置(检测项↔动作绑定)。静态 `init(ctx)` 持有上下文。                                      | `ModContext`, `ProtectionManager`, `SwitchTreeNode`, YACL |

### 3.3 Mixin 注入层

| 文件                           | 职责                                                                                                                                                     | 注入目标                                      |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `ClientPlayerEntityMixin.java` | 在 `tick()` 头部注入回调，触发 `START_TICK` 事件——**所有检测项的帧循环入口**。                                                                           | `ClientPlayerEntity.tick()` HEAD              |
| `EntityRendererMixin.java`     | 在 `updateRenderState()` 设置 `outlineColor` 后注入，用 `OutlineAction` 覆盖轮廓颜色，实现高亮。                                                         | `updateRenderState()` outlineColor 后         |
| `GameRendererMixin.java`       | 在 `close()` 方法 RETURN 处注入回调，触发 `GameRendererCloseEvent`——供 `FilledThroughWallsRenderer` 等清理 GPU 资源。                                    | `GameRenderer.close()` RETURN                 |
| `KeyBindingAccessor.java`      | **Accessor Mixin**，暴露 `KeyBinding.boundKey` 私有字段，供 `Utils.simulatePress()` 用。                                                                 | `KeyBinding.boundKey`                         |
| `InGameHudMixin.java`          | 在 `renderVignetteOverlay()` 中注入，通过 `@ModifyVariable` 修改晕影颜色，将原色与红色按 `RedVignetteAction.progress` 混合，实现血量越低晕影越红的效果。 | `InGameHud.renderVignetteOverlay()` INVOKE 前 |
| `LivingEntityMixin.java`       | 在 `onDamaged()` 头部注入回调，触发 `EntityDamagedEvents.PRE` 事件——供 `DamageDetection` 等检测伤害。                                                    | `LivingEntity.onDamaged()` HEAD               |

### 3.4 保护系统核心

| 文件                              | 职责                                                                                                                                                                                                                                                                                                                    |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ProtectionManager.java`          | **总管理器**。持有 `protections`(Map<Detection,Collection<Action>>)、`detectionRoot`/`actionRoot` 两棵 SwitchTreeNode 树。构造函数中预定义 `active/afk` 分类为默认关闭。提供 `predefineActionCategory()`/`predefineDetectionCategory()` API 供第三方扩展。`init()` 中注册所有检测项，注册时调用 `detection.init(ctx)`。 |
| `CategoryDefinition.java`         | **分类默认状态定义** (record)。声明树中枝干节点的默认启用状态（Identifier + defaultEnabled）。供 `ProtectionManager.predefineXxxCategory()` 使用。                                                                                                                                                                     |
| `SwitchTreeItem.java`             | **树节点接口**。定义 `getId()` 和 `isEnabledByDefault()`。Detection 和 Action 都实现此接口。                                                                                                                                                                                                                            |
| `SwitchTreeNode.java`             | **树状开关容器**。Identifier ID(/分隔层级)、enabled 状态、defaultEnabled 默认状态、父子引用。`isEffectivelyEnabled()`(级联检查)、`addOrGetNode()`(动态添加)、`setEnabled()` 触发通知所有叶节点。根节点持有 nodeMap 实现 O(1) 查找。                                                                                      |
| `GlobalProtectionConditions.java` | **全局保护前置条件**。静态工具类，持有 `List<Predicate<ClientPlayerEntity>>`。`shouldProtect(player)` 在 `GatedEvent` 的 gateFactory 中被调用，全部条件（AND 逻辑）满足才允许事件分发给检测项。当前条件：非创造/旁观、非 invulnerable、抗性提升未到 255 级。                                                                  |
| `GatedEvent.java`                 | **门控事件包装器**。在 Fabric Event 上叠加"按所有者挂起/恢复"能力。内部维护 `Map<Object,List<T>>` + `Set<Object>`，`listen(owner,listener)` 注册、`suspend/resume(owner)` 控制。纯 lambda + Supplier 实现，零反射。                                                                                                       |

### 3.5 检测项 (Detection)

所有检测项继承 `Detection`，构造时声明绑定的 Action 列表，并通过 `listen()` 声明事件监听。基类通过 `GatedEvent` 自动管理
suspend/resume，子类无需手动检查启用状态。触发动作使用 `tryExecuteAction()` 自动双重开关检查。
检测项是"事实单例"的，一个 `Identifier` 对应唯一实例。

| 文件                              | ID                             | 职责（触发条件 + 触发动作）                                                                                                                                                                          | 绑定的 Action                                                  |
|-----------------------------------|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| `Detection.java`                  | —                              | 抽象基类。ID、`boundActions`、绑定管理。`listen()`(声明门控事件监听)、`tryExecuteAction()`(自动双重开关包装)。                                                                                       | -                                                              |
| `AntiCreeperDetection.java`       | `combat/anti_creeper`          | 检测 8 格内苦力怕。ActionBar 显示距离/引信倒计时；近距离触发暂停/退出。                                                                                                                              | ActionBarTitleAction, PlaySoundAction, PauseAction, QuitAction |
| `AntiFallDetection.java`          | `environment/anti_fall`        | 三个子功能：防挖掘时掉落、已坠落保护（暂停/退出）、MLG 自动落地水。                                                                                                                                  | ActionBarTitleAction, PauseAction, QuitAction, MLGAction       |
| `AntiAmbushDetection.java`        | `combat/anti_ambush`           | 定期检测附近敌对生物/隐身玩家，ActionBar 显示信息，OutlineAction 高亮。                                                                                                                              | ActionBarTitleAction, OutlineAction                            |
| `ProjectileTrackerDetection.java` | `combat/projectile_tracker`    | 检测飞向玩家的弹射物（箭/火球等），ActionBar 警告。                                                                                                                                                  | ActionBarTitleAction                                           |
| `AntiSuffocationDetection.java`   | `environment/anti_suffocation` | 三个子功能：窒息检测（isInsideWall）、上方坠落方块检测、挖掘头顶方块意图检测。                                                                                                                       | ActionBarTitleAction                                           |
| `DamageDetection.java`            | `status/damage`                | 玩家受到伤害时自动暂停/退出。                                                                                                                                                                        | PauseAction, QuitAction                                        |
| `LavaDetection.java`              | `environment/lava`             | 玩家挖掘时扫描周围岩浆，ActionBar 显示最近岩浆距离，BlockOutlineAction 高亮。                                                                                                                        | ActionBarTitleAction, BlockOutlineAction                       |
| `LowHealthDetection.java`         | `status/low_health`            | 根据当前生命值计算 RedVignetteAction 的混合进度 (0~1)；低于阈值时触发暂停/退出。                                                                                                                     | RedVignetteAction, PauseAction, QuitAction                     |
| `LowHungerDetection.java`         | `status/low_hunger`            | 饥饿值低时评分背包中可食物品并在 ActionBar 推荐；极低时触发暂停/退出。                                                                                                                               | ActionBarTitleAction, PauseAction, QuitAction                  |
| `OnFireDetection.java`            | `environment/on_fire`          | 着火时搜索背包中灭火/防火物品，ActionBar 显示剩余时间及建议物品。                                                                                                                                    | ActionBarTitleAction                                           |

### 3.6 保护动作 (Action)

所有保护动作继承 `Action`，可以放在 `top.yangguangmc.safeguard.protection.action` 包内，也可以作为检测项的内部类。
保护动作 **不是**"事实单例"的——一个 `Identifier` 可对应多个实例（它们共享同一配置和开关状态）。

| 文件                      | ID                            | 职责                                                      | 默认启用 |
|---------------------------|-------------------------------|-----------------------------------------------------------|----------|
| `Action.java`             | —                             | 抽象基类。`getStateNode()` 获取树中开关节点。             | —        |
| `PauseAction.java`        | `active/afk/pause`            | 自动暂停（仅单人游戏）。发送聊天消息 + Toast，执行后 `setEnabled(false)`。 | **否**   |
| `QuitAction.java`         | `active/afk/quit`             | 自动退出。发送聊天消息 + Toast，执行后 `setEnabled(false)`。          | **否**   |
| `OutlineAction.java`      | `passive/other/outline`       | 实体轮廓高亮。ConcurrentHashMap 维护 UUID→剩余tick/颜色。 | 是       |
| `BlockOutlineAction.java` | `passive/other/block_outline` | 方块轮廓高亮。通过 FilledThroughWallsRenderer 标签隔离渲染。      | 是       |
| `PlaySoundAction.java`    | `passive/other/play_sound`    | 间隔播放音效。                                            | 是       |
| `RedVignetteAction.java`  | `passive/other/red_vignette`  | 红色晕影。持有静态 `progress` 字段 (0~1)，由 LowHealthDetection 更新。 | 是       |

> **注**: `ActionBarTitleAction` 是各 Detection 的内部类 (ID统一为 `passive/hud/action_bar_title`)，通过
> `client.inGameHud.setOverlayMessage()` 在 ActionBar 显示警告信息。`MLGAction` 是 `AntiFallDetection` 的内部类 (ID为
> `active/other/mlg`)。

### 3.7 工具类

| 文件                              | 职责                                                                                                                                                               |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `FilledThroughWallsRenderer.java` | **方块填充渲染器**。在指定位置绘制深度穿透填充立方体。使用 **标签隔离** 机制（`Map<String, List<BoxRenderState>>`），各调用方通过标签管理自己的方块，互不干扰。      |
| `Utils.java`                      | **工具类**。提供 `simulatePress()`(模拟按键)、`directionIndicator()`(方向指示器)、`hasDestroyIntention()`(判断玩家是否有挖掘意图) 等静态方法。                     |

---

## 4. 核心依赖关系图

```
                         Safeguard (ClientModInitializer)
                           │ 创建并注入
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                   ModContext                                      │
│  record: (instance, protectionManager, configMgr,                │
│           filledThroughWallsRenderer)                             │
│  常量: MOD_NAME=Safeguard, MOD_ID=safeguard, Toast类型            │
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
Minecraft 加载模组 → Safeguard.onInitializeClient()
  ├─ new ProtectionManager() → 空 Map + 两棵空树
  ├─ new ConfigManager()
  ├─ new FilledThroughWallsRenderer()
  ├─ new ModContext(this, pm, cm, ftr) → 封装为 record
  ├─ configManager.init(ctx) → 持有 ctx
  ├─ protectionManager.init(ctx)
  │   └─ 逐一 register() 所有 Detection 子类，添加节点到树
  ├─ configManager.tryLoad() → 从 JSON 加载配置
  ├─ ClientLifecycleEvents.CLIENT_STOPPING → configManager.trySave()
  ├─ ConfigScreen.init(ctx) → 静态持有 ctx
  ├─ SafeguardCommand.init(ctx) → 注册 /safeguard 命令
  └─ filledThroughWallsRenderer.init() → 注册渲染事件
```

### 5.2 运行时检测

检测项通过 `listen()` 声明事件监听。基类使用 `GatedEvent` 自动门控 —— 当检测项被禁用时，其监听器自动挂起，handler
中无需任何 `if (isEnabled())` 检查。

#### 5.2.1 每帧 Tick 检测

```
ClientPlayerEntity.tick() [Minecraft原生]
  └─ Mixin: ClientPlayerEntityMixin [HEAD注入]
       └─ ClientPlayerTickEvents.START_TICK.fire()
            │
            ├─ AntiCreeperDetection: 检测苦力怕距离 → ActionBar/暂停/退出
            ├─ AntiFallDetection: 防挖掘坠落 + 已坠落保护 + MLG自动落地水
            ├─ ProjectileTrackerDetection: 弹射物追踪 → ActionBar
            ├─ AntiAmbushDetection: 敌对生物/隐身玩家 → ActionBar+高亮
            ├─ AntiSuffocationDetection: 窒息/坠落方块/挖掘意图 → ActionBar
            ├─ LavaDetection: 扫描岩浆 → ActionBar+方块高亮
            ├─ LowHealthDetection: 血量→晕影混合进度 + 低血量暂停/退出
            ├─ LowHungerDetection: 背包食物评分推荐 + 极低暂停/退出
            └─ OnFireDetection: 灭火物品推荐 → ActionBar
```

#### 5.2.2 实体伤害检测

```
LivingEntity.onDamaged() [Minecraft原生]
  └─ Mixin: LivingEntityMixin [HEAD注入]
       └─ EntityDamagedEvents.PRE.fire(victim, source)
            └─ DamageDetection: 受害者是玩家? → 暂停 + 退出
```

#### 5.2.3 渲染相关

```
EntityRenderer.updateRenderState() → Mixin → OutlineAction 覆盖轮廓颜色 → 高亮

WorldRenderEvents.BEFORE_TRANSLUCENT → FilledThroughWallsRenderer 提取并绘制填充立方体

InGameHud.renderVignetteOverlay() → Mixin → RedVignetteAction 混合红色晕影

GameRenderer.close() → Mixin → GameRendererCloseEvent → 清理 GPU 资源
```

### 5.3 配置界面

```
ConfigScreen.create(parent)
  ├─ Category "检测项": detectionRoot → 每个节点 → Option<Boolean>
  ├─ Category "保护动作": actionRoot → 每个节点 → Option<Boolean>
  └─ Category "链接": 叶检测节点→boundActions → 绑定开关
```

### 5.4 启用状态配置与 SwitchTreeNode

由于需要实现"一个检测项或保护动作的分类路径上的任何一个节点都有独立的开关状态，仅当所有祖先节点均为'开'时该节点才'有效启用'"，所以使用树结构来储存所有开关状态。树的枝干节点即为分类，叶节点即为检测项或保护动作本身。

**检测项树**（`ProtectionManager#getDetectionStatesRoot`）：

```
Root (null)
├── safeguard:combat
│   ├── safeguard:combat/anti_creeper
│   ├── safeguard:combat/projectile_tracker
│   └── safeguard:combat/anti_ambush
├── safeguard:environment
│   ├── safeguard:environment/anti_fall
│   ├── safeguard:environment/anti_suffocation
│   ├── safeguard:environment/lava
│   └── safeguard:environment/on_fire
└── safeguard:status
    ├── safeguard:status/damage
    ├── safeguard:status/low_health
    └── safeguard:status/low_hunger
```

**保护动作树**（`ProtectionManager#getActionStatesRoot`）：

```
Root (null)
├── safeguard:active
│   ├── safeguard:active/afk
│   │   ├── safeguard:active/afk/pause
│   │   └── safeguard:active/afk/quit
│   └── safeguard:active/other
│       └── safeguard:active/other/mlg
└── safeguard:passive
    ├── safeguard:passive/hud
    │   └── safeguard:passive/hud/action_bar_title
    └── safeguard:passive/other
        ├── safeguard:passive/other/outline
        ├── safeguard:passive/other/block_outline
        ├── safeguard:passive/other/play_sound
        └── safeguard:passive/other/red_vignette
```

---

## 6. 关键设计模式与约定

- **Mixin 注入**: 在 Minecraft 原生代码中非侵入式注入钩子
- **门控事件 (GatedEvent)**: 在 Fabric Event 之上叠加按所有者挂起/恢复能力，检测项声明 `listen(event, handler)` 后基类自动管理启用状态
- **事件驱动**: Fabric Event API 实现自定义事件，检测项注册监听
- **树状开关**: 自定义 `SwitchTreeNode` 层级开关，支持 `defaultEnabled` 默认值 + `isEffectivelyEnabled()` 级联检查
- **分类定义 (CategoryDefinition)**: record 类型声明枝干节点默认状态，保护管理器公开 API 供第三方扩展
- **自定义渲染管线**: `FilledThroughWallsRenderer` 使用自定义 `RenderPipeline` 实现深度穿透的方块填充渲染
- **标签隔离渲染**: `FilledThroughWallsRenderer` 使用 `Map<String, List<BoxRenderState>>` 按标签隔离存储方块，不同来源互不干扰

### 命名约定

- **Identifier 路径**: `namespace:category/subcategory/leaf`，`/` 表示层级
    - 检测项: `safeguard:combat/anti_creeper`、`safeguard:environment/lava`、`safeguard:status/damage`
    - 动作: `safeguard:active/afk/pause`、`safeguard:passive/other/block_outline`
    - 分类: `combat`(战斗)、`environment`(环境)、`status`(状态)、`active/afk`(主动)、`passive/hud`(HUD)、`passive/other`(其他)
- **翻译键**: `detection.<ns>.<path>`、`action.<ns>.<path>`、`command.safeguard.*`、`screen.safeguard.*`、`category.safeguard.*`、`gui.safeguard.*`、`item.safeguard.*`、`messages.safeguard.*` (`/`→`.`)
- **默认启用**: 由树节点的 `defaultEnabled` 决定，分类级别可通过 `CategoryDefinition` 控制

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

## 8. 开发指南

1. **导入**: IntelliJ IDEA 打开 `build.gradle`，等待 Gradle 同步
2. **运行**: 使用 `Minecraft_Client` run configuration
3. **添加检测项**: 在 `protection/detection/` 下建类继承 `Detection`，构造器中 `super(path, actions...)` + `listen(GATED_START_TICK, this::onHandler)`，触发动作使用 `tryExecuteAction(actionClass, consumer)`。最后在 `ProtectionManager.init()` 调用 `register()`
4. **添加动作**: 在 `protection/action/` 下（或检测项的内部）建类继承 `Action`，实现具体逻辑
5. **添加翻译**: 在 `zh_cn.json`/`en_us.json` 添加对应翻译键。所有用户可见文本必须使用 `Text.translatable()`