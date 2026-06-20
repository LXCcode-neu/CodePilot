package com.codepliot.model;

/**
 * 通知操作执行结果。
 * <p>用户点击通知中的操作按钮后，后端执行对应动作的返回结果。</p>
 */
public record NotificationActionExecutionResult(
        /** 是否执行成功 */
        boolean success,
        /** 结果标题 */
        String title,
        /** 结果描述信息 */
        String message,
        /** 相关链接地址 */
        String linkUrl
) {
}
