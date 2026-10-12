import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Generates the original pixel art of the "Aurora Bedwars" resource pack.
 * Run: java tools/BedwarsPackGenerator.java src/main/resources/resourcepacks/aurora_bedwars src/main/resources/assets/aurora/icon.png
 */
public class BedwarsPackGenerator {
    static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    static final int[] RGB = {0xF1F3F6, 0xFF8A1F, 0xD94FD6, 0x4FB8FF, 0xFFD62E, 0x7BE34B, 0xFF8FC2, 0x5A5F69,
        0xA4AAB4, 0x1FB5B5, 0x8A4FE0, 0x3559E0, 0x8A5A32, 0x3F8A2C, 0xE0353A, 0x26272D};

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args[0]);
        Path tex = out.resolve("assets/minecraft/textures");
        Files.createDirectories(tex.resolve("block"));
        Files.createDirectories(tex.resolve("item"));
        Files.createDirectories(tex.resolve("entity/bed"));
        Files.writeString(out.resolve("pack.mcmeta"), """
            {
              "pack": {
                "pack_format": 34,
                "description": "§dAurora Bedwars §7· clean PvP textures"
              }
            }
            """, StandardCharsets.UTF_8);
        for (int i = 0; i < COLORS.length; i++) {
            save(wool(RGB[i], i), tex.resolve("block/" + COLORS[i] + "_wool.png"));
            save(terracotta(RGB[i], i), tex.resolve("block/" + COLORS[i] + "_terracotta.png"));
            save(glass(RGB[i], true), tex.resolve("block/" + COLORS[i] + "_stained_glass.png"));
            save(bed(RGB[i]), tex.resolve("entity/bed/" + COLORS[i] + ".png"));
        }
        save(glass(0xDDE6F0, false), tex.resolve("block/glass.png"));
        save(speckled(0xE6E1AE, 0xC9C28A, 11), tex.resolve("block/end_stone.png"));
        save(speckled(0x1C1230, 0x3B2266, 12), tex.resolve("block/obsidian.png"));
        String[] swords = {"wooden", "stone", "iron", "golden", "diamond"};
        int[][] palettes = {
            {0xD9AE6C, 0xB0844A, 0x845B2C, 0x3B2712, 0x6B4A2E},
            {0xC9C9C9, 0x9A9A9A, 0x6E6E6E, 0x2A2A2A, 0x4A4A55},
            {0xFFFFFF, 0xDCE2EA, 0xA9B2BF, 0x353A44, 0x4A4E5C},
            {0xFFF7B0, 0xFFD83A, 0xD99A12, 0x5A3A06, 0xB8860B},
            {0xE2FFFC, 0x63F2E4, 0x1FB5A8, 0x0B3F3B, 0x2E5F7A}};
        for (int i = 0; i < swords.length; i++) save(sword(palettes[i]), tex.resolve("item/" + swords[i] + "_sword.png"));
        BufferedImage logo = logo(palettes[4]);
        save(logo, out.resolve("pack.png"));
        if (args.length > 1) {
            Files.createDirectories(Path.of(args[1]).getParent());
            save(logo, Path.of(args[1]));
        }
        System.out.println("Generated Aurora Bedwars pack in " + out.toAbsolutePath());
    }

    static void save(BufferedImage img, Path path) throws IOException {
        ImageIO.write(img, "png", path.toFile());
    }

    static int shade(int rgb, double f) {
        int r = clamp((int) Math.round(((rgb >> 16) & 255) * f));
        int g = clamp((int) Math.round(((rgb >> 8) & 255) * f));
        int b = clamp((int) Math.round((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    /** Clean wool: soft knit pattern, gentle noise and a bevel so placed blocks read clearly. */
    static BufferedImage wool(int base, int seed) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(1000 + seed);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double f = 1 + (rnd.nextDouble() - 0.5) * 0.06;
                if (((x + y * 2) & 3) == 0) f -= 0.05; // knit
                if (x == 0 || y == 0) f += 0.10;
                if (x == 15 || y == 15) f -= 0.14;
                img.setRGB(x, y, opaque(shade(base, f)));
            }
        }
        return img;
    }

    static BufferedImage terracotta(int base, int seed) {
        int muted = mix(base, 0x9A7462, 0.3);
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(2000 + seed);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double f = 0.92 + (rnd.nextDouble() - 0.5) * 0.05;
                if (x == 0 || y == 0 || x == 15 || y == 15) f -= 0.12;
                img.setRGB(x, y, opaque(shade(muted, f)));
            }
        }
        return img;
    }

    /** Glass with a crisp frame and a small highlight; stained versions are tinted and translucent. */
    static BufferedImage glass(int base, boolean stained) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
                int argb;
                if (edge) argb = (0xE6 << 24) | shade(base, stained ? 0.75 : 0.9);
                else if (x + y >= 3 && x + y <= 5 && x > 1 && y > 1) argb = 0x9AFFFFFF; // highlight streak
                else argb = stained ? (0x70 << 24) | base : 0x00000000;
                img.setRGB(x, y, argb);
            }
        }
        return img;
    }

    static BufferedImage speckled(int base, int speck, int seed) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(seed);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int c = rnd.nextDouble() < 0.12 ? speck : shade(base, 1 + (rnd.nextDouble() - 0.5) * 0.08);
                img.setRGB(x, y, opaque(c));
            }
        }
        return img;
    }

    /**
     * 64x64 bed entity texture. Follows the vanilla bed model UV layout: head box at (0,0) and foot box
     * at (0,22) (16x16x6 each), legs at x=50. Top faces get the blanket (and pillow on the head).
     */
    static BufferedImage bed(int color) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int wood = 0x6B4A2E;
        int side = shade(color, 0.82);
        // Side / end faces of both halves.
        fill(img, 0, 6, 6, 16, side);
        fill(img, 22, 6, 6, 16, side);
        fill(img, 6, 0, 32, 6, side);
        fill(img, 0, 28, 6, 16, side);
        fill(img, 22, 28, 6, 16, side);
        fill(img, 6, 22, 32, 6, side);
        // Undersides: planks.
        planks(img, 28, 6, wood);
        planks(img, 28, 28, wood);
        // Head top: pillow + blanket.
        blanket(img, 6, 6, color);
        fill(img, 7, 7, 14, 5, 0xF4F4F6);
        fill(img, 7, 11, 14, 1, 0xD5D6DC);
        fill(img, 7, 7, 14, 1, 0xFFFFFF);
        // Foot top: blanket with a folded edge.
        blanket(img, 6, 28, color);
        fill(img, 6, 42, 16, 2, shade(color, 0.7));
        // Legs.
        for (int v = 0; v < 24; v += 6) fill(img, 50, v, 12, 6, shade(wood, 0.85));
        return img;
    }

    static void blanket(BufferedImage img, int x0, int y0, int color) {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double f = (x == 0 || x == 15) ? 0.85 : ((x + y) % 5 == 0 ? 0.95 : 1.0);
                img.setRGB(x0 + x, y0 + y, opaque(shade(color, f)));
            }
        }
    }

    static void planks(BufferedImage img, int x0, int y0, int wood) {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double f = (y % 4 == 3) ? 0.75 : (x == (y / 4 * 5) % 16 ? 0.85 : 1.0);
                img.setRGB(x0 + x, y0 + y, opaque(shade(wood, f)));
            }
        }
    }

    static void fill(BufferedImage img, int x0, int y0, int w, int h, int rgb) {
        for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) img.setRGB(x, y, opaque(rgb));
    }

    /**
     * Short PvP-style sword. Roles are placed on a 16x16 grid, then every empty pixel touching the
     * sprite becomes the outline color, which gives clean, readable pixel art for each material.
     * palette = {highlight, light, dark, outline, guard}
     */
    static BufferedImage sword(int[] p) {
        int[][] role = new int[16][16]; // 0 empty, 1 highlight, 2 light, 3 dark, 4 guard, 5 grip, 6 grip dark, 7 pommel
        for (int t = 0; t <= 6; t++) role[9 - t][6 + t] = 2;      // blade core
        for (int t = 0; t <= 5; t++) role[9 - t][7 + t] = 3;      // shaded edge
        for (int t = 0; t <= 5; t++) role[8 - t][6 + t] = 1;      // bright edge
        int[][] guard = {{8, 3}, {9, 4}, {10, 5}, {11, 6}, {12, 7}};
        for (int[] g : guard) role[g[0]][g[1]] = 4;
        role[11][4] = 5;
        role[12][3] = 6;
        role[13][2] = 5;
        role[14][1] = 7;
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int grip = 0x7A5230;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int c = switch (role[y][x]) {
                    case 1 -> opaque(p[0]);
                    case 2 -> opaque(p[1]);
                    case 3 -> opaque(p[2]);
                    case 4 -> opaque(p[4]);
                    case 5 -> opaque(grip);
                    case 6 -> opaque(shade(grip, 0.72));
                    case 7 -> opaque(shade(p[4], 1.25));
                    default -> touches(role, x, y) ? opaque(p[3]) : 0;
                };
                img.setRGB(x, y, c);
            }
        }
        return img;
    }

    static boolean touches(int[][] role, int x, int y) {
        int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : n) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && role[ny][nx] != 0) return true;
        }
        return false;
    }

    /** 128x128 pack / mod icon: diamond short sword on an aurora gradient. */
    static BufferedImage logo(int[] diamond) {
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                double t = (x + y) / 254.0;
                int c = mix(mix(0x2A1B5C, 0x7C3AED, t), 0x0EA5E9, Math.max(0, t - 0.5));
                double dx = Math.max(Math.max(14 - x, x - 113), 0), dy = Math.max(Math.max(14 - y, y - 113), 0);
                boolean inside = dx * dx + dy * dy <= 14 * 14;
                img.setRGB(x, y, inside ? opaque(c) : 0);
            }
        }
        BufferedImage s = sword(diamond);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int c = s.getRGB(x, y);
                if ((c >>> 24) == 0) continue;
                for (int yy = 0; yy < 6; yy++) for (int xx = 0; xx < 6; xx++) img.setRGB(16 + x * 6 + xx, 16 + y * 6 + yy, c);
            }
        }
        return img;
    }
}
