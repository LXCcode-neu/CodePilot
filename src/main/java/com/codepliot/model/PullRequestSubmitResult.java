package com.codepliot.model;

import java.time.LocalDateTime;

/**
 * Pull Request提交结果。
 * <p>封装Pull Request成功创建后的返回信息，包含PR编号、链接等。</p>
 */
public record PullRequestSubmitResult(
        /** 关联的修复任务ID */
        Long taskId,
        /** Pull Request编号 */
        Integer number,
        /** Pull Request页面URL地址 */
        String url,
        /** 源分支名称 */
        String branch,
        /** 提交时间 */
        LocalDateTime submittedAt
) {
}
