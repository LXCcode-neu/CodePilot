package com.codepliot.search;

import com.codepliot.search.dto.CodeSearchResult;
import com.codepliot.search.dto.SearchRequest;
import java.util.List;

/**
 * Facade for repository code search implementations.
 */
public interface CodeSearchFacade {

    /**
     * Search repository code and return snippets suitable for Agent context.
     */
    List<CodeSearchResult> search(SearchRequest request);
}
