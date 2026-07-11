package dev.primeclient.v1_21_11.screen;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.hud.editor.HudEditor;
import dev.primeclient.v1_21_11.render.GuiRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * HUD editor screen for 1.21.11. Thin shell: all interaction logic lives in
 * the core {@link HudEditor}; this class only forwards events and draws a dim
 * backdrop plus the overlay. The HUD itself keeps rendering underneath
 * through the regular Fabric HUD layer.
 */
public final class HudEditorScreen extends Screen {

    private static final String HINT =
            "Drag to move  •  Scroll=scale  •  Shift+scroll=rotate  •  Ctrl+scroll=opacity  •  R=tint  •  Esc=close";
    private static final int DIM_COLOR = 0x40000000;

    private final GuiRenderContext renderContext = new GuiRenderContext();

    public HudEditorScreen() {
        super(Component.literal("Prime HUD Editor"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Intentionally no super.render(): no vanilla blur, the game and HUD
        // must stay readable while editing.
        renderContext.prepare(graphics);
        renderContext.fillRect(0, 0, width, height, DIM_COLOR);
        PrimeClient.get().hudEditor().renderOverlay(renderContext, mouseX, mouseY);
        renderContext.drawText(HINT,
                (width - renderContext.textWidth(HINT)) / 2,
                height - 24,
                PrimeClient.get().themes().active().foreground(),
                true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && PrimeClient.get().hudEditor().mousePressed(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0) {
            PrimeClient.get().hudEditor().mouseDragged(event.x(), event.y(), width, height);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        PrimeClient.get().hudEditor().mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (PrimeClient.get().hudEditor().mouseScrolled(
                mouseX, mouseY, verticalAmount,
                Minecraft.getInstance().hasShiftDown(),
                Minecraft.getInstance().hasControlDown())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (PrimeClient.get().hudEditor().keyPressed(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        PrimeClient.get().profiles().saveActive();
        super.onClose();
    }
}
