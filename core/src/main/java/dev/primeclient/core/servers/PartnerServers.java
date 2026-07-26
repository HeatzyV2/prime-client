package dev.primeclient.core.servers;

import java.util.List;
import java.util.Locale;

/**
 * Hard-coded partner multiplayer servers shown in the vanilla server list.
 * Partners are injected client-side and cannot be removed by the player.
 */
public final class PartnerServers {

    public record Entry(String name, String address) {
    }

    private static final List<Entry> PARTNERS = List.of(
            new Entry("Elysia SMP", "elysiasmp.fr")
    );

    private PartnerServers() {
    }

    public static List<Entry> partners() {
        return PARTNERS;
    }

    public static boolean isPartnerAddress(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        String normalized = normalize(address);
        for (Entry entry : PARTNERS) {
            if (normalized.equals(normalize(entry.address()))
                    || normalized.startsWith(normalize(entry.address()) + ":")) {
                return true;
            }
        }
        return false;
    }

    /** Display name used when injecting into the vanilla list. */
    public static String displayName(Entry entry) {
        return entry.name() + " ★";
    }

    private static String normalize(String address) {
        String a = address.trim().toLowerCase(Locale.ROOT);
        if (a.startsWith("minecraft://")) {
            a = a.substring("minecraft://".length());
        }
        int slash = a.indexOf('/');
        if (slash >= 0) {
            a = a.substring(0, slash);
        }
        return a;
    }
}
