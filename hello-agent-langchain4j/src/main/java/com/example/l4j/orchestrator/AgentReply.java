package com.example.l4j.orchestrator;

/**
 * Agent 回复：内容 + 实际处理该轮请求的 Agent 链路（如 "weather -> math"）。
 */
public record AgentReply(
        String content,
        String agent
) {
}
