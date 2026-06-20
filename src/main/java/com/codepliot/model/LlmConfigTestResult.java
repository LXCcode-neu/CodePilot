package com.codepliot.model;

/**
 * LLM配置连通性测试结果。
 * <p>用于返回LLM API密钥配置的连接测试结果。</p>
 */
public record LlmConfigTestResult(
        /** 测试是否成功 */
        Boolean success,
        /** 测试结果描述信息 */
        String message
) {
}
