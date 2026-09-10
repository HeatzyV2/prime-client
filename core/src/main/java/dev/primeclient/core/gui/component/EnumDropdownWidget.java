package dev.primeclient.core.gui.component;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.i18n.PrimeLang;
import dev.primeclient.core.module.EnumSetting;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

/** Expandable enum picker for ClickGUI settings rows. */
public final class EnumDropdownWidget {

    private EnumSetting<?> openSetting;
    private int menuX;
    private int menuY;
    private int menuW;
    private int rowH = PrimeDesign.DROPDOWN_ROW;

    public boolean isOpen() {
        return openSetting != null;
    }

    public EnumSetting<?> openSetting() {
        return openSetting;
    }

    public void close() {
        openSetting = null;
    }

    public void open(EnumSetting<?> setting, int x, int y, int width) {
        this.openSetting = setting;
        this.menuX = x;
        this.menuY = y;
        this.menuW = Math.max(64, width);
    }

    public void renderCompact(RenderContext ctx, Theme theme, int labelX, int textY,
                              String name, String value, int valueX) {
        ctx.drawUiText(value, valueX, textY, theme.accent());
        ctx.drawUiText(name, labelX, textY, theme.foreground());
    }

    public void renderMenu(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        if (openSetting == null) {
            return;
        }
        Enum<?>[] values = openSetting.values();
        int h = values.length * rowH + 4;
        ctx.fillRoundedRect(menuX, menuY, menuW, h, PrimeDesign.RADIUS_SM, theme.surfaceElevated());
        ctx.fillRect(menuX, menuY, 2, h, theme.accent());
        int y = menuY + 2;
        for (Enum<?> value : values) {
            boolean hover = mouseX >= menuX && mouseX < menuX + menuW
                    && mouseY >= y && mouseY < y + rowH;
            boolean selected = value == openSetting.get();
            if (hover || selected) {
                ctx.fillRect(menuX + 2, y, menuW - 2, rowH,
                        ColorUtil.withAlpha(theme.accent(), selected ? 0.35f : 0.18f));
            }
            String label = PrimeLang.enumValue(value);
            ctx.drawUiText(label, menuX + 6, y + (rowH - ctx.uiFontHeight()) / 2,
                    selected ? theme.foreground() : theme.foregroundMuted());
            y += rowH;
        }
    }

    /** @return true if the click was consumed by the open menu */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean mousePressed(double mouseX, double mouseY) {
        if (openSetting == null) {
            return false;
        }
        Enum<?>[] values = openSetting.values();
        int h = values.length * rowH + 4;
        if (mouseX < menuX || mouseX >= menuX + menuW || mouseY < menuY || mouseY >= menuY + h) {
            close();
            return true;
        }
        int index = (int) ((mouseY - menuY - 2) / rowH);
        if (index >= 0 && index < values.length) {
            ((EnumSetting) openSetting).set(values[index]);
        }
        close();
        return true;
    }
}
