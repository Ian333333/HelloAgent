package com.example.agent.agent;

import com.example.agent.router.IntentRouter;
import com.example.agent.router.RouteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 多 Agent 编排器：系统的编排入口。
 *
 * 1. IntentRouter 用 LLM 判断用户消息需要哪些专家（支持多意图）
 * 2. 按路由结果顺序分发给专家 Agent；第一个专家收到原始问题，
 *    后续专家收到「原始问题 + 前序结果」的拼接上下文（顺序流水线协作）
 * 3. 循环控制：最多执行 MAX_STEPS 步（硬上限熔断），防止路由异常导致死循环
 * 4. 兜底：路由或某一步执行失败时降级为 GeneralAgent 处理原始问题
 */
@Component
@Primary
public class OrchestratingAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(OrchestratingAgent.class);
    private static final String DEFAULT_SESSION = "default";

    /** 单轮请求允许执行的专家步骤上限（熔断，防死循环）。 */
    private static final int MAX_STEPS = 3;

    private final IntentRouter router;
    private final GeneralAgent generalAgent;
    /** 专家注册表：专家名 -> Agent 实例。 */
    private final Map<String, Agent> specialists;

    public OrchestratingAgent(IntentRouter router,
                              GeneralAgent generalAgent,
                              MathAgent mathAgent,
                              WeatherAgent weatherAgent,
                              DateTimeAgent dateTimeAgent) {
        this.router = router;
        this.generalAgent = generalAgent;
        this.specialists = Map.of(
                "general", generalAgent,
                "math", mathAgent,
                "weather", weatherAgent,
                "datetime", dateTimeAgent);
    }

    @Override
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
            Agent agent = specialists.get(name);
            if (agent == null) {
                log.warn("session[{}] 未注册的专家：{}，跳过", sid, name);
                continue;
            }
            try {
                long start = System.currentTimeMillis();
                AgentReply reply = agent.chat(sid, context);
                log.info("session[{}] step[{}] done in {}ms", sid, name, System.currentTimeMillis() - start);

                chain = chain.isEmpty() ? name : chain + " -> " + name;
                lastContent = reply.content();
                // 顺序流水线：把原始问题与前序结果一起交给下一个专家
                context = message + "\n\n[前序步骤 " + name + " 的结果]\n" + reply.content();
            } catch (Exception e) {
                log.warn("session[{}] 专家 {} 执行失败，降级为 general：{}", sid, name, e.getMessage());
                return generalAgent.chat(sid, message);
            }
        }

        if (lastContent == null) {
            // 所有步骤都被跳过（理论上的兜底路径）
            return generalAgent.chat(sid, message);
        }
        return new AgentReply(lastContent, chain);
    }

    /** 路由 + 异常兜底：路由层自身已兜底，这里再防御一层。 */
    private List<String> route(String message) {
        try {
            RouteResult result = router.route(message);
            log.info("路由结果: {}", result.agents());
            return result.agents();
        } catch (Exception e) {
            log.warn("路由异常，降级为 general：{}", e.getMessage());
            return List.of("general");
        }
    }
}
