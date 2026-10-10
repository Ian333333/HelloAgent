package com.example.agent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 通用兜底 Agent：绑定了全部工具与会话记忆，
 * 大模型会根据用户输入自动判断是否需要调用工具。
 */
@Component
public class GeneralAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(GeneralAgent.class);
    private static final String DEFAULT_SESSION = "default";

    private final ChatClient chatClient;

    public GeneralAgent(@Qualifier("generalChatClient") ChatClient generalChatClient) {
        this.chatClient = generalChatClient;
    }

    @Override
    public AgentReply chat(String sessionId, String message) {
        String sid = (sessionId == null || sessionId.isBlank()) ? DEFAULT_SESSION : sessionId;
        log.info("session[{}] user: {}", sid, message);

        String reply = chatClient.prompt()
                .user(message)
                // 让记忆 Advisor 按会话 ID 存取上下文
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sid))
                .call()
                .content();

        log.info("session[{}] general assistant: {}", sid, reply);
        return new AgentReply(reply, "general");
    }
}
