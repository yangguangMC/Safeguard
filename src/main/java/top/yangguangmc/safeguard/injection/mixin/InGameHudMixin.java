package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.yangguangmc.safeguard.protection.action.RedVignetteAction;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @ModifyVariable(method = "renderVignetteOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIIII)V", shift = At.Shift.BEFORE), ordinal = 0)
    private int modifyVignetteColor(int color) {
        float progress = RedVignetteAction.getProgress();
        if (progress <= 0) return color;
        else {
            int redColor = ColorHelper.fromFloats(1.0F, 0.0F, progress, progress);
            int a = Math.round(ColorHelper.getAlpha(color) * (1 - progress) + ColorHelper.getAlpha(redColor) * progress);
            int r = Math.round(ColorHelper.getRed(color) * (1 - progress) + ColorHelper.getRed(redColor) * progress);
            int g = Math.round(ColorHelper.getGreen(color) * (1 - progress) + ColorHelper.getGreen(redColor) * progress);
            int b = Math.round(ColorHelper.getBlue(color) * (1 - progress) + ColorHelper.getBlue(redColor) * progress);
            return ColorHelper.getArgb(a, r, g, b);
        }
    }
}
