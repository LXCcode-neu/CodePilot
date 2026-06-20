package com.codepliot.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 补丁验证配置属性。
 * <p>
 * 绑定 {@code codepilot.verification} 前缀下的配置项，控制补丁验证阶段的
 * 命令超时、最大修复尝试次数及自定义验证命令列表。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "codepilot.verification")
public class PatchVerificationProperties {

    /** 验证命令执行超时时间（秒），默认 300 秒 */
    private int commandTimeoutSeconds = 300;
    /** 补丁修复最大尝试次数，默认 3 次 */
    private int maxRepairAttempts = 3;
    /** 是否启用自动检测验证命令，默认关闭 */
    private boolean autoDetectCommandsEnabled = false;
    /** 自定义验证命令列表 */
    private List<Command> commands = new ArrayList<>();

    public int getCommandTimeoutSeconds() {
        return commandTimeoutSeconds;
    }

    public void setCommandTimeoutSeconds(int commandTimeoutSeconds) {
        this.commandTimeoutSeconds = commandTimeoutSeconds;
    }

    public int getMaxRepairAttempts() {
        return maxRepairAttempts;
    }

    public void setMaxRepairAttempts(int maxRepairAttempts) {
        this.maxRepairAttempts = maxRepairAttempts;
    }

    public boolean isAutoDetectCommandsEnabled() {
        return autoDetectCommandsEnabled;
    }

    public void setAutoDetectCommandsEnabled(boolean autoDetectCommandsEnabled) {
        this.autoDetectCommandsEnabled = autoDetectCommandsEnabled;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands == null ? new ArrayList<>() : commands;
    }

    /**
     * 验证命令配置内部类。
     * <p>
     * 定义单条验证命令的名称、执行内容及工作目录。
     * </p>
     */
    public static class Command {

        /** 命令名称，用于展示和标识 */
        private String name = "";
        /** 实际执行的命令内容 */
        private String command = "";
        /** 命令执行的工作目录，默认为当前目录 */
        private String workingDirectory = ".";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public void setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
        }
    }
}
