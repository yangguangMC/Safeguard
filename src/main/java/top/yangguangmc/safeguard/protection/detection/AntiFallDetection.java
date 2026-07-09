package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AntiFallDetection extends Detection {
    public AntiFallDetection() {
        super("environment/anti_fall", new ActionBarTitleAction(), new QuitAction(), new PauseAction());
        ClientPlayerTickEvents.START_TICK.register((client, world, player) -> {
            if (getStateNode().isEffectivelyEnabled()) onStartTick(client, world, player);
        });
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        // 防挖掘坠落
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
            if (pos.equals(player.supportingBlockPos.orElse(null))) {
                ItemStack item = player.getActiveItem();
                if (client.options.attackKey.isPressed() || (player.getPitch() > 0 && item.getItem().canMine(item, world.getBlockState(pos), world, pos, player))) {
                    List<SafeResult> result = checkSafety(8, pos, world, player);
                    result.removeIf(r -> r.unsafety() <= 0);
                    if (!result.isEmpty()) {
                        ActionBarTitleAction action = getBoundAction(Identifier.of(ModContext.MOD_ID, "passive/hud/action_bar_title"));
                        if (isActionEffectivelyEnabled(action)) action.updateTitle(client, result);
                    }
                }
            }
        }
        // 已坠落保护
        if (!player.isOnGround() && player.fallDistance > 1.5 && checkSafety(5, player.getBlockPos(), world, player).stream().allMatch(result -> result.unsafety() > 0)) {
            PauseAction action1 = getBoundAction(Identifier.of(ModContext.MOD_ID, "active/afk/pause"));
            if (isActionEffectivelyEnabled(action1)) action1.pause(client, getName());
            QuitAction action2 = getBoundAction(Identifier.of(ModContext.MOD_ID, "active/afk/quit"));
            if (isActionEffectivelyEnabled(action2)) action2.quit(client, world, getName());
        }
    }

    private List<SafeResult> checkSafety(int checkHeight, BlockPos pos, ClientWorld world, ClientPlayerEntity player) {
        List<SafeResult> results = new ArrayList<>();
        for (int i = 1; i <= checkHeight; i++) {
            BlockState state = world.getBlockState(pos.down(i));
            if (!state.hasSolidTopSurface(world, pos, player)) {
                if (state.isAir()) {
                    results.add(new SafeResult(Text.literal("Cliff"), i, 2));
                    continue;
                } else if (state.isOf(Blocks.WATER)) {
                    results.add(new SafeResult(Blocks.WATER.getName(), i, 1));
                    continue;
                } else if (state.isOf(Blocks.LAVA)) {
                    results.add(new SafeResult(Blocks.LAVA.getName(), i, 3));
                    continue;
                }
            }
            results.add(new SafeResult(Text.literal("Solid"), i, 0));
        }
        return results;
    }

    private record SafeResult(Text name, int posBelow, int unsafety) {
    }

    private static class ActionBarTitleAction extends Action {
        public ActionBarTitleAction() {
            super("passive/hud/action_bar_title");
        }

        public void updateTitle(MinecraftClient client, List<SafeResult> results) {
            MutableText message = Text.empty();
            results.stream().max(Comparator.comparing(SafeResult::unsafety)).ifPresent(result -> {
                message.append("警告：下方 %d 格处存在 %s".formatted(result.posBelow(), result.name().getString())).styled(style -> {
                    if (result.unsafety() > 2) return style.withColor(Formatting.RED).withBold(true);
                    return style.withColor(Formatting.RED);
                });
                results.stream().min(Comparator.comparing(SafeResult::posBelow)).filter(o -> !result.equals(o)).ifPresent(result2 ->
                        message.append(" | 警告：下方 %d 格处存在 %s".formatted(result2.posBelow(), result2.name().getString())).styled(style -> {
                            if (result2.unsafety() > 2) return style.withColor(Formatting.RED).withBold(true);
                            return style.withColor(Formatting.RED);
                        }));
            });
            client.inGameHud.setOverlayMessage(message, false);
        }
    }
}
