package dev.aurora.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The jar runs on Minecraft 1.21 and 1.21.1 (same code; 1.21.2 changed too much). */
class MinecraftVersionTest {
    private static VersionPredicate minecraftRange() throws Exception {
        try (InputStream in = MinecraftVersionTest.class.getResourceAsStream("/fabric.mod.json")) {
            assertNotNull(in, "fabric.mod.json is on the classpath");
            JsonObject mod = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return VersionPredicate.parse(mod.getAsJsonObject("depends").get("minecraft").getAsString());
        }
    }

    @Test
    void loadsOnMinecraft121And1211() throws Exception {
        VersionPredicate range = minecraftRange();
        assertTrue(range.test(SemanticVersion.parse("1.21")));
        assertTrue(range.test(SemanticVersion.parse("1.21.1")));
    }

    @Test
    void refusesOtherVersions() throws Exception {
        VersionPredicate range = minecraftRange();
        for (String v : new String[]{"1.20.6", "1.21-rc.1", "1.21.2-rc.1", "1.21.2", "1.21.4", "1.22"}) {
            assertFalse(range.test(SemanticVersion.parse(v)), v);
        }
    }
}
