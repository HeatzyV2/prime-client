package dev.primeclient.v1_21_11.hud;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.hud.vanilla.VanillaHudComponent;
import dev.primeclient.v1_21_11.mixin.GuiLayerInvoker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws only the vanilla HUD layers that map to visible {@link VanillaHudComponent}
 * proxies — no world, blur, chat, crosshair or other {@link Gui#render} work.
 */
public final class VanillaHudLayerRenderer {

    private VanillaHudLayerRenderer() {
    }

    public static void renderVisibleLayers(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Gui gui = minecraft.gui;
        GuiLayerInvoker layers = (GuiLayerInvoker) gui;

        if (isVisible(VanillaHudComponent.BOSSBAR)) {
            layers.primeclient$renderBossOverlay(graphics, deltaTracker);
        }
        if (isVisible(VanillaHudComponent.STATUS_EFFECTS)) {
            layers.primeclient$renderEffects(graphics, deltaTracker);
        }
        if (isVisible(VanillaHudComponent.HOTBAR) || isVisible(VanillaHudComponent.EXPERIENCE)) {
            layers.primeclient$renderHotbarAndDecorations(graphics, deltaTracker);
        }
        if (isVisible(VanillaHudComponent.SCOREBOARD)) {
            layers.primeclient$renderScoreboardSidebar(graphics, deltaTracker);
        }
    }

    private static boolean isVisible(VanillaHudComponent component) {
        HudElement element = PrimeClient.get().hud().get(component.id());
        return element != null && element.isVisible();
    }
}
