package top.yangguangmc.safeguard;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.gui.screen.ConfigScreen;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;

public class SafeGuardCommand {
    private static ModContext ctx;

    public static void init(ModContext ctx) {
        SafeGuardCommand.ctx = ctx;
        ClientCommandRegistrationCallback.EVENT.register(SafeGuardCommand::register);
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
                ClientCommandManager.literal(ModContext.MOD_ID)
                        .then(
                                ClientCommandManager.literal("screen")
                                        .executes(context -> {
                                            MinecraftClient client = context.getSource().getClient();
                                            client.send(() -> client.setScreen(ConfigScreen.create(client.currentScreen)));
                                            return Command.SINGLE_SUCCESS;
                                        })
                        ).then(
                                ClientCommandManager.literal("detection")
                                        .then(
                                                ClientCommandManager.argument("id", IdentifierArgumentType.identifier())
                                                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(ctx.protectionManager().getDetectionStatesRoot().getNodeIds(), builder))
                                                        .executes(context -> {
                                                            Identifier id = context.getArgument("id", Identifier.class);
                                                            SwitchTreeNode detection = ctx.protectionManager().getDetectionStatesRoot().getNode(id);
                                                            if (detection != null) {
                                                                context.getSource().sendFeedback(newText(Text.literal("检测项")).append(ctx.protectionManager().getDetectionName(id)).append("当前为").append(String.valueOf(detection.isEnabled())).append("，有效值为").append(String.valueOf(detection.isEffectivelyEnabled())));
                                                                return Command.SINGLE_SUCCESS;
                                                            } else {
                                                                context.getSource().sendError(newText(Text.literal("找不到ID为 %s 的检测项！".formatted(id))));
                                                                return 0;
                                                            }
                                                        }).then(
                                                                ClientCommandManager.argument("state", BoolArgumentType.bool())
                                                                        .executes(context -> {
                                                                            Identifier id = context.getArgument("id", Identifier.class);
                                                                            boolean state = BoolArgumentType.getBool(context, "state");
                                                                            SwitchTreeNode detection = ctx.protectionManager().getDetectionStatesRoot().getNode(id);
                                                                            if (detection != null) {
                                                                                detection.setEnabled(state);
                                                                                context.getSource().sendFeedback(newText(Text.literal("已将检测项")).append(ctx.protectionManager().getDetectionName(id)).append("设为").append(String.valueOf(state)));
                                                                                return Command.SINGLE_SUCCESS;
                                                                            } else {
                                                                                context.getSource().sendError(newText(Text.literal("找不到ID为 %s 的检测项！".formatted(id))));
                                                                                return 0;
                                                                            }
                                                                        })
                                                        )
                                        )
                        ).then(
                                ClientCommandManager.literal("action")
                                        .then(
                                                ClientCommandManager.argument("id", IdentifierArgumentType.identifier())
                                                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(ctx.protectionManager().getActionStatesRoot().getNodeIds(), builder))
                                                        .executes(context -> {
                                                            Identifier id = context.getArgument("id", Identifier.class);
                                                            SwitchTreeNode action = ctx.protectionManager().getActionStatesRoot().getNode(id);
                                                            if (action != null) {
                                                                context.getSource().sendFeedback(newText(Text.literal("保护动作")).append(ctx.protectionManager().getActonName(id)).append("当前为").append(String.valueOf(action.isEnabled())).append("，有效值为").append(String.valueOf(action.isEffectivelyEnabled())));
                                                                return Command.SINGLE_SUCCESS;
                                                            } else {
                                                                context.getSource().sendError(newText(Text.literal("找不到ID为 %s 的保护动作！".formatted(id))));
                                                                return 0;
                                                            }
                                                        }).then(
                                                                ClientCommandManager.argument("state", BoolArgumentType.bool())
                                                                        .executes(context -> {
                                                                            Identifier id = context.getArgument("id", Identifier.class);
                                                                            boolean state = BoolArgumentType.getBool(context, "state");
                                                                            SwitchTreeNode action = ctx.protectionManager().getActionStatesRoot().getNode(id);
                                                                            if (action != null) {
                                                                                action.setEnabled(state);
                                                                                context.getSource().sendFeedback(newText(Text.literal("已将保护动作")).append(ctx.protectionManager().getActonName(id)).append("设为").append(String.valueOf(state)));
                                                                                return Command.SINGLE_SUCCESS;
                                                                            } else {
                                                                                context.getSource().sendError(newText(Text.literal("找不到ID为 %s 的保护动作！".formatted(id))));
                                                                                return 0;
                                                                            }
                                                                        })
                                                        )
                                        )
                        )
        );
    }

    private static MutableText newText(Text text) {
        return Text.translatable("messages.safeguard.prefix").styled(style -> style.withColor(Formatting.GREEN)).append(text);
    }
}
