# AGENTS.md - SafeGuard 代码风格约束与开发规范

> **目的**: 任何 AI 助手在为本项目添加/修改代码时， **必须**遵守本文档中的所有规范。
> **优先级**: 本文档规定 > 个人偏好。如有冲突，以本文档为准。

---

## 1. 硬约束：禁止引入的依赖

**一条黄金准则**：每当涉及依赖引入，如果你不明确能不能这样做，请大胆询问。 一般尽可能使用项目已经有的依赖 (还有依赖的依赖，比如说因为
Minecraft 依赖 Guava，所以我们可以使用 Guava)。 如果一定要引入依赖，请确保其更优或者别无选择。

### 1.1 已使用的库（仅限这些，不得重复引入替代品）

| 类别     | 已使用                                                   | **禁止引入**                                                       |
|----------|----------------------------------------------------------|--------------------------------------------------------------------|
| JSON     | Gson (`com.google.gson`) (因为它是 Minecraft 的原生依赖) | Jackson, Fastjson, org.json                                        |
| 日志     | SLF4J (`org.slf4j`) (因为它是 Fabric 官方指定)           | Log4j 直接使用, java.util.logging                                  |
| 配置GUI  | YACL 3 (`dev.isxander.yacl3`)                            | Cloth Config (其已经被官方宣布“不会再有实质性更新”), 自定义GUI框架 |
| 模组菜单 | ModMenu (`com.terraformersmc.modmenu`)                   | 无替代品                                                           |
| 字节码   | Mixin (`org.spongepowered.asm`)                          | ASM 的直接使用, Javassist                                          |
| 构建     | Fabric Loom + Gradle                                     | 不得切换到其他构建系统                                             |

### 1.2 不要引入

- ❌ **Lombok** — 项目未使用，不得引入
- ❌ **任何 DI/IoC 框架** (Guice, Spring, Dagger)
- ❌ **任何 ORM/数据库库** — 这是客户端模组
- ❌ **RxJava / 响应式库** — 项目使用事件驱动，无需响应式
- ❌ **Apache Commons / Guava** — 使用 Java 标准库 + Minecraft API 即可
- ❌ **Kotlin / Scala 等 JVM 语言** — 项目为纯 Java
- ❌ **新的 Maven 仓库** — 除非必要且别无选择

---

## 2. 命名规范

基本按照现代的 Java 标准。

### 2.1 类/接口/记录

| 类型           | 规范                                                        | 示例                                                           |
|----------------|-------------------------------------------------------------|----------------------------------------------------------------|
| 类名           | **PascalCase**，名词或名词短语                              | `ProtectionManager`, `AntiCreeperDetection`                    |
| 抽象类         | 不刻意加 `Abstract` 前缀 (除非理由充分)，直接用名词         | `Detection`, `Action`                                          |
| 接口           | 不加 `I` 前缀，直接用名词/形容词                            | `SwitchTreeItem`, `ModMenuApi`                                 |
| 记录 (record)  | PascalCase 名词                                             | `ModContext`                                                   |
| 入口类         | `final class`，实现生命周期接口                             | `public final class SafeGuard implements ClientModInitializer` |
| 工具类         | `private` 构造器并抛 `AssertionError`                       | `Utils`                                                        |
| Mixin 类       | `目标类名 + Mixin`，通常为抽象类，放在 `injection.mixin` 包 | `ClientPlayerEntityMixin`                                      |
| Accessor Mixin | `接口名 + Accessor`，方法前缀 `safeguard$`                  | `KeyBindingAccessor`                                           |

### 2.2 方法

| 类型        | 规范                          | 示例                                         |
|-------------|-------------------------------|----------------------------------------------|
| 方法名      | **camelCase**，动词或动词短语 | `onStartTick()`, `isEffectivelyEnabled()`    |
| 布尔查询    | `is` / `has` / `can` 前缀     | `isLeaf()`, `isEnabled()`                    |
| 获取器      | `get` 前缀（非 record）       | `getStateNode()`, `getDetectionName()`       |
| Record 组件 | 自动生成无 `get` 前缀的方法   | `ctx.protectionManager()`                    |
| 设置器      | `set` 前缀                    | `setEnabled()`, `setPlaying()`               |
| 初始化      | `init(ModContext ctx)`        | 两阶段初始化模式                             |
| 事件处理    | `on` + 事件名                 | `onStartTick()`, `onInitializeClient()`      |
| 工厂/构建   | `build`, `create`, `of`       | `buildTree()`, `create()`, `Identifier.of()` |
| Mixin 回调  | `private void`，不加 public   | `private void onStartTick(CallbackInfo ci)`  |

### 2.3 变量与常量

| 类型         | 规范                                                            | 示例                           |
|--------------|-----------------------------------------------------------------|--------------------------------|
| 静态常量     | **UPPER_SNAKE_CASE**                                            | `MOD_ID`, `SAFEGUARD_PAUSE`    |
| Logger       | `private static final Logger LOGGER`，记录器的名字必须是 MOD_ID | 每个类独立定义                 |
| 实例字段     | **camelCase**，`private` 优先，`protected` 供子类               | `detectionRoot`, `modContext`  |
| `final` 字段 | 能 `final` 就 `final`                                           | `private final Identifier id;` |
| 局部变量     | camelCase，不损失可读性的前提下尽可能简介                       | `minD`, `entity`, `action`     |
| 缩写         | 须权衡，尽量用全称，已有 `ctx` 可沿用                           | `context` not `cxt`            |

### 2.4 Identifier 路径命名

```
规则: namespace:category/subcategory/leaf
            / 表示树层级

检测项示例: safeguard:combat/anti_creeper
保护动作示例:   safeguard:active/afk/pause
分类示例:   combat, environment, active/afk, passive/hud, passive/other
```

- 路径中用 `/` 分隔层级
- 新增 Identifier 必须遵循现有分类体系
- 翻译键: `detection.<ns>.<path>` / `action.<ns>.<path>`（`/` → `.`）

---

## 3. 代码结构规范

### 3.1 类内部成员顺序

```
1. static final 常量 + Logger
2. 实例字段 (通常 final 优先于非 final)
3. 构造函数
4. init() / 生命周期方法
5. 方法 (按含义排序优先于按访问权限递减排序)
8. 内部类
```

### 3.2 两阶段初始化模式

所有需要 `ModContext` 的类 **必须**遵循：

```java
public class SomeManager {
    private ModContext ctx;

    // 阶段1: 构造 — 仅初始化自身字段，不访问外部
    public SomeManager() { /*...*/}

    // 阶段2: init — 注入 ModContext，完成注册/绑定
    public void init(ModContext ctx) {
        this.ctx = ctx;
    }
}
```

### 3.3 包结构

```
top.yangguangmc.safeguard              # 入口、上下文、配置管理、命令
  ├── gui.screen                       # GUI 界面类
  ├── injection.mixin                  # Mixin 注入类
  ├── protection                       # 保护系统核心
  │   ├── detection                    # 检测项
  │   ├── action                       # 保护动作
  │   └── event                        # 自定义事件
  └── util                             # 工具类
```

新类必须放在对应的包中，不得随意创建新包。

---

## 4. 编码风格

### 4.1 格式化

- **缩进**: 4 空格，不用 Tab
- **编码**: UTF-8
- **大括号**: K&R 风格（开括号不换行）
- **链式调用**: 在 `.` 前换行，每个方法调用一行
- **空行**: 方法间一个空行，逻辑块间一个空行

### 4.2 可见性

尽量保证封装性。

- 字段默认 `private`，子类访问用 `protected`
- 方法: 仅对外暴露必要 API，内部逻辑用 `private`
- 类: 入口类用 `final`，数据载体用 `record`
- 工具类: `public class` + `private` 构造抛 `AssertionError`

### 4.3 注释

- **Javadoc**: 复杂或以引起歧义的方法必须写（参考 `SwitchTreeNode`），不限中英但要在同一个文件内统一语言（比如
  `SwitchTreeNode` 全部的 Javadoc 都是中文）
- **行内注释**: 用 `//`，中文编写，解释"为什么"而非"是什么"
- **FIXME/TODO**: 明确标注已知问题
- **`@SuppressWarnings`**: 必须加注释说明原因

---

## 5. 错误处理规范

| 场景                                                                | 处理方式                                                | 示例                           |
|---------------------------------------------------------------------|---------------------------------------------------------|--------------------------------|
| 编程错误（不应发生）                                                | `throw new IllegalStateException("描述")`               | 重复 parent 初始化、根节点缺失 |
| 参数校验失败                                                        | `throw new IllegalArgumentException("描述")`            | 重复 Action ID                 |
| 绝不应执行到                                                        | `throw new AssertionError("描述")`                      | 工具类构造函数                 |
| 引用为空（仅必须检查的场景，一般情况下默认非空而不用加 `@NotNull`） | `Objects.requireNonNull(ref)`                           | `getStateNode()`               |
| Stream 查找在本不该无结果的时候无结果                               | `.orElseThrow()`                                        | `getDetection(id)`             |
| 方法返回 null 表示"不存在"                                          | 返回 `null` + `@Nullable` 标注 （但不应该影响正常编程） | `getNode(Identifier)`          |

**禁止事项**:

- ❌ 绝对不要吞异常（空 catch 块）
- ❌ 不要在非 CLI 代码中 `printStackTrace()`
- ❌ 尽量不要 `catch (Exception e)` 宽泛捕获，除非你知道你在做什么
- ❌ 不要返回 `null` 表示错误（区分"不存在"和"出错了"）

---

## 6. 特定库使用规范

### 6.1 SLF4J 日志

```java
private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);

private void exampleMethod() {
    // 使用占位符，不用字符串拼接
    LOGGER.info("Protections initialized with {} detections.", count);
    LOGGER.debug("Debug message: {}", value);
}

```

### 6.2 Minecraft Text / 翻译

```java
private void exampleMethod() {
    // 翻译文本 (推荐用于用户可见文字)
    Text.translatable("detection.safeguard.combat.anti_creeper");
    // 字面文本 (仅限开发/调试/未完工标记)
    Text.literal("警告：发现潜在偷袭者");
    // 格式化字符串
    String s = "detection.%s.%s".formatted(ns, path.replace("/", "."));
}
```

### 6.3 YACL 配置 GUI

参考 [YetAnotherConfigLib 官方 Wiki](https://docs.isxander.dev/yet-another-config-lib) 中的以下的官方示例：

```java
public static Screen create(Screen parent) {
    return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("Used for narration. Could be used to render a title in the future."))
            .category(ConfigCategory.createBuilder()
                    .name(Component.literal("Name of the category"))
                    .tooltip(Component.literal("This text will appear as a tooltip when you hover or focus the button with Tab. There is no need to add \n to wrap as YACL will do it for you."))
                    .group(OptionGroup.createBuilder()
                            .name(Component.literal("Name of the group"))
                            .description(OptionDescription.of(Component.literal("This text will appear when you hover over the name or focus on the collapse button with Tab.")))
                            .option(Option.<Boolean>createBuilder()
                                    .name(Component.literal("Boolean Option"))
                                    .description(OptionDescription.of(Component.literal("This text will appear as a tooltip when you hover over the option.")))
                                    .binding(true, () -> this.myBooleanOption, newVal -> this.myBooleanOption = newVal)
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())
                            .build())
                    .build())
            .build()
            .generateScreen(parentScreen);
}
```

### 6.4 Fabric 事件

```java
// 对于检测项，尽可能在 init() 中再进行事件注册。
// 世界 Tick
public void init(ModContext ctx) {
    // ...
    ClientTickEvents.END_WORLD_TICK.register(world -> {/*...*/});
}

// 自定义事件 (Fabric Event API)
public static final Event<StartTick> START_TICK = EventFactory.createArrayBacked(
        StartTick.class,
        callbacks -> (client, world, player) -> {
            for (StartTick e : callbacks) e.onStartTick(client, world, player);
        }
);
```

---

## 7. 架构模式约束

### 7.1 检测项添加规范

```java
public class XxxDetection extends Detection {   // 其实 Detection 的名字要不要带”Anti“前缀还有待商榷。
    public XxxDetection() {
        super("category/path/to/xxx",           // Identifier 路径
                new Action1(),                  // 绑定的 Action 实例
                new Action2());
    }

    @Override
    public void init(ModContext ctx) {
        super.init(ctx);                     // 必须先调用 super.init()
        ClientPlayerTickEvents.START_TICK.register((client, world, player) -> {
            if (getStateNode().isEffectivelyEnabled()) onStartTick(client, world, player);
        });
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        // 检测逻辑: 检查危险 → getBoundAction(id) → isActionEffectivelyEnabled → 触发
        // 其实这种方式稍微有点麻烦。有什么替代方案吗？
    }
}
```

关键点:

- 构造中调用 `super(path, actions...)`
- `init()` 中先 `super.init(ctx)`，再注册 `START_TICK`
- 每帧先检查 `getStateNode().isEffectivelyEnabled()`
- 触发动作前检查 `isActionEffectivelyEnabled(action)`

如前所述，这一连串启用检查十分繁琐且易遗漏。我们正在筹划改进它。

### 7.2 保护动作添加规范

```java
public class XxxAction extends Action {
    public XxxAction() {
        super("category/path/to/xxx");          // 或 "active/category/xxx"
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;                         // 激进操作(暂停/退出)返回 false
    }

    public void execute(/*...*/) {
        // 实现动作逻辑
        // 目前为止这样的方法依靠绑定该 Action 的 Detection 直接调用，有些凌乱。
        // 我们正在筹划改进它。
    }
}
```

关键点:

- 构造中调用 `super(path)`
- 重写 `isEnabledByDefault()` 指定默认开关状态
- 通过 `getStateNode()` 获取树中开关节点
- 通过 `modContext.protectionManager()` 访问管理器

### 7.3 注册新检测项

在 `ProtectionManager.init()` 中添加: `register(new XxxDetection());`

---

## 8. 禁止事项汇总

### 绝对不该

- ❌ 命名出现混乱，如使用 PascalCase 写静态常量
- ❌ 引入第 1 节列出的黑名单第三方依赖
- ❌ 硬编码用户可见文字，必须用 `Text.translatable()`
- ❌ 改变已有的 Identifier 路径（除非被要求）
- ❌ 版本控制相关：在不经过人工审查的情况下进行非只读操作（如提交、推送）
- ❌ 其他：随便读取和修改项目根目录之外的文件

### 应尽量避免

- ⚠️ 在 Mixin 中修改 Minecraft 原生方法的行为逻辑 — 多数情况应仅注入钩子，除非避无可避
- ⚠️ 在非工具类 public 方法返回 `null`  — 除非 `@Nullable` (在不引发明知不会导致问题的警告的前提下) 或在 Javadoc 中明确指出
- ⚠️ 过度抽象 — 遵循 YAGNI
- ⚠️ 长方法 — 拆分为有意义的私有方法
- ⚠️ 重复代码 — 提取到基类或 `Utils`
- ⚠️ 深层嵌套 — 用提前 return / 卫语句
- ⚠️ Stream 中带副作用的操作 — `forEach` 仅在终端操作
- ⚠️ 在完全不了解代码工作方式的情况下主观臆断地写代码 — 应勤翻阅项目和依赖的源码

---

## 9. 快速检查清单

新增代码前自查:

- [ ] 类名 PascalCase、方法 camelCase、常量 UPPER_SNAKE_CASE ✓
- [ ] 遵循两阶段 `init(ctx)` 初始化模式 ✓
- [ ] Identifier 路径符合现有分类体系 ✓
- [ ] 用户可见文字使用 `Text.translatable()` ✓
- [ ] Logger 声明为 `private static final Logger LOGGER` ✓
- [ ] 没有引入新依赖 ✓
- [ ] 在 `ProtectionManager.init()` 中注册新检测项 ✓
- [ ] 事件相关的代码是否检查了 effective enabled ✓
- [ ] 异常使用合适的类型 (`IllegalStateException` / `IllegalArgumentException`) ✓
- [ ] 新增类放在正确包下 ✓
- [ ] FIXME/TODO 标注未完成功能 ✓

---

## 10. 小贴士

- 有不懂的大胆问，有不可能做到的大胆提出或使用工具
- 项目根目录下的 contexts 文件夹内的东西是对项目的描述、约定等，以及项目常见依赖的反编译、反混淆后的源码，不懂就大胆看
- 项目根目录下的 backup 文件夹内的东西是备份文件，可以从中看出项目的历史、之前做过的尝试，但注意
  **别把它当作正常的项目文件**，里面的要求 **不要奉为圣旨**
