# GitHubM 后续增强：文件导入、分支安全、工作流与 MD3

基础来源：用户最新确认的 `GitHubM-完整源码-工作流与外链问题修正版.zip`（793 文件）。
本次为源码变更，不表示 APK 已成功编译或在所有 ROM 真机运行。

## 一、真实实现

- **非默认分支管理**：分支管理页新增“独立空白分支”，通过 Git Data API 创建空树、无父提交的根提交及新分支引用，不继承任何既有分支文件或提交。GitHub 权限/仓库规则仍可能拒绝。
- **按分支多选清理**：读取完整未截断 Git 树，展示路径筛选、全选筛选项、清空当前分支；输入完整分支名确认。Git Data API 一次创建删除树、提交、非强制更新分支引用；重新确认 HEAD，拒绝默认分支和受保护分支。上限 3,000 个文件，历史提交不被删除。
- **修正上传树对象**：上传预览先从 Git 提交查询真实树 SHA，而非把提交 SHA 直接当作树 SHA。
- **ZIP**：多文件选择及拖拽混合 ZIP 会自动识别并解析，不会原样上传 ZIP；新增 ZIP 内容 CRC32 核对和大文件解析时让出浏览器事件循环。保留 96MB 压缩、160MB 解压、32MB 单文件限额。多文件按路径检查冲突，拒绝静默覆盖。
- **Android SAF 文件夹选择**：Android 原生 `ACTION_OPEN_DOCUMENT_TREE` 按用户授权读取所选目录（含支持 DocumentsProvider 的第三方文件管理器）。在私有缓存生成临时 ZIP，经过受限大小的桥接数据分块传回前端，复用 ZIP 分析、差异预览和审核分支提交。关闭/完成后清理临时文件。Android 11+ 限制选择内置存储根目录、Download 根目录及 Android/data、Android/obb 等特殊目录。
- **上传工程类型提示**：本地扫描已选择的源码路径，显示项目根位置和常见构建配置，并提示目录错位风险；不会主动删除仓库里未上传的文件。
- **外链选择器**：使用原生 `Intent.createChooser(ACTION_VIEW)`，不再用可能为空的手动组件列表；尽可能从选择器排除 GitHubM 自己以避免循环。外部链接唤起 GitHubM 仍按系统“默认打开”设置决定。
- **构建工作流**：增加可选深度扫描最多 20 个常见子目录（默认为 8），追加 Expo、Capacitor、Cordova、Godot、Deno、Bun、Make/Meson 等标识与缺失条件；不冒充能自动构建任意 APK。
- **依赖稳定性**：将 `vite` 所使用的 `rolldown-vite@latest` 固定到当前 lockfile 已锁定的 `7.3.1`，减小未来安装时的构建漂移。
- **渐进 MD3**：在主框架及仓库详情、Actions、代码、上传、Fork、分支管理、设置页扩展样式令牌、圆角和键盘焦点；保留 React/Tailwind/原生导航，不是完全原生 Material Design 3 迁移。

## 二、验证

- 先前完整源码 ZIP：793 个文件全部被新解析器正常读出；以本轮源文件对比，**原有文件均保留**。
- ZIP CRC：正常样本通过、篡改数据样本被拒绝。
- TypeScript/TSX：涉及文件语法转译检查通过。项目加入 Vitest 回归用例但由于本机没有 node_modules/React/Vite 未能运行全部 Vitest 和 Vite build。
- 增强工作流建议：Expo/Capacitor/Godot/Deno/Bun/Flutter 6 个本地用例结果符合预期。
- AndroidManifest XML 检查通过；Kotlin 缺少 Android SDK/Gradle 依赖，不能声明完整原生编译通过。
- GitHub 真实空白分支创建、批量删除、PR 和 Actions 构建未在私人仓库联调，**使用前请在测试仓库验证**。

## 三、特别注意

1. 清空分支会改变那个非默认分支的当前文件树；虽然提交历史可恢复，仍是破坏性操作，必须认真确认。
2. 原生 SAF 只支持实现 Android DocumentsProvider 的文件管理器，无法遍历其他应用的私有目录或绕过 Android 系统权限。
3. 96MB ZIP + 160MB 解压总量仍可能使内存较小的手机吃紧；建议一次分批导入。若系统 WebView 缺乏 `DecompressionStream('deflate-raw')` 支持，会明确报错，而不是上传不完整文件。
4. GitHub Actions `workflow_dispatch` 仍要求工作流在默认分支中已登记。可在运行面板指定其他分支构建，但其工作流文件和输入仍需校验。
5. MD3 已覆盖整体框架和关键页面，但未完成全页面视觉比对及 Android 动态壁纸取色验收。
