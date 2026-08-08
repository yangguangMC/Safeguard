package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import top.yangguangmc.safeguard.protection.GlobalProtectionConditions;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.EntityDamagedEvents;

public class DamageDetection extends Detection {
    public DamageDetection() {
        super("status/damage", new PauseAction(), new QuitAction());
        listen(EntityDamagedEvents.GATED_PRE, this::onEntityDamaged);
    }

    private void onEntityDamaged(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ClientPlayerEntity player)) return;
        if (!GlobalProtectionConditions.shouldProtect(player)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        tryExecuteAction(PauseAction.class, action -> action.pause(client));
        tryExecuteAction(QuitAction.class, action -> action.quit(client));
    }
}
