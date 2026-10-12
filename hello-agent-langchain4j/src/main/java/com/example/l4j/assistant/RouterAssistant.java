package com.example.l4j.assistant;

import com.example.l4j.router.Routes;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 意图路由器：无工具、无记忆（路由要快且无副作用），
 * 返回值是 record，LangChain4j AiServices 会自动生成 JSON schema 提示并解析。
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "openAiChatModel")
public interface RouterAssistant {

    @SystemMessage("""
            你是 HelloAgent 的意图路由器。分析用户消息，判断需要哪些专家按顺序处理。
            可选专家：
            - weather：查询天气、气温、湿度、风速等
            - math：数学计算
            - datetime：当前日期、时间、星期几
            - knowledge：HelloAgent 项目知识、文档问答、与本项目架构/功能相关的问题（如"这个项目有几个模块"）
            - general：其他闲聊、常识问答、写作等
            规则：
            - 若消息包含多个任务，按依赖顺序全部列出，例如"查北京天气再算温差"输出 ["weather","math"]
            - 只需一个专家时输出单元素数组
            - 无法判断时输出 ["general"]
            只输出 JSON，不要输出任何其他内容。
            """)
    Routes route(@UserMessage String message);
}
