package dev.aurora.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.component.Component;
import dev.aurora.client.gui.component.FavoritesPanel;
import dev.aurora.client.gui.component.ModuleButton;
import dev.aurora.client.gui.component.Panel;
import dev.aurora.client.gui.component.ProfilesPanel;
import dev.aurora.client.gui.component.TextInput;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.modules.misc.ClickGui;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The ClickGUI: one draggable panel per category plus a profiles panel, a search bar,
 * tooltips, keyboard navigation and open/close animations.
 *
 * <p>Everything is drawn inside a matrix scaled by the GUI-scale setting; mouse coordinates are
 * divided by the same factor so components work purely in GUI-local units.</p>
 */
public final class ClickGuiScreen extends Screen {
    private static final float SEARCH_W = 190;
    private static final float SEARCH_H = 18;
    private static final long TOOLTIP_DELAY_MS = 450;

    private final ClickGui settings;
    private final List<Panel> panels = new ArrayList<>();
    private ProfilesPanel profilesPanel;
    private FavoritesPanel favoritesPanel;
    private java.util.Set<String> favorites = java.util.Set.of();
    private final TextInput search = new TextInput(32);
    private final Animation openAnim = new Animation(0, 10);
    private final Animation searchFocusAnim = new Animation(0, 16);
    private boolean searchFocused;
    private boolean closing;
    private float scale = 1;
    private boolean mouseInBody;
    private Component listening;
    private ModuleButton navFocus;
    private String tooltip;
    private long tooltipSince;
    private long openedAt;

    public ClickGuiScreen(ClickGui settings) {
        super(Text.literal("Aurora"));
        this.settings = settings;
        for (Category category : Category.values()) {
            Panel panel = new Panel(this, category.name(), category.getDisplayName(), category.getIcon());
            for (Module m : AuroraClient.modules().getByCategory(category)) panel.getChildren().add(new ModuleButton(this, m));
            panels.add(panel);
        }
        profilesPanel = new ProfilesPanel(this);
        panels.add(profilesPanel);
        favorites = ClickGui.parseFavorites(String.join(",", settings.favoriteIds()));
        favoritesPanel = new FavoritesPanel(this);
        panels.add(favoritesPanel);
    }

    // ------------------------------------------------------------------ helpers for components

    public ClickGui settings() {
        return settings;
    }

    public float guiWidth() {
        return width / scale;
    }

    public float guiHeight() {
        return height / scale;
    }

    /** Converts a GUI-local coordinate to the (unscaled) screen coordinate expected by scissor calls. */
    public int toScreen(float v) {
        return Math.round(v * scale);
    }

    public double animationSpeed() {
        return settings.getAnimationSpeed();
    }

    public boolean isMouseInBody() {
        return mouseInBody;
    }

    public void setMouseInBody(boolean value) {
        mouseInBody = value;
    }

    public Component getListening() {
        return listening;
    }

    public void setListening(Component component) {
        listening = component;
    }

    public ModuleButton getNavFocus() {
        return navFocus;
    }

    public void setNavFocus(ModuleButton button) {
        navFocus = button;
    }

    public boolean isFavorite(Module module) {
        return favorites.contains(module.getId());
    }

    /** Pins or unpins a module in the Favorites panel. */
    public void toggleFavorite(Module module) {
        settings.toggleFavorite(module);
        favorites = ClickGui.parseFavorites(String.join(",", settings.favoriteIds()));
        if (favoritesPanel.getChildren().contains(navFocus)) navFocus = null; // its button is about to be replaced
        favoritesPanel.refresh();
    }

    public void onProfileChanged() {
        profilesPanel.refresh();
        favorites = ClickGui.parseFavorites(String.join(",", settings.favoriteIds()));
        favoritesPanel.refresh();
        scale = settings.getGuiScale();
        layoutPanels();
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        scale = settings.getGuiScale();
        openAnim.snap(closing ? openAnim.get() : 0);
        closing = false;
        layoutPanels();
    }

    /** Places panels without a saved position in a responsive grid and keeps all panels on screen. */
    private void layoutPanels() {
        float gap = 8;
        int columns = Math.max(1, (int) ((guiWidth() - 16 + gap) / (Panel.WIDTH + gap)));
        float rowHeight = Math.max(140, (guiHeight() - 40) / 2);
        for (int i = 0; i < panels.size(); i++) {
            Panel p = panels.get(i);
            if (!p.hasPosition()) {
                int col = i % columns;
                int row = i / columns;
                float totalW = Math.min(panels.size(), columns) * (Panel.WIDTH + gap) - gap;
                float startX = Math.max(8, (guiWidth() - totalW) / 2);
                p.setPosition(startX + col * (Panel.WIDTH + gap), 36 + row * rowHeight);
            } else {
                p.setPosition(p.getX(), p.getY()); // clamps into the current window
            }
        }
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

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client != null && client.world != null) {
            if (settings.isBlur()) applyBlur(delta);
            float a = openAnim.getFloat();
            ctx.fillGradient(0, 0, width, height, ColorUtil.fade(0x66000000, a), ColorUtil.fade(0xA6000000, a));
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

        double mx = mouseX / scale;
        double my = mouseY / scale;
        String query = search.get();
        // Fade everything (shapes and text) with the open animation via the shader color modulator.
        RenderSystem.setShaderColor(1, 1, 1, Math.max(0.02f, a));
        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(0, (1 - a) * 14, 0);
        try {
            renderSearch(ctx, mx, my);
            for (Panel p : panels) p.render(ctx, mx, my, delta, query);
            renderFooter(ctx, mx, my);
            renderTooltip(ctx, mx, my);
        } finally {
            ctx.getMatrices().pop();
            ctx.draw();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private float searchX() {
        return guiWidth() / 2 - SEARCH_W / 2;
    }

    private boolean overSearch(double mx, double my) {
        return mx >= searchX() && mx < searchX() + SEARCH_W && my >= 8 && my < 8 + SEARCH_H;
    }

    private void renderSearch(DrawContext ctx, double mx, double my) {
        TextRenderer tr = textRenderer;
        searchFocusAnim.setTarget(searchFocused ? 1 : 0);
        float f = (float) searchFocusAnim.update();
        float x = searchX();
        Render2D.shadow(ctx, x, 8, SEARCH_W, SEARCH_H, 9, 4, 0x80000000);
        Render2D.roundedRect(ctx, x, 8, SEARCH_W, SEARCH_H, 9, Theme.panel());
        Render2D.roundedOutline(ctx, x, 8, SEARCH_W, SEARCH_H, 9, 1, ColorUtil.lerp(0x30FFFFFF, Theme.accent(), f));
        ctx.drawText(tr, "⌕", (int) x + 8, 13, ColorUtil.lerp(Theme.MUTED, Theme.accent(), f), false);
        boolean empty = search.isEmpty();
        String text = empty ? (searchFocused ? "" : "Search modules…  (Ctrl+F)") : search.get();
        boolean caret = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0;
        ctx.drawText(tr, text + (caret ? "_" : ""), (int) x + 20, 13, empty ? Theme.MUTED : Theme.TEXT, false);
    }

    private float hudButtonX() {
        return guiWidth() - 78;
    }

    private float footerY() {
        return guiHeight() - 22;
    }

    private boolean overHudButton(double mx, double my) {
        return mx >= hudButtonX() && mx < hudButtonX() + 70 && my >= footerY() && my < footerY() + 16;
    }

    private float keysButtonX() {
        return hudButtonX() - 76;
    }

    private boolean overKeysButton(double mx, double my) {
        return mx >= keysButtonX() && mx < keysButtonX() + 70 && my >= footerY() && my < footerY() + 16;
    }

    private void renderFooter(DrawContext ctx, double mx, double my) {
        TextRenderer tr = textRenderer;
        float bx = hudButtonX();
        float by = footerY();
        boolean hover = overHudButton(mx, my);
        Render2D.roundedRect(ctx, bx, by, 70, 16, 8, hover ? ColorUtil.withAlpha(Theme.accent(), 190) : Theme.panel());
        String label = "✎ Edit HUD";
        ctx.drawText(tr, label, (int) (bx + 35 - tr.getWidth(label) / 2f), (int) by + 4, Theme.TEXT, false);
        float kx = keysButtonX();
        boolean keysHover = overKeysButton(mx, my);
        Render2D.roundedRect(ctx, kx, by, 70, 16, 8, keysHover ? ColorUtil.withAlpha(Theme.accent(), 190) : Theme.panel());
        String keys = "⌨ Keybinds";
        ctx.drawText(tr, keys, (int) (kx + 35 - tr.getWidth(keys) / 2f), (int) by + 4, Theme.TEXT, false);
        String hint = "Left: toggle · Right: settings · Middle: pin · Drag headers · ↑↓ Enter: navigate · Esc: close";
        if (tr.getWidth(hint) < kx - 16) ctx.drawText(tr, hint, 8, (int) by + 4, ColorUtil.withAlpha(Theme.SUBTEXT, 170), false);
        String brand = "Aurora " + AuroraClient.version() + " · " + AuroraClient.config().getActiveProfile();
        ctx.drawText(tr, brand, (int) (guiWidth() - 8 - tr.getWidth(brand)), (int) by - 12, ColorUtil.withAlpha(Theme.MUTED, 200), false);
    }

    private void renderTooltip(DrawContext ctx, double mx, double my) {
        String current = null;
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.isOver(mx, my)) {
                current = p.getTooltip(mx, my);
                break;
            }
        }
        if (current == null || !current.equals(tooltip)) {
            tooltip = current;
            tooltipSince = System.currentTimeMillis();
        }
        if (tooltip == null || !settings.showTooltips() || System.currentTimeMillis() - tooltipSince < TOOLTIP_DELAY_MS) return;
        float fade = Math.min(1f, (System.currentTimeMillis() - tooltipSince - TOOLTIP_DELAY_MS) / 150f);
        List<OrderedText> lines = textRenderer.wrapLines(StringVisitable.plain(tooltip), 170);
        int w = 0;
        for (OrderedText line : lines) w = Math.max(w, textRenderer.getWidth(line));
        float tw = w + 10;
        float th = lines.size() * 10 + 7;
        float tx = (float) Math.min(mx + 10, guiWidth() - tw - 4);
        float ty = (float) Math.min(my + 12, guiHeight() - th - 4);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400);
        Render2D.shadow(ctx, tx, ty, tw, th, 4, 4, ColorUtil.fade(0x90000000, fade));
        Render2D.roundedRect(ctx, tx, ty, tw, th, 4, ColorUtil.fade(0xF20B0D12, fade));
        Render2D.roundedOutline(ctx, tx, ty, tw, th, 4, 1, ColorUtil.fade(ColorUtil.withAlpha(Theme.accent(), 120), fade));
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawText(textRenderer, lines.get(i), (int) tx + 5, (int) ty + 4 + i * 10,
                ColorUtil.withAlpha(Theme.TEXT, Math.max(4, (int) (255 * fade))), false);
        }
        ctx.getMatrices().pop();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;
        double mx = mouseX / scale;
        double my = mouseY / scale;
        Component wasListening = listening;
        if (overSearch(mx, my)) {
            searchFocused = true;
            if (button == 1) search.clear();
            return true;
        }
        searchFocused = false;
        if (overHudButton(mx, my) && button == 0 && client != null) {
            client.setScreen(new HudEditorScreen(this));
            return true;
        }
        if (overKeysButton(mx, my) && button == 0 && client != null) {
            client.setScreen(new KeybindsScreen(this));
            return true;
        }
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.mouseClicked(mx, my, button)) {
                panels.remove(i);
                panels.add(p); // bring to front
                if (listening == wasListening && wasListening != null && !wasListening.isHovered(mx, my)) listening = null;
                return true;
            }
        }
        listening = null;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double mx = mouseX / scale;
        double my = mouseY / scale;
        for (Panel p : panels) p.mouseReleased(mx, my, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return true; // dragging is resolved every frame from the current mouse position
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double mx = mouseX / scale;
        double my = mouseY / scale;
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseScrolled(mx, my, verticalAmount)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return false;
        if (listening != null) {
            for (Panel p : panels) if (p.keyPressed(keyCode, scanCode, modifiers)) return true;
            listening = null;
            return true;
        }
        if (searchFocused) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ESCAPE -> {
                    if (!search.isEmpty()) search.clear();
                    else searchFocused = false;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_DOWN -> {
                    searchFocused = false;
                    moveFocus(1);
                }
                default -> search.keyPressed(keyCode);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F && hasControlDown()) {
            searchFocused = true;
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (!search.isEmpty()) search.clear();
                else close();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveFocus(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveFocus(1);
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                searchFocused = true;
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (navFocus != null) navFocus.getModule().toggle();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (navFocus != null) navFocus.setExpanded(true);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (navFocus != null) navFocus.setExpanded(false);
                return true;
            }
            default -> {
            }
        }
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
            for (Panel p : panels) if (p.charTyped(chr, modifiers)) return true;
            return true;
        }
        if (!searchFocused && Character.isLetterOrDigit(chr)) searchFocused = true; // type anywhere to search
        if (searchFocused) {
            search.charTyped(chr);
            return true;
        }
        return false;
    }

    /** Keyboard navigation over visible, matching modules, panel by panel from left to right. */
    private void moveFocus(int direction) {
        List<Panel> ordered = new ArrayList<>(panels);
        ordered.sort(Comparator.comparingDouble(Panel::getX).thenComparingDouble(Panel::getY));
        List<ModuleButton> list = new ArrayList<>();
        List<Panel> owners = new ArrayList<>();
        String query = search.get();
        for (Panel p : ordered) {
            if (!p.isOpen()) continue;
            for (Component c : p.getChildren()) {
                if (c instanceof ModuleButton mb && mb.matches(query)) {
                    list.add(mb);
                    owners.add(p);
                }
            }
        }
        if (list.isEmpty()) return;
        int index = list.indexOf(navFocus);
        index = index < 0 ? (direction > 0 ? 0 : list.size() - 1) : Math.floorMod(index + direction, list.size());
        navFocus = list.get(index);
        owners.get(index).ensureVisible(navFocus);
    }
}
