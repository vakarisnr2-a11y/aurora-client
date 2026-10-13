package dev.aurora.client.module.modules.combat;

import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.util.CombatTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Plays a sound of your choice when you hit an entity, optionally only for hits the server confirms (the target
 * flashes red). Can mute vanilla's player attack sounds so only yours is heard.
 */
public final class HitSounds extends Module {
    public enum HitSound {
        CLICK, DING, PLING, HAT, SNARE, BASS, BELL, CHIME, XYLOPHONE, COW_BELL, BIT, EXPERIENCE, GLASS, AMETHYST, ANVIL;

        /** Resolved on use: SoundEvents need a bootstrapped game. */
        public SoundEvent event() {
            return switch (this) {
                case CLICK -> SoundEvents.UI_BUTTON_CLICK.value();
                case DING -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
                case PLING -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
                case HAT -> SoundEvents.BLOCK_NOTE_BLOCK_HAT.value();
                case SNARE -> SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value();
                case BASS -> SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value();
                case BELL -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
                case CHIME -> SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value();
                case XYLOPHONE -> SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE.value();
                case COW_BELL -> SoundEvents.BLOCK_NOTE_BLOCK_COW_BELL.value();
                case BIT -> SoundEvents.BLOCK_NOTE_BLOCK_BIT.value();
                case EXPERIENCE -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
                case GLASS -> SoundEvents.BLOCK_GLASS_BREAK;
                case AMETHYST -> SoundEvents.BLOCK_AMETHYST_BLOCK_HIT;
                case ANVIL -> SoundEvents.BLOCK_ANVIL_LAND;
            };
        }
    }

    /** Vanilla's attack sounds (server-sent, for every player's attacks nearby). */
    public static final Set<String> VANILLA_ATTACK_SOUNDS = Set.of("entity.player.attack.strong", "entity.player.attack.weak",
        "entity.player.attack.crit", "entity.player.attack.knockback", "entity.player.attack.sweep", "entity.player.attack.nodamage");

    private final EnumSetting<HitSound> sound = add(new EnumSetting<>("Sound", "Sound played on a hit", HitSound.DING));
    private final NumberSetting volume = add(new NumberSetting("Volume", "Hit sound volume", 0.8, 0.1, 1.0, 0.05));
    private final NumberSetting pitch = add(new NumberSetting("Pitch", "Hit sound pitch", 1.0, 0.5, 2.0, 0.05));
    private final BooleanSetting confirmed = add(new BooleanSetting("Confirmed Only",
        "Only play for hits the server confirms (the target flashes red); off plays on every swing that hits an entity", true));
    private final BooleanSetting muteVanilla = add(new BooleanSetting("Mute Vanilla", "Mute vanilla's attack sounds (yours and other players')", false));

    private long lastAttack;
    private long lastHit;
    private long lastPreviewMs;

    public HitSounds() {
        super("Hit Sounds", "Custom sound when you hit something; can mute vanilla attack sounds.", Category.COMBAT);
        on(TickEvent.Post.class, e -> tick());
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    protected void onEnable() {
        lastAttack = CombatTracker.INSTANCE.getLastAttackMs();
        lastHit = CombatTracker.INSTANCE.getLastHitMs();
    }

    private void tick() {
        CombatTracker tracker = CombatTracker.INSTANCE;
        long attack = tracker.getLastAttackMs();
        long hit = tracker.getLastHitMs();
        boolean play = confirmed.get() ? hit != lastHit : attack != lastAttack;
        lastAttack = attack;
        lastHit = hit;
        if (play) play();
    }

    private void play() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(sound.get().event(), pitch.getFloat(), volume.getFloat()));
    }

    @Override
    protected void onSettingChanged(Setting<?> setting) {
        if (setting != sound && setting != volume && setting != pitch) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || !(mc.currentScreen instanceof ClickGuiScreen)) return;
        long now = System.currentTimeMillis();
        if (now - lastPreviewMs < 250) return;
        lastPreviewMs = now;
        play();
    }

    /** Whether a sound being played should be muted. */
    public boolean mutes(Identifier id) {
        return muteVanilla.get() && isVanillaAttack(id);
    }

    public static boolean isVanillaAttack(Identifier id) {
        return id != null && Identifier.DEFAULT_NAMESPACE.equals(id.getNamespace()) && VANILLA_ATTACK_SOUNDS.contains(id.getPath());
    }

    @Override
    public String getDisplayInfo() {
        return sound.getDisplay();
    }
}
