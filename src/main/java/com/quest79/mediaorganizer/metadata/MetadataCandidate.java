package com.quest79.mediaorganizer.metadata;

import com.quest79.mediaorganizer.model.MediaKind;

import java.time.LocalDate;
import java.util.List;

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
        Integer runtimeMinutes
) {
    public MetadataCandidate {
        alternativeTitles = alternativeTitles == null ? List.of() : List.copyOf(alternativeTitles);
    }
}
