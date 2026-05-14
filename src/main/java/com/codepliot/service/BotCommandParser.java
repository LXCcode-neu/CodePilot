package com.codepliot.service;

import com.codepliot.model.BotCommand;
import com.codepliot.model.BotCommandType;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class BotCommandParser {

    private static final Pattern ACTION_CODE_PATTERN = Pattern.compile("\\bCP-[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE);

    public Optional<BotCommand> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(text);
        Matcher codeMatcher = ACTION_CODE_PATTERN.matcher(normalized);
        if (!codeMatcher.find()) {
            return Optional.empty();
        }
        String actionCode = codeMatcher.group().toUpperCase();
        BotCommandType type = resolveType(normalized);
        if (type == null) {
            return Optional.empty();
        }
        return Optional.of(new BotCommand(type, actionCode, text));
    }

    private BotCommandType resolveType(String normalized) {
        String compact = normalized.replace(" ", "");
        if (containsAny(compact, "确认PR", "提交PR", "创建PR", "提PR")
                || containsAny(normalized, "confirm pr", "submit pr", "create pr", "pr ")) {
            return BotCommandType.CONFIRM_PR;
        }
        if (containsAny(compact, "修复", "处理", "开始修")
                || containsAny(normalized, "fix", "run", "repair")) {
            return BotCommandType.RUN_FIX;
        }
        if (containsAny(compact, "忽略", "跳过", "不处理")
                || containsAny(normalized, "ignore", "skip")) {
            return BotCommandType.IGNORE;
        }
        if (containsAny(compact, "状态", "进度", "查一下")
                || containsAny(normalized, "status", "progress")) {
            return BotCommandType.STATUS;
        }
        if (containsAny(compact, "放弃", "取消", "停止")
                || containsAny(normalized, "cancel", "abort")) {
            return BotCommandType.CANCEL;
        }
        return null;
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text
                .replaceAll("@\\S+", " ")
                .replace('\u00A0', ' ')
                .trim()
                .toLowerCase();
    }
}
