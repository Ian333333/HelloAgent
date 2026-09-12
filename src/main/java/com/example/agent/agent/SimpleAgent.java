package com.example.agent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

/**
 * 最简单的可运行 Agent：
 * 底层是绑定了工具与会话记忆的 ChatClient，
 * 大模型会根据用户输入自动判断是否需要调用工具。
 */
@Component
public class SimpleAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(SimpleAgent.class);
    private static final String DEFAULT_SESSION = "default";

    private final ChatClient chatClient;

    public SimpleAgent(ChatClient agentChatClient) {
        this.chatClient = agentChatClient;
    }

    @Override
    public String chat(String sessionId, String message) {
        String sid = (sessionId == null || sessionId.isBlank()) ? DEFAULT_SESSION : sessionId;
        log.info("session[{}] user: {}", sid, message);

        String reply = chatClient.prompt()
                .user(message)
                // 让记忆 Advisor 按会话 ID 存取上下文
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sid))
                .call()
                .content();

        log.info("session[{}] assistant: {}", sid, reply);
        return reply;
    }
}
