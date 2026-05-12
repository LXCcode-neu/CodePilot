# Codex 总提示词：实现 GitHub Issue 自动监听与通知修复闭环

我正在开发 CodePilot 项目，这是一个面向 GitHub Issue 的智能代码修复 Agent 平台，技术栈为 Spring Boot 3、MySQL、Redis、JGit、SSE、LLM API，前端为 React + Vite + TypeScript + TailwindCSS + shadcn/ui。

当前项目已经具备：

1. 用户注册登录
2. 仓库管理
3. AgentTask 创建
4. AgentExecutor 执行修复流程
5. grep/glob/readFile 代码检索
6. Patch 生成
7. agent_task 和 agent_step 执行轨迹记录
8. SSE 实时推送执行过程

现在我要新增一个功能：GitHub Issue 自动监听与通知修复闭环。

## 目标

1. 用户可以配置需要监听的 GitHub 开源仓库。
2. 后端定时轮询这些仓库的 open issues。
3. 发现新 issue 后入库，避免重复处理。
4. 通过飞书或企业微信群机器人 Webhook 推送通知给用户。
5. 用户进入 CodePilot 后可以确认是否让 Agent 修复。
6. 用户确认后，系统基于该 issue 创建 AgentTask，并复用现有 AgentExecutor 异步修复流程。
7. Agent 生成 Patch 后，再次通过飞书/企业微信推送 Diff 审核通知。
8. 用户确认 Diff 后，系统可以创建修复分支、应用 patch、commit、push，并创建 GitHub Pull Request。
9. PR 创建成功后推送通知。

## 重要约束

请严格遵守：

1. 不要重构现有 AgentExecutor 主流程。
2. 不要破坏现有手动创建 AgentTask 的功能。
3. 新增模块应与现有模块解耦。
4. 通知模块要抽象接口，支持 FEISHU 和 WE_COM 两种实现。
5. webhook_url 和 GitHub Token 必须加密保存，日志和前端不能泄露完整值。
6. Issue 去重规则使用 repo_watch_id + issue_number。
7. 用户未确认 Patch 之前，不允许 push 到 GitHub。
8. PR 创建失败时，需要返回明确错误并记录日志。
9. 所有接口都要校验当前用户权限。
10. 请补充必要的单元测试和集成测试。
11. 如果现有表结构和 spec 不完全一致，优先兼容现有表，不要大规模重构。
12. 每个阶段完成后都要保证项目可以启动，原有功能不受影响。

## 需要先阅读的文档

请先阅读以下文档：

```text
specs/issue-auto-fix-notification/requirements.md
specs/issue-auto-fix-notification/design.md
specs/issue-auto-fix-notification/tasks.md
specs/issue-auto-fix-notification/test-plan.md
```

然后按 `tasks.md` 的阶段逐步实现。

每完成一个阶段，请说明：

1. 修改了哪些文件
2. 新增了哪些表
3. 新增了哪些接口
4. 如何测试
5. 是否影响原有功能

## 推荐实现顺序

第一版先完成：

1. 仓库监听配置
2. Issue 定时轮询
3. Issue 入库去重
4. 飞书/企业微信通知
5. 用户确认执行
6. 创建 AgentTask
7. Agent 生成 Patch 后推送 Diff 通知

第二版再完成：

1. Diff 确认
2. GitHub Token 配置
3. 创建分支
4. 应用 patch
5. commit + push
6. 创建 Pull Request
7. PR 成功通知

第三版再考虑：

1. GitHub Webhook 替代定时轮询
2. 飞书卡片消息
3. 企业微信 markdown 消息
4. 通知失败重试
5. 通知历史记录
