package com.example.agent.config;

import com.example.agent.tools.CalculatorTools;
import com.example.agent.tools.DateTimeTools;
import com.example.agent.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 核心配置：
 * 1. ChatMemory：基于滑动窗口的会话记忆（每个会话保留最近 20 条消息），四个专家共享
 * 2. 按领域暴露 4 个 ChatClient（general / math / weather / dateTime），
 *    各自绑定独立的系统提示词与本领域工具，实现专家的上下文和工具隔离
 */
@Configuration
public class AgentConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    /** 通用兜底：全部工具 + 通用提示词。 */
    @Bean
    public ChatClient generalChatClient(ChatModel chatModel,
                                        ChatMemory chatMemory,
                                        DateTimeTools dateTimeTools,
                                        CalculatorTools calculatorTools,
                                        WeatherTools weatherTools) {
        String systemPrompt = """
                你是一个名为 HelloAgent 的智能助手，可以调用工具帮助用户解决问题：
                - 询问当前时间、日期、星期几：使用日期时间工具
                - 需要数学计算：使用计算器工具，不要自己心算
                - 询问某地天气：使用天气工具
                拿到工具返回结果后，用简洁自然的中文回答用户。
                """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(dateTimeTools, calculatorTools, weatherTools)
                .build();
    }

    /** 数学专家：只绑计算器。 */
    @Bean
    public ChatClient mathChatClient(ChatModel chatModel, ChatMemory chatMemory,
                                     CalculatorTools calculatorTools) {
        String systemPrompt = """
                你是 HelloAgent 的数学计算专家。规则：
                - 所有数学运算必须使用计算器工具完成，禁止心算
                - 复杂表达式分步计算，逐步调用工具
                - 如果上下文中已有前序步骤的结果（如天气数据），直接提取其中的数值参与计算
                - 最后用简洁自然的中文给出答案和计算过程
                """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(calculatorTools)
                .build();
    }

    /** 天气专家：只绑天气工具，输出需明确给出数值，供下游专家使用。 */
    @Bean
    public ChatClient weatherChatClient(ChatModel chatModel, ChatMemory chatMemory,
                                        WeatherTools weatherTools) {
        String systemPrompt = """
                你是 HelloAgent 的天气专家。规则：
                - 查询天气必须使用天气工具获取实时数据，不要编造
                - 回答中明确给出具体数值（气温、湿度、风速等），这些数值可能被后续的计算步骤使用
                - 用简洁自然的中文回答
                """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(weatherTools)
                .build();
    }

    /** 日期时间专家：只绑日期时间工具。 */
    @Bean
    public ChatClient dateTimeChatClient(ChatModel chatModel, ChatMemory chatMemory,
                                         DateTimeTools dateTimeTools) {
        String systemPrompt = """
                你是 HelloAgent 的日期时间专家。规则：
                - 回答当前时间、日期、星期几时必须使用日期时间工具获取真实值，不要猜测
                - 涉及日期推算（如"三天后是星期几"）先取当前值再推算
                - 用简洁自然的中文回答
                """;

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(dateTimeTools)
                .build();
    }
}
