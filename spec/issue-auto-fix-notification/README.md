# issue-auto-fix-notification

这是 CodePilot 的 SDD spec 文档包，用于实现：

GitHub Issue 自动监听 + 飞书/企业微信通知 + 用户确认修复 + Diff 审核 + PR 提交。

建议放到项目目录：

```text
specs/issue-auto-fix-notification/
  requirements.md
  design.md
  tasks.md
  test-plan.md
  codex-prompt.md
```

使用方式：

1. 把本文件夹复制到 CodePilot 项目的 `specs/` 目录下。
2. 将 `codex-prompt.md` 的内容发给 Codex。
3. 让 Codex 按 `tasks.md` 分阶段实现。
4. 每完成一个阶段先运行项目和测试，再进入下一阶段。
