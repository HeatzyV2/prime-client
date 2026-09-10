package dev.primeclient.v1_21_8.screen;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.hud.editor.HudEditorState;
import dev.primeclient.v1_21_8.hud.VanillaHudLayerRenderer;
import dev.primeclient.v1_21_8.render.GuiRenderContext;
import dev.primeclient.v1_21_8.render.PanelBlur;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * HUD editor: flat dim backdrop (no world/blur) + lightweight vanilla HUD layers only.
 */
public final class HudEditorScreen extends Screen {

    /** Dim over flat backdrop — vanilla HUD draws on top. */
    private static final int WORLD_DIM = 0x68000000;
    /** Title / no level: same flat backdrop. */
    private static final int MENU_DIM = 0xE0101010;

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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Intentionally empty — super renders the 3D world + blur every frame (~10 FPS on servers).
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        PrimeClient client = PrimeClient.get();
        renderContext.prepare(graphics);
        int dim = minecraft.level != null ? WORLD_DIM : MENU_DIM;
        renderContext.fillRect(0, 0, width, height, dim);

        if (minecraft.level != null && minecraft.player != null) {
            HudEditorState.runVanillaHudRender(
                    () -> VanillaHudLayerRenderer.renderVisibleLayers(graphics, minecraft.getDeltaTracker()));
        }

        // One layout pass (incl. hidden for hit-testing), then draw without re-measuring.
        client.hud().layout(renderContext, true);
        client.hud().render(renderContext, false);
        client.hudEditor().tickAutosave();
        client.hudEditor().renderOverlay(renderContext, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && PrimeClient.get().hudEditor().mousePressed(mouseX, mouseY, hasAltDown(), hasShiftDown())) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            PrimeClient.get().hudEditor().mouseDragged(mouseX, mouseY, width, height, hasShiftDown(), hasAltDown());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        PrimeClient.get().hudEditor().mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (PrimeClient.get().hudEditor().mouseScrolled(
                mouseX, mouseY, verticalAmount, hasShiftDown(), hasControlDown())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (PrimeClient.get().hudEditor().keyPressed(keyCode, hasShiftDown(), hasControlDown())) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        HudEditorState.setActive(false);
        PanelBlur.end(minecraft);
        PrimeClient.get().hudEditor().flushAutosave();
        PrimeClient.get().profiles().saveActive();
        super.onClose();
    }
}
