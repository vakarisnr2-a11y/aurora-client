package dev.aurora.client.event.events;

import dev.aurora.client.event.Event;
import net.minecraft.text.Text;

import java.util.UUID;

/** Fired on the client thread for every chat or system message received from the server. */
public final class ChatReceiveEvent extends Event {
    public static final ChatReceiveEvent INSTANCE = new ChatReceiveEvent();

    public Text message;
    /** Sender of a signed player chat message; null for system messages (most server chat, e.g. Hypixel). */
    public UUID sender;
    /** True for action bar messages. */
    public boolean overlay;

    public ChatReceiveEvent set(Text message, UUID sender, boolean overlay) {
        resetCancelled();
        this.message = message;
        this.sender = sender;
        this.overlay = overlay;
        return this;
    }
}
