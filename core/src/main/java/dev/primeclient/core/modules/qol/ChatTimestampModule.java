package dev.primeclient.core.modules.qol;

import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.state.ChatOverlayState;

/** Enables timestamp prefixes on incoming chat via hooks. */
public final class ChatTimestampModule extends Module {

    public ChatTimestampModule() {
        super("chat-timestamp", "Chat Timestamp", "Prefix incoming chat with timestamps", ModuleCategory.QOL);
    }

    @Override
    protected void onEnable() {
        ChatOverlayState.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        ChatOverlayState.setEnabled(false);
    }
}
