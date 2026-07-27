package dev.primeclient.core.modules.prime;

import dev.primeclient.core.account.PrimeAccountService;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.presence.PrimePresenceService;
import dev.primeclient.core.state.ClientBadgeState;

/** Shows a Prime badge beside usernames in the tab list for other Prime Client users. */
public final class ClientBadgeModule extends Module {

    private final PrimePresenceService presence;
    private final PrimeAccountService account;

    public ClientBadgeModule(PrimePresenceService presence, PrimeAccountService account) {
        super("client-badge", "Client Badge",
                "Shows a Prime marker next to players using Prime Client in the tab list",
                ModuleCategory.PRIME);
        this.presence = presence;
        this.account = account;
        listen(ClientTickEvent.class, event -> {
            presence.tick();
            ClientBadgeState.setTier(account.tier());
        });
    }

    @Override
    protected void onEnable() {
        ClientBadgeState.setActive(true);
        ClientBadgeState.setTier(account.tier());
        presence.onModuleEnabled();
    }

    @Override
    protected void onDisable() {
        ClientBadgeState.setActive(false);
        presence.onModuleDisabled();
    }
}
