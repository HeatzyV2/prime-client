package dev.primeclient.core.modules.streamers;

import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.stream.StreamerPrivacyState;

/** Replaces the F3 debug overlay with a stream-safe minimal view. */
public final class StreamDebugShieldModule extends Module {

    public StreamDebugShieldModule() {
        super("stream-debug-shield", "Stream Debug Shield",
                "Sanitizes the F3 debug overlay for streams", ModuleCategory.STREAMERS);
    }

    @Override
    protected void onEnable() {
        StreamerPrivacyState.setDebugShield(true);
    }

    @Override
    protected void onDisable() {
        StreamerPrivacyState.setDebugShield(false);
    }
}
