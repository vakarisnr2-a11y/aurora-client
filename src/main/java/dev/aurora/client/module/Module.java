package dev.aurora.client.module;

import dev.aurora.client.event.Event;
import dev.aurora.client.event.EventBus;
import dev.aurora.client.event.EventPriority;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.Setting;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Base class for every feature.
 *
 * <p>Modules declare their settings as fields ({@code add(new ...Setting(...))}) and their event
 * handlers in the constructor via {@link #on}. Handlers are only subscribed to the event bus
 * while the module is enabled, so disabled modules cost nothing per tick or frame.</p>
 *
 * <p>Constructors must not touch Minecraft state (the client may not exist yet, e.g. in tests).</p>
 */
public abstract class Module {
    protected static final Logger LOGGER = LoggerFactory.getLogger("Aurora/Module");

    private final String name;
    private final String id;
    private final String description;
    private final Category category;
    private final boolean enabledByDefault;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Subscription<?>> subscriptions = new ArrayList<>();
    private final KeybindSetting keybind;
    private final BooleanSetting drawn;
    private List<Setting<?>> allSettings;
    private boolean enabled;
    private ModuleHost host = ModuleHost.NONE;

    protected Module(String name, String description, Category category) {
        this(name, description, category, KeybindSetting.NONE, false);
    }

    protected Module(String name, String description, Category category, int defaultKey, boolean enabledByDefault) {
        this.name = name;
        this.id = name.replace(" ", "").toLowerCase(Locale.ROOT);
        this.description = description;
        this.category = category;
        this.enabledByDefault = enabledByDefault;
        this.keybind = new KeybindSetting("Keybind", "Key or mouse button that toggles " + name
            + ". Right-click the field to switch between Toggle and Hold.", defaultKey, defaultBindMode());
        this.drawn = new BooleanSetting("Show in List", "Show " + name + " in the module list HUD while enabled", true);
        keybind.addListener(this::settingChanged);
        drawn.addListener(this::settingChanged);
    }

    /** Hold-to-use modules (zoom, freelook) override this. */
    protected KeybindSetting.Mode defaultBindMode() {
        return KeybindSetting.Mode.TOGGLE;
    }

    protected final <S extends Setting<?>> S add(S setting) {
        for (Setting<?> existing : settings) {
            if (existing.getName().equalsIgnoreCase(setting.getName())) {
                throw new IllegalStateException("Duplicate setting '" + setting.getName() + "' in " + name);
            }
        }
        settings.add(setting);
        setting.addListener(this::settingChanged);
        allSettings = null;
        return setting;
    }

    private void settingChanged(Setting<?> setting) {
        onSettingChanged(setting);
        host.onSettingChanged(this, setting);
    }

    /** Called when one of this module's settings changes. */
    protected void onSettingChanged(Setting<?> setting) {
    }

    protected final <E extends Event> void on(Class<E> type, Consumer<E> handler) {
        on(type, handler, EventPriority.NORMAL);
    }

    protected final <E extends Event> void on(Class<E> type, Consumer<E> handler, EventPriority priority) {
        Subscription<E> sub = new Subscription<>(type, handler, priority);
        subscriptions.add(sub);
        EventBus bus = host.bus();
        if (enabled && bus != null) sub.register(bus, this);
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void toggle() {
        setEnabled(!enabled, true);
    }

    public final void setEnabled(boolean value) {
        setEnabled(value, true);
    }

    /**
     * @param userAction false when the change comes from config loading (no notification is shown)
     */
    public final void setEnabled(boolean value, boolean userAction) {
        if (value == enabled) return;
        enabled = value;
        EventBus bus = host.bus();
        if (value) {
            if (bus != null) {
                for (Subscription<?> sub : subscriptions) sub.register(bus, this);
            }
            try {
                onEnable();
            } catch (Throwable t) {
                LOGGER.error("Failed to enable {}", name, t);
                enabled = false;
                if (bus != null) bus.unsubscribeAll(this);
                host.onToggled(this, false, userAction);
                return;
            }
            if (!enabled) return; // onEnable() disabled the module again (action modules)
        } else {
            if (bus != null) bus.unsubscribeAll(this);
            try {
                onDisable();
            } catch (Throwable t) {
                LOGGER.error("Failed to cleanly disable {}", name, t);
            }
        }
        host.onToggled(this, value, userAction);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Extra text shown next to the name in the module list (mode, value...). Null for none. */
    public String getDisplayInfo() {
        return null;
    }

    /** Whether the enabled state is stored in profiles. Action modules that open screens return false. */
    public boolean savesEnabledState() {
        return true;
    }

    /** Whether toggling this module produces a notification. */
    public boolean notifiesOnToggle() {
        return true;
    }

    public final String getName() {
        return name;
    }

    /** Lower-case name without spaces, used in configs and commands. */
    public final String getId() {
        return id;
    }

    public final String getDescription() {
        return description;
    }

    public final Category getCategory() {
        return category;
    }

    public final boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public final KeybindSetting getKeybind() {
        return keybind;
    }

    public final boolean isDrawn() {
        return drawn.get();
    }

    /** Module-specific settings followed by the common keybind and list visibility settings. */
    public final List<Setting<?>> getSettings() {
        if (allSettings == null) {
            List<Setting<?>> list = new ArrayList<>(settings.size() + 2);
            list.addAll(settings);
            list.add(keybind);
            list.add(drawn);
            allSettings = Collections.unmodifiableList(list);
        }
        return allSettings;
    }

    public final Setting<?> getSetting(String settingName) {
        for (Setting<?> s : getSettings()) {
            if (s.getName().equalsIgnoreCase(settingName)) return s;
        }
        return null;
    }

    /** Restores every setting (including the keybind) to its default value. */
    public void resetSettings() {
        for (Setting<?> s : getSettings()) s.reset();
    }

    final void setHost(ModuleHost host) {
        this.host = host == null ? ModuleHost.NONE : host;
    }

    protected static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    /** True when a world and player exist. */
    protected static boolean inGame() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.player != null && mc.world != null;
    }

    @Override
    public String toString() {
        return name;
    }

    private record Subscription<E extends Event>(Class<E> type, Consumer<E> handler, EventPriority priority) {
        void register(EventBus bus, Object owner) {
            bus.subscribe(owner, type, handler, priority);
        }
    }
}
