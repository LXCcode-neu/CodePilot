# CodePilot

中文 | English

---

## 中文说明

### 项目简介

CodePilot 是一个基于 Spring Boot 3 的 Issue-to-PR Agent 平台，面向 GitHub Issue 驱动的代码修复场景。

当前项目已经完成基础工程初始化，并实现了第一阶段的认证能力：

- 用户注册
- 用户登录
- JWT 鉴权
- 当前登录用户信息获取

### 当前已实现

- Java 17 + Spring Boot 3 + Maven 工程
- MyBatis-Plus / MySQL / Redis 基础依赖
- Spring Security + JWT 认证骨架
- 用户注册接口 `POST /api/auth/register`
- 用户登录接口 `POST /api/auth/login`
- 当前用户接口 `GET /api/user/me`
- 统一返回 `Result<T>`
- 错误码枚举 `ErrorCode`
- 业务异常 `BusinessException`
- 全局异常处理 `GlobalExceptionHandler`
- 基础实体 `BaseEntity`

### 当前未实现

以下模块当前仍然只保留骨架，尚未实现具体业务逻辑：

- 仓库管理
- Agent 任务创建
- GitHub 公开仓库拉取
- Java 文件扫描
- 相关代码检索
- 修复建议和 patch 生成
- 执行轨迹展示
- LLM 调用逻辑

### 包结构

```text
src/main/java/com/codepliot
├─ auth
├─ user
├─ project
├─ task
├─ git
├─ index
├─ agent
├─ llm
├─ trace
├─ sse
└─ common
```

### 配置文件

核心配置文件：

- [application.yml](/C:/CodePilot/demo/src/main/resources/application.yml)

启动前请准备：

- MySQL 数据库 `codepilot`
- Redis 服务
- 合法的 JWT 密钥

### 启动方式

使用 Maven Wrapper：

```powershell
.\mvnw.cmd spring-boot:run
```

或使用本机 Maven：

```powershell
mvn spring-boot:run
```

### 认证接口

注册：

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "demo_user",
  "password": "123456",
  "email": "demo@example.com"
}
```

登录：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "demo_user",
  "password": "123456"
}
```

获取当前用户：

```http
GET /api/user/me
Authorization: Bearer <token>
```

### 下一步建议

建议下一步实现 `project` 模块，包括：

- 仓库接入
- 公开仓库基础信息管理
- 后续 Git 拉取入口

因为认证能力已经具备，接下来最自然的就是把“用户 -> 仓库 -> Agent 任务”这条主链路往前推进。

---

## English

### Overview

CodePilot is an Issue-to-PR Agent platform built with Spring Boot 3 for GitHub Issue driven code-repair workflows.

The project now includes the initial backend scaffold and the first usable authentication layer:

- user registration
- user login
- JWT authentication
- current user info retrieval

### Implemented

- Java 17 + Spring Boot 3 + Maven
- MyBatis-Plus / MySQL / Redis base dependencies
- Spring Security + JWT auth scaffold
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/user/me`
- unified `Result<T>`
- `ErrorCode`
- `BusinessException`
- `GlobalExceptionHandler`
- `BaseEntity`

### Not Implemented Yet

The following modules are still placeholders only:

- repository management
- agent task creation
- public GitHub repository pulling
- Java file scanning
- related code retrieval
- repair suggestion and patch generation
- execution trace display
- LLM integration logic

### Configuration

Main configuration file:

- [application.yml](/C:/CodePilot/demo/src/main/resources/application.yml)

Before starting the project, prepare:

- MySQL database `codepilot`
- Redis instance
- a valid JWT secret

### Run

Using Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Or with local Maven:

```powershell
mvn spring-boot:run
```

### Recommended Next Step

Implement the `project` module next so the system can move from authenticated users to repository onboarding, which is the natural next step in the Issue-to-PR workflow.
