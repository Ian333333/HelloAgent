package com.example.agent.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 聊天请求。
 *
 * @param sessionId 会话 ID，可空（空则使用 default）；相同 ID 拥有连续上下文
 * @param message   用户消息
 */
public record ChatRequest(
        String sessionId,
        @NotBlank(message = "message 不能为空")
        String message
) {
}
