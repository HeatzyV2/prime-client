package dev.primeclient.v26_2.render;

import dev.primeclient.core.gui.BlurBackdrop;
import net.minecraft.client.Minecraft;

/** Clears blur post-effects when premium GUI screens close. */
public final class PanelBlur {

    private PanelBlur() {
    }

    public static void end(Minecraft minecraft) {
        BlurBackdrop.setActive(false);
        if (minecraft != null && minecraft.gameRenderer != null) {
            minecraft.gameRenderer.clearPostEffect();
        }
    }
}
