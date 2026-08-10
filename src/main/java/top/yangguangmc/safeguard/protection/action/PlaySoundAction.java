package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

public class PlaySoundAction extends Action {
    private final Holder<SoundEvent> sound;
    private final float pitch;
    private final long interval;
    private boolean playing = false;
    private long tickCount;
    private long lastPlay;

    public PlaySoundAction(Holder<SoundEvent> sound, float pitch, long interval) {
        super("passive/other/play_sound");
        this.sound = sound;
        this.pitch = pitch;
        this.interval = interval;
    }

    public void play(SoundManager soundManager) {
        soundManager.play(createSoundInstance(sound, pitch));
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public void tick(Minecraft client) {
        if (playing) {
            if (tickCount - lastPlay >= interval) {
                lastPlay = tickCount;
                play(client.getSoundManager());
            }
        }
        tickCount++;
    }
}
