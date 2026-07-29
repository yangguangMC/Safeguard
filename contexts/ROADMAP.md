# Safeguard 开发路线图 (Roadmap)

> 如果你作为 AI 助手正在阅读本文：注意本文的作用是 **记录长期规划而非指定短期目标**，并且本文的具体方案和实现排序
> **仅供参考**，实际会话中请以短期的明确的目标为准。

> **最后编辑于**: 2026-07-25

---

本路线图按 **依赖关系** 和 **实现难度** 拆解为可独立交付的任务单元。 每个任务单元均为"可工作单元"，适合目标明确的逐条实现。

**核心原则**：MVP 阶段采用"广度优先"策略——优先铺开一批 **功能多样、实现简单、高度独立**的检测项， 每种覆盖一类常见危险场景。
利用已有的 `Detection` 基类 + `GatedEvent` 自动门控 + 通用 Action（`ActionBarTitleAction`、`PlaySoundAction`、
`OutlineAction`）， 新增一个简单检测项只需编写检测逻辑本身。

---

## 以下每个阶段完成后和发布前，都要进行的任务

- ⚠️ 性能压测：确保实体扫描在高负载单机（300+ 实体、有高频红石、漏斗等）下不卡顿。
- ⚠️ 国际化（i18n）：完善 `zh_cn.json` / `en_us.json`，确保所有检测项和动作都有翻译。
- ⚠️ 代码清理：移除调试代码、统一注释风格、检查 FIXME/TODO 标记。
- ⚠️ 确保 `CONTEXT.md` 已经是最新的，能反映项目现状。

---

## 阶段 0：基础骨架（基建）

> 为后续所有功能提供运行环境、配置管理和模块框架。 **标注处表示已完工。**

### 0.1 项目初始化

- [x] 使用 Fabric Loom 创建 JDK 21 的 Gradle 项目。
- [x] 配置 `gradle.properties`。
- [x] 编写 `fabric.mod.json`（定义 `minecraft` 版本依赖、`fabric-loader` 版本、入口点）。
- [x] 创建客户端入口类 `Safeguard`（实现 `ClientModInitializer`），打印客户端启动日志。

### 0.2 配置系统

- [x] 实现 `ConfigManager`：使用 Gson 读写 `.minecraft/config/safeguard.json`。
- [x] 模组启动时加载配置，若无则生成默认配置。
- [x] 完整正确实现 `ConfigManager.save()` `ConfigManager.load()`。
- [ ] 实现配置变更监听，变更后自动序列化到磁盘。

### 0.3 保护系统框架

- [x] 定义 `Detection`（检测项基类）、`Action`（保护动作基类）。
- [x] 实现 `ProtectionManager`：持有 `protections`(Map)、`detectionRoot`/`actionRoot` 两棵 `SwitchTreeNode` 树。
- [x] 实现 `SwitchTreeNode`：树状开关容器，支持 `isEffectivelyEnabled()` 级联检查、`defaultEnabled` 默认值、O (1) 查找。
- [x] 实现 `GatedEvent`：门控事件包装器，在 Fabric Event 上叠加按所有者挂起/恢复能力，检测项无需手动检查启用状态。
- [x] 实现 `CategoryDefinition`：分类默认状态定义（record），供 `ProtectionManager` 预定义枝干节点默认启用状态。
- [x] 完成 Fabric 事件适配：`ClientPlayerTickEvents.START_TICK` 通过 Mixin 注入 `ClientPlayerEntity.tick()` HEAD。

---

## 阶段 1：MVP（最小可用产品）—— 广度优先

> **目标**：在短时间内覆盖尽可能多样的危险场景。每个检测项均为"简单检测"：
> 继承 `Detection` → `listen(GATED_START_TICK, handler)` → handler 中写几行检测逻辑 → 调用 `tryExecuteAction()` 触发已有
> Action。

### 1.1 新增基础检测项

- [x] **防苦力怕检测** (`combat/anti_creeper`)：8 格内检测最近苦力怕，显示距离/引信倒计时到 ActionBar；引信激活播放音效；近距离触发暂停/退出。
- [x] **防摔落检测** (`environment/anti_fall`)：防挖掘坠落（准星对准脚下方块 + 挖掘时检查下方 8
  格）；已坠落保护（fallDistance > 1.5 + 下方不安全 → 暂停/退出）；MLG 自动落地水（模拟下落轨迹 → 自动放水/黏液块）。
- [x] **防偷袭检测** (`combat/anti_ambush`)：每 5 帧检查 16 格内敌对生物/隐身玩家，ActionBar 显示数量 + 名称 +
  方向，OutlineAction 高亮不可见实体。 **已涵盖"怪物逼近警告"功能，不再单独列出。**
- [x] **弹射物追踪检测** (`combat/arrow_tracker`)：检测飞向玩家的弹射物（箭/火球等），角度偏差 < 10° 时 ActionBar 警告 +
  发射者信息。
- [x] **岩浆检测** (`environment/anti_lava`)：当玩家有挖掘意向时，检测周围 3×3 范围内是否有岩浆方块 → ActionBar 警告位置。
- [x] **低血量检测** (`status/low_health`)：当 `player.getHealth() / player.getMaxHealth() < 阈值`（默认 20%）时触发 → HUD
  边缘红闪 + ActionBar 警告 + 可选暂停/退出。
- [x] **着火检测** (`environment/on_fire`)：监听 `player.isOnFire()` 状态 → ActionBar 提示 + 搜索背包中可灭火物品。
- [x] **窒息检测** (`environment/anti_suffocation`)：检测玩家是否处于窒息状态、头顶是否有可坠落方块（沙砾/沙子等）→
  ActionBar 显示方块名称和距离。
- [x] **饥饿检测** (`status/low_hunger`)：当饥饿值低于阈值时触发 → ActionBar 提示 + 显示最佳食物建议。
- [x] **伤害检测** (`status/damage`)：受到任何伤害时触发 → 可选暂停/退出。

### 1.2 MVP（最小可用产品）公测前的打磨

- [x] 完善 `README.md`
- [ ] 完善 GitHub、模组发布页的文字、GitHub Release 文案、
- [ ] 自己在游戏中测试体验功能，发现与修复 Bug、发现可改进项并安排添加时间

---

## 阶段 2：系统完善与中等复杂度检测

> **目标**：修复已知问题，补充实现难度稍高或依赖较多基础设施的检测项。

### 2.1 配置与系统修复

- [ ] 实现检测项/保护动作特有的配置项
- [ ] 标准化 `Detection` → `Action` 调用方式（当前各 Detection 直接调用 Action 方法，较凌乱；考虑统一为事件驱动或回调接口）。
- [ ] 添加配置版本号，实现旧配置自动迁移逻辑。

### 2.2 新增检测项

- [ ] **装备耐久检测** (`status/low_durability`)：轮询玩家盔甲栏位和手持物品耐久度，低于阈值时触发 → ActionBar 提示 +
  对应槽位警告。
- [ ] **药效检测** (`status/effect_hud`)：监听 `player.getStatusEffects()`，在屏幕角落绘制当前 Buff/Debuff
  图标及剩余时间，时效不足时高亮警告。
- [ ] **光照检测** (`environment/anti_darkness`)：计算 `world.getLightLevel(player.getBlockPos())`，当亮度 ≤
  0（可生成怪物）时 → ActionBar 警告。 **注意：核心难点"计算最佳放火把位置"延后至阶段 3**，MVP 仅做亮度警告。

### 2.3 调整和扩展已有检测项的功能

- [ ] **防摔落检测** 补充：计算目标位置与当前位置间是否会造成死亡或致命伤（摔落伤害预测）。
- [ ] **低血量检测** 补充：计算斩杀线。
- [ ] **岩浆检测** 补充：若已经落入岩浆，提醒玩家可以自救的物品。
- [ ] 合并 **低血量检测** 和 **低饥饿值检测**

---

## 阶段 3：高级与主动保护

> **目标**：实现高风险/高复杂度的保护功能。所有主动操作类功能默认关闭。

### 3.1 弹射物弹道可视化

- [ ] 渲染弹射物飞行轨迹线（如箭矢、火球的预计路径），帮助玩家判断是否需要闪避。
- [ ] 高亮手持可生成弹射物物品且面向玩家的实体。

### 3.2 光照检测增强

- [ ] 计算最佳放火把位置（在玩家周围找到可放置火把且能最大化覆盖暗区的方块位置）→ 高亮提示。

### 3.3 完善保护功能

- [ ] 实现高风险功能需在配置中默认关闭，并增加二次确认提示。
- [ ] **着火检测**：补充： **自动灭火**：着火时自动使用水桶/寻找水源。
- [ ] **窒息检测** 补充： **窒息自动挖掘**：检测到窒息时自动寻找最快方式破坏头顶方块。
- [ ] **低饥饿值检测** 补充： **自动进食**：饥饿值低于阈值时自动食用快捷栏/背包中的最佳食物。
- [ ] **弹射物追踪检测** 补充：渲染弹射物飞行弹道线（渲染实现可能较复杂）。
