package com.codepliot.search;

import com.codepliot.search.dto.CodeSearchResult;
import com.codepliot.search.dto.SearchRequest;
import java.util.List;

/**
 * 仓库代码检索实现的统一门面。
 */
public interface CodeSearchFacade {

    /**
     * 检索仓库代码，并返回适合放入 Agent 上下文的代码片段。
     */
    List<CodeSearchResult> search(SearchRequest request);
}
