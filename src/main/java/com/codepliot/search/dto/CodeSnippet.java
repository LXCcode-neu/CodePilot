package com.codepliot.search.dto;

import lombok.Data;

/**
 * File snippet with line numbers around a search match.
 */
@Data
public class CodeSnippet {

    private String filePath;

    private Integer startLine;

    private Integer endLine;

    private String contentWithLineNumbers;
}
