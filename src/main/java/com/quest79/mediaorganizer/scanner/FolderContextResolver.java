package com.quest79.mediaorganizer.scanner;

import com.quest79.mediaorganizer.model.MediaFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class FolderContextResolver {

    List<MediaFile> apply(List<MediaFile> files) {
        Map<Path, List<MediaFile>> byFolder = files.stream()
                .collect(Collectors.groupingBy(file -> file.path().getParent()));

        List<MediaFile> resolved = new ArrayList<>(files.size());
        for (List<MediaFile> siblings : byFolder.values()) {
            String dominantTitle = dominantTitle(siblings);
            for (MediaFile file : siblings) {
                if (dominantTitle != null && shouldUseContextTitle(file)) {
                    resolved.add(file.withParsed(file.parsed().withContextTitle(
                            dominantTitle,
                            0.12,
                            "parent-folder sibling consensus: " + dominantTitle
                    )));
                } else {
                    resolved.add(file);
                }
            }
        }

        resolved.sort(Comparator.comparing(file -> file.path().toString().toLowerCase(Locale.ROOT)));
        return resolved;
    }

    private static boolean shouldUseContextTitle(MediaFile file) {
        String title = file.parsed().title();
        if (title.isBlank()) return true;

        String normalized = title.toLowerCase(Locale.ROOT);
        return normalized.matches("(episode|ep|e)\\s*\\d+")
                || normalized.matches("\\d+")
                || normalized.length() < 3;
    }

    private static String dominantTitle(List<MediaFile> siblings) {
        if (siblings.size() < 2) return null;

        Map<String, Long> counts = siblings.stream()
                .map(file -> file.parsed().title())
                .filter(title -> title != null && title.length() >= 3)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        HashMap::new,
                        Collectors.counting()
                ));

        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
