package dev.primeclient.core.gui.clickgui;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.gui.FavoritesManager;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.ColorSetting;
import dev.primeclient.core.module.DoubleSetting;
import dev.primeclient.core.module.EnumSetting;
import dev.primeclient.core.module.IntSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.Setting;
import dev.primeclient.core.module.StringSetting;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.Easing;

import java.util.List;

/** One draggable ClickGUI category panel. */
final class Panel {

    static final int WIDTH = 118;
    static final int HEADER_HEIGHT = 16;
    static final int ROW_HEIGHT = 14;
    private static final int PADDING = 5;
    private static final int SETTING_INDENT = 10;

    private final String title;
    private final List<Module> modules;
    private final FavoritesManager favorites;

    float x;
    float y;
    boolean collapsed;
    private Module expanded;

    private boolean draggingHeader;
    private float grabOffsetX;
    private float grabOffsetY;
    private float collapseProgress = 1f;

    Panel(String title, List<Module> modules, FavoritesManager favorites, float x, float y) {
        this.title = title;
        this.modules = modules;
        this.favorites = favorites;
        this.x = x;
        this.y = y;
        this.collapseProgress = collapsed ? 0f : 1f;
    }

    String title() {
        return title;
    }

    boolean hasModule(Module module) {
        return modules.size() == 1 && modules.get(0) == module;
    }

    void tick(float deltaSeconds) {
        float target = collapsed ? 0f : 1f;
        collapseProgress = Easing.lerp(collapseProgress, target, deltaSeconds * 12f);
    }

    int height() {
        return HEADER_HEIGHT + Math.round(rowCount() * ROW_HEIGHT * collapseProgress);
    }

    private int rowCount() {
        int count = modules.size();
        if (expanded != null && modules.contains(expanded)) {
            count += expanded.settings().size();
        }
        return count;
    }

    void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        int px = Math.round(x);
        int py = Math.round(y);

        ctx.fillRect(px, py, WIDTH, HEADER_HEIGHT, theme.backgroundLight());
        ctx.fillRect(px, py + HEADER_HEIGHT - 1, WIDTH, 1, theme.accent());
        ctx.drawText(title, px + PADDING, py + (HEADER_HEIGHT - ctx.fontHeight()) / 2 + 1, theme.foreground(), true);

        if (collapseProgress <= 0.01f) {
            return;
        }

        int visibleRows = Math.round(rowCount() * collapseProgress);
        int rowY = py + HEADER_HEIGHT;
        int drawn = 0;
        for (Module module : modules) {
            if (drawn >= visibleRows) {
                break;
            }
            renderModuleRow(ctx, theme, module, px, rowY, mouseX, mouseY);
            rowY += ROW_HEIGHT;
            drawn++;

            if (module == expanded) {
                for (Setting setting : module.settings()) {
                    if (drawn >= visibleRows) {
                        break;
                    }
                    ctx.fillRect(px, rowY, WIDTH, ROW_HEIGHT, theme.background());
                    ctx.fillRect(px + SETTING_INDENT - 4, rowY, 1, ROW_HEIGHT, theme.accent());
                    renderSetting(ctx, theme, setting, px, rowY);
                    rowY += ROW_HEIGHT;
                    drawn++;
                }
            }
        }
    }

    private void renderModuleRow(RenderContext ctx, Theme theme, Module module, int px, int rowY,
                                 double mouseX, double mouseY) {
        boolean hovered = contains(mouseX, mouseY, px, rowY, WIDTH, ROW_HEIGHT);
        ctx.fillRect(px, rowY, WIDTH, ROW_HEIGHT, hovered ? theme.backgroundLight() : theme.background());
        int nameColor = module.isEnabled() ? theme.accent() : theme.foreground();
        ctx.drawText(module.name(), px + PADDING, rowY + (ROW_HEIGHT - ctx.fontHeight()) / 2 + 1, nameColor, true);

        String star = favorites.isFavorite(module.id()) ? "*" : "+";
        int starX = px + WIDTH - PADDING - ctx.textWidth(star);
        if (!module.settings().isEmpty()) {
            starX -= ctx.textWidth("+") + 4;
            ctx.drawText(module == expanded ? "-" : "+",
                    px + WIDTH - PADDING - ctx.textWidth("+"),
                    rowY + (ROW_HEIGHT - ctx.fontHeight()) / 2 + 1, theme.foregroundMuted(), true);
        }
        ctx.drawText(star, starX, rowY + (ROW_HEIGHT - ctx.fontHeight()) / 2 + 1,
                favorites.isFavorite(module.id()) ? theme.accent() : theme.foregroundMuted(), true);
    }

    private void renderSetting(RenderContext ctx, Theme theme, Setting setting, int px, int rowY) {
        int textY = rowY + (ROW_HEIGHT - ctx.fontHeight()) / 2 + 1;
        int textX = px + SETTING_INDENT;
        switch (setting) {
            case BooleanSetting bool -> {
                ctx.drawText(setting.name(), textX, textY, theme.foreground(), true);
                int boxSize = 8;
                int boxX = px + WIDTH - PADDING - boxSize;
                int boxY = rowY + (ROW_HEIGHT - boxSize) / 2;
                ctx.fillRect(boxX, boxY, boxSize, boxSize, theme.backgroundLight());
                if (bool.get()) {
                    ctx.fillRect(boxX + 2, boxY + 2, boxSize - 4, boxSize - 4, theme.accent());
                }
            }
            case IntSetting number -> renderSlider(ctx, theme, setting.name(), String.valueOf(number.get()),
                    (number.get() - number.min()) / (float) (number.max() - number.min()), px, rowY, textX, textY);
            case DoubleSetting number -> renderSlider(ctx, theme, setting.name(), String.format("%.1f", number.get()),
                    (float) ((number.get() - number.min()) / (number.max() - number.min())), px, rowY, textX, textY);
            case EnumSetting<?> mode -> {
                ctx.drawText(setting.name(), textX, textY, theme.foreground(), true);
                String value = mode.get().name();
                ctx.drawText(value, px + WIDTH - PADDING - ctx.textWidth(value), textY, theme.accent(), true);
            }
            case ColorSetting color -> {
                ctx.drawText(setting.name(), textX, textY, theme.foreground(), true);
                int swatch = 8;
                ctx.fillRect(px + WIDTH - PADDING - swatch, rowY + (ROW_HEIGHT - swatch) / 2, swatch, swatch, color.get());
            }
            case StringSetting text -> {
                ctx.drawText(setting.name(), textX, textY, theme.foreground(), true);
                String value = text.get();
                ctx.drawText(value, px + WIDTH - PADDING - ctx.textWidth(value), textY, theme.foregroundMuted(), true);
            }
        }
    }

    private void renderSlider(RenderContext ctx, Theme theme, String name, String value, float fraction,
                              int px, int rowY, int textX, int textY) {
        ctx.drawText(name, textX, textY, theme.foreground(), true);
        ctx.drawText(value, px + WIDTH - PADDING - ctx.textWidth(value), textY - 4, theme.foregroundMuted(), true);
        int barX = sliderBarX(px);
        int barWidth = sliderBarWidth();
        int barY = rowY + ROW_HEIGHT - 5;
        ctx.fillRect(barX, barY, barWidth, 2, theme.backgroundLight());
        ctx.fillRect(barX, barY, Math.round(barWidth * fraction), 2, theme.accent());
    }

    private static int sliderBarX(int px) {
        return px + SETTING_INDENT;
    }

    private static int sliderBarWidth() {
        return WIDTH - SETTING_INDENT - PADDING;
    }

    Hit mousePressed(double mouseX, double mouseY, int button) {
        int px = Math.round(x);
        int py = Math.round(y);
        if (contains(mouseX, mouseY, px, py, WIDTH, HEADER_HEIGHT)) {
            if (button == 1) {
                collapsed = !collapsed;
            } else {
                draggingHeader = true;
                grabOffsetX = (float) (mouseX - x);
                grabOffsetY = (float) (mouseY - y);
            }
            return Hit.CONSUMED;
        }
        if (collapseProgress <= 0.01f
                || !contains(mouseX, mouseY, px, py + HEADER_HEIGHT, WIDTH, height() - HEADER_HEIGHT)) {
            return Hit.MISS;
        }

        int rowIndex = (int) ((mouseY - (py + HEADER_HEIGHT)) / ROW_HEIGHT);
        int cursor = 0;
        for (Module module : modules) {
            if (cursor == rowIndex) {
                if (button == 2) {
                    favorites.toggle(module.id());
                } else if (button == 0) {
                    module.toggle();
                } else if (!module.settings().isEmpty()) {
                    expanded = expanded == module ? null : module;
                }
                return Hit.CONSUMED;
            }
            cursor++;
            if (module == expanded) {
                for (Setting setting : module.settings()) {
                    if (cursor == rowIndex) {
                        return settingPressed(setting, mouseX, px);
                    }
                    cursor++;
                }
            }
        }
        return Hit.CONSUMED;
    }

    private Hit settingPressed(Setting setting, double mouseX, int px) {
        switch (setting) {
            case BooleanSetting bool -> bool.toggle();
            case EnumSetting<?> mode -> mode.cycle();
            case IntSetting number -> {
                applySlider(number, mouseX, px);
                return new Hit(number);
            }
            case DoubleSetting number -> {
                applySlider(number, mouseX, px);
                return new Hit(number);
            }
            case ColorSetting color -> {
                return new Hit(color);
            }
            case StringSetting text -> {
                return new Hit(text);
            }
        }
        return Hit.CONSUMED;
    }

    void dragSlider(Setting slider, double mouseX) {
        int px = Math.round(x);
        if (slider instanceof IntSetting number) {
            applySlider(number, mouseX, px);
        } else if (slider instanceof DoubleSetting number) {
            applySlider(number, mouseX, px);
        }
    }

    private void applySlider(IntSetting setting, double mouseX, int px) {
        float fraction = sliderFraction(mouseX, px);
        setting.set(Math.round(setting.min() + fraction * (setting.max() - setting.min())));
    }

    private void applySlider(DoubleSetting setting, double mouseX, int px) {
        float fraction = sliderFraction(mouseX, px);
        setting.set(setting.min() + fraction * (setting.max() - setting.min()));
    }

    private static float sliderFraction(double mouseX, int px) {
        return Math.clamp((float) (mouseX - sliderBarX(px)) / sliderBarWidth(), 0f, 1f);
    }

    void mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (draggingHeader) {
            x = Math.clamp((float) mouseX - grabOffsetX, 0, screenWidth - WIDTH);
            y = Math.clamp((float) mouseY - grabOffsetY, 0, Math.max(0, screenHeight - height()));
        }
    }

    void mouseReleased() {
        draggingHeader = false;
    }

    private static boolean contains(double mx, double my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }

    record Hit(Type type, Setting slider, StringSetting stringSetting, ColorSetting colorSetting) {
        enum Type {
            MISS,
            CONSUMED,
            SLIDER,
            STRING_EDIT,
            COLOR_EDIT
        }

        static final Hit MISS = new Hit(Type.MISS, null, null, null);
        static final Hit CONSUMED = new Hit(Type.CONSUMED, null, null, null);

        Hit(Setting slider) {
            this(Type.SLIDER, slider, null, null);
        }

        Hit(StringSetting setting) {
            this(Type.STRING_EDIT, null, setting, null);
        }

        Hit(ColorSetting setting) {
            this(Type.COLOR_EDIT, null, null, setting);
        }

        boolean isMiss() {
            return type == Type.MISS;
        }

        boolean isSlider() {
            return type == Type.SLIDER;
        }

        boolean isStringEdit() {
            return type == Type.STRING_EDIT;
        }

        boolean isColorEdit() {
            return type == Type.COLOR_EDIT;
        }
    }
}
