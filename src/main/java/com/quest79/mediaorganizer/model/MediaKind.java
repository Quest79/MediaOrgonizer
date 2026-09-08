package com.quest79.mediaorganizer.model;

public enum MediaKind {
    MOVIE("Movie"),
    SERIES_EPISODE("Series episode"),
    SPECIAL("Special / OVA / ONA"),
    EXTRA("Extra"),
    UNKNOWN("Unknown");

    private final String label;

    MediaKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
