package dev.primeclient.core.design;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimeDesignTest {

    @Test
    void animateMsReachesTargetWithEnoughTime() {
        boolean previous = PrimeDesign.reducedMotion;
        PrimeDesign.reducedMotion = false;
        try {
            float value = 0f;
            for (int i = 0; i < 20; i++) {
                value = PrimeDesign.animateMs(value, 1f, 0.016f, PrimeDesign.DURATION_HOVER_MS);
            }
            assertTrue(value > 0.9f);
        } finally {
            PrimeDesign.reducedMotion = previous;
        }
    }

    @Test
    void reducedMotionSnaps() {
        boolean previous = PrimeDesign.reducedMotion;
        PrimeDesign.reducedMotion = true;
        try {
            assertEquals(1f, PrimeDesign.animateMs(0f, 1f, 0.001f, 1000));
        } finally {
            PrimeDesign.reducedMotion = previous;
        }
    }

    @Test
    void easeOutCubicBounds() {
        assertEquals(0f, PrimeDesign.easeOutCubic(0f), 1e-5);
        assertEquals(1f, PrimeDesign.easeOutCubic(1f), 1e-5);
        assertTrue(PrimeDesign.easeOutCubic(0.5f) > 0.5f);
    }
}
