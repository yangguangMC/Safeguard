package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.yangguangmc.safeguard.protection.action.OutlineAction;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "updateRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;outlineColor:I", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    private void overwriteOutlineColor(T entity, S state, float tickProgress, CallbackInfo ci) {
        int outline = OutlineAction.getOutline(entity.getUuid());
        if (outline != 0) state.outlineColor = outline;
    }
}
