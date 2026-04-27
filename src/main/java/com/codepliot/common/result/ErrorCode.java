package com.codepliot.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "Success"),
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Resource not found"),
    VALIDATION_ERROR(422, "Validation error"),
    USERNAME_ALREADY_EXISTS(1001, "Username already exists"),
    USER_NOT_FOUND(1002, "User not found"),
    USERNAME_OR_PASSWORD_ERROR(1003, "Username or password is incorrect"),
    INVALID_TOKEN(1004, "Invalid or expired token"),
    PROJECT_REPO_NOT_FOUND(2001, "Project repository not found"),
    INVALID_GITHUB_REPO_URL(2002, "Invalid GitHub repository URL"),
    AGENT_TASK_NOT_FOUND(3001, "Agent task not found"),
    AGENT_STEP_NOT_FOUND(3002, "Agent step not found"),
    AGENT_TASK_RUNNING(3003, "Agent task is already running"),
    PATCH_RECORD_NOT_FOUND(4001, "Patch record not found"),
    INTERNAL_ERROR(500, "Internal server error");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
