package com.example.l4j.router;

import java.util.List;

/**
 * 路由结果的结构化格式（AiServices structured output 自动解析）。
 */
public record Routes(
        List<String> agents
) {
}
