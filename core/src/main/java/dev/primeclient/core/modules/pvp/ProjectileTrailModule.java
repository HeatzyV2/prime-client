package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.EnumSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;

/** Particle trails for thrown projectiles (Ender Pearls, arrows, tridents). */
public final class ProjectileTrailModule extends Module {

    public enum Style {
        NEON_SPARK,
        FLAME_BURST,
        ENCHANT_GLOW
    }

    private final EnumSetting<Style> style = addSetting(new EnumSetting<>(
            "style", "Trail Style", "Visual particle style for projectiles", Style.NEON_SPARK));
    private final BooleanSetting pearlsOnly = addSetting(new BooleanSetting(
            "pearls-only", "Ender Pearls Only", "Only attach trails to Ender Pearls", false));

    public ProjectileTrailModule(MinecraftAdapter adapter) {
        super("projectile-trails", "Projectile Trails", "Custom particle trails behind thrown projectiles", ModuleCategory.PVP);
    }

    public Style style() {
        return style.get();
    }

    public boolean pearlsOnly() {
        return pearlsOnly.get();
    }
}
