package dev.primeclient.core.hook;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.event.AttackEntityEvent;
import dev.primeclient.core.event.ChatMessageEvent;
import dev.primeclient.core.event.PlayerDamageEvent;
import dev.primeclient.core.event.PlayerDeathEvent;
import dev.primeclient.core.state.CinematicCameraState;
import dev.primeclient.core.state.ChatFilterState;
import dev.primeclient.core.state.ChatOverlayState;
import dev.primeclient.core.state.ZoomState;

/**
 * Bridge from version-layer Fabric hooks and mixins into the common core.
 *
 * <p>Every method is null-safe: hooks may fire before bootstrap or after
 * shutdown.</p>
 */
public final class PrimeHooks {

    private PrimeHooks() {
    }

    public static void onChatMessage(String text, boolean outgoing) {
        if (!outgoing && ChatFilterState.shouldFilter(text)) {
            return;
        }
        PrimeClient client = tryGet();
        if (client != null) {
            client.events().post(new ChatMessageEvent(text, outgoing));
        }
    }

    /** Formats incoming chat for display; called by version-layer chat hooks. */
    public static String formatChatMessage(String text, boolean outgoing, long timestampMillis) {
        if (outgoing) {
            return text;
        }
        return ChatOverlayState.formatIncoming(text, timestampMillis);
    }

    public static void onAttackEntity(String targetName) {
        PrimeClient client = tryGet();
        if (client != null) {
            client.events().post(new AttackEntityEvent(targetName));
        }
    }

    public static void onPlayerDamage(float amount) {
        PrimeClient client = tryGet();
        if (client != null) {
            client.events().post(new PlayerDamageEvent(amount));
        }
    }

    public static void onPlayerDeath(double x, double y, double z) {
        PrimeClient client = tryGet();
        if (client != null) {
            client.events().post(new PlayerDeathEvent(x, y, z));
        }
    }

    /** Called by the GameRenderer mixin to apply zoom FOV. */
    public static float fovMultiplier() {
        return ZoomState.multiplier();
    }

    /** Called by the Camera mixin to apply cinematic smoothing. */
    public static boolean cinematicCameraActive() {
        return CinematicCameraState.active();
    }

    public static float cinematicYaw() {
        return CinematicCameraState.yaw();
    }

    public static float cinematicPitch() {
        return CinematicCameraState.pitch();
    }

    public static boolean hideVanillaCrosshair() {
        return dev.primeclient.core.state.CrosshairState.hideVanillaCrosshair();
    }

    private static PrimeClient tryGet() {
        try {
            return PrimeClient.get();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }
}
