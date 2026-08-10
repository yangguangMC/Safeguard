package top.yangguangmc.safeguard.injection.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.yangguangmc.safeguard.protection.action.OutlineAction;

/**
 * Overrides {@link Entity#getTeamColor()} to apply custom outline colors
 * registered by {@link OutlineAction}.
 * <p>
 * In 1.20.6, entity outlines are rendered through the team color system —
 * {@link net.minecraft.client.renderer.LevelRenderer} calls {@code getTeamColorValue()}
 * and passes the result to {@code OutlineVertexConsumerProvider}. By intercepting
 * this method, we can inject custom outline colors without capturing local variables
 * from the deeply nested entity rendering loop.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @ModifyReturnValue(method = "getTeamColor", at = @At("RETURN"))
    private int addOutlineColor(int original) {
        if (original != 0xFFFFFF) return original;
        int color = OutlineAction.getOutline((Entity) (Object) this);
        if (color != 0) return color;
        return original;
    }
}
