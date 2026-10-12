package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Animated list of enabled modules. Aligns to the right when placed on the right half of the screen. */
public final class ModuleList extends HudModule {
    public enum Sorting {
        LENGTH, ALPHABETICAL
    }

    public enum ColorMode {
        ACCENT, RAINBOW, GRADIENT, STATIC
    }

    private final EnumSetting<Sorting> sorting = add(new EnumSetting<>("Sorting", "Order of entries", Sorting.LENGTH));
    private final EnumSetting<ColorMode> colorMode = add(new EnumSetting<>("Color Mode", "How entries are colored", ColorMode.GRADIENT));
    private final ColorSetting staticColor = add(new ColorSetting("Color", "Color for the Static mode", 0xFFFFFFFF, false))
        .visibleWhen(() -> colorMode.get() == ColorMode.STATIC);
    private final BooleanSetting background = add(new BooleanSetting("Background", "Translucent background behind entries", true));
    private final BooleanSetting sidebar = add(new BooleanSetting("Sidebar", "Accent bar along the screen edge", true));
    private final BooleanSetting suffix = add(new BooleanSetting("Suffix", "Show module info such as modes", true));
    private final BooleanSetting lowercase = add(new BooleanSetting("Lowercase", "Display names in lowercase", false));

    private final Map<Module, Animation> animations = new IdentityHashMap<>();
    private final List<Module> visible = new ArrayList<>();
    private final Map<Module, String> labels = new IdentityHashMap<>();

    public ModuleList() {
        super("Module List", "Shows enabled modules with slide animations.", Category.VISUALS, 1.0, 0.0, true);
    }

    private String label(Module m) {
        String name = lowercase.get() ? m.getName().toLowerCase(Locale.ROOT) : m.getName();
        String info = suffix.get() ? m.getDisplayInfo() : null;
        return info == null ? name : name + " §7" + info;
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        visible.clear();
        for (Module m : AuroraClient.modules().getAll()) {
            Animation a = animations.computeIfAbsent(m, k -> new Animation(0, 12));
            a.setTarget(m.isEnabled() && m.isDrawn() && m != this ? 1 : 0);
            if (a.update() > 0.01) {
                visible.add(m);
                labels.put(m, label(m));
            }
        }
        if (visible.isEmpty()) {
            if (editor) {
                ctx.drawText(tr, "Module List", 0, 0, Theme.accent(), true);
                setSize(tr.getWidth("Module List"), tr.fontHeight);
            } else {
                setSize(60, 10);
            }
            return;
        }
        if (sorting.get() == Sorting.LENGTH) {
            visible.sort(Comparator.comparingInt((Module m) -> -tr.getWidth(labels.get(m))));
        } else {
            visible.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        }

        int maxWidth = 0;
        for (Module m : visible) maxWidth = Math.max(maxWidth, tr.getWidth(labels.get(m)));
        boolean right = isRightSide();
        int lineHeight = tr.fontHeight + 3;
        int fullWidth = maxWidth + 6;
        float y = 0;
        for (int i = 0; i < visible.size(); i++) {
            Module m = visible.get(i);
            float a = animations.get(m).getFloat();
            String text = labels.get(m);
            int w = tr.getWidth(text);
            int color = color(i, visible.size());
            float slide = (1 - a) * (w + 8);
            float x = right ? fullWidth - w - 4 + slide : 2 - slide;
            if (background.get()) {
                float bx = right ? x - 2 : 0;
                float bw = right ? fullWidth - bx : x + w + 2;
                Render2D.rect(ctx, bx, y, bw, lineHeight * a, ColorUtil.fade(Theme.hudBackground(), a));
            }
            if (sidebar.get()) {
                Render2D.rect(ctx, right ? fullWidth - 1.5f : 0, y, 1.5f, lineHeight * a, ColorUtil.fade(color, a));
            }
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x, y + 2, 0);
            ctx.drawText(tr, text, 0, 0, ColorUtil.fade(color, Math.max(0.05f, a)), true);
            ctx.getMatrices().pop();
            y += lineHeight * a;
        }
        setSize(fullWidth, Math.max(1, (int) Math.ceil(y)));
    }

    private int color(int index, int count) {
        return switch (colorMode.get()) {
            case ACCENT -> Theme.accent(index * 120L);
            case RAINBOW -> ColorUtil.rainbow(index * 150L, 0.6f, 0.65f, 1f);
            case GRADIENT -> ColorUtil.lerp(Theme.accent(), ColorUtil.brighten(Theme.accent(), 0.55f),
                count <= 1 ? 0 : (float) Math.abs(Math.sin(System.currentTimeMillis() / 900.0 + index * 0.35)));
            case STATIC -> staticColor.getColor();
        };
    }
}
