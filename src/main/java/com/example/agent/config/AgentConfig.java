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
 * 1. ChatMemory：基于滑动窗口的会话记忆（每个会话保留最近 20 条消息）
 * 2. ChatClient：绑定系统提示词、记忆 Advisor 和三个工具（时间 / 计算器 / 天气）
 */
@Configuration
public class AgentConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient agentChatClient(ChatModel chatModel,
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
}
