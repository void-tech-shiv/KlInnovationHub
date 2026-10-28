package com.klu.model;

import java.util.Locale;

/**
 * Allowed collaboration application states. All new applications start in
 * PENDING and owners may take exactly one terminal decision.
 */
public enum CollabApplicationStatus {
    PENDING,
    ACCEPTED,
    REJECTED;

    /**
     * Accept only owner actions; PENDING is an initial state, not a decision.
     */
    public static CollabApplicationStatus parseOwnerDecision(String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new IllegalArgumentException("Status must be ACCEPTED or REJECTED");
        }

        String normalized = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if ("ACCEPTED".equals(normalized)) {
            return ACCEPTED;
        }
        if ("REJECTED".equals(normalized)) {
            return REJECTED;
        }
        throw new IllegalArgumentException("Status must be ACCEPTED or REJECTED");
    }
}
