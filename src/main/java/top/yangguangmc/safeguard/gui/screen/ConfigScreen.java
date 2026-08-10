package top.yangguangmc.safeguard.gui.screen;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.detection.Detection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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
                category.group(buildGroupForBranch(child, ctx.protectionManager()::getDetectionName));
            }
        }
        return category.build();
    }

    // ==================== Action Category ====================

    private static ConfigCategory buildActionCategory(SwitchTreeNode root) {
        ConfigCategory.Builder category = ConfigCategory.createBuilder()
                .name(Component.translatable("category.safeguard.actions"));
        for (SwitchTreeNode child : sortChildren(root.getChildren())) {
            if (!child.isLeaf()) {
                category.group(buildGroupForBranch(child, ctx.protectionManager()::getActonName));
            }
        }
        return category.build();
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
     * 每个检测项一个 OptionGroup，组名即为检测项名，其下陈列与其绑定的保护动作
     */
    private static OptionGroup buildLinkGroup(Detection detection) {
        return OptionGroup.createBuilder()
                .name(ctx.protectionManager().getDetectionName(detection.getId()))
                .collapsed(true)
                .options(detection.getBoundActions().stream()
                        .map(action -> Option.<Boolean>createBuilder()
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
                                .build())
                        .toList())
                .build();
    }

    // ==================== Recursive Build Logic ====================

    /**
     * 为根的直接子枝干节点创建一个 OptionGroup。
     * OptionGroup 的名称即为该枝干节点的翻译名，默认展开。
     * 组内包含枝干自身开关及其所有后代节点。
     */
    private static OptionGroup buildGroupForBranch(SwitchTreeNode branch, Function<ResourceLocation, Component> nameProvider) {
        OptionGroup.Builder group = OptionGroup.createBuilder()
                .name(nameProvider.apply(branch.getId()))
                .collapsed(false);
        group.option(createBranchOption(branch, nameProvider, 0));
        recurseChildren(branch, group, nameProvider);
        return group.build();
    }

    /**
     * 递归将枝干节点的所有子节点展开到同一个 OptionGroup 中。
     * 叶节点直接作为 Option；更深层枝干先以 LabelOption 分隔，再插入自身开关，最后递归。
     */
    private static void recurseChildren(SwitchTreeNode branch, OptionGroup.Builder group,
                                        Function<ResourceLocation, Component> nameProvider) {
        for (SwitchTreeNode child : sortChildren(branch.getChildren())) {
            int level = child.getLevel();
            if (child.isLeaf()) {
                group.option(createLeafOption(child, nameProvider, level));
            } else {
                group.option(createBranchLabel(child, nameProvider, level));
                group.option(createBranchOption(child, nameProvider, level));
                recurseChildren(child, group, nameProvider);
            }
        }
    }

    // ==================== Option Factory Methods ====================

    /**
     * 枝干节点开关：金色名称突出其为分类控制节点。缩进 = level * 4 空格
     */
    private static Option<Boolean> createBranchOption(SwitchTreeNode node, Function<ResourceLocation, Component> nameProvider, int level) {
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
    private static Option<Boolean> createLeafOption(SwitchTreeNode node, Function<ResourceLocation, Component> nameProvider, int level) {
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
    private static LabelOption createBranchLabel(SwitchTreeNode node, Function<ResourceLocation, Component> nameProvider, int level) {
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