# Design: GitHub Issue 自动监听与通知修复闭环

## 1. 总体架构

本功能在现有 CodePilot 架构上新增四个模块：

1. GitHub Issue Listener
2. Notification Center
3. Issue Confirm Workflow
4. Pull Request Service

整体结构：

```text
GitHub API
    |
    v
GitHubIssuePollingJob
    |
    v
GitHubIssueEventService
    |
    v
NotificationService
    |
    v
飞书 / 企业微信

用户确认执行
    |
    v
AgentTaskService
    |
    v
AgentExecutor
    |
    v
PatchRecord
    |
    v
NotificationService

用户确认提交 PR
    |
    v
PullRequestService
    |
    v
GitHub
```

## 2. 模块设计

### 2.1 GitHub Issue Listener

负责监听用户绑定仓库的新 Issue。

第一阶段使用定时轮询：

```text
@Scheduled
每 5 分钟扫描 watch_enabled = true 的仓库
调用 GitHub API 获取 open issues
对比数据库已有 issue
新 issue 入库
触发通知
```

后续可替换为 GitHub Webhook。

### 2.2 Notification Center

负责统一推送通知。

支持：

1. 飞书机器人 Webhook
2. 企业微信群机器人 Webhook

抽象接口：

```java
public interface NotificationSender {
    NotificationChannelType type();
    void send(NotificationChannel channel, NotificationMessage message);
}
```

实现类：

```text
FeishuNotificationSender
WeComNotificationSender
```

### 2.3 Issue Confirm Workflow

负责用户确认是否执行修复。

核心逻辑：

```text
github_issue_event.status = NEW / NOTIFIED
        ↓
用户点击执行修复
        ↓
创建 agent_task
        ↓
github_issue_event.status = TASK_CREATED
```

### 2.4 Pull Request Service

负责用户确认 Diff 后提交 PR。

核心步骤：

```text
1. 检查 patch_record 是否已确认
2. 检查用户 GitHub Token
3. 创建新分支
4. 应用 patch
5. git add
6. git commit
7. git push
8. 调用 GitHub API 创建 PR
9. 保存 PR 信息
10. 推送通知
```

## 3. 数据库设计

### 3.1 user_repo_watch

用户监听的 GitHub 仓库。

```sql
CREATE TABLE user_repo_watch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    owner VARCHAR(100) NOT NULL,
    repo_name VARCHAR(100) NOT NULL,
    repo_url VARCHAR(500) NOT NULL,
    default_branch VARCHAR(100) DEFAULT 'main',
    watch_enabled TINYINT(1) NOT NULL DEFAULT 1,
    watch_mode VARCHAR(32) NOT NULL DEFAULT 'POLLING',
    last_checked_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_repo (user_id, owner, repo_name)
);
```

字段说明：

```text
user_id：所属用户
owner：GitHub 仓库 owner
repo_name：仓库名称
repo_url：仓库地址
default_branch：默认分支
watch_enabled：是否开启监听
watch_mode：监听模式，第一阶段使用 POLLING
last_checked_at：上次检查时间
```

---

### 3.2 github_issue_event

系统发现的 GitHub Issue。

```sql
CREATE TABLE github_issue_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    repo_watch_id BIGINT NOT NULL,
    github_issue_id BIGINT NOT NULL,
    issue_number INT NOT NULL,
    issue_title VARCHAR(500) NOT NULL,
    issue_body TEXT,
    issue_url VARCHAR(500),
    issue_state VARCHAR(32) NOT NULL,
    sender_login VARCHAR(100),
    event_action VARCHAR(32) DEFAULT 'opened',
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    agent_task_id BIGINT DEFAULT NULL,
    notified_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_repo_issue (repo_watch_id, issue_number)
);
```

status 枚举：

```text
NEW            新发现，未处理
NOTIFIED       已通知用户
IGNORED        用户忽略
TASK_CREATED   已创建 AgentTask
RUNNING        Agent 正在修复
PATCH_READY    Patch 已生成
PR_CREATED     PR 已创建
FAILED         处理失败
```

---

### 3.3 notification_channel

用户配置的通知通道。

```sql
CREATE TABLE notification_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    channel_name VARCHAR(100),
    webhook_url TEXT NOT NULL,
    secret VARCHAR(255) DEFAULT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

channel_type 枚举：

```text
FEISHU
WE_COM
```

注意：

```text
webhook_url 必须加密保存。
日志中禁止打印完整 webhook。
前端展示时需要脱敏。
```

---

### 3.4 notification_record

通知记录，可选，但建议实现。

```sql
CREATE TABLE notification_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    title VARCHAR(255),
    content TEXT,
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    sent_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

status 枚举：

```text
PENDING
SUCCESS
FAILED
```

---

### 3.5 patch_record 扩展

如果当前 patch_record 没有以下字段，需要补充：

```sql
ALTER TABLE patch_record
ADD COLUMN safety_check_result TEXT DEFAULT NULL;

ALTER TABLE patch_record
ADD COLUMN confirmed TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE patch_record
ADD COLUMN confirmed_at DATETIME DEFAULT NULL;

ALTER TABLE patch_record
ADD COLUMN pr_url VARCHAR(500) DEFAULT NULL;

ALTER TABLE patch_record
ADD COLUMN pr_number INT DEFAULT NULL;

ALTER TABLE patch_record
ADD COLUMN pr_status VARCHAR(32) DEFAULT NULL;
```

### 3.6 agent_task 扩展

如果当前 agent_task 表没有 source 字段，可以新增：

```sql
ALTER TABLE agent_task
ADD COLUMN source_type VARCHAR(32) DEFAULT 'MANUAL';

ALTER TABLE agent_task
ADD COLUMN source_id BIGINT DEFAULT NULL;
```

source_type 枚举：

```text
MANUAL
GITHUB_ISSUE
```

## 4. 后端包结构

建议新增：

```text
com.codepilot.github
  GitHubIssueClient.java
  GitHubIssuePollingJob.java
  GitHubIssueEventService.java
  GitHubPullRequestService.java
  dto/
    GitHubIssueDTO.java
    CreatePullRequestRequest.java

com.codepilot.notification
  NotificationService.java
  NotificationSender.java
  NotificationMessage.java
  NotificationChannelType.java
  NotificationEventType.java
  FeishuNotificationSender.java
  WeComNotificationSender.java
  NotificationTemplateFactory.java

com.codepilot.watch
  UserRepoWatchController.java
  UserRepoWatchService.java
  UserRepoWatch.java
  UserRepoWatchMapper.java

com.codepilot.issue
  GitHubIssueEventController.java
  GitHubIssueEvent.java
  GitHubIssueEventMapper.java

com.codepilot.pr
  PullRequestController.java
  PullRequestService.java
```

## 5. 核心接口设计

### 5.1 添加仓库监听

```http
POST /api/repo-watches
```

请求：

```json
{
  "owner": "turlin",
  "repoName": "codepilot-demo",
  "repoUrl": "https://github.com/turlin/codepilot-demo",
  "defaultBranch": "main"
}
```

响应：

```json
{
  "id": 1,
  "owner": "turlin",
  "repoName": "codepilot-demo",
  "watchEnabled": true
}
```

---

### 5.2 查询仓库监听列表

```http
GET /api/repo-watches
```

响应：

```json
[
  {
    "id": 1,
    "owner": "turlin",
    "repoName": "codepilot-demo",
    "repoUrl": "https://github.com/turlin/codepilot-demo",
    "watchEnabled": true,
    "lastCheckedAt": "2026-05-12T10:00:00"
  }
]
```

---

### 5.3 启用或禁用仓库监听

```http
PUT /api/repo-watches/{id}/enabled
```

请求：

```json
{
  "enabled": false
}
```

---

### 5.4 添加通知通道

```http
POST /api/notification/channels
```

请求：

```json
{
  "channelType": "FEISHU",
  "channelName": "CodePilot 飞书通知群",
  "webhookUrl": "https://open.feishu.cn/open-apis/bot/v2/hook/xxx"
}
```

响应：

```json
{
  "id": 1,
  "channelType": "FEISHU",
  "channelName": "CodePilot 飞书通知群",
  "enabled": true
}
```

---

### 5.5 查询通知通道

```http
GET /api/notification/channels
```

响应：

```json
[
  {
    "id": 1,
    "channelType": "FEISHU",
    "channelName": "CodePilot 飞书通知群",
    "webhookMasked": "https://open.feishu.cn/open-apis/bot/v2/hook/***abcd",
    "enabled": true
  }
]
```

---

### 5.6 测试通知通道

```http
POST /api/notification/channels/{id}/test
```

响应：

```json
{
  "success": true,
  "message": "测试通知发送成功"
}
```

---

### 5.7 查询 Issue 事件列表

```http
GET /api/issues/events
```

响应：

```json
[
  {
    "id": 1,
    "repoName": "codepilot-demo",
    "issueNumber": 18,
    "issueTitle": "登录接口空指针异常",
    "issueUrl": "https://github.com/turlin/codepilot-demo/issues/18",
    "status": "NEW",
    "createdAt": "2026-05-12T10:00:00"
  }
]
```

---

### 5.8 忽略 Issue

```http
POST /api/issues/events/{id}/ignore
```

响应：

```json
{
  "success": true
}
```

---

### 5.9 基于 Issue 执行修复

```http
POST /api/issues/events/{id}/run
```

响应：

```json
{
  "taskId": 1001,
  "status": "TASK_CREATED"
}
```

业务逻辑：

```text
1. 校验 issue_event 是否属于当前用户
2. 校验状态是否允许执行
3. 创建 AgentTask
4. 写入 issue 标题和描述
5. 更新 issue_event.agent_task_id
6. 更新 issue_event.status = TASK_CREATED
7. 异步启动 AgentExecutor
```

---

### 5.10 确认 Diff

```http
POST /api/patches/{patchId}/confirm
```

响应：

```json
{
  "success": true,
  "confirmed": true
}
```

---

### 5.11 确认并提交 PR

```http
POST /api/patches/{patchId}/confirm-and-pr
```

请求：

```json
{
  "branchName": "codepilot/fix-issue-18",
  "commitMessage": "fix: resolve issue #18",
  "prTitle": "fix: resolve issue #18",
  "prBody": "This PR was generated by CodePilot."
}
```

响应：

```json
{
  "prUrl": "https://github.com/turlin/codepilot-demo/pull/22",
  "prNumber": 22
}
```

## 6. 定时轮询设计

### 6.1 轮询任务

```java
@Component
public class GitHubIssuePollingJob {

    @Scheduled(fixedDelay = 300000)
    public void pollOpenIssues() {
        // 1. 查询 watch_enabled = true 的仓库
        // 2. 调用 GitHub API 获取 open issues
        // 3. 对比数据库是否已存在
        // 4. 新 Issue 入库
        // 5. 推送通知
        // 6. 更新 last_checked_at
    }
}
```

### 6.2 去重规则

使用：

```text
repo_watch_id + issue_number
```

作为唯一键。

### 6.3 轮询范围

第一阶段只处理：

```text
state = open
pull_request 字段为空
```

因为 GitHub API 中 Pull Request 也可能出现在 issues 列表中，需要过滤掉 PR。

## 7. 通知设计

### 7.1 NotificationMessage

```java
public record NotificationMessage(
        String title,
        String content,
        NotificationEventType eventType,
        String linkUrl
) {}
```

### 7.2 NotificationEventType

```java
public enum NotificationEventType {
    NEW_ISSUE,
    REPAIR_STARTED,
    PATCH_READY,
    REPAIR_FAILED,
    PR_CREATED
}
```

### 7.3 新 Issue 通知模板

```text
【CodePilot 发现新 Issue】

仓库：{owner}/{repo}
Issue：#{issueNumber} {issueTitle}
创建人：{senderLogin}

请进入 CodePilot 确认是否让 Agent 修复：
{issueEventUrl}
```

### 7.4 Patch Ready 通知模板

```text
【CodePilot 已生成修复 Diff】

仓库：{owner}/{repo}
Issue：#{issueNumber} {issueTitle}
任务：#{taskId}

Agent 已生成修复 Diff，等待你确认是否提交 PR。

查看 Diff：
{patchReviewUrl}
```

### 7.5 PR 创建成功通知模板

```text
【CodePilot 已创建 Pull Request】

仓库：{owner}/{repo}
PR：#{prNumber} {prTitle}

查看 PR：
{prUrl}
```

## 8. 飞书发送实现

```java
@Service
public class FeishuNotificationSender implements NotificationSender {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.FEISHU;
    }

    @Override
    public void send(NotificationChannel channel, NotificationMessage message) {
        Map<String, Object> body = Map.of(
                "msg_type", "text",
                "content", Map.of(
                        "text", message.title() + "\n\n" + message.content()
                )
        );

        restTemplate.postForObject(channel.getWebhookUrl(), body, String.class);
    }
}
```

## 9. 企业微信发送实现

```java
@Service
public class WeComNotificationSender implements NotificationSender {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.WE_COM;
    }

    @Override
    public void send(NotificationChannel channel, NotificationMessage message) {
        Map<String, Object> body = Map.of(
                "msgtype", "text",
                "text", Map.of(
                        "content", message.title() + "\n\n" + message.content()
                )
        );

        restTemplate.postForObject(channel.getWebhookUrl(), body, String.class);
    }
}
```

## 10. AgentTask 创建规则

当用户对某个 Issue 点击执行修复时，创建 AgentTask：

```text
projectId：对应仓库项目 ID
issueTitle：github_issue_event.issue_title
issueDescription：github_issue_event.issue_body + issue_url
sourceType：GITHUB_ISSUE
sourceId：github_issue_event.id
status：PENDING
```

## 11. PR 提交设计

### 11.1 分支命名规则

默认：

```text
codepilot/fix-issue-{issueNumber}
```

例如：

```text
codepilot/fix-issue-18
```

### 11.2 Commit Message

默认：

```text
fix: resolve issue #{issueNumber}
```

### 11.3 PR Title

默认：

```text
fix: resolve issue #{issueNumber} - {issueTitle}
```

### 11.4 PR Body

默认：

```md
This PR was generated by CodePilot.

Related issue: #{issueNumber}

## Summary

{patchSummary}

## Safety Check

{safetyCheckResult}
```

## 12. 安全设计

### 12.1 Webhook URL 安全

要求：

1. 加密保存 webhook_url。
2. 日志禁止打印完整 webhook。
3. 前端展示脱敏。
4. 删除通知通道时清理 webhook。

### 12.2 GitHub Token 安全

如果 V2 实现 PR 提交，需要用户提供 GitHub Token。

要求：

1. token 加密保存。
2. token 不返回前端。
3. token 日志脱敏。
4. token 只在提交 PR 时解密使用。
5. 权限不足时提示用户检查权限。

### 12.3 Patch 安全

要求：

1. 未经用户确认，不允许 push 到 GitHub。
2. patch 应用失败，不允许继续创建 PR。
3. PR 创建失败时记录错误。
4. 修改文件路径必须限制在仓库目录内，避免路径穿越。

## 13. 状态流转

### github_issue_event 状态流转

```text
NEW
  ↓ 通知成功
NOTIFIED
  ↓ 用户忽略
IGNORED

NOTIFIED
  ↓ 用户点击执行修复
TASK_CREATED
  ↓ Agent 开始执行
RUNNING
  ↓ Patch 生成
PATCH_READY
  ↓ 用户确认并提交 PR
PR_CREATED

任意执行失败
  ↓
FAILED
```

### patch_record 状态流转

```text
created
  ↓
confirmed = false
  ↓ 用户确认
confirmed = true
  ↓ 创建 PR
pr_status = CREATED
```

## 14. 前端页面设计

### 14.1 仓库监听页面

功能：

1. 添加仓库监听。
2. 查看已监听仓库。
3. 启用/禁用监听。
4. 查看最近检查时间。

### 14.2 通知配置页面

功能：

1. 添加飞书 Webhook。
2. 添加企业微信 Webhook。
3. 测试通知。
4. 启用/禁用通知通道。
5. 删除通知通道。

### 14.3 Issue 事件页面

功能：

1. 展示系统发现的新 Issue。
2. 查看 Issue 标题、仓库、创建人、状态。
3. 点击查看 GitHub Issue。
4. 点击“执行修复”。
5. 点击“忽略”。

### 14.4 Diff 审核页面

功能：

1. 展示 patch diff。
2. 展示修改文件列表。
3. 展示 Agent 修复摘要。
4. 展示安全检查结果。
5. 点击“确认”。
6. 点击“确认并提交 PR”。

## 15. 与现有系统的关系

本功能不重构现有 AgentExecutor。

新增流程只负责把 GitHub Issue 转换成 AgentTask。

也就是说：

```text
GitHub Issue Event
        ↓
AgentTask
        ↓
复用现有 AgentExecutor
```

现有模块保持不变：

1. 用户登录注册。
2. 仓库管理。
3. Agent 任务执行。
4. 代码检索。
5. Patch 生成。
6. SSE 轨迹展示。

新增模块作为入口和出口：

```text
入口：Issue 自动监听 + 通知
出口：用户确认 + PR 提交
```
