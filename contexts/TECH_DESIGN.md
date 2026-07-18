# SafeGuard 技术设计文档（TECH_DESIGN）

## 一、技术栈选择

| 组件               | 选型                           | 理由                                      |
|--------------------|--------------------------------|-------------------------------------------|
| **Minecraft 版本** | 1.21.11                        | 高版本长期支持，社区活跃，Fabric 文档完善 |
| **模组加载器**     | Fabric Loader 0.19.0+          | 轻量、现代、社区驱动                      |
| **模组 API**       | Fabric API 0.141.0+            | 提供事件系统、网络、渲染等核心能力        |
| **Java 版本**      | JDK 21                         | Fabric 1.21 要求                          |
| **构建工具**       | Gradle 8.x + Fabric Loom 1.17+ | 官方推荐，支持增量编译和热重载            |
| **配置库**         | YetAnotherConfigLib 3.8+       | 提供可视化配置界面，集成简单              |
| **模组菜单**       | Mod Menu 17+                   | 提供配置入口，用户体验友好                |
| **序列化**         | Gson                           | 配置文件读写                              |
| **日志**           | SLF4J + Log4j                  | Fabric 内置，统一日志输出                 |

## 二、项目结构

```
Safe Guard/
    .idea/
    contexts/       # 含有项目概述相关文件
        PRD.md
        RESEARCH.md
        TECH_DESIGN.md
    gradle/
    src/
        main/
            java/       # 源代码根目录
                top/
                    yangguangmc/
                        safeguard/
                            gui/            # GUI 相关
                                ...
                            injection/      # 注入相关，如 Mixin 等
                                ...
                            protection/     # 保护相关，核心功能
                                action/         # 保护动作
                                    ...
                                detection/      # 检测项
                                    ...
                                event/          # 供保护相关功能使用的事件
                                    ...
                                ProtectionManager.java  # 管理所有检测项和保护动作
                                SwitchTreeItem.java     # 一个能配合 SwitchTreeNode 的规范
                                SwitchTreeNode.java     # 实现灵活开关配置的树结构
                            util/           # 实用工具
                                ...
                            ModContext.java     # 全局上下文容器
                            SafeGuard.java      # 主类
                            SafeGuardCommand.java           # 命令相关
                            SafeGuardModMenuApiImpl.java    # Mod Menu API 实现
            resources/  # 资源文件根目录
                ...
    build.gradle
    gradle.properties
    LICENSE.txt
    log4j-dev.xml
    README.md
    settings.gradle
```

## 三、数据模型

### 3.1 配置数据模型（ModConfig）

%% TODO: 配置到底怎么写 %%

## 四、关键技术点

### 4.1 事件监听机制

使用 Fabric API 的事件系统进行游戏状态监听，并做好启用判断：

```java

public class ExampleDetection extends Detection {
    public ExampleDetection() {
        super("category/to/the/detection/id", new ExampleAction());
    }

    @Override
    public void init(ModContext ctx) {
        super.init(ctx);
        ExampleEvents.EXAMPLE_CALLBACK.register((arg1, arg2) -> {
            if (getStateNode().isEffectivelyEnabled()) onExampleEvent(arg1, arg2);
        });
    }

    private void onExampleEvent(Type1 arg1, Type2 arg2) {
        // ...
    }
}
```

### 4.3 HUD 渲染

%% TODO: 缺乏统一健壮的规范 %%

### 4.4 配置持久化

- 配置文件存储在 `.minecraft/config/safeguard.json`
- 使用 Cloth Config API 的 `ConfigBuilder` 构建配置界面
- 配置变更时自动保存至文件
- 启动时加载配置，若文件不存在则创建默认配置

### 4.5 性能优化策略

| 策略             | 说明                                                                |
|------------------|---------------------------------------------------------------------|
| **检测频率控制** | 不同检测项使用不同频率（如实体扫描每 5 tick 一次，位置检测每 tick） |
| **空间分区**     | 实体扫描时仅检测玩家周围一定半径内的实体                            |
| **缓存机制**     | 缓存方块检测结果，避免重复计算                                      |
| **异步处理**     | 非关键路径的检测可异步执行                                          |
| **冷却机制**     | 同一警告类型有冷却时间，避免刷屏                                    |

### 4.6 Mod Menu 集成

实现 `ModMenuApi` 接口以集成配置入口：

```java

public class SafeGuardModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::create;
    }
}
```

### 4.7 错误隔离

单个检测模块的异常不应影响整个模组：

%% TODO %%

---

*本文档版本 v1.0，最后更新于 2026 年 7 月*
