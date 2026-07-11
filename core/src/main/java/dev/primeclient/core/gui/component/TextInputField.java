package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;

/**
 * Single-line text field with cursor, backspace and character limit.
 * Allocation-free during typing (reuses {@link StringBuilder}).
 */
public final class TextInputField {

    private final StringBuilder buffer = new StringBuilder();
    private final String placeholder;
    private final int maxLength;
    private boolean focused;
    private int cursor;
    private long blinkMillis;

    public TextInputField(String initial, String placeholder, int maxLength) {
        this.placeholder = placeholder == null ? "" : placeholder;
        this.maxLength = Math.max(1, maxLength);
        setText(initial);
    }

    public void setText(String text) {
        buffer.setLength(0);
        if (text != null) {
            int len = Math.min(text.length(), maxLength);
            buffer.append(text, 0, len);
        }
        cursor = buffer.length();
    }

    public String text() {
        return buffer.toString();
    }

    public boolean focused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            blinkMillis = System.currentTimeMillis();
        }
    }

    public boolean charTyped(char c) {
        if (!focused || c < ' ' || buffer.length() >= maxLength) {
            return false;
        }
        buffer.insert(cursor, c);
        cursor++;
        blinkMillis = System.currentTimeMillis();
        return true;
    }

    public boolean keyPressed(int glfwKey) {
        if (!focused) {
            return false;
        }
        if (glfwKey == 259) { // Backspace
            if (cursor > 0 && !buffer.isEmpty()) {
                buffer.deleteCharAt(cursor - 1);
                cursor--;
                blinkMillis = System.currentTimeMillis();
            }
            return true;
        }
        if (glfwKey == 261) { // Delete
            if (cursor < buffer.length()) {
                buffer.deleteCharAt(cursor);
                blinkMillis = System.currentTimeMillis();
            }
            return true;
        }
        if (glfwKey == 263 && cursor > 0) { // Left
            cursor--;
            blinkMillis = System.currentTimeMillis();
            return true;
        }
        if (glfwKey == 262 && cursor < buffer.length()) { // Right
            cursor++;
            blinkMillis = System.currentTimeMillis();
            return true;
        }
        if (glfwKey == 268) { // Home
            cursor = 0;
            return true;
        }
        if (glfwKey == 269) { // End
            cursor = buffer.length();
            return true;
        }
        return false;
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, int width) {
        int h = PrimeDesign.INPUT_HEIGHT;
        ctx.fillRect(x, y, width, h, focused ? theme.surfaceElevated() : theme.backgroundLight());
        ctx.fillRect(x, y + h - 1, width, 1, focused ? theme.accent() : theme.border());

        String display = buffer.isEmpty() ? placeholder : buffer.toString();
        int color = buffer.isEmpty() ? theme.foregroundMuted() : theme.foreground();
        int textY = y + (h - ctx.fontHeight()) / 2 + 1;
        int textX = x + PrimeDesign.SPACE_SM;
        int maxTextW = width - PrimeDesign.SPACE_SM * 2;
        ctx.drawText(trimToWidth(ctx, display, maxTextW), textX, textY, color, true);

        if (focused && (System.currentTimeMillis() - blinkMillis) % 1000 < 500) {
            String before = buffer.substring(0, cursor);
            int cx = textX + ctx.textWidth(trimToWidth(ctx, before, maxTextW));
            ctx.fillRect(cx, textY - 1, 1, ctx.fontHeight() + 1, theme.accent());
        }
    }

    public boolean hit(double mx, double my, int x, int y, int width) {
        return mx >= x && mx < x + width && my >= y && my < y + PrimeDesign.INPUT_HEIGHT;
    }

    private static String trimToWidth(RenderContext ctx, String text, int maxWidth) {
        if (ctx.textWidth(text) <= maxWidth) {
            return text;
        }
        for (int i = text.length() - 1; i >= 0; i--) {
            String sub = text.substring(0, i);
            if (ctx.textWidth(sub) <= maxWidth) {
                return sub;
            }
        }
        return "";
    }
}
