package top.yangguangmc.safeguard.injection.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onStartTick(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        ClientPlayerTickEvents.START_TICK.invoker().onStartTick(client, client.level, (LocalPlayer) (Object) this);
    }
}
