package com.quest79.mediaorganizer.scanner;

import com.quest79.mediaorganizer.model.MediaFile;
import com.quest79.mediaorganizer.model.ParsedMedia;
import com.quest79.mediaorganizer.parser.FilenameParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class MediaScanner {

    private static final Logger LOG = Logger.getLogger(MediaScanner.class.getName());
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mkv", "mp4", "m4v", "avi", "mov", "wmv", "ts", "m2ts", "webm", "mpg", "mpeg"
    );

    private final FilenameParser parser = new FilenameParser();
    private final FolderContextResolver contextResolver = new FolderContextResolver();

    public List<MediaFile> scan(Path root, BiConsumer<Integer, Integer> progress) throws IOException {
        if (root == null || !Files.isDirectory(root)) {
            throw new IOException("Selected path is not a readable directory.");
        }

        List<Path> mediaPaths;
        try (Stream<Path> walk = Files.walk(root)) {
            mediaPaths = walk
                    .filter(Files::isRegularFile)
                    .filter(MediaScanner::isVideoFile)
                    .sorted()
                    .toList();
        }

        int total = mediaPaths.size();
        List<MediaFile> parsed = new java.util.ArrayList<>(total);

        for (int i = 0; i < mediaPaths.size(); i++) {
            Path path = mediaPaths.get(i);
            ParsedMedia result = parser.parse(path);
            MediaFile file = new MediaFile(path, root, result, MediaFile.statusFor(result));
            parsed.add(file);

            LOG.fine(() -> "Parsed " + path + " => " + result.normalizedPreview()
                    + " | " + String.join("; ", result.evidence()));

            if (progress != null) {
                progress.accept(i + 1, total);
            }
        }

        return contextResolver.apply(parsed);
    }

    private static boolean isVideoFile(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return false;
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return VIDEO_EXTENSIONS.contains(extension);
    }
}
