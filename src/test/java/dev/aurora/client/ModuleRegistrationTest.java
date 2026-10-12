package dev.aurora.client;

import dev.aurora.client.event.EventBus;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.Modules;
import dev.aurora.client.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Constructs every real module (constructors must not touch Minecraft state) and checks the registry. */
class ModuleRegistrationTest {
    @Test
    void allModulesRegisterWithUniqueNamesAndSettings() {
        ModuleManager manager = new ModuleManager(new EventBus());
        manager.registerAll(Modules.createAll());
        assertTrue(manager.getAll().size() >= 35, "expected all built-in modules");
        for (Category c : Category.values()) {
            assertFalse(manager.getByCategory(c).isEmpty(), "empty category " + c);
        }
        for (Module m : manager.getAll()) {
            assertFalse(m.getDescription().isBlank(), m.getName() + " needs a description");
            Set<String> names = new HashSet<>();
            for (Setting<?> s : m.getSettings()) assertTrue(names.add(s.getName().toLowerCase()), "duplicate setting in " + m.getName());
        }
    }
}
