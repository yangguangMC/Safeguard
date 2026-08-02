package top.yangguangmc.safeguard.protection.action;

import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

/**
 * 危险等级枚举，定义了保护动作的优先级和格式化规则。
 * <p>
 * 等级从高到低排列（{@link #ordinal()} 值越小越危险），
 * 每个等级自带的 {@link #style()} 规定了该等级下 ActionBar 文字的固定样式。
 * </p>
 */
public enum DangerLevel {
    /**
     * 致命危险 — 即将受到致命伤害（苦力怕引信已激活、已坠落、正在窒息）。
     */
    CRITICAL(Formatting.RED, true),
    /**
     * 高风险 — 存在明确但非立即致命的威胁（苦力怕近距离、弹射物飞向玩家、着火无灭火物品）。
     */
    HIGH(Formatting.RED, false),
    /**
     * 中等风险 — 需要留意但不紧急（怪物偷袭、岩浆接近、着火有灭火方案）。
     */
    MEDIUM(Formatting.GOLD, false),
    /**
     * 低风险 — 轻微威胁（饥饿值低、苦力怕在远处、挖掘意图检测到坠落方块）。
     */
    LOW(Formatting.YELLOW, false),
    /**
     * 纯信息 — 不构成威胁，仅为参考（背包食物推荐）。
     */
    INFO(Formatting.GRAY, false);

    private final Formatting color;
    private final boolean bold;

    DangerLevel(Formatting color, boolean bold) {
        this.color = color;
        this.bold = bold;
    }

    /**
     * 获取该危险等级对应的文本样式。
     *
     * @return 包含颜色和加粗设置的 {@link Style}
     */
    public @NotNull Style style() {
        Style style = Style.EMPTY.withColor(color);
        return bold ? style.withBold(true) : style;
    }
}