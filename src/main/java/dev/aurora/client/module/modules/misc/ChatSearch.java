package dev.aurora.client.module.modules.misc;

import dev.aurora.client.chat.ChatSearchState;
import dev.aurora.client.mixin.ChatHudAccessor;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;

/**
 * Adds a search field to the chat screen (bottom right, Ctrl+F). While it holds text, the chat only shows messages
 * containing it; closing the chat clears the search. Nothing is deleted.
 */
public final class ChatSearch extends Module {
    public ChatSearch() {
        super("Chat Search", "Search box in the chat screen (Ctrl+F) that filters chat messages.", Category.MISC);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    protected void onDisable() {
        setQuery("");
    }

    /** Applies a new query and rebuilds the visible chat lines. */
    public static void setQuery(String query) {
        if (!ChatSearchState.setQuery(query)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) return;
        ChatHud hud = mc.inGameHud.getChatHud();
        ((ChatHudAccessor) hud).aurora$refresh();
        hud.resetScroll();
    }
}
