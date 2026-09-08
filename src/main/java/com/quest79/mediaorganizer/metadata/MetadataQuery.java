package com.quest79.mediaorganizer.metadata;

import com.quest79.mediaorganizer.model.MediaKind;

public record MetadataQuery(
        String title,
        Integer year,
        Integer season,
        Integer episode,
        Integer absoluteEpisode,
        MediaKind expectedKind
) {
}
