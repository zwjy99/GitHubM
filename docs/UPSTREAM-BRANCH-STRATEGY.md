# Fork 上游更新与个人修改分支：建议操作

## 为什么要分开两种更新

- 原作者上游仓库的更新，不会自动更新你的 Fork。
- 个人修改应放在独立分支，如 `my-changes`；默认分支可以用于追踪原作者的最新版本。
- 不要使用 `git push --force`、不要在个人修改分支执行无审查的覆盖上传。

## 目前新增的安全方案

通过 GitHubM 的上传页面生成 `githubm-review-upstream.yml`，提交到一个新审核分支，再通过 Pull Request 合并到默认分支。

- 定时：检查仓库官方 `parent`，发现比默认分支更新的上游提交时，新建 `githubm/upstream-review-...` 审核分支并提交 PR，**不自动合并**。
- 手动：在 Actions 中运行此工作流并将 `target_branch` 填为你的 `my-changes`，即可向个人修改分支提出合并上游更新的 PR。
- 无更新：什么都不做；冲突：停止并列出冲突文件，不改变远端目标分支。
- 如果之前的审核 PR 还在，重新运行不会再创建同名目标的重复 PR。
- PR 的检查可能需要人工在 GitHub 里批准运行；不同仓库权限、分支保护和工作流策略也会影响是否能创建 PR。

## 推荐分支布局

```text
上游 upstream/main     原作者最新内容（只读）
你自己的 main          通过 PR 审核后跟随上游
你自己的 my-changes    个人增强与修改（保留提交历史）
临时 githubm/upstream-review-...    审核上游合并提议
```

无需每次收到上游更新都重新创建个人修改分支。Git 会保留独立提交，新的更新通过正常合并/冲突解决融入原有分支；对于仅少量改动，建议始终把修改保留在少量清晰提交中。

## 使用前需注意

1. 用户必须把工作流提交并合入默认分支，且为 Fork 启用 Actions 的定时工作流。
2. 工作流使用仓库 `GITHUB_TOKEN`，需允许 Contents 和 Pull requests 写权限及允许 Actions 创建 Pull Request。
3. 某些 GitHub Actions 自动创建的 PR 可能需要人工批准检查。
4. PR 合并之前，默认分支或个人修改分支不会自动变更。
5. 以前生成的 `githubm-sync-upstream.yml` 会直接同步默认分支；**新模板不会自动移除旧工作流**，启用前请检查并手动禁用旧版定时同步以免冲突。

官方文档：
- https://docs.github.com/en/pull-requests/how-tos/work-with-forks/syncing-a-fork
- https://docs.github.com/en/actions/how-tos/manage-workflow-runs/disable-and-enable-workflows
- https://docs.github.com/en/actions/concepts/security/github_token
