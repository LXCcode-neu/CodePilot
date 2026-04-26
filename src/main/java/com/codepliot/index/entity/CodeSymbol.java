package com.codepliot.index.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 符号级索引实体。
 * 对应 code_symbol 表，保存类、方法、函数等抽取后的结构化信息。
 */
@Data
@TableName("code_symbol")
@EqualsAndHashCode(callSuper = true)
public class CodeSymbol extends BaseEntity {

    @TableField("project_id")
    private Long projectId;

    @TableField("file_id")
    private Long fileId;

    @TableField("language")
    private String language;

    @TableField("file_path")
    private String filePath;

    @TableField("symbol_type")
    private String symbolType;

    @TableField("symbol_name")
    private String symbolName;

    @TableField("parent_symbol")
    private String parentSymbol;

    @TableField("signature")
    private String signature;

    @TableField("annotations")
    private String annotations;

    @TableField("route_path")
    private String routePath;

    @TableField("import_text")
    private String importText;

    @TableField("start_line")
    private Integer startLine;

    @TableField("end_line")
    private Integer endLine;

    @TableField("content")
    private String content;
}
