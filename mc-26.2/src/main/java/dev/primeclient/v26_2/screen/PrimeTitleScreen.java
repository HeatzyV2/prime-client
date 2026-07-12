package dev.primeclient.v26_2.screen;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.gui.menu.TitleMenu;
import dev.primeclient.v26_2.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
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
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        extractPanorama(extractor, delta);
        renderContext.prepare(extractor);
        renderContext.fillRect(0, 0, width, height, OVERLAY);
        titleMenu.render(renderContext, PrimeClient.get().themes().active(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (titleMenu.mousePressed(event.x(), event.y(), event.button(), width, height)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
