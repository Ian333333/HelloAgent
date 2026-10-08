package com.example.agent.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 意图路由器：用一次轻量 LLM 调用判断用户消息需要哪些专家 Agent 处理。
 * 路由用的 ChatClient 不挂工具、不挂记忆（路由要快且无副作用）。
 * 任何失败（模型异常 / JSON 解析失败 / 结果为空）都兜底返回 ["general"]。
 */
@Component
public class IntentRouter {

    private static final Logger log = LoggerFactory.getLogger(IntentRouter.class);

    /** 允许的专家名，与 OrchestratingAgent 中注册的专家一一对应。 */
    private static final Set<String> ALLOWED = Set.of("general", "math", "weather", "datetime");

    private static final String ROUTER_SYSTEM = """
            你是 HelloAgent 的意图路由器。分析用户消息，判断需要哪些专家按顺序处理。
            可选专家：
            - weather：查询天气、气温、湿度、风速等
            - math：数学计算
            - datetime：当前日期、时间、星期几
            - general：其他闲聊、常识问答、写作等
            规则：
            - 若消息包含多个任务，按依赖顺序全部列出，例如"查北京天气再算温差"输出 ["weather","math"]
            - 只需一个专家时输出单元素数组
            - 无法判断时输出 ["general"]
            只输出 JSON，不要输出任何其他内容。
            """;

    /** 路由结果的结构化格式（由 BeanOutputConverter 自动解析）。 */
    public record Routes(List<String> agents) {
    }

    private final ChatClient chatClient;
    private final BeanOutputConverter<Routes> converter = new BeanOutputConverter<>(Routes.class);

    public IntentRouter(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(ROUTER_SYSTEM + "\n" + converter.getFormat())
                .build();
    }

    /**
     * 对用户消息做意图路由。
     *
     * @param message 用户输入
     * @return 归一化后的路由结果；解析失败或结果为空时返回 ["general"]
     */
    public RouteResult route(String message) {
        try {
            String raw = chatClient.prompt().user(message).call().content();
            log.info("router raw reply: {}", raw);

            List<String> agents = converter.convert(raw).agents();
            List<String> sanitized = agents == null ? List.of()
                    : agents.stream().filter(ALLOWED::contains).distinct().toList();

            if (sanitized.isEmpty()) {
                return fallback(raw, "路由结果为空或无有效专家，降级为 general");
            }
            return new RouteResult(sanitized, raw);
        } catch (Exception e) {
            log.warn("意图路由失败，降级为 general：{}", e.getMessage());
            return new RouteResult(List.of("general"), e.getMessage());
        }
    }

    private RouteResult fallback(String raw, String reason) {
        log.warn("{}：{}", reason, raw);
        return new RouteResult(List.of("general"), raw);
    }
}
