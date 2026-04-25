# Agent Runtime Spec

中文 | English

---

## 中文说明

### 文件定位

本文件描述的是 **CodePilot 平台内部业务 Agent 的运行规范**，不是仓库开发规范。

它用于约束未来平台中的 Issue-to-PR 修复 Agent 在执行任务时应遵循的目标、阶段划分和边界。

### 运行目标

平台内 Agent 的核心目标是：

1. 接收 GitHub Issue 相关上下文
2. 理解待修复问题
3. 检索相关代码
4. 生成修复建议
5. 输出候选 patch
6. 记录执行轨迹，供用户查看

### MVP 范围

当前只定义 MVP 期望，不表示已经完成实现：

- 支持公开 GitHub 仓库
- 以 Java 项目为主要扫描对象
- 支持 Issue 驱动的任务创建
- 支持修复建议和 patch 候选输出
- 支持轨迹展示

### 当前明确不做

以下能力暂不纳入运行时规范实现范围：

- GitHub OAuth
- 自动提交 PR
- 多 Agent 协作
- 向量数据库召回
- 自动运行测试

### 预期执行阶段

未来业务 Agent 的执行流程建议拆分为以下阶段：

1. `issue_intake`
   - 接收 Issue 标题、描述、仓库信息
2. `repo_prepare`
   - 拉取或更新目标公开仓库
3. `java_scan`
   - 扫描 Java 文件，建立基础索引
4. `code_retrieval`
   - 根据 Issue 内容检索候选代码
5. `repair_plan`
   - 生成修复思路与风险提示
6. `patch_generation`
   - 生成候选 patch
7. `trace_publish`
   - 输出执行轨迹

### 运行边界

1. Agent 生成的 patch 默认是“候选结果”，不应被视为已验证修复。
2. 在未接入自动测试前，Agent 不应宣称修复已经被验证通过。
3. 在未接入 PR 自动化前，Agent 不应直接推送远程仓库。
4. 在未启用多 Agent 前，默认按单 Agent 流程设计。
5. LLM 输出必须保留可审计轨迹，不应只保存最终答案。

### 后续扩展建议

未来可以在本文件继续补充：

- Agent 输入输出协议
- Trace 事件模型
- Prompt 分层策略
- Patch 置信度模型
- 人工审核与确认机制

---

## English

### Purpose

This file describes the **runtime specification for business agents inside the CodePilot platform**, not the repository development rules.

It defines the intended goals, phases, and boundaries for the future Issue-to-PR fixing agent.

### Runtime Goals

The core goals of the platform agent are:

1. receive GitHub Issue context
2. understand the issue to be fixed
3. retrieve related code
4. generate repair suggestions
5. output candidate patches
6. record execution traces for user visibility

### MVP Scope

This document describes the intended MVP scope only. It does not mean these capabilities are already implemented.

- support public GitHub repositories
- focus on Java code scanning
- support issue-driven task creation
- support repair suggestion and candidate patch output
- support trace display

### Explicitly Out of Scope for Now

The following are intentionally excluded from the current runtime scope:

- GitHub OAuth
- automatic PR submission
- multi-agent collaboration
- vector database retrieval
- automatic test execution

### Suggested Execution Stages

The future business agent workflow is recommended to be split into these stages:

1. `issue_intake`
2. `repo_prepare`
3. `java_scan`
4. `code_retrieval`
5. `repair_plan`
6. `patch_generation`
7. `trace_publish`

### Runtime Boundaries

1. Generated patches are candidate outputs by default and should not be treated as verified fixes.
2. Before automated testing is integrated, the agent must not claim the fix is validated.
3. Before PR automation is integrated, the agent must not push directly to remote repositories.
4. Before multi-agent support is introduced, the design should assume a single-agent workflow.
5. LLM outputs should remain auditable, and the platform should preserve more than the final answer alone.

### Future Extensions

This document can later be extended with:

- agent input/output contracts
- trace event model
- prompt layering strategy
- patch confidence model
- human review and approval flow
