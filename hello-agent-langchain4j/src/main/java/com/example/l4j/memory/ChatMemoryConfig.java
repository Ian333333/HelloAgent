package com.example.l4j.memory;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会话记忆：每个 sessionId 惰性创建一个 20 条消息的滑动窗口。
 * 对照 springai 模块的 ChatMemory Bean——LangChain4j 用 Provider 按 memoryId 分会话，
 * 默认 InMemoryChatMemoryStore（重启即失，与 springai 版行为一致）。
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .build();
    }
}
