package top.yangguangmc.safeguard.injection.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.yangguangmc.safeguard.protection.action.OutlineAction;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @ModifyReturnValue(method = "hasOutline", at = @At("RETURN"))
    private boolean hasOutline(boolean original, Entity entity) {
        if (OutlineAction.getOutline(entity) != 0) return true;
        return original;
    }
}
