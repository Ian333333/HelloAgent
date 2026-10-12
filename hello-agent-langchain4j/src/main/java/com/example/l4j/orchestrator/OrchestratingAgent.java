package com.example.l4j.orchestrator;

import com.example.l4j.assistant.DateTimeAssistant;
import com.example.l4j.assistant.GeneralAssistant;
import com.example.l4j.assistant.KnowledgeAssistant;
import com.example.l4j.assistant.MathAssistant;
import com.example.l4j.assistant.RouterAssistant;
import com.example.l4j.assistant.WeatherAssistant;
import com.example.l4j.router.Routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 多 Agent 编排器：与 springai 模块的 OrchestratingAgent 逻辑 1:1 对照。
 *
 * 1. RouterAssistant 用 LLM 判断用户消息需要哪些专家（支持多意图，返回 Routes record）
 * 2. 按路由结果顺序分发给专家 Assistant；第一个专家收到原始问题，
 *    后续专家收到「原始问题 + 前序结果」的拼接上下文（顺序流水线协作）
 * 3. 循环控制：最多执行 MAX_STEPS 步（硬上限熔断），防止路由异常导致死循环
 * 4. 兜底：路由或某一步执行失败时降级为 GeneralAssistant 处理原始问题
 */
@Component
public class OrchestratingAgent {

    private static final Logger log = LoggerFactory.getLogger(OrchestratingAgent.class);
    private static final String DEFAULT_SESSION = "default";

    /** 单轮请求允许执行的专家步骤上限（熔断，防死循环）。 */
    private static final int MAX_STEPS = 3;

    /** 允许的专家名，与 specialists 注册表一一对应。 */
    private static final Set<String> ALLOWED = Set.of("general", "math", "weather", "datetime", "knowledge");

    /** 统一的专家签名（各 Assistant 接口的方法引用都满足它）。 */
    private interface Specialist {
        String chat(String sessionId, String message);
    }

    private final RouterAssistant router;
    private final GeneralAssistant generalAssistant;
    /** 专家注册表：专家名 -> Assistant。 */
    private final Map<String, Specialist> specialists;

    public OrchestratingAgent(RouterAssistant router,
                              GeneralAssistant generalAssistant,
                              MathAssistant mathAssistant,
                              WeatherAssistant weatherAssistant,
                              DateTimeAssistant dateTimeAssistant,
                              KnowledgeAssistant knowledgeAssistant) {
        this.router = router;
        this.generalAssistant = generalAssistant;
        this.specialists = Map.of(
                "general", generalAssistant::chat,
                "math", mathAssistant::chat,
                "weather", weatherAssistant::chat,
                "datetime", dateTimeAssistant::chat,
                "knowledge", knowledgeAssistant::chat);
    }

    public AgentReply chat(String sessionId, String message) {
        String sid = (sessionId == null || sessionId.isBlank()) ? DEFAULT_SESSION : sessionId;
        log.info("session[{}] user: {}", sid, message);

        List<String> steps = route(message);
        // 熔断：无论路由返回多少步，最多只执行 MAX_STEPS 步
        if (steps.size() > MAX_STEPS) {
            log.warn("session[{}] 路由返回 {} 步，截断为 {} 步", sid, steps.size(), MAX_STEPS);
            steps = steps.subList(0, MAX_STEPS);
        }

        String context = message;
        String chain = "";
        String lastContent = null;

        for (String name : steps) {
            Specialist specialist = specialists.get(name);
            if (specialist == null) {
                log.warn("session[{}] 未注册的专家：{}，跳过", sid, name);
                continue;
            }
            try {
                long start = System.currentTimeMillis();
                String reply = specialist.chat(sid, context);
                log.info("session[{}] step[{}] done in {}ms", sid, name, System.currentTimeMillis() - start);

                chain = chain.isEmpty() ? name : chain + " -> " + name;
                lastContent = reply;
                // 顺序流水线：把原始问题与前序结果一起交给下一个专家
                context = message + "\n\n[前序步骤 " + name + " 的结果]\n" + reply;
            } catch (Exception e) {
                log.warn("session[{}] 专家 {} 执行失败，降级为 general：{}", sid, name, e.getMessage());
                return new AgentReply(generalAssistant.chat(sid, message), "general");
            }
        }

        if (lastContent == null) {
            // 所有步骤都被跳过（理论上的兜底路径）
            return new AgentReply(generalAssistant.chat(sid, message), "general");
        }
        return new AgentReply(lastContent, chain);
    }

    /** 路由 + 归一化 + 兜底：过滤非法专家名，空结果降级 general。 */
    private List<String> route(String message) {
        try {
            Routes routes = router.route(message);
            List<String> agents = routes == null ? null : routes.agents();
            if (agents != null) {
                List<String> sanitized = agents.stream().filter(ALLOWED::contains).distinct().toList();
                if (!sanitized.isEmpty()) {
                    log.info("路由结果: {}", sanitized);
                    return sanitized;
                }
            }
            log.warn("路由结果为空或无有效专家，降级为 general");
        } catch (Exception e) {
            log.warn("路由异常，降级为 general：{}", e.getMessage());
        }
        return List.of("general");
    }
}
