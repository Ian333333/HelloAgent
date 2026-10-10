package com.example.agent.agent;

import com.example.agent.router.IntentRouter;
import com.example.agent.router.RouteResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 编排器单测：mock 路由器与各专家，不调用真实 LLM。
 */
@ExtendWith(MockitoExtension.class)
class OrchestratingAgentTest {

    private static final String SID = "s1";
    private static final String MESSAGE = "查一下北京的天气，然后算算气温和 0 度相差多少";

    @Mock
    private IntentRouter router;
    @Mock
    private GeneralAgent generalAgent;
    @Mock
    private MathAgent mathAgent;
    @Mock
    private WeatherAgent weatherAgent;
    @Mock
    private DateTimeAgent dateTimeAgent;

    private OrchestratingAgent orchestrator() {
        return new OrchestratingAgent(router, generalAgent, mathAgent, weatherAgent, dateTimeAgent);
    }

    @Test
    void 单意图_直通对应专家() {
        when(router.route(MESSAGE)).thenReturn(new RouteResult(List.of("datetime"), "raw"));
        when(dateTimeAgent.chat(SID, MESSAGE)).thenReturn(new AgentReply("今天是星期三", "datetime"));

        AgentReply reply = orchestrator().chat(SID, MESSAGE);

        assertThat(reply.content()).isEqualTo("今天是星期三");
        assertThat(reply.agent()).isEqualTo("datetime");
        verify(dateTimeAgent, times(1)).chat(SID, MESSAGE);
        verify(generalAgent, never()).chat(SID, MESSAGE);
    }

    @Test
    void 多意图_顺序流水线传递上下文() {
        when(router.route(MESSAGE)).thenReturn(new RouteResult(List.of("weather", "math"), "raw"));
        when(weatherAgent.chat(SID, MESSAGE)).thenReturn(new AgentReply("北京 当前天气：晴，气温 18.5℃", "weather"));
        when(mathAgent.chat(eq(SID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new AgentReply("相差 18.5 度", "math"));

        AgentReply reply = orchestrator().chat(SID, MESSAGE);

        assertThat(reply.content()).isEqualTo("相差 18.5 度");
        assertThat(reply.agent()).isEqualTo("weather -> math");

        // 第一个专家收到原始问题
        verify(weatherAgent).chat(SID, MESSAGE);
        // 第二个专家收到「原始问题 + 前序结果」拼接的上下文
        ArgumentCaptor<String> mathInput = ArgumentCaptor.forClass(String.class);
        verify(mathAgent).chat(eq(SID), mathInput.capture());
        assertThat(mathInput.getValue()).contains(MESSAGE).contains("18.5");
    }

    @Test
    void 专家执行失败_降级为general() {
        when(router.route(MESSAGE)).thenReturn(new RouteResult(List.of("weather"), "raw"));
        when(weatherAgent.chat(SID, MESSAGE)).thenThrow(new RuntimeException("LLM 超时"));
        when(generalAgent.chat(SID, MESSAGE)).thenReturn(new AgentReply("通用回复", "general"));

        AgentReply reply = orchestrator().chat(SID, MESSAGE);

        assertThat(reply.content()).isEqualTo("通用回复");
        assertThat(reply.agent()).isEqualTo("general");
        // 降级时用原始问题（不含前序上下文）
        verify(generalAgent).chat(SID, MESSAGE);
    }

    @Test
    void 路由返回超步数_截断为3步() {
        List<String> tooMany = List.of("weather", "math", "datetime", "weather", "math");
        when(router.route(MESSAGE)).thenReturn(new RouteResult(tooMany, "raw"));
        when(weatherAgent.chat(eq(SID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new AgentReply("晴", "weather"));
        when(mathAgent.chat(eq(SID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new AgentReply("42", "math"));
        when(dateTimeAgent.chat(eq(SID), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new AgentReply("周三", "datetime"));

        AgentReply reply = orchestrator().chat(SID, MESSAGE);

        // 只执行前 3 步（weather/math/datetime），第 4、5 步被熔断
        assertThat(reply.agent()).isEqualTo("weather -> math -> datetime");
        verify(weatherAgent, times(1)).chat(eq(SID), org.mockito.ArgumentMatchers.anyString());
        verify(dateTimeAgent, times(1)).chat(eq(SID), org.mockito.ArgumentMatchers.anyString());
    }
}
