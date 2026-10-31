# GitHubM 构建工作流匹配与兼容性检查

本功能基于现有快捷构建页增强，**不替换原 Actions 工作流列表、运行历史、日志、产物下载**。源码基础：`GitHubM-在线包名反查与来源匹配优化-完整源码.zip` → 安全上传与上游审核同步版。在线包名反查模块 `src/lib/onlinePackageSearch.ts` 保持原样。

## 核心规则

1. **先读已有 Actions**：最多每页 100 个，保留所有工作流，按名称初筛构建类型；“核对前 5 个工作流配置”才逐个读取 `.github/workflows/*.yml` 并检查 `workflow_dispatch`、输入参数及部分过时 Action。GitHub Pages 自动生成的虚拟工作流不冒充可编辑 YAML。
2. **没有合适工作流，再扫描项目**：请求一次根目录列表；若有 `package.json`，额外读取一次并检查依赖、脚本、锁文件。没有通过额外的无限制全仓库搜索。
3. **展示诊断依据与缺口**：Android Gradle、Flutter、React Native、Tauri、Electron、Web、Node、Rust、Go、Python、Maven、CMake、.NET、iOS、Unity、Docker 等。标记“有起步模板、待审核”“尚缺条件”“已识别、需定制”。
4. **模板不是万能**：目前可预览 Android Gradle / Flutter Android / Tauri Windows / Rust Linux / Web npm / Web pnpm 六种保守模板。其他生态系统只展示检测与缺失项，不把源码强行转换成 APK 或安装包。Node.js 项目不能只凭 package.json 推断为 Web。
5. **安全上传**：用户预览编辑 YAML 并确认后，程序从默认分支 SHA 新建 `githubm/workflow-review-...` 审核分支，将文件提交到该分支，不直接覆盖 `main`，不自动启动任何工作流。用户自行审查 PR 并合并，工作流加入默认分支后才能手动运行。
6. **性能与权限**：代码扫描不会自动遍历所有子目录，也不会默认逐个请求全部工作流。每次手动审核最多查看五个 YAML；支持翻页读取剩余工作流。读取私有仓库 Actions 至少需要 Actions read 权限；创建分支和工作流涉及 Contents/Workflows 写权限，具体取决于所用令牌类型。

## 常见“匹配得差不多但还缺条件”的例子

| 工程 | 识别凭据 | 可能的缺口 | 可选方案 |
|---|---|---|---|
| Flutter | `pubspec.yaml` | 无 `android/`、SDK/插件不兼容、签名缺失 | 只有 Android 目录存在时提供调试 APK 草案；否则先补平台工程 |
| Android | `gradlew`、`settings.gradle` | 无 Wrapper、模块名不同、JDK/AGP 不兼容 | 先核实 Gradle 任务，必要时修改生成的草案 |
| Tauri | `src-tauri/`、`package.json` | 包管理器、Rust/Node、Windows 依赖和签名 | Windows Tauri 草案或独立 Web 静态构建（若确实有 build 脚本） |
| React Native | `react-native`、`metro.config` | 无 `android/`、版本不兼容 | 展示缺失项，不错误套用普通 Vite 模板 |
| Web / Next | Vite、Next 等依赖与配置 | 无 build 脚本，Next 可能是服务端渲染 | 只有可靠静态构建方案时显示模板；否则需定制 |
| Rust | `Cargo.toml` | 可能是库或 workspace，并无可执行目标 | Linux 二进制起步模板需审核目标 |
| iOS | Xcode 工程 | 需要 macOS Runner、签名和配置文件 | 显示必须准备的条件，不生成假 IPA |

## 还未实现的深度检测

- 全仓库多层 Monorepo 工程递归构建图分析；现阶段只读根目录与可用的 `package.json`。
- 自动读取仓库所有 Secrets 的内容（GitHub 不允许读出密钥值，也不应尝试）；只能提醒确认是否已配置。
- 自动修复 Gradle、Flutter、Electron、Xcode 的所有版本问题，或保证发布签名与 APK 安装成功。
- 完整安装依赖后的 Android/网页端构建联调：本交付包进行了纯工具模块的 TypeScript 编译和运行时模拟测试，未把源码语法检查当作最终构建成功。

## 官方参考

- [手动运行工作流](https://docs.github.com/actions/managing-workflow-runs/manually-running-a-workflow)
- [Actions 工作流语法](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
- [获取仓库工作流 API](https://docs.github.com/en/rest/actions/workflows)
- [Actions 密钥与权限](https://docs.github.com/en/code-security/reference/secret-security/secret-types)
