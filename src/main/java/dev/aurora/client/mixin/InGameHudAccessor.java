package dev.aurora.client.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Comparator;

@Mixin(InGameHud.class)
public interface InGameHudAccessor {
    /** Vanilla's sidebar order (highest score first, then name), so the Scoreboard HUD lists lines identically. */
    @Accessor("SCOREBOARD_ENTRY_COMPARATOR")
    static Comparator<ScoreboardEntry> aurora$scoreboardOrder() {
        throw new AssertionError();
    }
}
