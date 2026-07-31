package dev.primeclient.core.hud.elements;

import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.notification.Notification;
import dev.primeclient.core.notification.NotificationManager;
import dev.primeclient.core.notification.NotificationPreferences;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.util.ColorUtil;
import dev.primeclient.core.util.Easing;

import java.util.function.Consumer;

/** Premium notification stack with icons and slide-in animation. */
public final class NotificationsElement extends HudElement implements Consumer<Notification> {

    private static final int WIDTH = 168;
    private static final int ROW_HEIGHT = 26;
    private static final int GAP = 4;
    private static final int PADDING = 4;
    private static final int ACCENT_WIDTH = 3;

    private final NotificationManager notifications;
    private final ThemeManager themes;
    private final NotificationPreferences prefs;

    private RenderContext ctx;
    private long now;
    private int cursorY;

    public NotificationsElement(NotificationManager notifications, ThemeManager themes,
                                  NotificationPreferences prefs) {
        super("notifications", "Notifications", HudAnchor.TOP_RIGHT, -4, 4);
        this.notifications = notifications;
        this.themes = themes;
        this.prefs = prefs;
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return WIDTH;
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        int count = notifications.activeCount();
        return count == 0 ? 0 : count * ROW_HEIGHT + (count - 1) * GAP;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        this.ctx = ctx;
        this.now = nowMillis;
        this.cursorY = 0;
        notifications.forEachActive(nowMillis, this);
        this.ctx = null;
    }

    @Override
    public void accept(Notification notification) {
        Theme theme = themes.active();
        float progress = notification.progress(now);
        float slide = (1f - Easing.easeOutCubic(1f - progress)) * 12f * prefs.slideStrength();
        int y = cursorY + Math.round(slide);

        int radius = ROW_HEIGHT / 2;
        int levelCol = levelColor(notification, theme);
        int bgFill = ColorUtil.withAlpha(0xFF0D0D10, 0.94f);

        // Dynamic Island pill shape with soft shadow and glowing accent border
        ctx.fillSoftShadow(0, y, WIDTH, ROW_HEIGHT, radius, 0x80000000);
        ctx.fillRoundedBorder(0, y, WIDTH, ROW_HEIGHT, radius, 1,
                ColorUtil.withAlpha(levelCol, 0.55f), bgFill);

        // Circular badge for level icon
        int badgeSize = 16;
        int badgeX = 5;
        int badgeY = y + (ROW_HEIGHT - badgeSize) / 2;
        ctx.fillRoundedRect(badgeX, badgeY, badgeSize, badgeSize, badgeSize / 2, ColorUtil.withAlpha(levelCol, 0.25f));
        ctx.drawSmoothText(levelIcon(notification), badgeX + 4, badgeY + 1, levelCol, 0.75f);

        int textX = badgeX + badgeSize + 6;
        ctx.drawSmoothText(notification.title(), textX, y + 3, theme.foreground(), 0.75f);
        ctx.drawSmoothText(notification.message(), textX, y + 13, theme.foregroundMuted(), 0.65f);

        // Glowing progress bar at bottom of pill
        int barWidth = Math.max(0, Math.round((WIDTH - 24) * (1f - progress)));
        if (barWidth > 0) {
            ctx.fillRoundedRect(12, y + ROW_HEIGHT - 3, barWidth, 2, 1, ColorUtil.withAlpha(levelCol, 0.85f));
        }
        cursorY += ROW_HEIGHT + GAP;
    }

    private static int levelColor(Notification notification, Theme theme) {
        return switch (notification.level()) {
            case INFO -> theme.accent();
            case SUCCESS -> theme.success();
            case WARNING -> theme.warning();
            case ERROR -> theme.error();
        };
    }

    private static String levelIcon(Notification notification) {
        return switch (notification.level()) {
            case INFO -> "i";
            case SUCCESS -> "✓";
            case WARNING -> "!";
            case ERROR -> "×";
        };
    }
}
