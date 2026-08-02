package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

/**
 * ActionBar 标题保护动作 — 在屏幕上方覆盖层显示危险警告。
 * <p>
 * 所有检测项共享同一实例（通过 {@code passive/hud/action_bar_title} ID），
 * 内部使用静态字段进行跨检测项的优先级仲裁：同一 tick 内仅显示危险等级最高的消息，
 * 消除多检测项同时触发时的竞争闪烁问题。
 * </p>
 * <p>
 * 格式化职责完全由本类承担：各检测项传入纯文本内容（不含样式）和对应的
 * {@link DangerLevel}，本类按 DangerLevel 的统一规则套用样式后显示。
 * </p>
 */
public class ActionBarTitleAction extends Action {

    /**
     * 同一 tick 内当前已显示的最高危险等级
     */
    private static DangerLevel currentLevel = DangerLevel.LOW;

    /**
     * 同一 tick 内当前已显示的消息文本（原始，未套用样式）
     */
    private static Text currentRawMessage;

    static {
        currentRawMessage = Text.empty();
    }

    public ActionBarTitleAction() {
        super("passive/hud/action_bar_title");
    }

    /**
     * 尝试更新 ActionBar 标题。
     * <p>
     * 仅当 {@code level} 的危险等级不低于当前已显示等级时，才会真正更新显示。
     * 较低等级的消息会被静默忽略，确保重要警告不被次要信息覆盖。
     * </p>
     *
     * @param level   此消息的危险等级，用于决定是否覆盖当前消息及应用的样式
     * @param rawText 纯文本内容（不含颜色、加粗等样式），将由本方法套用 {@link DangerLevel#style()}
     * @param client  Minecraft 客户端实例，用于调用 {@code inGameHud.setOverlayMessage()}
     */
    public void updateTitle(@NotNull DangerLevel level, @NotNull Text rawText, @NotNull MinecraftClient client) {
        if (level.compareTo(currentLevel) > 0) return;  // 新消息危险等级不够高，忽略
        currentLevel = level;
        currentRawMessage = rawText;
        client.inGameHud.setOverlayMessage(rawText.copy().styled(style -> applyStyle(style, level.style())), false);
    }

    /**
     * 应用危险等级的样式到给定的 Style 上。
     * <p>
     * 子类可通过覆写此方法自定义样式应用逻辑。
     * </p>
     *
     * @param base      基础样式（通常为 {@link Style#EMPTY}）
     * @param fromLevel 来自 {@link DangerLevel#style()} 的样式
     * @return 组合后的样式
     */
    protected Style applyStyle(Style base, Style fromLevel) {
        return base.withColor(fromLevel.getColor()).withBold(fromLevel.isBold());
    }

    /**
     * 重置仲裁状态，应在每 tick 所有检测项处理开始前调用。
     * <p>
     * 由 {@link top.yangguangmc.safeguard.protection.ProtectionManager} 在分发事件前调用。
     * </p>
     */
    public static void resetForTick() {
        currentLevel = DangerLevel.LOW;
        currentRawMessage = Text.empty();
    }
}