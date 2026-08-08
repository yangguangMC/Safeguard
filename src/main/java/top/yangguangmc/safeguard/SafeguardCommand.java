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

public class SafeguardCommand {
    private static ModContext ctx;

    public static void init(ModContext ctx) {
        SafeguardCommand.ctx = ctx;
        ClientCommandRegistrationCallback.EVENT.register(SafeguardCommand::register);
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
                                                                context.getSource().sendFeedback(newText(Text.translatable("command.safeguard.detection.status", ctx.protectionManager().getDetectionName(id), detection.isEnabled(), detection.isEffectivelyEnabled())));
                                                                return Command.SINGLE_SUCCESS;
                                                            } else {
                                                                context.getSource().sendError(newText(Text.translatable("command.safeguard.detection.not_found", id.toString())));
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
                                                                                context.getSource().sendFeedback(newText(Text.translatable("command.safeguard.detection.set", ctx.protectionManager().getDetectionName(id), state)));
                                                                                return Command.SINGLE_SUCCESS;
                                                                            } else {
                                                                                context.getSource().sendError(newText(Text.translatable("command.safeguard.detection.not_found", id.toString())));
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
                                                                context.getSource().sendFeedback(newText(Text.translatable("command.safeguard.action.status", ctx.protectionManager().getActonName(id), action.isEnabled(), action.isEffectivelyEnabled())));
                                                                return Command.SINGLE_SUCCESS;
                                                            } else {
                                                                context.getSource().sendError(newText(Text.translatable("command.safeguard.action.not_found", id.toString())));
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
                                                                                context.getSource().sendFeedback(newText(Text.translatable("command.safeguard.action.set", ctx.protectionManager().getActonName(id), state)));
                                                                                return Command.SINGLE_SUCCESS;
                                                                            } else {
                                                                                context.getSource().sendError(newText(Text.translatable("command.safeguard.action.not_found", id.toString())));
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
