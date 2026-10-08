package com.example.agent.web;

/**
 * 聊天响应。
 *
 * @param sessionId 会话 ID
 * @param reply     Agent 回复
 * @param agent     实际处理链路，如 "weather -> math"（单专家时为 "general"）
 */
public record ChatResponse(
        String sessionId,
        String reply,
        String agent
) {
}
