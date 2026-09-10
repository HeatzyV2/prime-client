package dev.primeclient.core.cosmetics;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.state.EmoteState;

import java.util.Locale;

/** Client-only {@code /emotes} command + wheel helper. */
public final class EmoteService {

    private final MinecraftAdapter adapter;

    public EmoteService(MinecraftAdapter adapter) {
        this.adapter = adapter;
    }

    public boolean handleClientCommand(String raw) {
        if (raw == null) {
            return false;
        }
        String text = raw.trim();
        if (!text.regionMatches(true, 0, "/emotes", 0, 7)
                && !text.regionMatches(true, 0, "/emote", 0, 6)) {
            return false;
        }
        // Avoid matching unrelated commands
        if (text.regionMatches(true, 0, "/emotes", 0, 7)) {
            if (text.length() > 7 && !Character.isWhitespace(text.charAt(7))) {
                return false;
            }
        } else if (text.length() > 6 && !Character.isWhitespace(text.charAt(6))) {
            return false;
        }

        String rest;
        if (text.regionMatches(true, 0, "/emotes", 0, 7)) {
            rest = text.length() > 7 ? text.substring(7).trim() : "";
        } else {
            rest = text.length() > 6 ? text.substring(6).trim() : "";
        }

        if (rest.isEmpty() || "help".equalsIgnoreCase(rest) || "?".equals(rest) || "list".equalsIgnoreCase(rest)) {
            if (rest.isEmpty()) {
                EmoteState.setWheelOpen(!EmoteState.wheelOpen());
                say(EmoteState.wheelOpen()
                        ? "Emote wheel opened — click an emote or type §f/emote <id>§7."
                        : "Emote wheel closed.");
            } else {
                printHelp();
            }
            return true;
        }
        if ("close".equalsIgnoreCase(rest) || "cancel".equalsIgnoreCase(rest)) {
            EmoteState.setWheelOpen(false);
            say("Emote wheel closed.");
            return true;
        }

        String id = rest.toLowerCase(Locale.ROOT);
        if (!id.startsWith("emote-")) {
            id = "emote-" + id;
        }
        if (!EmoteCatalog.isKnown(id)) {
            say("§cUnknown emote. §7Try §f/emotes list§7.");
            return true;
        }
        EmoteState.setWheelOpen(false);
        EmoteState.playLocal(id);
        EmoteDefinition def = EmoteCatalog.get(id);
        say("Playing §f" + (def != null ? def.name() : id) + "§7.");
        return true;
    }

    private void printHelp() {
        say("Prime Emotes — visible to Prime peers on LAN / integrated.");
        say("§f/emotes §7— toggle emote wheel");
        say("§f/emote <name> §7— play (wave dance sit laugh cry flex clap sleep victory)");
        StringBuilder list = new StringBuilder("§7Available: §f");
        boolean first = true;
        for (String id : EmoteCatalog.all().keySet()) {
            if (!first) {
                list.append("§7, §f");
            }
            list.append(id.substring("emote-".length()));
            first = false;
        }
        say(list.toString());
    }

    private void say(String message) {
        adapter.runOnClientThread(() -> adapter.displayClientMessage("§c[Prime] §7" + message));
    }
}
