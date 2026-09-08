package com.quest79.mediaorganizer.model;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ParsedMedia(
        String rawName,
        String title,
        Integer year,
        Integer season,
        Integer episodeStart,
        Integer episodeEnd,
        Integer absoluteEpisode,
        MediaKind kind,
        String releaseGroup,
        String resolution,
        String codec,
        String hdr,
        String audio,
        String language,
        String edition,
        Integer part,
        Integer disc,
        double confidence,
        List<String> evidence
) {
    public ParsedMedia {
        rawName = Objects.requireNonNullElse(rawName, "");
        title = Objects.requireNonNullElse(title, "").trim();
        kind = kind == null ? MediaKind.UNKNOWN : kind;
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public String confidenceText() {
        return String.format(Locale.ROOT, "%.0f%%", confidence * 100.0);
    }

    public String episodeText() {
        if (episodeStart != null) {
            if (episodeEnd != null && !episodeEnd.equals(episodeStart)) {
                return "%02d-%02d".formatted(episodeStart, episodeEnd);
            }
            return "%02d".formatted(episodeStart);
        }
        return absoluteEpisode == null ? "" : "Abs %d".formatted(absoluteEpisode);
    }

    public String seasonText() {
        return season == null ? "" : "%02d".formatted(season);
    }

    public String releaseSummary() {
        return Stream.of(resolution, codec, hdr, audio, language,
                        releaseGroup == null ? null : "[" + releaseGroup + "]",
                        edition)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" · "));
    }

    public String normalizedPreview() {
        String safeTitle = title.isBlank() ? "Unknown title" : title;

        if (kind == MediaKind.MOVIE) {
            return year == null ? safeTitle : safeTitle + " (" + year + ")";
        }

        if (season != null && episodeStart != null) {
            String episodeCode = "S%02dE%02d".formatted(season, episodeStart);
            if (episodeEnd != null && !episodeEnd.equals(episodeStart)) {
                episodeCode += "-E%02d".formatted(episodeEnd);
            }
            return safeTitle + " - " + episodeCode;
        }

        if (absoluteEpisode != null) {
            return safeTitle + " - " + absoluteEpisode;
        }

        if (kind == MediaKind.SPECIAL) {
            return safeTitle + " - Special";
        }

        return safeTitle;
    }

    public ParsedMedia withContextTitle(String contextTitle, double confidenceBoost, String reason) {
        if (contextTitle == null || contextTitle.isBlank()) {
            return this;
        }
        List<String> updated = Stream.concat(evidence.stream(), Stream.of(reason)).toList();
        return new ParsedMedia(
                rawName, contextTitle, year, season, episodeStart, episodeEnd, absoluteEpisode,
                kind, releaseGroup, resolution, codec, hdr, audio, language, edition,
                part, disc, Math.min(1.0, confidence + confidenceBoost), updated
        );
    }
}
