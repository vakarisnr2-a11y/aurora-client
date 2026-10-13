import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates the built-in "Aurora Bedwars" resource pack: clean, sharp 32x32 PvP textures.
 *
 * <pre>
 * java tools/BedwarsPackGenerator.java src/main/resources/resourcepacks/aurora_bedwars
 * </pre>
 *
 * <p>Everything is drawn in code so the pack can be regenerated and tweaked. Items are built from shapes on a
 * {@link Canvas}: each shape is filled with a material (a six-step color ramp), then every material gets the same
 * top-left bevel (light edges up and left, shaded edges down and right) and a dark outline around the sprite. That
 * keeps the lighting identical on every texture. Blocks are tileable 32x32 patterns with the same light direction.</p>
 */
public class BedwarsPackGenerator {
    static Path root;
    static Path tex;
    static int written;

    public static void main(String[] args) throws IOException {
        root = Path.of(args.length > 0 ? args[0] : "src/main/resources/resourcepacks/aurora_bedwars");
        tex = root.resolve("assets/minecraft/textures");
        Weapons.generate();
        Legacy16.generate();
        System.out.println("Generated " + written + " textures in " + root.toAbsolutePath());
    }

    /** Writes {@code img} to {@code textures/<path>.png}. */
    static void save(Img img, String path) throws IOException {
        Path file = tex.resolve(path + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(img.toBuffered(), "png", file.toFile());
        written++;
    }

    static void saveRaw(BufferedImage img, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        ImageIO.write(img, "png", file.toFile());
        written++;
    }

    // =====================================================================================================
    // Colors
    // =====================================================================================================

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

        static int argb(int a, int r, int g, int b) {
            return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
        }

        static int clamp(int v) {
            return Math.max(0, Math.min(255, v));
        }

        static int opaque(int rgb) {
            return 0xFF000000 | rgb;
        }

        static int alpha(int c, int alpha) {
            return (clamp(alpha) << 24) | (c & 0xFFFFFF);
        }

        static int mix(int x, int y, double t) {
            return argb((int) Math.round(a(x) + (a(y) - a(x)) * t), (int) Math.round(r(x) + (r(y) - r(x)) * t),
                (int) Math.round(g(x) + (g(y) - g(x)) * t), (int) Math.round(b(x) + (b(y) - b(x)) * t));
        }

        /** Multiplies the color channels by {@code f} (keeps alpha). */
        static int scale(int c, double f) {
            return argb(a(c), (int) Math.round(r(c) * f), (int) Math.round(g(c) * f), (int) Math.round(b(c) * f));
        }

        /** Source-over compositing. */
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

        static float[] hsv(int c) {
            float[] out = new float[3];
            java.awt.Color.RGBtoHSB(r(c), g(c), b(c), out);
            return out;
        }

        static int fromHsv(float h, float s, float v, int alpha) {
            int rgb = java.awt.Color.HSBtoRGB(h - (float) Math.floor(h), Math.max(0, Math.min(1, s)), Math.max(0, Math.min(1, v)));
            return alpha(rgb, alpha);
        }

        /** Moves hue {@code h} (0..1) up to {@code amount} towards {@code target}. */
        static float hueToward(float h, float target, float amount) {
            float d = target - h;
            if (d > 0.5f) d -= 1;
            if (d < -0.5f) d += 1;
            return h + Math.max(-amount, Math.min(amount, d));
        }

        /**
         * A six-step ramp from one base color: outline, dark, mid-dark, base, light, highlight. Shadows drift towards
         * blue and highlights towards warm yellow, which reads better than plain darkening.
         */
        static int[] ramp(int base) {
            float[] hsv = hsv(base);
            float h = hsv[0], s = hsv[1], v = hsv[2];
            float cold = 0.68f, warm = 0.14f;
            return new int[]{
                fromHsv(hueToward(h, cold, 0.035f), Math.min(1, s * 1.1f + 0.08f), v * 0.30f, 255),
                fromHsv(hueToward(h, cold, 0.025f), Math.min(1, s * 1.08f + 0.04f), v * 0.56f, 255),
                fromHsv(hueToward(h, cold, 0.012f), Math.min(1, s * 1.04f), v * 0.78f, 255),
                opaque(base & 0xFFFFFF),
                fromHsv(hueToward(h, warm, 0.015f), s * 0.88f, Math.min(1, v * 1.14f + 0.04f), 255),
                fromHsv(hueToward(h, warm, 0.03f), s * 0.62f, Math.min(1, v * 1.28f + 0.1f), 255),
            };
        }

        /** A ramp from six explicit RGB colors (outline .. highlight). */
        static int[] ramp(int o, int d, int md, int b, int l, int hi) {
            return new int[]{opaque(o), opaque(d), opaque(md), opaque(b), opaque(l), opaque(hi)};
        }

        /** The same ramp one step darker (for guards, back faces). */
        static int[] darker(int[] r) {
            return new int[]{r[0], r[0], r[1], r[2], r[3], r[4]};
        }
    }

    // =====================================================================================================
    // Images and canvases
    // =====================================================================================================

    /** An ARGB image with pixel helpers. */
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

    /** Shade steps of a ramp. */
    static final int OUT = 0, DARK = 1, MID = 2, BASE = 3, LIGHT = 4, HIGH = 5;

    interface Pred {
        boolean test(int x, int y);
    }

    /**
     * Pixel art built from materials. Each pixel holds a material and a shade step. {@link #fill} paints shapes with
     * the base shade; {@link #bevel} then lights edges facing up/left and shades edges facing down/right for every
     * material; {@link #outline} surrounds the sprite with each material's darkest color. Pixels painted with
     * {@link #paint} keep their explicit shade.
     */
    static final class Canvas {
        final int w, h;
        final int[] mat, shade;
        final boolean[] locked;
        final List<int[]> ramps = new ArrayList<>();

        Canvas(int w, int h) {
            this.w = w;
            this.h = h;
            mat = new int[w * h];
            shade = new int[w * h];
            locked = new boolean[w * h];
            java.util.Arrays.fill(mat, -1);
        }

        int mat(int[] ramp) {
            ramps.add(ramp);
            return ramps.size() - 1;
        }

        int m(int x, int y) {
            return x < 0 || y < 0 || x >= w || y >= h ? -1 : mat[y * w + x];
        }

        int s(int x, int y) {
            return x < 0 || y < 0 || x >= w || y >= h ? -1 : shade[y * w + x];
        }

        void put(int x, int y, int m) {
            if (x < 0 || y < 0 || x >= w || y >= h) return;
            int i = y * w + x;
            mat[i] = m;
            shade[i] = BASE;
            locked[i] = false;
        }

        void paint(int x, int y, int m, int s) {
            if (x < 0 || y < 0 || x >= w || y >= h) return;
            int i = y * w + x;
            mat[i] = m;
            shade[i] = s;
            locked[i] = true;
        }

        /** Changes the shade of an existing pixel (no-op on empty pixels). */
        void tone(int x, int y, int s) {
            if (m(x, y) < 0) return;
            shade[y * w + x] = s;
            locked[y * w + x] = true;
        }

        void erase(int x, int y) {
            if (x < 0 || y < 0 || x >= w || y >= h) return;
            mat[y * w + x] = -1;
            locked[y * w + x] = false;
        }

        void fill(Pred p, int m) {
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (p.test(x, y)) put(x, y, m);
        }

        void fill(Pred p, int m, int s) {
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (p.test(x, y)) paint(x, y, m, s);
        }

        /** A filled disc (pixel centers within {@code r} of (cx, cy)). */
        void disc(double cx, double cy, double r, int m) {
            fill((x, y) -> sq(x + 0.5 - cx) + sq(y + 0.5 - cy) <= r * r, m);
        }

        /** A thick segment (pixel centers within {@code r} of the segment). */
        void seg(double ax, double ay, double bx, double by, double r, int m) {
            fill((x, y) -> segDist(x + 0.5, y + 0.5, ax, ay, bx, by) <= r, m);
        }

        /** A 1 px line with a fixed shade (Bresenham between pixel coordinates). */
        void line(int x0, int y0, int x1, int y1, int m, int s) {
            int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
            while (true) {
                paint(x0, y0, m, s);
                if (x0 == x1 && y0 == y1) break;
                int e2 = 2 * err;
                if (e2 >= dy) {
                    err += dy;
                    x0 += sx;
                }
                if (e2 <= dx) {
                    err += dx;
                    y0 += sy;
                }
            }
        }

        /** Lights edges facing up/left and shades edges facing down/right, per material. */
        void bevel() {
            int[] out = shade.clone();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x, m = mat[i];
                    if (m < 0 || locked[i]) continue;
                    boolean u = m(x, y - 1) != m, l = m(x - 1, y) != m, d = m(x, y + 1) != m, r = m(x + 1, y) != m;
                    boolean lit = u || l, dark = d || r;
                    if (lit && !dark) out[i] = u && l ? HIGH : LIGHT;
                    else if (dark && !lit) out[i] = d && r ? DARK : MID;
                    else out[i] = BASE;
                }
            }
            System.arraycopy(out, 0, shade, 0, out.length);
        }

        /** Surrounds the sprite with a 1 px outline in the darkest color of the touching material. */
        void outline() {
            int[] m2 = mat.clone();
            int[] s2 = shade.clone();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (mat[y * w + x] >= 0) continue;
                    int best = -1;
                    int[][] n = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
                    for (int[] d : n) {
                        int nm = m(x + d[0], y + d[1]);
                        if (nm < 0) continue;
                        if (best < 0 || lum(ramps.get(nm)[OUT]) < lum(ramps.get(best)[OUT])) best = nm;
                    }
                    if (best >= 0) {
                        m2[y * w + x] = best;
                        s2[y * w + x] = OUT;
                    }
                }
            }
            System.arraycopy(m2, 0, mat, 0, mat.length);
            System.arraycopy(s2, 0, shade, 0, shade.length);
        }

        Img img() {
            Img img = new Img(w, h);
            for (int i = 0; i < mat.length; i++) if (mat[i] >= 0) img.px[i] = ramps.get(mat[i])[shade[i]];
            return img;
        }

        /** Bevel, outline and render. */
        Img finish() {
            bevel();
            outline();
            return img();
        }
    }

    static double sq(double v) {
        return v * v;
    }

    static double lum(int c) {
        return 0.299 * Col.r(c) + 0.587 * Col.g(c) + 0.114 * Col.b(c);
    }

    static double segDist(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay, len = dx * dx + dy * dy;
        double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    /** Deterministic hash noise in [0, 1). */
    static double hash(int x, int y, int seed) {
        long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    // =====================================================================================================
    // Palette
    // =====================================================================================================

    static final class Pal {
        private Pal() {
        }

        // Tool and weapon materials: outline, dark, mid-dark, base, light, highlight.
        static final int[] WOOD = Col.ramp(0x2E1B0C, 0x6B4423, 0x8F5F33, 0xB07B45, 0xCC9A5E, 0xE6BF85);
        static final int[] STONE = Col.ramp(0x1D1E23, 0x4D4F57, 0x696B74, 0x878A93, 0xA8AAB2, 0xCACCD2);
        static final int[] IRON = Col.ramp(0x262A33, 0x737B89, 0x9CA4B1, 0xC4CAD4, 0xE4E8EE, 0xFFFFFF);
        static final int[] GOLD = Col.ramp(0x3D2304, 0x9E5F0B, 0xD38F12, 0xF2BE2C, 0xFFDF5E, 0xFFF4BD);
        static final int[] DIAMOND = Col.ramp(0x072A30, 0x127B84, 0x1BAFB8, 0x39DCDC, 0x8BF5EE, 0xE0FFFB);
        static final int[] NETHERITE = Col.ramp(0x120F16, 0x29232E, 0x3B3341, 0x4F4555, 0x6C6072, 0x93889A);
        /** Tool handles. */
        static final int[] HANDLE = Col.ramp(0x24160C, 0x4A2F1B, 0x63401F, 0x7E542C, 0x9A6B3B, 0xB8874F);
        /** Leather grip wrap on swords. */
        static final int[] GRIP = Col.ramp(0x150D08, 0x2E1D12, 0x3F281A, 0x553724, 0x6E4A31, 0x8A6042);
        static final int[] STRING = Col.ramp(0x2C2C32, 0x8C8C94, 0xB4B4BC, 0xD6D6DC, 0xEEEEF2, 0xFFFFFF);
        static final int[] FEATHER = Col.ramp(0x34343B, 0xA9A9B2, 0xCFCFD6, 0xEDEDF2, 0xFFFFFF, 0xFFFFFF);
        static final int[] RED = Col.ramp(0x3D0A0E, 0x7E1A1F, 0xA82A2E, 0xD23F3F, 0xEE6A62, 0xFF9E92);
        static final int[] FLINT = Col.ramp(0x18181C, 0x323238, 0x46464E, 0x5C5C66, 0x7A7A86, 0x9C9CA8);
    }

    // =====================================================================================================
    // Group 1: weapons and tools
    // =====================================================================================================

    /**
     * Swords, pickaxes, axes, shears, bows, crossbows, arrows, fishing rods and sticks. Long items lie on the
     * anti-diagonal like vanilla: handle bottom-left, tip top-right. Swords are short (the blade ends well before the
     * corner) so they cover less of the screen in first person.
     */
    static final class Weapons {
        private Weapons() {
        }

        /** Item axis: pixels with x + y = 31 run from the bottom-left to the top-right corner. */
        static final int K0 = 31;

        static void generate() throws IOException {
            String[] tiers = {"wooden", "stone", "iron", "golden", "diamond", "netherite"};
            int[][] ramps = {Pal.WOOD, Pal.STONE, Pal.IRON, Pal.GOLD, Pal.DIAMOND, Pal.NETHERITE};
            for (int i = 0; i < tiers.length; i++) {
                save(sword(ramps[i], i), "item/" + tiers[i] + "_sword");
                save(pickaxe(ramps[i]), "item/" + tiers[i] + "_pickaxe");
                save(axe(ramps[i]), "item/" + tiers[i] + "_axe");
            }
            save(shears(), "item/shears");
            save(bow(-1), "item/bow");
            for (int s = 0; s < 3; s++) save(bow(s), "item/bow_pulling_" + s);
            save(crossbow(-1, null), "item/crossbow_standby");
            for (int s = 0; s < 3; s++) save(crossbow(s, null), "item/crossbow_pulling_" + s);
            save(crossbow(3, "arrow"), "item/crossbow_arrow");
            save(crossbow(3, "firework"), "item/crossbow_firework");
            save(arrow(), "item/arrow");
            save(fishingRod(true), "item/fishing_rod");
            save(fishingRod(false), "item/fishing_rod_cast");
            save(stick(), "item/stick");
        }

        /**
         * A short sword. In axis coordinates j = x - y runs along the blade and dk = x + y - 31 across it, so the
         * blade is a band |dk| <= 2: upper-left half lit, lower-right half shaded, a bright cutting edge on top.
         * Guards differ a little per tier (wings on gold, diamond and netherite) so tiers are told apart by shape too.
         */
        static Img sword(int[] metal, int tier) {
            Canvas c = new Canvas(32, 32);
            int[] guardRamp = switch (tier) {
                case 0 -> Col.darker(Pal.WOOD);
                case 1 -> Col.darker(Pal.STONE);
                case 4 -> Col.ramp(0x0B2E33, 0x145D66, 0x1D7F88, 0x27A0A6, 0x4CC3C3, 0x93E6E0);
                case 5 -> Col.ramp(0x2B1606, 0x5A3410, 0x84501C, 0xA86A2A, 0xC98B44, 0xE5B26E);
                default -> Col.darker(metal);
            };
            int blade = c.mat(metal), guard = c.mat(guardRamp), grip = c.mat(Pal.GRIP);
            final int tip = 17, guard0 = -9, blade0 = -7, grip0 = -18;
            int wing = tier >= 3 ? 1 : 0;
            int guardHalf = tier <= 1 ? 4 : 5;
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    int j = x - y, dk = x + y - K0;
                    if (j >= blade0 && j <= tip) {
                        int rem = tip - j;
                        int hw = rem >= 5 ? 2 : rem >= 2 ? 1 : 0;
                        if (Math.abs(dk) <= hw) {
                            int s = dk <= -2 ? HIGH : dk == -1 ? LIGHT : dk == 0 ? BASE : dk == 1 ? MID : DARK;
                            if (dk == -hw && hw > 0) s = HIGH;
                            if (rem <= 1) s = LIGHT;
                            c.paint(x, y, blade, s);
                        }
                    }
                    int ad = Math.abs(dk);
                    int gj = guard0 + (ad >= guardHalf - 1 ? wing : 0);
                    if ((j == gj || j == gj + 1) && ad <= guardHalf) c.put(x, y, guard);
                    if (tier == 5 && ad == guardHalf && j == guard0 + 2) c.put(x, y, guard); // netherite spikes
                    if (j >= grip0 && j < guard0 && (dk == -1 || dk == 0)) {
                        c.paint(x, y, grip, ((j - grip0) / 2) % 2 == 0 ? LIGHT : MID);
                    }
                }
            }
            // Fuller: a short groove down the middle of the blade.
            for (int j = blade0 + 2; j <= tip - 7; j++) {
                if (((j + K0) & 1) != 0) continue;
                int x = (j + K0) / 2, y = (K0 - j) / 2;
                c.tone(x, y, LIGHT);
            }
            // Pommel: a small knob at the end of the grip.
            int px = (grip0 - 3 + K0) / 2, py = (K0 - grip0 + 3) / 2;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) c.put(px + dx, py + dy, guard);
            return c.finish();
        }

        /** Handle along the axis from the bottom-left corner up to {@code top} (axis j). */
        static void handle(Canvas c, int m, int jFrom, int jTo) {
            c.fill((x, y) -> {
                int j = x - y, dk = x + y - K0;
                return j >= jFrom && j <= jTo && (dk == -1 || dk == 0);
            }, m);
        }

        static Img pickaxe(int[] head) {
            Canvas c = new Canvas(32, 32);
            int handle = c.mat(Pal.HANDLE), h = c.mat(head);
            handle(c, handle, -22, 13);
            c.fill((x, y) -> {
                int j = x - y, dk = x + y - K0;
                if (Math.abs(dk) > 11) return false;
                double jc = 17.0 - dk * dk / 17.0;
                double half = 2.7 - Math.abs(dk) / 11.0 * 1.1;
                return Math.abs(j - jc) <= half;
            }, h);
            return c.finish();
        }

        static Img axe(int[] head) {
            Canvas c = new Canvas(32, 32);
            int handle = c.mat(Pal.HANDLE), h = c.mat(head);
            handle(c, handle, -22, 13);
            c.fill((x, y) -> {
                int j = x - y, dk = x + y - K0;
                if (dk < -10 || dk > 3) return false;
                if (dk >= 1) return j >= 9 && j <= 12 && dk <= 2; // poll behind the handle
                int depth = -dk; // 0 at the handle .. 10 at the cutting edge
                double top = 11 + (depth <= 2 ? 2 : 2 + (depth - 2) * 0.75);
                double bottom = 11 - (depth <= 2 ? 2 : 2 + (depth - 2) * 1.0); // the beard reaches further down
                if (depth >= 9) {
                    top -= (depth - 8) * 1.6;
                    bottom += (depth - 8) * 1.6;
                }
                return j >= bottom && j <= top;
            }, h);
            c.bevel();
            // Sharpened edge: a bright band along the far side and a shaded bevel line just inside it.
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    if (c.m(x, y) != h) continue;
                    int dk = x + y - K0;
                    if (dk <= -8) c.tone(x, y, dk <= -9 ? HIGH : LIGHT);
                    else if (dk == -7 && c.s(x, y) == BASE) c.tone(x, y, MID);
                }
            }
            c.outline();
            return c.img();
        }

        static Img shears() {
            Canvas c = new Canvas(32, 32);
            int handle = c.mat(Pal.RED), blade = c.mat(Pal.IRON), rivet = c.mat(Pal.FLINT);
            double px = 15.5, py = 16.5;
            // Handles: two arms ending in separate rings.
            c.seg(px, py, 8.0, 19.5, 1.2, handle);
            c.seg(px, py, 12.0, 24.5, 1.2, handle);
            c.fill((x, y) -> ring(x, y, 5.5, 20.5, 3.0, 1.5), handle);
            c.fill((x, y) -> ring(x, y, 11.5, 27.0, 3.0, 1.5), handle);
            // Two blades, slightly open, tapering to points.
            c.fill((x, y) -> segDist(x + 0.5, y + 0.5, px, py, 27.5, 3.5) <= 1.8 - 1.1 * along(x, y, px, py, 27.5, 3.5), blade);
            c.fill((x, y) -> segDist(x + 0.5, y + 0.5, px, py, 28.5, 8.5) <= 1.6 - 1.0 * along(x, y, px, py, 28.5, 8.5), blade);
            c.paint((int) px, (int) py, rivet, LIGHT);
            c.paint((int) px - 1, (int) py + 1, rivet, MID);
            return c.finish();
        }

        static boolean ring(int x, int y, double cx, double cy, double outer, double inner) {
            double d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy);
            return d <= outer && d >= inner;
        }

        /** 0 at the segment start, 1 at its end, for the pixel centre's projection. */
        static double along(int x, int y, double ax, double ay, double bx, double by) {
            double dx = bx - ax, dy = by - ay;
            return Math.max(0, Math.min(1, ((x + 0.5 - ax) * dx + (y + 0.5 - ay) * dy) / (dx * dx + dy * dy)));
        }

        /**
         * Bow arc bulging towards the top-left, string between the tips. {@code stage} -1 is the bow at rest;
         * 0..2 pull the string towards the bottom-right with an arrow nocked.
         */
        static Img bow(int stage) {
            Canvas c = new Canvas(32, 32);
            int wood = c.mat(Pal.HANDLE), grip = c.mat(Pal.GRIP), tipM = c.mat(Pal.IRON), string = c.mat(Pal.STRING);
            double t1x = 28.0, t1y = 3.5, t2x = 3.5, t2y = 28.0, sag = 10.5;
            arc(c, t1x, t1y, t2x, t2y, sag, wood, grip, 1.05, 0.55);
            c.put((int) t1x - 1, (int) t1y, tipM);
            c.put((int) t2x, (int) t2y - 1, tipM);
            double pull = stage < 0 ? 0 : new double[]{3.0, 5.5, 8.0}[stage];
            double nx = (t1x + t2x) / 2 + pull / Math.sqrt(2), ny = (t1y + t2y) / 2 + pull / Math.sqrt(2);
            int sx1 = (int) Math.floor(t1x - 1.5), sy1 = (int) Math.floor(t1y + 1.5);
            int sx2 = (int) Math.floor(t2x + 1.5), sy2 = (int) Math.floor(t2y - 1.5);
            int nxi = (int) Math.floor(nx), nyi = (int) Math.floor(ny);
            c.line(sx1, sy1, nxi, nyi, string, stage < 0 ? MID : LIGHT);
            c.line(nxi, nyi, sx2, sy2, string, stage < 0 ? MID : LIGHT);
            if (stage >= 0) nockedArrow(c, nx, ny, 22.5);
            return c.finish();
        }

        /** An arc from (ax, ay) to (bx, by) bulging towards the top-left by {@code sag}; the middle 16% is the grip. */
        static void arc(Canvas c, double ax, double ay, double bx, double by, double sag, int wood, int grip, double r0, double rMid) {
            double nxv = -1 / Math.sqrt(2), nyv = -1 / Math.sqrt(2);
            for (int i = 0; i <= 400; i++) {
                double t = i / 400.0;
                double s = Math.sin(Math.PI * t);
                double x = ax + (bx - ax) * t + nxv * sag * s, y = ay + (by - ay) * t + nyv * sag * s;
                double r = r0 + rMid * s;
                c.disc(x, y, r, t > 0.42 && t < 0.58 ? grip : wood);
            }
        }

        /** Arrow from the nock (on the string) towards the top-left. */
        static void nockedArrow(Canvas c, double nx, double ny, double length) {
            int shaft = c.mat(Pal.WOOD), head = c.mat(Pal.FLINT), feather = c.mat(Pal.FEATHER);
            double ux = -1 / Math.sqrt(2), uy = -1 / Math.sqrt(2);
            double hx = nx + ux * length, hy = ny + uy * length;
            for (int i = 0; i <= 200; i++) {
                double t = i / 200.0;
                int x = (int) Math.floor(nx + (hx - nx) * t), y = (int) Math.floor(ny + (hy - ny) * t);
                c.paint(x, y, shaft, t < 0.5 ? BASE : LIGHT);
            }
            // Fletching: two feathers either side of the shaft near the nock.
            for (int i = 1; i <= 4; i++) {
                int x = (int) Math.floor(nx + ux * i), y = (int) Math.floor(ny + uy * i);
                c.paint(x + 1, y, feather, i % 2 == 0 ? LIGHT : BASE);
                c.paint(x, y + 1, feather, i % 2 == 0 ? BASE : MID);
            }
            // Arrowhead.
            int x = (int) Math.floor(hx), y = (int) Math.floor(hy);
            c.paint(x, y, head, HIGH);
            c.paint(x + 1, y, head, LIGHT);
            c.paint(x, y + 1, head, LIGHT);
            c.paint(x + 2, y, head, BASE);
            c.paint(x, y + 2, head, BASE);
            c.paint(x + 1, y + 1, head, MID);
        }

        /**
         * Crossbow: prod (bow) on the top-left, stock along the main diagonal to the bottom-right. {@code stage} -1
         * at rest, 0..2 pulling, 3 loaded with {@code ammo} ("arrow" or "firework").
         */
        static Img crossbow(int stage, String ammo) {
            Canvas c = new Canvas(32, 32);
            int stock = c.mat(Pal.HANDLE), dark = c.mat(Col.darker(Pal.HANDLE)), metal = c.mat(Pal.IRON);
            int prod = c.mat(Pal.WOOD), string = c.mat(Pal.STRING);
            c.seg(8.0, 8.0, 27.5, 27.5, 1.9, stock);
            c.seg(23.5, 23.5, 28.0, 28.0, 2.1, dark); // butt
            c.seg(20.5, 24.5, 22.0, 26.5, 0.9, metal); // trigger
            double t1x = 23.5, t1y = 2.5, t2x = 2.5, t2y = 23.5;
            arc(c, t1x, t1y, t2x, t2y, 4.5, prod, prod, 1.25, 0.45);
            c.put((int) t1x, (int) t1y, metal);
            c.put((int) t2x, (int) t2y, metal);
            c.fill((x, y) -> Math.hypot(x + 0.5 - 9.5, y + 0.5 - 9.5) <= 1.6, metal); // prod clamp
            double pull = stage < 0 ? 0 : stage >= 3 ? 7.5 : new double[]{2.5, 5.0, 7.5}[stage];
            double mx = (t1x + t2x) / 2, my = (t1y + t2y) / 2;
            double nx = mx + pull / Math.sqrt(2), ny = my + pull / Math.sqrt(2);
            int nxi = (int) Math.floor(nx), nyi = (int) Math.floor(ny);
            c.line((int) t1x - 1, (int) t1y + 1, nxi, nyi, string, LIGHT);
            c.line(nxi, nyi, (int) t2x + 1, (int) t2y - 1, string, LIGHT);
            if ("arrow".equals(ammo)) nockedArrow(c, nx, ny, 20.5);
            if ("firework".equals(ammo)) firework(c, nx, ny);
            return c.finish();
        }

        static void firework(Canvas c, double nx, double ny) {
            int red = c.mat(Pal.RED), paper = c.mat(Pal.FEATHER), cone = c.mat(Pal.FLINT);
            double ux = -1 / Math.sqrt(2), uy = -1 / Math.sqrt(2);
            for (int i = 0; i <= 15; i++) {
                double x = nx + ux * i, y = ny + uy * i;
                int m = (i / 3) % 2 == 0 ? red : paper;
                c.paint((int) Math.floor(x), (int) Math.floor(y), m, BASE);
                c.paint((int) Math.floor(x) + 1, (int) Math.floor(y), m, LIGHT);
                c.paint((int) Math.floor(x), (int) Math.floor(y) + 1, m, MID);
            }
            int x = (int) Math.floor(nx + ux * 16), y = (int) Math.floor(ny + uy * 16);
            c.paint(x, y, cone, LIGHT);
            c.paint(x - 1, y - 1, cone, HIGH);
        }

        static Img arrow() {
            Canvas c = new Canvas(32, 32);
            int shaft = c.mat(Pal.WOOD), head = c.mat(Pal.FLINT), feather = c.mat(Pal.FEATHER), band = c.mat(Pal.RED);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    int j = x - y, dk = x + y - K0;
                    if (j >= -19 && j <= 15 && (dk == 0 || dk == -1)) c.put(x, y, shaft);
                }
            }
            // Fletching.
            for (int j = -21; j <= -13; j++) {
                for (int dk = -4; dk <= 3; dk++) {
                    if (((j + dk + K0) & 1) != 0 || dk == 0 || dk == -1) continue;
                    int spread = (j - (-21)) / 2 + 1;
                    if (Math.abs(dk + 0.5) > Math.min(3.5, spread)) continue;
                    int x = (j + dk + K0) / 2, y = (dk + K0 - j) / 2;
                    c.put(x, y, j == -15 || j == -16 ? band : feather);
                }
            }
            // Head.
            c.fill((x, y) -> {
                int j = x - y, dk = x + y - K0;
                return j >= 15 && j <= 23 && Math.abs(dk + 0.5) <= (23 - j) * 0.45 + 0.6;
            }, head);
            return c.finish();
        }

        static Img fishingRod(boolean withLine) {
            Canvas c = new Canvas(32, 32);
            int rod = c.mat(Pal.HANDLE), grip = c.mat(Pal.GRIP), reel = c.mat(Pal.IRON), line = c.mat(Pal.STRING), hook = c.mat(Pal.IRON);
            c.fill((x, y) -> {
                int j = x - y, dk = x + y - K0;
                if (j < -23 || j > 23) return false;
                return j < 4 ? dk == -1 || dk == 0 : dk == 0;
            }, rod);
            c.fill((x, y) -> {
                int j = x - y, dk = x + y - K0;
                return j >= -22 && j <= -12 && dk >= -1 && dk <= 1;
            }, grip);
            c.disc(11.5, 23.5, 1.9, reel);
            c.paint(11, 23, reel, MID);
            if (withLine) {
                c.line(27, 4, 27, 22, line, LIGHT);
                c.paint(27, 23, hook, LIGHT);
                c.paint(27, 24, hook, BASE);
                c.paint(26, 25, hook, BASE);
                c.paint(25, 24, hook, MID);
                c.paint(25, 23, hook, HIGH);
            }
            return c.finish();
        }

        static Img stick() {
            Canvas c = new Canvas(32, 32);
            int wood = c.mat(Pal.HANDLE);
            c.fill((x, y) -> {
                int j = x - y, dk = x + y - K0;
                return j >= -21 && j <= 21 && (dk == -1 || dk == 0);
            }, wood);
            Img img = c.finish();
            // Knots.
            int knot = Pal.HANDLE[MID];
            img.set(12, 18, knot);
            img.set(20, 11, knot);
            return img;
        }
    }

    // =====================================================================================================
    // Legacy 16x16 textures, replaced group by group
    // =====================================================================================================

    static final class Legacy16 {
        private Legacy16() {
        }

        static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        static final int[] RGB = {0xF1F3F6, 0xFF8A1F, 0xD94FD6, 0x4FB8FF, 0xFFD62E, 0x7BE34B, 0xFF8FC2, 0x5A5F69,
            0xA4AAB4, 0x1FB5B5, 0x8A4FE0, 0x3559E0, 0x8A5A32, 0x3F8A2C, 0xE0353A, 0x26272D};

        static void generate() throws IOException {
            Files.writeString(root.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "pack_format": 34,
                    "description": "§dAurora Bedwars §7· clean PvP textures"
                  }
                }
                """, StandardCharsets.UTF_8);
            Path t = tex;
            for (int i = 0; i < COLORS.length; i++) {
                saveRaw(wool(RGB[i], i), t.resolve("block/" + COLORS[i] + "_wool.png"));
                saveRaw(terracotta(RGB[i], i), t.resolve("block/" + COLORS[i] + "_terracotta.png"));
                saveRaw(glass(RGB[i], true), t.resolve("block/" + COLORS[i] + "_stained_glass.png"));
                saveRaw(bed(RGB[i]), t.resolve("entity/bed/" + COLORS[i] + ".png"));
            }
            saveRaw(glass(0xDDE6F0, false), t.resolve("block/glass.png"));
            saveRaw(speckled(0xE6E1AE, 0xC9C28A, 11), t.resolve("block/end_stone.png"));
            saveRaw(speckled(0x1C1230, 0x3B2266, 12), t.resolve("block/obsidian.png"));
            saveRaw(logo(), root.resolve("pack.png"));
        }

        static int shade(int rgb, double f) {
            int r = Col.clamp((int) Math.round(((rgb >> 16) & 255) * f));
            int g = Col.clamp((int) Math.round(((rgb >> 8) & 255) * f));
            int b = Col.clamp((int) Math.round((rgb & 255) * f));
            return (r << 16) | (g << 8) | b;
        }

        static int mix(int a, int b, double t) {
            return Col.mix(a, b, t) & 0xFFFFFF;
        }

        static BufferedImage wool(int base, int seed) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Random rnd = new Random(1000 + seed);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double f = 1 + (rnd.nextDouble() - 0.5) * 0.06;
                    if (((x + y * 2) & 3) == 0) f -= 0.05;
                    if (x == 0 || y == 0) f += 0.10;
                    if (x == 15 || y == 15) f -= 0.14;
                    img.setRGB(x, y, Col.opaque(shade(base, f)));
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
                    img.setRGB(x, y, Col.opaque(shade(muted, f)));
                }
            }
            return img;
        }

        static BufferedImage glass(int base, boolean stained) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
                    int argb;
                    if (edge) argb = (0xE6 << 24) | shade(base, stained ? 0.75 : 0.9);
                    else if (x + y >= 3 && x + y <= 5 && x > 1 && y > 1) argb = 0x9AFFFFFF;
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
                    img.setRGB(x, y, Col.opaque(c));
                }
            }
            return img;
        }

        static BufferedImage bed(int color) {
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            int wood = 0x6B4A2E;
            int side = shade(color, 0.82);
            fill(img, 0, 6, 6, 16, side);
            fill(img, 22, 6, 6, 16, side);
            fill(img, 6, 0, 32, 6, side);
            fill(img, 0, 28, 6, 16, side);
            fill(img, 22, 28, 6, 16, side);
            fill(img, 6, 22, 32, 6, side);
            planks(img, 28, 6, wood);
            planks(img, 28, 28, wood);
            blanket(img, 6, 6, color);
            fill(img, 7, 7, 14, 5, 0xF4F4F6);
            fill(img, 7, 11, 14, 1, 0xD5D6DC);
            fill(img, 7, 7, 14, 1, 0xFFFFFF);
            blanket(img, 6, 28, color);
            fill(img, 6, 42, 16, 2, shade(color, 0.7));
            for (int v = 0; v < 24; v += 6) fill(img, 50, v, 12, 6, shade(wood, 0.85));
            return img;
        }

        static void blanket(BufferedImage img, int x0, int y0, int color) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double f = (x == 0 || x == 15) ? 0.85 : ((x + y) % 5 == 0 ? 0.95 : 1.0);
                    img.setRGB(x0 + x, y0 + y, Col.opaque(shade(color, f)));
                }
            }
        }

        static void planks(BufferedImage img, int x0, int y0, int wood) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double f = (y % 4 == 3) ? 0.75 : (x == (y / 4 * 5) % 16 ? 0.85 : 1.0);
                    img.setRGB(x0 + x, y0 + y, Col.opaque(shade(wood, f)));
                }
            }
        }

        static void fill(BufferedImage img, int x0, int y0, int w, int h, int rgb) {
            for (int y = y0; y < y0 + h; y++) for (int x = x0; x < x0 + w; x++) img.setRGB(x, y, Col.opaque(rgb));
        }

        /** 128x128 pack icon: the 32x32 diamond sword on an aurora gradient. */
        static BufferedImage logo() {
            BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    double t = (x + y) / 254.0;
                    int c = mix(mix(0x2A1B5C, 0x7C3AED, t), 0x0EA5E9, Math.max(0, t - 0.5));
                    double dx = Math.max(Math.max(14 - x, x - 113), 0), dy = Math.max(Math.max(14 - y, y - 113), 0);
                    img.setRGB(x, y, dx * dx + dy * dy <= 14 * 14 ? Col.opaque(c) : 0);
                }
            }
            Img s = Weapons.sword(Pal.DIAMOND, 4);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    int c = s.get(x, y);
                    if (Col.a(c) == 0) continue;
                    for (int yy = 0; yy < 3; yy++) for (int xx = 0; xx < 3; xx++) img.setRGB(16 + x * 3 + xx, 16 + y * 3 + yy, c);
                }
            }
            return img;
        }
    }
}
