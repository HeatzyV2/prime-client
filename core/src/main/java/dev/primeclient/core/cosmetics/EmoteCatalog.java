package dev.primeclient.core.cosmetics;

import java.util.LinkedHashMap;
import java.util.Map;

/** Built-in emote catalog (9 emotes). */
public final class EmoteCatalog {

    private static final Map<String, EmoteDefinition> EMOTES = new LinkedHashMap<>();

    static {
        register("emote-wave", "Wave", 40,
                p(0, -40, 0, 0, 0), p(20, -70, 0, -5, 0), p(-20, -70, 0, -5, 0), p(0, -40, 0, 0, 0));
        register("emote-dance", "Dance", 60,
                p(30, -20, 10, 0, 0), p(-30, -20, -10, 0, 0.05f),
                p(30, -20, 10, 0, 0), p(-30, -20, -10, 0, 0.05f));
        register("emote-sit", "Sit", 80,
                p(0, 20, 0, 10, 0.45f), p(0, 20, 0, 10, 0.45f));
        register("emote-laugh", "Laugh", 40,
                p(15, -10, 5, -15, 0), p(-15, -10, -5, -10, 0.02f), p(15, -10, 5, -15, 0));
        register("emote-cry", "Cry", 50,
                p(0, 30, 0, 35, 0.1f), p(5, 35, 0, 40, 0.12f), p(0, 30, 0, 35, 0.1f));
        register("emote-flex", "Flex", 45,
                p(0, -100, 0, -10, 0), p(0, -110, 0, -15, 0), p(0, -100, 0, -10, 0));
        register("emote-clap", "Clap", 36,
                p(25, -50, 0, 0, 0), p(-25, -50, 0, 0, 0), p(25, -50, 0, 0, 0), p(-25, -50, 0, 0, 0));
        register("emote-sleep", "Sleep", 100,
                p(0, 40, 0, 70, 0.55f), p(0, 40, 0, 70, 0.55f));
        register("emote-victory", "Victory", 50,
                p(0, -140, 0, -20, 0), p(20, -150, 5, -25, 0), p(-20, -150, -5, -25, 0), p(0, -140, 0, -20, 0));
    }

    private EmoteCatalog() {
    }

    private static EmoteDefinition.Pose p(float armYaw, float armPitch, float bodyYaw, float headPitch, float crouch) {
        return new EmoteDefinition.Pose(armYaw, armPitch, bodyYaw, headPitch, crouch);
    }

    private static void register(String id, String name, int ticks, EmoteDefinition.Pose... frames) {
        EMOTES.put(id, new EmoteDefinition(id, name, ticks, frames));
    }

    public static EmoteDefinition get(String id) {
        return id == null ? null : EMOTES.get(id);
    }

    public static Map<String, EmoteDefinition> all() {
        return EMOTES;
    }

    public static boolean isKnown(String id) {
        return id != null && EMOTES.containsKey(id);
    }
}
