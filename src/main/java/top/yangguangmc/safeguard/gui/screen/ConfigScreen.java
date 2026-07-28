package top.yangguangmc.safeguard.gui.screen;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;

public class ConfigScreen {
    private static ModContext ctx;

    public static void init(ModContext ctx) {
        ConfigScreen.ctx = ctx;
    }

    public static Screen create(Screen parent) {
        SwitchTreeNode root1 = ctx.protectionManager().getDetectionStatesRoot();
        SwitchTreeNode root2 = ctx.protectionManager().getActionStatesRoot();
        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("screen.safeguard.config").styled(style -> style.withColor(Formatting.GREEN)))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("category.safeguard.detections"))
                        .group(OptionGroup.createBuilder()
                                .options(root1.getNodeIds().stream()
                                        .map(root1::getNode)
                                        .map(node -> Option.<Boolean>createBuilder()
                                                .name(Text.literal("    ".repeat(node.getLevel())).append(ctx.protectionManager().getDetectionName(node.getId())))
                                                .description(OptionDescription.createBuilder()
                                                        .text(Text.translatable("screen.safeguard.effective_value", node.isEffectivelyEnabled()))
                                                        .text(Text.translatable("screen.safeguard.default_value", node.getDefaultEnabled()))
                                                        .text(Text.literal(node.getId().toString()).styled(style -> style.withColor(Formatting.GRAY)))
                                                        .build())
                                                .binding(node.getDefaultEnabled(), node::isEnabled, node::setEnabled)
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .toList())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("category.safeguard.actions"))
                        .group(OptionGroup.createBuilder()
                                .options(root2.getNodeIds().stream()
                                        .map(root2::getNode)
                                        .map(node -> Option.<Boolean>createBuilder()
                                                .name(Text.literal("    ".repeat(node.getLevel())).append(ctx.protectionManager().getActonName(node.getId())))
                                                .description(OptionDescription.createBuilder()
                                                        .text(Text.translatable("screen.safeguard.effective_value", node.isEffectivelyEnabled()))
                                                        .text(Text.translatable("screen.safeguard.default_value", node.getDefaultEnabled()))
                                                        .text(Text.literal(node.getId().toString()).styled(style -> style.withColor(Formatting.GRAY)))
                                                        .build())
                                                .binding(node.getDefaultEnabled(), node::isEnabled, node::setEnabled)
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .toList())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("category.safeguard.links"))
                        .group(OptionGroup.createBuilder()
                                .options(root1.getNodeIds().stream()
                                        .filter(id -> root1.getNode(id).isLeaf())
                                        .map(id -> ctx.protectionManager().getDetection(id))
                                        .flatMap(detection -> detection.getBoundActions().stream())
                                        .map(action -> Option.<Boolean>createBuilder()
                                                .name(Text.translatable("screen.safeguard.link_format", ctx.protectionManager().getDetectionName(action.getParent().getId()), ctx.protectionManager().getActonName(action.getId())))
                                                .description(OptionDescription.createBuilder()
                                                        .text(Text.translatable("screen.safeguard.default_true"))
                                                        .text(Text.literal(action.getParent().getId().toString()).append(" --> ").append(action.getId().toString()))
                                                        .build())
                                                .binding(true, () -> action.getParent().isBindingEnabled(action.getId()), enabled -> action.getParent().setBindingEnabled(action.getId(), enabled))
                                                .controller(TickBoxControllerBuilder::create)
                                                .build())
                                        .toList())
                                .build())
                        .build())
                .build()
                .generateScreen(parent);
    }
}