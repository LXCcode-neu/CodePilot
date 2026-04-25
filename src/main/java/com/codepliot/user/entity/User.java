package com.codepliot.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codepliot.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("user")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("email")
    private String email;
}
