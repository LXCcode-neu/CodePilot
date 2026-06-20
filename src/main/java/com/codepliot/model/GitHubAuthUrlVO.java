package com.codepliot.model;

/**
 * GitHub OAuth 授权 URL 视图对象。
 * <p>用于返回 GitHub OAuth 授权跳转地址，引导用户完成授权流程。</p>
 *
 * @param url GitHub OAuth 授权页面的完整 URL
 */
public record GitHubAuthUrlVO(String url) {
}
