package com.codepliot.controller;

import com.codepliot.exception.BusinessException;
import com.codepliot.model.NotificationActionExecutionResult;
import com.codepliot.service.NotificationActionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification-actions")
public class NotificationActionController {

    private final NotificationActionService notificationActionService;

    public NotificationActionController(NotificationActionService notificationActionService) {
        this.notificationActionService = notificationActionService;
    }

    @GetMapping(value = "/{token}/{action}", produces = MediaType.TEXT_HTML_VALUE)
    public String confirm(@PathVariable String token, @PathVariable String action) {
        String actionName = switch (normalizeAction(action)) {
            case "run" -> "执行修复";
            case "ignore" -> "忽略 Issue";
            default -> throw new BusinessException(com.codepliot.model.ErrorCode.BAD_REQUEST, "Unsupported notification action");
        };
        return page(
                "确认操作",
                "请确认是否要" + actionName + "。",
                """
                        <form method="post">
                          <button class="primary" type="submit">%s</button>
                        </form>
                        """.formatted(escape(actionName)),
                null
        );
    }

    @PostMapping(value = "/{token}/run", produces = MediaType.TEXT_HTML_VALUE)
    public String run(@PathVariable String token) {
        return resultPage(notificationActionService.runFix(token));
    }

    @PostMapping(value = "/{token}/ignore", produces = MediaType.TEXT_HTML_VALUE)
    public String ignore(@PathVariable String token) {
        return resultPage(notificationActionService.ignore(token));
    }

    private String resultPage(NotificationActionExecutionResult result) {
        String link = result.linkUrl() == null || result.linkUrl().isBlank()
                ? ""
                : "<a class=\"secondary\" href=\"" + escape(result.linkUrl()) + "\">打开 CodePilot</a>";
        return page(result.title(), result.message(), link, result.linkUrl());
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toLowerCase();
    }

    private String page(String title, String message, String body, String linkUrl) {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                  <style>
                    body { margin: 0; min-height: 100vh; display: grid; place-items: center; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f8fafc; color: #0f172a; }
                    main { width: min(92vw, 520px); border: 1px solid #e2e8f0; border-radius: 16px; background: white; padding: 28px; box-shadow: 0 18px 45px rgba(15, 23, 42, .08); }
                    h1 { margin: 0 0 12px; font-size: 24px; line-height: 1.25; }
                    p { margin: 0 0 24px; color: #475569; line-height: 1.7; }
                    form { margin: 0; }
                    button, a { display: inline-flex; align-items: center; justify-content: center; min-height: 40px; padding: 0 16px; border-radius: 10px; font-size: 14px; font-weight: 700; text-decoration: none; cursor: pointer; }
                    .primary { border: 0; background: #0f172a; color: white; }
                    .secondary { border: 1px solid #cbd5e1; background: white; color: #0f172a; }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>%s</h1>
                    <p>%s</p>
                    %s
                  </main>
                </body>
                </html>
                """.formatted(
                escape(title),
                escape(title),
                escape(message),
                body == null ? "" : body
        );
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
