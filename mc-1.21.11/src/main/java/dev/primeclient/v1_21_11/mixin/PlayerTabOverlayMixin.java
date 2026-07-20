package dev.primeclient.v1_21_11.mixin;

import dev.primeclient.core.hook.PrimeHooks;
import dev.primeclient.v1_21_11.render.PrimeTabBadgeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

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
        int columnLeft = x + 11 - width;
        PrimeTabBadgeRenderer.draw(graphics, columnLeft + 8, y);
    }
}
