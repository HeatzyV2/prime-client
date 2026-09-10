package dev.primeclient.core.hud.editor;

/**
 * Centralized keyboard/mouse hint strings for the HUD editor chrome.
 */
public final class HudEditorHints {

    public static final String LINE_1 =
            "List · drag · Alt+click cycle · Arrows nudge · L lock · H guides · G grid · Esc";
    public static final String LINE_2 = "";

    private HudEditorHints() {
    }

    public static String[] lines() {
        return new String[] {
            "LMB drag · RMB props · Scroll scale",
            "Arrows nudge · Shift ×4 · Alt+click cycle",
            "G grid · H guides · S snap · L lock",
            "E / toolbar Elements · Del remove",
            "Ctrl+Z / Y undo · R reset",
            "Esc close"
        };
    }
}
