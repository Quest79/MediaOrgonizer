# Media Organizer

Media Organizer is a Windows-first JavaFX desktop application for safely understanding and organizing messy movie, anime, and TV libraries.

## Current milestone: scan + parse preview

The first usable milestone is intentionally non-destructive. It can:

- Choose and remember a media library folder.
- Scan large folder trees on a background thread so the UI stays responsive.
- Detect common video files recursively.
- Parse filenames with multiple passes rather than a single regex.
- Extract title, year, season, episode/range, absolute anime episode, release group, resolution, codec, HDR, audio, language, edition, part, and disc hints.
- Use parent-folder and sibling evidence to improve ambiguous files.
- Classify likely movie, series episode, special, extra, or unknown media.
- Show confidence and the evidence used for each parse.
- Preview normalized title / season / episode information.
- Perform zero rename, move, delete, or overwrite operations.

The Apply Changes and Undo controls are present but deliberately disabled until the preview/operation-history layer is implemented.

## Safety architecture

Filesystem scanning/parsing, metadata providers, matching, filesystem operations, and UI are separate systems. Metadata providers do not have filesystem write access.

The intended write path is:

Scan -> Parse -> Match -> Proposed operations -> Preview -> Explicit confirmation -> Execute -> History/Undo

No destructive operation should bypass that path.

## Run

Requirements:

- JDK 21
- Maven 3.9+

From the repository root:

    mvn clean test
    mvn javafx:run

## Package layout

- com.quest79.mediaorganizer.model - immutable scan/parse models
- com.quest79.mediaorganizer.parser - filename intelligence
- com.quest79.mediaorganizer.scanner - folder traversal and sibling context
- com.quest79.mediaorganizer.metadata - provider-neutral metadata contracts
- com.quest79.mediaorganizer.ui - JavaFX presentation only
- com.quest79.mediaorganizer.settings - local preferences
- com.quest79.mediaorganizer.util - logging and shared utilities

## Next development stages

1. TMDB and AniList provider implementations with local caching and rate limiting.
2. Provider-neutral candidate matching and confidence scoring.
3. Manual match/search and reusable user overrides.
4. Current Path -> Proposed Path operation preview.
5. Collision/duplicate/missing-episode diagnostics.
6. Safe move/rename executor.
7. Persistent operation history and Undo.
8. Configurable naming templates and release-metadata retention.
9. Windows packaging with no console window for normal use.
