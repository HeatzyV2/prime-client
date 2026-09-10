package dev.primeclient.core.hud.editor;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.hud.FakeRenderContext;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.theme.ThemeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudEditorTest {

    private static final int GLFW_E = 69;
    private static final int GLFW_G = 71;
    private static final int GLFW_H = 72;
    private static final int GLFW_L = 76;
    private static final int GLFW_R = 82;
    private static final int GLFW_S = 83;
    private static final int GLFW_V = 86;
    private static final int GLFW_Y = 89;
    private static final int GLFW_Z = 90;
    private static final int GLFW_RIGHT = 262;
    private static final int GLFW_LEFT = 263;
    private static final int GLFW_UP = 265;

    private static class BoxElement extends HudElement {
        private final int width;
        private final int height;

        BoxElement(String id, int width, int height) {
            super(id, "Box " + id, HudAnchor.TOP_LEFT, 0, 0);
            this.width = width;
            this.height = height;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return width;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return height;
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
        box = hud.register(new BoxElement("box", 20, 10));
        hud.render(new FakeRenderContext(200, 100)); // bounds: 0,0 20x10
    }

    @Test
    void pressSelectsElementUnderCursor() {
        assertTrue(editor.mousePressed(5, 5));
        assertSame(box, editor.selected());

        assertTrue(editor.mousePressed(150, 90));
        assertSame(box, editor.selected());
        editor.mouseReleased();
    }

    @Test
    void dragMovesAndReanchorsToBottomRight() {
        editor.mousePressed(5, 5);
        // Drag so the element lands in the bottom-right third; edge guides keep it flush.
        editor.mouseDragged(185, 95, 200, 100);
        editor.mouseReleased();

        assertEquals(HudAnchor.BOTTOM_RIGHT, box.anchor());
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
    void dragDoesNotCrashWhenElementTallerThanScreen() {
        HudManager tallHud = new HudManager();
        HudEditor tallEditor = new HudEditor(tallHud, new ThemeManager());
        BoxElement tall = tallHud.register(new BoxElement("tall-box", 20, 300));
        tallHud.render(new FakeRenderContext(480, 270));

        tallEditor.mousePressed(5, 5);
        tallEditor.mouseDragged(132, 3, 480, 270);
        tallEditor.mouseReleased();
        tallHud.render(new FakeRenderContext(480, 270));

        assertSame(tall, tallEditor.selected());
        assertEquals(0f, tall.lastY()); // pinned to the top instead of throwing
    }

    @Test
    void dragDoesNotCrashWhenElementWiderThanScreen() {
        BoxElement wide = hud.register(new BoxElement("wide", 300, 10));
        wide.setLayout(HudAnchor.TOP_LEFT, 0, 50);
        hud.render(new FakeRenderContext(200, 100));

        assertTrue(editor.mousePressed(10, 55));
        editor.mouseDragged(60, 55, 200, 100);
        editor.mouseReleased();
        hud.render(new FakeRenderContext(200, 100));

        assertEquals(0f, wide.lastX()); // pinned to the left edge
    }

    @Test
    void dragSnapsToAnotherElementEdge() {
        BoxElement other = hud.register(new BoxElement("other", 20, 10));
        other.setLayout(HudAnchor.TOP_LEFT, 100, 20);
        hud.render(new FakeRenderContext(200, 100));

        editor.mousePressed(5, 5);
        // Raw x would be 97; "other" starts at x=100, within the 4px snap threshold.
        editor.mouseDragged(102, 60, 200, 100);
        editor.mouseReleased();
        hud.render(new FakeRenderContext(200, 100));

        assertEquals(100f, box.lastX());
    }

    @Test
    void snapCanBeToggledOffWithS() {
        assertTrue(editor.keyPressed(GLFW_S));
        editor.mousePressed(5, 5);
        editor.mouseDragged(102, 60, 200, 100);
        editor.mouseReleased();
        hud.render(new FakeRenderContext(200, 100));

        assertEquals(97f, box.lastX()); // no grid rounding, no guide snapping
    }

    @Test
    void arrowKeysNudgeSelectedElement() {
        editor.mousePressed(5, 5);
        assertTrue(editor.keyPressed(GLFW_RIGHT));
        assertTrue(editor.keyPressed(GLFW_RIGHT));
        hud.render(new FakeRenderContext(200, 100));
        assertEquals(2f, box.lastX());

        assertTrue(editor.keyPressed(GLFW_RIGHT, true, false)); // Shift = 10px
        hud.render(new FakeRenderContext(200, 100));
        assertEquals(12f, box.lastX());

        assertTrue(editor.keyPressed(GLFW_LEFT));
        assertTrue(editor.keyPressed(GLFW_UP)); // already at top: stays clamped
        hud.render(new FakeRenderContext(200, 100));
        assertEquals(11f, box.lastX());
        assertEquals(0f, box.lastY());
    }

    @Test
    void arrowKeysWithoutSelectionDoNothing() {
        assertFalse(editor.keyPressed(GLFW_RIGHT));
    }

    @Test
    void undoRestoresLayoutAndRedoReappliesIt() {
        editor.mousePressed(5, 5);
        editor.mouseDragged(50, 50, 200, 100);
        editor.mouseReleased();
        HudAnchor movedAnchor = box.anchor();
        float movedOffsetX = box.offsetX();

        assertTrue(editor.keyPressed(GLFW_Z, false, true));
        assertEquals(HudAnchor.TOP_LEFT, box.anchor());
        assertEquals(0f, box.offsetX());
        assertEquals(0f, box.offsetY());

        assertTrue(editor.keyPressed(GLFW_Y, false, true));
        assertEquals(movedAnchor, box.anchor());
        assertEquals(movedOffsetX, box.offsetX());
    }

    @Test
    void selectionClickWithoutDragLeavesNothingToUndo() {
        editor.mousePressed(5, 5);
        editor.mouseReleased();
        assertTrue(editor.keyPressed(GLFW_Z, false, true)); // handled, but nothing changes
        assertEquals(HudAnchor.TOP_LEFT, box.anchor());
        assertEquals(0f, box.offsetX());
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
        assertTrue(editor.keyPressed(GLFW_R));
        assertEquals(0xFFFFFFFF, box.tintArgb());
    }

    @Test
    void vKeyTogglesVisibility() {
        editor.mousePressed(5, 5);
        assertTrue(box.isVisible());
        assertTrue(editor.keyPressed(GLFW_V));
        assertFalse(box.isVisible());
        assertTrue(editor.keyPressed(GLFW_V));
        assertTrue(box.isVisible());
    }

    @Test
    void hiddenElementIsNotSelectableOnCanvas() {
        box.setVisible(false);
        hud.layout(new FakeRenderContext(200, 100), true);
        assertFalse(editor.mousePressed(5, 5));
        assertNull(editor.selected());
    }

    @Test
    void hiddenElementCannotBeDraggedOnCanvas() {
        editor.mousePressed(5, 5);
        box.setVisible(false);
        editor.mousePressed(150, 90);
        editor.mouseDragged(180, 90, 200, 100);
        editor.mouseReleased();
        assertEquals(0f, box.offsetX());
    }

    @Test
    void lockedElementCannotBeDraggedOrNudged() {
        editor.mousePressed(5, 5);
        assertTrue(editor.keyPressed(GLFW_L));
        assertTrue(box.isLocked());

        editor.mousePressed(5, 5);
        editor.mouseDragged(50, 50, 200, 100);
        editor.mouseReleased();
        assertEquals(0f, box.offsetX());
        assertEquals(0f, box.offsetY());

        assertTrue(editor.keyPressed(GLFW_RIGHT));
        assertEquals(0f, box.offsetX());
    }

    @Test
    void guidesToggleIndependentOfGrid() {
        assertFalse(editor.gridShown());
        assertTrue(editor.guidesOn());
        assertTrue(editor.keyPressed(GLFW_H));
        assertFalse(editor.guidesOn());
        assertFalse(editor.gridShown());
        assertTrue(editor.keyPressed(GLFW_G));
        assertTrue(editor.gridShown());
        assertFalse(editor.guidesOn());
    }

    @Test
    void guidesOffSkipsMagneticSnapWhileSnapOn() {
        BoxElement other = hud.register(new BoxElement("other", 20, 10));
        other.setLayout(HudAnchor.TOP_LEFT, 100, 20);
        hud.render(new FakeRenderContext(200, 100));

        assertTrue(editor.keyPressed(GLFW_H)); // guides off
        editor.mousePressed(5, 5);
        editor.mouseDragged(102, 60, 200, 100);
        editor.mouseReleased();
        hud.render(new FakeRenderContext(200, 100));

        assertEquals(97f, box.lastX()); // no guide snap, grid still off
    }

    @Test
    void altClickCyclesOverlappingElements() {
        // Keep clear of the compact toolbar (y≈6–24).
        box.setLayout(HudAnchor.TOP_LEFT, 40, 40);
        BoxElement top = hud.register(new BoxElement("top", 20, 10));
        top.setLayout(HudAnchor.TOP_LEFT, 40, 40);
        hud.render(new FakeRenderContext(200, 100));

        assertTrue(editor.mousePressed(50, 45, false));
        assertSame(top, editor.selected()); // topmost first

        assertTrue(editor.mousePressed(50, 45, true));
        assertSame(box, editor.selected()); // Alt cycles to next

        assertTrue(editor.mousePressed(50, 45, true));
        assertSame(top, editor.selected());
    }

    @Test
    void altClickCyclesEvenWhenSelectionNotUnderCursor() {
        box.setLayout(HudAnchor.TOP_LEFT, 40, 40);
        BoxElement top = hud.register(new BoxElement("top", 20, 10));
        top.setLayout(HudAnchor.TOP_LEFT, 40, 40);
        BoxElement side = hud.register(new BoxElement("side", 10, 10));
        side.setLayout(HudAnchor.TOP_LEFT, 100, 40);
        hud.render(new FakeRenderContext(200, 100));

        assertTrue(editor.mousePressed(105, 45, false));
        assertSame(side, editor.selected());

        assertTrue(editor.mousePressed(45, 45, true));
        assertSame(top, editor.selected());
    }

    @Test
    void rotatedElementHitUsesOrientedBounds() {
        box.setRotation(45f);
        box.setLayout(HudAnchor.TOP_LEFT, 40, 40);
        hud.render(new FakeRenderContext(200, 100));
        // Center of the box should still hit after rotation.
        float cx = box.lastX() + box.lastWidth() / 2f;
        float cy = box.lastY() + box.lastHeight() / 2f;
        assertTrue(box.containsPoint(cx, cy));
        // Far outside the AABB corner of an axis-aligned box but also outside OBB.
        assertFalse(box.containsPoint(cx + box.lastWidth(), cy + box.lastHeight()));
    }

    @Test
    void eKeyTogglesElementList() {
        assertFalse(editor.listOpen());
        assertTrue(editor.keyPressed(GLFW_E));
        assertTrue(editor.listOpen());
        assertTrue(editor.keyPressed(GLFW_E));
        assertFalse(editor.listOpen());
    }

    @Test
    void cornerHandleResizesSelectedElement() {
        editor.mousePressed(5, 5);
        assertSame(box, editor.selected());
        float start = box.scale();
        // SE corner handle
        assertTrue(editor.mousePressed(20, 10, false, false));
        editor.mouseDragged(40, 30, 200, 100, false, false);
        editor.mouseReleased();
        assertTrue(box.scale() > start);
    }

    /**
     * Panel geometry below assumes FakeRenderContext metrics (6px glyphs, 9px lines)
     * on a 400x300 screen; renderOverlay must run once to lay the panels out.
     */
    @Nested
    class Panels {

        private FakeRenderContext ctx;

        @BeforeEach
        void renderPanels() {
            ctx = new FakeRenderContext(400, 300);
            hud.render(ctx);
            editor.keyPressed(GLFW_E);
            editor.renderOverlay(ctx, 0, 0);
        }

        @Test
        void listRowClickSelectsElementAndClosesPopover() {
            assertTrue(editor.listOpen());
            assertTrue(editor.clickElementsRowForTest(0, false));
            assertSame(box, editor.selected());
            assertFalse(editor.listOpen());
        }

        @Test
        void listEyeZoneClickTogglesVisibility() {
            assertTrue(editor.clickElementsRowForTest(0, true));
            assertFalse(box.isVisible());
        }

        @Test
        void listSelectionRecoversOffScreenElement() {
            box.setLayout(HudAnchor.TOP_LEFT, -500, -500);
            hud.render(ctx);
            if (!editor.listOpen()) {
                editor.keyPressed(GLFW_E);
            }
            editor.renderOverlay(ctx, 0, 0);

            assertTrue(editor.clickElementsRowForTest(0, false));
            hud.render(ctx);
            assertTrue(box.lastX() > 0f);
            assertTrue(box.lastY() > 0f);
        }

        @Test
        void scrollOverListPanelIsConsumedWithoutZooming() {
            for (int i = 0; i < 20; i++) {
                hud.register(new BoxElement("extra-" + i, 20, 10));
            }
            hud.render(ctx);
            if (!editor.listOpen()) {
                editor.keyPressed(GLFW_E);
            }
            editor.renderOverlay(ctx, 0, 0);
            assertTrue(editor.scrollElementsListForTest(1));
            assertEquals(1f, box.scale(), 1e-5);
        }

        @Test
        void toolbarGridButtonTogglesLikeGKey() {
            editor.keyPressed(GLFW_E); // close popover
            editor.renderOverlay(ctx, 0, 0);
            boolean before = editor.gridShown();
            assertTrue(editor.clickToolbarForTest(1)); // Grid
            assertTrue(editor.keyPressed(GLFW_G));
            assertEquals(before, editor.gridShown());
        }

        @Test
        void toolbarResetAllRestoresDefaults() {
            box.setLayout(HudAnchor.BOTTOM_RIGHT, -30, -30);
            box.setScale(2f);
            box.setVisible(false);
            editor.keyPressed(GLFW_E);
            editor.renderOverlay(ctx, 0, 0);

            assertTrue(editor.clickToolbarForTest(6)); // Reset
            assertEquals(HudAnchor.TOP_LEFT, box.anchor());
            assertEquals(0f, box.offsetX());
            assertEquals(1f, box.scale(), 1e-5);
            assertTrue(box.isVisible());
        }

        @Test
        void canvasDragWorksWithInspectorOpen() {
            editor.select(box);
            editor.renderOverlay(ctx, 0, 0);

            assertTrue(editor.mousePressed(10, 5));
            editor.mouseDragged(80, 60, 400, 300);
            editor.mouseReleased();
            hud.render(ctx);

            assertTrue(box.lastX() > 0f || box.lastY() > 0f);
        }

        @Test
        void inspectorVisibilityButtonToggles() {
            editor.select(box);
            editor.renderOverlay(ctx, 0, 0);
            assertTrue(editor.clickInspectorVisibilityForTest());
            assertFalse(box.isVisible());
        }

        @Test
        void dragAfterListSelectMovesElement() {
            assertTrue(editor.clickElementsRowForTest(0, false));
            assertSame(box, editor.selected());
            editor.mousePressed(10, 5);
            editor.mouseDragged(90, 70, 400, 300);
            editor.mouseReleased();
            hud.render(ctx);

            assertTrue(box.lastX() > 0f || box.lastY() > 0f);
        }

        @Test
        void canvasPressArmsDragWithoutMovingUntilMotion() {
            editor.select(box);
            editor.mousePressed(160, 120);
            hud.render(ctx);
            assertEquals(0f, box.lastX());
            assertEquals(0f, box.lastY());
        }

        @Test
        void listPopoverConsumesClicksWhileOpen() {
            assertTrue(editor.listOpen());
            assertTrue(editor.clickElementsRowForTest(0, false));
            assertSame(box, editor.selected());
        }

        @Test
        void inactiveModuleElementNotSelectableOnCanvas() {
            if (editor.listOpen()) {
                editor.keyPressed(GLFW_E);
            }
            box.setLayout(HudAnchor.TOP_LEFT, 180, 80);
            box.setActive(false);
            hud.layout(ctx, true);
            editor.renderOverlay(ctx, 0, 0);
            assertFalse(editor.mousePressed(185, 85));
            assertNull(editor.selected());
        }

        @Test
        void listCanSelectInactiveModuleElementForLayout() {
            box.setActive(false);
            hud.layout(ctx, true);
            if (!editor.listOpen()) {
                editor.keyPressed(GLFW_E);
            }
            editor.renderOverlay(ctx, 0, 0);
            assertTrue(editor.clickElementsRowForTest(0, false));
            assertSame(box, editor.selected());
        }
    }
}
