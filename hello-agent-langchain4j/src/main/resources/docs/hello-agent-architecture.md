# HelloAgent 项目架构说明

## 项目概况

HelloAgent 是一个多 Agent 编排示例项目，位于 C:\Users\HP\IdeaProjects\HelloAgent，
采用 Maven 多模块结构，包含两个子模块，用两套框架实现了完全相同的多 Agent 编排架构，
供对照学习。

## 模块划分

项目包含以下两个模块：

1. hello-agent-springai：基于 Spring Boot 3.4.5 + Spring AI 1.0.0 实现，运行在 8080 端口。
   Agent 通过 ChatClient Bean 定义，工具用 org.springframework.ai.tool.annotation.Tool 注解，
   记忆使用 MessageWindowChatMemory + MessageChatMemoryAdvisor。

2. hello-agent-langchain4j：基于 Spring Boot 3.4.5 + LangChain4j 1.22 实现，运行在 8081 端口。
   Agent 通过 @AiService 声明式接口定义，工具用 dev.langchain4j.agent.tool.Tool 注解，
   记忆使用 ChatMemoryProvider（按 sessionId 惰性创建 20 条消息的滑动窗口）。

## 编排架构

两个模块共用同一套编排模式：Router + 专家 + 编排器。

- 路由（Router）：一次轻量 LLM 调用判断用户消息需要哪些专家，支持多意图识别，
  按依赖顺序输出专家序列（例如"查北京天气再算温差"输出 weather、math 两步）。
- 专家（Specialist）：每个专家绑定专属系统提示词与本领域工具，实现上下文和工具隔离。
  专家包括：general（通用兜底）、math（数学计算）、weather（天气查询）、
  datetime（日期时间）、knowledge（知识库问答，挂 RAG 检索器）。
- 编排器（OrchestratingAgent）：纯 Java 代码实现循环控制——路由结果最多执行 3 步（硬上限熔断），
  后续专家收到「原始问题 + 前序结果」的拼接上下文（顺序流水线协作），
  路由或执行失败时降级为 general 兜底。

## 会话记忆

两个模块的会话记忆都基于 sessionId 隔离，每个会话保留最近 20 条消息（滑动窗口），
默认存储在内存中，应用重启后丢失。
