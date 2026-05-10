package com.codepliot.search.dto;

import lombok.Data;

/**
 * grep/ripgrep 返回的单行匹配结果。
 */
@Data
public class GrepMatch {

    private String filePath;

    private Integer lineNumber;

    private Integer column;

    private String lineText;

    private String query;
}
