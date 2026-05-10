package com.codepliot.search.dto;

import lombok.Data;

/**
 * 代码检索门面返回的排序代码片段。
 */
@Data
public class CodeSearchResult {

    private String filePath;

    private Integer startLine;

    private Integer endLine;

    private Double score;

    private String reason;

    private String contentWithLineNumbers;
}
