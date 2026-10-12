package com.example.l4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 知识库专家 Assistant：挂 contentRetriever，回答基于项目语料的 RAG 问答。
 * 每个专家有自己的上下文增强方式——这个专家用「检索增强」而非工具。
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "contentRetriever")
public interface KnowledgeAssistant {

    @SystemMessage("""
            你是 HelloAgent 的知识库专家。回答时优先依据检索到的资料内容；
            资料中没有相关信息时明确说明不知道，不要编造。
            引用资料中的具体内容时保持原文准确。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
