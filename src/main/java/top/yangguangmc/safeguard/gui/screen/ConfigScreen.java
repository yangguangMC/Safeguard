package top.yangguangmc.safeguard.gui.screen;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.detection.Detection;
import top.yangguangmc.safeguard.protection.option.ConfigOption;
import top.yangguangmc.safeguard.protection.option.DoubleOption;
import top.yangguangmc.safeguard.protection.option.IntOption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfigScreen {
    private static ModContext ctx;

    private ConfigScreen() {
        throw new AssertionError();
    }

    public static void init(ModContext ctx) {
        ConfigScreen.ctx = ctx;
    }

    public static Screen create(Screen parent) {
        SwitchTreeNode detectionRoot = ctx.protectionManager().getDetectionStatesRoot();
        SwitchTreeNode actionRoot = ctx.protectionManager().getActionStatesRoot();
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("screen.safeguard.config").withStyle(style -> style.withColor(ChatFormatting.GREEN)))
                .category(buildDetectionCategory(detectionRoot))
                .category(buildActionCategory(actionRoot))
                .category(buildLinksCategory(detectionRoot))
                .save(ctx.configManager()::trySave)
                .build()
                .generateScreen(parent);
    }

    // ==================== Detection Category ====================

    private static ConfigCategory buildDetectionCategory(SwitchTreeNode root) {
        ConfigCategory.Builder category = ConfigCategory.createBuilder()
                .name(Component.translatable("category.safeguard.detections"));
        for (SwitchTreeNode child : sortChildren(root.getChildren())) {
            if (!child.isLeaf()) {
                category.group(buildGroupForBranch(child, ctx.protectionManager()::getDetectionName, ConfigScreen::detectionLeafOptions));
            }
        }
        return category.build();
    }

    /**
     * 某个检测项叶节点自身的配置项（{@code ConfigOption}），直接绑定到该检测项的唯一实例。
     */
    private static List<Option<?>> detectionLeafOptions(SwitchTreeNode leaf) {
        Detection detection = ctx.protectionManager().getDetection(leaf.getId());
        List<Option<?>> result = new ArrayList<>();
        for (ConfigOption<?> option : detection.getOptions()) {
            result.add(buildDirectOption(option, leaf.getId(),
                    ctx.protectionManager().getDetectionOptionName(leaf.getId(), option.key())));
        }
        return result;
    }

    // ==================== Action Category ====================

    private static ConfigCategory buildActionCategory(SwitchTreeNode root) {
        ConfigCategory.Builder category = ConfigCategory.createBuilder()
                .name(Component.translatable("category.safeguard.actions"));
        for (SwitchTreeNode child : sortChildren(root.getChildren())) {
            if (!child.isLeaf()) {
                category.group(buildGroupForBranch(child, ctx.protectionManager()::getActonName, ConfigScreen::actionLeafOptions));
            }
        }
        return category.build();
    }

    /**
     * 某个动作叶节点（ID）的全局配置项（非 pairScoped）。
     * <p>
     * 同一动作 ID 可能对应多个检测项各自 {@code new} 出来的实例，读取取第一个实例的当前值，
     * 写入则扇出到全部实例，保持它们同步（见 {@link #buildActionOption}）。
     * </p>
     */
    private static List<Option<?>> actionLeafOptions(SwitchTreeNode leaf) {
        List<Action> instances = ctx.protectionManager().getActionInstances(leaf.getId());
        if (instances.isEmpty()) return List.of();
        List<Option<?>> result = new ArrayList<>();
        for (ConfigOption<?> option : instances.getFirst().getOptions()) {
            if (option.isPairScoped()) continue;
            result.add(buildActionOption(option, instances, leaf.getId(),
                    ctx.protectionManager().getActionOptionName(leaf.getId(), option.key())));
        }
        return result;
    }

    // ==================== Links Category ====================

    private static ConfigCategory buildLinksCategory(SwitchTreeNode detectionRoot) {
        ConfigCategory.Builder category = ConfigCategory.createBuilder()
                .name(Component.translatable("category.safeguard.links"));
        detectionRoot.getNodeIds().stream()
                .filter(id -> detectionRoot.getNode(id).isLeaf())
                .map(id -> ctx.protectionManager().getDetection(id))
                .filter(detection -> !detection.getBoundActions().isEmpty())
                .forEach(detection -> category.group(buildLinkGroup(detection)));
        return category.build();
    }

    /**
     * 每个检测项一个 OptionGroup，组名即为检测项名，其下陈列与其绑定的保护动作；
     * 若某个绑定的动作实例持有"检测项-动作对专属"配置项（{@link ConfigOption#isPairScoped()}），
     * 紧跟在对应绑定开关之后展示，随其开关状态置灰。
     */
    private static OptionGroup buildLinkGroup(Detection detection) {
        OptionGroup.Builder group = OptionGroup.createBuilder()
                .name(ctx.protectionManager().getDetectionName(detection.getId()))
                .collapsed(true);
        for (Action action : detection.getBoundActions()) {
            Option<Boolean> bindingOption = Option.<Boolean>createBuilder()
                    .name(Component.literal("-> ").append(ctx.protectionManager().getActonName(action.getId())))
                    .description(OptionDescription.createBuilder()
                            .text(Component.translatable("screen.safeguard.default_true"))
                            .text(Component.literal(detection.getId().toString())
                                    .append(" -> ")
                                    .append(action.getId().toString())
                                    .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)))
                            .build())
                    .binding(true,
                            () -> detection.isBindingEnabled(action.getId()),
                            enabled -> detection.setBindingEnabled(action.getId(), enabled))
                    .controller(TickBoxControllerBuilder::create)
                    .build();
            group.option(bindingOption);

            List<Option<Boolean>> chain = List.of(bindingOption);
            for (ConfigOption<?> pairOption : action.getOptions().stream().filter(ConfigOption::isPairScoped).toList()) {
                Option<?> built = buildDirectOption(pairOption, action.getId(),
                        Component.literal("    ").append(ctx.protectionManager().getActionOptionName(action.getId(), pairOption.key())));
                group.option(built);
                wireAvailability(built, chain);
            }
        }
        return group.build();
    }

    // ==================== Recursive Build Logic ====================

    /**
     * 为根的直接子枝干节点创建一个 OptionGroup。
     * OptionGroup 的名称即为该枝干节点的翻译名，默认展开。
     * 组内包含枝干自身开关及其所有后代节点。
     */
    private static OptionGroup buildGroupForBranch(SwitchTreeNode branch, Function<Identifier, Component> nameProvider,
                                                    Function<SwitchTreeNode, List<Option<?>>> leafOptionsProvider) {
        OptionGroup.Builder group = OptionGroup.createBuilder()
                .name(nameProvider.apply(branch.getId()))
                .collapsed(false);
        Option<Boolean> branchOption = createBranchOption(branch, nameProvider, 0);
        group.option(branchOption);
        recurseChildren(branch, group, nameProvider, leafOptionsProvider, List.of(branchOption));
        return group.build();
    }

    /**
     * 递归将枝干节点的所有子节点展开到同一个 OptionGroup 中。
     * 叶节点直接作为 Option，其后紧跟 {@code leafOptionsProvider} 提供的该叶节点专属配置项
     * （缩进多一级，且随整条祖先开关链的"有效启用状态"联动置灰）；
     * 更深层枝干先以 LabelOption 分隔，再插入自身开关，最后递归。
     */
    private static void recurseChildren(SwitchTreeNode branch, OptionGroup.Builder group,
                                        Function<Identifier, Component> nameProvider,
                                        Function<SwitchTreeNode, List<Option<?>>> leafOptionsProvider,
                                        List<Option<Boolean>> ancestorToggles) {
        for (SwitchTreeNode child : sortChildren(branch.getChildren())) {
            int level = child.getLevel();
            if (child.isLeaf()) {
                Option<Boolean> leafToggle = createLeafOption(child, nameProvider, level);
                group.option(leafToggle);
                List<Option<Boolean>> chain = withAppended(ancestorToggles, leafToggle);
                for (Option<?> extra : leafOptionsProvider.apply(child)) {
                    group.option(extra);
                    wireAvailability(extra, chain);
                }
            } else {
                group.option(createBranchLabel(child, nameProvider, level));
                Option<Boolean> branchToggle = createBranchOption(child, nameProvider, level);
                group.option(branchToggle);
                List<Option<Boolean>> chain = withAppended(ancestorToggles, branchToggle);
                recurseChildren(child, group, nameProvider, leafOptionsProvider, chain);
            }
        }
    }

    // ==================== Config Option Factory Methods ====================

    /**
     * 构建一个直接绑定到给定 {@link ConfigOption} 的 YACL {@link Option}（单一宿主实例场景：
     * 检测项自身配置项、动作的成对专属配置项）。
     */
    private static <T> Option<?> buildDirectOption(ConfigOption<T> option, Identifier ownerId, Component name) {
        return option.buildYaclOption(name, buildOptionDescription(option, ownerId), option::get, option::set);
    }

    /**
     * 构建一个动作全局配置项对应的 YACL {@link Option}：读取代表实例（第一个）的当前值，
     * 写入时扇出到全部同 ID 实例，使它们保持同步。
     */
    private static <T> Option<?> buildActionOption(ConfigOption<T> option, List<Action> instances, Identifier ownerId, Component name) {
        String key = option.key();
        Consumer<T> setter = value -> {
            for (Action instance : instances) {
                instance.getOptions().stream()
                        .filter(o -> o.key().equals(key))
                        .findFirst()
                        .ifPresent(target -> setOptionValueUnchecked(target, value));
            }
        };
        return option.buildYaclOption(name, buildOptionDescription(option, ownerId), option::get, setter);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setOptionValueUnchecked(ConfigOption<?> option, T value) {
        ((ConfigOption<T>) option).set(value);
    }

    /**
     * 拼装配置项的描述面板：默认值、（有界数值类型的）取值范围、灰色的完整键名。
     */
    private static <T> OptionDescription buildOptionDescription(ConfigOption<T> option, Identifier ownerId) {
        OptionDescription.Builder builder = OptionDescription.createBuilder()
                .text(Component.translatable("screen.safeguard.default_value", option.formatValue(option.defaultValue())));
        if (option instanceof IntOption intOption) {
            builder.text(Component.translatable("screen.safeguard.range",
                    Component.literal(String.valueOf(intOption.min())), Component.literal(String.valueOf(intOption.max()))));
        } else if (option instanceof DoubleOption doubleOption) {
            builder.text(Component.translatable("screen.safeguard.range",
                    doubleOption.formatValue(doubleOption.min()), doubleOption.formatValue(doubleOption.max())));
        }
        builder.text(Component.literal(ownerId + "." + option.key()).withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)));
        return builder.build();
    }

    /**
     * 让 {@code dependent} 的可用性跟随一条开关链（祖先分支开关 + 自身叶开关，或单个绑定开关）的
     * "全部为真"状态，随 GUI 中挂起（尚未应用）的改动即时联动，无需保存即可预览。
     */
    private static void wireAvailability(Option<?> dependent, List<Option<Boolean>> chain) {
        Runnable update = () -> dependent.setAvailable(chain.stream().allMatch(Option::pendingValue));
        update.run();
        for (Option<Boolean> toggle : chain) {
            toggle.addEventListener((opt, event) -> {
                if (event == OptionEventListener.Event.STATE_CHANGE) update.run();
            });
        }
    }

    private static List<Option<Boolean>> withAppended(List<Option<Boolean>> list, Option<Boolean> extra) {
        List<Option<Boolean>> result = new ArrayList<>(list);
        result.add(extra);
        return result;
    }

    // ==================== Switch Option Factory Methods ====================

    /**
     * 枝干节点开关：金色名称突出其为分类控制节点。缩进 = level * 4 空格
     */
    private static Option<Boolean> createBranchOption(SwitchTreeNode node, Function<Identifier, Component> nameProvider, int level) {
        return Option.<Boolean>createBuilder()
                .name(Component.literal("    ".repeat(level))
                        .append(nameProvider.apply(node.getId())).withStyle(style -> style.withColor(ChatFormatting.GOLD)))
                .description(OptionDescription.createBuilder()
                        .text(Component.translatable("screen.safeguard.effective_value", formatBool(node.isEffectivelyEnabled())))
                        .text(Component.translatable("screen.safeguard.default_value", formatBool(node.getDefaultEnabled())))
                        .text(Component.translatable("screen.safeguard.branch_description"))
                        .text(Component.literal(node.getId().toString()).withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)))
                        .build())
                .binding(node.getDefaultEnabled(), node::isEnabled, node::setEnabled)
                .controller(option -> BooleanControllerBuilder.create(option).coloured(true))
                .build();
    }

    /**
     * 叶节点开关：缩进 = level * 4 空格，视觉上与父枝干区分
     */
    private static Option<Boolean> createLeafOption(SwitchTreeNode node, Function<Identifier, Component> nameProvider, int level) {
        return Option.<Boolean>createBuilder()
                .name(Component.literal("    ".repeat(level)).append(nameProvider.apply(node.getId())))
                .description(OptionDescription.createBuilder()
                        .text(Component.translatable("screen.safeguard.effective_value", formatBool(node.isEffectivelyEnabled())))
                        .text(Component.translatable("screen.safeguard.default_value", formatBool(node.getDefaultEnabled())))
                        .text(Component.literal(node.getId().toString()).withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)))
                        .build())
                .binding(node.getDefaultEnabled(), node::isEnabled, node::setEnabled)
                .controller(option -> BooleanControllerBuilder.create(option).coloured(true))
                .build();
    }

    /**
     * 更深层枝干的分隔标签：青色装饰线，缩进 = level * 4 空格
     */
    private static LabelOption createBranchLabel(SwitchTreeNode node, Function<Identifier, Component> nameProvider, int level) {
        return LabelOption.create(
                Component.literal("    ".repeat(level) + "── ")
                        .append(nameProvider.apply(node.getId()))
                        .append(" ──")
                        .withStyle(style -> style.withColor(ChatFormatting.DARK_AQUA))
        );
    }

    // ==================== Utility ====================

    /**
     * 按节点路径末尾名称排序，确保同层级条目顺序一致
     */
    private static List<SwitchTreeNode> sortChildren(Collection<SwitchTreeNode> children) {
        List<SwitchTreeNode> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparing(SwitchTreeNode::getIdName));
        return sorted;
    }

    private static String formatBool(boolean bool) {
        return bool
                ? ChatFormatting.GREEN + CommonComponents.OPTION_ON.getString()
                : ChatFormatting.RED + CommonComponents.OPTION_OFF.getString();
    }
}
