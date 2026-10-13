package dev.aurora.client.hud;

/**
 * Snapping for the HUD editor: screen edges and centre lines, edges and centres of other elements, and an optional
 * grid. Pure math (rectangles as float quadruples) so it can be unit-tested.
 */
public final class HudSnapper {
    /** Output of {@link #snap}. A guide is NaN when nothing was aligned on that axis. */
    public static final class Result {
        public float x;
        public float y;
        public float guideX = Float.NaN;
        public float guideY = Float.NaN;
    }

    private HudSnapper() {
    }

    /**
     * Snaps a rectangle being dragged.
     *
     * @param others     other elements as {x, y, w, h} quadruples
     * @param otherCount number of rectangles in {@code others}
     * @param threshold  maximum distance (GUI pixels) at which an edge snaps
     * @param grid       grid size, or 0 for no grid. The grid only applies on an axis where nothing else snapped.
     * @param elements   whether to snap to other elements
     */
    public static Result snap(float x, float y, float w, float h, float screenW, float screenH, float[] others, int otherCount,
                              float threshold, int grid, boolean elements, Result out) {
        out.guideX = Float.NaN;
        out.guideY = Float.NaN;
        // Horizontal: candidate left-edge positions and the guide line each one aligns on.
        float bestX = Float.NaN;
        float bestDx = threshold;
        float[] cx = {0, 0, screenW - w, screenW, (screenW - w) / 2f, screenW / 2f};
        for (int i = 0; i < cx.length; i += 2) {
            float d = Math.abs(cx[i] - x);
            if (d < bestDx) {
                bestDx = d;
                bestX = cx[i];
                out.guideX = cx[i + 1];
            }
        }
        float bestY = Float.NaN;
        float bestDy = threshold;
        float[] cy = {0, 0, screenH - h, screenH, (screenH - h) / 2f, screenH / 2f};
        for (int i = 0; i < cy.length; i += 2) {
            float d = Math.abs(cy[i] - y);
            if (d < bestDy) {
                bestDy = d;
                bestY = cy[i];
                out.guideY = cy[i + 1];
            }
        }
        if (elements) {
            for (int k = 0; k < otherCount; k++) {
                float ox = others[k * 4], oy = others[k * 4 + 1], ow = others[k * 4 + 2], oh = others[k * 4 + 3];
                // left-left, right-right, left to their right, right to their left, centre-centre
                float[] xs = {ox, ox, ox + ow - w, ox + ow, ox + ow, ox + ow, ox - w, ox, ox + ow / 2f - w / 2f, ox + ow / 2f};
                for (int i = 0; i < xs.length; i += 2) {
                    float d = Math.abs(xs[i] - x);
                    if (d < bestDx) {
                        bestDx = d;
                        bestX = xs[i];
                        out.guideX = xs[i + 1];
                    }
                }
                float[] ys = {oy, oy, oy + oh - h, oy + oh, oy + oh, oy + oh, oy - h, oy, oy + oh / 2f - h / 2f, oy + oh / 2f};
                for (int i = 0; i < ys.length; i += 2) {
                    float d = Math.abs(ys[i] - y);
                    if (d < bestDy) {
                        bestDy = d;
                        bestY = ys[i];
                        out.guideY = ys[i + 1];
                    }
                }
            }
        }
        out.x = !Float.isNaN(bestX) ? bestX : grid > 0 ? Math.round(x / grid) * (float) grid : x;
        out.y = !Float.isNaN(bestY) ? bestY : grid > 0 ? Math.round(y / grid) * (float) grid : y;
        out.x = Math.max(0, Math.min(Math.max(0, screenW - w), out.x));
        out.y = Math.max(0, Math.min(Math.max(0, screenH - h), out.y));
        return out;
    }
}
