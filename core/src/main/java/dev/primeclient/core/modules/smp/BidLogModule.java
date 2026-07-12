package dev.primeclient.core.modules.smp;

import dev.primeclient.core.event.ChatMessageEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.module.IntSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.module.StringSetting;
import dev.primeclient.core.theme.ThemeManager;

import java.util.ArrayDeque;
import java.util.Deque;

/** Captures recent auction bid messages from chat. */
public final class BidLogModule extends Module {

    private final StringSetting keywords = addSetting(new StringSetting(
            "keywords", "Keywords", "Comma-separated bid keywords",
            "bid,outbid,auction,ah,offer,highest,snipe"));
    private final IntSetting maxLines = addSetting(new IntSetting(
            "max-lines", "Max lines", "Lines shown in the HUD", 3, 1, 6));

    private final SmpChatLogHud element;
    private final Deque<String> log = new ArrayDeque<>();

    public BidLogModule(HudManager hud, ThemeManager themes) {
        super("bid-log", "Bid Log", "Recent auction and bid chat lines", ModuleCategory.QOL);
        this.element = hud.register(new SmpChatLogHud(
                "bid-log", "Bid Log", themes, HudAnchor.BOTTOM_LEFT, 4, -112, maxLines.get()));
        element.setVisible(false);
        listen(ChatMessageEvent.class, this::onChat);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        element.sync(log, maxLines.get());
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void onChat(ChatMessageEvent event) {
        if (event.outgoing() || !SmpEconomyKeywords.matches(event.text(), keywords.get())) {
            return;
        }
        log.addFirst(truncate(event.text(), 52));
        while (log.size() > maxLines.get()) {
            log.removeLast();
        }
        element.sync(log, maxLines.get());
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
