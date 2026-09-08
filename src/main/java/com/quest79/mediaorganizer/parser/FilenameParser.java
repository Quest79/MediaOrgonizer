package com.quest79.mediaorganizer.parser;

import com.quest79.mediaorganizer.model.MediaKind;
import com.quest79.mediaorganizer.model.ParsedMedia;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FilenameParser {

    private static final Pattern LEADING_GROUP = Pattern.compile("^\\[([^]\\r\\n]+)]\\s*");
    private static final Pattern BRACKET = Pattern.compile("\\[([^]\\r\\n]+)]");
    private static final Pattern PAREN_GROUP = Pattern.compile("\\(([^)]*)\\)");
    private static final Pattern YEAR = Pattern.compile("(?<!\\d)(19\\d{2}|20\\d{2})(?!\\d)");
    private static final Pattern SEASON_EPISODE = Pattern.compile(
            "(?i)(?<![A-Z0-9])S(\\d{1,2})[ ._-]*E(\\d{1,3})(?:[ ._-]*(?:-|~|to)[ ._-]*(?:S\\d{1,2}[ ._-]*)?E?(\\d{1,3}))?(?!\\d)"
    );
    private static final Pattern X_EPISODE = Pattern.compile(
            "(?i)(?<!\\d)(\\d{1,2})x(\\d{1,3})(?:[-~](\\d{1,3}))?(?!\\d)"
    );
    private static final Pattern ABSOLUTE_AT_END = Pattern.compile(
            "(?i)^(.*?)(?:\\s+-\\s+|\\s+)(\\d{1,3})(?:v\\d+)?(?:\\s*-\\s*(\\d{1,3}))?\\s*$"
    );

    // Deliberately do not use \b here: underscore is a word character and is extremely common in release names.
    private static final Pattern RESOLUTION = Pattern.compile(
            "(?i)(?<![A-Z0-9])(?:DVD|BD|UHD)?[ ._-]*(480p|576p|720p|1080p|1440p|2160p|4320p|4k|8k)(?![A-Z0-9])"
    );
    private static final Pattern CODEC = Pattern.compile(
            "(?i)(?<![A-Z0-9])(x264|x265|h[ ._-]?264|h[ ._-]?265|hevc|av1|vp9|mpeg[ ._-]?2)(?![A-Z0-9])"
    );
    private static final Pattern HDR = Pattern.compile(
            "(?i)(?<![A-Z0-9])(HDR10\\+?|HDR|Dolby[ ._-]?Vision|DV)(?![A-Z0-9])"
    );
    private static final Pattern AUDIO = Pattern.compile(
            "(?i)(?<![A-Z0-9])(TrueHD(?:[ ._-]?Atmos)?|Atmos|DTS(?:[ ._-]?HD(?:[ ._-]?MA)?)?|DDP?5\\.?1|EAC3|AC3|AAC(?:2\\.?0|5\\.?1)?|FLAC|Opus)(?![A-Z0-9])"
    );
    private static final Pattern LANGUAGE = Pattern.compile(
            "(?i)(?<![A-Z0-9])(English|Japanese|Dual[ ._-]?Audio|Multi[ ._-]?Audio|Dub(?:bed)?|Sub(?:bed)?)(?![A-Z0-9])"
    );
    private static final Pattern EDITION = Pattern.compile(
            "(?i)(?<![A-Z0-9])(Director'?s[ ._-]?Cut|Extended(?:[ ._-]?Edition)?|Remaster(?:ed)?|Unrated|Theatrical(?:[ ._-]?Cut)?|Special[ ._-]?Edition|Ultimate[ ._-]?Edition)(?![A-Z0-9])"
    );
    private static final Pattern PART = Pattern.compile("(?i)(?<![A-Z0-9])Part[ ._-]?(\\d{1,2})(?![A-Z0-9])");
    private static final Pattern DISC = Pattern.compile("(?i)(?<![A-Z0-9])(?:Disc|Disk|CD)[ ._-]?(\\d{1,2})(?![A-Z0-9])");
    private static final Pattern SPECIAL = Pattern.compile("(?i)(?<![A-Z0-9])(OVA|ONA|Specials?|SP)(?![A-Z0-9])");
    private static final Pattern EXTRA = Pattern.compile("(?i)(?<![A-Z0-9])(NCOP|NCED|Trailer|Teaser|Interview|Featurette|Sample)(?![A-Z0-9])");
    private static final Pattern MOVIE_WORD = Pattern.compile("(?i)(?<![A-Z0-9])(Movie|Film)(?![A-Z0-9])");
    private static final Pattern HASH = Pattern.compile("(?i)^[A-F0-9]{6,12}$");

    private static final Set<String> NON_GROUP_TOKENS = Set.of(
            "1080p", "720p", "2160p", "hevc", "x264", "x265", "av1", "aac", "flac"
    );

    public ParsedMedia parse(Path path) {
        String filename = path.getFileName().toString();
        String stem = stripExtension(filename);
        String working = stem;
        List<String> evidence = new ArrayList<>();

        String releaseGroup = null;
        Matcher leadingGroupMatcher = LEADING_GROUP.matcher(working);
        if (leadingGroupMatcher.find()) {
            String candidate = leadingGroupMatcher.group(1).trim();
            if (!isTechnicalGroup(candidate)) {
                releaseGroup = candidate;
                evidence.add("leading release/fansub group: " + candidate);
                working = working.substring(leadingGroupMatcher.end());
            }
        }

        String resolution = firstGroup(RESOLUTION, stem);
        String codec = normalizeCodec(firstGroup(CODEC, stem));
        String hdr = normalizeHdr(firstGroup(HDR, stem));
        String audio = firstGroup(AUDIO, stem);
        String language = firstGroup(LANGUAGE, stem);
        String edition = firstGroup(EDITION, stem);
        Integer part = integerGroup(PART, stem, 1);
        Integer disc = integerGroup(DISC, stem, 1);

        addEvidenceIfPresent(evidence, resolution, "resolution");
        addEvidenceIfPresent(evidence, codec, "codec");
        addEvidenceIfPresent(evidence, hdr, "HDR");
        addEvidenceIfPresent(evidence, audio, "audio");
        addEvidenceIfPresent(evidence, language, "language");
        addEvidenceIfPresent(evidence, edition, "edition");
        if (part != null) evidence.add("part number: " + part);
        if (disc != null) evidence.add("disc number: " + disc);

        working = removeTechnicalBrackets(working);
        working = removeTechnicalParentheses(working);
        working = removePattern(working, RESOLUTION);
        working = removePattern(working, CODEC);
        working = removePattern(working, HDR);
        working = removePattern(working, AUDIO);
        working = removePattern(working, LANGUAGE);
        working = removePattern(working, EDITION);
        working = removeSourceNoise(working);

        Integer year = null;
        Matcher yearMatcher = YEAR.matcher(working);
        if (yearMatcher.find()) {
            year = Integer.valueOf(yearMatcher.group(1));
            evidence.add("year token: " + year);
            working = yearMatcher.replaceFirst(" ");
        }

        Integer season = null;
        Integer episodeStart = null;
        Integer episodeEnd = null;
        Integer absoluteEpisode = null;
        boolean explicitEpisode = false;

        Matcher seMatcher = SEASON_EPISODE.matcher(working);
        if (seMatcher.find()) {
            season = Integer.valueOf(seMatcher.group(1));
            episodeStart = Integer.valueOf(seMatcher.group(2));
            episodeEnd = nullableInt(seMatcher.group(3));
            explicitEpisode = true;
            evidence.add("explicit SxxEyy episode token");
            working = seMatcher.replaceFirst(" ");
        } else {
            Matcher xMatcher = X_EPISODE.matcher(working);
            if (xMatcher.find()) {
                season = Integer.valueOf(xMatcher.group(1));
                episodeStart = Integer.valueOf(xMatcher.group(2));
                episodeEnd = nullableInt(xMatcher.group(3));
                explicitEpisode = true;
                evidence.add("explicit NxMM episode token");
                working = xMatcher.replaceFirst(" ");
            }
        }

        boolean special = SPECIAL.matcher(stem).find();
        boolean extra = EXTRA.matcher(stem).find();
        boolean movieWord = MOVIE_WORD.matcher(stem).find();

        if (!explicitEpisode && !movieWord && !special && !extra) {
            String absoluteCandidate = cleanupForAbsoluteMatch(working);
            Matcher absoluteMatcher = ABSOLUTE_AT_END.matcher(absoluteCandidate);
            if (absoluteMatcher.matches()) {
                int value = Integer.parseInt(absoluteMatcher.group(2));
                if (value > 0 && value < 1000 && (year == null || value != year)) {
                    absoluteEpisode = value;
                    episodeEnd = nullableInt(absoluteMatcher.group(3));
                    evidence.add("anime-style absolute episode token");
                    working = absoluteMatcher.group(1);
                }
            }
        }

        String title = cleanTitle(working);

        MediaKind kind;
        double confidence;

        if (extra) {
            kind = MediaKind.EXTRA;
            confidence = title.isBlank() ? 0.56 : 0.78;
            evidence.add("extra marker");
        } else if (special) {
            kind = MediaKind.SPECIAL;
            confidence = title.isBlank() ? 0.58 : 0.80;
            evidence.add("special / OVA / ONA marker");
        } else if (explicitEpisode) {
            kind = MediaKind.SERIES_EPISODE;
            confidence = title.isBlank() ? 0.64 : 0.90;
            evidence.add("series classification from explicit season/episode");
        } else if (absoluteEpisode != null) {
            kind = MediaKind.SERIES_EPISODE;
            confidence = releaseGroup == null ? 0.74 : 0.84;
            evidence.add("series classification from absolute episode pattern");
        } else if (year != null || movieWord) {
            kind = MediaKind.MOVIE;
            confidence = (year != null && !title.isBlank()) ? 0.88 : 0.72;
            evidence.add(year != null ? "movie classification from title + year context" : "movie keyword");
        } else {
            kind = MediaKind.UNKNOWN;
            confidence = title.isBlank() ? 0.15 : 0.42;
            evidence.add("insufficient episode/movie evidence");
        }

        if (releaseGroup != null) confidence += 0.03;
        if (resolution != null) confidence += 0.01;

        return new ParsedMedia(
                filename, title, year, season, episodeStart, episodeEnd, absoluteEpisode,
                kind, releaseGroup, resolution, codec, hdr, audio, language, edition,
                part, disc, Math.min(confidence, 0.98), evidence
        );
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot <= 0 ? filename : filename.substring(0, dot);
    }

    private static String removeTechnicalBrackets(String input) {
        Matcher matcher = BRACKET.matcher(input);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1).trim();
            if (isTechnicalGroup(token)) {
                matcher.appendReplacement(out, " ");
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(" " + token + " "));
            }
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String removeTechnicalParentheses(String input) {
        Matcher matcher = PAREN_GROUP.matcher(input);
        StringBuilder out = new StringBuilder();

        while (matcher.find()) {
            String token = matcher.group(1).trim();
            if (isTechnicalGroup(token)) {
                matcher.appendReplacement(out, " ");
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(" (" + token + ") "));
            }
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private static boolean isTechnicalGroup(String token) {
        String normalized = token.toLowerCase(Locale.ROOT).replace(" ", "");
        if (HASH.matcher(token).matches() || NON_GROUP_TOKENS.contains(normalized)) {
            return true;
        }

        return RESOLUTION.matcher(token).find()
                || CODEC.matcher(token).find()
                || HDR.matcher(token).find()
                || AUDIO.matcher(token).find()
                || LANGUAGE.matcher(token).find()
                || token.matches("(?i).*(?:10bit|8bit|Hi10P|DVD|Blu[ ._-]?Ray|WEB[ ._-]?DL|WEBRip|REMUX|BDRip).*");
    }

    private static String removeSourceNoise(String input) {
        return input
                .replaceAll("(?i)(?<![A-Z0-9])(?:UHD|DVD|Blu[ ._-]?Ray|BDRip|WEB[ ._-]?DL|WEBRip|REMUX|DVDRip|HDTV|AMZN|NF|CR)(?![A-Z0-9])", " ")
                .replaceAll("(?i)(?<![A-Z0-9])(?:10bit|8bit|Hi10P)(?![A-Z0-9])", " ");
    }

    private static String removePattern(String input, Pattern pattern) {
        return pattern.matcher(input).replaceAll(" ");
    }

    private static String cleanupForAbsoluteMatch(String input) {
        return input
                .replace('.', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String cleanTitle(String input) {
        return input
                .replaceAll("(?i)(?<![A-Z0-9])(?:S\\d{1,2}|Season[ ._-]?\\d{1,2})(?![A-Z0-9])", " ")
                .replaceAll("[._]+", " ")
                .replaceAll("\\s+-\\s*$", " ")
                .replaceAll("^\\s*-\\s+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String firstGroup(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1).replace('_', ' ').trim() : null;
    }

    private static String normalizeCodec(String codec) {
        if (codec == null) return null;
        String value = codec.toUpperCase(Locale.ROOT).replaceAll("[ ._-]", "");
        return switch (value) {
            case "H264", "X264" -> "H.264";
            case "H265", "X265", "HEVC" -> "HEVC";
            case "AV1" -> "AV1";
            case "VP9" -> "VP9";
            default -> codec;
        };
    }

    private static String normalizeHdr(String hdr) {
        if (hdr == null) return null;
        String value = hdr.toUpperCase(Locale.ROOT).replaceAll("[ ._-]", "");
        if (value.equals("DV") || value.equals("DOLBYVISION")) return "Dolby Vision";
        if (value.equals("HDR10+")) return "HDR10+";
        if (value.startsWith("HDR")) return "HDR";
        return hdr;
    }

    private static Integer integerGroup(Pattern pattern, String input, int group) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? Integer.valueOf(matcher.group(group)) : null;
    }

    private static Integer nullableInt(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private static void addEvidenceIfPresent(List<String> evidence, String value, String label) {
        if (value != null && !value.isBlank()) {
            evidence.add(label + ": " + value);
        }
    }
}
