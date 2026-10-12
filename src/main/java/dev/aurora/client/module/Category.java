package dev.aurora.client.module;

public enum Category {
    COMBAT("Combat", "⚔"),
    VISUALS("Visuals", "◉"),
    MOVEMENT("Movement", "➤"),
    PLAYER("Player", "☺"),
    WORLD("World", "☀"),
    UTILITY("Utility", "⚙"),
    MISC("Misc", "✦");

    private final String displayName;
    private final String icon;

    Category(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}
