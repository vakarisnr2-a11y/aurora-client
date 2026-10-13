import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates the Aurora Client logo: a sharp "A" peak crossed by an aurora ribbon, filled with a teal-to-violet-to-pink
 * aurora gradient, plus a small star.
 *
 * <pre>
 * java tools/LogoGenerator.java src/main/resources/assets/aurora
 *   -> textures/gui/logo.png   256x256 emblem on transparent background (menus, loading screen)
 *   -> icon.png                128x128 mod icon (emblem on a dark rounded square)
 * </pre>
 */
public class LogoGenerator {
    static final Color TEAL = new Color(0x2DD4BF);
    static final Color SKY = new Color(0x38BDF8);
    static final Color VIOLET = new Color(0x8B5CF6);
    static final Color PINK = new Color(0xEC4899);

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "src/main/resources/assets/aurora");
        Files.createDirectories(out.resolve("textures/gui"));
        ImageIO.write(emblem(256, true), "png", out.resolve("textures/gui/logo.png").toFile());
        ImageIO.write(icon(128), "png", out.resolve("icon.png").toFile());
        System.out.println("Wrote " + out.resolve("textures/gui/logo.png") + " and " + out.resolve("icon.png"));
    }

    static Graphics2D smooth(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g;
    }

    /** The mark in a 256-unit design space. */
    static Shape mark() {
        // The "A" peak: an inverted V with a flat-cut base, slightly rounded by the outline stroke later.
        Path2D peak = new Path2D.Float();
        peak.moveTo(128, 30);
        peak.lineTo(222, 214);
        peak.lineTo(176, 214);
        peak.lineTo(128, 116);
        peak.lineTo(80, 214);
        peak.lineTo(34, 214);
        peak.closePath();
        Area mark = new Area(peak);
        // The aurora ribbon: a sweeping band across the legs that rises to the right and leaves the peak at the edges.
        mark.add(new Area(ribbon(0)));
        return mark;
    }

    /** A wavy band from left to right; {@code grow} widens it (for the cut-out outline). */
    static Shape ribbon(float grow) {
        GeneralPath top = new GeneralPath();
        GeneralPath shape = new GeneralPath();
        int steps = 64;
        float x0 = 18, x1 = 240;
        float[] ys = new float[steps + 1];
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            ys[i] = 182 - 46 * t + 12 * (float) Math.sin(t * Math.PI * 1.6 + 0.4);
        }
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float x = x0 + (x1 - x0) * t;
            float thickness = 9 + 9 * (float) Math.sin(t * Math.PI); // tapered ends
            float y = ys[i] - thickness / 2f - grow;
            if (i == 0) shape.moveTo(x, y);
            else shape.lineTo(x, y);
        }
        for (int i = steps; i >= 0; i--) {
            float t = i / (float) steps;
            float x = x0 + (x1 - x0) * t;
            float thickness = 9 + 9 * (float) Math.sin(t * Math.PI);
            shape.lineTo(x, ys[i] + thickness / 2f + grow);
        }
        shape.closePath();
        top.append(shape, false);
        return top;
    }

    static LinearGradientPaint aurora(float size) {
        float s = size / 256f;
        return new LinearGradientPaint(new Point2D.Float(30 * s, 220 * s), new Point2D.Float(226 * s, 32 * s),
            new float[]{0f, 0.35f, 0.7f, 1f}, new Color[]{TEAL, SKY, VIOLET, PINK}, MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    /** The emblem on transparency, with a soft glow when {@code glow} is set. */
    static BufferedImage emblem(int size, boolean glow) {
        float s = size / 256f;
        AffineTransform scale = AffineTransform.getScaleInstance(s, s);
        Shape mark = scale.createTransformedShape(mark());
        Shape cut = scale.createTransformedShape(ribbon(5));
        Shape ribbon = scale.createTransformedShape(ribbon(0));

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        if (glow) {
            BufferedImage halo = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D hg = smooth(halo);
            hg.setPaint(aurora(size));
            hg.setStroke(new BasicStroke(10 * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            hg.fill(mark);
            hg.draw(mark);
            hg.dispose();
            BufferedImage blurred = blur(blur(halo, Math.max(3, Math.round(9 * s))), Math.max(3, Math.round(9 * s)));
            Graphics2D g = smooth(img);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g.drawImage(blurred, 0, 0, null);
            g.dispose();
        }
        Graphics2D g = smooth(img);
        // Peak, with a transparent gap around the ribbon so the band reads as crossing in front.
        Area peakOnly = new Area(mark);
        peakOnly.subtract(new Area(cut));
        peakOnly.add(new Area(ribbon));
        g.setPaint(aurora(size));
        g.fill(peakOnly);
        // A brighter highlight along the ribbon's upper edge.
        g.setClip(ribbon);
        g.setPaint(new LinearGradientPaint(new Point2D.Float(0, 120 * s), new Point2D.Float(0, 200 * s),
            new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 110), new Color(255, 255, 255, 0)}));
        g.fill(ribbon);
        g.setClip(null);
        // A four-pointed star up and to the right of the peak.
        star(g, 196 * s, 62 * s, 18 * s, new Color(255, 255, 255, 235));
        g.dispose();
        return img;
    }

    static void star(Graphics2D g, float cx, float cy, float r, Color color) {
        Path2D p = new Path2D.Float();
        float w = r * 0.22f;
        p.moveTo(cx, cy - r);
        p.quadTo(cx + w * 0.4f, cy - w * 0.4f, cx + r, cy);
        p.quadTo(cx + w * 0.4f, cy + w * 0.4f, cx, cy + r);
        p.quadTo(cx - w * 0.4f, cy + w * 0.4f, cx - r, cy);
        p.quadTo(cx - w * 0.4f, cy - w * 0.4f, cx, cy - r);
        p.closePath();
        g.setPaint(color);
        g.fill(p);
    }

    /** The mod icon: the emblem on a deep night rounded square with an aurora glow. */
    static BufferedImage icon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        RoundRectangle2D box = new RoundRectangle2D.Float(2, 2, size - 4, size - 4, size * 0.42f, size * 0.42f);
        g.setPaint(new LinearGradientPaint(new Point2D.Float(0, 0), new Point2D.Float(0, size),
            new float[]{0f, 1f}, new Color[]{new Color(0x161A33), new Color(0x0A0B14)}));
        g.fill(box);
        g.setClip(box);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(size * 0.5f, size * 0.38f), size * 0.62f,
            new float[]{0f, 1f}, new Color[]{new Color(139, 92, 246, 120), new Color(139, 92, 246, 0)}));
        g.fill(box);
        g.setClip(null);
        g.setPaint(new Color(255, 255, 255, 34));
        g.setStroke(new BasicStroke(2f));
        g.draw(box);
        int inner = Math.round(size * 0.80f);
        BufferedImage mark = emblem(inner * 2, true);
        int off = (size - inner) / 2;
        g.drawImage(mark, off, off + Math.round(size * 0.01f), inner, inner, null);
        g.dispose();
        return img;
    }

    static BufferedImage blur(BufferedImage src, int radius) {
        int n = radius * 2 + 1;
        float[] k = new float[n];
        for (int i = 0; i < n; i++) k[i] = 1f / n;
        BufferedImage padded = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        new ConvolveOp(new Kernel(n, 1, k), ConvolveOp.EDGE_NO_OP, null).filter(src, padded);
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        new ConvolveOp(new Kernel(1, n, k), ConvolveOp.EDGE_NO_OP, null).filter(padded, out);
        return out;
    }
}
