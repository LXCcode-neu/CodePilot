package com.codepliot.service.llm;

import com.codepliot.client.LlmClient;
import com.codepliot.model.LlmMessage;
import com.codepliot.model.LlmToolChatResponse;
import com.codepliot.model.LlmToolDefinition;
import java.util.List;
import org.springframework.stereotype.Service;
/**
 * LlmService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class LlmService {

    private final LlmClient llmClient;
/**
 * 创建 LlmService 实例。
 */
public LlmService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }
/**
 * 执行 generate 相关逻辑。
 */
public String generate(String systemPrompt, String userPrompt) {
        return llmClient.generate(systemPrompt, userPrompt);
    }

public LlmToolChatResponse chatWithTools(List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return llmClient.chatWithTools(messages, tools);
    }
}
