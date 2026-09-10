package dev.primeclient.core.hud.editor;

/**
 * Centralized keyboard/mouse hint strings for the HUD editor chrome.
 */
public final class HudEditorHints {

    public static final String LINE_1 =
            "Drag · corners resize · Alt+click stack · E elements · L lock · Esc";
    public static final String LINE_2 = "";

    private HudEditorHints() {
    }

    public static String[] lines() {
        return new String[] {
            "LMB drag · corner handles resize · Alt+click cycle stack",
            "Scroll scale · Shift+scroll rotate · Ctrl+scroll opacity",
            "E elements popover · G grid · H guides · S snap · L lock",
            "Front/Back · R tint · Ctrl+Z / Y undo · Esc close"
        };
    }
}
