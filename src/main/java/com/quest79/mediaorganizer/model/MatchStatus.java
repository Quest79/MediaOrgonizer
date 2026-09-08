package com.quest79.mediaorganizer.model;

public enum MatchStatus {
    HIGH_CONFIDENCE("High confidence"),
    PARSED("Parsed"),
    NEEDS_REVIEW("Needs review"),
    UNKNOWN("Unknown"),
    DUPLICATE("Duplicate"),
    CONFLICT("Conflict");

    private final String label;

    MatchStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
