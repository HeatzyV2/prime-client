package dev.primeclient.v26_2.mixin;

import dev.primeclient.core.hook.PrimeHooks;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {

    @Shadow
    private float blockLightFlicker;

    @Shadow
    private boolean needsUpdate;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void primeclient$stableFullbrightTick(CallbackInfo ci) {
        if (PrimeHooks.fullbrightActive()) {
            this.blockLightFlicker = 0.0F;
            this.needsUpdate = true;
            ci.cancel();
        }
    }

    @Inject(method = "extract", at = @At("TAIL"))
    private void primeclient$applyFullbright(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (!PrimeHooks.fullbrightActive()) {
            return;
        }
        renderState.brightness = 1.0F;
        renderState.darknessEffectScale = 0.0F;
        renderState.bossOverlayWorldDarkening = 0.0F;
        renderState.nightVisionEffectIntensity = 1.0F;
        renderState.blockFactor = 1.4F;
    }
}
