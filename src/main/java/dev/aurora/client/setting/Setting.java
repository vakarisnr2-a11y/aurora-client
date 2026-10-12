package dev.aurora.client.setting;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A named, validated, JSON-serializable value owned by a module.
 *
 * <p>Every write goes through {@link #sanitize(Object)} so the stored value is always valid,
 * no matter whether it came from the GUI, a command or a (possibly hand-edited) config file.</p>
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    protected T value;
    private BooleanSupplier visibility = () -> true;
    private boolean hidden;
    private final List<Consumer<Setting<?>>> listeners = new ArrayList<>(1);

    protected Setting(String name, String description, T defaultValue) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? "" : description;
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.value = defaultValue;
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final T getDefault() {
        return defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        T validated = sanitize(newValue == null ? defaultValue : newValue);
        if (Objects.equals(validated, value)) return;
        value = validated;
        fireChanged();
    }

    /** Clamp / correct a candidate value. Must never return null. */
    protected T sanitize(T candidate) {
        return candidate;
    }

    public void reset() {
        set(defaultValue);
    }

    public boolean isDefault() {
        return Objects.equals(value, defaultValue);
    }

    /** Visible in the ClickGUI (not hidden and its visibility condition holds). */
    public boolean isVisible() {
        return !hidden && visibility.getAsBoolean();
    }

    public boolean isHidden() {
        return hidden;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
        this.visibility = Objects.requireNonNull(condition);
        return (S) this;
    }

    /** Hidden settings are persisted but never shown in the GUI (e.g. HUD positions). */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S hide() {
        this.hidden = true;
        return (S) this;
    }

    public void addListener(Consumer<Setting<?>> listener) {
        listeners.add(listener);
    }

    protected final void fireChanged() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).accept(this);
        }
    }

    public abstract JsonElement toJson();

    /**
     * Loads a value from JSON.
     *
     * @return {@code false} if the element had the wrong type / format (the current value is kept)
     */
    public abstract boolean load(JsonElement json);

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
