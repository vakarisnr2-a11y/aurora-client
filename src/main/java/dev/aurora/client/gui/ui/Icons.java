package dev.aurora.client.gui.ui;

import dev.aurora.client.module.Category;
import dev.aurora.client.render.Render2D;
import net.minecraft.client.gui.DrawContext;

/** Small vector line icons (drawn in a 14x14 box at x, y) for the sidebar, buttons and fields. */
public final class Icons {
    public enum Icon {
        COMBAT, VISUALS, MOVEMENT, PLAYER, WORLD, UTILITY, MISC, STAR, PROFILES, HUD, KEYBOARD, SEARCH, GEAR, BACK, CLOSE, CHECK,
        THEME, PLUS
    }

    private static final float T = 1.4f; // stroke

    private Icons() {
    }

    public static Icon of(Category category) {
        return switch (category) {
            case COMBAT -> Icon.COMBAT;
            case VISUALS -> Icon.VISUALS;
            case MOVEMENT -> Icon.MOVEMENT;
            case PLAYER -> Icon.PLAYER;
            case WORLD -> Icon.WORLD;
            case UTILITY -> Icon.UTILITY;
            case MISC -> Icon.MISC;
        };
    }

    public static void draw(DrawContext ctx, Icon icon, float x, float y, int c) {
        switch (icon) {
            case COMBAT -> {
                // Sword: blade, guard, grip.
                Render2D.line(ctx, x + 4.5f, y + 9.5f, x + 12, y + 2, T, c);
                Render2D.line(ctx, x + 2.5f, y + 7.5f, x + 6.5f, y + 11.5f, T, c);
                Render2D.line(ctx, x + 4.5f, y + 9.5f, x + 2, y + 12, T + 0.4f, c);
            }
            case VISUALS -> {
                Render2D.roundedOutline(ctx, x + 1, y + 3.5f, 12, 7, 3.5f, T, c);
                Render2D.circle(ctx, x + 7, y + 7, 2, c);
            }
            case MOVEMENT -> {
                // Double chevron.
                Render2D.line(ctx, x + 2, y + 3, x + 6, y + 7, T, c);
                Render2D.line(ctx, x + 6, y + 7, x + 2, y + 11, T, c);
                Render2D.line(ctx, x + 7, y + 3, x + 11, y + 7, T, c);
                Render2D.line(ctx, x + 11, y + 7, x + 7, y + 11, T, c);
            }
            case PLAYER -> {
                Render2D.ring(ctx, x + 7, y + 4.5f, 2.6f, T, c);
                Render2D.roundedOutline(ctx, x + 2.5f, y + 8.5f, 9, 5, 2.5f, T, c);
            }
            case WORLD -> {
                Render2D.ring(ctx, x + 7, y + 7, 5.5f, T, c);
                Render2D.roundedOutline(ctx, x + 4.6f, y + 1.5f, 4.8f, 11, 2.4f, 1f, c);
                Render2D.rect(ctx, x + 1.5f, y + 6.5f, 11, 1, c);
            }
            case UTILITY -> {
                // Sliders.
                Render2D.rect(ctx, x + 1.5f, y + 3.3f, 11, T, c);
                Render2D.rect(ctx, x + 1.5f, y + 9.3f, 11, T, c);
                Render2D.circle(ctx, x + 4.5f, y + 4, 2, c);
                Render2D.circle(ctx, x + 9.5f, y + 10, 2, c);
            }
            case MISC -> {
                for (int i = 0; i < 4; i++) {
                    Render2D.roundedRect(ctx, x + 2 + (i % 2) * 6, y + 2 + (i / 2) * 6, 4, 4, 1.2f, c);
                }
            }
            case STAR -> star(ctx, x + 7, y + 7.3f, 6, 2.6f, c);
            case PROFILES -> {
                Render2D.roundedOutline(ctx, x + 1.5f, y + 4, 9, 8.5f, 2, T, c);
                Render2D.rect(ctx, x + 4, y + 1.8f, 8.5f, T, c);
                Render2D.rect(ctx, x + 12.5f - T, y + 1.8f, T, 8, c);
            }
            case HUD -> {
                Render2D.roundedOutline(ctx, x + 1, y + 2, 12, 10, 2, T, c);
                Render2D.rect(ctx, x + 3, y + 4, 3.5f, 2, c);
                Render2D.rect(ctx, x + 8, y + 8, 3, 2, c);
            }
            case KEYBOARD -> {
                Render2D.roundedOutline(ctx, x + 0.5f, y + 3, 13, 8.5f, 2, T, c);
                for (int i = 0; i < 4; i++) Render2D.rect(ctx, x + 3 + i * 2.3f, y + 5.2f, 1.2f, 1.2f, c);
                Render2D.rect(ctx, x + 4, y + 8, 6, 1.2f, c);
            }
            case SEARCH -> {
                Render2D.ring(ctx, x + 6, y + 6, 4, T, c);
                Render2D.line(ctx, x + 9, y + 9, x + 12.5f, y + 12.5f, T + 0.3f, c);
            }
            case GEAR -> {
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    float cx = x + 7 + (float) Math.cos(a) * 5, cy = y + 7 + (float) Math.sin(a) * 5;
                    Render2D.circle(ctx, cx, cy, 1.3f, c);
                }
                Render2D.ring(ctx, x + 7, y + 7, 4.2f, 1.8f, c);
            }
            case BACK -> {
                Render2D.line(ctx, x + 9, y + 2.5f, x + 4.5f, y + 7, T + 0.2f, c);
                Render2D.line(ctx, x + 4.5f, y + 7, x + 9, y + 11.5f, T + 0.2f, c);
            }
            case CLOSE -> {
                Render2D.line(ctx, x + 3, y + 3, x + 11, y + 11, T, c);
                Render2D.line(ctx, x + 11, y + 3, x + 3, y + 11, T, c);
            }
            case CHECK -> {
                Render2D.line(ctx, x + 2.5f, y + 7.5f, x + 5.5f, y + 10.5f, T + 0.2f, c);
                Render2D.line(ctx, x + 5.5f, y + 10.5f, x + 11.5f, y + 3.5f, T + 0.2f, c);
            }
            case THEME -> {
                Render2D.ring(ctx, x + 7, y + 7, 5.5f, T, c);
                Render2D.circle(ctx, x + 5, y + 5, 1.2f, c);
                Render2D.circle(ctx, x + 9, y + 5, 1.2f, c);
                Render2D.circle(ctx, x + 5, y + 9, 1.2f, c);
            }
            case PLUS -> {
                Render2D.rect(ctx, x + 2.5f, y + 6.3f, 9, T, c);
                Render2D.rect(ctx, x + 6.3f, y + 2.5f, T, 9, c);
            }
        }
    }

    /** A filled five-pointed star centred on (cx, cy). */
    public static void star(DrawContext ctx, float cx, float cy, float outer, float inner, int c) {
        float[] pts = new float[20];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + Math.PI * i / 5;
            float r = (i & 1) == 0 ? outer : inner;
            pts[i * 2] = cx + (float) Math.cos(a) * r;
            pts[i * 2 + 1] = cy + (float) Math.sin(a) * r;
        }
        Render2D.polygon(ctx, cx, cy, pts, c);
    }
}
