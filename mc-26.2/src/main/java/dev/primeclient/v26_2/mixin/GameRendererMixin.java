package dev.primeclient.v26_2.mixin;

import dev.primeclient.core.hook.PrimeHooks;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyVariable(
            method = "getFov",
            at = @At("STORE"),
            ordinal = 0
    )
    private float primeclient$applyZoomFov(float fov) {
        return fov * PrimeHooks.fovMultiplier();
    }
}
