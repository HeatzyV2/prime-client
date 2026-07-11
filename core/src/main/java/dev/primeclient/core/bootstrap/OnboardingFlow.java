package dev.primeclient.core.bootstrap;

import dev.primeclient.core.PrimeClient;
import dev.primeclient.core.gui.menu.OnboardingManager;

/** Applies onboarding choices to themes, profiles and modules. */
public final class OnboardingFlow {

    private OnboardingFlow() {
    }

    public static void applyChoices(PrimeClient client) {
        OnboardingManager onboarding = client.onboarding();
        client.themes().setActive(onboarding.chosenTheme());
        String profile = onboarding.chosenProfile();
        if (!profile.equals(client.profiles().activeProfile())) {
            client.profiles().switchTo(profile);
        }
        FirstRunConfigurator.applyPreset(client.modules(), client.favorites(), profile);
        client.profiles().saveActive();
        client.notifications().success("Configuration appliquée",
                "Profil " + profileLabel(profile) + " • thème " + onboarding.chosenTheme());
    }

    private static String profileLabel(String id) {
        return switch (id) {
            case "pvp" -> "PvP";
            case "survival" -> "Survie";
            default -> "Équilibré";
        };
    }
}
