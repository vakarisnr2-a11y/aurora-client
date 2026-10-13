package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.mixin.InGameHudAccessor;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The sidebar scoreboard as a HUD element: move and scale it in the HUD editor and hide the red score numbers.
 *
 * <p>While enabled, vanilla's sidebar is not drawn (InGameHudMixin) and this element draws the same lines in the same
 * order (vanilla's comparator, at most 15 lines, the objective of your team color first).</p>
 */
public final class ScoreboardHud extends HudModule {
    public enum Style {
        VANILLA, THEME
    }

    private static final int MAX_LINES = 15;
    private static final String JOINER = ": ";
    private static final String[] SAMPLE = {"§eAurora", " ", "Kills: §a12", "Beds: §a3", " ", "§7mc.example.net"};

    private final BooleanSetting hideNumbers = add(new BooleanSetting("Hide Numbers", "Hide the red score numbers on the right", true));
    private final EnumSetting<Style> style = add(new EnumSetting<>("Style", "Vanilla look or the HUD Theme look", Style.VANILLA));
    private final BooleanSetting title = add(new BooleanSetting("Title", "Show the scoreboard title", true));
    private final BooleanSetting textShadow = add(new BooleanSetting("Text Shadow", "Drop shadow under the text", false));

    private final List<ScoreboardEntry> entries = new ArrayList<>();
    private final Text[] names = new Text[MAX_LINES];
    private final Text[] scores = new Text[MAX_LINES];
    private final int[] scoreWidths = new int[MAX_LINES];

    public ScoreboardHud() {
        super("Scoreboard", "Move, scale and restyle the sidebar scoreboard and hide its red numbers.", Category.VISUALS, 1.0, 0.5, false);
    }

    /** The objective vanilla would show: the one for your team color, otherwise the sidebar objective. */
    private static ScoreboardObjective objective(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return null;
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = null;
        Team team = scoreboard.getScoreHolderTeam(mc.player.getNameForScoreboard());
        if (team != null) {
            ScoreboardDisplaySlot slot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
            if (slot != null) objective = scoreboard.getObjectiveForSlot(slot);
        }
        return objective != null ? objective : scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        ScoreboardObjective objective = objective(mc);
        Text heading;
        int n;
        if (objective != null) {
            Scoreboard scoreboard = objective.getScoreboard();
            NumberFormat format = hideNumbers.get() ? BlankNumberFormat.INSTANCE : objective.getNumberFormatOr(StyledNumberFormat.RED);
            entries.clear();
            for (ScoreboardEntry e : scoreboard.getScoreboardEntries(objective)) {
                if (!e.hidden()) entries.add(e);
            }
            entries.sort(InGameHudAccessor.aurora$scoreboardOrder());
            n = Math.min(MAX_LINES, entries.size());
            for (int i = 0; i < n; i++) {
                ScoreboardEntry e = entries.get(i);
                names[i] = Team.decorateName(scoreboard.getScoreHolderTeam(e.owner()), e.name());
                scores[i] = e.formatted(format);
                scoreWidths[i] = tr.getWidth(scores[i]);
            }
            heading = objective.getDisplayName();
        } else if (editor) {
            n = SAMPLE.length - 1;
            for (int i = 0; i < n; i++) {
                names[i] = Text.literal(SAMPLE[i + 1]);
                scores[i] = Text.literal(hideNumbers.get() ? "" : String.valueOf(n - i));
                scoreWidths[i] = tr.getWidth(scores[i]);
            }
            heading = Text.literal(SAMPLE[0]);
        } else {
            setSize(60, 40);
            return;
        }
        draw(ctx, tr, heading, n);
    }

    private void draw(DrawContext ctx, TextRenderer tr, Text heading, int n) {
        int joiner = tr.getWidth(JOINER);
        int titleW = tr.getWidth(heading);
        int width = title.get() ? titleW : 0;
        for (int i = 0; i < n; i++) {
            width = Math.max(width, tr.getWidth(names[i]) + (scoreWidths[i] > 0 ? joiner + scoreWidths[i] : 0));
        }
        int line = tr.fontHeight;
        boolean themed = style.get() == Style.THEME;
        int pad = themed ? HudStyle.padding() : 2;
        int titleH = title.get() ? line : 0;
        int boxW = width + pad * 2;
        int boxH = titleH + n * line + 1 + (themed ? pad * 2 - 2 : 0);
        boolean shadow = textShadow.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        int top = themed ? pad - 1 : 0;
        if (themed) {
            HudStyle.background(ctx, 0, 0, boxW, boxH);
        } else {
            // Same colors as vanilla: a darker title row over a lighter body.
            if (title.get()) ctx.fill(0, 0, boxW, titleH, mc.options.getTextBackgroundColor(0.4f));
            ctx.fill(0, titleH, boxW, boxH, mc.options.getTextBackgroundColor(0.3f));
        }
        if (title.get()) {
            ctx.drawText(tr, heading, pad + width / 2 - titleW / 2, top + 1, 0xFFFFFFFF, shadow);
            if (themed) ctx.fill(pad, top + titleH, boxW - pad, top + titleH + 1, 0x30FFFFFF);
        }
        for (int i = 0; i < n; i++) {
            int y = top + titleH + 1 + i * line;
            ctx.drawText(tr, names[i], pad, y, 0xFFFFFFFF, shadow);
            if (scoreWidths[i] > 0) ctx.drawText(tr, scores[i], pad + width - scoreWidths[i], y, 0xFFFFFFFF, shadow);
        }
        setSize(boxW, boxH);
    }
}
