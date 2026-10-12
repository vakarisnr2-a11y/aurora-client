package dev.aurora.client.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of loading or importing a profile. */
public final class LoadReport {
    private final String profile;
    private final List<String> warnings = new ArrayList<>();
    private boolean recovered;
    private String backupFile;
    private int modulesLoaded;
    private boolean created;

    public LoadReport(String profile) {
        this.profile = profile;
    }

    void warn(String message) {
        warnings.add(message);
    }

    void markRecovered(String backup) {
        this.recovered = true;
        this.backupFile = backup;
    }

    void markCreated() {
        this.created = true;
    }

    void moduleLoaded() {
        modulesLoaded++;
    }

    public String getProfile() {
        return profile;
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /** True when the file was malformed, backed up and replaced with defaults. */
    public boolean isRecovered() {
        return recovered;
    }

    public String getBackupFile() {
        return backupFile;
    }

    public int getModulesLoaded() {
        return modulesLoaded;
    }

    /** True when the profile did not exist and was created with defaults. */
    public boolean isCreated() {
        return created;
    }

    public boolean isClean() {
        return !recovered && warnings.isEmpty();
    }
}
