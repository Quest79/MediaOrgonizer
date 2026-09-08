package com.quest79.mediaorganizer.parser;

import com.quest79.mediaorganizer.model.MediaKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilenameParserTest {

    private final FilenameParser parser = new FilenameParser();

    @Test
    void parsesSubsPleaseAnimeAbsoluteEpisode() {
        var parsed = parser.parse(Path.of("[SubsPlease] Frieren - 17 (1080p) [ABC123].mkv"));

        assertEquals("Frieren", parsed.title());
        assertEquals(17, parsed.absoluteEpisode());
        assertEquals("SubsPlease", parsed.releaseGroup());
        assertEquals("1080p", parsed.resolution());
        assertEquals(MediaKind.SERIES_EPISODE, parsed.kind());
        assertTrue(parsed.confidence() >= 0.80);
    }

    @Test
    void parsesStandardSeasonEpisode() {
        var parsed = parser.parse(Path.of("Sousou no Frieren S01E17.mkv"));

        assertEquals("Sousou no Frieren", parsed.title());
        assertEquals(1, parsed.season());
        assertEquals(17, parsed.episodeStart());
        assertEquals(MediaKind.SERIES_EPISODE, parsed.kind());
    }

    @Test
    void parsesEnglishAnimeTitleWithAbsoluteEpisode() {
        var parsed = parser.parse(Path.of("Frieren - Beyond Journey's End - 17.mkv"));

        assertEquals("Frieren - Beyond Journey's End", parsed.title());
        assertEquals(17, parsed.absoluteEpisode());
        assertEquals(MediaKind.SERIES_EPISODE, parsed.kind());
    }

    @Test
    void parsesModernMovieRelease() {
        var parsed = parser.parse(Path.of("Dune.Part.Two.2024.2160p.UHD.BluRay.REMUX.mkv"));

        assertEquals("Dune Part Two", parsed.title());
        assertEquals(2024, parsed.year());
        assertEquals("2160p", parsed.resolution());
        assertEquals(MediaKind.MOVIE, parsed.kind());
    }

    @Test
    void movieNumberDoesNotBecomeEpisodeWhenMovieKeywordExists() {
        var parsed = parser.parse(Path.of("Made in Abyss Movie 3 - Dawn of the Deep Soul.mkv"));

        assertEquals(MediaKind.MOVIE, parsed.kind());
        assertTrue(parsed.title().contains("Movie 3"));
    }
}
