package com.primeclient;

import com.primeclient.achievements.AchievementService;
import com.primeclient.api.PrimeClientAPI;
import com.primeclient.api.PrimeClientAPIImpl;
import com.primeclient.commands.PrimeCommand;
import com.primeclient.database.Database;
import com.primeclient.database.DatabaseFactory;
import com.primeclient.detection.ClientDetectionService;
import com.primeclient.friends.FriendsService;
import com.primeclient.listeners.MissionListener;
import com.primeclient.listeners.PlayerConnectionListener;
import com.primeclient.missions.MissionService;
import com.primeclient.network.NetworkService;
import com.primeclient.notifications.NotificationService;
import com.primeclient.placeholders.PrimePlaceholders;
import com.primeclient.profile.ProfileService;
import com.primeclient.rewards.RewardService;
import com.primeclient.xp.XpService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrimeClientPlugin extends JavaPlugin {

    private static PrimeClientPlugin instance;
    private static PrimeClientAPI api;

    private Database database;
    private ClientDetectionService detection;
    private RewardService rewards;
    private XpService xp;
    private ProfileService profiles;
    private AchievementService achievements;
    private MissionService missions;
    private FriendsService friends;
    private NetworkService network;
    private NotificationService notifications;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("achievements.yml", false);
        saveResource("missions.yml", false);

        database = DatabaseFactory.create(this);
        database.init();

        notifications = new NotificationService(this);
        detection = new ClientDetectionService(this, database);
        rewards = new RewardService(this, database, notifications);
        xp = new XpService(this, database, detection);
        achievements = new AchievementService(this, database, xp);
        missions = new MissionService(this, database, xp, notifications);
        profiles = new ProfileService(this, database, detection, xp);
        friends = new FriendsService(this, notifications);
        network = new NetworkService(this);

        api = new PrimeClientAPIImpl(detection, database, xp);

        detection.register();
        xp.startPlaytimeTask();
        network.start();

        var cmd = getCommand("prime");
        if (cmd != null) {
            PrimeCommand handler = new PrimeCommand(this, profiles, rewards, achievements);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MissionListener(missions, detection), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PrimePlaceholders(this, detection, database).register();
            getLogger().info("PlaceholderAPI hooked.");
        }

        getLogger().info("PrimeClient enabled — channel primeclient:main");
    }

    @Override
    public void onDisable() {
        if (xp != null) {
            xp.shutdown();
        }
        if (network != null) {
            network.shutdown();
        }
        if (detection != null) {
            detection.unregister();
        }
        if (database != null) {
            database.close();
        }
        api = null;
        instance = null;
    }

    public void reloadAll() {
        reloadConfig();
        achievements.reload();
        missions.reload();
        getLogger().info("PrimeClient reloaded.");
    }

    public static PrimeClientPlugin get() {
        return instance;
    }

    public static PrimeClientAPI api() {
        return api;
    }

    public Database database() {
        return database;
    }

    public ClientDetectionService detection() {
        return detection;
    }

    public RewardService rewards() {
        return rewards;
    }

    public XpService xp() {
        return xp;
    }

    public ProfileService profiles() {
        return profiles;
    }

    public AchievementService achievements() {
        return achievements;
    }

    public MissionService missions() {
        return missions;
    }

    public FriendsService friends() {
        return friends;
    }

    public NetworkService network() {
        return network;
    }

    public NotificationService notifications() {
        return notifications;
    }
}
