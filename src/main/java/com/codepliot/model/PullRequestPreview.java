package com.codepliot.model;

import java.util.List;

/**
 * Pull Request预览对象。
 * <p>在正式提交PR之前，向前端展示即将创建的Pull Request的完整信息预览，
 * 包括标题、分支、变更统计等。</p>
 */
public record PullRequestPreview(
        /** Pull Request标题 */
        String title,
        /** 源分支名称 */
        String branchName,
        /** 提交信息 */
        String commitMessage,
        /** Pull Request正文描述 */
        String body,
        /** 变更文件数量 */
        Integer changedFiles,
        /** 新增代码行数 */
        Integer addedLines,
        /** 删除代码行数 */
        Integer removedLines,
        /** 涉及的文件路径列表 */
        List<String> touchedFiles,
        /** 是否已准备好提交 */
        Boolean ready,
        /** 当前状态描述 */
        String status
) {
}
