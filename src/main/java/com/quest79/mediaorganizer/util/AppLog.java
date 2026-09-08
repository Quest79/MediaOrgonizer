package com.quest79.mediaorganizer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLog {

    private static boolean configured;

    private AppLog() {
    }

    public static synchronized void configure() {
        if (configured) return;
        configured = true;

        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);

        try {
            Path logDir = Path.of(System.getProperty("user.home"), ".media-organizer", "logs");
            Files.createDirectories(logDir);

            FileHandler fileHandler = new FileHandler(
                    logDir.resolve("media-organizer.log").toString(),
                    2_000_000,
                    3,
                    true
            );
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            root.addHandler(fileHandler);
        } catch (IOException ex) {
            root.log(Level.WARNING, "Could not initialize file logging.", ex);
        }
    }
}
