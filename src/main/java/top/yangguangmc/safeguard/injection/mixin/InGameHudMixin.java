package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import top.yangguangmc.safeguard.protection.GlobalProtectionConditions;
import top.yangguangmc.safeguard.protection.action.RedVignetteAction;

import java.util.Objects;

/**
 * 1.20.6 adapted version: ColorHelper uses Argb inner class,
 * and drawTexture signature is different (no RenderPipeline or color parameter).
 * We modify the color variable set via setShaderColor instead.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Final
    @Shadow
    private MinecraftClient client;
    @Shadow
    public float vignetteDarkness;

    @Inject(method = "renderVignetteOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIFFIIII)V", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    private void modifyVignetteColor(DrawContext context, Entity entity, CallbackInfo ci) {
        if (!GlobalProtectionConditions.shouldProtect(MinecraftClient.getInstance().player)) return;

        WorldBorder worldBorder = Objects.requireNonNull(client.world).getWorldBorder();
        float f = 0.0F;
        if (entity != null) {
            float g = (float) worldBorder.getDistanceInsideBorder(entity);
            double d = Math.min(
                    worldBorder.getShrinkingSpeed() * worldBorder.getWarningTime() * 1000.0, Math.abs(worldBorder.getSizeLerpTarget() - worldBorder.getSize())
            );
            double e = Math.max(worldBorder.getWarningBlocks(), d);
            if (g < e) {
                f = 1.0F - (float) (g / e);
            }
        }
        if (f > 0.0F) return;

        float progress = RedVignetteAction.getProgress();
        if (progress <= 0) return;
        float g = vignetteDarkness;
        g = MathHelper.clamp(g, 0.0F, 1.0F);
        context.setShaderColor(g * (1 - progress) + 0 * progress,
                g * (1 - progress) + progress * progress,
                g * (1 - progress) + progress * progress, 1.0F);
    }
}
