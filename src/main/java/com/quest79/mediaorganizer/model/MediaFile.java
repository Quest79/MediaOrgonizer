package com.quest79.mediaorganizer.model;

import java.nio.file.Path;
import java.util.Objects;

public record MediaFile(Path path, Path root, ParsedMedia parsed, MatchStatus status) {

    public MediaFile {
        path = Objects.requireNonNull(path);
        root = Objects.requireNonNull(root);
        parsed = Objects.requireNonNull(parsed);
        status = Objects.requireNonNull(status);
    }

    public String relativePath() {
        try {
            return root.relativize(path).toString();
        } catch (IllegalArgumentException ignored) {
            return path.toString();
        }
    }

    public MediaFile withParsed(ParsedMedia updated) {
        return new MediaFile(path, root, updated, statusFor(updated));
    }

    public static MatchStatus statusFor(ParsedMedia parsed) {
        if (parsed.kind() == MediaKind.UNKNOWN || parsed.title().isBlank()) {
            return MatchStatus.UNKNOWN;
        }
        if (parsed.confidence() >= 0.82) {
            return MatchStatus.HIGH_CONFIDENCE;
        }
        if (parsed.confidence() >= 0.60) {
            return MatchStatus.PARSED;
        }
        return MatchStatus.NEEDS_REVIEW;
    }
}
