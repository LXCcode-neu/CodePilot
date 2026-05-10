package com.codepliot.search.dto;

import lombok.Data;

/**
 * 检索匹配附近带行号的文件片段。
 */
@Data
public class CodeSnippet {

    private String filePath;

    private Integer startLine;

    private Integer endLine;

    private String contentWithLineNumbers;
}
