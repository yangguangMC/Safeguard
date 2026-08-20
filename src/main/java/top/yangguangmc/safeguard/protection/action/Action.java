package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeItem;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.detection.Detection;
import top.yangguangmc.safeguard.protection.option.ConfigOption;
import top.yangguangmc.safeguard.protection.option.OptionSet;

import java.util.Collection;
import java.util.Objects;

public abstract class Action implements SwitchTreeItem {
    private final ResourceLocation id;
    private Detection parent;
    private final OptionSet options = new OptionSet();
    protected ModContext modContext;

    public Action(ResourceLocation id) {
        this.id = id;
    }

    public Action(String path) {
        this(ResourceLocation.tryBuild(ModContext.MOD_ID, path));
    }

    @Override
    public ResourceLocation getId() {
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

    /**
     * 注册一个该动作特有的配置项，通常在构造器中调用。
     * 注册顺序即为 GUI 中的展示顺序。默认按动作 ID 全局生效；
     * 若该配置项由某个检测项专属的静态子类持有，应调用 {@link ConfigOption#pairScoped()} 标记。
     */
    protected <O extends ConfigOption<?>> O registerOption(O option) {
        return options.register(option);
    }

    /**
     * 获取该动作注册的全部配置项（保序）。
     */
    public Collection<ConfigOption<?>> getOptions() {
        return options.options();
    }

    protected SimpleSoundInstance createSoundInstance(Holder<SoundEvent> sound, float pitch) {
        return createSoundInstance(sound.value(), pitch);
    }

    protected SimpleSoundInstance createSoundInstance(SoundEvent sound, float pitch) {
        return new SimpleSoundInstance(sound.getLocation(),
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
