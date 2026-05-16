package com.codepliot.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "codepilot.verification")
public class PatchVerificationProperties {

    private int commandTimeoutSeconds = 300;
    private int maxRepairAttempts = 3;
    private boolean autoDetectCommandsEnabled = false;
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

    public static class Command {

        private String name = "";
        private String command = "";
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
