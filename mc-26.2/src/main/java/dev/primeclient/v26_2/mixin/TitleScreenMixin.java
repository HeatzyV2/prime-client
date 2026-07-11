package dev.primeclient.v26_2.mixin;

import dev.primeclient.v26_2.screen.PrimeTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void primeclient$replaceTitleMenu(CallbackInfo ci) {
        Minecraft.getInstance().gui.setScreen(new PrimeTitleScreen());
        ci.cancel();
    }
}
