package dev.aurora.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/** A slider value clamped to [min, max] and snapped to {@code step}. */
public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final int decimals;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        if (!(min < max)) throw new IllegalArgumentException("min must be < max for " + name);
        if (step <= 0) throw new IllegalArgumentException("step must be > 0 for " + name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.decimals = step >= 1 ? 0 : Math.min(4, (int) Math.ceil(-Math.log10(step) - 1e-9));
        this.value = sanitize(defaultValue);
    }

    @Override
    protected Double sanitize(Double candidate) {
        double v = candidate;
        if (Double.isNaN(v) || Double.isInfinite(v)) v = getDefault();
        v = Math.max(min, Math.min(max, v));
        v = min + Math.round((v - min) / step) * step;
        v = Math.max(min, Math.min(max, v));
        // Remove floating point noise such as 0.30000000000000004.
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    public float getFloat() {
        return value.floatValue();
    }

    public double getDouble() {
        return value;
    }

    /** Position of the value on the slider, 0..1. */
    public double getProgress() {
        return (value - min) / (max - min);
    }

    public void setProgress(double progress) {
        set(min + Math.max(0, Math.min(1, progress)) * (max - min));
    }

    public void increment(int direction) {
        set(value + direction * step);
    }

    public String format() {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public boolean load(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) return false;
        JsonPrimitive p = json.getAsJsonPrimitive();
        try {
            if (p.isNumber()) {
                set(p.getAsDouble());
                return true;
            }
            if (p.isString()) {
                set(Double.parseDouble(p.getAsString().trim()));
                return true;
            }
        } catch (NumberFormatException ignored) {
            // fall through: invalid value keeps the current one
        }
        return false;
    }
}
