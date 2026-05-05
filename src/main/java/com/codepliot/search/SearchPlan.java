package com.codepliot.search;

import java.util.List;

/**
 * Read-only search plan generated from the issue text.
 */
public record SearchPlan(
        List<SearchPlanQuery> queries,
        List<String> fileGlobs,
        String source,
        String warning
) {

    public SearchPlan {
        queries = queries == null ? List.of() : List.copyOf(queries);
        fileGlobs = fileGlobs == null ? List.of() : List.copyOf(fileGlobs);
    }

    public static SearchPlan deterministic(List<SearchPlanQuery> queries, List<String> fileGlobs) {
        return new SearchPlan(queries, fileGlobs, "deterministic", "");
    }
}
