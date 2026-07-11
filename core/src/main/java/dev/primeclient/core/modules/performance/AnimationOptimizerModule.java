package dev.primeclient.core.modules.performance;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;

/** Disables fancy graphics to reduce animation and effect overhead. */
public final class AnimationOptimizerModule extends Module {

    private final MinecraftAdapter adapter;
    private boolean savedFancyGraphics;

    public AnimationOptimizerModule(MinecraftAdapter adapter) {
        super("animation-optimizer", "Animation Optimizer", "Disables fancy graphics", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        savedFancyGraphics = adapter.fancyGraphics();
        adapter.setFancyGraphics(false);
    }

    @Override
    protected void onDisable() {
        adapter.setFancyGraphics(savedFancyGraphics);
    }
}
