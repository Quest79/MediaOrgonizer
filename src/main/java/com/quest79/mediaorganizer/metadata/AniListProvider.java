package com.quest79.mediaorganizer.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quest79.mediaorganizer.model.MediaKind;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class AniListProvider implements MetadataProvider {

    private static final URI ENDPOINT = URI.create("https://graphql.anilist.co");
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SEARCH_QUERY = """
            query ($search: String!) {
              Page(page: 1, perPage: 12) {
                media(search: $search, type: ANIME) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  synonyms
                  format
                  episodes
                  duration
                  seasonYear
                  startDate {
                    year
                    month
                    day
                  }
                }
              }
            }
            """;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String id() {
        return "anilist";
    }

    @Override
    public String displayName() {
        return "AniList";
    }

    @Override
    public Set<MediaKind> supportedKinds() {
        return Set.of(
                MediaKind.SERIES_EPISODE,
                MediaKind.SPECIAL,
                MediaKind.MOVIE,
                MediaKind.UNKNOWN
        );
    }

    @Override
    public CompletableFuture<List<MetadataCandidate>> search(MetadataQuery query) {
        String search = query == null || query.title() == null ? "" : query.title().trim();
        if (search.isBlank()) {
            return CompletableFuture.completedFuture(List.of());
        }

        ObjectNode payload = JSON.createObjectNode();
        payload.put("query", SEARCH_QUERY);
        payload.putObject("variables").put("search", search);

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "MediaOrganizer/0.2")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new IllegalStateException(
                                "AniList returned HTTP " + response.statusCode()
                        ));
                    }

                    try {
                        JsonNode root = JSON.readTree(response.body());

                        if (root.has("errors")) {
                            throw new IllegalStateException(
                                    root.path("errors").path(0).path("message").asText("AniList search failed")
                            );
                        }

                        JsonNode media = root.path("data").path("Page").path("media");
                        if (!media.isArray()) {
                            return List.<MetadataCandidate>of();
                        }

                        List<MetadataCandidate> candidates = new ArrayList<>();
                        for (JsonNode item : media) {
                            candidates.add(toCandidate(item));
                        }
                        return List.copyOf(candidates);
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    private static MetadataCandidate toCandidate(JsonNode item) {
        String romaji = text(item.path("title").path("romaji"));
        String english = text(item.path("title").path("english"));
        String nativeTitle = text(item.path("title").path("native"));

        String canonical = !english.isBlank() ? english : romaji;
        if (canonical.isBlank()) canonical = nativeTitle;

        LinkedHashSet<String> alternate = new LinkedHashSet<>();
        addIfPresent(alternate, romaji);
        addIfPresent(alternate, english);
        addIfPresent(alternate, nativeTitle);

        JsonNode synonyms = item.path("synonyms");
        if (synonyms.isArray()) {
            for (JsonNode synonym : synonyms) {
                addIfPresent(alternate, synonym.asText(""));
            }
        }

        String format = text(item.path("format"));
        MediaKind kind = "MOVIE".equalsIgnoreCase(format)
                ? MediaKind.MOVIE
                : MediaKind.SERIES_EPISODE;

        Integer year = nullableInt(item.path("seasonYear"));
        if (year == null) {
            year = nullableInt(item.path("startDate").path("year"));
        }

        LocalDate releaseDate = date(item.path("startDate"));

        return new MetadataCandidate(
                "anilist",
                item.path("id").asText(""),
                kind,
                canonical,
                List.copyOf(alternate),
                year,
                null,
                null,
                null,
                releaseDate,
                nullableInt(item.path("duration")),
                format,
                nullableInt(item.path("episodes"))
        );
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText("").trim();
    }

    private static Integer nullableInt(JsonNode node) {
        if (node == null || node.isNull() || !node.canConvertToInt()) return null;
        int value = node.asInt();
        return value <= 0 ? null : value;
    }

    private static LocalDate date(JsonNode startDate) {
        Integer year = nullableInt(startDate.path("year"));
        Integer month = nullableInt(startDate.path("month"));
        Integer day = nullableInt(startDate.path("day"));

        if (year == null) return null;

        try {
            return LocalDate.of(year, month == null ? 1 : month, day == null ? 1 : day);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void addIfPresent(Set<String> titles, String value) {
        if (value != null && !value.isBlank()) titles.add(value.trim());
    }
}
