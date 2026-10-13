package dev.aurora.client.chat;

import java.util.Locale;

/**
 * The current chat search query. While it is not empty, ChatHudMixin skips wrapped lines of messages that do not
 * contain it, so the chat only shows matches (the messages themselves are kept).
 */
public final class ChatSearchState {
    private static String query = "";
    private static String lowerQuery = "";

    private ChatSearchState() {
    }

    public static boolean isFiltering() {
        return !query.isEmpty();
    }

    public static String query() {
        return query;
    }

    /** @return true if the query changed */
    public static boolean setQuery(String value) {
        String q = value == null ? "" : value.trim();
        if (q.equals(query)) return false;
        query = q;
        lowerQuery = q.toLowerCase(Locale.ROOT);
        return true;
    }

    /** Case-insensitive match of a message's plain text. */
    public static boolean matches(String plain) {
        return lowerQuery.isEmpty() || plain.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }
}
