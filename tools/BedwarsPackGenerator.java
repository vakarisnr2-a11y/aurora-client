import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
}
