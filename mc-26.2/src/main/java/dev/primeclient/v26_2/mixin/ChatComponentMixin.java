package dev.primeclient.v26_2.mixin;

import dev.primeclient.core.hook.PrimeHooks;
import dev.primeclient.core.stream.StreamRedactor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Component primeclient$redactSimpleMessage(Component message) {
        return redact(message);
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Component primeclient$redactSignedMessage(Component message) {
        return redact(message);
    }

    private static Component redact(Component message) {
        if (!PrimeHooks.streamChatRedact() || message == null) {
            return message;
        }
        String redacted = StreamRedactor.redactComponent(message.getString());
        if (redacted.equals(message.getString())) {
            return message;
        }
        return Component.literal(redacted);
    }
}
