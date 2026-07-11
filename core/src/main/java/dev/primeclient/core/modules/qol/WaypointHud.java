package dev.primeclient.core.modules.qol;

import dev.primeclient.core.adapter.MinecraftAdapter;

/** Shared waypoint HUD helpers. */
final class WaypointHud {

    private WaypointHud() {
    }

    static String directionTo(MinecraftAdapter adapter, double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = (angle - adapter.playerYaw() + 360) % 360;
        if (relative < 45 || relative >= 315) {
            return "Ahead";
        }
        if (relative < 135) {
            return "Right";
        }
        if (relative < 225) {
            return "Behind";
        }
        return "Left";
    }
}
