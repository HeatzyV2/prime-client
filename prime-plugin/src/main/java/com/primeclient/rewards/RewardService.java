package com.primeclient.rewards;

import com.primeclient.PrimeClientPlugin;
import com.primeclient.database.Database;
import com.primeclient.detection.PrimeClientDetectedEvent;
import com.primeclient.notifications.NotificationService;
import com.primeclient.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public final class RewardService implements Listener {

    public static final String FIRST_JOIN = "first_join";

    private final PrimeClientPlugin plugin;
    private final Database database;
    private final NotificationService notifications;

    public RewardService(PrimeClientPlugin plugin, Database database, NotificationService notifications) {
        this.plugin = plugin;
        this.database = database;
        this.notifications = notifications;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onDetected(PrimeClientDetectedEvent event) {
        if (!event.isFirstDetection()) {
            return;
        }
        grantFirstJoin(event.getPlayer());
    }

    public void grantFirstJoin(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rewards.first_join");
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }
        if (!database.claimReward(player.getUniqueId(), FIRST_JOIN)) {
            return;
        }
        String message = section.getString("message", "<gold>⚡</gold> <white>Prime reward unlocked!</white>");
        Text.send(player, message);
        notifications.rewardAvailable(player, "First Prime join reward claimed");

        List<String> commands = section.getStringList("commands");
        for (String raw : commands) {
            String cmd = Text.applyPlayer(raw, player);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        if (plugin.getConfig().getBoolean("notifications.first-join-broadcast", false)) {
            String broadcast = plugin.getConfig().getString(
                    "notifications.broadcast",
                    "<gold>⚡</gold> <yellow>%player%</yellow> <gray>joue avec Prime Client</gray>");
            Bukkit.getServer().sendMessage(Text.mm(Text.applyPlayer(broadcast, player)));
        }
    }

    public boolean hasClaimed(Player player, String rewardId) {
        return database.hasReward(player.getUniqueId(), rewardId);
    }
}
