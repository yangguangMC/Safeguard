package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.yangguangmc.safeguard.protection.event.EntityDamagedEvents;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "onDamaged", at = @At("HEAD"))
    private void onDamaged(DamageSource source, CallbackInfo ci) {
        EntityDamagedEvents.PRE.invoker().onEntityDamaged((LivingEntity) (Object) this, source);
    }
}
