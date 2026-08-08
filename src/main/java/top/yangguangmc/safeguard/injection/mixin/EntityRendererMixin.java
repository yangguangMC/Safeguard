package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.yangguangmc.safeguard.protection.action.OutlineAction;

/**
 * 1.20.6 adapted version: EntityRenderState does not exist.
 * We inject into the render method to set outline color via the entity's team color override.
 * The OutlineAction stores outline colors per entity UUID; we override
 * the team color value returned by hasOutline check in WorldRenderer.
 */

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    /**
     * Inject into render() to potentially modify the entity's outline color.
     * In 1.20.6, entity outlines are determined by WorldRenderer checking
     * hasOutline() and then using getTeamColorValue() for the color.
     * We mix into getTeamColorValue() at the Entity level instead.
     *
     * Since we can't easily change getTeamColorValue per-entity via mixin
     * without affecting all entities, we use a different approach:
     * We mix into WorldRenderer.render() to check OutlineAction.getOutline()
     * before setting the outline color. See WorldRenderer mixin note below.
     *
     * For now, this class exists as a placeholder to register the mixin.
     * The actual outline color override is handled by WorldRenderer
     * checking OutlineAction.getOutline() in its entity rendering loop.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(T entity, float yaw, float tickDelta,
                          net.minecraft.client.util.math.MatrixStack matrices,
                          net.minecraft.client.render.VertexConsumerProvider vertexConsumers, int light,
                          CallbackInfo ci) {
        // Placeholder - outline color is handled via OutlineAction.getOutline()
    }
}
