package dev.primeclient.v1_21_11.mixin;

import dev.primeclient.core.hook.PrimeHooks;
import dev.primeclient.core.state.AlwaysDayState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void primeclient$noRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (PrimeHooks.noRainActive()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void primeclient$noThunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (PrimeHooks.noRainActive()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void primeclient$alwaysDayTime(CallbackInfoReturnable<Long> cir) {
        if (PrimeHooks.alwaysDayActive()) {
            cir.setReturnValue(AlwaysDayState.NOON_TICKS);
        }
    }
}
