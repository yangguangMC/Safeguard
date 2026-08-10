package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.yangguangmc.safeguard.protection.GlobalProtectionConditions;
import top.yangguangmc.safeguard.protection.action.RedVignetteAction;

@Mixin(Gui.class)
public class GuiMixin {
    @ModifyVariable(method = "renderVignette", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V", shift = At.Shift.BEFORE), ordinal = 0)
    private int modifyVignetteColor(int color) {
        if (!GlobalProtectionConditions.shouldProtect(Minecraft.getInstance().player)) return color;
        float progress = RedVignetteAction.getProgress();
        if (progress <= 0) return color;
        else {
            int redColor = ARGB.colorFromFloat(1.0F, 0.0F, progress, progress);
            int a = Math.round(ARGB.alpha(color) * (1 - progress) + ARGB.alpha(redColor) * progress);
            int r = Math.round(ARGB.red(color) * (1 - progress) + ARGB.red(redColor) * progress);
            int g = Math.round(ARGB.green(color) * (1 - progress) + ARGB.green(redColor) * progress);
            int b = Math.round(ARGB.blue(color) * (1 - progress) + ARGB.blue(redColor) * progress);
            return ARGB.color(a, r, g, b);
        }
    }
}
