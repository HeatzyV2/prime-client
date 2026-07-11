package dev.primeclient.v1_21_11.render;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** {@link RenderContext} over 1.21.11's immediate-mode {@link GuiGraphics}. */
public final class GuiRenderContext implements RenderContext {

    private GuiGraphics graphics;
    private Font font;
    private int screenWidth;
    private int screenHeight;
    private float drawOpacity = 1f;

    public void prepare(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        this.graphics = graphics;
        this.font = minecraft.font;
        this.screenWidth = minecraft.getWindow().getGuiScaledWidth();
        this.screenHeight = minecraft.getWindow().getGuiScaledHeight();
        this.drawOpacity = 1f;
    }

    @Override
    public int screenWidth() {
        return screenWidth;
    }

    @Override
    public int screenHeight() {
        return screenHeight;
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + height, applyOpacity(argb));
    }

    @Override
    public void drawText(String text, int x, int y, int argb, boolean shadow) {
        graphics.drawString(font, text, x, y, applyOpacity(argb), shadow);
    }

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int fontHeight() {
        return font.lineHeight;
    }

    @Override
    public void pushTransform(float translateX, float translateY, float scale,
                              float rotationDegrees, float pivotLocalX, float pivotLocalY) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(translateX, translateY);
        if (rotationDegrees != 0f) {
            pose.rotate((float) Math.toRadians(rotationDegrees));
        }
        pose.scale(scale, scale);
        if (pivotLocalX != 0f || pivotLocalY != 0f) {
            pose.translate(-pivotLocalX, -pivotLocalY);
        }
    }

    @Override
    public void popTransform() {
        graphics.pose().popMatrix();
    }

    @Override
    public void setDrawOpacity(float opacity) {
        this.drawOpacity = Math.clamp(opacity, 0f, 1f);
    }

    @Override
    public void drawTexture(String texturePath, int x, int y, int width, int height,
                              int textureWidth, int textureHeight, int tintArgb) {
        Identifier id = Identifier.fromNamespaceAndPath(PrimeClient.MOD_ID, texturePath);
        graphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, width, height,
                textureWidth, textureHeight, applyOpacity(tintArgb));
    }

    private int applyOpacity(int argb) {
        return drawOpacity >= 0.999f ? argb : ColorUtil.withAlpha(argb, drawOpacity);
    }
}
