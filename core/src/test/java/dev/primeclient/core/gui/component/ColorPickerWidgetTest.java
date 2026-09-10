package dev.primeclient.core.gui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorPickerWidgetTest {

    @Test
    void applyHexUpdatesColor() {
        ColorPickerWidget picker = new ColorPickerWidget();
        picker.load(0xFFFFFFFF);
        assertTrue(picker.applyHex("#FF0000"));
        assertEquals(0xFFFF0000, picker.selectedArgb());
        assertEquals("#FFFF0000", picker.hex());
    }

    @Test
    void rejectBadHex() {
        ColorPickerWidget picker = new ColorPickerWidget();
        picker.load(0xFF00FF00);
        assertFalse(picker.applyHex("red"));
        assertEquals(0xFF00FF00, picker.selectedArgb());
    }
}
