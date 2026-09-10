package dev.primeclient.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorUtilHexTest {

    @Test
    void parsesRgbShortAndLong() {
        assertEquals(0xFFFF0000, ColorUtil.parseHex("#F00", 0));
        assertEquals(0xFFFF0000, ColorUtil.parseHex("#FF0000", 0));
        assertEquals(0x80FF0000, ColorUtil.parseHex("#80FF0000", 0));
    }

    @Test
    void rejectsInvalid() {
        assertFalse(ColorUtil.isHex(""));
        assertFalse(ColorUtil.isHex("#GG0000"));
        assertEquals(0x12345678, ColorUtil.parseHex("nope", 0x12345678));
    }

    @Test
    void roundTripArgb() {
        int argb = 0xCC3366AA;
        assertTrue(ColorUtil.isHex(ColorUtil.toHex(argb)));
        assertEquals(argb, ColorUtil.parseHex(ColorUtil.toHex(argb), 0));
    }
}
