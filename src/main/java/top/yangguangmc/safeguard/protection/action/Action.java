package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeItem;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.detection.Detection;

import java.util.Objects;

public abstract class Action implements SwitchTreeItem {
    private final Identifier id;
    private Detection parent;
    protected ModContext modContext;

    public Action(Identifier id) {
        this.id = id;
    }

    public Action(String path) {
        this(Identifier.fromNamespaceAndPath(ModContext.MOD_ID, path));
    }

    @Override
    public Identifier getId() {
        return id;
    }

    public Detection getParent() {
        return parent;
    }

    public void initParent(Detection parent) {
        if (this.parent != null) throw new IllegalStateException("Duplicate parent initialization");
        this.parent = parent;
    }

    public void init(ModContext ctx) {
        modContext = ctx;
    }

    protected SwitchTreeNode getStateNode() {
        return Objects.requireNonNull(modContext.protectionManager().getActionStatesRoot().getNode(getId()));
    }

    protected SimpleSoundInstance createSoundInstance(Holder<SoundEvent> sound, float pitch) {
        return createSoundInstance(sound.value(), pitch);
    }

    protected SimpleSoundInstance createSoundInstance(SoundEvent sound, float pitch) {
        return new SimpleSoundInstance(sound.location(),
                SoundSource.MASTER,
                1.0F,
                pitch,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0F,
                0.0F,
                0.0F,
                true);
    }
}
