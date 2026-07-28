# AGENTS.md - SafeGuard 代码风格约束与开发规范

> **目的**: 任何 AI 助手在为本项目添加/修改代码时， **必须**遵守本文档中的所有规范。
> **优先级**: 本文档规定 > 个人偏好。如有冲突，以本文档为准。

在阅读本规则之前，请注意你应该已经读过全局规则（本机 Document 文件夹\Cline\Rules\java-coding-standards.md）了。
本文档仅补充其未涉及的部分。 并且在开始任务之前，请阅读 **项目上下文文档**（项目根目录/contexts/CONTEXT.md）了解项目目前的情况。
在每次完成修改任务后，也应该及时更新 CONTEXT.md 的内容。

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

基本按照现代的 Java 标准。 多数内容已经在全局规则中声明，此处仅补充它不包含的信息。

### 2.1 类/接口/记录

| 类型           | 规范                                                        | 示例                                                           |
|----------------|-------------------------------------------------------------|----------------------------------------------------------------|
| 入口类         | `final class`，实现生命周期接口                             | `public final class SafeGuard implements ClientModInitializer` |
| Mixin 类       | `目标类名 + Mixin`，通常为抽象类，放在 `injection.mixin` 包 | `ClientPlayerEntityMixin`                                      |
| Accessor Mixin | `接口名 + Accessor`，方法前缀 `safeguard$`                  | `KeyBindingAccessor`                                           |

### 2.2 方法

| 类型       | 规范                        | 示例                                         |
|------------|-----------------------------|----------------------------------------------|
| 初始化     | `init(ModContext ctx)`      | 两阶段初始化模式                             |
| 事件处理   | `on` + 事件名               | `onStartTick()`, `onInitializeClient()`      |
| 工厂/构建  | `build`, `create`, `of`     | `buildTree()`, `create()`, `Identifier.of()` |
| Mixin 回调 | `private void`，不加 public | `private void onStartTick(CallbackInfo ci)`  |

### 2.3 变量与常量

均已包含在全局规则中。

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

均已包含在全局规则中。

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

新类须放在对应的包中，不得随意创建新包。

---

## 4. 编码风格

### 4.1 格式化

均已包含在全局规则中。

### 4.2 可见性

均已包含在全局规则中。

### 4.3 注释

- **Javadoc**: 复杂或以引起歧义的方法必须写（参考 `SwitchTreeNode`），不限中英但要在同一个文件内统一语言（比如
  `SwitchTreeNode` 全部的 Javadoc 都是中文）
- **行内注释**: 用 `//`，中文编写，解释"为什么"而非"是什么"
- **FIXME/TODO**: 明确标注已知问题
- **`@SuppressWarnings`**: 必须加注释说明原因

---

## 5. 错误处理规范

均已包含在全局规则中。

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
        // 注册事件。Detection、SwitchTreeNode 和 GatedEvent 会自动管理当前保护动作的启用状态。
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    @Override
    public void init(ModContext ctx) {
        super.init(ctx);                     // 必须先调用 super.init()
        // ...
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        // 检测逻辑: 检查危险 → tryExecuteAction() → 触发
    }
}
```

关键点:

- 构造中调用 `super(path, actions...)`，注册各个事件
- `init()` 中先 `super.init(ctx)`

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
若有需要，在构造函数中调用其 `predefineDetectionCategory()` 等方法确定其默认启用状态。

---

## 8. 禁止事项汇总

其余内容在全局规则中。补充内容：

### 绝对不该

- ❌ 硬编码用户可见文字，必须用 `Text.translatable()`（除非仅停留于测试阶段）
- ❌ 改变已有的 Identifier 路径（除非被要求）

**再次强调**： **无论是读还是写任何文件**，都务必 **显式指定 UTF-8 without BOM 编码**，否则出现严重的乱码问题！

### 应尽量避免

- ⚠️ 在 Mixin 中修改 Minecraft 原生方法的行为逻辑 — 多数情况应仅注入钩子，除非避无可避
- ⚠️ 在非工具类 public 方法返回 `null`  — 除非 `@Nullable` (在不引发明知不会导致问题的警告的前提下) 或在 Javadoc 中明确指出
- ⚠️ 在完全不了解代码工作方式的情况下主观臆断地写代码 — 应勤翻阅项目和依赖的源码

---

## 9. 快速检查清单

自查内容补充:

- [ ] Identifier 路径符合现有分类体系 ✓
- [ ] 用户可见文字使用 `Text.translatable()` ✓
- [ ] 在 `ProtectionManager.init()` 中注册新检测项 ✓
- [ ] FIXME/TODO 标注未完成功能 ✓

---

## 10. 小贴士

- 有不懂的大胆问，有不可能做到的大胆提出或使用工具
- 项目根目录下的 contexts 文件夹内的东西是对项目的描述、约定等，以及项目常见依赖的反编译、反混淆后的源码，不懂就大胆看
- 项目根目录下的 backup 文件夹内的东西是备份文件，可以从中看出项目的历史、之前做过的尝试，但注意
  **别把它当作正常的项目文件**，里面的要求 **不要奉为圣旨**
