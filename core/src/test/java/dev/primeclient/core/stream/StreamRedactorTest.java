package dev.primeclient.core.stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamRedactorTest {

    @BeforeEach
    void enableRedaction() {
        StreamerPrivacyState.reset();
        StreamerPrivacyState.setRedactCoords(true);
        StreamerPrivacyState.setRedactIps(true);
        StreamerPrivacyState.setRedactWhispers(true);
    }

    @AfterEach
    void reset() {
        StreamerPrivacyState.reset();
    }

    @Test
    void redactsLabeledCoordinates() {
        String result = StreamRedactor.redact("Position X: 120 Y: 64 Z: -340");
        assertFalse(result.contains("120"));
        assertFalse(result.contains("-340"));
        assertTrue(result.contains("[hidden]"));
    }

    @Test
    void redactsIpv4Addresses() {
        String result = StreamRedactor.redact("Connect to 192.168.0.42:25565");
        assertFalse(result.contains("192.168.0.42"));
        assertTrue(result.contains("[hidden]"));
    }

    @Test
    void redactComponentMatchesRedact() {
        String input = "Teleport to [10, 64, -20]";
        assertEquals(StreamRedactor.redact(input), StreamRedactor.redactComponent(input));
    }

    @Test
    void masksPlayerNamesStably() {
        String first = StreamNameMask.maskPlayerName("Notch");
        String second = StreamNameMask.maskPlayerName("Notch");
        assertEquals(first, second);
        assertTrue(first.startsWith("Player_"));
        assertNotEquals("Notch", first);
    }
}
