package dev.aurora.client.module.modules.utility;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Tab list upgrades: player heads on every server (vanilla hides them on servers without encryption), sorting by
 * name or ping, and your own team at the top.
 */
public final class BetterTab extends Module {
    public enum Sort {
        VANILLA, NAME, PING
    }

    private final BooleanSetting heads = add(new BooleanSetting("Player Heads", "Always show player heads (offline-mode servers show default skins)", true));
    private final EnumSetting<Sort> sort = add(new EnumSetting<>("Sort", "Vanilla (team, then name), name only, or lowest ping first", Sort.VANILLA));
    private final BooleanSetting ownTeamFirst = add(new BooleanSetting("Own Team First", "List players of your scoreboard team at the top", true));

    public BetterTab() {
        super("Better Tab", "Tab list with player heads, sorting options and your team at the top.", Category.UTILITY);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    public boolean forceHeads() {
        return heads.get();
    }

    /** Re-orders vanilla's (already limited) tab list entries. */
    public List<PlayerListEntry> order(List<PlayerListEntry> entries) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Team own = mc.player == null ? null : mc.player.getScoreboardTeam();
        String ownTeam = own == null ? null : own.getName();
        return order(entries, e -> e.getProfile().getName(), PlayerListEntry::getLatency,
            e -> ownTeam != null && e.getScoreboardTeam() != null && ownTeam.equals(e.getScoreboardTeam().getName()),
            sort.get(), ownTeamFirst.get());
    }

    /** Pure sorting (stable): optional own-team partition, then by name or ping; unknown (negative) ping last. */
    public static <T> List<T> order(List<T> in, Function<T, String> name, ToIntFunction<T> ping, Predicate<T> ownTeam,
                                    Sort sort, boolean ownFirst) {
        List<T> out = new ArrayList<>(in);
        Comparator<T> cmp = switch (sort) {
            case VANILLA -> (a, b) -> 0;
            case NAME -> Comparator.comparing(t -> name.apply(t).toLowerCase(Locale.ROOT));
            case PING -> Comparator.comparingInt(t -> {
                int p = ping.applyAsInt(t);
                return p < 0 ? Integer.MAX_VALUE : p;
            });
        };
        if (ownFirst) cmp = Comparator.comparing((T t) -> !ownTeam.test(t)).thenComparing(cmp);
        out.sort(cmp);
        return out;
    }
}
