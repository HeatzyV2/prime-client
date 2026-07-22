package dev.primeclient.core.gui.social;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.gui.GuiLayout;
import dev.primeclient.core.gui.UiChrome;
import dev.primeclient.core.social.SocialClient;
import dev.primeclient.core.social.SocialService;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * In-game social overlay — friends, party invite/accept, join banner.
 * Opened from the pause menu. Same backend as the launcher.
 */
public final class SocialHubUi {

    private static final int PANEL_W = 400;
    private static final int ROW_H = 38;
    private static final int PAD = 16;
    private static final int ACTION_W = 52;

    private final SocialService social;
    private final MinecraftAdapter adapter;
    private String status = "";
    private boolean statusError;
    private long lastRefreshMs;
    private int scroll;

    public SocialHubUi(SocialService social, MinecraftAdapter adapter) {
        this.social = social;
        this.adapter = adapter;
    }

    public void onOpen() {
        scroll = 0;
        status = "";
        statusError = false;
        refresh(true);
    }

    public void refresh(boolean forceConnect) {
        lastRefreshMs = System.currentTimeMillis();
        if (forceConnect || !social.client().connected()) {
            social.ensureConnected();
        }
        if (social.client().connected()) {
            social.client().refreshFriends();
            CompletableFuture.runAsync(() -> social.client().refreshParty());
        }
    }

    public void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        if (System.currentTimeMillis() - lastRefreshMs > 8_000) {
            refresh(false);
        }

        int sw = ctx.screenWidth();
        int sh = ctx.screenHeight();
        int panelH = Math.min(460, sh - 36);
        int x = (sw - PANEL_W) / 2;
        int y = (sh - panelH) / 2;

        ctx.fillRect(0, 0, sw, sh, 0x88000000);
        UiChrome.glassPanel(ctx, theme, x, y, PANEL_W, panelH);

        ctx.drawSmoothText("Social", x + PAD, y + 14, theme.foreground(), 1.15f);
        drawStatusPill(ctx, theme, x, y);

        boolean refreshHover = hit(mouseX, mouseY, x + PANEL_W - PAD - 64, y + 12, 64, 18);
        UiChrome.button(ctx, theme, x + PANEL_W - PAD - 64, y + 12, 64, 18, refreshHover, false);
        ctx.drawSmoothText("Refresh", x + PANEL_W - PAD - 52, y + 16, theme.foreground(), 0.78f);

        int contentTop = y + 42;
        int contentBottom = y + panelH - 28;
        int cursorY = contentTop;

        String joinAddr = resolveJoinAddress();
        if (joinAddr != null && !joinAddr.isBlank()) {
            boolean hover = hit(mouseX, mouseY, x + PAD, cursorY, PANEL_W - PAD * 2, 40);
            UiChrome.card(ctx, theme, x + PAD, cursorY, PANEL_W - PAD * 2, 40, hover);
            ctx.fillRoundedRect(x + PAD + 8, cursorY + 12, 8, 8, 4, theme.accent());
            ctx.drawSmoothText("Party server", x + PAD + 24, cursorY + 8, theme.foregroundMuted(), 0.72f);
            ctx.drawSmoothText(GuiLayout.trimToWidth(ctx, joinAddr, PANEL_W - 120),
                    x + PAD + 24, cursorY + 20, theme.foreground(), 0.9f);
            ctx.drawSmoothText(hover ? "Click to join" : "Join →",
                    x + PANEL_W - PAD - 70, cursorY + 14, theme.accent(), 0.78f);
            cursorY += 48;
        }

        SocialClient.Party party = social.client().party();
        if (party != null) {
            boolean leaveHover = hit(mouseX, mouseY, x + PAD, cursorY, 88, 18);
            UiChrome.button(ctx, theme, x + PAD, cursorY, 88, 18, leaveHover, false);
            ctx.drawSmoothText("Leave party", x + PAD + 10, cursorY + 4, theme.foreground(), 0.78f);
            ctx.drawSmoothText(party.members().size() + " members",
                    x + PAD + 100, cursorY + 4, theme.foregroundMuted(), 0.78f);
            cursorY += 26;
        }

        ctx.drawSmoothText("FRIENDS", x + PAD, cursorY, theme.foregroundMuted(), 0.72f);
        cursorY += 14;

        Map<String, SocialClient.Friend> friendsMap = social.client().friends();
        List<SocialClient.Friend> friends = new ArrayList<>(friendsMap.values());
        friends.sort(Comparator
                .comparingInt((SocialClient.Friend f) -> statusRank(f.status()))
                .thenComparing(f -> f.username(), String.CASE_INSENSITIVE_ORDER));

        int listTop = cursorY;
        int listH = contentBottom - listTop;
        ctx.pushClip(x + PAD - 2, listTop, PANEL_W - PAD * 2 + 4, listH);

        if (friends.isEmpty()) {
            UiChrome.cardLite(ctx, theme, x + PAD, listTop + 4, PANEL_W - PAD * 2, 52, false);
            String emptyTitle = social.client().connected() ? "No friends yet" : "Not connected";
            String emptySub = social.client().connected()
                    ? "Add friends from the Prime Launcher"
                    : (social.client().statusMessage().isBlank()
                    ? "Tap Refresh to connect"
                    : social.client().statusMessage());
            ctx.drawSmoothText(emptyTitle, x + PAD + 14, listTop + 16, theme.foreground(), 0.92f);
            ctx.drawSmoothText(GuiLayout.trimToWidth(ctx, emptySub, PANEL_W - 56),
                    x + PAD + 14, listTop + 32, theme.foregroundMuted(), 0.78f);
        } else {
            int i = 0;
            for (SocialClient.Friend f : friends) {
                int rowY = listTop + (i - scroll) * (ROW_H + 4);
                i++;
                if (rowY + ROW_H < listTop || rowY > listTop + listH) {
                    continue;
                }
                boolean hover = hit(mouseX, mouseY, x + PAD, rowY, PANEL_W - PAD * 2, ROW_H);
                UiChrome.cardLite(ctx, theme, x + PAD, rowY, PANEL_W - PAD * 2, ROW_H, hover);

                int dot = statusColor(theme, f.status());
                ctx.fillRoundedRect(x + PAD + 10, rowY + 15, 8, 8, 4, dot);

                String name = GuiLayout.trimToWidth(ctx, f.username(), 120);
                ctx.drawSmoothText(name, x + PAD + 26, rowY + 8, theme.foreground(), 0.88f);

                String meta = formatMeta(f);
                ctx.drawSmoothText(GuiLayout.trimToWidth(ctx, meta, PANEL_W - 160),
                        x + PAD + 26, rowY + 21, theme.foregroundMuted(), 0.72f);

                int btnX = x + PANEL_W - PAD - 10 - ACTION_W;
                if (f.pending() && f.incoming()) {
                    drawAction(ctx, theme, btnX, rowY + 10, ACTION_W, "Accept", mouseX, mouseY);
                } else if (!f.pending()) {
                    drawAction(ctx, theme, btnX, rowY + 10, ACTION_W, "Invite", mouseX, mouseY);
                }
            }
        }
        ctx.popClip();

        String footer = !status.isBlank()
                ? status
                : "Esc close  ·  Invite / Accept  ·  Same friends as the launcher";
        ctx.drawSmoothText(footer, x + PAD, y + panelH - 18,
                statusError ? theme.accent() : theme.foregroundMuted(), 0.72f);
    }

    public boolean mousePressed(double mouseX, double mouseY, int button, int sw, int sh) {
        if (button != 0) {
            return true;
        }
        int panelH = Math.min(460, sh - 36);
        int x = (sw - PANEL_W) / 2;
        int y = (sh - panelH) / 2;

        if (hit(mouseX, mouseY, x + PANEL_W - PAD - 64, y + 12, 64, 18)) {
            refresh(true);
            status = "Refreshing…";
            statusError = false;
            return true;
        }

        int cursorY = y + 42;
        String joinAddr = resolveJoinAddress();
        if (joinAddr != null && !joinAddr.isBlank()) {
            if (hit(mouseX, mouseY, x + PAD, cursorY, PANEL_W - PAD * 2, 40)) {
                adapter.joinMultiplayerServer(joinAddr);
                social.consumePendingPartyServer();
                status = "Joining " + joinAddr;
                statusError = false;
                return true;
            }
            cursorY += 48;
        }

        SocialClient.Party party = social.client().party();
        if (party != null) {
            if (hit(mouseX, mouseY, x + PAD, cursorY, 88, 18)) {
                if (social.client().leaveParty()) {
                    status = "Left party";
                    statusError = false;
                    refresh(false);
                } else {
                    status = "Leave failed";
                    statusError = true;
                }
                return true;
            }
            cursorY += 26;
        }

        cursorY += 14; // FRIENDS label
        int listTop = cursorY;
        List<SocialClient.Friend> friends = sortedFriends();
        int i = 0;
        for (SocialClient.Friend f : friends) {
            int rowY = listTop + (i - scroll) * (ROW_H + 4);
            i++;
            int btnX = x + PANEL_W - PAD - 10 - ACTION_W;
            if (!hit(mouseX, mouseY, btnX, rowY + 10, ACTION_W, 16)) {
                continue;
            }
            if (f.pending() && f.incoming()) {
                boolean ok = social.client().acceptFriend(f.uuid());
                status = ok ? "Accepted " + f.username() : "Accept failed";
                statusError = !ok;
                if (ok) {
                    social.client().refreshFriends();
                }
                return true;
            }
            if (!f.pending()) {
                boolean ok = social.client().inviteParty(f.uuid());
                status = ok ? "Invited " + f.username() : "Invite failed (create a party in the launcher)";
                statusError = !ok;
                return true;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int max = Math.max(0, social.client().friends().size() - 8);
        scroll = GuiLayout.clamp(scroll - (int) Math.signum(amount), 0, max);
        return true;
    }

    public boolean keyPressed(int key) {
        if (key == 256) { // Esc
            adapter.closeCurrentScreen();
            return true;
        }
        if (key == 294) { // F5
            refresh(true);
            return true;
        }
        return true;
    }

    private List<SocialClient.Friend> sortedFriends() {
        List<SocialClient.Friend> friends = new ArrayList<>(social.client().friends().values());
        friends.sort(Comparator
                .comparingInt((SocialClient.Friend f) -> statusRank(f.status()))
                .thenComparing(f -> f.username(), String.CASE_INSENSITIVE_ORDER));
        return friends;
    }

    private void drawStatusPill(RenderContext ctx, Theme theme, int panelX, int panelY) {
        SocialClient.ConnState state = social.client().state();
        String label = switch (state) {
            case CONNECTED -> "Online";
            case CONNECTING -> "Connecting";
            case ERROR -> "Error";
            case DISCONNECTED -> "Offline";
        };
        int color = switch (state) {
            case CONNECTED -> 0xFF22C55E;
            case CONNECTING -> 0xFFEAB308;
            case ERROR -> 0xFFEF4444;
            case DISCONNECTED -> theme.foregroundMuted();
        };
        int pillX = panelX + PAD + ctx.smoothTextWidth("Social", 1.15f) + 10;
        int pillW = ctx.smoothTextWidth(label, 0.72f) + 18;
        ctx.fillRoundedRect(pillX, panelY + 14, pillW, 14, PrimeDesign.RADIUS_SM,
                ColorUtil.withAlpha(color, 0.18f));
        ctx.fillRoundedRect(pillX + 5, panelY + 18, 6, 6, 3, color);
        ctx.drawSmoothText(label, pillX + 14, panelY + 17, color, 0.72f);
    }

    private static void drawAction(RenderContext ctx, Theme theme, int x, int y, int w,
                                   String label, double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, w, 16);
        ctx.fillRoundedRect(x, y, w, 16, PrimeDesign.RADIUS_SM,
                hover ? theme.accent() : theme.backgroundLight());
        int tw = ctx.smoothTextWidth(label, 0.7f);
        ctx.drawSmoothText(label, x + (w - tw) / 2, y + 4,
                hover ? theme.foreground() : theme.foregroundMuted(), 0.7f);
    }

    private String resolveJoinAddress() {
        SocialClient.Party party = social.client().party();
        if (party != null && party.serverAddress() != null && !party.serverAddress().isBlank()) {
            return party.serverAddress();
        }
        return social.pendingPartyServer();
    }

    private static int statusRank(String status) {
        return switch (normalize(status)) {
            case "in-game", "ingame" -> 0;
            case "online" -> 1;
            case "away" -> 2;
            default -> 3;
        };
    }

    private static int statusColor(Theme theme, String status) {
        return switch (normalize(status)) {
            case "in-game", "ingame" -> theme.accent();
            case "online" -> 0xFF22C55E;
            case "away" -> 0xFFEAB308;
            default -> 0xFF6B7280;
        };
    }

    private static String formatMeta(SocialClient.Friend f) {
        if (f.pending()) {
            return f.incoming() ? "Incoming request" : "Pending request";
        }
        if (f.serverAddress() != null && !f.serverAddress().isBlank()) {
            return f.serverAddress();
        }
        if (f.activity() != null && !f.activity().isBlank()) {
            return f.activity();
        }
        return switch (normalize(f.status())) {
            case "in-game", "ingame" -> "In game";
            case "online" -> "Online";
            case "away" -> "Away";
            default -> "Offline";
        };
    }

    private static String normalize(String status) {
        return status == null ? "" : status.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
