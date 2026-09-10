package dev.primeclient.v1_21_5.screen;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.gui.menu.TitleMenu;
import dev.primeclient.v1_21_5.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Prime Client title screen — replaces vanilla {@code TitleScreen}. */
public final class PrimeTitleScreen extends Screen {

    private static final int OVERLAY = 0x28000000;

    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final TitleMenu titleMenu = new TitleMenu(PrimeClient.get().adapter());

    public PrimeTitleScreen() {
        super(Component.literal("Prime Client"));
    }

    @Override
    protected void init() {
        titleMenu.resetFade();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        titleMenu.tick(1f / 20f);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Skip default menu backdrop — panorama is drawn in render() like AccountSwitcher.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderPanorama(graphics, delta);
        renderContext.prepare(graphics);
        renderContext.fillRect(0, 0, width, height, OVERLAY);
        titleMenu.render(renderContext, PrimeClient.get().themes().active(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (titleMenu.mousePressed(mouseX, mouseY, button, width, height)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
