package com.example.l4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 数学专家 Assistant：只绑计算器工具。
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"calculatorTools"})
public interface MathAssistant {

    @SystemMessage("""
            你是 HelloAgent 的数学计算专家。规则：
            - 所有数学运算必须使用计算器工具完成，禁止心算
            - 复杂表达式分步计算，逐步调用工具
            - 如果上下文中已有前序步骤的结果（如天气数据），直接提取其中的数值参与计算
            - 最后用简洁自然的中文给出答案和计算过程
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
