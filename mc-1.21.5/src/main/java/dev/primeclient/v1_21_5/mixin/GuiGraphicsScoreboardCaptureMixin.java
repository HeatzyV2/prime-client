package dev.primeclient.v1_21_5.mixin;

import dev.primeclient.core.hud.vanilla.VanillaHudMeasurements;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records fill rects while the scoreboard sidebar is rendering. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsScoreboardCaptureMixin {

    @Inject(method = "fill(IIIII)V", at = @At("HEAD"))
    private void primeclient$captureLegacyFill(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        VanillaHudMeasurements.recordScoreboardFill(x1, y1, x2, y2);
    }

    @Inject(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V", at = @At("HEAD"))
    private void primeclient$captureRenderTypeFill(
            RenderType type, int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        VanillaHudMeasurements.recordScoreboardFill(x1, y1, x2, y2);
    }
}
