package com.example.agent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 日期时间专家 Agent：只绑定日期时间工具，回答时间相关问题。
 */
@Component
public class DateTimeAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(DateTimeAgent.class);
    private static final String DEFAULT_SESSION = "default";

    private final ChatClient chatClient;

    public DateTimeAgent(@Qualifier("dateTimeChatClient") ChatClient dateTimeChatClient) {
        this.chatClient = dateTimeChatClient;
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

        log.info("session[{}] datetime assistant: {}", sid, reply);
        return new AgentReply(reply, "datetime");
    }
}
