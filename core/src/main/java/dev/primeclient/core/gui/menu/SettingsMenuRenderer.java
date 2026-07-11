package dev.primeclient.core.gui.menu;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.cloud.CloudSyncManager;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.design.PrimeLogo;
import dev.primeclient.core.profile.ProfileManager;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;

/** Full settings hub with searchable categories. */
public final class SettingsMenuRenderer {

    public enum Category {
        GENERAL("General"),
        APPEARANCE("Appearance"),
        PERFORMANCE("Performance"),
        CONTROLS("Controls"),
        ACCOUNT("Account"),
        PRIVACY("Privacy"),
        ABOUT("About");

        final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private Category active = Category.GENERAL;
    private final StringBuilder search = new StringBuilder();

    public Category active() {
        return active;
    }

    public void render(RenderContext ctx, Theme theme, ThemeManager themes, ProfileManager profiles,
                       CloudSyncManager cloud, MinecraftAdapter adapter,
                       int screenW, int screenH, double mouseX, double mouseY) {
        int panelW = 320;
        int panelH = 220;
        int x = (screenW - panelW) / 2;
        int y = (screenH - panelH) / 2;
        ctx.fillRect(x, y, panelW, panelH, theme.background());
        ctx.fillRect(x, y, panelW, 2, theme.accent());
        ctx.drawText("Settings", x + 12, y + 10, theme.accent(), true);

        int tabY = y + 28;
        int tabX = x + 8;
        for (Category cat : Category.values()) {
            if (!matchesSearch(cat.label)) {
                continue;
            }
            boolean sel = cat == active;
            int tw = ctx.textWidth(cat.label) + 10;
            ctx.fillRect(tabX, tabY, tw, 14, sel ? theme.surfaceElevated() : theme.backgroundLight());
            ctx.drawText(cat.label, tabX + 5, tabY + 3, sel ? theme.accent() : theme.foregroundMuted(), true);
            tabX += tw + 4;
        }

        int rowY = y + 52;
        switch (active) {
            case GENERAL -> {
                row(ctx, theme, x + 12, rowY, "Profile", profiles.activeProfile()); rowY += 16;
                row(ctx, theme, x + 12, rowY, "Minecraft", adapter.minecraftVersion()); rowY += 16;
                row(ctx, theme, x + 12, rowY, "Cloud sync", cloud.autoSync() ? "Enabled" : "Disabled");
            }
            case APPEARANCE -> {
                row(ctx, theme, x + 12, rowY, "Thème actif", themes.active().name());
                rowY += 18;
                ctx.fillRect(x + 12, rowY, 100, 16, theme.backgroundLight());
                ctx.drawText("Prime Dark", x + 18, rowY + 4, theme.foreground(), true);
                ctx.fillRect(x + 120, rowY, 100, 16, theme.backgroundLight());
                ctx.drawText("Prime Light", x + 126, rowY + 4, theme.foreground(), true);
            }
            case PERFORMANCE -> row(ctx, theme, x + 12, rowY, "Tip", "Use Performance Profiles module");
            case CONTROLS -> row(ctx, theme, x + 12, rowY, "ClickGUI", "Right Shift  •  HUD Editor: H");
            case ACCOUNT -> {
                row(ctx, theme, x + 12, rowY, "Player", adapter.playerName()); rowY += 16;
                row(ctx, theme, x + 12, rowY, "Prime Account", "Connect via Prime Account module");
            }
            case PRIVACY -> row(ctx, theme, x + 12, rowY, "Data", "Configs stored locally only");
            case ABOUT -> {
                PrimeLogo.draw(ctx, x + 12, rowY, 14, 0xFFFFFFFF);
                row(ctx, theme, x + 12 + PrimeLogo.widthForHeight(14) + 6, rowY + 2,
                        "Prime Client", "v" + PrimeDesign.VERSION);
                rowY += 20;
                row(ctx, theme, x + 12, rowY, "Legitimate client", "Visual & QoL only");
            }
        }
        String q = search.isEmpty() ? "Search settings..." : search.toString();
        ctx.drawText(q, x + 12, y + panelH - 18, theme.foregroundMuted(), true);
    }

    public boolean mousePressed(double mx, double my, int screenW, int screenH, ThemeManager themes) {
        int panelW = 320;
        int panelH = 220;
        int x = (screenW - panelW) / 2;
        int y = (screenH - panelH) / 2;
        int tabY = y + 28;
        int tabX = x + 8;
        for (Category cat : Category.values()) {
            if (!matchesSearch(cat.label)) {
                continue;
            }
            int tw = cat.label.length() * 6 + 10;
            if (mx >= tabX && mx < tabX + tw && my >= tabY && my < tabY + 14) {
                active = cat;
                return true;
            }
            tabX += tw + 4;
        }
        if (active == Category.APPEARANCE) {
            int rowY = y + 70;
            if (mx >= x + 12 && mx < x + 112 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-dark");
                return true;
            }
            if (mx >= x + 120 && mx < x + 220 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-light");
                return true;
            }
        }
        return mx >= x && mx < x + panelW && my >= y && my < y + panelH;
    }

    public boolean charTyped(char c) {
        if (c < ' ') {
            return false;
        }
        search.append(c);
        return true;
    }

    public boolean keyPressed(int key) {
        if (key == 259 && !search.isEmpty()) {
            search.setLength(search.length() - 1);
            return true;
        }
        return false;
    }

    private boolean matchesSearch(String label) {
        if (search.isEmpty()) {
            return true;
        }
        return label.toLowerCase().contains(search.toString().toLowerCase());
    }

    private static void row(RenderContext ctx, Theme theme, int x, int y, String k, String v) {
        ctx.drawText(k, x, y, theme.foreground(), true);
        ctx.drawText(v, x + 120, y, theme.foregroundMuted(), true);
    }
}
