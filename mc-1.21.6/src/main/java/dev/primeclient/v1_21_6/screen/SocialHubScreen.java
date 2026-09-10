package dev.primeclient.v1_21_6.screen;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.gui.social.SocialHubUi;
import dev.primeclient.v1_21_6.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** In-game social hub shell for 1.21.6. */
public final class SocialHubScreen extends Screen {

    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final SocialHubUi ui = new SocialHubUi(PrimeClient.get().social(), PrimeClient.get().adapter());
    private final Screen parent;

    public SocialHubScreen(Screen parent) {
        super(Component.literal("Social Hub"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ui.onOpen();
        dev.primeclient.core.gui.BlurBackdrop.setActive(true);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level != null) {
            super.renderBackground(graphics, mouseX, mouseY, delta);
        } else {
            renderPanorama(graphics, delta);
        }
        renderContext.prepare(graphics);
        ui.render(renderContext, PrimeClient.get().themes().active(), mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level == null) {
            super.renderBackground(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ui.mousePressed(mouseX, mouseY, button, width, height)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ui.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ui.keyPressed(keyCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (ui.charTyped(codePoint)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        dev.primeclient.v1_21_6.render.PanelBlur.end(minecraft);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
