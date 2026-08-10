package top.yangguangmc.safeguard.protection.action;

import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 危险等级枚举，定义了保护动作的优先级、格式化规则和前缀翻译键。
 * <p>
 * 等级从高到低排列（{@link #ordinal()} 值越小越危险），
 * 每个等级自带的 {@link #style()} 规定了该等级下 ActionBar 文字的固定样式，
 * {@link #getPrefixKey()} 提供可国际化的前缀模板（如"危险：%s"/"警告：%s"）。
 * </p>
 */
public enum DangerLevel {
    /**
     * 致命危险 — 即将受到致命伤害（苦力怕引信已激活、已坠落、正在窒息）。
     */
    CRITICAL(ChatFormatting.RED, true, "danger.safeguard.level.critical"),
    /**
     * 高风险 — 存在明确但非立即致命的威胁（苦力怕近距离、弹射物飞向玩家、着火无灭火物品）。
     */
    HIGH(ChatFormatting.RED, false, "danger.safeguard.level.high"),
    /**
     * 中等风险 — 需要留意但不紧急（怪物偷袭、岩浆接近、着火有灭火方案）。
     */
    MEDIUM(ChatFormatting.GOLD, false, "danger.safeguard.level.medium"),
    /**
     * 低风险 — 轻微威胁（饥饿值低、苦力怕在远处、挖掘意图检测到坠落方块）。
     */
    LOW(ChatFormatting.YELLOW, false, "danger.safeguard.level.low"),
    /**
     * 纯信息 — 不构成威胁，仅为参考（背包食物推荐），无前缀。
     */
    INFO(ChatFormatting.GRAY, false, null);

    private final ChatFormatting color;
    private final boolean bold;
    @Nullable
    private final String prefixKey;

    DangerLevel(ChatFormatting color, boolean bold, @Nullable String prefixKey) {
        this.color = color;
        this.bold = bold;
        this.prefixKey = prefixKey;
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

    /**
     * 获取该危险等级的前缀翻译键。
     * <p>
     * 翻译值应为模板字符串，包含一个 {@code %s} 占位符表示消息内容。
     * 各语言可自由控制前缀文字和占位符位置。
     * 返回 {@code null} 表示无需前缀（如 {@link #INFO}）。
     * </p>
     *
     * @return 翻译键，或 {@code null} 表示无前缀
     */
    @Nullable
    public String getPrefixKey() {
        return prefixKey;
    }
}