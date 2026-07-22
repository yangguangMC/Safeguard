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
                        .name(Text.literal("检测项"))
                        .group(OptionGroup.createBuilder()
                                .options(root1.getNodeIds().stream()
                                        .map(root1::getNode)
                                        .map(node -> Option.<Boolean>createBuilder()
                                                .name(Text.literal("    ".repeat((int) node.getId().getPath().chars().filter(c -> c == '/').count())).append(ctx.protectionManager().getDetectionName(node.getId())))
                                                .description(OptionDescription.createBuilder()
                                                        .text(Text.literal("有效值：" + node.isEffectivelyEnabled()))
                                                        .text(Text.literal("默认值：" + (node.isLeaf() ? ctx.protectionManager().getDetectionDefaultState(node.getId()) : true)))
                                                        .text(Text.literal(node.getId().toString()).styled(style -> style.withColor(Formatting.GRAY)))
                                                        .build())
                                                .binding(node.isLeaf() ? ctx.protectionManager().getDetectionDefaultState(node.getId()) : true, node::isEnabled, node::setEnabled)
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .toList())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("保护动作"))
                        .group(OptionGroup.createBuilder()
                                .options(root2.getNodeIds().stream()
                                        .map(root2::getNode)
                                        .map(node -> Option.<Boolean>createBuilder()
                                                .name(Text.literal("    ".repeat((int) node.getId().getPath().chars().filter(c -> c == '/').count())).append(ctx.protectionManager().getActonName(node.getId())))
                                                .description(OptionDescription.createBuilder()
                                                        .text(Text.literal("有效值：" + node.isEffectivelyEnabled()))
                                                        .text(Text.literal("默认值：" + (node.isLeaf() ? ctx.protectionManager().getActionDefaultState(node.getId()) : true)))
                                                        .text(Text.literal(node.getId().toString()).styled(style -> style.withColor(Formatting.GRAY)))
                                                        .build())
                                                .binding(node.isLeaf() ? ctx.protectionManager().getActionDefaultState(node.getId()) : true, node::isEnabled, node::setEnabled)
                                                .controller(BooleanControllerBuilder::create)
                                                .build())
                                        .toList())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("链接"))
                        .group(OptionGroup.createBuilder()
                                .options(root1.getNodeIds().stream()
                                        .filter(id -> root1.getNode(id).isLeaf())
                                        .map(id -> ctx.protectionManager().getDetection(id))
                                        .flatMap(detection -> detection.getBoundActions().stream())
                                        .map(action -> Option.<Boolean>createBuilder()
                                                .name(ctx.protectionManager().getDetectionName(action.getParent().getId()).copy().append(" --> ").append(ctx.protectionManager().getActonName(action.getId())))
                                                .description(OptionDescription.createBuilder()
                                                        .text(Text.literal("默认值：true"))
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
