package dev.primeclient.v26_2.mixin;

import dev.primeclient.core.hook.PrimeHooks;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void primeclient$hideVanillaCrosshair(CallbackInfo ci) {
        if (PrimeHooks.hideVanillaCrosshair()) {
            ci.cancel();
        }
    }
}
