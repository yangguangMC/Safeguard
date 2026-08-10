package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.yangguangmc.safeguard.protection.action.OutlineAction;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;outlineColor:I", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    private void overwriteOutlineColor(T entity, S state, float tickProgress, CallbackInfo ci) {
        int outline = OutlineAction.getOutline(entity.getUUID());
        if (outline != 0) state.outlineColor = outline;
    }
}
