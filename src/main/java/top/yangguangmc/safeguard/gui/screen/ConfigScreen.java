package top.yangguangmc.safeguard.gui.screen;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
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
                .title(Text.translatable("screen.safeguard.config").styled(style -> style.withColor(Formatting.GREEN)))
                .category(buildDetectionCategory(detectionRoot))
                .category(buildActionCategory(actionRoot))
                .category(buildLinksCategory(detectionRoot))
                .save(ctx.configManager()::trySave)
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory buildDetectionCategory(SwitchTreeNode root) {
        ConfigCategory.Builder category = ConfigCategory.createBuilder()
                .name(Text.translatable("category.safeguard.detections"));
        for (SwitchTreeNode child : sortChildren(root.getChildren())) {
            if (!child.isLeaf()) {
                category.group(buildGroupForBranch(child, ctx.protectionManager()::getDetectionName));
            }
        }
        return category.build();
    }

    private static ConfigCategory buildActionCategory(SwitchTreeNode root) {
        ConfigCategory.Builder category = ConfigCategory.createBuilder()
                .name(Text.translatable("category.safeguard.actions"));
        for (SwitchTreeNode child : sortChildren(root.getChildren())) {
            if (!child.isLeaf()) {
                category.group(buildGroupForBranch(child, ctx.protectionManager()::getActonName));
            }
        }
        return category.build();
    }

    private static ConfigCategory buildLinksCategory(SwitchTreeNode detectionRoot) {
        ConfigCategory.Builder category = ConfigCategory.createBuilder()
                .name(Text.translatable("category.safeguard.links"));
        detectionRoot.getNodeIds().stream()
                .filter(id -> detectionRoot.getNode(id).isLeaf())
                .map(id -> ctx.protectionManager().getDetection(id))
                .filter(detection -> !detection.getBoundActions().isEmpty())
                .forEach(detection -> category.group(buildLinkGroup(detection)));
        return category.build();
    }

    private static OptionGroup buildLinkGroup(Detection detection) {
        return OptionGroup.createBuilder()
                .name(ctx.protectionManager().getDetectionName(detection.getId()))
                .collapsed(true)
                .options(detection.getBoundActions().stream()
                        .map(action -> Option.<Boolean>createBuilder()
                                .name(Text.literal("-> ").append(ctx.protectionManager().getActonName(action.getId())))
                                .description(OptionDescription.createBuilder()
                                        .text(Text.translatable("screen.safeguard.default_true"))
                                        .text(Text.literal(detection.getId().toString())
                                                .append(" -> ")
                                                .append(action.getId().toString())
                                                .styled(style -> style.withColor(Formatting.DARK_GRAY)))
                                        .build())
                                .binding(true,
                                        () -> detection.isBindingEnabled(action.getId()),
                                        enabled -> detection.setBindingEnabled(action.getId(), enabled))
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .toList())
                .build();
    }

    private static OptionGroup buildGroupForBranch(SwitchTreeNode branch, Function<Identifier, Text> nameProvider) {
        OptionGroup.Builder group = OptionGroup.createBuilder()
                .name(nameProvider.apply(branch.getId()))
                .collapsed(false);
        group.option(createBranchOption(branch, nameProvider, 0));
        recurseChildren(branch, group, nameProvider);
        return group.build();
    }

    private static void recurseChildren(SwitchTreeNode branch, OptionGroup.Builder group,
                                        Function<Identifier, Text> nameProvider) {
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

    private static Option<Boolean> createBranchOption(SwitchTreeNode node, Function<Identifier, Text> nameProvider, int level) {
        return Option.<Boolean>createBuilder()
                .name(Text.literal("    ".repeat(level))
                        .append(nameProvider.apply(node.getId())).styled(style -> style.withColor(Formatting.GOLD)))
                .description(OptionDescription.createBuilder()
                        .text(Text.translatable("screen.safeguard.effective_value", formatBool(node.isEffectivelyEnabled())))
                        .text(Text.translatable("screen.safeguard.default_value", formatBool(node.getDefaultEnabled())))
                        .text(Text.translatable("screen.safeguard.branch_description"))
                        .text(Text.literal(node.getId().toString()).styled(style -> style.withColor(Formatting.DARK_GRAY)))
                        .build())
                .binding(node.getDefaultEnabled(), node::isEnabled, node::setEnabled)
                .controller(option -> BooleanControllerBuilder.create(option).coloured(true))
                .build();
    }

    private static Option<Boolean> createLeafOption(SwitchTreeNode node, Function<Identifier, Text> nameProvider, int level) {
        return Option.<Boolean>createBuilder()
                .name(Text.literal("    ".repeat(level)).append(nameProvider.apply(node.getId())))
                .description(OptionDescription.createBuilder()
                        .text(Text.translatable("screen.safeguard.effective_value", formatBool(node.isEffectivelyEnabled())))
                        .text(Text.translatable("screen.safeguard.default_value", formatBool(node.getDefaultEnabled())))
                        .text(Text.literal(node.getId().toString()).styled(style -> style.withColor(Formatting.DARK_GRAY)))
                        .build())
                .binding(node.getDefaultEnabled(), node::isEnabled, node::setEnabled)
                .controller(option -> BooleanControllerBuilder.create(option).coloured(true))
                .build();
    }

    private static LabelOption createBranchLabel(SwitchTreeNode node, Function<Identifier, Text> nameProvider, int level) {
        return LabelOption.create(
                Text.literal("    ".repeat(level) + "── ")
                        .append(nameProvider.apply(node.getId()))
                        .append(" ──")
                        .styled(style -> style.withColor(Formatting.DARK_AQUA))
        );
    }

    private static List<SwitchTreeNode> sortChildren(Collection<SwitchTreeNode> children) {
        List<SwitchTreeNode> sorted = new ArrayList<>(children);
        sorted.sort(Comparator.comparing(SwitchTreeNode::getIdName));
        return sorted;
    }

    private static String formatBool(boolean bool) {
        return bool
                ? Formatting.GREEN + ScreenTexts.ON.getString()
                : Formatting.RED + ScreenTexts.OFF.getString();
    }
}
