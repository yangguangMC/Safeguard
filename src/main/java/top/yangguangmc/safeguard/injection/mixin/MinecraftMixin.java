package top.yangguangmc.safeguard.injection.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.yangguangmc.safeguard.protection.action.OutlineAction;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
    private boolean hasOutline(boolean original, Entity entity) {
        if (OutlineAction.getOutline(entity) != 0) return true;
        return original;
    }
}
