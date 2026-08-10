package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
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
        if (!(entity instanceof LocalPlayer player)) return;
        if (!GlobalProtectionConditions.shouldProtect(player)) return;
        Minecraft client = Minecraft.getInstance();
        tryExecuteAction(PauseAction.class, action -> action.pause(client));
        tryExecuteAction(QuitAction.class, action -> action.quit(client));
    }
}
