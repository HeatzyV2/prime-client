package dev.primeclient.v1_21_11.mixin;

import dev.primeclient.core.hook.PrimeHooks;
import dev.primeclient.v1_21_11.render.PrimeTabBadgeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Unique
    private long primeclient$lastTabRenderMs;
    @Unique
    private long primeclient$tabOpenStartMs;
    @Unique
    private boolean primeclient$tabPosePushed;

    @Inject(method = "decorateName", at = @At("RETURN"), cancellable = true)
    private void primeclient$reserveBadgeSpace(PlayerInfo playerInfo, MutableComponent name,
                                               CallbackInfoReturnable<Component> cir) {
        if (!PrimeHooks.clientBadgeActive() || playerInfo == null || cir.getReturnValue() == null) {
            return;
        }
        if (!PrimeHooks.isPrimePlayer(playerInfo.getProfile().id())) {
            return;
        }
        int spaceWidth = Math.max(1, Minecraft.getInstance().font.width(" "));
        int spaces = Math.max(1, (PrimeTabBadgeRenderer.width() + 2 + spaceWidth - 1) / spaceWidth);
        cir.setReturnValue(Component.literal(" ".repeat(spaces)).append(cir.getReturnValue()));
    }

    @Inject(method = "renderPingIcon", at = @At("HEAD"))
    private void primeclient$renderBadge(GuiGraphics graphics, int width, int x, int y,
                                         PlayerInfo playerInfo, CallbackInfo ci) {
        if (!PrimeHooks.clientBadgeActive() || playerInfo == null) {
            return;
        }
        if (!PrimeHooks.isPrimePlayer(playerInfo.getProfile().id())) {
            return;
        }
        PrimeTabBadgeRenderer.draw(graphics, x + 9, y);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void primeclient$tabAnimBegin(GuiGraphics graphics, int width, Scoreboard scoreboard,
                                          Objective objective, CallbackInfo ci) {
        primeclient$tabPosePushed = false;
        if (!PrimeHooks.tabAnimationActive()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - primeclient$lastTabRenderMs > 120L) {
            primeclient$tabOpenStartMs = now;
        }
        primeclient$lastTabRenderMs = now;
        int duration = Math.max(1, PrimeHooks.tabAnimationDurationMs());
        float t = Math.min(1f, (now - primeclient$tabOpenStartMs) / (float) duration);
        float ease = t * t * (3f - 2f * t);
        float slide = (1f - ease) * -PrimeHooks.tabAnimationSlidePixels();
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(0f, slide);
        primeclient$tabPosePushed = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void primeclient$tabAnimEnd(GuiGraphics graphics, int width, Scoreboard scoreboard,
                                        Objective objective, CallbackInfo ci) {
        if (primeclient$tabPosePushed) {
            graphics.pose().popMatrix();
            primeclient$tabPosePushed = false;
        }
    }
}
