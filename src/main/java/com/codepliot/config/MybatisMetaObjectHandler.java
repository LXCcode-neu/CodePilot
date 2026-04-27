package com.codepliot.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
/**
 * MybatisMetaObjectHandler 配置类，负责注册或绑定应用配置。
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {
    /**
     * 新增Fill相关逻辑。
     */
@Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }
    /**
     * 更新Fill相关逻辑。
     */
@Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}

