package top.yangguangmc.safeguard.protection.action;

public class RedVignetteAction extends Action {
    private static float progress = 0;

    public RedVignetteAction() {
        super("passive/hud/red_vignette");
    }

    public static float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        RedVignetteAction.progress = progress;
    }
}
