package com.codepliot.search.dto;

import lombok.Data;

/**
 * Ranked code snippet returned by the code search facade.
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
