import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Generates the built-in "Aurora Bedwars" resource pack: vanilla's own 16x textures with an Aurora Client look.
 *
 * <pre>
 * java tools/BedwarsPackGenerator.java src/main/resources/resourcepacks/aurora_bedwars [minecraft-1.21.1.jar]
 * </pre>
 *
 * <p>The textures are read from your Minecraft 1.21.1 jar (the Gradle/Loom cache or the launcher's versions
 * folder is found automatically) and edited, so the pack keeps the shapes and colors everyone knows. The edits:</p>
 * <ul>
 *   <li>an "aurora" color grade on items and blocks: shadows lean violet, highlights lean cool teal-white and colors
 *       are a little richer (textures the game tints itself, like leather or potion liquid, are left neutral);</li>
 *   <li>short swords: a stretch of every blade is removed and the tip moved back;</li>
 *   <li>calmer team blocks: wool and terracotta noise is reduced so team colors read as clean solid colors;</li>
 *   <li>Aurora-styled extras drawn here: a dark GUI with violet accents, crosshair, enchantment glint, low fire,
 *       hit particles and the pack icon.</li>
 * </ul>
 */
public class BedwarsPackGenerator {
    static Path root;
    static Path tex;
    static ZipFile vanilla;
    static int written;
    /** Strength of the aurora grade on items. */
    static final double ITEM_GRADE = 1.6;

    public static void main(String[] args) throws IOException {
        root = Path.of(args.length > 0 ? args[0] : "src/main/resources/resourcepacks/aurora_bedwars");
        tex = root.resolve("assets/minecraft/textures");
        vanilla = new ZipFile(findJar(args.length > 1 ? args[1] : null).toFile());
        deleteTree(root.resolve("assets"));
        Weapons.generate();
        Armor.generate();
        Items.generate();
        TeamBlocks.generate();
        System.out.println("Generated " + written + " textures in " + root.toAbsolutePath());
    }

    /** The Minecraft 1.21.1 client jar: the given path, Loom's cache or the launcher's versions folder. */
    static Path findJar(String given) {
        List<Path> candidates = new ArrayList<>();
        if (given != null) candidates.add(Path.of(given));
        String home = System.getProperty("user.home");
        candidates.add(Path.of(home, ".gradle/caches/fabric-loom/1.21.1/minecraft-client.jar"));
        String appData = System.getenv("APPDATA");
        if (appData != null) candidates.add(Path.of(appData, ".minecraft/versions/1.21.1/1.21.1.jar"));
        candidates.add(Path.of(home, ".minecraft/versions/1.21.1/1.21.1.jar"));
        candidates.add(Path.of(home, "Library/Application Support/minecraft/versions/1.21.1/1.21.1.jar"));
        for (Path p : candidates) if (Files.isRegularFile(p)) return p;
        throw new IllegalStateException("Minecraft 1.21.1 jar not found; pass its path as the second argument. Tried: " + candidates);
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    /** A vanilla texture, e.g. {@code v("item/iron_sword")}. */
    static Img v(String path) throws IOException {
        ZipEntry e = vanilla.getEntry("assets/minecraft/textures/" + path + ".png");
        if (e == null) throw new IOException("No vanilla texture " + path);
        try (InputStream in = vanilla.getInputStream(e)) {
            BufferedImage b = ImageIO.read(in);
            Img img = new Img(b.getWidth(), b.getHeight());
            BufferedImage argb = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_ARGB);
            argb.getGraphics().drawImage(b, 0, 0, null);
            argb.getRGB(0, 0, img.w, img.h, img.px, 0, img.w);
            return img;
        }
    }

    /** Copies a vanilla file (e.g. an animation .mcmeta) unchanged. */
    static void copyVanilla(String path) throws IOException {
        ZipEntry e = vanilla.getEntry("assets/minecraft/textures/" + path);
        if (e == null) throw new IOException("No vanilla file " + path);
        Path file = tex.resolve(path);
        Files.createDirectories(file.getParent());
        try (InputStream in = vanilla.getInputStream(e)) {
            Files.copy(in, file);
        }
    }

    /** Writes {@code img} to {@code textures/<path>.png}. */
    static void save(Img img, String path) throws IOException {
        Path file = tex.resolve(path + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(img.toBuffered(), "png", file.toFile());
        written++;
    }

    /** The common case: a vanilla texture with the aurora grade. */
    static void graded(String path) throws IOException {
        save(Grade.apply(v(path), ITEM_GRADE), path);
    }

    // =====================================================================================================
    // Images and colors
    // =====================================================================================================

    static final class Img {
        final int w, h;
        final int[] px;

        Img(int w, int h) {
            this.w = w;
            this.h = h;
            this.px = new int[w * h];
        }

        boolean in(int x, int y) {
            return x >= 0 && y >= 0 && x < w && y < h;
        }

        int get(int x, int y) {
            return in(x, y) ? px[y * w + x] : 0;
        }

        void set(int x, int y, int c) {
            if (in(x, y)) px[y * w + x] = c;
        }

        void over(int x, int y, int c) {
            if (in(x, y)) px[y * w + x] = Col.over(px[y * w + x], c);
        }

        void rect(int x, int y, int rw, int rh, int c) {
            for (int yy = y; yy < y + rh; yy++) for (int xx = x; xx < x + rw; xx++) set(xx, yy, c);
        }

        void draw(Img src, int dx, int dy) {
            for (int y = 0; y < src.h; y++) for (int x = 0; x < src.w; x++) over(dx + x, dy + y, src.get(x, y));
        }

        Img copy() {
            Img c = new Img(w, h);
            System.arraycopy(px, 0, c.px, 0, px.length);
            return c;
        }

        BufferedImage toBuffered() {
            BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            b.setRGB(0, 0, w, h, px, 0, w);
            return b;
        }
    }

    static final class Col {
        private Col() {
        }

        static int a(int c) {
            return c >>> 24;
        }

        static int r(int c) {
            return (c >> 16) & 255;
        }

        static int g(int c) {
            return (c >> 8) & 255;
        }

        static int b(int c) {
            return c & 255;
        }

        static int clamp(int v) {
            return Math.max(0, Math.min(255, v));
        }

        static int argb(int a, int r, int g, int b) {
            return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
        }

        static int alpha(int c, int alpha) {
            return (clamp(alpha) << 24) | (c & 0xFFFFFF);
        }

        static int mix(int x, int y, double t) {
            return argb((int) Math.round(a(x) + (a(y) - a(x)) * t), (int) Math.round(r(x) + (r(y) - r(x)) * t),
                (int) Math.round(g(x) + (g(y) - g(x)) * t), (int) Math.round(b(x) + (b(y) - b(x)) * t));
        }

        static int over(int dst, int src) {
            int sa = a(src);
            if (sa == 255) return src;
            if (sa == 0) return dst;
            int da = a(dst);
            double s = sa / 255.0, d = da / 255.0 * (1 - s), out = s + d;
            if (out <= 0) return 0;
            return argb((int) Math.round(out * 255), (int) Math.round((r(src) * s + r(dst) * d) / out),
                (int) Math.round((g(src) * s + g(dst) * d) / out), (int) Math.round((b(src) * s + b(dst) * d) / out));
        }

        static double lum(int c) {
            return (0.299 * r(c) + 0.587 * g(c) + 0.114 * b(c)) / 255.0;
        }
    }

    /**
     * The aurora color grade. Shadows get a violet cast, highlights a cool teal-white cast, and saturation rises a
     * little. Luminance is kept, so textures stay as readable as vanilla.
     */
    static final class Grade {
        private Grade() {
        }

        static final double[] VIOLET = {0.62, 0.40, 1.00};
        static final double[] TEAL = {0.50, 1.00, 0.95};

        static int color(int c, double strength) {
            int a = Col.a(c);
            if (a == 0) return c;
            double r = Col.r(c) / 255.0, g = Col.g(c) / 255.0, b = Col.b(c) / 255.0;
            double l = 0.299 * r + 0.587 * g + 0.114 * b;
            double sat = 1 + 0.12 * strength;
            r = l + (r - l) * sat;
            g = l + (g - l) * sat;
            b = l + (b - l) * sat;
            double sh = Math.pow(1 - l, 2) * 0.22 * strength, hi = Math.pow(l, 3) * 0.10 * strength;
            double vl = 0.299 * VIOLET[0] + 0.587 * VIOLET[1] + 0.114 * VIOLET[2];
            double tl = 0.299 * TEAL[0] + 0.587 * TEAL[1] + 0.114 * TEAL[2];
            r += (VIOLET[0] - vl) * sh + (TEAL[0] - tl) * hi;
            g += (VIOLET[1] - vl) * sh + (TEAL[1] - tl) * hi;
            b += (VIOLET[2] - vl) * sh + (TEAL[2] - tl) * hi;
            return Col.argb(a, (int) Math.round(r * 255), (int) Math.round(g * 255), (int) Math.round(b * 255));
        }

        static Img apply(Img img, double strength) {
            Img out = img.copy();
            for (int i = 0; i < out.px.length; i++) out.px[i] = color(out.px[i], strength);
            return out;
        }

        /** Pulls every opaque pixel {@code amount} of the way to the texture's average color (calmer noise). */
        static Img calm(Img img, double amount) {
            long r = 0, g = 0, b = 0, n = 0;
            for (int c : img.px) {
                if (Col.a(c) == 0) continue;
                r += Col.r(c);
                g += Col.g(c);
                b += Col.b(c);
                n++;
            }
            if (n == 0) return img.copy();
            int mean = Col.argb(255, (int) (r / n), (int) (g / n), (int) (b / n));
            Img out = img.copy();
            for (int i = 0; i < out.px.length; i++) {
                int c = out.px[i];
                if (Col.a(c) == 0) continue;
                out.px[i] = Col.alpha(Col.mix(c, mean, amount), Col.a(c));
            }
            return out;
        }
    }

    // =====================================================================================================
    // Group 1: weapons and tools
    // =====================================================================================================

    /** Swords (short), pickaxes, axes, shears, bows, crossbows, arrows, fishing rods and sticks. */
    static final class Weapons {
        private Weapons() {
        }

        static final String[] TIERS = {"wooden", "stone", "iron", "golden", "diamond", "netherite"};

        static void generate() throws IOException {
            for (String t : TIERS) {
                save(Grade.apply(shortSword(v("item/" + t + "_sword")), ITEM_GRADE), "item/" + t + "_sword");
                graded("item/" + t + "_pickaxe");
                graded("item/" + t + "_axe");
            }
            for (String s : new String[]{"shears", "bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2", "crossbow_standby",
                "crossbow_pulling_0", "crossbow_pulling_1", "crossbow_pulling_2", "crossbow_arrow", "crossbow_firework", "arrow",
                "spectral_arrow", "fishing_rod", "fishing_rod_cast", "stick"}) {
                graded("item/" + s);
            }
        }

        /**
         * Shortens a vanilla sword: the blade lies on the diagonal, so pixels with x - y in [5, 11) are removed and
         * everything beyond (the rest of the blade and the tip) moves 3 px down-left to close the gap. The blade ends
         * 3 px earlier on each axis, about a quarter shorter.
         */
        static Img shortSword(Img src) {
            final int cut = 5, k = 3;
            Img out = new Img(src.w, src.h);
            for (int y = 0; y < src.h; y++) {
                for (int x = 0; x < src.w; x++) {
                    int c = src.get(x, y);
                    if (Col.a(c) == 0) continue;
                    int j = x - y;
                    if (j >= cut && j < cut + 2 * k) continue;
                    if (j >= cut + 2 * k) out.set(x - k, y + k, c);
                    else out.set(x, y, c);
                }
            }
            return out;
        }
    }
    // =====================================================================================================
    // Group 2: armor
    // =====================================================================================================

    /**
     * Armor icons and worn armor for leather, chainmail, iron, gold, diamond and netherite. Leather's base textures
     * are copied unchanged because the game tints them with the dye color (team colors in Bedwars); its untinted
     * overlays get the grade like everything else.
     */
    static final class Armor {
        private Armor() {
        }

        static void generate() throws IOException {
            String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
            for (String m : new String[]{"leather", "chainmail", "iron", "golden", "diamond", "netherite"}) {
                for (String p : pieces) {
                    if (m.equals("leather")) {
                        save(v("item/leather_" + p), "item/leather_" + p);
                        graded("item/leather_" + p + "_overlay");
                    } else {
                        graded("item/" + m + "_" + p);
                    }
                }
            }
            for (String m : new String[]{"chainmail", "iron", "gold", "diamond", "netherite"}) {
                graded("models/armor/" + m + "_layer_1");
                graded("models/armor/" + m + "_layer_2");
            }
            for (String l : new String[]{"1", "2"}) {
                save(v("models/armor/leather_layer_" + l), "models/armor/leather_layer_" + l);
                graded("models/armor/leather_layer_" + l + "_overlay");
            }
        }
    }
    // =====================================================================================================
    // Block design: tileable height maps, one light, eight-step ramps
    // =====================================================================================================

    /**
     * Blocks are designed here rather than edited from vanilla. A block is a height map (wrapping at the edges so it
     * tiles) lit from the top-left; the brightness picks one of seven shades from the block's ramp, so surfaces are
     * smooth and clean. Most blocks are framed by a one-pixel bevel so block edges stay visible while bridging.
     */
    static final class Blocks {
        private Blocks() {
        }

        static final double LX, LY, LZ;

        static {
            double l = Math.sqrt(1 + 1.15 * 1.15 + 1.35 * 1.35);
            LX = -1 / l;
            LY = -1.15 / l;
            LZ = 1.35 / l;
        }

        /**
         * A ramp of 8 colors around {@code base} (index 4): 0 is a deep outline tone, 1-3 shadows drifting towards
         * violet, 5-7 highlights blending towards a cool white.
         */
        static int[] ramp(int base) {
            float[] hsv = java.awt.Color.RGBtoHSB(Col.r(base), Col.g(base), Col.b(base), null);
            float h = hsv[0], sat = hsv[1], val = hsv[2];
            int[] r = new int[8];
            r[0] = hsvRgb(toward(h, 0.72f, 0.07f), Math.min(1, sat * 1.1f + 0.15f), val * 0.24f + 0.03f);
            float[] dv = {0.50f, 0.66f, 0.83f};
            for (int i = 0; i < 3; i++) r[1 + i] = hsvRgb(toward(h, 0.72f, 0.05f - i * 0.015f), Math.min(1, sat * (1.1f - i * 0.03f) + 0.04f), val * dv[i] + 0.02f);
            r[4] = 0xFF000000 | base;
            double[] tw = {0.16, 0.34, 0.56};
            for (int i = 0; i < 3; i++) r[5 + i] = Col.mix(0xFF000000 | base, 0xFFF0FBFF, tw[i]);
            return r;
        }

        static int hsvRgb(float h, float s, float v) {
            return 0xFF000000 | (java.awt.Color.HSBtoRGB(h - (float) Math.floor(h), Math.max(0, Math.min(1, s)), Math.max(0, Math.min(1, v))) & 0xFFFFFF);
        }

        static float toward(float h, float target, float amount) {
            float d = target - h;
            if (d > 0.5f) d -= 1;
            if (d < -0.5f) d += 1;
            return h + Math.max(-amount, Math.min(amount, d));
        }

        /** Shade 1..7 from a surface normal; {@code contrast} scales how much the slope matters. */
        static int level(double nx, double ny, double nz, double contrast, double offset) {
            double diff = nx * LX + ny * LY + nz * LZ;
            double v = 0.5 + (diff - LZ) * 1.6 * contrast + offset;
            return 1 + (int) Math.max(0, Math.min(6, Math.round(v * 6)));
        }

        /**
         * Lights a wrapping height map. {@code ramps[rampOf[i]]} colors pixel i; {@code offset[i]} brightens or
         * darkens it (patterns that are not geometry).
         */
        static Img light(int w, int h, double[] height, int[][] ramps, int[] rampOf, double[] offset, double contrast) {
            Img img = new Img(w, h);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double hl = height[y * w + Math.floorMod(x - 1, w)], hr = height[y * w + Math.floorMod(x + 1, w)];
                    double hu = height[Math.floorMod(y - 1, h) * w + x], hd = height[Math.floorMod(y + 1, h) * w + x];
                    double nx = -(hr - hl) / 2, ny = -(hd - hu) / 2, nz = 1, len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    int i = y * w + x;
                    int s = level(nx / len, ny / len, nz / len, contrast, offset == null ? 0 : offset[i]);
                    img.set(x, y, ramps[rampOf == null ? 0 : rampOf[i]][s]);
                }
            }
            return img;
        }

        static Img light(double[] height, int[] ramp, double[] offset, double contrast) {
            return light(16, 16, height, new int[][]{ramp}, null, offset, contrast);
        }

        /** Height map with a sunken one-pixel groove on the bottom and right edges (a framed tile). */
        static double[] framed(double depth) {
            double[] hgt = new double[256];
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) hgt[y * 16 + x] = (x == 15 || y == 15) ? -depth : 0;
            return hgt;
        }

        static double hash(int x, int y, int seed) {
            long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
            h = (h ^ (h >>> 13)) * 1274126177L;
            h ^= h >>> 16;
            return (h & 0xFFFFFF) / (double) 0x1000000;
        }

        /** Shortest signed distance on a 16 px torus. */
        static double wrap(double d) {
            while (d > 8) d -= 16;
            while (d < -8) d += 16;
            return d;
        }

        /** Sparse speckle offsets: a few pixels a step lighter or darker. */
        static double[] speckle(int seed, double light, double dark) {
            double[] o = new double[256];
            for (int i = 0; i < 256; i++) {
                double n = hash(i % 16, i / 16, seed);
                o[i] = n < light ? 0.17 : n > 1 - dark ? -0.17 : 0;
            }
            return o;
        }
    }

    // =====================================================================================================
    // Group 3: resources and Bedwars items
    // =====================================================================================================

    /**
     * Resources and shop items keep vanilla's art with the grade, so they are recognized at a glance. TNT, sponges
     * and the ladder are blocks and get Aurora designs.
     */
    static final class Items {
        private Items() {
        }

        static void generate() throws IOException {
            for (String s : new String[]{"iron_ingot", "gold_ingot", "diamond", "emerald", "netherite_ingot", "netherite_scrap",
                "iron_nugget", "gold_nugget", "fire_charge", "ender_pearl", "golden_apple", "snowball", "egg", "bucket",
                "water_bucket", "milk_bucket", "lava_bucket", "potion", "splash_potion", "lingering_potion", "glass_bottle",
                "experience_bottle", "totem_of_undying"}) {
                graded("item/" + s);
            }
            save(v("item/potion_overlay"), "item/potion_overlay"); // tinted by the game: keep it neutral
            for (int i = 0; i < 32; i++) graded(String.format("item/compass_%02d", i));
            save(tntSide(), "block/tnt_side");
            save(tntEnd(true), "block/tnt_top");
            save(tntEnd(false), "block/tnt_bottom");
            save(sponge(false), "block/sponge");
            save(sponge(true), "block/wet_sponge");
            save(ladder(), "block/ladder");
        }

        static final int[] TNT_RED = Blocks.ramp(0xC8282E);
        static final int[] NIGHT = Blocks.ramp(0x1C2033);
        static final int[] VIOLET = Blocks.ramp(0x8B5CF6);
        static final int[] PAPER = Blocks.ramp(0xE8EAF2);

        /** 3x5 letters. */
        static final String[] T = {"###", ".#.", ".#.", ".#.", ".#."};
        static final String[] N = {"#.#", "###", "###", "#.#", "#.#"};

        /**
         * Aurora TNT: three round dynamite sticks with a dark night-blue label wrapped around them, a violet rim on
         * the label and the letters in white.
         */
        static Img tntSide() {
            double[] hgt = new double[256];
            int[] rampOf = new int[256];
            double[] off = new double[256];
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int i = y * 16 + x;
                    double u = (x % 5 + 0.5) / 5.0 * 2 - 1; // across one stick, -1..1 (sticks are 5 px; the 16th column is a seam)
                    hgt[i] = x == 15 ? -1 : Math.sqrt(Math.max(0, 1 - u * u)) * 2.2;
                    if (y >= 5 && y <= 10) {
                        rampOf[i] = (y == 5 || y == 10) ? 2 : 1;
                        hgt[i] = 2.6; // the label sits on top of the sticks
                        if (y == 5) hgt[i] = 2.3;
                    }
                }
            }
            Img img = Blocks.light(16, 16, hgt, new int[][]{TNT_RED, NIGHT, VIOLET}, rampOf, off, 0.9);
            String[][] letters = {T, N, T};
            for (int l = 0; l < 3; l++) {
                for (int yy = 0; yy < 5; yy++) {
                    for (int xx = 0; xx < 3; xx++) {
                        if (letters[l][yy].charAt(xx) == '#') img.set(2 + l * 4 + xx, 6 + yy - 0 + (yy >= 4 ? 0 : 0), PAPER[yy == 0 ? 6 : 5]);
                    }
                }
            }
            return img;
        }

        /** Stick ends in a 2x2 bundle; the top has a fuse with a teal spark. */
        static Img tntEnd(boolean fuse) {
            double[] hgt = new double[256];
            int[] rampOf = new int[256];
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double dx = (x % 8) + 0.5 - 4, dy = (y % 8) + 0.5 - 4, d = Math.hypot(dx, dy);
                    hgt[y * 16 + x] = d <= 3.4 ? (d <= 1.4 ? 0.6 : 1.4) : -0.8;
                    rampOf[y * 16 + x] = d <= 3.4 ? 0 : 1;
                }
            }
            Img img = Blocks.light(16, 16, hgt, new int[][]{TNT_RED, NIGHT}, rampOf, null, 0.9);
            if (fuse) {
                int[] wick = Blocks.ramp(0x2A2A33);
                img.set(7, 7, wick[3]);
                img.set(8, 7, wick[4]);
                img.set(7, 8, wick[2]);
                img.set(8, 8, wick[3]);
                img.set(8, 6, 0xFF7CF5E8); // spark
                img.set(9, 5, 0xFFCFFFF8);
            }
            return img;
        }

        /**
         * Smooth sponge with neat 2x2 pores on an offset grid: each pore is shaded inside its top-left and has a lit
         * lower-right rim. The wet sponge is darker and olive, with glossy droplets.
         */
        static Img sponge(boolean wet) {
            int[] r = Blocks.ramp(wet ? 0xA9A23A : 0xE3C54A);
            Img img = new Img(16, 16);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double n = Blocks.hash(x, y, wet ? 9 : 8);
                    img.set(x, y, r[n < 0.08 ? 5 : n > 0.95 ? 3 : 4]);
                }
            }
            for (int py = 0; py < 4; py++) {
                for (int px = 0; px < 4; px++) {
                    int x0 = px * 4 + (py % 2 == 0 ? 0 : 2) + 1, y0 = py * 4 + 1;
                    img.set(x0 % 16, y0, r[1]);
                    img.set((x0 + 1) % 16, y0, r[2]);
                    img.set(x0 % 16, y0 + 1, r[2]);
                    img.set((x0 + 1) % 16, y0 + 1, r[3]);
                    img.set((x0 + 2) % 16, y0 + 1, r[6]); // lit rim
                    img.set((x0 + 1) % 16, y0 + 2, r[6]);
                    img.set((x0 + 2) % 16, y0 + 2, r[5]);
                }
            }
            if (wet) {
                img.set(6, 10, 0xFFE8FBFF);
                img.set(13, 6, 0xFFE8FBFF);
                img.set(2, 14, 0xFFD8F4FF);
            }
            return img;
        }

        /** Dark wooden rails with violet-steel brackets and four rungs. */
        static Img ladder() {
            Img img = new Img(16, 16);
            int[] wood = Blocks.ramp(0x7A5132), steel = Blocks.ramp(0x6D5BA8);
            for (int y = 0; y < 16; y++) {
                for (int x : new int[]{1, 2, 13, 14}) img.set(x, y, wood[x == 1 || x == 13 ? 5 : 3]);
                if (y % 4 == 1 || y % 4 == 2) {
                    for (int x = 3; x <= 12; x++) img.set(x, y, wood[y % 4 == 1 ? 5 : 2]);
                    img.set(2, y, steel[y % 4 == 1 ? 6 : 4]);
                    img.set(13, y, steel[y % 4 == 1 ? 5 : 3]);
                }
            }
            return img;
        }
    }
    // =====================================================================================================
    // Group 4: team blocks and beds
    // =====================================================================================================

    /**
     * Wool, concrete, terracotta, stained glass (and panes) and beds in all 16 colors, drawn from scratch. The team
     * colors are spread out so neighbours are easy to tell apart, and each material has its own surface: wool is a
     * soft quilted cushion, concrete a polished slab with a gloss streak, terracotta matte 2x2 tiles, glass a clean
     * pane with corner brackets.
     */
    static final class TeamBlocks {
        private TeamBlocks() {
        }

        static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        static final int[] RGB = {0xE9ECF2, 0xF2862A, 0xC94BD6, 0x5AB8F5, 0xF5CF2E, 0x7BD43A, 0xF28DBD, 0x585E6B,
            0xA0A6B1, 0x14A2A8, 0x7C43CF, 0x3354D1, 0x7A4E2E, 0x3F7A22, 0xD1312C, 0x1F2026};

        static void generate() throws IOException {
            for (int i = 0; i < COLORS.length; i++) {
                int[] r = Blocks.ramp(RGB[i]);
                save(wool(r), "block/" + COLORS[i] + "_wool");
                save(concrete(r, i), "block/" + COLORS[i] + "_concrete");
                save(terracotta(Blocks.ramp(muted(RGB[i])), i), "block/" + COLORS[i] + "_terracotta");
                save(glass(r, true), "block/" + COLORS[i] + "_stained_glass");
                save(paneTop(r), "block/" + COLORS[i] + "_stained_glass_pane_top");
                save(bed(r), "entity/bed/" + COLORS[i]);
            }
            save(terracotta(Blocks.ramp(0x9A5B3E), 16), "block/terracotta");
            int[] clear = Blocks.ramp(0xBFE3F2);
            save(glass(clear, false), "block/glass");
            save(paneTop(clear), "block/glass_pane_top");
        }

        /** Terracotta colors: the team color pulled towards baked clay and slightly darker. */
        static int muted(int rgb) {
            return Col.mix(0xFF000000 | rgb, 0xFF9C6A50, 0.32) & 0xFFFFFF;
        }

        /** Distance from the pixel centre to the nearest block edge (0.5 on the border pixels). */
        static double edge(int x, int y) {
            return Math.min(Math.min(x + 0.5, y + 0.5), Math.min(15.5 - x, 15.5 - y));
        }

        /** A soft cushion: rounded edges, a dashed stitch line two pixels in and a faint diagonal sheen. */
        static Img wool(int[] r) {
            double[] hgt = new double[256], off = new double[256];
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int i = y * 16 + x;
                    double d = edge(x, y), rad = 3.0;
                    hgt[i] = d >= rad ? rad : Math.sqrt(rad * rad - (rad - d) * (rad - d));
                    boolean stitch = Math.round(d - 0.5) == 2 && (x + y) % 2 == 0;
                    if (stitch) off[i] -= 0.2;
                    int diag = x + y;
                    if (diag >= 7 && diag <= 9 && d > 2.5) off[i] += 0.1; // sheen
                    if (Blocks.hash(x, y, 31) < 0.05) off[i] += 0.12; // felt
                }
            }
            return Blocks.light(hgt, r, off, 0.75);
        }

        /** Polished slab: flat, a crisp bevel frame and a glossy streak near the top-left corner. */
        static Img concrete(int[] r, int seed) {
            double[] hgt = Blocks.framed(1.0), off = Blocks.speckle(200 + seed, 0.03, 0.03);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int diag = x + y;
                    if ((diag == 5 || diag == 6) && x >= 2 && y >= 2) off[y * 16 + x] += 0.34;
                    if (diag == 8 && x >= 3 && y >= 3 && x <= 6) off[y * 16 + x] += 0.18;
                }
            }
            return Blocks.light(hgt, r, off, 1.0);
        }

        /** Matte terracotta: four 8x8 tiles with fine grooves and a little grain. */
        static Img terracotta(int[] r, int seed) {
            double[] hgt = new double[256], off = Blocks.speckle(300 + seed, 0.09, 0.07);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int lx = x % 8, ly = y % 8;
                    double d = Math.min(Math.min(lx + 0.5, ly + 0.5), Math.min(7.5 - lx, 7.5 - ly));
                    hgt[y * 16 + x] = (lx == 7 || ly == 7) ? -0.9 : Math.min(d, 1.6);
                }
            }
            return Blocks.light(hgt, r, off, 0.8);
        }

        /**
         * Clean glass: a thin frame lit on the top/left, bright corner brackets and two diagonal sheen lines.
         * Stained glass fills the middle with a translucent tint.
         */
        static Img glass(int[] r, boolean stained) {
            Img img = new Img(16, 16);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    boolean border = x == 0 || y == 0 || x == 15 || y == 15;
                    int c;
                    if (border) c = Col.alpha(r[(x == 0 || y == 0) ? 5 : 3], 215);
                    else c = stained ? Col.alpha(r[4], 96) : 0;
                    img.set(x, y, c);
                }
            }
            int bracket = Col.alpha(r[7], 255);
            for (int i = 0; i < 4; i++) {
                img.set(i, 0, bracket);
                img.set(0, i, bracket);
                img.set(15 - i, 15, Col.alpha(r[5], 255));
                img.set(15, 15 - i, Col.alpha(r[5], 255));
            }
            int sheen = stained ? 0x70FFFFFF : 0x96FFFFFF;
            for (int i = 0; i < 6; i++) img.set(3 + i, 8 - i, sheen);
            for (int i = 0; i < 3; i++) img.set(6 + i, 9 - i, sheen);
            return img;
        }

        static Img paneTop(int[] r) {
            Img img = new Img(16, 16);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) img.set(x, y, Col.alpha(r[x % 2 == 0 ? 4 : 5], 225));
            return img;
        }

        // ------------------------------------------------------------------ beds

        static final int[] FRAME = Blocks.ramp(0x3E3458);
        static final int[] SHEET = Blocks.ramp(0xE6E9F1);
        static final int[] PIPING = Blocks.ramp(0x8B5CF6);

        /**
         * 64x64 bed on the vanilla model layout: the head piece has a white pillow with violet piping on the sheet,
         * then the blanket; the foot piece is covered by the blanket, which folds over the foot end. Frame,
         * underside and legs are dark night-violet wood. The blanket is quilted in diamonds with a soft sheen.
         */
        static Img bed(int[] b) {
            Img img = new Img(64, 64);
            // Head end (6..21, 0..5): frame, sheet edge, pillow side.
            for (int y = 0; y < 6; y++) {
                for (int x = 6; x < 22; x++) {
                    int c = y < 2 ? FRAME[y == 0 ? 3 : 4] : y == 2 ? SHEET[3] : y == 3 ? PIPING[5] : SHEET[y == 4 ? 6 : 5];
                    img.set(x, y, c);
                }
            }
            // Head top (6..21, 6..21) with the side faces (0..5 and 22..27) continuing it.
            for (int y = 6; y < 22; y++) {
                for (int x = 0; x < 28; x++) {
                    int side = x < 6 ? x : x >= 22 ? 27 - x : -1;
                    int c;
                    if (side >= 0 && side < 1) c = FRAME[4];
                    else if (side >= 0 && side < 3) c = SHEET[side == 1 ? 3 : 4];
                    else if (y <= 12) c = pillow(x, y, side);
                    else if (y <= 14) c = SHEET[y == 13 ? 6 : 4];
                    else c = quilt(b, x, y, y == 15);
                    img.set(x, y, c);
                }
            }
            // Foot end (22..37, 22..27): frame, sheet edge, blanket folded over.
            for (int y = 22; y < 28; y++) {
                for (int x = 22; x < 38; x++) {
                    int c = y < 24 ? FRAME[y == 22 ? 3 : 4] : y == 24 ? SHEET[4] : b[y == 25 ? 5 : y == 27 ? 2 : 3];
                    img.set(x, y, c);
                }
            }
            // Foot top (6..21, 28..43) and sides.
            for (int y = 28; y < 44; y++) {
                for (int x = 0; x < 28; x++) {
                    int side = x < 6 ? x : x >= 22 ? 27 - x : -1;
                    int c;
                    if (side >= 0 && side < 2) c = FRAME[side == 0 ? 3 : 4];
                    else if (side == 2) c = SHEET[4];
                    else c = quilt(b, x, y, false);
                    if (y == 43 && side < 0) c = b[2]; // hem
                    img.set(x, y, c);
                }
            }
            // Undersides and legs.
            for (int y = 6; y < 22; y++) for (int x = 28; x < 44; x++) img.set(x, y, plank(x, y));
            for (int y = 28; y < 44; y++) for (int x = 28; x < 44; x++) img.set(x, y, plank(x, y));
            for (int y = 0; y < 24; y++) for (int x = 50; x < 62; x++) img.set(x, y, FRAME[(x - 50) % 3 == 0 ? 4 : y % 6 == 5 ? 2 : 3]);
            return img;
        }

        static int pillow(int x, int y, int side) {
            if (side >= 0) return y == 6 ? SHEET[5] : SHEET[side == 3 ? 4 : 5];
            int px = x - 6;
            boolean inPillow = px >= 1 && px <= 14 && y >= 7 && y <= 12;
            if (!inPillow) return SHEET[4];
            boolean rim = px == 1 || px == 14 || y == 7 || y == 12;
            if (rim) return PIPING[y == 7 || px == 1 ? 5 : 3];
            return SHEET[y == 8 ? 7 : y == 11 ? 5 : 6];
        }

        /** Diamond quilting: darker stitch lines on the diagonals, lit pixels just above them, a fold on top. */
        static int quilt(int[] b, int x, int y, boolean fold) {
            if (fold) return b[6];
            int u = Math.floorMod(x + y, 6), v = Math.floorMod(x - y, 6);
            if (u == 0 || v == 0) return b[3];
            if (u == 1 || v == 1) return b[5];
            return b[4];
        }

        static int plank(int x, int y) {
            int ly = y % 4;
            if (ly == 3) return FRAME[2];
            if (ly == 0) return FRAME[4];
            return FRAME[Blocks.hash(x / 3, y, 404) < 0.2 ? 2 : 3];
        }
    }
}
