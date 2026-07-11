package dev.primeclient.core.hud.editor;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.hud.FakeRenderContext;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.theme.ThemeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudEditorTest {

    private static final class BoxElement extends HudElement {
        BoxElement() {
            super("box", "Box", HudAnchor.TOP_LEFT, 0, 0);
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return 20;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return 10;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
        }
    }

    private HudManager hud;
    private HudEditor editor;
    private BoxElement box;

    @BeforeEach
    void setUp() {
        hud = new HudManager();
        editor = new HudEditor(hud, new ThemeManager());
        box = hud.register(new BoxElement());
        hud.render(new FakeRenderContext(200, 100)); // bounds: 0,0 20x10
    }

    @Test
    void pressSelectsElementUnderCursor() {
        assertTrue(editor.mousePressed(5, 5));
        assertSame(box, editor.selected());

        assertFalse(editor.mousePressed(150, 90));
        assertNull(editor.selected());
    }

    @Test
    void dragMovesAndReanchorsToBottomRight() {
        editor.mousePressed(5, 5);
        // Drag so the element lands in the bottom-right third.
        editor.mouseDragged(185, 95, 200, 100);
        editor.mouseReleased();

        assertEquals(HudAnchor.BOTTOM_RIGHT, box.anchor());
        // Clamped inside the screen: x = 180, y = 90 -> equals the anchor base, offset 0.
        assertEquals(0f, box.offsetX());
        assertEquals(0f, box.offsetY());
    }

    @Test
    void dragIsClampedInsideScreen() {
        editor.mousePressed(5, 5);
        editor.mouseDragged(-50, -50, 200, 100);

        assertEquals(HudAnchor.TOP_LEFT, box.anchor());
        assertEquals(0f, box.offsetX());
        assertEquals(0f, box.offsetY());
    }

    @Test
    void scrollScalesSelectedElement() {
        editor.mousePressed(5, 5);
        assertTrue(editor.mouseScrolled(5, 5, 1, false, false));
        assertEquals(1.1f, box.scale(), 1e-5);

        assertFalse(editor.mouseScrolled(150, 90, 1, false, false));
    }

    @Test
    void shiftScrollRotatesSelectedElement() {
        editor.mousePressed(5, 5);
        assertTrue(editor.mouseScrolled(5, 5, 1, true, false));
        assertEquals(5f, box.rotation(), 1e-5);
    }

    @Test
    void ctrlScrollAdjustsOpacity() {
        editor.mousePressed(5, 5);
        assertTrue(editor.mouseScrolled(5, 5, -1, false, true));
        assertEquals(0.95f, box.opacity(), 1e-5);
    }

    @Test
    void rKeyCyclesTint() {
        editor.mousePressed(5, 5);
        assertTrue(editor.keyPressed(82));
        assertEquals(0xFFFFFFFF, box.tintArgb());
    }
}
