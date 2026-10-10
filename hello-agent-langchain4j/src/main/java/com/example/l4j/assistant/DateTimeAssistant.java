package com.example.l4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 日期时间专家 Assistant：只绑日期时间工具。
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"dateTimeTools"})
public interface DateTimeAssistant {

    @SystemMessage("""
            你是 HelloAgent 的日期时间专家。规则：
            - 回答当前时间、日期、星期几时必须使用日期时间工具获取真实值，不要猜测
            - 涉及日期推算（如"三天后是星期几"）先取当前值再推算
            - 用简洁自然的中文回答
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
