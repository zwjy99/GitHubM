# 工作流与外部链接修复说明

基于“代码页分支源码下载修正版”的完整源码检查，非重新拼接旧版本。

## Android 外部 URL（仓库右上角箭头、README 浏览器查看）

- `MainActivity.chooseExternalApp` 过去先创建隐式 VIEW Chooser、再用 `EXTRA_EXCLUDE_COMPONENTS` 排除本应用。在部分 Android 设备上其余浏览器的可见性/默认处理状态不满足条件时，Chooser 会显示空白。
- 新版 Manifest 为 Android 11+ 声明 `http` 与 `https` VIEW 处理程序查询；代码实际枚举候选组件，移除本应用，然后以显式 Intent 构建一次性选择列表。
- 无其他处理器时明确弹出提示，不显示一个空列表；不强制更改 Android 的默认打开设置。
- 仍只接受 HTTP(S) 公开链接，不将 PAT 或 OAuth 令牌传递给外部处理程序。
- 如果系统隐藏/禁用了全部其他浏览器，应用无法自行制造一个浏览器。须在真机验证常用浏览器和 URLCheck 的处理行为。

## GitHub Actions 工作流与构建建议

- 展示 GitHub Actions API 返回的工作流，不因名字不是 `build` 就直接判定不可构建。
- 手动分批核对最多五份 YAML：核实 `workflow_dispatch`、参数、过时 Actions；再利用 YAML 构建/产物命令识别名字很普通的 CI 工作流。
- 已有工作流允许选择目标分支，运行前额外核对目标分支是否包含对应 YAML，是否支持手动触发，以及 Inputs 是否一致。若不满足则阻止提交，避免“已经构建”假提示。
- 获取默认分支失败时显示明确错误，不再因为 `loading = loadingWf || !defaultBranch` 永久转圈。
- 可单独设置工程分析分支；根目录之外，还会在限额内查看 `apps/`、`packages/`、`android/`、`frontend/` 等常见子目录（最多八个目录）。
- 子目录工程不直接套用根目录工作流模板：标出缺少 `working-directory`、构建命令或产物路径的风险；默认分支才允许创建工作流审核分支。
- 限制额外 API 请求，避免为了构建分析扫描整个巨大 monorepo。

## 还需实际验证

- Android APK Gradle/Kotlin 编译及各品牌真机链接选择器（当前交付环境无法完成）。
- GitHub 账号验证不同分支的 `workflow_dispatch` 与实际运行权限。
- JSX 与 Kotlin 已进行静态检查；该检查不是完整编译或运行验收。

## 常用官方资料

- https://developer.android.com/training/package-visibility/declaring
- https://developer.android.com/training/sharing/send
- https://docs.github.com/actions/managing-workflow-runs/manually-running-a-workflow
