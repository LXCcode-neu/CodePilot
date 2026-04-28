package com.codepliot.service.agent.tool;

import com.codepliot.model.AgentContext;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.ToolResult;
import com.codepliot.model.CodeIndexBuildResult;
import com.codepliot.model.CodeIndexToolResult;
import com.codepliot.service.index.lucene.LuceneCodeIndexService;
import com.codepliot.service.index.CodeIndexBuildService;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.AgentStepType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
/**
 * BuildCodeIndexTool 服务类，负责封装业务流程和领域能力。
 */
@Component
@Order(20)
public class BuildCodeIndexTool implements AgentTool {

    private final CodeIndexBuildService codeIndexBuildService;
    private final LuceneCodeIndexService luceneCodeIndexService;
/**
 * 创建 BuildCodeIndexTool 实例。
 */
public BuildCodeIndexTool(CodeIndexBuildService codeIndexBuildService,
                              LuceneCodeIndexService luceneCodeIndexService) {
        this.codeIndexBuildService = codeIndexBuildService;
        this.luceneCodeIndexService = luceneCodeIndexService;
    }
    /**
     * 执行 taskStatus 相关逻辑。
     */
@Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.INDEXING;
    }
    /**
     * 执行 stepType 相关逻辑。
     */
@Override
    public AgentStepType stepType() {
        return AgentStepType.BUILD_CODE_INDEX;
    }
    /**
     * 执行 stepName 相关逻辑。
     */
@Override
    public String stepName() {
        return "构建代码索引";
    }
    /**
     * 执行 execute 相关逻辑。
     */
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
