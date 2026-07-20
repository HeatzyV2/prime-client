package dev.primeclient.v1_21_11.mixin;

import dev.primeclient.core.PrimeClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void primeclient$addSocialHubButton(CallbackInfo ci) {
        int x = this.width / 2 - 102;
        int y = this.height / 4 + 168;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("prime.gui.pause.social_hub"),
                        btn -> PrimeClient.get().adapter().openSocialHub())
                .bounds(x, y, 204, 20)
                .build());
    }
}
