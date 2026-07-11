package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.design.PrimeLogo;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.Easing;

/** Boot splash shown while Prime Client initializes. */
public final class LoadingOverlay {

    private float progress;
    private String stage = "Loading Core...";
    private boolean visible = true;
    private float fade = 1f;

    public void setStage(String stage, float progress) {
        this.stage = stage;
        this.progress = Math.clamp(progress, 0f, 1f);
    }

    public void hide() {
        visible = false;
    }

    public boolean visible() {
        return visible || fade > 0.01f;
    }

    public void tick(float deltaSeconds) {
        fade = Easing.lerp(fade, visible ? 1f : 0f, deltaSeconds * 6f);
    }

    public void render(RenderContext ctx, Theme theme) {
        if (fade <= 0.01f) {
            return;
        }
        int alpha = Math.round(220 * fade);
        ctx.fillRect(0, 0, ctx.screenWidth(), ctx.screenHeight(), (alpha << 24));
        ctx.fillGradientVertical(0, 0, ctx.screenWidth(), ctx.screenHeight(),
                theme.gradientTop(), theme.gradientBottom());

        int logoH = 36;
        int cy = ctx.screenHeight() / 2 - 28;
        PrimeLogo.drawCentered(ctx, ctx.screenWidth() / 2, cy, logoH, 0xFFFFFFFF);
        ctx.drawText(stage, (ctx.screenWidth() - ctx.textWidth(stage)) / 2, cy + logoH + 8, theme.foregroundMuted(), true);

        int barW = 180;
        int barX = (ctx.screenWidth() - barW) / 2;
        int barY = cy + logoH + 24;
        ctx.fillRect(barX, barY, barW, 4, theme.backgroundLight());
        ctx.fillRect(barX, barY, Math.round(barW * progress), 4, theme.accent());
        ctx.drawText("v" + PrimeDesign.VERSION, barX, barY + PrimeDesign.SPACE_MD, theme.foregroundMuted(), true);
        ctx.drawText("Premium Minecraft Client", barX, barY + PrimeDesign.SPACE_MD + 12, theme.foregroundMuted(), true);
    }
}
