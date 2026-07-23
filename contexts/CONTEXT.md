# SafeGuard 项目上下文文档

> **最后编辑于**: 2026-07-23
> **目标读者**: 不了解此项目的开发者 / AI 助手
> **目的**: 快速了解项目结构、模块职责、依赖关系和数据流转

**注意：** 本文档仅代表本项目 **目前的状态**，不对未来发展方向做出任何硬性的规定，同时必须及时更新来保证符合本项目的最新情况。

---

## 1. 项目概述

**SafeGuard** 是一个面向 Minecraft 1.21.11 的 **Fabric 客户端模组**，专注于原版生存及其衍生场景。核心使命是：
**通过主动接管或被动提醒的方式，保护玩家免受游戏内各种危险情境的伤害**——包括但不限于摔落、岩浆、窒息、怪物偷袭、玩家偷袭（PVP）等。

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
│   │   │   ├── SafeGuardScreen.java      # 抽象基础 Screen
│   │   │   └── ConfigScreen.java         # YACL 配置界面
│   │   ├── injection/mixin/
│   │   │   ├── ClientPlayerEntityMixin.java   # 注入 tick()
│   │   │   ├── EntityRendererMixin.java       # 注入渲染状态
│   │   │   └── KeyBindingAccessor.java        # Accessor Mixin
│   │   ├── protection/
│   │   │   ├── ProtectionManager.java     # 保护功能总管理器
│   │   │   ├── SwitchTreeItem.java        # 树节点接口
│   │   │   ├── SwitchTreeNode.java        # 树状开关容器
│   │   │   ├── detection/
│   │   │   │   ├── Detection.java              # 检测项抽象基类
│   │   │   │   ├── AntiCreeperDetection.java   # 防苦力怕
│   │   │   │   ├── AntiFallDetection.java      # 防摔落+MLG
│   │   │   │   ├── AntiAmbushDetection.java    # 防偷袭
│   │   │   │   └── ProjectileTrackerDetection.java # 弹射物追踪
│   │   │   ├── action/
│   │   │   │   ├── Action.java             # 保护动作基类
│   │   │   │   ├── PauseAction.java        # 自动暂停
│   │   │   │   ├── QuitAction.java         # 自动退出
│   │   │   │   ├── OutlineAction.java      # 轮廓高亮
│   │   │   │   └── PlaySoundAction.java    # 播放音效
│   │   │   └── event/
│   │   │       └── ClientPlayerTickEvents.java # Tick事件
│   │   └── util/
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
    └── CONTEXT.md      # 用于快速了解项目结构的介绍文档
```

---

## 3. 各文件/模块职责一览

### 3.1 入口 & 生命周期

| 文件                           | 职责                                                                                                                                       | 关键依赖                                           |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| `SafeGuard.java`               | **模组主入口**，实现 `ClientModInitializer`。创建 `ProtectionManager` 和 `ConfigManager`，组装 `ModContext`，注册命令和配置界面。          | `ProtectionManager`, `ConfigManager`, `ModContext` |
| `ModContext.java`              | **全局上下文记录 (record)**，持有 `SafeGuard` 实例、`ProtectionManager`、`ConfigManager` 引用。定义常量 `MOD_ID`=`safeguard`、Toast 类型。 | 被几乎所有模块引用                                 |
| `ConfigManager.java`           | **配置管理器**。负责将检测项/动作的树状开关状态及绑定关系保存为 JSON。`save()` 有已知 Bug (FIXME)，`load()` 未实现。                       | `ProtectionManager`, `SwitchTreeNode`, YACL        |
| `SafeGuardModMenuApiImpl.java` | **Mod Menu 集成**。实现 `ModMenuApi`，提供配置界面工厂方法 → `ConfigScreen::create`。                                                      | `ConfigScreen`, ModMenu API                        |

### 3.2 命令系统 & GUI

| 文件                    | 职责                                                                                                                                                         | 关键依赖                                                  |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| `SafeGuardCommand.java` | **客户端命令** `/safeguard`。子命令: `screen`(打开配置)、`detection <id> [state]`(查看/设置检测项)、`action <id> [state]`(查看/设置动作)。提供 ID 自动补全。 | `ModContext`, `ProtectionManager`, Brigadier              |
| `SafeGuardScreen.java`  | **抽象基础 Screen**。title 居中显示、关闭返回 parent screen。                                                                                                | Minecraft Screen API                                      |
| `ConfigScreen.java`     | **YACL 配置界面**。三个配置分类：检测项开关(从 detectionRoot 树)、动作开关(从 actionRoot 树)、链接配置(检测项↔动作绑定)。                                    | `ModContext`, `ProtectionManager`, `SwitchTreeNode`, YACL |

### 3.3 Mixin 注入层

| 文件                           | 职责                                                                                             | 注入目标                              |
|--------------------------------|--------------------------------------------------------------------------------------------------|---------------------------------------|
| `ClientPlayerEntityMixin.java` | 在 `tick()` 头部注入回调，触发 `START_TICK` 事件——**所有检测项的帧循环入口**。                   | `ClientPlayerEntity.tick()` HEAD      |
| `EntityRendererMixin.java`     | 在 `updateRenderState()` 设置 `outlineColor` 后注入，用 `OutlineAction` 覆盖轮廓颜色，实现高亮。 | `updateRenderState()` outlineColor 后 |
| `KeyBindingAccessor.java`      | **Accessor Mixin**，暴露 `KeyBinding.boundKey` 私有字段，供 `Utils.simulatePress()` 用。         | `KeyBinding.boundKey`                 |

### 3.4 保护系统核心

| 文件                     | 职责                                                                                                                                                                                                    |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ProtectionManager.java` | **总管理器**。持有 `protections`(Map<Detection,Collection<Action>>)、`detectionRoot`/`actionRoot` 两棵 SwitchTreeNode 树。init() 注册全部 4 个检测项。                                                  |
| `SwitchTreeItem.java`    | **树节点接口**。定义 `getId()` 和 `isEnabledByDefault()`。Detection 和 Action 都实现此接口。                                                                                                            |
| `SwitchTreeNode.java`    | **树状开关容器**。Identifier ID(/分隔层级)、enabled 状态、父子引用。`isEffectivelyEnabled()`(级联检查)、`addOrGetNode()`(动态添加)、`unmodifiableView()`(只读视图)。根节点持有 nodeMap 实现 O(1) 查找。 |

### 3.5 检测项 (Detection)

所有检测项继承 `Detection`，构造时声明绑定的 Action 列表，通过 `ClientPlayerTickEvents.START_TICK` 每帧执行，也可以自己订阅
Fabric API 的其他事件，但目前为止必须手动检查自身启用状态。 与保护动作不同，检测项是“事实单例”的，可以被使用一个
`Identifier` 从 `ProtectionManager` 那里获取到唯一的实例， 一个保护项有且仅有一个 ID 与之一一对应。 由于种种原因，检测项和保护动作不带有
`enabled` 字段。原因见下方 5.4 节。

| 文件                              | ID                      | 职责                                                                                                                                                                                  | 绑定的 Action                                                  |
|-----------------------------------|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| `Detection.java`                  | —                       | 抽象基类。ID、`boundActions`(Map<Action,Boolean>)、绑定管理。`isActionEffectivelyEnabled()` 检查动作的双重开关。                                                                      | Action                                                         |
| `AntiCreeperDetection.java`       | `combat/anti_creeper`   | **防苦力怕**。8格内检测最近苦力怕，显示距离/引信倒计时到 ActionBar；引信激活播放音效；2/3距离内触发暂停/退出。                                                                        | ActionBarTitleAction, PlaySoundAction, PauseAction, QuitAction |
| `AntiFallDetection.java`          | `environment/anti_fall` | **防摔落**。三个子功能：(1)防挖掘坠落(准星对准脚下方块+挖掘时检查下方8格)；(2)已坠落保护(fallDistance>1.5+下方不安全→暂停/退出)；(3)**MLG自动落地水**(模拟下落轨迹→自动放水/黏液块)。 | ActionBarTitleAction, PauseAction, QuitAction, MLGAction       |
| `AntiAmbushDetection.java`        | `combat/anti_ambush`    | **防偷袭**。每5帧检查16格内敌对生物/隐身玩家，ActionBar显示数量+名称+方向，OutlineAction 高亮不可见实体。                                                                             | ActionBarTitleAction, OutlineAction                            |
| `ProjectileTrackerDetection.java` | `combat/arrow_tracker`  | **弹射物追踪**。检测飞向玩家的弹射物(箭/火球等)，角度偏差<10°时 ActionBar 警告+发射者信息。                                                                                           | ActionBarTitleAction                                           |

### 3.6 保护动作 (Action)

所有保护动作继承 `Action`，可以放在 `top.yangguangmc.safeguard.protection.action` 包内，也可以作为检测项的内部类，关键在于它与绑定它的检测项的耦合程度。
与检测项不同，保护动作 **不是**“事实单例”的，一个 `Identifier` 可能对应不止一个保护动作对象，多个保护动作实例可以对应同一个
ID，并因此具有共享的配置和开关状态。 其他内容详见 5.4 节。

| 文件                   | ID                         | 职责                                                                                                                                        | 默认启用 |
|------------------------|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `Action.java`          | —                          | 抽象基类。ID、`parent`(所属Detection)、`modContext`。`getStateNode()`获取树中开关节点。                                                     | —        |
| `PauseAction.java`     | `active/afk/pause`         | **自动暂停**。仅单人游戏可用；多人游戏Toast提示+自关闭。打开 GameMenuScreen，播放音效，执行后 `setEnabled(false)`。                         | **否**   |
| `QuitAction.java`      | `active/afk/quit`          | **自动退出**。disconnect()断开连接，播放音效，Toast提示，执行后 `setEnabled(false)`。                                                       | **否**   |
| `OutlineAction.java`   | `passive/other/outline`    | **实体轮廓高亮**。静态 ConcurrentHashMap 维护 UUID→剩余tick/颜色。世界tick递减，归零移除。EntityRendererMixin 渲染时读取覆盖 outlineColor。 | 是       |
| `PlaySoundAction.java` | `passive/other/play_sound` | **间隔播放音效**。支持设置音效/音高/间隔(tick)。`setPlaying()` 控制状态，`tick()` 检查计时。                                                | 是       |

> **注**: `ActionBarTitleAction` 是各 Detection 的内部类 (ID统一为 `passive/hud/action_bar_title`)，通过
> `client.inGameHud.setOverlayMessage()` 在 ActionBar 显示警告信息。

---

## 4. 核心依赖关系图

```
                         SafeGuard (ClientModInitializer)
                           │ 创建并注入
                           ▼
┌──────────────────────────────────────────────────┐
│                   ModContext                      │
│  record: (instance, protectionManager, configMgr) │
│  常量: MOD_ID=safeguard, Toast类型                │
└──────┬──────────────────────┬────────────────────┘
       │                      │
       ▼                      ▼
┌──────────────┐    ┌──────────────────┐
│ConfigManager │    │ProtectionManager │◄──────────────────────┐
│ save()/load()│    │ protections:     │                       │
│  ↕ JSON文件  │    │  Map<Det,Act[]>  │                       │
│              │    │ detectionRoot:   │                       │
│ 依赖: Gson,  │    │  SwitchTreeNode  │                       │
│  YACL,       │    │ actionRoot:      │                       │
│  SwitchTree  │    │  SwitchTreeNode  │                       │
└──────────────┘    └──────┬───────────┘                       │
                           │ 注册 Detection                    │
                           ▼                                   │
            ┌──────────────────────────────┐                   │
            │    Detection    (检测项)      │                   │
            │  boundActions: Map<Act,Bool> │                   │
            └──────────┬───────────────────┘                   │
                       │ 触发                                   │
                       ▼                                        │
            ┌──────────────────────────────┐                   │
            │     Action  多个 (保护动作)    │──────────────────┘
            │  PauseAction, QuitAction,    │   通过 ctx 引用回
            │  OutlineAction, PlaySound,   │   ProtectionManager
            │  ActionBarTitleAction        │   获取状态节点
            └──────────────────────────────┘
```

---

## 5. 数据流转详解

### 5.1 启动初始化

```
Minecraft 加载模组 → SafeGuard.onInitializeClient()
  ├─ new ProtectionManager() → 空 Map + 两棵空树
  ├─ new ConfigManager()
  ├─ new ModContext(this, pm, cm) → 封装为 record
  ├─ configManager.init(ctx) → 持有 ctx
  ├─ protectionManager.init(ctx)
  │   ├─ register(AntiCreeperDetection)   → 添加 detection/action 节点到树
  │   ├─ register(AntiFallDetection)      → 添加 detection/action 节点到树
  │   ├─ register(ProjectileTrackerDetection) → 添加节点
  │   └─ register(AntiAmbushDetection)    → 添加节点
  ├─ ClientCommandRegistrationCallback → 注册 /safeguard 命令
  └─ ConfigScreen.init(ctx) → 静态持有 ctx
```

### 5.2 运行时检测 (每帧)

```
ClientPlayerEntity.tick() [Minecraft原生]
  │
  └─ Mixin: ClientPlayerEntityMixin [HEAD注入]
       └─ ClientPlayerTickEvents.START_TICK.fire(client, world, player)
            │
            ├─ AntiCreeperDetection: 遍历 CreeperEntity
            │   ├─ 距离≤8格 → ActionBar警告(距离+倒计时)
            │   ├─ 引信激活 → PlaySoundAction
            │   └─ 距离≤2/3×8格 → PauseAction / QuitAction
            │
            ├─ AntiFallDetection:
            │   ├─ 防挖掘坠落: 准星对脚下方块+挖掘? → checkSafety(8格) → ActionBar
            │   ├─ 已坠落保护: fallDistance>1.5+下方不安全 → Pause/Quit
            │   └─ MLG自动落地水: 模拟轨迹→自动放水/黏液块
            │
            ├─ ProjectileTrackerDetection: 弹射物速度指向玩家(角度<10°)? → ActionBar
            │
            └─ AntiAmbushDetection: 每5帧, 16格内敌人/隐身玩家 → ActionBar+Outline

EntityRenderer.updateRenderState() [Minecraft原生]
  └─ Mixin: EntityRendererMixin [outlineColor设置后]
       └─ OutlineAction.getOutline(entityUUID)!=0 → 覆盖 outlineColor → 高亮
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

由于需要实现的需求是 “一个检测项或保护动作的分类路径上的任何一个节点都有独立的开关状态，仅当这所有祖先节点节点均为‘开’时该节点才‘有效启用’”，
所以使用树结构来储存所有检测项、保护动作的开关状态。 这样一来，树的枝干节点即为分类，叶节点即为检测项或保护动作本身。

```
isEffectivelyEnabled() = 自身.enabled AND 所有祖先.enabled (递归到根)
unmodifiableView() → 只读视图 (禁止 addOrGetNode, 允许 setEnabled)
```

目前使用两棵树，分别保存检测项和保护动作的启用状态。 检测项树（使用 `ProtectionManager#getDetectionStatesRoot` 获取）：

```
Root (null)
├── safeguard:combat                [枝干节点]
│   ├── safeguard:combat/anti_creeper       [叶]
│   ├── safeguard:combat/projectile_tracker [叶]
│   └── safeguard:combat/anti_ambush        [叶]
└── safeguard:environment           [枝干节点]
    └── safeguard:environment/anti_fall     [叶]
```

保护动作树（使用 `ProtectionManager#getActionStatesRoot` 获取）：

```
Root (null)
├── safeguard:active                [枝干节点]
│   └── safeguard:active/afk        [枝干节点]
│       ├── safeguard:active/afk/pause  [叶]
│       └── safeguard:active/afk/quit   [叶]
└── safeguard:passive               [枝干节点]
    ├── safeguard:passive/hud       [枝干节点]
    │   └── safeguard:passive/hud/action_bar_title  [叶]
    └── safeguard:passive/other     [枝干节点]
        ├── safeguard:passive/other/outline [叶]
        └── passive/other/play_sound        [叶]
```

但具体的某个检测项能否触发具体的某个保护动作，除 `isEffectivelyEnabled` 外还其取决于检测项保存的一个 Action -> boolean
的映射表的内容， 其值代表对应的绑定关系是否开启。

#### 5.4.2 双重开关控制

一个 Action 被触发需同时满足:

1. **树开关**: `actionRoot.getNode(actionId).isEffectivelyEnabled()` = true
2. **绑定开关**: `detection.boundActions.get(action)` = true

其实还有一个事实上的第三重开关，在这两重之前：如果绑定保护动作的检测项没被启用，那就算以上两个条件都满足也显然不会触发。
PauseAction/QuitAction 执行后自关闭: `getStateNode().setEnabled(false)`

> 这种检查机制有点过于麻烦且容易遗漏，要寻找办法改进。
> 其实我们搞出这些分类就是希望可以让“active/afk”这样的分类能够默认关闭，在玩家有挂机需求时手动一次性打开所有“AFK适用”的保护动作等。
> 所以现阶段让 `PauseAction` 和 `QuitAction` 重写 `isEnabledByDefault()` 返回 `false` 其实不是我想要的解决方案。
> 相反，我们希望“默认启用状态”在树结构中。但当前的两棵树无法满足该需求。难道引入另外两棵树？这不方便且不优美。

---

## 6. 关键设计模式与约定

### 架构模式

- **Mixin 注入**: 在 Minecraft 原生代码中非侵入式注入钩子
- **事件驱动**: Fabric Event API 实现自定义 `START_TICK` 事件，检测项注册监听
- **树状开关**: 自定义 `SwitchTreeNode` 层级开关，父子级联有效状态检查
- **不可变视图**: `unmodifiableView()` 返回内部类 `Unmodifiable`，对外隐藏写操作

### 命名约定

- **Identifier 路径**: `namespace:category/subcategory/leaf`，`/` 表示层级
    - 检测项: `safeguard:combat/anti_creeper`、`safeguard:environment/anti_fall`
    - 动作: `safeguard:active/afk/pause`、`safeguard:passive/other/outline`
    - 分类: `combat`(战斗)、`environment`(环境)、`active/afk`(主动)、`passive/hud`(HUD)、`passive/other`(其他)
- **翻译键**: `detection.<ns>.<path>`、`action.<ns>.<path>` (`/`→`.`)
- **默认启用**: 大多数 Action 默认 true, PauseAction/QuitAction 默认 false

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

1. **ConfigManager.save () 有 Bug**: 代码中有 FIXME 注释，保存逻辑不正常
2. **ConfigManager.load () 未实现**: 方法体为空
3. **README.md 内容不全**: "它能做什么"等章节标注 TODO

---

## 9. 开发指南

1. **导入**: IntelliJ IDEA 打开 `build.gradle`，等待 Gradle 同步
2. **运行**: 使用 `Minecraft_Client` run configuration
3. **添加检测项**: 在 `protection/detection/` 下建类继承 `Detection`，构造传入 Identifier+Action 列表，`init()` 注册
   `START_TICK`，在 `ProtectionManager.init()` 调用 `register()`
4. **添加动作**: 在 `protection/action/` 下（或检测项的内部）建类继承 `Action`，实现具体逻辑
5. **添加翻译**: 在 `zh_cn.json`/`en_us.json` 添加 `detection.*`/`action.*` 键
