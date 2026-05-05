package com.codepliot.search;

import java.util.List;

/**
 * One grep/glob probe in a Claude Code style search plan.
 */
public record SearchPlanQuery(
        String pattern,
        boolean regexEnabled,
        List<String> globPatterns,
        String reason
) {

    public SearchPlanQuery {
        globPatterns = globPatterns == null ? List.of() : List.copyOf(globPatterns);
    }
}
