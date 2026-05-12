# Spec: GitHub Issue 自动监听与通知修复闭环

## 1. 背景

当前 CodePilot 已支持用户手动创建 Agent 修复任务，流程为：

用户创建任务 -> 拉取 GitHub 仓库 -> 检索代码 -> Agent 分析 -> 生成 Patch -> 展示执行轨迹

但当前流程仍然依赖用户手动发现 Issue、手动创建任务，自动化程度不够。

本次功能希望让 CodePilot 支持监听用户绑定的开源 GitHub 仓库。当仓库出现新的 Issue 时，系统可以自动感知，并通过飞书或企业微信推送通知给用户。用户收到通知后，可以进入 CodePilot 确认是否执行修复。Agent 修复完成后，系统再次推送 Diff 审核通知。用户确认后，系统将修复结果作为 Pull Request 提交到 GitHub。

## 2. 目标

实现从 GitHub Issue 感知到 Agent 修复再到 PR 提交的闭环流程。

核心目标：

1. 支持用户配置需要监听的 GitHub 仓库。
2. 支持定时轮询 GitHub 仓库的新 Issue。
3. 支持将新 Issue 入库，避免重复处理。
4. 支持飞书 Webhook 通知。
5. 支持企业微信群机器人 Webhook 通知。
6. 支持用户收到通知后进入 CodePilot 确认是否执行修复。
7. 支持将 Issue 转换为 AgentTask。
8. 支持复用现有 AgentExecutor 执行修复流程。
9. 支持 Agent 修复完成后推送 Diff 审核通知。
10. 支持用户确认 Diff 后创建 GitHub Pull Request。

## 3. 非目标

本阶段暂不实现以下内容：

1. 不开发原生手机 App。
2. 不实现微信公众号推送。
3. 不实现微信小程序订阅消息。
4. 不强制使用 GitHub Webhook，第一阶段先使用定时轮询。
5. 不做多 Agent 协作。
6. 不做自动合并 PR。
7. 不在用户未确认的情况下自动修改远程仓库。
8. 不把飞书或企业微信做成完整应用，第一阶段只支持机器人 Webhook。

## 4. 用户故事

### Story 1：用户可以绑定需要监听的 GitHub 仓库

作为用户，我希望在 CodePilot 中添加我关注的 GitHub 仓库，这样系统可以自动检查这个仓库是否有新的 Issue。

### Story 2：系统可以发现新的 Issue

作为系统，我希望定时轮询用户绑定的 GitHub 仓库，当发现新的 open issue 时，将其保存到数据库。

### Story 3：用户可以收到新 Issue 通知

作为用户，我希望当仓库出现新 Issue 时，可以在飞书或企业微信中收到通知。

### Story 4：用户可以确认是否执行修复

作为用户，我希望收到通知后不要让 Agent 自动乱改，而是由我进入 CodePilot 确认是否让 Agent 修复。

### Story 5：Agent 可以修复来自 GitHub Issue 的任务

作为用户，我希望确认执行后，系统可以自动创建 AgentTask，并复用现有 CodePilot Agent 修复流程。

### Story 6：用户可以收到 Diff 审核通知

作为用户，我希望 Agent 修复完成后，系统能把 Diff 审核链接推送到飞书或企业微信。

### Story 7：用户确认后系统可以提交 PR

作为用户，我希望确认 Diff 没问题后，CodePilot 可以自动创建分支、提交 commit、push 到 GitHub，并创建 Pull Request。

## 5. 业务流程

```text
用户绑定 GitHub 仓库
        ↓
系统定时轮询 open issues
        ↓
发现新 Issue
        ↓
保存 github_issue_event
        ↓
推送飞书/企业微信通知
        ↓
用户进入 CodePilot
        ↓
用户点击“执行修复”
        ↓
创建 AgentTask
        ↓
Agent 异步执行修复
        ↓
生成 Patch/Diff
        ↓
推送 Diff 审核通知
        ↓
用户确认 Diff
        ↓
创建 GitHub 分支
        ↓
应用 Patch
        ↓
commit + push
        ↓
创建 Pull Request
        ↓
推送 PR 创建成功通知
```

## 6. 角色与权限

### 普通用户

可以：

1. 添加自己的 GitHub 仓库监听配置。
2. 添加飞书/企业微信通知通道。
3. 查看系统发现的 Issue。
4. 确认是否执行修复。
5. 查看 Agent 执行轨迹。
6. 查看 Diff。
7. 确认是否提交 PR。

不可以：

1. 操作其他用户的仓库。
2. 查看其他用户的通知配置。
3. 使用其他用户的 GitHub Token 提交 PR。

## 7. 验收标准

### AC-1：仓库监听配置

当用户添加一个 GitHub 仓库监听配置后，系统应保存 owner、repoName、repoUrl、defaultBranch、watchEnabled 等信息。

### AC-2：定时轮询 Issue

当仓库监听开启时，系统应定时调用 GitHub API 获取 open issues，并将新的 issue 保存到 github_issue_event 表。

### AC-3：Issue 去重

对于同一个 repo 和 issue_number，系统不应重复创建 github_issue_event。

### AC-4：新 Issue 通知

当系统发现新的 Issue 后，应通过用户配置的飞书或企业微信 Webhook 发送通知。

### AC-5：用户确认执行

当用户点击“执行修复”后，系统应基于该 github_issue_event 创建 AgentTask，并将 issue_event 状态更新为 TASK_CREATED。

### AC-6：Agent 修复

AgentTask 创建后，系统应复用现有 AgentExecutor 异步执行修复流程。

### AC-7：Diff 通知

当 Agent 生成 patch_record 后，系统应通过飞书或企业微信推送 Diff 审核通知。

### AC-8：用户确认 Diff

当用户确认 Diff 后，patch_record.confirmed 应更新为 true，confirmed_at 应记录确认时间。

### AC-9：创建 PR

当用户点击“确认并提交 PR”后，系统应创建新分支、应用 patch、commit、push，并调用 GitHub API 创建 Pull Request。

### AC-10：PR 通知

PR 创建成功后，系统应保存 pr_url，并推送 PR 创建成功通知。

## 8. 异常场景

### 场景 1：GitHub API 请求失败

系统应记录错误日志，不应影响其他仓库轮询。

### 场景 2：Webhook 推送失败

系统应记录通知失败状态，但不影响 Issue 入库。

### 场景 3：用户重复点击执行修复

系统应避免重复创建 AgentTask。

### 场景 4：Agent 修复失败

系统应将任务状态更新为 FAILED，并推送失败通知。

### 场景 5：Patch 应用失败

系统不应继续创建 PR，应记录失败原因并通知用户。

### 场景 6：GitHub Token 无权限

系统应返回明确错误，提示用户检查 Token 权限。

## 9. 优先级

### P0

1. 仓库监听配置。
2. 定时轮询 GitHub Issue。
3. Issue 入库和去重。
4. 飞书/企业微信 Webhook 通知。
5. 用户确认后创建 AgentTask。
6. Agent 修复完成后推送 Diff 通知。

### P1

1. 用户确认 Diff。
2. 创建分支。
3. 应用 patch。
4. commit + push。
5. 创建 Pull Request。

### P2

1. GitHub Webhook 替代轮询。
2. 飞书卡片消息。
3. 企业微信 markdown 消息。
4. 通知失败重试。
5. 通知历史记录。
