package top.yangguangmc.safeguard;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import top.yangguangmc.safeguard.gui.screen.ConfigScreen;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;

public class SafeguardCommand {
    private static ModContext ctx;

    public static void init(ModContext ctx) {
        SafeguardCommand.ctx = ctx;
        ClientCommandRegistrationCallback.EVENT.register(SafeguardCommand::register);
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
                ClientCommandManager.literal(ModContext.MOD_ID)
                        .then(
                                ClientCommandManager.literal("screen")
                                        .executes(context -> {
                                            Minecraft client = context.getSource().getClient();
                                            client.tell(() -> client.setScreen(ConfigScreen.create(client.screen)));
                                            return Command.SINGLE_SUCCESS;
                                        })
                        ).then(
                                ClientCommandManager.literal("detection")
                                        .then(
                                                ClientCommandManager.argument("id", ResourceLocationArgument.id())
                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ctx.protectionManager().getDetectionStatesRoot().getNodeIds(), builder))
                                                        .executes(context -> {
                                                            ResourceLocation id = context.getArgument("id", ResourceLocation.class);
                                                            SwitchTreeNode detection = ctx.protectionManager().getDetectionStatesRoot().getNode(id);
                                                            if (detection != null) {
                                                                context.getSource().sendFeedback(newText(Component.translatable("command.safeguard.detection.status", ctx.protectionManager().getDetectionName(id), detection.isEnabled(), detection.isEffectivelyEnabled())));
                                                                return Command.SINGLE_SUCCESS;
                                                            } else {
                                                                context.getSource().sendError(newText(Component.translatable("command.safeguard.detection.not_found", id.toString())));
                                                                return 0;
                                                            }
                                                        }).then(
                                                                ClientCommandManager.argument("state", BoolArgumentType.bool())
                                                                        .executes(context -> {
                                                                            ResourceLocation id = context.getArgument("id", ResourceLocation.class);
                                                                            boolean state = BoolArgumentType.getBool(context, "state");
                                                                            SwitchTreeNode detection = ctx.protectionManager().getDetectionStatesRoot().getNode(id);
                                                                            if (detection != null) {
                                                                                detection.setEnabled(state);
                                                                                context.getSource().sendFeedback(newText(Component.translatable("command.safeguard.detection.set", ctx.protectionManager().getDetectionName(id), state)));
                                                                                return Command.SINGLE_SUCCESS;
                                                                            } else {
                                                                                context.getSource().sendError(newText(Component.translatable("command.safeguard.detection.not_found", id.toString())));
                                                                                return 0;
                                                                            }
                                                                        })
                                                        )
                                        )
                        ).then(
                                ClientCommandManager.literal("action")
                                        .then(
                                                ClientCommandManager.argument("id", ResourceLocationArgument.id())
                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ctx.protectionManager().getActionStatesRoot().getNodeIds(), builder))
                                                        .executes(context -> {
                                                            ResourceLocation id = context.getArgument("id", ResourceLocation.class);
                                                            SwitchTreeNode action = ctx.protectionManager().getActionStatesRoot().getNode(id);
                                                            if (action != null) {
                                                                context.getSource().sendFeedback(newText(Component.translatable("command.safeguard.action.status", ctx.protectionManager().getActonName(id), action.isEnabled(), action.isEffectivelyEnabled())));
                                                                return Command.SINGLE_SUCCESS;
                                                            } else {
                                                                context.getSource().sendError(newText(Component.translatable("command.safeguard.action.not_found", id.toString())));
                                                                return 0;
                                                            }
                                                        }).then(
                                                                ClientCommandManager.argument("state", BoolArgumentType.bool())
                                                                        .executes(context -> {
                                                                            ResourceLocation id = context.getArgument("id", ResourceLocation.class);
                                                                            boolean state = BoolArgumentType.getBool(context, "state");
                                                                            SwitchTreeNode action = ctx.protectionManager().getActionStatesRoot().getNode(id);
                                                                            if (action != null) {
                                                                                action.setEnabled(state);
                                                                                context.getSource().sendFeedback(newText(Component.translatable("command.safeguard.action.set", ctx.protectionManager().getActonName(id), state)));
                                                                                return Command.SINGLE_SUCCESS;
                                                                            } else {
                                                                                context.getSource().sendError(newText(Component.translatable("command.safeguard.action.not_found", id.toString())));
                                                                                return 0;
                                                                            }
                                                                        })
                                                        )
                                        )
                        )
        );
    }

    private static MutableComponent newText(Component text) {
        return Component.translatable("messages.safeguard.prefix").withStyle(style -> style.withColor(ChatFormatting.GREEN)).append(text);
    }
}
