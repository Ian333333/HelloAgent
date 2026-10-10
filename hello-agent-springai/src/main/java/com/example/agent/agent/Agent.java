package com.example.agent.agent;

/**
 * Agent 统一抽象：基于 sessionId 维持多轮对话记忆。
 */
public interface Agent {

    /**
     * 与 Agent 进行一轮对话。
     *
     * @param sessionId 会话 ID，相同 ID 共享上下文记忆；为空则使用 default
     * @param message   用户输入
     * @return Agent 回复（内容 + 处理链路）
     */
    AgentReply chat(String sessionId, String message);
}
