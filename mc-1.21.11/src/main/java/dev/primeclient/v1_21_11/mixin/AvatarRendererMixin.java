package dev.primeclient.v1_21_11.mixin;

import dev.primeclient.core.state.CosmeticsState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "extractCapeState", at = @At("RETURN"))
    private void primeclient$primeCape(Avatar entity, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity instanceof LocalPlayer
                && CosmeticsState.capeStyle() != CosmeticsState.CapeStyle.NONE) {
            state.showCape = true;
        }
    }
}
