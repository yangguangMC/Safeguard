package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
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
        this(Identifier.of(ModContext.MOD_ID, path));
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

    protected PositionedSoundInstance createSoundInstance(RegistryEntry<SoundEvent> sound, float pitch) {
        return createSoundInstance(sound.value(), pitch);
    }

    protected PositionedSoundInstance createSoundInstance(SoundEvent sound, float pitch) {
        return new PositionedSoundInstance(sound.getId(),
                SoundCategory.MASTER,
                1.0F,
                pitch,
                SoundInstance.createRandom(),
                false,
                0,
                SoundInstance.AttenuationType.NONE,
                0.0F,
                0.0F,
                0.0F,
                true);
    }
}
