package dev.primeclient.core.gui.account;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.account.LauncherAccountStore;
import dev.primeclient.core.account.LauncherAccountStore.AccountEntry;
import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.design.PrimeDesign;
import dev.primeclient.core.i18n.PrimeLang;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** In-game account picker — title menu only (no world loaded). */
public final class AccountSwitcherUi {

    private static final int PANEL_W = 320;
    private static final int ROW_H = 28;
    private static final int PAD = 14;

    private final MinecraftAdapter adapter;
    private final List<AccountEntry> accounts = new ArrayList<>();
    private String draftName = "";
    private String status = "";
    private boolean statusError;
    private boolean busy;
    private final AtomicBoolean switching = new AtomicBoolean(false);
    private int scroll;

    public AccountSwitcherUi(MinecraftAdapter adapter) {
        this.adapter = adapter;
        reload();
    }

    public void reload() {
        accounts.clear();
        accounts.addAll(LauncherAccountStore.list());
        status = "";
        statusError = false;
    }

    public void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        int sw = ctx.screenWidth();
        int sh = ctx.screenHeight();
        int panelH = Math.min(420, sh - 40);
        int x = (sw - PANEL_W) / 2;
        int y = (sh - panelH) / 2;

        ctx.fillRect(0, 0, sw, sh, 0x88000000);
        ctx.fillRoundedRect(x, y, PANEL_W, panelH, PrimeDesign.RADIUS_MD, theme.surfaceElevated());
        ctx.fillRoundedBorder(x, y, PANEL_W, panelH, PrimeDesign.RADIUS_MD, 1,
                ColorUtil.withAlpha(theme.accent(), 0.35f), theme.surfaceElevated());

        String title = PrimeLang.get("prime.gui.account.title", "Switch Account");
        ctx.drawSmoothText(title, x + PAD, y + PAD, theme.foreground(), 1.05f);

        String current = adapter.playerName();
        if (current != null && !current.isBlank()) {
            ctx.drawSmoothText(
                    PrimeLang.get("prime.gui.account.current", "Current") + ": " + current,
                    x + PAD, y + PAD + 18, theme.foregroundMuted(), 0.82f);
        }

        int listTop = y + 52;
        int listBottom = y + panelH - 88;
        int listH = listBottom - listTop;
        ctx.fillRoundedRect(x + PAD, listTop, PANEL_W - PAD * 2, listH, PrimeDesign.RADIUS_SM,
                ColorUtil.withAlpha(theme.background(), 0.55f));

        Optional<String> activeId = LauncherAccountStore.activeAccountId();
        int visible = Math.max(1, listH / ROW_H);
        int maxScroll = Math.max(0, accounts.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (accounts.isEmpty()) {
            ctx.drawSmoothText(
                    PrimeLang.get("prime.gui.account.empty", "No launcher accounts found."),
                    x + PAD + 8, listTop + 10, theme.foregroundMuted(), 0.85f);
        } else {
            for (int i = scroll; i < accounts.size(); i++) {
                int rowY = listTop + (i - scroll) * ROW_H;
                if (rowY + ROW_H > listBottom) {
                    break;
                }
                AccountEntry a = accounts.get(i);
                boolean hover = mouseX >= x + PAD && mouseX < x + PANEL_W - PAD
                        && mouseY >= rowY && mouseY < rowY + ROW_H;
                boolean active = activeId.map(id -> id.equals(a.id())).orElse(false)
                        || a.username().equalsIgnoreCase(current);
                if (hover || active) {
                    ctx.fillRoundedRect(x + PAD + 2, rowY + 2, PANEL_W - PAD * 2 - 4, ROW_H - 4,
                            PrimeDesign.RADIUS_SM,
                            ColorUtil.withAlpha(active ? theme.accent() : theme.backgroundLight(),
                                    active ? 0.35f : 0.45f));
                }
                String badge = a.microsoft() ? "MS" : "OFF";
                String label = a.username() + "  ·  " + badge;
                ctx.drawSmoothText(label, x + PAD + 10, rowY + (ROW_H - ctx.fontHeight()) / 2,
                        theme.foreground(), 0.88f);
            }
        }

        int fieldY = y + panelH - 72;
        ctx.fillRoundedRect(x + PAD, fieldY, PANEL_W - PAD * 2 - 78, 26, PrimeDesign.RADIUS_SM,
                ColorUtil.withAlpha(theme.background(), 0.65f));
        String placeholder = PrimeLang.get("prime.gui.account.offline_hint", "Offline name…");
        String fieldText = draftName.isEmpty() ? placeholder : draftName + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        ctx.drawSmoothText(fieldText, x + PAD + 8, fieldY + 6,
                draftName.isEmpty() ? theme.foregroundMuted() : theme.foreground(), 0.85f);

        drawButton(ctx, theme, x + PANEL_W - PAD - 70, fieldY, 70, 26,
                PrimeLang.get("prime.gui.account.add", "Add"), mouseX, mouseY, !busy);

        if (!status.isBlank()) {
            ctx.drawSmoothText(status, x + PAD, y + panelH - 28,
                    statusError ? 0xFFFF6B6B : theme.foregroundMuted(), 0.78f);
        } else if (busy) {
            ctx.drawSmoothText(
                    PrimeLang.get("prime.gui.account.switching", "Switching…"),
                    x + PAD, y + panelH - 28, theme.accent(), 0.78f);
        } else {
            ctx.drawSmoothText(
                    PrimeLang.get("prime.gui.account.hint", "Click an account · Esc to close"),
                    x + PAD, y + panelH - 28, theme.foregroundMuted(), 0.78f);
        }
    }

    public boolean mousePressed(double mouseX, double mouseY, int button, int screenW, int screenH) {
        if (button != 0 || busy) {
            return true;
        }
        int panelH = Math.min(420, screenH - 40);
        int x = (screenW - PANEL_W) / 2;
        int y = (screenH - panelH) / 2;
        int listTop = y + 52;
        int listBottom = y + panelH - 88;
        int fieldY = y + panelH - 72;

        if (hit(mouseX, mouseY, x + PANEL_W - PAD - 70, fieldY, 70, 26)) {
            addOffline();
            return true;
        }

        if (mouseX >= x + PAD && mouseX < x + PANEL_W - PAD
                && mouseY >= listTop && mouseY < listBottom) {
            int index = scroll + (int) ((mouseY - listTop) / ROW_H);
            if (index >= 0 && index < accounts.size()) {
                switchTo(accounts.get(index));
            }
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            scroll = Math.max(0, scroll - 1);
        } else if (delta < 0) {
            scroll += 1;
        }
        return true;
    }

    public boolean charTyped(char c) {
        if (busy) {
            return true;
        }
        if (c >= 32 && c < 127 && draftName.length() < 16) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                draftName += c;
            }
        }
        return true;
    }

    public boolean keyPressed(int key) {
        if (key == 256) { // Escape
            adapter.closeAccountSwitcher();
            return true;
        }
        if (busy) {
            return true;
        }
        if (key == 259) { // Backspace
            if (!draftName.isEmpty()) {
                draftName = draftName.substring(0, draftName.length() - 1);
            }
            return true;
        }
        if (key == 257 || key == 335) { // Enter
            addOffline();
            return true;
        }
        return true;
    }

    private void addOffline() {
        if (busy || draftName.isBlank()) {
            return;
        }
        Optional<AccountEntry> created = LauncherAccountStore.addOffline(draftName);
        if (created.isEmpty()) {
            status = PrimeLang.get("prime.gui.account.invalid", "Invalid or duplicate name.");
            statusError = true;
            return;
        }
        draftName = "";
        reload();
        switchTo(created.get());
    }

    private void switchTo(AccountEntry account) {
        if (!switching.compareAndSet(false, true)) {
            return;
        }
        busy = true;
        status = "";
        statusError = false;
        Thread worker = new Thread(() -> {
            try {
                var payload = LauncherAccountStore.resolveForSwitch(account);
                adapter.runOnClientThread(() -> {
                    boolean ok = adapter.applyMinecraftSession(
                            payload.username(),
                            payload.uuid(),
                            payload.accessToken(),
                            payload.microsoft());
                    if (ok) {
                        LauncherAccountStore.setActive(account.id());
                        reload();
                        status = PrimeLang.get("prime.gui.account.switched", "Switched to")
                                + " " + payload.username();
                        statusError = false;
                        PrimeClient.get().notifications().success("Account", payload.username());
                        adapter.closeAccountSwitcher();
                    } else {
                        status = PrimeLang.get("prime.gui.account.failed", "Could not apply session.");
                        statusError = true;
                    }
                    busy = false;
                    switching.set(false);
                });
            } catch (Exception e) {
                adapter.runOnClientThread(() -> {
                    status = e.getMessage() != null ? e.getMessage() : "Switch failed";
                    statusError = true;
                    busy = false;
                    switching.set(false);
                });
            }
        }, "prime-account-switch");
        worker.setDaemon(true);
        worker.start();
    }

    private static void drawButton(RenderContext ctx, Theme theme, int x, int y, int w, int h,
                                   String label, double mx, double my, boolean enabled) {
        boolean hover = enabled && hit(mx, my, x, y, w, h);
        int fill = hover ? theme.accent() : ColorUtil.withAlpha(theme.backgroundLight(), 0.7f);
        ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_SM, fill);
        int tw = ctx.smoothTextWidth(label, 0.85f);
        ctx.drawSmoothText(label, x + (w - tw) / 2, y + (h - ctx.fontHeight()) / 2 + 1,
                hover ? 0xFFFFFFFF : theme.foreground(), 0.85f);
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
