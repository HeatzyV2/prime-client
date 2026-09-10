package dev.primeclient.v1_21_5.mixin;

import dev.primeclient.v1_21_5.hud.VanillaHudTransforms;
import dev.primeclient.core.hud.vanilla.VanillaHudComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Boss bar transforms on ≤1.21.5 (no Gui.renderBossOverlay helper yet). */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void primeclient$bossHead(GuiGraphics graphics, CallbackInfo ci) {
        if (VanillaHudTransforms.isHidden(VanillaHudComponent.BOSSBAR)) {
            ci.cancel();
            return;
        }
        VanillaHudTransforms.push(graphics, VanillaHudComponent.BOSSBAR);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void primeclient$bossTail(GuiGraphics graphics, CallbackInfo ci) {
        VanillaHudTransforms.pop(graphics, VanillaHudComponent.BOSSBAR);
    }
}
