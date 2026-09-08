package com.quest79.mediaorganizer.metadata;

import com.quest79.mediaorganizer.model.MediaKind;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record MetadataCandidate(
        String providerId,
        String providerItemId,
        MediaKind kind,
        String canonicalTitle,
        List<String> alternativeTitles,
        Integer year,
        Integer season,
        Integer episode,
        String episodeTitle,
        LocalDate releaseDate,
        Integer runtimeMinutes,
        String format,
        Integer totalEpisodes
) {
    public MetadataCandidate {
        providerId = Objects.requireNonNullElse(providerId, "");
        providerItemId = Objects.requireNonNullElse(providerItemId, "");
        kind = kind == null ? MediaKind.UNKNOWN : kind;
        canonicalTitle = Objects.requireNonNullElse(canonicalTitle, "");
        alternativeTitles = alternativeTitles == null ? List.of() : List.copyOf(alternativeTitles);
        format = Objects.requireNonNullElse(format, "");
    }

    public String displayText() {
        StringBuilder text = new StringBuilder(canonicalTitle);

        if (year != null) {
            text.append(" (").append(year).append(")");
        }
        if (!format.isBlank()) {
            text.append(" · ").append(format.replace('_', ' '));
        }
        if (totalEpisodes != null) {
            text.append(" · ").append(totalEpisodes).append(" eps");
        }

        List<String> usefulAlternates = alternativeTitles.stream()
                .filter(title -> title != null && !title.isBlank())
                .filter(title -> !title.equalsIgnoreCase(canonicalTitle))
                .distinct()
                .limit(3)
                .toList();

        if (!usefulAlternates.isEmpty()) {
            text.append("\n");
            text.append(usefulAlternates.stream().collect(Collectors.joining(" / ")));
        }

        return text.toString();
    }
}
