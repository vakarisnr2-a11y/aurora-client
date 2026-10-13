package dev.aurora.client.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** Private chat state used by Better Chat (stacking) and the chat search / copy features. */
@Mixin(ChatHud.class)
public interface ChatHudAccessor {
    /** Full messages, newest first. */
    @Accessor("messages")
    List<ChatHudLine> aurora$getMessages();

    /** Wrapped lines, newest first; each message's last line has {@code endOfEntry} set. */
    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> aurora$getVisibleMessages();

    /** Rebuilds the wrapped lines from the messages. */
    @Invoker("refresh")
    void aurora$refresh();
}
