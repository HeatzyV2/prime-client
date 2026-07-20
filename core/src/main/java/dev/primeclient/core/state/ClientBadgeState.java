package dev.primeclient.core.state;

/** Whether the Prime Client tab-list badge module is active. */
public final class ClientBadgeState {

    private static boolean active;

    private ClientBadgeState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
