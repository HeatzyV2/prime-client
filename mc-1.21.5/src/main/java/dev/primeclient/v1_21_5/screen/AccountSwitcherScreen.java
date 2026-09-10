package dev.primeclient.v1_21_5.screen;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.gui.account.AccountSwitcherUi;
import dev.primeclient.v1_21_5.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Title-menu account switcher shell for 1.21.5. */
public final class AccountSwitcherScreen extends Screen {

    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final AccountSwitcherUi ui = new AccountSwitcherUi(PrimeClient.get().adapter());
    private final Screen parent;

    public AccountSwitcherScreen(Screen parent) {
        super(Component.literal("Switch Account"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderPanorama(graphics, delta);
        renderContext.prepare(graphics);
        ui.render(renderContext, PrimeClient.get().themes().active(), mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
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
    public boolean charTyped(char codePoint, int modifiers) {
        if (ui.charTyped(codePoint)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ui.keyPressed(keyCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent != null ? parent : new PrimeTitleScreen());
        }
    }
}
