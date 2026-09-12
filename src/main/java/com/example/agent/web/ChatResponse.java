package com.example.agent.web;

/**
 * 聊天响应。
 *
 * @param sessionId 会话 ID
 * @param reply     Agent 回复
 */
public record ChatResponse(
        String sessionId,
        String reply
) {
}
