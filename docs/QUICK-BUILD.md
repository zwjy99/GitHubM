# GitHubM 快捷构建说明

## 解决什么问题

仓库提供源代码、但没有 Release 安装包，不代表它一定能直接构建。GitHubM 的 **仓库 → Actions → 快捷构建** 会优先读取仓库**现有** GitHub Actions 工作流，不重复生成一套“默认工程”。

- 按工作流名称与路径分类展示 Android / Web / 桌面 / 一般构建选项；只是标签，真实产物由 YAML 决定。
- 列表通过 REST API 加载，初始最多 100 项，后续分页由用户主动加载，避免批量请求消耗额度。
- 点击“检查参数并运行”时，读取 **默认分支**的 YAML，必须确认存在 `workflow_dispatch`；不能手动触发的工作流不误显示为可运行。
- 读取 `workflow_dispatch.inputs` 常见 YAML 写法，自动填写默认值、提供选项与必填校验；复杂 YAML 不能完整解析时提示用户检查原文件。
- 默认选用仓库 API 返回的 `default_branch`，不是固定 `main`，也允许选择其他分支或输入名称。
- 提交运行后保留原有 **运行记录 / Jobs / 日志**；产物按钮进入原有 **Artifacts / Releases** 页面。
- Actions 需要具有足够的仓库 **Actions 写权限**，且仓库启用了 Actions；没有权限时仍允许查看公开的工作流详情。

GitHub 官方规则：仅当默认分支包含该工作流的 `workflow_dispatch` 配置时，才能手动触发。参见 https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow 和 https://docs.github.com/en/rest/actions/workflows

## 代码齐全，但是没有工作流？

点“分析项目并推荐工作流”，仅查看仓库根目录，不自动下载大量代码：

- 常规 Android Gradle 工程且有 `gradlew` 和 `settings.gradle(.kts)` → 建议 **Debug APK** 起步构建。
- 具有 `package.json` + Vite 配置的 Web 工程 → 建议 **Web 静态构建 Artifact**。创建前还会检查 `scripts.build` 是否存在。
- 其他工程、复杂单体仓库或不能可靠识别的情况 → 不胡乱生成工作流；可以复制仓库与现有配置摘要给原来的 **AI 助手** 分析。

生成的工作流写在 `.github/workflows/githubm-android-build.yml` 或 `githubm-web-build.yml`。用户能在最终确认前阅读与修改 YAML；只有点击确认才会提交。Contents API **没有 SHA 不能覆盖现有文件**，所以同名文件会被拒绝，不会悄悄覆盖项目里的工作流。创建后需要再手动触发；不会自动改项目源代码、版本号、签名、Gradle 配置或锁文件。

起步模板 **不等于适用于任何项目**：Web 构建假设输出 `dist/`，Android 构建假设 `assembleDebug` 有效，复杂项目可能使用不同的版本、子模块、构建命令、签名或第三方依赖。请根据项目 README 和实际日志检查与调整。GitHub Actions 不保证存在安装包；只有工作流产生的 Artifacts / Releases 才能下载。

## 失败、版本错误与 AI 助手

构建失败不自动更改文件。打开失败运行、检查 Job 的详细日志，点击“复制诊断提示并前往 AI 助手”，再粘贴提示词以及你选择公开的失败日志。AI 可以提出版本、路径、依赖调整建议；写入文件仍需你确认。注意日志可能包含敏感信息，发送前应先检查、删除令牌、密码与私密路径。

## 未完成验证

代码已通过静态转译检查和工作流解析逻辑测试。由于当前本地缺少 `node_modules`，未成功完成整套 Web/Android 构建；只有将源码合并到仓库、完成依赖安装并在 GitHub Actions 上运行后才能确认工作流与实际产物。
