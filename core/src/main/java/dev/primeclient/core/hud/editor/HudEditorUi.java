package dev.primeclient.core.hud.editor;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.UiChrome;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD Editor V3 chrome — canvas-first.
 *
 * <ul>
 *   <li>Compact top toolbar</li>
 *   <li>Elements popover (not a permanent left column)</li>
 *   <li>Contextual inspector dock when something is selected</li>
 *   <li>Discrete hint strip</li>
 * </ul>
 */
final class HudEditorUi {

    record Rect(int x, int y, int w, int h) {
        static final Rect EMPTY = new Rect(0, 0, 0, 0);

        boolean contains(double px, double py) {
            return w > 0 && h > 0 && px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private enum Slider {
        NONE, SCALE, OPACITY, ROTATION
    }

    private static final int TOOLBAR_Y = 6;
    private static final int POPOVER_WIDTH = PrimeDesign.EDITOR_POPOVER_W;
    private static final int POPOVER_MAX_H = PrimeDesign.EDITOR_POPOVER_MAX_H;
    private static final int INSPECTOR_H = PrimeDesign.EDITOR_INSPECTOR_HEIGHT;
    private static final int HINT_RESERVE = 12;
    private static final int ROW_HEIGHT = 15;
    private static final int SWATCH = 11;

    private final HudEditor editor;

    private Rect toolbar = Rect.EMPTY;
    private Rect btnElements = Rect.EMPTY;
    private Rect btnGrid = Rect.EMPTY;
    private Rect btnGuides = Rect.EMPTY;
    private Rect btnSnap = Rect.EMPTY;
    private Rect btnUndo = Rect.EMPTY;
    private Rect btnRedo = Rect.EMPTY;
    private Rect btnResetAll = Rect.EMPTY;

    /** Floating elements popover (V3 — replaces the full-height left rail). */
    private Rect listPanel = Rect.EMPTY;
    private Rect listView = Rect.EMPTY;
    private final List<HudElement> listElements = new ArrayList<>();
    private float listScroll;
    private int listContentHeight;

    private Rect propsPanel = Rect.EMPTY;
    private Rect btnVisibility = Rect.EMPTY;
    private Rect btnLock = Rect.EMPTY;
    private Rect trackScale = Rect.EMPTY;
    private Rect trackOpacity = Rect.EMPTY;
    private Rect trackRotation = Rect.EMPTY;
    private Rect swatchRow = Rect.EMPTY;
    private Rect btnCenterX = Rect.EMPTY;
    private Rect btnCenterY = Rect.EMPTY;
    private Rect btnFront = Rect.EMPTY;
    private Rect btnBack = Rect.EMPTY;
    private Rect btnReset = Rect.EMPTY;

    private Rect hintBar = Rect.EMPTY;
    private Rect stackBadge = Rect.EMPTY;

    private Slider activeSlider = Slider.NONE;

    HudEditorUi(HudEditor editor) {
        this.editor = editor;
    }

    boolean isOverUi(double mouseX, double mouseY) {
        return toolbar.contains(mouseX, mouseY)
                || (editor.listOpen() && listPanel.contains(mouseX, mouseY))
                || (editor.selected() != null && propsPanel.contains(mouseX, mouseY))
                || hintBar.contains(mouseX, mouseY)
                || stackBadge.contains(mouseX, mouseY);
    }

    boolean blocksCanvasDrag(double mouseX, double mouseY) {
        return isOverUi(mouseX, mouseY);
    }

    boolean isPanelBackdrop(double mouseX, double mouseY) {
        return false;
    }

    int inspectorTop(int screenHeight) {
        return screenHeight - HINT_RESERVE - INSPECTOR_H - 6;
    }

    HudElement hoveredListElement(double mouseX, double mouseY) {
        int index = listRowIndexAt(mouseX, mouseY);
        return index >= 0 ? listElements.get(index) : null;
    }

    /** Test helper: simulate a row / eye click in the open elements popover. */
    boolean clickListRowForTest(int index, boolean eye) {
        if (!editor.listOpen() || index < 0 || index >= listElements.size() || listView.w() <= 0) {
            return false;
        }
        double y = listView.y() + index * ROW_HEIGHT + ROW_HEIGHT / 2.0 - listScroll;
        double x = eye ? listView.x() + listView.w() - 6 : listView.x() + 8;
        return mousePressed(x, y);
    }

    /** Test helper: click a toolbar label by matching drawn button order. */
    boolean clickToolbarButtonForTest(int index) {
        Rect[] buttons = {btnElements, btnGrid, btnGuides, btnSnap, btnUndo, btnRedo, btnResetAll};
        if (index < 0 || index >= buttons.length || buttons[index].w() <= 0) {
            return false;
        }
        Rect r = buttons[index];
        return mousePressed(r.x() + r.w() / 2.0, r.y() + r.h() / 2.0);
    }

    boolean scrollListForTest(double delta) {
        if (!editor.listOpen() || listView.w() <= 0) {
            return false;
        }
        return mouseScrolled(listView.x() + listView.w() / 2.0, listView.y() + listView.h() / 2.0, delta);
    }

    boolean clickVisibilityForTest() {
        if (btnVisibility.w() <= 0) {
            return false;
        }
        return mousePressed(btnVisibility.x() + btnVisibility.w() / 2.0,
                btnVisibility.y() + btnVisibility.h() / 2.0);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    boolean mousePressed(double mouseX, double mouseY) {
        if (stackBadge.contains(mouseX, mouseY)) {
            editor.cycleStackUnderCursor();
            return true;
        }
        if (toolbar.contains(mouseX, mouseY)) {
            if (btnElements.contains(mouseX, mouseY)) {
                editor.toggleList();
            } else if (btnGrid.contains(mouseX, mouseY)) {
                editor.toggleGrid();
            } else if (btnGuides.contains(mouseX, mouseY)) {
                editor.toggleGuides();
            } else if (btnSnap.contains(mouseX, mouseY)) {
                editor.toggleSnap();
            } else if (btnUndo.contains(mouseX, mouseY)) {
                editor.undo();
            } else if (btnRedo.contains(mouseX, mouseY)) {
                editor.redo();
            } else if (btnResetAll.contains(mouseX, mouseY)) {
                editor.resetAll();
            }
            return true;
        }
        if (editor.listOpen() && listPanel.contains(mouseX, mouseY)) {
            int index = listRowIndexAt(mouseX, mouseY);
            if (index >= 0) {
                HudElement element = listElements.get(index);
                if (mouseX >= listView.x() + listView.w() - 14) {
                    editor.toggleVisibility(element);
                } else {
                    editor.select(element);
                    editor.closeList();
                }
            }
            return true;
        }
        if (editor.selected() != null && propsPanel.contains(mouseX, mouseY)) {
            if (propsInteractiveHit(mouseX, mouseY)) {
                HudElement selected = editor.selected();
                if (btnVisibility.contains(mouseX, mouseY)) {
                    editor.toggleVisibility(selected);
                } else if (btnLock.contains(mouseX, mouseY)) {
                    editor.toggleLockSelected();
                } else if (trackHit(trackScale, mouseX, mouseY)) {
                    activeSlider = Slider.SCALE;
                    editor.beginGesture();
                    applySlider(mouseX);
                } else if (trackHit(trackOpacity, mouseX, mouseY)) {
                    activeSlider = Slider.OPACITY;
                    editor.beginGesture();
                    applySlider(mouseX);
                } else if (trackHit(trackRotation, mouseX, mouseY)) {
                    activeSlider = Slider.ROTATION;
                    editor.beginGesture();
                    applySlider(mouseX);
                } else if (swatchRow.contains(mouseX, mouseY)) {
                    int index = (int) ((mouseX - swatchRow.x()) / (SWATCH + 3));
                    if (index >= 0 && index < HudEditor.TINT_PRESETS.length
                            && mouseX - swatchRow.x() - index * (SWATCH + 3) < SWATCH) {
                        editor.setSelectedTint(HudEditor.TINT_PRESETS[index]);
                    }
                } else if (btnCenterX.contains(mouseX, mouseY)) {
                    editor.centerSelectedX();
                } else if (btnCenterY.contains(mouseX, mouseY)) {
                    editor.centerSelectedY();
                } else if (btnFront.contains(mouseX, mouseY)) {
                    editor.bringSelectedToFront();
                } else if (btnBack.contains(mouseX, mouseY)) {
                    editor.sendSelectedToBack();
                } else if (btnReset.contains(mouseX, mouseY)) {
                    editor.resetSelected();
                }
            }
            return true;
        }
        if (hintBar.contains(mouseX, mouseY)) {
            return true;
        }
        // Click outside closes the elements popover.
        if (editor.listOpen()) {
            editor.closeList();
        }
        return false;
    }

    private boolean propsInteractiveHit(double mouseX, double mouseY) {
        if (editor.selected() == null || !propsPanel.contains(mouseX, mouseY)) {
            return false;
        }
        return btnVisibility.contains(mouseX, mouseY)
                || btnLock.contains(mouseX, mouseY)
                || trackHit(trackScale, mouseX, mouseY)
                || trackHit(trackOpacity, mouseX, mouseY)
                || trackHit(trackRotation, mouseX, mouseY)
                || swatchRow.contains(mouseX, mouseY)
                || btnCenterX.contains(mouseX, mouseY)
                || btnCenterY.contains(mouseX, mouseY)
                || btnFront.contains(mouseX, mouseY)
                || btnBack.contains(mouseX, mouseY)
                || btnReset.contains(mouseX, mouseY);
    }

    private static boolean trackHit(Rect track, double mouseX, double mouseY) {
        return track.w() > 0
                && mouseX >= track.x() - 2 && mouseX < track.x() + track.w() + 2
                && mouseY >= track.y() - 4 && mouseY < track.y() + track.h() + 4;
    }

    boolean mouseDragged(double mouseX, double mouseY) {
        if (activeSlider == Slider.NONE) {
            return false;
        }
        applySlider(mouseX);
        return true;
    }

    void mouseReleased() {
        activeSlider = Slider.NONE;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (editor.listOpen() && listView.contains(mouseX, mouseY)) {
            int maxScroll = Math.max(0, listContentHeight - listView.h());
            if (maxScroll > 0) {
                listScroll = HudEditor.clampSafe((float) (listScroll - scrollDelta * ROW_HEIGHT * 2), 0, maxScroll);
                return true;
            }
        }
        return isOverUi(mouseX, mouseY);
    }

    private void applySlider(double mouseX) {
        HudElement selected = editor.selected();
        if (selected == null) {
            activeSlider = Slider.NONE;
            return;
        }
        Rect track = switch (activeSlider) {
            case SCALE -> trackScale;
            case OPACITY -> trackOpacity;
            case ROTATION -> trackRotation;
            case NONE -> Rect.EMPTY;
        };
        if (track.w() <= 0) {
            return;
        }
        editor.markMutated();
        float t = HudEditor.clampSafe((float) (mouseX - track.x()) / track.w(), 0f, 1f);
        switch (activeSlider) {
            case SCALE -> selected.setScale(HudElement.MIN_SCALE
                    + t * (HudElement.MAX_SCALE - HudElement.MIN_SCALE));
            case OPACITY -> selected.setOpacity(HudElement.MIN_OPACITY
                    + t * (HudElement.MAX_OPACITY - HudElement.MIN_OPACITY));
            case ROTATION -> selected.setRotation(snapRotation(-180f + t * 360f));
            case NONE -> {
            }
        }
    }

    private static float snapRotation(float degrees) {
        for (int snap = -180; snap <= 180; snap += 45) {
            if (Math.abs(degrees - snap) <= 3f) {
                return snap;
            }
        }
        return degrees;
    }

    private static float normalizedRotation(HudElement element) {
        float r = element.rotation() % 360f;
        if (r > 180f) {
            r -= 360f;
        }
        if (r < -180f) {
            r += 360f;
        }
        return r;
    }

    int listRowIndexAt(double mouseX, double mouseY) {
        if (!listView.contains(mouseX, mouseY)) {
            return -1;
        }
        int index = (int) ((mouseY - listView.y() + listScroll) / ROW_HEIGHT);
        return index >= 0 && index < listElements.size() ? index : -1;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        renderToolbar(ctx, theme, mouseX, mouseY);
        renderElementPopover(ctx, theme, mouseX, mouseY);
        renderInspector(ctx, theme, mouseX, mouseY);
        renderStackBadge(ctx, theme, mouseX, mouseY);
        renderHints(ctx, theme);
    }

    private void renderToolbar(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        String title = "HUD";
        String[] labels = {"Elements", "Grid", "Guides", "Snap", "Undo", "Redo", "Reset"};
        int pad = 5;
        int gap = 3;
        int buttonH = 15;
        int titleW = ctx.uiTextWidth(title);
        int total = titleW + 8;
        int[] widths = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            widths[i] = ctx.uiTextWidth(labels[i]) + pad * 2;
            total += widths[i] + gap;
        }
        int x = Math.max(4, (ctx.screenWidth() - total - pad * 2) / 2);
        int y = TOOLBAR_Y;
        toolbar = new Rect(x, y, total + pad * 2, buttonH + 6);
        UiChrome.editorPanel(ctx, theme, toolbar.x(), toolbar.y(), toolbar.w(), toolbar.h());
        int textY = y + 3 + (buttonH - ctx.uiFontHeight()) / 2;
        int cursor = x + pad;
        ctx.drawUiText(title, cursor, textY, theme.accent());
        cursor += titleW + 8;
        Rect[] rects = new Rect[labels.length];
        boolean[] active = {
                editor.listOpen(), editor.gridShown(), editor.guidesOn(), editor.snapOn(),
                false, false, false
        };
        boolean[] enabled = {true, true, true, true, editor.canUndo(), editor.canRedo(), true};
        for (int i = 0; i < labels.length; i++) {
            rects[i] = new Rect(cursor, y + 3, widths[i], buttonH);
            boolean hover = enabled[i] && rects[i].contains(mouseX, mouseY);
            UiChrome.button(ctx, theme, cursor, y + 3, widths[i], buttonH, hover, active[i]);
            int color = !enabled[i] ? ColorUtil.withAlpha(theme.foregroundMuted(), 0.5f)
                    : active[i] ? 0xFFFFFFFF : theme.foreground();
            ctx.drawUiText(labels[i], cursor + pad, textY, color);
            cursor += widths[i] + gap;
        }
        btnElements = rects[0];
        btnGrid = rects[1];
        btnGuides = rects[2];
        btnSnap = rects[3];
        btnUndo = rects[4];
        btnRedo = rects[5];
        btnResetAll = rects[6];
    }

    private void renderElementPopover(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        if (!editor.listOpen()) {
            listPanel = Rect.EMPTY;
            listView = Rect.EMPTY;
            listElements.clear();
            return;
        }
        listElements.clear();
        listElements.addAll(editor.hud().all());
        int x = Math.max(6, btnElements.x());
        int y = toolbar.y() + toolbar.h() + 4;
        int contentH = Math.min(POPOVER_MAX_H, 28 + listElements.size() * ROW_HEIGHT);
        int h = Math.max(48, contentH);
        if (y + h > inspectorTop(ctx.screenHeight()) - 4) {
            h = Math.max(48, inspectorTop(ctx.screenHeight()) - y - 4);
        }
        if (x + POPOVER_WIDTH > ctx.screenWidth() - 6) {
            x = ctx.screenWidth() - POPOVER_WIDTH - 6;
        }
        listPanel = new Rect(x, y, POPOVER_WIDTH, h);
        UiChrome.editorPanel(ctx, theme, x, y, POPOVER_WIDTH, h);
        ctx.drawUiText("Elements", x + 8, y + 5, theme.foregroundMuted());
        int viewY = y + 5 + ctx.uiFontHeight() + 4;
        listView = new Rect(x + 4, viewY, POPOVER_WIDTH - 8, y + h - viewY - 5);
        listContentHeight = listElements.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, listContentHeight - listView.h());
        listScroll = HudEditor.clampSafe(listScroll, 0, maxScroll);

        ctx.pushClip(listView.x(), listView.y(), listView.w(), listView.h());
        int rowY = listView.y() - Math.round(listScroll);
        for (HudElement element : listElements) {
            if (rowY + ROW_HEIGHT >= listView.y() && rowY < listView.y() + listView.h()) {
                boolean isSelected = element == editor.selected();
                boolean hover = listView.contains(mouseX, mouseY)
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                if (isSelected || hover) {
                    ctx.fillRoundedRect(listView.x(), rowY, listView.w(), ROW_HEIGHT - 1, PrimeDesign.RADIUS_SM,
                            ColorUtil.withAlpha(isSelected ? theme.accent() : theme.surfaceElevated(),
                                    isSelected ? 0.28f : 0.5f));
                }
                int nameColor;
                if (!element.isActive()) {
                    nameColor = ColorUtil.withAlpha(theme.warning(), 0.85f);
                } else if (element.isVisible()) {
                    nameColor = theme.foreground();
                } else {
                    nameColor = ColorUtil.withAlpha(theme.foregroundMuted(), 0.7f);
                }
                ctx.drawUiText(truncate(ctx, element.name(), listView.w() - 20),
                        listView.x() + 3, rowY + (ROW_HEIGHT - ctx.uiFontHeight()) / 2, nameColor);
                int dot = !element.isActive()
                        ? ColorUtil.withAlpha(theme.warning(), 0.9f)
                        : element.isVisible() ? theme.success() : ColorUtil.withAlpha(theme.error(), 0.8f);
                ctx.fillRoundedRect(listView.x() + listView.w() - 9, rowY + (ROW_HEIGHT - 5) / 2, 5, 5, 2, dot);
            }
            rowY += ROW_HEIGHT;
        }
        ctx.popClip();
    }

    private void renderInspector(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        HudElement selected = editor.selected();
        if (selected == null) {
            propsPanel = Rect.EMPTY;
            btnVisibility = Rect.EMPTY;
            btnLock = Rect.EMPTY;
            trackScale = Rect.EMPTY;
            trackOpacity = Rect.EMPTY;
            trackRotation = Rect.EMPTY;
            swatchRow = Rect.EMPTY;
            btnCenterX = Rect.EMPTY;
            btnCenterY = Rect.EMPTY;
            btnFront = Rect.EMPTY;
            btnBack = Rect.EMPTY;
            btnReset = Rect.EMPTY;
            return;
        }
        int pad = 6;
        int w = Math.min(440, ctx.screenWidth() - 16);
        int x = (ctx.screenWidth() - w) / 2;
        int y = inspectorTop(ctx.screenHeight());
        int h = INSPECTOR_H;
        propsPanel = new Rect(x, y, w, h);
        UiChrome.editorPanel(ctx, theme, x, y, w, h);

        int row1Y = y + 5;
        String title = selected.name();
        if (!selected.isActive()) {
            title = title + " · off";
        }
        int nameW = Math.min(ctx.uiTextWidth(title) + 4, 96);
        ctx.drawUiText(truncate(ctx, title, 92), x + pad, row1Y, theme.foreground());

        boolean visible = selected.isVisible();
        btnVisibility = new Rect(x + pad + nameW + 4, row1Y - 1, 44, 13);
        UiChrome.button(ctx, theme, btnVisibility.x(), btnVisibility.y(), btnVisibility.w(), 13,
                btnVisibility.contains(mouseX, mouseY), visible);
        String visLabel = visible ? "Hide" : "Show";
        ctx.drawUiText(visLabel, btnVisibility.x() + (btnVisibility.w() - ctx.uiTextWidth(visLabel)) / 2,
                row1Y, visible ? 0xFFFFFFFF : theme.foregroundMuted());

        boolean locked = selected.isLocked();
        btnLock = new Rect(btnVisibility.x() + btnVisibility.w() + 3, row1Y - 1, 44, 13);
        UiChrome.button(ctx, theme, btnLock.x(), btnLock.y(), btnLock.w(), 13,
                btnLock.contains(mouseX, mouseY), locked);
        String lockLabel = locked ? "Unlock" : "Lock";
        ctx.drawUiText(lockLabel, btnLock.x() + (btnLock.w() - ctx.uiTextWidth(lockLabel)) / 2,
                row1Y, locked ? 0xFFFFFFFF : theme.foregroundMuted());

        int sliderX = btnLock.x() + btnLock.w() + 6;
        int sliderW = Math.max(54, (x + w - pad - sliderX - 6) / 3);
        trackScale = renderSlider(ctx, theme, sliderX, row1Y, sliderW, "Scale",
                String.format("%.1fx", selected.scale()),
                (selected.scale() - HudElement.MIN_SCALE) / (HudElement.MAX_SCALE - HudElement.MIN_SCALE));
        trackOpacity = renderSlider(ctx, theme, sliderX + sliderW + 5, row1Y, sliderW, "Opacity",
                String.format("%.0f%%", selected.opacity() * 100f),
                (selected.opacity() - HudElement.MIN_OPACITY) / (HudElement.MAX_OPACITY - HudElement.MIN_OPACITY));
        float rotation = normalizedRotation(selected);
        trackRotation = renderSlider(ctx, theme, sliderX + (sliderW + 5) * 2, row1Y, sliderW, "Rot",
                String.format("%.0f°", rotation),
                (rotation + 180f) / 360f);

        int row2Y = y + h - SWATCH - 7;
        swatchRow = new Rect(x + pad, row2Y, HudEditor.TINT_PRESETS.length * (SWATCH + 2), SWATCH);
        for (int i = 0; i < HudEditor.TINT_PRESETS.length; i++) {
            int tint = HudEditor.TINT_PRESETS[i];
            int sx = x + pad + i * (SWATCH + 2);
            boolean current = selected.tintArgb() == tint;
            if (tint == 0) {
                ctx.fillRoundedBorder(sx, row2Y, SWATCH, SWATCH, PrimeDesign.RADIUS_SM, 1,
                        current ? theme.accent() : theme.border(), theme.backgroundLight());
            } else {
                ctx.fillRoundedBorder(sx, row2Y, SWATCH, SWATCH, PrimeDesign.RADIUS_SM, 1,
                        current ? theme.accent() : ColorUtil.withAlpha(theme.border(), 0.5f), tint);
            }
        }

        int btnW = 42;
        int btnY = row2Y - 1;
        btnFront = new Rect(x + w - pad - btnW * 5 - 12, btnY, btnW, 13);
        btnBack = new Rect(x + w - pad - btnW * 4 - 9, btnY, btnW, 13);
        btnCenterX = new Rect(x + w - pad - btnW * 3 - 6, btnY, btnW, 13);
        btnCenterY = new Rect(x + w - pad - btnW * 2 - 3, btnY, btnW, 13);
        btnReset = new Rect(x + w - pad - btnW, btnY, btnW, 13);
        drawSmallButton(ctx, theme, btnFront, "Front", mouseX, mouseY, theme.foreground());
        drawSmallButton(ctx, theme, btnBack, "Back", mouseX, mouseY, theme.foreground());
        drawSmallButton(ctx, theme, btnCenterX, "Ctr X", mouseX, mouseY, theme.foreground());
        drawSmallButton(ctx, theme, btnCenterY, "Ctr Y", mouseX, mouseY, theme.foreground());
        drawSmallButton(ctx, theme, btnReset, "Reset", mouseX, mouseY, theme.warning());
    }

    private void renderStackBadge(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        int count = editor.stackCountUnderCursor(mouseX, mouseY);
        if (count < 2 || isOverUi(mouseX, mouseY)) {
            stackBadge = Rect.EMPTY;
            return;
        }
        String label = count + " · Alt";
        int w = ctx.uiTextWidth(label) + 10;
        int h = ctx.uiFontHeight() + 4;
        int x = (int) Math.min(mouseX + 12, ctx.screenWidth() - w - 4);
        int y = (int) Math.min(mouseY + 14, ctx.screenHeight() - h - HINT_RESERVE - 8);
        stackBadge = new Rect(x, y, w, h);
        ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_SM,
                ColorUtil.withAlpha(theme.surfaceElevated(), 0.92f));
        ctx.drawUiText(label, x + 5, y + 2, theme.accent());
    }

    private void drawSmallButton(RenderContext ctx, Theme theme, Rect rect, String label,
                                 double mouseX, double mouseY, int textColor) {
        UiChrome.button(ctx, theme, rect.x(), rect.y(), rect.w(), rect.h(),
                rect.contains(mouseX, mouseY), false);
        ctx.drawUiText(label, rect.x() + (rect.w() - ctx.uiTextWidth(label)) / 2,
                rect.y() + (rect.h() - ctx.uiFontHeight()) / 2, textColor);
    }

    private Rect renderSlider(RenderContext ctx, Theme theme, int x, int y, int w,
                              String label, String value, float t) {
        ctx.drawUiText(label, x, y, theme.foregroundMuted());
        ctx.drawUiText(value, x + w - ctx.uiTextWidth(value), y, theme.foreground());
        int trackY = y + ctx.uiFontHeight() + 2;
        Rect track = new Rect(x, trackY, w, 3);
        ctx.fillRoundedRect(x, trackY, w, 3, 1, ColorUtil.withAlpha(theme.backgroundLight(), 0.9f));
        int fill = Math.round(HudEditor.clampSafe(t, 0f, 1f) * w);
        if (fill > 0) {
            ctx.fillRoundedRect(x, trackY, fill, 3, 1, theme.accent());
        }
        int knobX = x + Math.round(HudEditor.clampSafe(t, 0f, 1f) * (w - 4));
        ctx.fillRoundedRect(knobX, trackY - 2, 4, 7, 2, 0xFFFFFFFF);
        return track;
    }

    private void renderHints(RenderContext ctx, Theme theme) {
        int fontH = ctx.uiFontHeight();
        String line = HudEditorHints.LINE_1;
        int w = Math.min(ctx.screenWidth() - 24, ctx.uiTextWidth(line) + 16);
        int x = (ctx.screenWidth() - w) / 2;
        int y = ctx.screenHeight() - fontH - 4;
        hintBar = new Rect(x, y - 2, w, fontH + 6);
        ctx.fillRoundedRect(hintBar.x(), hintBar.y(), hintBar.w(), hintBar.h(),
                PrimeDesign.RADIUS_SM, ColorUtil.withAlpha(theme.surfaceElevated(), 0.45f));
        ctx.drawUiText(line, x + 8, y, ColorUtil.withAlpha(theme.foregroundMuted(), 0.88f));
    }

    private static String truncate(RenderContext ctx, String text, int maxWidth) {
        if (ctx.uiTextWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int budget = maxWidth - ctx.uiTextWidth(ellipsis);
        if (budget <= 0) {
            return ellipsis;
        }
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (ctx.uiTextWidth(text.substring(0, mid)) <= budget) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return text.substring(0, lo) + ellipsis;
    }
}
