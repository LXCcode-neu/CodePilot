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
- LLM analysis and patch generation abstraction
- patch record persistence, safety review, and manual confirmation
- task deletion and cascading cleanup when deleting a project repository
- Redis-backed task run lock
- Spring Async background execution
- SSE task event subscription

### Backend Package Layout

The backend now uses a flat top-level layer layout. There are no additional business subpackages under each main layer; class names are used to convey the domain meaning:

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

### SSE Note

Task event subscription endpoint:

- `GET /api/tasks/{taskId}/events`

The current frontend condenses SSE into three stages:

- started
- running
- completed / failed

### Patch Confirmation

- After patch generation, tasks now enter `WAITING_CONFIRM` instead of going directly to `COMPLETED`
- The backend runs a rule-based patch safety check before manual confirmation
- Manual confirmation endpoint:
  - `POST /api/tasks/{taskId}/confirm`
- This MVP still does not auto-apply patches or create PRs

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
