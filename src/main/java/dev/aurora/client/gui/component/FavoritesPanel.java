package dev.aurora.client.gui.component;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.module.modules.misc.ClickGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Pinned modules. Middle-click (or Shift+click) a module anywhere in the ClickGUI to pin or unpin it. */
public final class FavoritesPanel extends Panel {
    public FavoritesPanel(ClickGuiScreen screen) {
        super(screen, ClickGui.FAVORITES_PANEL, "Favorites", "★");
        refresh();
    }

    public void refresh() {
        children.clear();
        for (String id : screen.settings().favoriteIds()) {
            AuroraClient.modules().find(id).ifPresent(m -> children.add(new ModuleButton(screen, m)));
        }
        if (children.isEmpty()) children.add(new Hint());
    }

    private static final class Hint extends Component {
        @Override
        public float getHeight() {
            return 26;
        }

        @Override
        public void render(DrawContext ctx, double mouseX, double mouseY, float delta) {
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.drawText(tr, "Middle-click or Shift+click", (int) x + 5, (int) y + 4, Theme.MUTED, false);
            ctx.drawText(tr, "a module to pin it here", (int) x + 5, (int) y + 14, Theme.MUTED, false);
        }
    }
}
