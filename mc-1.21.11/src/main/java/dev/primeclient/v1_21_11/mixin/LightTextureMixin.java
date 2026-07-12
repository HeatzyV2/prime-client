package dev.primeclient.v1_21_11.mixin;

import dev.primeclient.core.hook.PrimeHooks;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Shadow
    private float blockLightRedFlicker;

    @Shadow
    private boolean updateLightTexture;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void primeclient$stableFullbrightTick(CallbackInfo ci) {
        if (PrimeHooks.fullbrightActive()) {
            this.blockLightRedFlicker = 0.0F;
            this.updateLightTexture = true;
            ci.cancel();
        }
    }

    @Inject(method = "calculateDarknessScale", at = @At("RETURN"), cancellable = true)
    private void primeclient$noDarknessPulse(CallbackInfoReturnable<Float> cir) {
        if (PrimeHooks.fullbrightActive()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getBrightness(FI)F", at = @At("RETURN"), cancellable = true)
    private static void primeclient$fullbrightLevel(CallbackInfoReturnable<Float> cir) {
        if (PrimeHooks.fullbrightActive()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(
            method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void primeclient$fullbrightDimension(CallbackInfoReturnable<Float> cir) {
        if (PrimeHooks.fullbrightActive()) {
            cir.setReturnValue(1.0F);
        }
    }
}
