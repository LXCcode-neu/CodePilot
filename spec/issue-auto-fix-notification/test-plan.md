# Test Plan: GitHub Issue 自动监听与通知修复闭环

## 1. 测试范围

本测试计划覆盖以下功能：

1. 仓库监听配置。
2. 通知通道配置。
3. GitHub Issue 定时轮询。
4. Issue 入库和去重。
5. 飞书通知。
6. 企业微信通知。
7. 用户确认执行修复。
8. AgentTask 创建。
9. Patch 生成后通知。
10. Diff 确认。
11. PR 创建。
12. PR 成功通知。

## 2. 单元测试

### 2.1 UserRepoWatchServiceTest

测试点：

1. 添加仓库监听成功。
2. 重复添加同一仓库失败。
3. 禁用仓库监听成功。
4. 只能查询当前用户的监听仓库。

### 2.2 GitHubIssueEventServiceTest

测试点：

1. 新 Issue 入库成功。
2. 重复 Issue 不重复入库。
3. Issue 状态从 NEW 到 NOTIFIED。
4. Issue 状态从 NOTIFIED 到 TASK_CREATED。
5. Issue 状态从 TASK_CREATED 到 PATCH_READY。
6. Issue 状态从任意中间状态到 FAILED。

### 2.3 NotificationServiceTest

测试点：

1. 用户无通知通道时不报错。
2. 用户有飞书通道时调用 FeishuNotificationSender。
3. 用户有企业微信通道时调用 WeComNotificationSender。
4. 某一个通道失败不影响其他通道。
5. 发送失败记录日志。

### 2.4 FeishuNotificationSenderTest

测试点：

1. 请求体格式正确。
2. 文本内容包含标题和正文。
3. Webhook 请求失败时抛出或记录异常。

### 2.5 WeComNotificationSenderTest

测试点：

1. 请求体格式正确。
2. msgtype = text。
3. content 正确。
4. Webhook 请求失败时抛出或记录异常。

### 2.6 PullRequestServiceTest

测试点：

1. 未确认 patch 不能直接提交 PR。
2. patch 应用失败时不创建 PR。
3. PR 创建成功后保存 pr_url。
4. 重复提交 PR 时返回错误。

## 3. 集成测试

### 3.1 仓库监听配置集成测试

步骤：

1. 登录用户。
2. 调用 POST /api/repo-watches。
3. 调用 GET /api/repo-watches。
4. 校验列表中存在新仓库。
5. 调用 PUT /api/repo-watches/{id}/enabled。
6. 校验 watch_enabled 改变。

预期：

1. 接口返回成功。
2. 数据库写入正确。
3. 用户只能看到自己的数据。

---

### 3.2 通知配置集成测试

步骤：

1. 添加飞书通知通道。
2. 添加企业微信通知通道。
3. 查询通知通道。
4. 调用测试通知接口。
5. 删除通知通道。

预期：

1. webhook_url 不明文返回。
2. 测试通知可以发送。
3. 删除后不再发送通知。

---

### 3.3 Issue 轮询集成测试

准备：

使用一个测试 GitHub 仓库，创建一个 open issue。

步骤：

1. 添加仓库监听。
2. 手动触发 GitHubIssuePollingJob。
3. 查询 github_issue_event。
4. 再次触发 GitHubIssuePollingJob。

预期：

1. 第一次触发后 issue 入库。
2. 第二次触发后不重复入库。
3. github_issue_event.status = NOTIFIED。
4. 用户收到通知。

---

### 3.4 Issue 执行修复集成测试

步骤：

1. 准备一个 github_issue_event。
2. 调用 POST /api/issues/events/{id}/run。
3. 查询 agent_task。
4. 查询 github_issue_event。

预期：

1. 创建 AgentTask。
2. agent_task.source_type = GITHUB_ISSUE。
3. github_issue_event.agent_task_id 不为空。
4. github_issue_event.status = TASK_CREATED。
5. AgentExecutor 被异步触发。

---

### 3.5 Patch Ready 通知集成测试

步骤：

1. 创建 AgentTask。
2. 模拟 Agent 生成 patch_record。
3. 触发 Patch Ready 通知。
4. 查询通知记录。

预期：

1. 用户收到 Diff 审核通知。
2. 通知中包含 patchReviewUrl。
3. github_issue_event.status = PATCH_READY。

---

### 3.6 确认 Diff 集成测试

步骤：

1. 准备一个 patch_record。
2. 调用 POST /api/patches/{patchId}/confirm。
3. 查询 patch_record。

预期：

1. confirmed = true。
2. confirmed_at 不为空。

---

### 3.7 创建 PR 集成测试

步骤：

1. 准备一个已确认的 patch_record。
2. 配置 GitHub Token。
3. 调用 POST /api/patches/{patchId}/confirm-and-pr。
4. 查询 patch_record。

预期：

1. 创建远程分支。
2. GitHub 上出现 PR。
3. patch_record.pr_url 不为空。
4. 用户收到 PR 创建成功通知。

## 4. 异常测试

### 4.1 GitHub API 失败

模拟：

GitHubIssueClient 请求失败。

预期：

1. 当前仓库记录错误日志。
2. 不影响其他仓库轮询。
3. 系统不崩溃。

---

### 4.2 Webhook 推送失败

模拟：

Webhook URL 填写错误。

预期：

1. Issue 仍然入库。
2. 通知记录为 FAILED。
3. 接口返回明确错误。
4. 不打印完整 webhook_url。

---

### 4.3 重复点击执行修复

步骤：

1. 对同一个 issue_event 连续调用 run 接口两次。

预期：

1. 只创建一个 AgentTask。
2. 第二次请求返回“任务已创建”或明确错误。

---

### 4.4 Agent 修复失败

模拟：

AgentExecutor 抛出异常。

预期：

1. agent_task.status = FAILED。
2. github_issue_event.status = FAILED。
3. 用户收到失败通知。

---

### 4.5 Patch 应用失败

模拟：

patch 内容无法 apply。

预期：

1. 不创建 commit。
2. 不 push。
3. 不创建 PR。
4. 返回 patch 应用失败原因。

---

### 4.6 GitHub Token 无权限

模拟：

Token 无 push 权限。

预期：

1. PR 创建失败。
2. 返回权限不足提示。
3. patch_record.pr_status 不应为 CREATED。

---

### 4.7 重复提交 PR

步骤：

1. 对同一个 patch_record 调用两次 confirm-and-pr。

预期：

1. 第一次创建 PR。
2. 第二次返回“PR 已存在”。
3. 不重复 push 分支。

## 5. 验收用例

### 验收用例 1：新 Issue 自动通知

前置条件：

1. 用户已登录。
2. 用户已添加 GitHub 仓库监听。
3. 用户已配置飞书通知通道。
4. GitHub 仓库存在新 open issue。

操作：

1. 等待定时轮询或手动触发轮询。
2. 查看飞书通知。
3. 查看 CodePilot Issue 事件页面。

预期：

1. 飞书收到新 Issue 通知。
2. CodePilot 页面出现该 Issue。
3. Issue 状态为 NOTIFIED。

---

### 验收用例 2：用户确认后 Agent 修复

前置条件：

1. 已存在状态为 NOTIFIED 的 issue_event。

操作：

1. 点击“执行修复”。
2. 查看 Agent 任务页面。
3. 等待 Agent 执行完成。

预期：

1. 创建 AgentTask。
2. 任务异步执行。
3. 可以看到执行轨迹。
4. 最终生成 Patch。

---

### 验收用例 3：Diff Ready 通知

前置条件：

1. Agent 已生成 Patch。

操作：

1. 查看飞书或企业微信。

预期：

1. 收到 Diff 审核通知。
2. 通知中包含 CodePilot Diff 审核链接。
3. 点击链接可以进入审核页面。

---

### 验收用例 4：确认后提交 PR

前置条件：

1. Patch 已生成。
2. 用户已配置 GitHub Token。
3. Patch 可以正常应用。

操作：

1. 点击“确认并提交 PR”。

预期：

1. 系统创建新分支。
2. 系统提交 commit。
3. 系统 push 到 GitHub。
4. 系统创建 PR。
5. 用户收到 PR 创建成功通知。
6. patch_record 保存 pr_url 和 pr_number。

## 6. 回归测试

本功能上线后，需要确认原有功能不受影响：

1. 用户注册登录正常。
2. 仓库管理正常。
3. 手动创建 AgentTask 正常。
4. Agent 异步执行正常。
5. grep/glob/readFile 检索正常。
6. Patch 生成正常。
7. SSE 执行轨迹正常。
8. 前端页面可以正常访问。
