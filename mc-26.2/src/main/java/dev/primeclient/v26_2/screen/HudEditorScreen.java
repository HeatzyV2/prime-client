package dev.primeclient.v26_2.screen;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.hud.editor.HudEditorHints;
import dev.primeclient.core.hud.editor.HudEditorState;
import dev.primeclient.v26_2.render.GuiRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * HUD editor screen for 26.2. Defers in-game HUD to this screen so the world
 * can blur while Prime + vanilla HUD elements stay sharp.
 */
public final class HudEditorScreen extends Screen {

    private static final int DIM_COLOR = 0x28000000;

    private final GuiRenderContext renderContext = new GuiRenderContext();

    public HudEditorScreen() {
        super(Component.literal("Prime HUD Editor"));
    }

    @Override
    protected void init() {
        HudEditorState.setActive(true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        if (minecraft.level != null) {
            extractBlurredBackground(extractor);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        PrimeClient client = PrimeClient.get();
        Minecraft minecraft = Minecraft.getInstance();
        renderContext.prepare(extractor);
        client.hud().layout(renderContext);

        if (minecraft.level != null) {
            HudEditorState.runVanillaHudRender(
                    () -> minecraft.gui.hud.extractRenderState(extractor, minecraft.getDeltaTracker()));
            client.hud().layout(renderContext);
        }

        renderContext.fillRect(0, 0, width, height, DIM_COLOR);
        client.hud().render(renderContext);
        client.hudEditor().renderOverlay(renderContext, mouseX, mouseY);

        int color = client.themes().active().foreground();
        int muted = client.themes().active().foregroundMuted();
        drawCenteredHint(HudEditorHints.LINE_1, height - 26, color);
        drawCenteredHint(HudEditorHints.LINE_2, height - 14, muted);
    }

    private void drawCenteredHint(String text, int y, int color) {
        renderContext.drawText(text, (width - renderContext.textWidth(text)) / 2, y, color, true);
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
                mouseX, mouseY, verticalAmount, isShiftDown(), isControlDown())) {
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
        HudEditorState.setActive(false);
        PrimeClient.get().profiles().saveActive();
        super.onClose();
    }

    private static boolean isShiftDown() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean isControlDown() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }
}
