package dev.aurora.client.module;

import dev.aurora.client.event.EventBus;
import dev.aurora.client.event.events.ModuleToggleEvent;
import dev.aurora.client.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Registry of all modules with lookup by class, id and category. */
public final class ModuleManager implements ModuleHost {
    private final EventBus bus;
    private final List<Module> modules = new ArrayList<>();
    private final List<Module> view = Collections.unmodifiableList(modules);
    private final Map<String, Module> byId = new LinkedHashMap<>();
    private final Map<Class<? extends Module>, Module> byClass = new HashMap<>();
    private final Map<Category, List<Module>> byCategory = new EnumMap<>(Category.class);
    private Runnable dirtyListener = () -> {
    };

    public ModuleManager(EventBus bus) {
        this.bus = bus;
        for (Category c : Category.values()) byCategory.put(c, new ArrayList<>());
    }

    public <T extends Module> T register(T module) {
        if (byId.containsKey(module.getId())) {
            throw new IllegalStateException("Duplicate module id: " + module.getId());
        }
        if (byClass.containsKey(module.getClass())) {
            throw new IllegalStateException("Module class registered twice: " + module.getClass().getName());
        }
        module.setHost(this);
        modules.add(module);
        byId.put(module.getId(), module);
        byClass.put(module.getClass(), module);
        byCategory.get(module.getCategory()).add(module);
        return module;
    }

    public void registerAll(Iterable<? extends Module> list) {
        for (Module m : list) register(m);
    }

    /** Looks up a registered module by class. Throws if it is not registered (programming error). */
    public <T extends Module> T get(Class<T> type) {
        Module m = byClass.get(type);
        if (m == null) throw new IllegalArgumentException("Module not registered: " + type.getSimpleName());
        return type.cast(m);
    }

    /** Finds a module by id or display name, ignoring case and spaces. */
    public Optional<Module> find(String nameOrId) {
        if (nameOrId == null) return Optional.empty();
        return Optional.ofNullable(byId.get(nameOrId.replace(" ", "").toLowerCase(Locale.ROOT)));
    }

    public List<Module> getAll() {
        return view;
    }

    public List<Module> getByCategory(Category category) {
        return Collections.unmodifiableList(byCategory.get(category));
    }

    public List<Module> getEnabled() {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.isEnabled()) out.add(m);
        return out;
    }

    public void setDirtyListener(Runnable listener) {
        this.dirtyListener = listener == null ? () -> {
        } : listener;
    }

    @Override
    public EventBus bus() {
        return bus;
    }

    @Override
    public void onToggled(Module module, boolean enabled, boolean userAction) {
        if (module.savesEnabledState()) dirtyListener.run();
        if (userAction && bus != null) bus.post(ModuleToggleEvent.INSTANCE.set(module, enabled));
    }

    @Override
    public void onSettingChanged(Module module, Setting<?> setting) {
        dirtyListener.run();
    }
}
