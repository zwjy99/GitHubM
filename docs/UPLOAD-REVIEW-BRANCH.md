# GitHubM 安全上传、文件夹导入与 Fork 同步

本页面从源码直接审核上传，不会直接更改原仓库默认分支。

## 三种入口共用同一提交机制

1. **一次选择多个文件**：在系统文件选择框内一次勾选多个文件，而非需要多次选择；适合少量普通文件。
2. **选择文件夹**：保留 `src/`、`android/` 等层级。Android WebView/文件管理器若不支持 `webkitdirectory`，可把同一文件夹打成 ZIP 再导入。
3. **源码 ZIP**：在设备上解压并审查路径，只加入白名单范围内的正常源码文件。遇到压缩格式不支持、单文件超过限制，停止导入，避免项目残缺。

所有入口都走：**选择文件 → 完整差异预览 → 创建独立 `githubm/upload-...` 分支 → 分批提交（每批 20 文件，一个 Git commit）→ 手动发起 Pull Request**。默认保留同名旧文件；覆盖时必须显式勾选确认。上传过程中远端分支如果变化，将拒绝非快进更新。

## 大型源码

本地 ZIP 限 96 MB 压缩内容 / 160 MB 解压总量 / 1,500 个文件，单文件最多 32 MB。选择大包会占用手机内存，请优先用电脑端 Git 或 GitHub Desktop 处理特别大的源码。Git Blob API 比 Contents API 更适合大于 1MB 的源文件，但它**不等于 Git LFS**；大型二进制不适合用这个工具上传。

出于安全考虑会排除 `.env`、签名密钥、凭证及依赖缓存；务必核对「被排除」文件后再发布。如涉及敏感文件请通过 GitHub Secrets 管理。ZIP 内存在不支持的压缩项时会取消整包导入，不提交不完整的工程。

## 自动同步上游 Fork

- **手动同步**：调用 GitHub 官方 `POST /repos/{owner}/{repo}/merge-upstream`，当上游冲突或权限不足时显示错误，不强制覆盖。
- **定时同步**：可以在页面中将可选的 `githubm-sync-upstream.yml` 模板加入上传队列，使用相同的新分支、差异预览和 PR 审核。只有合并到 Fork **默认分支**、允许 GitHub Actions 运行且赋予 `contents: write` 后，计划任务才能执行；GitHub 可能默认关闭 Fork 的定时工作流，需在仓库 Actions 设置中启用。
- 上游的自动同步会尝试更新**你的 Fork 的默认分支**，并不是只读跟踪；如果默认分支上有你的本地改动，建议改用手动同步、创建 PR 审核。
- 这不是后台手机自动拉取，也不能在关闭 App 时依赖手机完成。定时运行由 GitHub Actions 提供。

官方参考：
- [Create a Git tree](https://docs.github.com/en/rest/git/trees#create-a-tree)
- [Sync a fork branch](https://docs.github.com/en/rest/branches/branches#sync-a-fork-branch-with-the-upstream-repository)
- [GitHub Actions 触发事件 schedule](https://docs.github.com/en/actions/reference/events-that-trigger-workflows#schedule)
- [文件夹选择兼容性](https://developer.mozilla.org/en-US/docs/Web/API/HTMLInputElement/webkitdirectory)

## 与应用库来源匹配的关系

应用库仍以**在线源码中的真实包名声明、版本、Fork 上游与安装包元数据**寻找候选；离线来源目录只作为辅助缓存，不是判定没有仓库的依据。此次主要修复上传流程，不改变已确认的来源关联。
