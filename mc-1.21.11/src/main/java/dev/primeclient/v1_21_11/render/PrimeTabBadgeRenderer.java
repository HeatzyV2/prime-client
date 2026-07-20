package dev.primeclient.v1_21_11.render;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.design.PrimeLogo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Draws the Prime logo in the player tab list. */
public final class PrimeTabBadgeRenderer {

    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath(PrimeClient.MOD_ID, PrimeLogo.TEXTURE);

    private PrimeTabBadgeRenderer() {
    }

    public static int height() {
        return 8;
    }

    public static int width() {
        return PrimeLogo.widthForHeight(height());
    }

    public static void draw(GuiGraphics graphics, int x, int y) {
        int h = height();
        int w = width();
        graphics.blit(RenderPipelines.GUI_TEXTURED, LOGO, x, y, 0f, 0f, w, h,
                PrimeLogo.SRC_WIDTH, PrimeLogo.SRC_HEIGHT, 0xFFFFFFFF);
    }
}
