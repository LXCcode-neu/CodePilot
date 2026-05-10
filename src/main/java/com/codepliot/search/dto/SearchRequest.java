package com.codepliot.search.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 按需仓库代码检索请求对象。
 */
@Data
public class SearchRequest {

    private String repoPath;

    private String issueText;

    private String query;

    private Integer maxResults;

    private Integer contextBeforeLines;

    private Integer contextAfterLines;

    private List<String> globPatterns = new ArrayList<>();

    private boolean regexEnabled;
}
