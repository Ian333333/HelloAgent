package com.example.l4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 通用兜底 Assistant：绑定全部三个工具。
 * 注意必须用 EXPLICIT 装配——AUTOMATIC 会把所有 @Tool Bean 都绑进来，破坏专家的工具隔离。
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"dateTimeTools", "calculatorTools", "weatherTools"})
public interface GeneralAssistant {

    @SystemMessage("""
            你是一个名为 HelloAgent 的智能助手，可以调用工具帮助用户解决问题：
            - 询问当前时间、日期、星期几：使用日期时间工具
            - 需要数学计算：使用计算器工具，不要自己心算
            - 询问某地天气：使用天气工具
            拿到工具返回结果后，用简洁自然的中文回答用户。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
