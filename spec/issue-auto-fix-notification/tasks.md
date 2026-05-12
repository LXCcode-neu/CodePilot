# Tasks: GitHub Issue 自动监听与通知修复闭环

## Phase 1：数据库与基础模型

### Task 1.1 创建 user_repo_watch 表

实现内容：

1. 新增 user_repo_watch 表。
2. 新增实体类 UserRepoWatch。
3. 新增 Mapper/Repository。
4. 新增基础 CRUD Service。

完成标准：

1. 可以保存用户监听仓库。
2. 同一个用户不能重复添加同一个 owner/repo。
3. 可以启用或禁用监听。

---

### Task 1.2 创建 github_issue_event 表

实现内容：

1. 新增 github_issue_event 表。
2. 新增实体类 GitHubIssueEvent。
3. 新增 Mapper/Repository。
4. 添加唯一键 repo_watch_id + issue_number。

完成标准：

1. 可以保存 GitHub Issue。
2. 重复 issue 不会重复入库。
3. Issue 状态可以流转。

---

### Task 1.3 创建 notification_channel 表

实现内容：

1. 新增 notification_channel 表。
2. 新增实体类 NotificationChannel。
3. 新增 Mapper/Repository。
4. 支持 channel_type = FEISHU / WE_COM。
5. webhook_url 加密保存。

完成标准：

1. 用户可以保存飞书 Webhook。
2. 用户可以保存企业微信 Webhook。
3. 可以启用、禁用、删除通知通道。
4. 响应和日志中不会泄露完整 webhook_url。

---

### Task 1.4 扩展 patch_record 表

实现内容：

1. 添加 confirmed 字段。
2. 添加 confirmed_at 字段。
3. 添加 safety_check_result 字段。
4. 添加 pr_url 字段。
5. 添加 pr_number 字段。
6. 添加 pr_status 字段。

完成标准：

1. Patch 可以记录确认状态。
2. Patch 可以记录 PR 信息。

---

### Task 1.5 扩展 agent_task 表

实现内容：

1. 添加 source_type 字段。
2. 添加 source_id 字段。
3. 手动创建的任务 source_type 默认为 MANUAL。
4. 从 GitHub Issue 创建的任务 source_type 为 GITHUB_ISSUE。

完成标准：

1. 现有手动任务不受影响。
2. 可以追踪 AgentTask 来源。

---

## Phase 2：通知模块

### Task 2.1 定义通知抽象

实现内容：

1. 创建 NotificationService。
2. 创建 NotificationSender 接口。
3. 创建 NotificationMessage。
4. 创建 NotificationEventType。
5. 创建 NotificationChannelType。

完成标准：

1. NotificationService 可以根据用户通知通道发送消息。
2. 后续可以扩展更多通知平台。

---

### Task 2.2 实现飞书通知

实现内容：

1. 创建 FeishuNotificationSender。
2. 使用 Webhook 发送文本消息。
3. 支持发送测试消息。
4. 失败时记录日志。

完成标准：

1. 用户配置飞书 Webhook 后可以收到测试消息。
2. 系统可以推送新 Issue 通知。
3. 系统可以推送 Patch Ready 通知。
4. 系统可以推送 PR Created 通知。

---

### Task 2.3 实现企业微信通知

实现内容：

1. 创建 WeComNotificationSender。
2. 使用 Webhook 发送文本消息。
3. 支持发送测试消息。
4. 失败时记录日志。

完成标准：

1. 用户配置企业微信 Webhook 后可以收到测试消息。
2. 系统可以推送新 Issue 通知。
3. 系统可以推送 Patch Ready 通知。
4. 系统可以推送 PR Created 通知。

---

### Task 2.4 实现通知模板

实现内容：

1. 创建 NotificationTemplateFactory。
2. 新增新 Issue 通知模板。
3. 新增 Agent 开始修复通知模板。
4. 新增 Patch Ready 通知模板。
5. 新增 PR Created 通知模板。
6. 新增 Repair Failed 通知模板。

完成标准：

1. 通知内容清晰。
2. 通知中包含 CodePilot 页面链接。
3. 通知中包含 GitHub Issue/PR 链接。

---

## Phase 3：仓库监听配置接口

### Task 3.1 添加仓库监听接口

实现接口：

```http
POST /api/repo-watches
```

完成标准：

1. 用户可以添加 owner/repoName/repoUrl/defaultBranch。
2. 重复仓库返回明确错误。
3. 默认 watch_enabled = true。

---

### Task 3.2 查询仓库监听列表

实现接口：

```http
GET /api/repo-watches
```

完成标准：

1. 只返回当前用户的监听仓库。
2. 返回监听状态和 last_checked_at。

---

### Task 3.3 启用/禁用仓库监听

实现接口：

```http
PUT /api/repo-watches/{id}/enabled
```

完成标准：

1. 用户只能修改自己的监听配置。
2. 可以开启或关闭监听。

---

## Phase 4：通知配置接口

### Task 4.1 添加通知通道

实现接口：

```http
POST /api/notification/channels
```

完成标准：

1. 支持 FEISHU。
2. 支持 WE_COM。
3. webhook_url 加密保存。
4. 不在响应中返回完整 webhook_url。

---

### Task 4.2 查询通知通道

实现接口：

```http
GET /api/notification/channels
```

完成标准：

1. 只返回当前用户的通知通道。
2. webhook_url 需要脱敏展示。

---

### Task 4.3 测试通知通道

实现接口：

```http
POST /api/notification/channels/{id}/test
```

完成标准：

1. 能发送测试通知。
2. 发送失败时返回错误原因。
3. 不泄露 webhook_url。

---

### Task 4.4 删除通知通道

实现接口：

```http
DELETE /api/notification/channels/{id}
```

完成标准：

1. 用户只能删除自己的通知通道。
2. 删除后不再发送通知。

---

## Phase 5：GitHub Issue 定时轮询

### Task 5.1 实现 GitHubIssueClient

实现内容：

1. 根据 owner/repo 获取 open issues。
2. 过滤 pull request。
3. 解析 issue_number、title、body、url、sender。

完成标准：

1. 可以获取指定仓库的 open issues。
2. 不把 PR 当成 issue 处理。

---

### Task 5.2 实现 GitHubIssuePollingJob

实现内容：

1. 使用 @Scheduled 定时执行。
2. 查询所有 watch_enabled = true 的仓库。
3. 调用 GitHubIssueClient。
4. 新 Issue 入库。
5. 更新 last_checked_at。
6. 单个仓库失败不影响其他仓库。

完成标准：

1. 定时任务可以正常运行。
2. 单个仓库失败不影响其他仓库。
3. 新 issue 不重复入库。

---

### Task 5.3 新 Issue 触发通知

实现内容：

1. Issue 新入库后调用 NotificationService。
2. 推送 NEW_ISSUE 类型消息。
3. 更新 github_issue_event.status = NOTIFIED。
4. 更新 notified_at。

完成标准：

1. 新 Issue 出现后用户可以收到飞书/企业微信通知。
2. 通知失败不影响 Issue 入库。

---

## Phase 6：Issue 事件管理

### Task 6.1 查询 Issue 事件列表

实现接口：

```http
GET /api/issues/events
```

完成标准：

1. 用户只能查看自己的 issue_event。
2. 支持按状态过滤。
3. 支持分页。

---

### Task 6.2 忽略 Issue

实现接口：

```http
POST /api/issues/events/{id}/ignore
```

完成标准：

1. 用户可以忽略某个 Issue。
2. 忽略后状态变为 IGNORED。
3. 被忽略的 Issue 不能再创建任务，除非后续支持重新打开。

---

### Task 6.3 基于 Issue 创建 AgentTask

实现接口：

```http
POST /api/issues/events/{id}/run
```

实现内容：

1. 校验权限。
2. 校验状态。
3. 创建 AgentTask。
4. 写入 issueTitle 和 issueDescription。
5. 更新 issue_event.agent_task_id。
6. 更新状态为 TASK_CREATED。
7. 异步启动 AgentExecutor。

完成标准：

1. 用户点击执行修复后能创建任务。
2. 同一个 issue_event 不能重复创建多个任务。
3. Agent 修复流程复用现有逻辑。

---

## Phase 7：Agent 修复完成后的 Diff 通知

### Task 7.1 监听 Patch 生成完成

实现内容：

1. 在 PatchRecord 创建成功后触发通知。
2. 通知类型为 PATCH_READY。
3. 通知中包含 Diff 审核页面链接。

完成标准：

1. Agent 修复完成后用户能收到通知。
2. 通知内容包含任务号、仓库名、Issue 标题。

---

### Task 7.2 更新 Issue 状态

实现内容：

1. Patch 生成成功后，将 github_issue_event.status 改为 PATCH_READY。
2. Agent 失败时，将状态改为 FAILED。

完成标准：

1. Issue 事件状态和 AgentTask 状态保持一致。
2. 前端可以正确展示当前进度。

---

## Phase 8：Diff 确认

### Task 8.1 确认 Patch

实现接口：

```http
POST /api/patches/{patchId}/confirm
```

实现内容：

1. 校验 patch 属于当前用户。
2. 更新 confirmed = true。
3. 更新 confirmed_at。

完成标准：

1. 用户可以确认 Diff。
2. 已确认状态可以在前端展示。

---

### Task 8.2 Diff 审核页面支持

前端实现：

1. 展示 diff 内容。
2. 展示修改文件列表。
3. 展示 safety_check_result。
4. 展示确认按钮。
5. 展示确认并提交 PR 按钮。

完成标准：

1. 用户可以清楚看到 Agent 修改了什么。
2. 用户确认前不会提交 PR。

---

## Phase 9：PR 提交

### Task 9.1 实现 GitHub Token 配置

实现内容：

1. 用户可以配置 GitHub Token。
2. Token 加密保存。
3. 前端不展示完整 Token。
4. 日志不打印 Token。

完成标准：

1. 用户可以保存 Token。
2. 后端可以在提交 PR 时读取并解密 Token。

---

### Task 9.2 实现 PullRequestService

实现内容：

1. 根据 patch_record 找到对应仓库。
2. 创建本地修复分支。
3. 应用 patch。
4. git add。
5. git commit。
6. git push。
7. 调用 GitHub API 创建 PR。
8. 保存 pr_url、pr_number、pr_status。

完成标准：

1. 用户确认后可以创建 PR。
2. PR 创建成功后可以在 GitHub 打开。
3. PR 信息保存到 patch_record。

---

### Task 9.3 实现确认并提交 PR 接口

实现接口：

```http
POST /api/patches/{patchId}/confirm-and-pr
```

完成标准：

1. 如果 patch 未确认，则先确认。
2. 创建 PR 成功后返回 prUrl。
3. 创建失败时返回明确错误。
4. 不允许重复创建 PR。

---

### Task 9.4 PR 成功通知

实现内容：

1. PR 创建成功后调用 NotificationService。
2. 推送 PR_CREATED 消息。
3. 通知中包含 PR 链接。

完成标准：

1. 用户可以在飞书/企业微信收到 PR 成功通知。
2. 可以点击链接进入 GitHub PR 页面。

---

## Phase 10：前端页面

### Task 10.1 仓库监听页面

实现内容：

1. 添加仓库。
2. 展示仓库列表。
3. 开启/关闭监听。
4. 显示 last_checked_at。

---

### Task 10.2 通知配置页面

实现内容：

1. 添加飞书 Webhook。
2. 添加企业微信 Webhook。
3. 测试通知。
4. 删除通知通道。
5. webhook 脱敏展示。

---

### Task 10.3 Issue 事件页面

实现内容：

1. 展示新 Issue。
2. 展示状态。
3. 查看 GitHub Issue。
4. 点击执行修复。
5. 点击忽略。

---

### Task 10.4 Diff 审核页面

实现内容：

1. 展示 Patch。
2. 展示修改文件。
3. 展示安全检查。
4. 确认 Diff。
5. 确认并提交 PR。

---

## Phase 11：回归测试

### Task 11.1 主链路测试

测试流程：

1. 添加仓库监听。
2. 配置飞书通知。
3. 手动触发 Issue 轮询。
4. 新 Issue 入库。
5. 收到通知。
6. 点击执行修复。
7. Agent 生成 Patch。
8. 收到 Diff 通知。
9. 确认 Patch。
10. 提交 PR。
11. 收到 PR 通知。

完成标准：

1. 全流程跑通。
2. 数据状态正确。
3. 通知正确。
4. PR 可以打开。

---

### Task 11.2 异常测试

测试内容：

1. GitHub API 失败。
2. Webhook 失败。
3. 重复 Issue。
4. 重复点击执行修复。
5. Agent 修复失败。
6. Patch 应用失败。
7. GitHub Token 无权限。
8. 重复提交 PR。

完成标准：

1. 系统不会崩溃。
2. 状态不会错乱。
3. 错误信息清晰。
