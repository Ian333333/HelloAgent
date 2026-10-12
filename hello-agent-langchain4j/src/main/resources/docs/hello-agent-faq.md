# HelloAgent 常见问题（FAQ）

## 这个项目提供哪些工具？

HelloAgent 的 Agent 可以调用三类工具：

1. 计算器工具：内置递归下降解析器，支持加减乘除、小数、括号和一元负号，
   输入数学表达式字符串即可求值，不依赖脚本引擎。
2. 天气工具：调用 Open-Meteo 免费接口（无需 API Key），先通过地理编码接口把
   城市名转为经纬度，再查询实时天气，返回气温、湿度、风速和天气描述。
3. 日期时间工具：获取当前日期时间（格式 yyyy-MM-dd HH:mm:ss）和星期几。

LangChain4j 模块还有 RAG 知识检索能力（knowledge 专家），语料来自项目内
docs 目录下的 Markdown 文档，使用本地 bge-small-zh 模型向量化。

## 会话记忆窗口是多大？

每个 sessionId 对应一个独立会话，采用滑动窗口记忆，保留最近 20 条消息。
相同 sessionId 的连续请求共享上下文；记忆默认存储在内存中，重启即失效，
生产环境可替换为持久化存储（如 JDBC、Redis）。

## 如何更换模型？

模型通过环境变量配置：OPENAI_API_KEY（API 密钥）、OPENAI_BASE_URL（OpenAI 兼容
服务地址）、OPENAI_MODEL（模型名，默认 qwen3.7-flash）。任何 OpenAI 兼容网关
均可使用，例如 DeepSeek、千问兼容模式等。

## RAG 用的什么 embedding 模型？

使用本地 ONNX 推理的 bge-small-zh-v1.5 模型（512 维向量），无需调用外部 API，
离线可用。语料按 300 token 切块，相邻块有 50 token 重叠；检索时返回相似度
不低于 0.5 的前 4 条结果。

## 前端页面在哪？

两个模块各自内嵌一个聊天页面：Spring AI 版访问 http://localhost:8080，
LangChain4j 版访问 http://localhost:8081。后端 API 均为
POST /api/agent/chat，请求体为 JSON（sessionId 与 message 字段），
响应中带有 agent 字段，显示实际处理该轮请求的编排链路（如 weather -> math）。
