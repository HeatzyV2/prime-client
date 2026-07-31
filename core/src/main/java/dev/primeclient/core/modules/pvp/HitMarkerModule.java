package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.AttackEntityEvent;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.elements.HitMarkerElement;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

/** Neon hitmarker overlay triggered when hitting enemies. */
public final class HitMarkerModule extends Module {

    private final HitMarkerElement element;

    public HitMarkerModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("hit-marker", "Hit Marker", "Renders a neon cross marker when hitting targets", ModuleCategory.PVP);
        this.element = hud.register(new HitMarkerElement(themes));
        element.setVisible(false);
        listen(AttackEntityEvent.class, event -> onAttack());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void onAttack() {
        if (isEnabled()) {
            element.triggerHit();
        }
    }
}
