package com.codepliot.search.dto;

import lombok.Data;

/**
 * Single line match returned by grep/ripgrep.
 */
@Data
public class GrepMatch {

    private String filePath;

    private Integer lineNumber;

    private Integer column;

    private String lineText;

    private String query;
}
