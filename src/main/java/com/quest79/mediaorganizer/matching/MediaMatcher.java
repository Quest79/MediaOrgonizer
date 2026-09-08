package com.quest79.mediaorganizer.matching;

import com.quest79.mediaorganizer.metadata.MetadataCandidate;
import com.quest79.mediaorganizer.model.MediaFile;
import com.quest79.mediaorganizer.model.MediaKind;
import com.quest79.mediaorganizer.model.ParsedMedia;

import java.util.ArrayList;
import java.util.List;

public final class MediaMatcher {

    public MediaFile applyManualMatch(MediaFile file, MetadataCandidate candidate) {
        ParsedMedia parsed = file.parsed();

        List<String> evidence = new ArrayList<>(parsed.evidence());
        evidence.add("manual metadata match: " + candidate.providerId()
                + " #" + candidate.providerItemId()
                + " -> " + candidate.canonicalTitle());

        MediaKind resolvedKind = candidate.kind() == MediaKind.MOVIE
                ? MediaKind.MOVIE
                : (parsed.kind() == MediaKind.SPECIAL ? MediaKind.SPECIAL : MediaKind.SERIES_EPISODE);

        Integer year = candidate.year() != null ? candidate.year() : parsed.year();

        ParsedMedia matched = new ParsedMedia(
                parsed.rawName(),
                candidate.canonicalTitle(),
                year,
                parsed.season(),
                parsed.episodeStart(),
                parsed.episodeEnd(),
                parsed.absoluteEpisode(),
                resolvedKind,
                parsed.releaseGroup(),
                parsed.resolution(),
                parsed.codec(),
                parsed.hdr(),
                parsed.audio(),
                parsed.language(),
                parsed.edition(),
                parsed.part(),
                parsed.disc(),
                0.99,
                evidence
        );

        return file.withParsed(matched);
    }
}
