package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.yangguangmc.safeguard.protection.GlobalProtectionConditions;
import top.yangguangmc.safeguard.protection.action.RedVignetteAction;

/**
 * 1.20.6 adapted version: ColorHelper uses Argb inner class,
 * and drawTexture signature is different (no RenderPipeline or color parameter).
 * We modify the color variable set via setShaderColor instead.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @ModifyVariable(method = "renderVignetteOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;setShaderColor(FFFF)V", shift = At.Shift.BEFORE), ordinal = 0)
    private float modifyVignetteColor(float channel) {
        if (!GlobalProtectionConditions.shouldProtect(MinecraftClient.getInstance().player)) return channel;
        float progress = RedVignetteAction.getProgress();
        if (progress <= 0) return channel;
        // Blend the gray channel toward red: reduce green/blue, increase red
        return channel * (1 - progress) + 1.0F * progress;
    }
}
