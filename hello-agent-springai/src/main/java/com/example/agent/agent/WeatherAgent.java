package com.example.agent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 天气专家 Agent：只绑定天气工具，查询实时天气并明确给出数值
 * （气温、湿度、风速），便于下游 Agent 继续计算。
 */
@Component
public class WeatherAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(WeatherAgent.class);
    private static final String DEFAULT_SESSION = "default";

    private final ChatClient chatClient;

    public WeatherAgent(@Qualifier("weatherChatClient") ChatClient weatherChatClient) {
        this.chatClient = weatherChatClient;
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

        log.info("session[{}] weather assistant: {}", sid, reply);
        return new AgentReply(reply, "weather");
    }
}
