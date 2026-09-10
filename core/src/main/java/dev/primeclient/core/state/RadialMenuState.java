package dev.primeclient.core.state;

/** Open/closed state for the in-game radial profile wheel. */
public final class RadialMenuState {

    private static boolean open;

    private RadialMenuState() {
    }

    public static boolean open() {
        return open;
    }

    public static void setOpen(boolean value) {
        open = value;
    }

    public static void reset() {
        open = false;
    }
}
