package com.quest79.mediaorganizer.settings;

import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

public final class LocalSettings {

    private static final String LAST_LIBRARY = "lastLibrary";
    private final Preferences preferences = Preferences.userNodeForPackage(LocalSettings.class);

    public Optional<Path> lastLibrary() {
        String value = preferences.get(LAST_LIBRARY, "");
        if (value.isBlank()) return Optional.empty();
        return Optional.of(Path.of(value));
    }

    public void setLastLibrary(Path path) {
        if (path == null) return;
        preferences.put(LAST_LIBRARY, path.toAbsolutePath().normalize().toString());
    }
}
