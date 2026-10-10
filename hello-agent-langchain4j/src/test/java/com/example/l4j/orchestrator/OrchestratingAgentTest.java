package com.example.l4j.orchestrator;

import com.example.l4j.assistant.DateTimeAssistant;
import com.example.l4j.assistant.GeneralAssistant;
import com.example.l4j.assistant.MathAssistant;
import com.example.l4j.assistant.RouterAssistant;
import com.example.l4j.assistant.WeatherAssistant;
import com.example.l4j.router.Routes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 编排器单测：mock 路由器与各专家 Assistant 接口，不调用真实 LLM。
 */
@ExtendWith(MockitoExtension.class)
class OrchestratingAgentTest {

    private static final String SID = "s1";
    private static final String MESSAGE = "查一下北京的天气，然后算算气温和 0 度相差多少";

    @Mock
    private RouterAssistant router;
    @Mock
    private GeneralAssistant generalAssistant;
    @Mock
    private MathAssistant mathAssistant;
    @Mock
    private WeatherAssistant weatherAssistant;
    @Mock
    private DateTimeAssistant dateTimeAssistant;

    private OrchestratingAgent orchestrator() {
        return new OrchestratingAgent(router, generalAssistant, mathAssistant,
                weatherAssistant, dateTimeAssistant);
    }

    @Test
    void 单意图_直通对应专家() {
        when(router.route(MESSAGE)).thenReturn(new Routes(List.of("datetime")));
        when(dateTimeAssistant.chat(SID, MESSAGE)).thenReturn("今天是星期三");

        AgentReply reply = orchestrator().chat(SID, MESSAGE);

        assertThat(reply.content()).isEqualTo("今天是星期三");
        assertThat(reply.agent()).isEqualTo("datetime");
        verify(dateTimeAssistant, times(1)).chat(SID, MESSAGE);
        verify(generalAssistant, never()).chat(SID, MESSAGE);
    }

    @Test
    void 多意图_顺序流水线传递上下文() {
        when(router.route(MESSAGE)).thenReturn(new Routes(List.of("weather", "math")));
        when(weatherAssistant.chat(SID, MESSAGE)).thenReturn("北京 当前天气：晴，气温 18.5℃");
        when(mathAssistant.chat(eq(SID), anyString())).thenReturn("相差 18.5 度");

        AgentReply reply = orchestrator().chat(SID, MESSAGE);

        assertThat(reply.content()).isEqualTo("相差 18.5 度");
        assertThat(reply.agent()).isEqualTo("weather -> math");

        // 第一个专家收到原始问题
        verify(weatherAssistant).chat(SID, MESSAGE);
        // 第二个专家收到「原始问题 + 前序结果」拼接的上下文
        ArgumentCaptor<String> mathInput = ArgumentCaptor.forClass(String.class);
        verify(mathAssistant).chat(eq(SID), mathInput.capture());
        assertThat(mathInput.getValue()).contains(MESSAGE).contains("18.5");
    }

    @Test
    void 专家执行失败_降级为general() {
        when(router.route(MESSAGE)).thenReturn(new Routes(List.of("weather")));
        when(weatherAssistant.chat(SID, MESSAGE)).thenThrow(new RuntimeException("LLM 超时"));
        when(generalAssistant.chat(SID, MESSAGE)).thenReturn("通用回复");

        AgentReply reply = orchestrator().chat(SID, MESSAGE);

        assertThat(reply.content()).isEqualTo("通用回复");
        assertThat(reply.agent()).isEqualTo("general");
        // 降级时用原始问题（不含前序上下文）
        verify(generalAssistant).chat(SID, MESSAGE);
    }

    @Test
    void 路由返回超步数_截断为3步() {
        List<String> tooMany = List.of("weather", "math", "datetime", "weather", "math");
        when(router.route(MESSAGE)).thenReturn(new Routes(tooMany));
        when(weatherAssistant.chat(eq(SID), anyString())).thenReturn("晴");
        when(mathAssistant.chat(eq(SID), anyString())).thenReturn("42");
        when(dateTimeAssistant.chat(eq(SID), anyString())).thenReturn("周三");

        AgentReply reply = orchestrator().chat(SID, MESSAGE);

        // 只执行前 3 步（weather/math/datetime），第 4、5 步被熔断
        assertThat(reply.agent()).isEqualTo("weather -> math -> datetime");
        verify(weatherAssistant, times(1)).chat(eq(SID), anyString());
        verify(dateTimeAssistant, times(1)).chat(eq(SID), anyString());
    }

    @Test
    void 路由返回非法专家名_兜底general() {
        // 路由返回非法专家名（模拟模型输出异常），过滤后为空 → 降级 general
        when(router.route(MESSAGE)).thenReturn(new Routes(List.of("unknown-agent")));
        when(generalAssistant.chat(SID, MESSAGE)).thenReturn("通用回复");

        AgentReply reply = orchestrator().chat(SID, MESSAGE);

        assertThat(reply.content()).isEqualTo("通用回复");
        assertThat(reply.agent()).isEqualTo("general");
        verify(generalAssistant).chat(SID, MESSAGE);
    }
}
