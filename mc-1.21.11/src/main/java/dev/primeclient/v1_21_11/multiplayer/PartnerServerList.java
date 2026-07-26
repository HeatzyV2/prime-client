package dev.primeclient.v1_21_11.multiplayer;

import dev.primeclient.core.servers.PartnerServers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

/** Injects partner servers into the vanilla multiplayer list. */
public final class PartnerServerList {

    private PartnerServerList() {
    }

    public static void ensurePartners(ServerList list) {
        if (list == null) {
            return;
        }
        for (PartnerServers.Entry partner : PartnerServers.partners()) {
            if (containsAddress(list, partner.address())) {
                continue;
            }
            ServerData data = new ServerData(
                    PartnerServers.displayName(partner),
                    partner.address(),
                    ServerData.Type.OTHER);
            // No save() during load — re-injected every load, avoids recursion.
            list.add(data, false);
        }
    }

    public static void ensureLoaded() {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        ensurePartners(list);
    }

    private static boolean containsAddress(ServerList list, String address) {
        for (int i = 0; i < list.size(); i++) {
            ServerData data = list.get(i);
            if (data != null && PartnerServers.isPartnerAddress(data.ip)
                    && hostKey(data.ip).equals(hostKey(address))) {
                return true;
            }
        }
        return false;
    }

    private static String hostKey(String address) {
        if (address == null) {
            return "";
        }
        String a = address.trim().toLowerCase();
        int colon = a.lastIndexOf(':');
        if (colon > 0 && !a.startsWith("[")) {
            a = a.substring(0, colon);
        }
        return a;
    }
}
