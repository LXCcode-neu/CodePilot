package com.codepliot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * CodeFile 实体类，用于映射数据库表或持久化结构。
 */
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

