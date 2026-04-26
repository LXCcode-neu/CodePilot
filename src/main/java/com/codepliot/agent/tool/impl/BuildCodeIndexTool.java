package com.codepliot.agent.tool.impl;

import com.codepliot.agent.context.AgentContext;
import com.codepliot.agent.tool.AgentTool;
import com.codepliot.agent.tool.ToolResult;
import com.codepliot.index.dto.CodeIndexBuildResult;
import com.codepliot.index.dto.CodeIndexToolResult;
import com.codepliot.index.lucene.LuceneCodeIndexService;
import com.codepliot.index.service.CodeIndexBuildService;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.trace.entity.AgentStepType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 真实代码索引构建工具。
 */
@Component
@Order(20)
public class BuildCodeIndexTool implements AgentTool {

    private final CodeIndexBuildService codeIndexBuildService;
    private final LuceneCodeIndexService luceneCodeIndexService;

    public BuildCodeIndexTool(CodeIndexBuildService codeIndexBuildService,
                              LuceneCodeIndexService luceneCodeIndexService) {
        this.codeIndexBuildService = codeIndexBuildService;
        this.luceneCodeIndexService = luceneCodeIndexService;
    }

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.INDEXING;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.BUILD_CODE_INDEX;
    }

    @Override
    public String stepName() {
        return "构建代码索引";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        CodeIndexBuildResult buildResult = codeIndexBuildService.build(context.projectId());
        int indexDocCount = luceneCodeIndexService.rebuildProjectIndex(context.projectId());
        CodeIndexToolResult result = new CodeIndexToolResult(
                buildResult.fileCount(),
                buildResult.symbolCount(),
                indexDocCount,
                buildResult.warningCount(),
                buildResult.languageStats()
        );
        return ToolResult.success("code index build completed", result);
    }
}
