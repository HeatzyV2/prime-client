package com.primeclient.profile;

import com.primeclient.PrimeClientPlugin;
import com.primeclient.api.PrimeProfile;
import com.primeclient.database.Database;
import com.primeclient.detection.ClientDetectionService;
import com.primeclient.utils.Text;
import com.primeclient.xp.XpService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ProfileService {

    private final PrimeClientPlugin plugin;
    private final Database database;
    private final ClientDetectionService detection;
    private final XpService xp;

    public ProfileService(
            PrimeClientPlugin plugin,
            Database database,
            ClientDetectionService detection,
            XpService xp) {
        this.plugin = plugin;
        this.database = database;
        this.detection = detection;
        this.xp = xp;
    }

    public void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, Text.mm("<gold>⚡ Prime Client</gold>"));
        PrimeProfile profile = database.findProfile(player.getUniqueId()).orElse(null);
        boolean connected = detection.isVerified(player.getUniqueId());
        String version = detection.getVersion(player.getUniqueId()).orElse(
                profile != null ? profile.clientVersion() : "—");
        long playtime = profile != null ? profile.playtimeSeconds() : 0L;
        int xpAmount = profile != null ? profile.xp() : 0;
        int level = profile != null ? profile.level() : Text.levelForXp(xpAmount);

        inv.setItem(11, icon(Material.NETHER_STAR, "<gold>Statut</gold>",
                List.of(connected ? "<green>✔ Connecté</green>" : "<red>✘ Non détecté</red>")));
        inv.setItem(12, icon(Material.BOOK, "<yellow>Version</yellow>",
                List.of("<white>" + (version == null || version.isBlank() ? "—" : version) + "</white>")));
        inv.setItem(13, icon(Material.CLOCK, "<aqua>Temps joué</aqua>",
                List.of("<white>" + xp.formatHours(playtime) + " heures</white>")));
        inv.setItem(14, icon(Material.EXPERIENCE_BOTTLE, "<green>XP Prime</green>",
                List.of("<white>" + xpAmount + "</white>")));
        inv.setItem(15, icon(Material.DIAMOND, "<light_purple>Niveau</light_purple>",
                List.of("<white>" + level + "</white>")));

        player.openInventory(inv);
    }

    public void sendInfo(Player sender, Player target) {
        PrimeProfile profile = database.findProfile(target.getUniqueId()).orElse(null);
        boolean connected = detection.isVerified(target.getUniqueId());
        String version = detection.getVersion(target.getUniqueId()).orElse(
                profile != null ? profile.clientVersion() : "—");
        Text.send(sender, "<gold>⚡ Prime info — " + target.getName() + "</gold>");
        Text.send(sender, "<gray>Statut:</gray> " + (connected ? "<green>Prime</green>" : "<red>Vanilla/autre</red>"));
        Text.send(sender, "<gray>Version:</gray> <white>" + version + "</white>");
        if (profile != null) {
            Text.send(sender, "<gray>XP/Niveau:</gray> <white>" + profile.xp() + " / " + profile.level() + "</white>");
            Text.send(sender, "<gray>Temps:</gray> <white>" + xp.formatHours(profile.playtimeSeconds()) + "h</white>");
        }
    }

    private static ItemStack icon(Material material, String name, List<String> loreMm) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.mm(name));
        meta.lore(loreMm.stream().map(Text::mm).map(c -> (Component) c).toList());
        stack.setItemMeta(meta);
        return stack;
    }
}
