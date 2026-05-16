package com.codepliot.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;

public class CliPrinter {

    private final ObjectMapper objectMapper;
    private final boolean jsonMode;

    public CliPrinter(ObjectMapper objectMapper, boolean jsonMode) {
        this.objectMapper = objectMapper;
        this.jsonMode = jsonMode;
    }

    public boolean jsonMode() {
        return jsonMode;
    }

    public void json(JsonNode node) {
        try {
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
        } catch (JsonProcessingException exception) {
            throw new CliException(1, "输出 JSON 失败: " + exception.getMessage());
        }
    }

    public void config(CliConfig config, String configPath) {
        if (jsonMode) {
            var node = objectMapper.createObjectNode();
            node.put("serverUrl", config.effectiveServerUrl());
            node.put("hasAccessToken", config.accessToken() != null && !config.accessToken().isBlank());
            if (config.defaultProjectId() != null) {
                node.put("defaultProjectId", config.defaultProjectId());
            }
            node.put("outputMode", config.outputMode() == null ? "text" : config.outputMode());
            node.put("configPath", configPath);
            json(node);
            return;
        }
        System.out.println("CodePilot CLI 配置");
        System.out.println("服务地址: " + config.effectiveServerUrl());
        System.out.println("登录状态: " + (config.accessToken() == null || config.accessToken().isBlank() ? "未登录" : "已保存 Token"));
        if (config.defaultProjectId() != null) {
            System.out.println("默认项目: " + config.defaultProjectId());
        }
        System.out.println("配置文件: " + configPath);
    }

    public void projectList(JsonNode result) {
        if (jsonMode) {
            json(result);
            return;
        }
        JsonNode data = result.path("data");
        if (!data.isArray() || data.isEmpty()) {
            System.out.println("暂无项目。");
            return;
        }
        System.out.println("项目列表");
        for (JsonNode item : data) {
            System.out.printf("#%s  %s  %s%n",
                    item.path("id").asText("-"),
                    item.path("repoName").asText("(未命名项目)"),
                    item.path("repoUrl").asText(""));
        }
    }

    public void projectDetail(JsonNode result) {
        if (jsonMode) {
            json(result);
            return;
        }
        JsonNode project = result.path("data");
        System.out.println("项目详情");
        printField("项目 ID", project.path("id").asText("-"));
        printField("项目名称", project.path("repoName").asText(""));
        printField("仓库地址", project.path("repoUrl").asText(""));
        printOptional("GitHub Owner", project.path("githubOwner").asText(""));
        printOptional("GitHub Repo", project.path("githubRepoName").asText(""));
        printOptional("默认分支", project.path("defaultBranch").asText(""));
        printField("状态", project.path("status").asText("-"));
        printOptional("本地路径", project.path("localPath").asText(""));
    }

    public void taskList(JsonNode result) {
        if (jsonMode) {
            json(result);
            return;
        }
        JsonNode data = result.path("data");
        if (!data.isArray() || data.isEmpty()) {
            System.out.println("暂无任务。");
            return;
        }
        System.out.println("任务列表");
        for (JsonNode item : data) {
            System.out.printf("#%s  [%s]  %s%n",
                    item.path("id").asText("-"),
                    status(item.path("status").asText("")),
                    item.path("issueTitle").asText("(无标题)"));
        }
    }

    public void taskDetail(JsonNode result) {
        if (jsonMode) {
            json(result);
            return;
        }
        JsonNode task = result.path("data");
        System.out.println("任务详情");
        printField("任务 ID", task.path("id").asText("-"));
        printField("项目 ID", task.path("projectId").asText("-"));
        printField("状态", status(task.path("status").asText("")));
        printField("标题", task.path("issueTitle").asText(""));
        printField("来源", source(task.path("sourceType").asText("")));
        printField("模型", task.path("llmDisplayName").asText(task.path("llmModelName").asText("")));
        printOptional("结果摘要", task.path("resultSummary").asText(""));
        printOptional("错误信息", task.path("errorMessage").asText(""));
        printOptional("创建时间", task.path("createdAt").asText(""));
        printOptional("更新时间", task.path("updatedAt").asText(""));
    }

    public void steps(JsonNode result) {
        if (jsonMode) {
            json(result);
            return;
        }
        JsonNode data = result.path("data");
        if (!data.isArray() || data.isEmpty()) {
            System.out.println("暂无执行步骤。");
            return;
        }
        System.out.println("执行步骤");
        int index = 1;
        for (JsonNode step : data) {
            System.out.printf("%d. [%s] %s%n",
                    index++,
                    status(step.path("status").asText("")),
                    stepName(step.path("stepType").asText(""), step.path("stepName").asText("")));
            String error = step.path("errorMessage").asText("");
            if (!error.isBlank()) {
                System.out.println("   错误: " + error);
            }
        }
    }

    public void patch(JsonNode result) {
        if (jsonMode) {
            json(result);
            return;
        }
        JsonNode patch = result.path("data");
        System.out.println("Patch 信息");
        printField("Patch ID", patch.path("id").asText("-"));
        printField("任务 ID", patch.path("taskId").asText("-"));
        printField("确认状态", patch.path("confirmed").asBoolean(false) ? "已确认" : "未确认");
        printField("PR 状态", patch.path("prSubmitted").asBoolean(false) ? "已提交" : "未提交");
        printOptional("PR 地址", patch.path("prUrl").asText(""));
        printOptional("方案", patch.path("solution").asText(""));
        printOptional("风险", patch.path("risk").asText(""));
        JsonNode fileChanges = patch.path("fileChanges");
        if (fileChanges.isArray() && !fileChanges.isEmpty()) {
            System.out.println("变更文件");
            for (JsonNode file : fileChanges) {
                System.out.printf("- %s (+%s/-%s)%n",
                        file.path("filePath").asText(""),
                        file.path("addedLines").asText("0"),
                        file.path("removedLines").asText("0"));
            }
        }
        String patchText = patch.path("patch").asText("");
        if (!patchText.isBlank()) {
            System.out.println();
            System.out.println(patchText);
        }
    }

    public void success(String message, JsonNode result) {
        if (jsonMode) {
            json(result);
            return;
        }
        System.out.println(message);
    }

    public void raw(String message) {
        if (!jsonMode) {
            System.out.println(message);
        }
    }

    public void commandMap(JsonNode result) {
        if (jsonMode) {
            json(result);
            return;
        }
        JsonNode data = result.path("data");
        if (data.isObject()) {
            Iterator<String> names = data.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                JsonNode value = data.path(name);
                if (!value.isContainerNode()) {
                    printField(name, value.asText(""));
                }
            }
        } else {
            System.out.println(result.path("message").asText("操作完成"));
        }
    }

    private void printField(String name, String value) {
        System.out.println(name + ": " + (value == null || value.isBlank() ? "-" : value));
    }

    private void printOptional(String name, String value) {
        if (value != null && !value.isBlank()) {
            printField(name, value);
        }
    }

    public static String status(String value) {
        return switch (value == null ? "" : value) {
            case "PENDING" -> "待运行";
            case "CLONING_REPOSITORY" -> "克隆仓库";
            case "SEARCHING_CODE" -> "检索代码";
            case "ANALYZING_ISSUE" -> "分析问题";
            case "GENERATING_PATCH" -> "生成 Patch";
            case "VERIFYING_PATCH" -> "验证 Patch";
            case "REPAIRING_PATCH" -> "修复 Patch";
            case "REVIEWING_PATCH" -> "AI 审查";
            case "WAITING_CONFIRM" -> "等待确认";
            case "VERIFY_FAILED" -> "验证失败";
            case "COMPLETED" -> "已完成";
            case "FAILED" -> "失败";
            case "CANCEL_REQUESTED" -> "取消中";
            case "CANCELLED" -> "已取消";
            case "SUCCESS" -> "成功";
            case "RUNNING" -> "运行中";
            default -> value == null || value.isBlank() ? "-" : value;
        };
    }

    private String source(String value) {
        return switch (value == null ? "" : value) {
            case "GITHUB_ISSUE" -> "GitHub Issue";
            case "SENTRY_ALERT" -> "Sentry 告警";
            case "MANUAL" -> "手动创建";
            default -> value == null || value.isBlank() ? "-" : value;
        };
    }

    private String stepName(String stepType, String fallback) {
        return switch (stepType == null ? "" : stepType) {
            case "CLONE_REPOSITORY" -> "克隆仓库";
            case "SEARCH_RELEVANT_CODE" -> "检索相关代码";
            case "ANALYZE_ISSUE" -> "分析问题";
            case "GENERATE_PATCH" -> "生成 Patch";
            case "VERIFY_PATCH" -> "验证 Patch";
            case "REPAIR_PATCH" -> "修复 Patch";
            case "REVIEW_PATCH" -> "AI 代码审查";
            default -> fallback == null || fallback.isBlank() ? stepType : fallback;
        };
    }
}
