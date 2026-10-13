package dev.aurora.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.clickgui.Element;
import dev.aurora.client.gui.clickgui.ModuleCard;
import dev.aurora.client.gui.clickgui.ProfilesView;
import dev.aurora.client.gui.clickgui.SettingsView;
import dev.aurora.client.gui.component.TextInput;
import dev.aurora.client.gui.menu.AuroraLogo;
import dev.aurora.client.gui.ui.Icons;
import dev.aurora.client.gui.ui.Ui;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.modules.misc.ClickGui;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The ClickGUI: a floating window with a sidebar (category icons, favorites, profiles, HUD editor, keybinds), a search
 * bar and a grid of module cards with switches. Right-clicking a card slides in its settings page.
 *
 * <p>Everything is drawn in window-local units inside one matrix transform (GUI scale setting, open animation); the
 * mouse is mapped back with the same transform and {@link #toScreenX}/{@link #toScreenY} convert for scissor calls.</p>
 */
public final class ClickGuiScreen extends Screen {
    private static final float SIDEBAR_W = 138;
    private static final float HEADER_H = 48;
    private static final float MIN_W = 460;
    private static final float MIN_H = 300;
    private static final float CARD_MIN_W = 186;
    private static final long TOOLTIP_DELAY_MS = 450;

    private enum Kind {
        FAVORITES, CATEGORY, PROFILES, HUD, KEYBINDS
    }

    private record Item(Kind kind, Category category, String label, Icons.Icon icon) {
    }

    private final ClickGui settings;
    private final List<Item> items = new ArrayList<>();
    private final Map<Module, ModuleCard> cards = new IdentityHashMap<>();
    private final List<ModuleCard> visible = new ArrayList<>();
    private final TextInput search = new TextInput(32);
    private final Animation openAnim = new Animation(0, 10);
    private final Animation searchFocusAnim = new Animation(0, 16);
    private final Animation settingsAnim = new Animation(0, 12);
    private final Animation indicator = new Animation(-1, 16);
    private final Animation scroll = new Animation(0, 18);
    private final Animation[] itemHover;
    private final ProfilesView profilesView;
    private Item page;
    private SettingsView settingsView;
    private String lastQuery = "";
    private double scrollTarget;
    private boolean searchFocused;
    private boolean closing;
    private boolean mouseInContent;
    private Element listening;
    private Set<String> favorites = Set.of();
    private String tooltip;
    private long tooltipSince;
    private long openedAt;

    // Transform of the current frame (window-local units -> GUI-scaled screen units).
    private float s = 1;
    private float localW;
    private float localH;
    private float shiftY;
    private float winX, winY, winW, winH;

    public ClickGuiScreen(ClickGui settings) {
        super(Text.literal("Aurora"));
        this.settings = settings;
        items.add(new Item(Kind.FAVORITES, null, "Favorites", Icons.Icon.STAR));
        for (Category c : Category.values()) items.add(new Item(Kind.CATEGORY, c, c.getDisplayName(), Icons.of(c)));
        items.add(new Item(Kind.PROFILES, null, "Profiles", Icons.Icon.PROFILES));
        items.add(new Item(Kind.HUD, null, "Edit HUD", Icons.Icon.HUD));
        items.add(new Item(Kind.KEYBINDS, null, "Keybinds", Icons.Icon.KEYBOARD));
        itemHover = new Animation[items.size()];
        for (int i = 0; i < itemHover.length; i++) itemHover[i] = new Animation(0, 16);
        for (Module m : AuroraClient.modules().getAll()) cards.put(m, new ModuleCard(this, m));
        profilesView = new ProfilesView(this);
        refreshFavorites();
        page = items.get(favorites.isEmpty() ? 1 : 0);
        rebuild();
    }

    // ------------------------------------------------------------------ API for widgets

    public ClickGui settings() {
        return settings;
    }

    public double animationSpeed() {
        return settings.getAnimationSpeed();
    }

    public int toScreenX(float localX) {
        return Math.round(width / 2f + (localX - localW / 2f) * s);
    }

    public int toScreenY(float localY) {
        return Math.round(height / 2f + shiftY + (localY - localH / 2f) * s);
    }

    private double toLocalX(double screenX) {
        return (screenX - width / 2f) / s + localW / 2f;
    }

    private double toLocalY(double screenY) {
        return (screenY - height / 2f - shiftY) / s + localH / 2f;
    }

    public boolean isMouseInContent() {
        return mouseInContent;
    }

    public void setMouseInContent(boolean value) {
        mouseInContent = value;
    }

    public Element getListening() {
        return listening;
    }

    public void setListening(Element element) {
        listening = element;
    }

    public boolean isFavorite(Module module) {
        return favorites.contains(module.getId());
    }

    public void toggleFavorite(Module module) {
        settings.toggleFavorite(module);
        refreshFavorites();
        if (page.kind() == Kind.FAVORITES && search.isEmpty()) rebuild();
    }

    private void refreshFavorites() {
        favorites = ClickGui.parseFavorites(String.join(",", settings.favoriteIds()));
    }

    public void openSettings(Module module) {
        settingsView = new SettingsView(this, module);
        listening = null;
    }

    public void closeSettings() {
        settingsView = null;
        listening = null;
    }

    public void onProfileChanged() {
        profilesView.refresh();
        refreshFavorites();
        settingsView = null;
        rebuild();
    }

    // ------------------------------------------------------------------ pages

    private void select(Item item) {
        switch (item.kind()) {
            case HUD -> {
                if (client != null) client.setScreen(new HudEditorScreen(this));
            }
            case KEYBINDS -> {
                if (client != null) client.setScreen(new KeybindsScreen(this));
            }
            default -> {
                page = item;
                settingsView = null;
                search.clear();
                searchFocused = false;
                rebuild();
            }
        }
    }

    /** Recomputes the cards shown for the current page or search. */
    private void rebuild() {
        visible.clear();
        String q = search.get().trim().toLowerCase(Locale.ROOT);
        lastQuery = search.get();
        if (!q.isEmpty()) {
            for (Module m : AuroraClient.modules().getAll()) {
                if (m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
                    visible.add(cards.get(m));
                }
            }
        } else if (page.kind() == Kind.FAVORITES) {
            for (String id : settings.favoriteIds()) AuroraClient.modules().find(id).ifPresent(m -> visible.add(cards.get(m)));
        } else if (page.kind() == Kind.CATEGORY) {
            for (Module m : AuroraClient.modules().getByCategory(page.category())) visible.add(cards.get(m));
        }
        for (int i = 0; i < visible.size(); i++) visible.get(i).resetAppear(i);
        scrollTarget = 0;
        scroll.snap(0);
    }

    private String pageTitle() {
        return search.isEmpty() ? page.label() : "Search";
    }

    private String pageSubtitle() {
        if (!search.isEmpty()) {
            return visible.size() + (visible.size() == 1 ? " module matches" : " modules match") + " \"" + search.get() + "\"";
        }
        return switch (page.kind()) {
            case FAVORITES -> visible.isEmpty() ? "Pinned modules" : visible.size() + " pinned";
            case CATEGORY -> {
                int on = 0;
                for (ModuleCard c : visible) if (c.getModule().isEnabled()) on++;
                yield visible.size() + " modules  ·  " + on + " enabled";
            }
            default -> "";
        };
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        openAnim.snap(closing ? openAnim.get() : 0);
        closing = false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        listening = null;
    }

    @Override
    public boolean shouldPause() {
        return settings.pausesGame();
    }

    // ------------------------------------------------------------------ layout

    private void layout(float open) {
        float fit = Math.min(width / (MIN_W + 24), height / (MIN_H + 24));
        float base = Math.max(0.5f, Math.min(settings.getGuiScale(), fit));
        localW = width / base;
        localH = height / base;
        s = base * (0.94f + 0.06f * open);
        shiftY = (1 - open) * 10;
        winW = Math.min(localW - 24, 660);
        winH = Math.min(localH - 24, 400);
        winX = (localW - winW) / 2f;
        winY = (localH - winH) / 2f;
    }

    private float contentX() {
        return winX + SIDEBAR_W;
    }

    private float contentW() {
        return winW - SIDEBAR_W;
    }

    private float bodyTop() {
        return winY + HEADER_H + 6;
    }

    private float searchW() {
        return Math.min(190, contentW() * 0.42f);
    }

    private float searchX() {
        return winX + winW - Ui.PAD - 22 - 6 - searchW();
    }

    private boolean overSearch(double mx, double my) {
        return mx >= searchX() && mx < searchX() + searchW() && my >= winY + 13 && my < winY + 35;
    }

    private boolean overClose(double mx, double my) {
        float cx = winX + winW - Ui.PAD - 22;
        return mx >= cx && mx < cx + 22 && my >= winY + 13 && my < winY + 35;
    }

    private float itemHeight() {
        return winH < 340 ? 18 : 22;
    }

    private float itemY(int index) {
        float y = winY + 64;
        int firstClient = items.size() - 3;
        if (index >= firstClient) y += 18; // gap and "CLIENT" heading before the client pages
        return y + index * (itemHeight() + 2);
    }

    private int itemAt(double mx, double my) {
        if (mx < winX + 8 || mx >= winX + SIDEBAR_W - 8) return -1;
        for (int i = 0; i < items.size(); i++) {
            float y = itemY(i);
            if (my >= y && my < y + itemHeight()) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float a = openAnim.getFloat();
        if (client != null && client.world != null) {
            if (settings.isBlur()) applyBlur(delta);
            ctx.fillGradient(0, 0, width, height, ColorUtil.fade(0x70000000, a), ColorUtil.fade(0xB0000000, a));
        } else {
            super.renderBackground(ctx, mouseX, mouseY, delta);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        openAnim.setSpeed(10 * animationSpeed());
        openAnim.setTarget(closing ? 0 : 1);
        float a = (float) openAnim.update();
        if (closing && a < 0.04f) {
            if (client != null) client.setScreen(null);
            return;
        }
        renderBackground(ctx, mouseX, mouseY, delta);
        if (!search.get().equals(lastQuery)) rebuild();
        layout(a);
        double mx = toLocalX(mouseX);
        double my = toLocalY(mouseY);

        ctx.draw();
        RenderSystem.setShaderColor(1, 1, 1, Math.max(0.02f, a));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f + shiftY, 0);
        ctx.getMatrices().scale(s, s, 1);
        ctx.getMatrices().translate(-localW / 2f, -localH / 2f, 0);
        try {
            float radius = Math.max(4, Theme.radius() * 2.5f);
            Ui.window(ctx, winX, winY, winW, winH, radius);
            renderSidebar(ctx, mx, my, radius);
            renderHeader(ctx, mx, my);
            renderContent(ctx, mx, my, delta, a);
            renderTooltip(ctx, mx, my);
        } finally {
            ctx.getMatrices().pop();
            ctx.draw();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private void renderSidebar(DrawContext ctx, double mx, double my, float radius) {
        Render2D.roundedRect(ctx, winX, winY, SIDEBAR_W, winH, radius, Theme.sidebarColor());
        Render2D.rect(ctx, winX + SIDEBAR_W - radius, winY, radius, winH, Theme.sidebarColor());
        Render2D.rect(ctx, winX + SIDEBAR_W - 1, winY + 10, 1, winH - 20, Ui.BORDER);
        AuroraLogo.icon(ctx, (int) winX + 14, (int) winY + 14, 22);
        UiFont.title(ctx, "Aurora", winX + 42, winY + 13, Ui.TEXT);
        UiFont.draw(ctx, "v" + AuroraClient.version(), winX + 42, winY + 28, Ui.MUTED);
        UiFont.bold(ctx, "MODULES", winX + 16, winY + 50, Ui.MUTED);
        UiFont.bold(ctx, "CLIENT", winX + 16, itemY(items.size() - 3) - 14, Ui.MUTED);

        int selected = items.indexOf(page);
        float h = itemHeight();
        if (search.isEmpty() && selected >= 0) {
            float target = itemY(selected);
            if (indicator.get() < 0) indicator.snap(target);
            indicator.setSpeed(16 * animationSpeed());
            indicator.setTarget(target);
            float iy = (float) indicator.update();
            Render2D.roundedRect(ctx, winX + 8, iy, SIDEBAR_W - 16, h, Ui.R_MEDIUM, Ui.accent(0x2C));
            Render2D.roundedRect(ctx, winX + 8, iy + h / 2f - 5, 3, 10, 1.5f, Ui.accent(0xFF));
        }
        int hovered = itemAt(mx, my);
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            float y = itemY(i);
            itemHover[i].setSpeed(16 * animationSpeed());
            itemHover[i].setTarget(i == hovered ? 1 : 0);
            float hv = (float) itemHover[i].update();
            boolean active = i == selected && search.isEmpty();
            if (hv > 0.01f && !active) {
                Render2D.roundedRect(ctx, winX + 8, y, SIDEBAR_W - 16, h, Ui.R_MEDIUM, ColorUtil.withAlpha(0xFFFFFF, Math.round(12 * hv)));
            }
            int color = active ? Ui.TEXT : ColorUtil.lerp(Ui.SUBTEXT, Ui.TEXT, hv);
            Icons.draw(ctx, item.icon(), winX + 18, y + (h - 14) / 2f, active ? Ui.accentLight() : color);
            UiFont.draw(ctx, item.label(), winX + 38, y + (h - 8) / 2f, color, UiFont.Size.BODY, active);
            if (item.kind() == Kind.CATEGORY) {
                int on = 0;
                for (Module m : AuroraClient.modules().getByCategory(item.category())) if (m.isEnabled()) on++;
                if (on > 0) {
                    String count = String.valueOf(on);
                    float cw = UiFont.width(count) + 8;
                    Render2D.roundedRect(ctx, winX + SIDEBAR_W - 14 - cw, y + (h - 12) / 2f, cw, 12, 6, active ? Ui.accent(0x80) : 0x18FFFFFF);
                    UiFont.draw(ctx, count, winX + SIDEBAR_W - 14 - cw + 4, y + (h - 8) / 2f, active ? Ui.TEXT : Ui.MUTED);
                }
            }
        }
        String profile = AuroraClient.config().getActiveProfile();
        UiFont.draw(ctx, UiFont.trim("Profile: " + profile, (int) SIDEBAR_W - 28, UiFont.Size.BODY, false), winX + 16, winY + winH - 18, Ui.MUTED);
    }

    private void renderHeader(DrawContext ctx, double mx, double my) {
        float cx = contentX() + Ui.PAD + 2;
        if (settingsView == null) {
            UiFont.title(ctx, pageTitle(), cx, winY + 14, Ui.TEXT);
            String sub = pageSubtitle();
            if (!sub.isEmpty()) UiFont.draw(ctx, UiFont.trim(sub, (int) (searchX() - cx - 8), UiFont.Size.BODY, false), cx, winY + 31, Ui.SUBTEXT);
        }
        searchFocusAnim.setTarget(searchFocused ? 1 : 0);
        float f = (float) searchFocusAnim.update();
        float sx = searchX();
        boolean showSearch = settingsView == null;
        if (showSearch) {
            Ui.field(ctx, sx, winY + 13, searchW(), 22, Math.max(f, overSearch(mx, my) ? 0.35f : 0));
            Icons.draw(ctx, Icons.Icon.SEARCH, sx + 7, winY + 17, ColorUtil.lerp(Ui.MUTED, Ui.accentLight(), f));
            boolean empty = search.isEmpty();
            String text = empty ? (searchFocused ? "" : "Search modules…") : search.get();
            boolean caret = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0;
            UiFont.draw(ctx, UiFont.trim(text, (int) searchW() - 34, UiFont.Size.BODY, false) + (caret ? "|" : ""), sx + 25, winY + 20,
                empty ? Ui.MUTED : Ui.TEXT);
        }
        float closeX = winX + winW - Ui.PAD - 22;
        boolean closeHover = overClose(mx, my);
        Render2D.roundedRect(ctx, closeX, winY + 13, 22, 22, Ui.R_MEDIUM, closeHover ? 0x30F87171 : Ui.surface());
        Icons.draw(ctx, Icons.Icon.CLOSE, closeX + 4, winY + 17, closeHover ? Ui.DANGER : Ui.SUBTEXT);
    }

    private void renderContent(DrawContext ctx, double mx, double my, float delta, float open) {
        settingsAnim.setSpeed(12 * animationSpeed());
        settingsAnim.setTarget(settingsView != null ? 1 : 0);
        float sa = (float) settingsAnim.update();
        float cx = contentX(), cw = contentW();
        mouseInContent = false;
        if (settingsView != null) {
            // The settings page slides in from the right.
            ctx.draw();
            RenderSystem.setShaderColor(1, 1, 1, Math.max(0.02f, open * sa));
            ctx.getMatrices().push();
            ctx.getMatrices().translate((1 - sa) * 30, 0, 0);
            settingsView.render(ctx, cx, winY, cw, winH - 6, mx, my, delta);
            ctx.getMatrices().pop();
            ctx.draw();
            RenderSystem.setShaderColor(1, 1, 1, Math.max(0.02f, open));
            return;
        }
        if (page.kind() == Kind.PROFILES && search.isEmpty()) {
            profilesView.render(ctx, cx, winY, cw, winH - 8, mx, my);
            return;
        }
        float top = bodyTop();
        float bottom = winY + winH - 8;
        float left = cx + Ui.PAD;
        float gridW = cw - Ui.PAD * 2;
        int cols = Math.max(1, (int) ((gridW + Ui.GAP) / (CARD_MIN_W + Ui.GAP)));
        float cardW = (gridW - (cols - 1) * Ui.GAP) / cols;
        int rows = (visible.size() + cols - 1) / cols;
        float contentHeight = rows * (ModuleCard.HEIGHT + Ui.GAP);
        double maxScroll = Math.max(0, contentHeight - (bottom - top));
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll.setSpeed(18 * animationSpeed());
        scroll.setTarget(scrollTarget);
        float sc = (float) scroll.update();
        mouseInContent = mx >= cx && mx < cx + cw && my >= top && my < bottom;
        ctx.enableScissor(toScreenX(cx), toScreenY(top - 4), toScreenX(cx + cw), toScreenY(bottom));
        for (int i = 0; i < visible.size(); i++) {
            ModuleCard card = visible.get(i);
            float x = left + (i % cols) * (cardW + Ui.GAP);
            float y = top + 2 + (i / cols) * (ModuleCard.HEIGHT + Ui.GAP) - sc;
            card.setBounds(x, y, cardW);
            if (y + ModuleCard.HEIGHT >= top - 8 && y <= bottom) card.render(ctx, mx, my, delta);
        }
        ctx.disableScissor();
        if (visible.isEmpty()) {
            float midY = top + (bottom - top) / 2f - 14;
            boolean favoritesPage = page.kind() == Kind.FAVORITES && search.isEmpty();
            String title = !search.isEmpty() ? "No modules match" : favoritesPage ? "No favorites yet" : "Nothing here";
            String hint = !search.isEmpty() ? "Try another word" : favoritesPage ? "Middle-click (or Shift+click) a module to pin it here" : "";
            if (favoritesPage) Icons.draw(ctx, Icons.Icon.STAR, cx + cw / 2f - 7, midY - 20, Ui.MUTED);
            UiFont.centered(ctx, title, cx + cw / 2f, midY, Ui.SUBTEXT, UiFont.Size.BODY, true);
            if (!hint.isEmpty()) UiFont.centered(ctx, hint, cx + cw / 2f, midY + 14, Ui.MUTED, UiFont.Size.BODY, false);
        }
        if (maxScroll > 0) {
            float visibleH = bottom - top;
            float barH = Math.max(16, visibleH * visibleH / contentHeight);
            float barY = top + (float) (sc / maxScroll) * (visibleH - barH);
            Render2D.roundedRect(ctx, cx + cw - 5, barY, 3, barH, 1.5f, Ui.accent(0x90));
        }
    }

    private void renderTooltip(DrawContext ctx, double mx, double my) {
        String current = null;
        if (settingsView != null) current = settingsView.getTooltip(mx, my);
        else if (page.kind() == Kind.PROFILES && search.isEmpty()) current = profilesView.getTooltip(mx, my);
        else if (mouseInContent) {
            for (ModuleCard c : visible) if (c.isHovered(mx, my)) current = c.getTooltip(mx, my);
        }
        int item = itemAt(mx, my);
        if (item >= 0 && items.get(item).kind() == Kind.HUD) current = "Open the HUD editor";
        if (item >= 0 && items.get(item).kind() == Kind.KEYBINDS) current = "All keybinds and conflicts";
        if (current == null || !current.equals(tooltip)) {
            tooltip = current;
            tooltipSince = System.currentTimeMillis();
        }
        if (tooltip == null || !settings.showTooltips() || System.currentTimeMillis() - tooltipSince < TOOLTIP_DELAY_MS) return;
        float fade = Math.min(1f, (System.currentTimeMillis() - tooltipSince - TOOLTIP_DELAY_MS) / 150f);
        String[] lines = Ui.wrap(tooltip, 180, 4);
        int w = 0;
        for (String line : lines) w = Math.max(w, UiFont.width(line));
        float tw = w + 14;
        float th = lines.length * 11 + 9;
        float tx = (float) Math.min(mx + 12, winX + winW - tw - 4);
        float ty = (float) Math.min(my + 14, winY + winH - th - 4);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400);
        Render2D.shadow(ctx, tx, ty, tw, th, Ui.R_MEDIUM, 5, ColorUtil.fade(0x90000000, fade));
        Render2D.roundedRect(ctx, tx, ty, tw, th, Ui.R_MEDIUM, ColorUtil.fade(0xF4080A10, fade));
        Render2D.roundedOutline(ctx, tx, ty, tw, th, Ui.R_MEDIUM, 1, ColorUtil.fade(Ui.accent(0x70), fade));
        for (int i = 0; i < lines.length; i++) {
            UiFont.draw(ctx, lines[i], tx + 7, ty + 5 + i * 11, ColorUtil.withAlpha(Ui.TEXT, Math.max(4, (int) (255 * fade))));
        }
        ctx.getMatrices().pop();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;
        double mx = toLocalX(mouseX);
        double my = toLocalY(mouseY);
        Element wasListening = listening;
        if (settingsView == null && overSearch(mx, my)) {
            searchFocused = true;
            if (button == 1) search.clear();
            return true;
        }
        searchFocused = false;
        if (overClose(mx, my) && button == 0) {
            close();
            return true;
        }
        int item = itemAt(mx, my);
        if (item >= 0) {
            if (button == 0) select(items.get(item));
            return true;
        }
        boolean handled = false;
        if (settingsView != null) {
            handled = settingsView.mouseClicked(mx, my, button);
        } else if (page.kind() == Kind.PROFILES && search.isEmpty()) {
            handled = profilesView.mouseClicked(mx, my, button);
        } else if (mouseInContent) {
            for (ModuleCard c : visible) {
                if (c.mouseClicked(mx, my, button)) {
                    handled = true;
                    break;
                }
            }
        }
        if (listening == wasListening && wasListening != null && !wasListening.isHovered(mx, my)) listening = null;
        boolean inWindow = mx >= winX && mx < winX + winW && my >= winY && my < winY + winH;
        if (!handled && !inWindow && button == 0) close(); // a click outside the window closes it
        return handled || inWindow;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (settingsView != null) settingsView.mouseReleased(toLocalX(mouseX), toLocalY(mouseY), button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return true; // sliders and pickers follow the mouse every frame
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double mx = toLocalX(mouseX);
        double my = toLocalY(mouseY);
        if (settingsView != null) return settingsView.mouseScrolled(mx, my, verticalAmount);
        if (page.kind() == Kind.PROFILES && search.isEmpty()) return profilesView.mouseScrolled(mx, my, verticalAmount);
        if (mx >= contentX() && mx < winX + winW && my >= bodyTop() && my < winY + winH) {
            scrollTarget -= verticalAmount * 32;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return false;
        if (listening != null) {
            if (settingsView != null && settingsView.keyPressed(keyCode, scanCode, modifiers)) return true;
            listening = null;
            return true;
        }
        if (searchFocused) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ESCAPE -> {
                    if (!search.isEmpty()) search.clear();
                    else searchFocused = false;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_TAB -> searchFocused = false;
                default -> search.keyPressed(keyCode);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F && hasControlDown()) {
            searchFocused = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (settingsView != null) closeSettings();
            else if (!search.isEmpty()) search.clear();
            else close();
            return true;
        }
        if (settingsView != null && settingsView.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (settings.getKeybind().matchesKey(keyCode) && System.currentTimeMillis() - openedAt > 250) {
            close();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (closing) return false;
        if (listening != null) {
            if (settingsView != null) settingsView.charTyped(chr, modifiers);
            return true;
        }
        if (!searchFocused && settingsView == null && Character.isLetterOrDigit(chr)) searchFocused = true; // type anywhere to search
        if (settingsView != null) return false;
        if (searchFocused) {
            search.charTyped(chr);
            return true;
        }
        return false;
    }
}
