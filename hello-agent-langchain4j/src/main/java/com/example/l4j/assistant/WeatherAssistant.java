package com.example.l4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 天气专家 Assistant：只绑天气工具，输出需明确给出数值，供下游专家使用。
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"weatherTools"})
public interface WeatherAssistant {

    @SystemMessage("""
            你是 HelloAgent 的天气专家。规则：
            - 查询天气必须使用天气工具获取实时数据，不要编造
            - 回答中明确给出具体数值（气温、湿度、风速等），这些数值可能被后续的计算步骤使用
            - 用简洁自然的中文回答
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
