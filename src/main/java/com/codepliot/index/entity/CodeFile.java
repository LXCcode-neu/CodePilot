package com.codepliot.index.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("code_file")
@EqualsAndHashCode(callSuper = true)
public class CodeFile extends BaseEntity {

    @TableField("project_id")
    private Long projectId;

    @TableField("file_path")
    private String filePath;

    @TableField("language")
    private String language;

    @TableField("package_name")
    private String packageName;

    @TableField("module_name")
    private String moduleName;

    @TableField("class_name")
    private String className;

    @TableField("content_hash")
    private String contentHash;

    @TableField("size")
    private Long size;

    @TableField("parse_status")
    private String parseStatus;

    @TableField("parse_error")
    private String parseError;
}
