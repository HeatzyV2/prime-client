package dev.primeclient.v26_2.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.primeclient.core.hook.PrimeHooks;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void primeclient$lowFireHead(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            TextureAtlasSprite sprite,
            CallbackInfo ci) {
        if (PrimeHooks.lowFireActive()) {
            poseStack.pushPose();
            poseStack.translate(0.0F, PrimeHooks.lowFireHeightOffset(), 0.0F);
        }
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void primeclient$lowFireReturn(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            TextureAtlasSprite sprite,
            CallbackInfo ci) {
        if (PrimeHooks.lowFireActive()) {
            poseStack.popPose();
        }
    }
}
