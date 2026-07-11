package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.theme.Theme;

/** Title screen controller — input, fade-in, and navigation via the adapter. */
public final class TitleMenu {

    private final TitleMenuRenderer renderer = new TitleMenuRenderer();
    private final MinecraftAdapter adapter;
    private float fade;

    public TitleMenu(MinecraftAdapter adapter) {
        this.adapter = adapter;
    }

    public void resetFade() {
        fade = 0f;
    }

    public void tick(float deltaSeconds) {
        renderer.tick(deltaSeconds);
        if (fade < 1f) {
            fade = Math.min(1f, fade + deltaSeconds * 1.8f);
        }
    }

    public void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        renderer.render(ctx, theme, mouseX, mouseY,
                adapter.minecraftVersion(), PrimeDesign.VERSION, fade);
    }

    public boolean mousePressed(double mouseX, double mouseY, int button,
                                int screenWidth, int screenHeight) {
        if (button != 0 || fade < 0.35f) {
            return false;
        }
        TitleMenuAction action = renderer.hitAction(mouseX, mouseY, screenWidth, screenHeight);
        if (action == null) {
            return false;
        }
        switch (action) {
            case SINGLEPLAYER -> adapter.openSingleplayer();
            case MULTIPLAYER -> adapter.openMultiplayer();
            case PRIME_CLIENT -> adapter.openClickGui();
            case OPTIONS -> adapter.openOptions();
            case QUIT -> adapter.quitGame();
        }
        return true;
    }
}
