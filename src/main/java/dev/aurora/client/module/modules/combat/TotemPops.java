package dev.aurora.client.module.modules.combat;

import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Counts how many totems of undying each nearby player has popped.
 *
 * <p>The server announces every pop to nearby clients (the totem particles and sound); ClientPlayNetworkHandlerMixin
 * forwards those to {@link #onTotemPop}. A player's count resets when they die. Chat messages are local only.</p>
 */
public final class TotemPops extends HudModule {
    private static final int ROW = 17;
    private static final int ICON = 16;
    private static final int PAD = 2;

    private final BooleanSetting self = add(new BooleanSetting("Self", "Also count your own pops", true));
    private final NumberSetting timeout = add(new NumberSetting("Timeout", "Seconds a player stays listed after their last pop", 60, 10, 300, 5));
    private final NumberSetting maxRows = add(new NumberSetting("Max Rows", "Most players listed at once", 5, 1, 10, 1));
    private final BooleanSetting chat = add(new BooleanSetting("Chat Message", "Print pops and deaths in your chat (only you see it)", false));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    private final Tracker tracker = new Tracker();
    private final List<Tracker.Entry> rows = new ArrayList<>();
    private ItemStack icon;

    public TotemPops() {
        super("Totem Pops", "Counts the totems each player has popped.", Category.COMBAT, 0.3, 0.02, false);
        on(TickEvent.Post.class, e -> tick());
    }

    @Override
    protected void onDisable() {
        tracker.clear();
    }

    /** Called on the client thread for every totem pop the server shows us. */
    public void onTotemPop(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(entity instanceof PlayerEntity player) || (player == mc.player && !self.get())) return;
        int pops = tracker.pop(player.getUuid(), player.getName().getString(), System.currentTimeMillis());
        if (chat.get()) message(Text.literal(popMessage(player.getName().getString(), pops)).formatted(Formatting.GOLD));
    }

    private void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            tracker.clear();
            return;
        }
        for (Tracker.Entry entry : tracker.entries()) {
            PlayerEntity player = mc.world.getPlayerByUuid(entry.uuid);
            if (player != null && (player.isDead() || player.getHealth() <= 0)) {
                if (chat.get()) message(Text.literal(deathMessage(entry.name, entry.pops)).formatted(Formatting.RED));
                tracker.remove(entry.uuid);
            }
        }
        tracker.expire(System.currentTimeMillis(), Math.round(timeout.get() * 1000));
    }

    private static void message(Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud != null) mc.inGameHud.getChatHud().addMessage(text);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        rows.clear();
        tracker.mostRecent(maxRows.getInt(), rows);
        if (rows.isEmpty() && editor) {
            rows.add(new Tracker.Entry(new UUID(0, 1), "Steve", 3, 0));
            rows.add(new Tracker.Entry(new UUID(0, 2), "Alex", 1, 0));
        }
        if (rows.isEmpty()) {
            setSize(60, ROW + PAD * 2);
            return;
        }
        if (icon == null) icon = new ItemStack(Items.TOTEM_OF_UNDYING);
        int nameW = 0;
        int countW = 0;
        for (Tracker.Entry e : rows) {
            nameW = Math.max(nameW, tr.getWidth(e.name));
            countW = Math.max(countW, tr.getWidth(String.valueOf(e.pops)));
        }
        int width = PAD + ICON + 3 + nameW + 6 + countW + PAD + 1;
        int height = PAD * 2 + rows.size() * ROW - 1;
        if (background.get()) HudStyle.panel(ctx, 0, 0, width, height);
        for (int i = 0; i < rows.size(); i++) {
            Tracker.Entry e = rows.get(i);
            int y = PAD + i * ROW;
            ctx.drawItem(icon, PAD, y);
            ctx.drawText(tr, e.name, PAD + ICON + 3, y + 4, Theme.TEXT, true);
            String count = String.valueOf(e.pops);
            ctx.drawText(tr, count, width - PAD - 1 - tr.getWidth(count), y + 4, Theme.accent(), true);
        }
        setSize(width, height);
    }

    public static String popMessage(String name, int pops) {
        return name + " popped " + pops + (pops == 1 ? " totem" : " totems");
    }

    public static String deathMessage(String name, int pops) {
        return name + " died after popping " + pops + (pops == 1 ? " totem" : " totems");
    }

    /** Pop counts per player, independent of Minecraft so it can be unit-tested. */
    public static final class Tracker {
        public static final class Entry {
            public final UUID uuid;
            public String name;
            public int pops;
            public long lastPopMs;

            Entry(UUID uuid, String name, int pops, long lastPopMs) {
                this.uuid = uuid;
                this.name = name;
                this.pops = pops;
                this.lastPopMs = lastPopMs;
            }
        }

        private final Map<UUID, Entry> entries = new LinkedHashMap<>();

        /** Records a pop and returns the player's new total. */
        public int pop(UUID uuid, String name, long now) {
            Entry e = entries.computeIfAbsent(uuid, id -> new Entry(id, name, 0, now));
            e.name = name;
            e.pops++;
            e.lastPopMs = now;
            return e.pops;
        }

        public int pops(UUID uuid) {
            Entry e = entries.get(uuid);
            return e == null ? 0 : e.pops;
        }

        public void remove(UUID uuid) {
            entries.remove(uuid);
        }

        public void clear() {
            entries.clear();
        }

        /** Drops players whose last pop is older than {@code timeoutMs}. */
        public void expire(long now, long timeoutMs) {
            entries.values().removeIf(e -> now - e.lastPopMs > timeoutMs);
        }

        /** A snapshot, so entries can be removed while iterating it. */
        public List<Entry> entries() {
            return new ArrayList<>(entries.values());
        }

        /** Fills {@code out} with up to {@code max} entries, most recent pop first. */
        public void mostRecent(int max, List<Entry> out) {
            out.addAll(entries.values());
            out.sort(Comparator.comparingLong((Entry e) -> e.lastPopMs).reversed());
            while (out.size() > max) out.remove(out.size() - 1);
        }
    }
}
