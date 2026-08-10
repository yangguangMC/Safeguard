package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
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
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Final
    @Shadow
    private Minecraft minecraft;
    @Shadow
    public float vignetteBrightness;

    @Inject(method = "renderVignette", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    private void modifyVignetteColor(GuiGraphics context, Entity entity, CallbackInfo ci) {
        if (!GlobalProtectionConditions.shouldProtect(Minecraft.getInstance().player)) return;

        WorldBorder worldBorder = Objects.requireNonNull(minecraft.level).getWorldBorder();
        float f = 0.0F;
        if (entity != null) {
            float g = (float) worldBorder.getDistanceToBorder(entity);
            double d = Math.min(
                    worldBorder.getLerpSpeed() * worldBorder.getWarningTime() * 1000.0, Math.abs(worldBorder.getLerpTarget() - worldBorder.getSize())
            );
            double e = Math.max(worldBorder.getWarningBlocks(), d);
            if (g < e) {
                f = 1.0F - (float) (g / e);
            }
        }
        if (f > 0.0F) return;

        float progress = RedVignetteAction.getProgress();
        if (progress <= 0) return;
        float g = vignetteBrightness;
        g = Mth.clamp(g, 0.0F, 1.0F);
        context.setColor(g * (1 - progress) + 0 * progress,
                g * (1 - progress) + progress * progress,
                g * (1 - progress) + progress * progress, 1.0F);
    }
}
