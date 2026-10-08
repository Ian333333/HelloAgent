package com.example.agent.router;

import java.util.List;

/**
 * 意图路由结果：按执行顺序排列的专家名（如 ["weather", "math"]），
 * 以及路由模型的原始输出（便于日志与排查）。
 */
public record RouteResult(
        List<String> agents,
        String rawReply
) {
}
