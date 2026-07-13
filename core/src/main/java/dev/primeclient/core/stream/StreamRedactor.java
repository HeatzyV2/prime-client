package dev.primeclient.core.stream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Redacts location, network, and seed leaks from chat and debug text. */
public final class StreamRedactor {

    private static final String HIDDEN = "[hidden]";

    private static final Pattern XYZ_LABEL =
            Pattern.compile("(?i)([xyz])\\s*[:=]\\s*-?\\d+(?:\\.\\d+)?");
    private static final Pattern TRIPLET =
            Pattern.compile("(?<!\\w)-?\\d{1,6}(?:\\.\\d+)?\\s+-?\\d{1,3}(?:\\.\\d+)?\\s+-?\\d{1,6}(?:\\.\\d+)?(?!\\w)");
    private static final Pattern BRACKET_COORDS =
            Pattern.compile("\\[\\s*-?\\d+(?:\\.\\d+)?\\s*,\\s*-?\\d+(?:\\.\\d+)?\\s*,\\s*-?\\d+(?:\\.\\d+)?\\s*\\]");
    private static final Pattern TP_COMMAND =
            Pattern.compile("(?i)/tp(?:\\s+\\S+)?\\s+-?\\d+(?:\\.\\d+)?(?:\\s+-?\\d+(?:\\.\\d+)?){0,2}");
    private static final Pattern DIMENSION_COORD =
            Pattern.compile("(?i)(overworld|nether|the\\s+end|end)\\s*[:\\-]\\s*-?\\d+(?:\\.\\d+)?(?:\\s*/\\s*-?\\d+(?:\\.\\d+)?){0,2}");
    private static final Pattern IPV4 =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern PORT =
            Pattern.compile("(?i)(?:port|:\\s*)\\d{2,5}\\b");
    private static final Pattern SEED =
            Pattern.compile("(?i)(?:seed|world\\s+seed)\\s*[:=]\\s*\\S+");
    private static final Pattern WHISPER =
            Pattern.compile("(?i)(?:whispers?|msg|tell)\\s+(?:to\\s+)?\\S+\\s*:\\s*.+");

    private StreamRedactor() {
    }

    public static String redact(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String out = input;
        if (StreamerPrivacyState.redactCoords()) {
            out = XYZ_LABEL.matcher(out).replaceAll("$1: " + HIDDEN);
            out = BRACKET_COORDS.matcher(out).replaceAll("[" + HIDDEN + "]");
            out = TP_COMMAND.matcher(out).replaceAll("/tp " + HIDDEN);
            out = DIMENSION_COORD.matcher(out).replaceAll("$1: " + HIDDEN);
            out = replaceTriplets(out);
        }
        if (StreamerPrivacyState.redactIps()) {
            out = IPV4.matcher(out).replaceAll(HIDDEN);
            out = PORT.matcher(out).replaceAll("port " + HIDDEN);
        }
        if (StreamerPrivacyState.redactWhispers()) {
            out = WHISPER.matcher(out).replaceAll("whisper " + HIDDEN);
        }
        out = SEED.matcher(out).replaceAll("seed: " + HIDDEN);
        return out;
    }

    /** Sanitizes plain text extracted from a chat {@code Component}. */
    public static String redactComponent(String plainText) {
        return redact(plainText);
    }

    private static String replaceTriplets(String input) {
        Matcher matcher = TRIPLET.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(HIDDEN));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
