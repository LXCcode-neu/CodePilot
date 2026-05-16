# CodePilot

中文 | English

---

## 中文

### 项目简介

CodePilot 是一个基于 Spring Boot 3 的 Issue-to-Patch / Issue-to-PR Agent 原型项目，面向 GitHub 仓库问题分析、代码检索、LLM 分析与 patch 生成场景。

当前仓库包含：

- Spring Boot 3 后端
- `frontend/` 下的 React + Vite 前端控制台

### 当前已实现

- 用户注册、登录、JWT 鉴权
- 项目仓库记录管理
- Agent 任务创建、查询、异步运行
- JGit 仓库拉取
- LLM function calling 驱动的 grep / glob / read 多轮代码搜索
- LLM 分析与 patch 生成抽象
- patch_record 落库与查询
- Redis 任务运行锁
- Spring Async 后台执行
- SSE 任务事件订阅

### 后端目录结构

后端现在采用单层主包结构，不再在每个主层级下继续按业务二次分包，主要依靠类名区分职责：

```text
src/main/java/com/codepliot
├── client
├── config
├── controller
├── entity
├── exception
├── handler
├── model
├── repository
├── service
├── policy
├── policy
└── utils
```

说明：

- `controller`：HTTP 接口
- `entity`：数据库实体
- `exception`：业务异常定义
- `handler`：全局异常处理、安全过滤器/处理器
- `model`：DTO、VO、上下文、返回模型、枚举
- `repository`：Mapper / Repository
- `service`：业务服务、执行编排、索引与工具实现
- `client`：外部客户端抽象或实现
- `utils`：工具类

### 关键配置

主配置文件：

- [application.yml](/C:/CodePilot/src/main/resources/application.yml)

启动前请准备：

- MySQL 数据库 `codepilot`
- Redis 实例
- 可写的工作目录 `codepilot.workspace.root`
- 合法的 JWT 密钥
- 可用的 LLM 配置

### 启动后端

推荐方式：

```powershell
mvn package -DskipTests
java -jar target/codepilot-0.0.1-SNAPSHOT.jar
```

默认地址：

- `http://localhost:8080`

说明：

- 如果 `mvn clean` 失败，通常是本地有进程占用了 `target/` 下的文件；关闭相关 Java 进程后再执行即可。

### 启动前端开发环境

```powershell
cd frontend
npm install
npm run dev
```

默认地址：

- `http://localhost:5173`

### 打包前端到 Spring Boot

```powershell
cd frontend
npm install
npm run build:static
```

然后启动后端即可通过 `http://localhost:8080` 访问前端。

### SSE 说明

任务实时事件订阅接口：

- `GET /api/tasks/{taskId}/events`

当前前端会将 SSE 收敛为三个阶段：

- 开始
- 执行中
- 完成 / 失败

### 异步执行说明

当前 MVP 使用：

- Spring Async
- Redis 运行锁

而不是 MQ，原因是：

- 当前仍是单体应用
- 任务依赖本地 workspace 与按需 grep / glob / read 代码搜索
- Spring Async + Redis 已能满足当前需求

如果后续演进为多实例或独立 worker 架构，再升级到 RabbitMQ / Redis Stream 更合适。

---

## English

### Overview

CodePilot is a Spring Boot 3 based Issue-to-Patch / Issue-to-PR prototype for GitHub repository analysis, code retrieval, LLM-based issue analysis, and patch generation.

This repository contains:

- a Spring Boot backend
- a React + Vite frontend console under `frontend/`

### Implemented

- user registration, login, and JWT auth
- project repository record management
- agent task creation, querying, and async execution
- JGit repository sync
- LLM function-calling driven multi-round code search with grep / glob / read tools
- global LLM API key list management with encrypted API key storage, active-key switching, and optional per-project overrides
- LLM analysis and patch generation abstraction
- patch record persistence, safety review, and manual confirmation
- AI Patch Review gate after deterministic patch verification
- group robot notification approval links for GitHub Issue auto-fix or ignore decisions
- repair-result group notifications with diff summary and PR draft preview
- Sentry alert webhook intake that can create and auto-run repair tasks
- Feishu app bot message commands for mobile approval: `修复 CP-xxxxx`, `忽略 CP-xxxxx`, `状态 CP-xxxxx`, `确认PR CP-xxxxx`
- task deletion and cascading cleanup when deleting a project repository
- Redis-backed task run lock
- Spring Async background execution
- SSE task event subscription

### Backend Package Layout

The backend keeps the main layers at the top level. The `service` layer is grouped by responsibility so domain workflows do not pile up in one package:

```text
src/main/java/com/codepliot
├── client
├── config
├── controller
├── entity
├── exception
├── handler
├── model
├── repository
├── service
│   ├── agent
│   ├── auth
│   ├── bot
│   ├── git
│   ├── githubIssue
│   ├── llm
│   ├── notification
│   ├── patch
│   ├── project
│   ├── sentry
│   ├── sse
│   └── task
└── utils
```

### Main Configuration

Main config file:

- [application.yml](/C:/CodePilot/src/main/resources/application.yml)

Prepare before startup:

- MySQL database `codepilot`
- Redis instance
- writable `codepilot.workspace.root`
- valid JWT secret
- working LLM configuration
- `CODEPILOT_API_KEY_ENCRYPTION_KEY` for encrypting project-level LLM API keys in the database
- optional `GITHUB_TOKEN` for reading private repositories or increasing GitHub API rate limits
- `GITHUB_CLIENT_ID` for GitHub OAuth authorization
- `GITHUB_CLIENT_SECRET` for GitHub OAuth authorization
- `GITHUB_OAUTH_REDIRECT_URI` for the frontend callback page, for example `http://localhost:5173/github/callback`
- `CODEPILOT_PUBLIC_BASE_URL` for externally reachable group robot action links, for example `https://codepilot.example.com`
- `CODEPILOT_VERIFICATION_COMMAND_TIMEOUT_SECONDS` for each patch verification command timeout, default `300`
- `CODEPILOT_VERIFICATION_MAX_REPAIR_ATTEMPTS` for automatic repair attempts after verification failure, default `3`
- `CODEPILOT_VERIFICATION_AUTO_DETECT_COMMANDS_ENABLED` enables Maven/Go/Python/Node command auto-detection, default `false`
- optional AI Patch Review gate configuration:
  - `CODEPILOT_PATCH_REVIEW_ENABLED`, default `true`
  - `CODEPILOT_PATCH_REVIEW_MIN_SCORE`, default `70`
  - `CODEPILOT_PATCH_REVIEW_FAIL_ON_HIGH_RISK`, default `true`
- optional Sentry alert auto-fix configuration:
  - `CODEPILOT_SENTRY_ENABLED=true`
  - `CODEPILOT_SENTRY_WEBHOOK_TOKEN` for authenticating Sentry webhook calls
  - `SENTRY_AUTH_TOKEN` for server-side Sentry event detail lookup
  - `SENTRY_ORG` for the default Sentry organization slug
  - `SENTRY_API_BASE_URL`, default `https://sentry.io/api/0`
  - `CODEPILOT_SENTRY_AUTO_RUN_ENABLED`, default `true`
- optional Feishu app bot configuration for mobile chat commands:
  - `FEISHU_BOT_ENABLED=true`
  - `FEISHU_APP_ID`
  - `FEISHU_APP_SECRET`
  - `FEISHU_VERIFICATION_TOKEN`
  - `FEISHU_ENCRYPT_KEY`
  - `FEISHU_DEFAULT_CHAT_ID` if you want the Feishu app bot to send the first Issue notification without relying on a webhook channel

LLM configuration:

- Configure global API keys from the sidebar "模型配置" page.
- Multiple keys can be stored; one key is marked active and used by default.
- Project-level overrides can still be configured from project cards when needed.
- API keys are encrypted before being stored in `llm_api_key_config` or `project_llm_config`.
- Task creation uses the project override first, then falls back to the global config.
- Each task stores the provider/model snapshot used at creation time.

### Run Backend

Recommended:

```powershell
mvn package -DskipTests
java -jar target/codepilot-0.0.1-SNAPSHOT.jar
```

Backend address:

- `http://localhost:8080`

Note:

- If `mvn clean` fails, a local process is usually holding files under `target/`.

### Run CLI

The first CLI version is a thin Java client over the running CodePilot backend. Human-facing terminal output is in Simplified Chinese.

```powershell
mvn -q -DskipTests compile exec:java "-Dexec.args=help"
```

Common commands:

```powershell
mvn -q -DskipTests compile exec:java "-Dexec.args=config set-server http://localhost:8080"
mvn -q -DskipTests compile exec:java "-Dexec.args=auth login --username your_name --password your_password"
mvn -q -DskipTests compile exec:java "-Dexec.args=project list"
mvn -q -DskipTests compile exec:java "-Dexec.args=task create --project 1 --title test --desc test"
mvn -q -DskipTests compile exec:java "-Dexec.args=task watch 1"
mvn -q -DskipTests compile exec:java "-Dexec.args=patch confirm 1"
mvn -q -DskipTests compile exec:java "-Dexec.args=pr submit 1"
```

Use `--json` for machine-readable output in CI.

### Run Frontend Dev Server

```powershell
cd frontend
npm install
npm run dev
```

Frontend dev address:

- `http://localhost:5173`

### Build Frontend Into Spring Boot

```powershell
cd frontend
npm install
npm run build:static
```

Then start the backend and open `http://localhost:8080`.

### GitHub Issues

The dashboard can load GitHub issues for repositories added to CodePilot.

- Backend endpoint:
  - `GET /api/projects/{projectId}/github/issues?state=open&page=1&pageSize=20`
- Import endpoint:
  - `POST /api/projects/{projectId}/github/issues/{issueNumber}/import-task`
- Public repositories can be read without a token, but GitHub rate limits are lower.
- Private repositories require `GITHUB_TOKEN` with repository and Issues read access.
- Configure the token before starting the backend:

```powershell
$env:GITHUB_TOKEN="your_github_token_here"
java -jar target/codepilot-0.0.1-SNAPSHOT.jar
```

### Sentry Alert Auto-Fix

CodePilot can receive Sentry alert webhooks and turn them into agent repair tasks.

- Webhook endpoint:
  - `POST /api/sentry/alerts`
- Required webhook token, either as a header:
  - `X-CodePilot-Sentry-Token: <CODEPILOT_SENTRY_WEBHOOK_TOKEN>`
  - or as a query parameter when the Sentry UI cannot add custom headers: `/api/sentry/alerts?token=<CODEPILOT_SENTRY_WEBHOOK_TOKEN>`
- Incoming alerts are stored in `sentry_alert_event`.
- Project-level Sentry mappings are configured from the CodePilot project card with the `Sentry` button and stored in `sentry_project_mapping`.
- CodePilot deduplicates active tasks by Sentry organization, project, and issue/event identity.
- When `CODEPILOT_SENTRY_AUTO_RUN_ENABLED=true`, the created task is submitted to the existing agent runner.
- Generated patches still must pass automatic verification before entering `WAITING_CONFIRM`.
- Sentry auth tokens stay server-side and are never returned to the frontend.

CodePilot setup:

- Start the backend with the Sentry server-side environment variables below.
- Open the project list in the CodePilot frontend.
- Click `Sentry` on the target project card.
- Fill the Sentry organization slug and project slug.
- Choose whether alerts should auto-run repair tasks for that project.

The old `codepilot.sentry.project-mappings` YAML map remains available only as an operations fallback. Normal users should configure mappings in the frontend.

```yaml
codepilot:
  sentry:
    enabled: true
    webhook-token: ${CODEPILOT_SENTRY_WEBHOOK_TOKEN}
    organization-slug: your-sentry-org
```

Local startup example:

```powershell
$env:CODEPILOT_SENTRY_ENABLED="true"
$env:CODEPILOT_SENTRY_WEBHOOK_TOKEN="change-this-random-token"
$env:SENTRY_AUTH_TOKEN="sntrys_your_token_here"
$env:SENTRY_ORG="your-sentry-org"
mvn spring-boot:run
```

Sentry setup:

- Create an Internal Integration in Sentry.
- Set the webhook URL to `https://your-codepilot-domain/api/sentry/alerts?token=<CODEPILOT_SENTRY_WEBHOOK_TOKEN>`, or use `/api/sentry/alerts` plus the `X-CodePilot-Sentry-Token` header if your Sentry setup supports custom headers.
- Enable Alert Rule Action for the integration.
- In an Issue Alert rule, add the action `Send a notification via <your integration>`.
- Use alert filters such as environment and level to avoid creating repair tasks for noisy non-production alerts.

### Issue Auto-Fix Group Approval

When repository watching and a notification channel are configured, new GitHub Issues can be pushed to Feishu or WeCom group robots with an operation code.

- New GitHub Issues discovered by polling are also created as `PENDING` tasks in the task list; approving or running the issue reuses that task instead of creating a second one.
- New Issue notification includes:
  - an action code like `CP-8K2F`
  - mobile chat commands such as `修复 CP-8K2F`, `忽略 CP-8K2F`, and `状态 CP-8K2F`
- Feishu app bot event callback endpoint:
  - `POST /api/bot/events/feishu`
- Action codes are stored in `bot_action_code`, expire after 24 hours, and are bound to the first chat that uses the code.
- After patch generation, CodePilot sends a group notification containing:
  - changed-file summary
  - added/removed line counts
  - PR draft title, branch, commit message, and body preview
  - commands such as `确认PR CP-8K2F` and `状态 CP-8K2F`
- The Feishu app bot can reply directly in the same chat when `FEISHU_APP_ID` and `FEISHU_APP_SECRET` are configured.
- If `FEISHU_DEFAULT_CHAT_ID` is configured, the Feishu app bot also sends the initial Issue notification to that chat. Otherwise, keep using the existing notification channel webhook for the first notification.
- Pull request submission still uses server-side encrypted GitHub OAuth tokens. Tokens are never sent to the frontend or chat.
- Before each agent run and PR submission, the local repository workspace is synced with the latest remote default branch.

### Patch Verification Gate

After a patch is generated, CodePilot applies it in an isolated verification workspace before allowing confirmation.

- `git apply --check` and `git apply` always run and must pass.
- Maven/Go/Python/Node verification commands are not auto-detected by default because many repositories need databases, Redis, secrets, or project-specific setup.
- Set `CODEPILOT_VERIFICATION_AUTO_DETECT_COMMANDS_ENABLED=true` to restore automatic command detection.
- Prefer explicit `codepilot.verification.commands` entries for stable project checks, for example a command name, shell command, and repository-relative working directory.
- If verification fails, the task moves to `VERIFY_FAILED`, PR confirmation is blocked, and the failure is pushed back through the configured notification path.
- When verification fails, CodePilot asks the LLM to generate a corrected replacement patch and retries verification up to `CODEPILOT_VERIFICATION_MAX_REPAIR_ATTEMPTS`, default `3`.
- If all repair attempts fail, the task moves to `VERIFY_FAILED`; a `VERIFY_FAILED` task can be rerun after the underlying issue is fixed or the agent is asked to try again.
- Verification command results are stored in `patch_verification_record` with the task, patch, attempt number, command, exit code, timeout flag, and output summary.

### AI Patch Review Gate

After deterministic verification passes, CodePilot runs an LLM-based code review before a task can enter `WAITING_CONFIRM`.

- The review runs in the `REVIEWING_PATCH` task status.
- The review checks whether the patch is focused, clear, safe, aligned with the issue or Sentry alert, and compliant with repository constraints.
- The LLM must return structured JSON with `passed`, `score`, `riskLevel`, `summary`, `findings`, and `recommendations`.
- Results are stored in `patch_review_record`.
- If the review response cannot be parsed, the review is treated as failed.
- If `score < CODEPILOT_PATCH_REVIEW_MIN_SCORE`, PR confirmation is blocked.
- If `riskLevel=HIGH` and `CODEPILOT_PATCH_REVIEW_FAIL_ON_HIGH_RISK=true`, PR confirmation is blocked.
- Failed review moves the task to `VERIFY_FAILED` and stores the review summary as command evidence context for UI and retry flows.
- The task detail page displays the latest AI Patch Review result, score, risk level, findings, and recommendations.

### GitHub Account Authorization

CodePilot now supports GitHub OAuth account authorization for importing repositories into the project list and for submitting pull requests with the connected GitHub identity.

- Frontend route:
  - `GET /github-auth`
  - `GET /github/callback`
- Backend endpoints:
  - `GET /api/github/auth-url`
  - `POST /api/github/callback`
  - `GET /api/github/account`
  - `DELETE /api/github/account`
  - `GET /api/github/repositories`
  - `POST /api/projects/import-github-repo`
- OAuth access tokens are encrypted before being stored in `user_github_account`.
- Imported repositories are persisted in `project_repo` and keep GitHub owner/name/id metadata for later Issue and PR operations.

Local setup example:

```powershell
$env:GITHUB_CLIENT_ID="your_github_oauth_client_id"
$env:GITHUB_CLIENT_SECRET="your_github_oauth_client_secret"
$env:GITHUB_OAUTH_REDIRECT_URI="http://localhost:5173/github/callback"
mvn spring-boot:run
```

### SSE Note

Task event subscription endpoint:

- `GET /api/tasks/{taskId}/events`

The current frontend condenses SSE into three stages:

- started
- running
- completed / failed / cancelled

### Agentic Code Search

Code search uses an agentic tool loop:

- the LLM chooses `grep`, `glob`, and `read` tool calls
- the backend executes tools and returns observations
- search stops when the LLM stops requesting tools
- there is no fixed search-round limit
- `codepilot.search.max-duration` is a runtime safety guard, not a search strategy limit

### Patch Confirmation

- After patch generation and successful automatic verification, tasks enter `WAITING_CONFIRM` instead of going directly to `COMPLETED`
- The backend runs a rule-based patch safety check before manual confirmation
- Manual confirmation endpoint:
  - `POST /api/tasks/{taskId}/confirm`
- This MVP still does not auto-apply patches or create PRs

### Task Cancellation And Rerun

- Manual task cancellation endpoint:
  - `POST /api/tasks/{taskId}/cancel`
- Running tasks move through `CANCEL_REQUESTED` and then `CANCELLED` once the executor reaches a cancellation checkpoint.
- The executor interrupts the registered worker thread so blocking LLM HTTP calls can exit earlier when the provider/client honors interruption.
- Verification subprocesses are stopped when cancellation is requested.
- `PENDING`, `FAILED`, `VERIFY_FAILED`, and `CANCELLED` tasks can be run again.
- Before every rerun, old agent steps, patch records, and verification records for that task are deleted so the detail page starts from the new clone step instead of stacking historical runs.

### Deletion Behavior

- Manual task deletion endpoint:
  - `DELETE /api/tasks/{taskId}`
- Deleting a project repository now also deletes its related agent tasks, agent steps, and patch records
- Running tasks cannot be deleted until the current run finishes

### Async Execution Note

The current MVP uses:

- Spring Async
- Redis task run locks

instead of MQ, because:

- the app is still a monolith
- task execution depends on local workspace files and on-demand grep / glob / read code search
- Spring Async + Redis is sufficient for the current stage

RabbitMQ or Redis Stream would be a more natural next step for multi-instance or worker-based deployment later.
