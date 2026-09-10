package dev.primeclient.core.modules.pvp;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.event.ChatMessageEvent;
import dev.primeclient.core.event.PlayerDeathEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.hud.SimpleLineHud;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.ThemeManager;

import java.util.Locale;

/** Kills without dying this session. */
public final class StreakCounterModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;
    private int streak;

    public StreakCounterModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("streak-counter", "Streak Counter", "Kills without dying this session", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "streak-counter", "Streak Counter", themes, HudAnchor.TOP_RIGHT, -4, 52));
        element.setVisible(false);
        listen(PlayerDeathEvent.class, event -> {
            streak = 0;
            refresh();
        });
        listen(ChatMessageEvent.class, this::onChat);
    }

    @Override
    protected void onEnable() {
        streak = 0;
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void onChat(ChatMessageEvent event) {
        if (event.outgoing()) {
            return;
        }
        String player = adapter.playerName();
        if (player.isEmpty()) {
            return;
        }
        String lower = event.text().toLowerCase(Locale.ROOT);
        String name = player.toLowerCase(Locale.ROOT);
        if (lower.contains("killed by " + name) || lower.contains("slain by " + name)
                || lower.contains(name + " killed")) {
            streak++;
            refresh();
        }
    }

    private void refresh() {
        element.setText("Streak: " + streak);
    }
}
