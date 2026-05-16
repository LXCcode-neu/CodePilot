package com.codepliot.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodePilotCli {

    private static final Set<String> TERMINAL_STATUSES = Set.of(
            "WAITING_CONFIRM",
            "COMPLETED",
            "FAILED",
            "VERIFY_FAILED",
            "CANCELLED"
    );
    private static final Set<String> FAILURE_STATUSES = Set.of("FAILED", "VERIFY_FAILED", "CANCELLED");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CliConfigStore configStore = new CliConfigStore(objectMapper);

    public static void main(String[] args) {
        int exitCode = new CodePilotCli().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    int run(String[] args) {
        try {
            execute(args);
            return 0;
        } catch (CliException exception) {
            System.err.println(exception.getMessage());
            return exception.exitCode();
        } catch (RuntimeException exception) {
            System.err.println("CLI 执行失败: " + exception.getMessage());
            return 1;
        }
    }

    private void execute(String[] args) {
        ParsedArgs parsedArgs = ParsedArgs.parse(args);
        CliConfig config = configStore.load();
        if (parsedArgs.serverOverride() != null) {
            config = config.withServerUrl(parsedArgs.serverOverride());
        }
        CliPrinter printer = new CliPrinter(objectMapper, config.jsonMode(parsedArgs.jsonMode()));
        CodePilotApiClient apiClient = new CodePilotApiClient(objectMapper, config);

        if (parsedArgs.commands().isEmpty() || parsedArgs.hasFlag("help") || "help".equals(parsedArgs.command(0))) {
            printHelp();
            return;
        }

        String group = parsedArgs.command(0);
        switch (group) {
            case "config" -> handleConfig(parsedArgs, config, printer);
            case "auth" -> handleAuth(parsedArgs, config, apiClient, printer);
            case "project" -> handleProject(parsedArgs, apiClient, printer);
            case "task" -> handleTask(parsedArgs, apiClient, printer);
            case "patch" -> handlePatch(parsedArgs, apiClient, printer);
            case "pr" -> handlePr(parsedArgs, apiClient, printer);
            case "version" -> handleVersion(apiClient, printer);
            default -> throw usage("未知命令: " + group);
        }
    }

    private void handleConfig(ParsedArgs args, CliConfig config, CliPrinter printer) {
        String action = args.command(1);
        if ("set-server".equals(action)) {
            String serverUrl = args.requireValue("url", 2, "请提供服务地址，例如 codepilot config set-server http://localhost:8080");
            CliConfig updated = config.withServerUrl(serverUrl);
            configStore.save(updated);
            printer.raw("服务地址已更新: " + updated.effectiveServerUrl());
            return;
        }
        if ("show".equals(action)) {
            printer.config(config, configStore.configPath().toString());
            return;
        }
        throw usage("用法: codepilot config set-server <url> 或 codepilot config show");
    }

    private void handleAuth(ParsedArgs args, CliConfig config, CodePilotApiClient apiClient, CliPrinter printer) {
        String action = args.command(1);
        if ("login".equals(action)) {
            String token = args.flagValue("token");
            if (token == null || token.isBlank()) {
                String username = args.flagValue("username");
                String password = args.flagValue("password");
                if (username == null || password == null) {
                    throw usage("用法: codepilot auth login --token <token>，或 --username <用户名> --password <密码>");
                }
                JsonNode result = apiClient.login(username, password);
                token = result.path("data").path("token").asText("");
                if (token.isBlank()) {
                    throw new CliException(3, "登录成功响应里没有 token");
                }
            }
            configStore.save(config.withAccessToken(token));
            printer.raw("登录信息已保存。");
            return;
        }
        if ("logout".equals(action)) {
            configStore.save(config.withoutAccessToken());
            printer.raw("已退出登录。");
            return;
        }
        if ("status".equals(action)) {
            if (config.accessToken() == null || config.accessToken().isBlank()) {
                printer.raw("当前未登录。");
                return;
            }
            printer.commandMap(apiClient.get("/api/user/me"));
            return;
        }
        throw usage("用法: codepilot auth login|logout|status");
    }

    private void handleProject(ParsedArgs args, CodePilotApiClient apiClient, CliPrinter printer) {
        String action = args.command(1);
        if ("list".equals(action)) {
            printer.projectList(apiClient.get("/api/projects"));
            return;
        }
        if ("show".equals(action)) {
            String id = args.requireValue("projectId", 2, "请提供项目 ID");
            printer.projectDetail(apiClient.get("/api/projects/" + id));
            return;
        }
        throw usage("用法: codepilot project list 或 codepilot project show <projectId>");
    }

    private void handleTask(ParsedArgs args, CodePilotApiClient apiClient, CliPrinter printer) {
        String action = args.command(1);
        switch (action) {
            case "create" -> {
                Long projectId = parseLong(requiredFlag(args, "project"), "project");
                String title = requiredFlag(args, "title");
                String desc = requiredFlag(args, "desc");
                ObjectNode body = objectMapper.createObjectNode();
                body.put("projectId", projectId);
                body.put("issueTitle", title);
                body.put("issueDescription", desc);
                JsonNode result = apiClient.post("/api/tasks", body);
                if (printer.jsonMode()) {
                    printer.json(result);
                } else {
                    JsonNode task = result.path("data");
                    System.out.printf("任务已创建: #%s [%s] %s%n",
                            task.path("id").asText("-"),
                            CliPrinter.status(task.path("status").asText("")),
                            task.path("issueTitle").asText(""));
                }
            }
            case "list" -> printer.taskList(apiClient.get("/api/tasks"));
            case "show" -> printer.taskDetail(apiClient.get("/api/tasks/" + args.requireValue("taskId", 2, "请提供任务 ID")));
            case "steps" -> printer.steps(apiClient.get("/api/tasks/" + args.requireValue("taskId", 2, "请提供任务 ID") + "/steps"));
            case "run" -> printer.success("任务已提交运行。", apiClient.post("/api/tasks/" + args.requireValue("taskId", 2, "请提供任务 ID") + "/run"));
            case "cancel" -> printer.success("已请求取消任务。", apiClient.post("/api/tasks/" + args.requireValue("taskId", 2, "请提供任务 ID") + "/cancel"));
            case "watch" -> watchTask(args, apiClient, printer);
            default -> throw usage("用法: codepilot task create|list|show|steps|watch|run|cancel");
        }
    }

    private void handlePatch(ParsedArgs args, CodePilotApiClient apiClient, CliPrinter printer) {
        String action = args.command(1);
        if ("show".equals(action)) {
            printer.patch(apiClient.get("/api/tasks/" + args.requireValue("taskId", 2, "请提供任务 ID") + "/patch"));
            return;
        }
        if ("confirm".equals(action)) {
            printer.success("Patch 已确认。", apiClient.post("/api/tasks/" + args.requireValue("taskId", 2, "请提供任务 ID") + "/confirm"));
            return;
        }
        throw usage("用法: codepilot patch show <taskId> 或 codepilot patch confirm <taskId>");
    }

    private void handlePr(ParsedArgs args, CodePilotApiClient apiClient, CliPrinter printer) {
        String action = args.command(1);
        if ("submit".equals(action)) {
            JsonNode result = apiClient.post("/api/tasks/" + args.requireValue("taskId", 2, "请提供任务 ID") + "/patch/pull-request");
            if (printer.jsonMode()) {
                printer.json(result);
                return;
            }
            JsonNode pr = result.path("data");
            System.out.println("Pull Request 已提交。");
            System.out.println("编号: #" + pr.path("number").asText("-"));
            System.out.println("分支: " + pr.path("branch").asText("-"));
            System.out.println("地址: " + pr.path("url").asText("-"));
            return;
        }
        throw usage("用法: codepilot pr submit <taskId>");
    }

    private void handleVersion(CodePilotApiClient apiClient, CliPrinter printer) {
        ObjectNode version = objectMapper.createObjectNode();
        version.put("cliVersion", "0.1.0");
        version.put("serverUrl", configStore.load().effectiveServerUrl());
        if (printer.jsonMode()) {
            printer.json(version);
            return;
        }
        System.out.println("CodePilot CLI 版本: 0.1.0");
        System.out.println("后端地址: " + version.path("serverUrl").asText());
    }

    private void watchTask(ParsedArgs args, CodePilotApiClient apiClient, CliPrinter printer) {
        String taskId = args.requireValue("taskId", 2, "请提供任务 ID");
        long intervalSeconds = parseLong(args.flagValueOrDefault("interval", "2"), "interval");
        long timeoutSeconds = parseLong(args.flagValueOrDefault("timeout", "1800"), "timeout");
        long deadline = System.nanoTime() + Duration.ofSeconds(Math.max(timeoutSeconds, 1)).toNanos();
        String lastStatus = "";
        while (System.nanoTime() < deadline) {
            JsonNode taskResult = apiClient.get("/api/tasks/" + taskId);
            JsonNode task = taskResult.path("data");
            String status = task.path("status").asText("");
            if (printer.jsonMode()) {
                printer.json(taskResult);
            } else if (!status.equals(lastStatus)) {
                System.out.printf("任务 #%s 状态: %s%n", taskId, CliPrinter.status(status));
                String summary = task.path("resultSummary").asText("");
                if (!summary.isBlank()) {
                    System.out.println("结果摘要: " + summary);
                }
            }
            lastStatus = status;
            if (TERMINAL_STATUSES.contains(status)) {
                if (FAILURE_STATUSES.contains(status)) {
                    throw new CliException(3, "任务已结束，但状态为 " + CliPrinter.status(status));
                }
                return;
            }
            sleep(intervalSeconds);
        }
        throw new CliException(1, "等待任务超时");
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(Math.max(seconds, 1)).toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CliException(1, "等待被中断");
        }
    }

    private String requiredFlag(ParsedArgs args, String name) {
        String value = args.flagValue(name);
        if (value == null || value.isBlank()) {
            throw usage("缺少参数 --" + name);
        }
        return value;
    }

    private long parseLong(String value, String name) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw usage("参数 " + name + " 必须是数字: " + value);
        }
    }

    private CliException usage(String message) {
        return new CliException(2, message + "\n运行 codepilot help 查看用法。");
    }

    private void printHelp() {
        System.out.println("""
                CodePilot CLI

                用法:
                  codepilot config set-server <url>
                  codepilot config show
                  codepilot auth login --token <token>
                  codepilot auth login --username <用户名> --password <密码>
                  codepilot auth logout
                  codepilot auth status
                  codepilot project list
                  codepilot project show <projectId>
                  codepilot task create --project <id> --title <标题> --desc <描述>
                  codepilot task list
                  codepilot task show <taskId>
                  codepilot task steps <taskId>
                  codepilot task watch <taskId> [--interval 2] [--timeout 1800]
                  codepilot task run <taskId>
                  codepilot task cancel <taskId>
                  codepilot patch show <taskId>
                  codepilot patch confirm <taskId>
                  codepilot pr submit <taskId>
                  codepilot version

                全局参数:
                  --json              输出 JSON
                  --server <url>      临时覆盖后端地址
                """);
    }

    private record ParsedArgs(List<String> commands, Map<String, String> flags, boolean jsonMode, String serverOverride) {

        static ParsedArgs parse(String[] args) {
            List<String> commands = new ArrayList<>();
            Map<String, String> flags = new HashMap<>();
            boolean jsonMode = false;
            String serverOverride = null;
            List<String> tokens = Arrays.asList(args);
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if ("--json".equals(token)) {
                    jsonMode = true;
                    continue;
                }
                if ("--server".equals(token)) {
                    serverOverride = requireNext(tokens, ++i, "--server");
                    continue;
                }
                if (token.startsWith("--")) {
                    String name = token.substring(2);
                    String value = "true";
                    if (i + 1 < tokens.size() && !tokens.get(i + 1).startsWith("--")) {
                        value = tokens.get(++i);
                    }
                    flags.put(name, value);
                    continue;
                }
                commands.add(token);
            }
            return new ParsedArgs(List.copyOf(commands), Map.copyOf(flags), jsonMode, serverOverride);
        }

        String command(int index) {
            return index >= 0 && index < commands.size() ? commands.get(index) : "";
        }

        boolean hasFlag(String name) {
            return flags.containsKey(name);
        }

        String flagValue(String name) {
            return flags.get(name);
        }

        String flagValueOrDefault(String name, String fallback) {
            String value = flags.get(name);
            return value == null || value.isBlank() ? fallback : value;
        }

        String requireValue(String name, int index, String message) {
            String value = command(index);
            if (value == null || value.isBlank()) {
                throw new CliException(2, message + "\n运行 codepilot help 查看用法。");
            }
            return value;
        }

        private static String requireNext(List<String> tokens, int index, String flagName) {
            if (index >= tokens.size()) {
                throw new CliException(2, "缺少参数 " + flagName + " 的值");
            }
            return tokens.get(index);
        }
    }
}
