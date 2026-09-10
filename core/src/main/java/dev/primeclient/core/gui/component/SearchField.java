package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/**
 * Search field built on {@link TextInputField} with a leading affordance and
 * clear-on-Escape semantics owned by the parent.
 */
public final class SearchField {

    private final TextInputField field;
    private float focusProgress;

    public SearchField(String placeholder, int maxLength) {
        this.field = new TextInputField("", placeholder == null ? "Search…" : placeholder, maxLength);
    }

    public TextInputField field() {
        return field;
    }

    public String query() {
        return field.text();
    }

    public void setQuery(String query) {
        field.setText(query);
    }

    public void clear() {
        field.setText("");
    }

    public void setFocused(boolean focused) {
        field.setFocused(focused);
    }

    public boolean focused() {
        return field.focused();
    }

    public void tick(float deltaSeconds) {
        focusProgress = PrimeDesign.animateMs(focusProgress, field.focused() ? 1f : 0f,
                deltaSeconds, PrimeDesign.DURATION_HOVER_MS);
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, int width) {
        int h = PrimeDesign.SEARCH_HEIGHT;
        int radius = PrimeDesign.RADIUS_MD;
        ctx.fillRoundedRect(x, y, width, h, radius,
                ColorUtil.withAlpha(theme.surfaceElevated(), 0.92f));
        int accentA = Math.round(0x40 + 0x80 * focusProgress);
        ctx.fillRect(x + radius, y + h - 1, width - radius * 2, 1,
                ColorUtil.withAlpha(theme.accent(), accentA / 255f));
        String mark = "⌕";
        ctx.drawUiText(mark, x + PrimeDesign.SPACE_SM, y + (h - ctx.uiFontHeight()) / 2,
                theme.foregroundMuted());
        int fieldX = x + PrimeDesign.SPACE_SM + ctx.uiTextWidth(mark) + PrimeDesign.SPACE_SM;
        int fieldW = width - (fieldX - x) - PrimeDesign.SPACE_SM;
        // Reuse TextInputField drawing inset into the search chrome.
        field.render(ctx, theme, fieldX, y + (h - PrimeDesign.INPUT_HEIGHT) / 2, fieldW);
    }

    public boolean hit(double mx, double my, int x, int y, int width) {
        return mx >= x && mx < x + width && my >= y && my < y + PrimeDesign.SEARCH_HEIGHT;
    }

    public boolean charTyped(char c) {
        return field.charTyped(c);
    }

    public boolean keyPressed(int glfwKey) {
        return field.keyPressed(glfwKey);
    }
}
