package dev.primeclient.core.gui.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TitleMenuRendererTest {

    private static final int W = 854;
    private static final int H = 480;

    private final TitleMenuRenderer renderer = new TitleMenuRenderer();

    @Test
    void hitActionDetectsSingleplayerButton() {
        int stackH = 5 * 30 + 4 * 8;
        int startY = (H - stackH) / 2;
        int x = W - 48 - 220;
        double mx = x + 110;
        double my = startY + 15;

        assertEquals(TitleMenuAction.SINGLEPLAYER, renderer.hitAction(mx, my, W, H));
    }

    @Test
    void hitActionReturnsNullOutsideButtons() {
        assertNull(renderer.hitAction(10, 10, W, H));
    }
}
