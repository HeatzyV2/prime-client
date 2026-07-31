package dev.primeclient.core.cosmetics;

/** Definition for a client-side emote animation (pose keyframes). */
public record EmoteDefinition(
        String id,
        String name,
        int durationTicks,
        Pose[] keyframes
) {
    /** Simple body pose multipliers applied by the version-layer animator. */
    public record Pose(
            float armYaw,
            float armPitch,
            float bodyYaw,
            float headPitch,
            float crouch
    ) {
        public static final Pose NEUTRAL = new Pose(0, 0, 0, 0, 0);
    }

    public Pose sample(float progress) {
        if (keyframes == null || keyframes.length == 0) {
            return Pose.NEUTRAL;
        }
        if (keyframes.length == 1) {
            return keyframes[0];
        }
        float clamped = Math.max(0f, Math.min(1f, progress));
        float scaled = clamped * (keyframes.length - 1);
        int i = Math.min(keyframes.length - 2, (int) scaled);
        float t = scaled - i;
        Pose a = keyframes[i];
        Pose b = keyframes[i + 1];
        return new Pose(
                lerp(a.armYaw, b.armYaw, t),
                lerp(a.armPitch, b.armPitch, t),
                lerp(a.bodyYaw, b.bodyYaw, t),
                lerp(a.headPitch, b.headPitch, t),
                lerp(a.crouch, b.crouch, t));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
