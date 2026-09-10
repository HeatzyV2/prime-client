package dev.primeclient.v1_21_7.screen;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.gui.clickgui.ClickGui;
import dev.primeclient.v1_21_7.render.GuiRenderContext;
import dev.primeclient.core.gui.BlurBackdrop;
import dev.primeclient.v1_21_7.render.PanelBlur;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * ClickGUI screen for 1.21.7. Thin shell around the core {@link ClickGui}.
 */
public final class ClickGuiScreen extends Screen {

    private static final int DIM_COLOR = 0x60000000;

    private final GuiRenderContext renderContext = new GuiRenderContext();

    public ClickGuiScreen() {
        super(Component.literal("Prime ClickGUI"));
    }

    @Override
    protected void init() {
        PrimeClient.get().clickGui().onOpen();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // Animations advance from render via frame-delta timing.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level != null) {
            // In-world: dim overlay only — MC 1.21.7 allows one blur call per frame.
            BlurBackdrop.setActive(false);
        } else {
            renderPanorama(graphics, delta);
            BlurBackdrop.setActive(false);
        }
        renderContext.prepare(graphics);
        int dim = minecraft.level != null ? DIM_COLOR : 0x28000000;
        renderContext.fillRect(0, 0, width, height, dim);
        PrimeClient.get().clickGui().render(renderContext, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Skip default blurred background — we draw our own backdrop in render().
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (PrimeClient.get().clickGui().mousePressed(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        PrimeClient.get().clickGui().mouseDragged(mouseX, mouseY, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        PrimeClient.get().clickGui().mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (PrimeClient.get().clickGui().mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (PrimeClient.get().clickGui().charTyped(codePoint)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (PrimeClient.get().clickGui().keyPressed(keyCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        PanelBlur.end(minecraft);
        PrimeClient.get().profiles().saveActive();
        super.onClose();
    }
}
