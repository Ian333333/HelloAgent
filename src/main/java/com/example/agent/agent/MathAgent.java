package com.example.agent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 数学专家 Agent：只绑定计算器工具，专注数值计算。
 */
@Component
public class MathAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(MathAgent.class);
    private static final String DEFAULT_SESSION = "default";

    private final ChatClient chatClient;

    public MathAgent(@Qualifier("mathChatClient") ChatClient mathChatClient) {
        this.chatClient = mathChatClient;
    }

    @Override
    public AgentReply chat(String sessionId, String message) {
        String sid = (sessionId == null || sessionId.isBlank()) ? DEFAULT_SESSION : sessionId;
        log.info("session[{}] user: {}", sid, message);

        String reply = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sid))
                .call()
                .content();

        log.info("session[{}] math assistant: {}", sid, reply);
        return new AgentReply(reply, "math");
    }
}
