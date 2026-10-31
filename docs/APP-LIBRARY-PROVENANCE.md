# GitHubM 应用库：多线索溯源与匹配增强（2026-10-31）

本版基于用户上传的 GitHubM 源码快照及后续“仓库链接唤起与双代理修正版”编写，不保证与远端未来提交一致。

## 使用入口

进入 **应用库** → 选择已导入应用 → **搜索候选开源仓库** → 对候选或已保存仓库选择 **调查上游/源码**。页面下方还可以展开 **应用来源深度调查**（F-Droid、APK、本地文件比对）。点击仓库名称进入**原有统一仓库详情页**，而不是复制一个新的仓库信息页面。

## 调查能力和证据等级

| 方式 | 本版功能 | 限制及可信度 |
|---|---|---|
| 本机已安装应用 | Android 扫描包名、版本名、versionCode、当前签名与可用签名历史、安装来源；系统应用可选显示 | 包名、签名只是身份线索；仅当前 Android 用户可查询的包 |
| 仓库名称、简介、README | 原有 4~5 次分阶段 GitHub 仓库搜索，包含 Fork | 仅候选，不自动关联 |
| GitHub parent / source | 详情 API 获取直接上游和根上游，展示并可单独保存关联 | 证明 GitHub Fork 关系，不证明 APK 出自某 Fork |
| 更新网址、README 引用 | 读取用户保存的更新 URL、仓库主页、简介、README 里引用的 GitHub 地址 | 老版本 APK 经常仍指向原版，属于弱线索 |
| 源码配置 | 按需读取选定仓库的 README、Gradle、Flutter 配置等少量文件，比对包名、版本 | 代码路径是辅助线索；无 CI/构建哈希证明 |
| F-Droid | 按包名查官方 F-Droid JSON 版本元数据，并尝试读取 fdroiddata 的 SourceCode、WebSite、AuthorName | F-Droid 显示接口不是 APK 身份验证 API；部分网页端可能因 CORS 读不到 GitLab 元数据 |
| 开发者其他项目 | 按需读取同一作者最多 50 个最近更新的公开仓库供核对 | 同作者也可能有多个同名/衍生项目，非自动认定 |
| Fork 家族 | 若有根仓库，可单次检查最多 50 个 Fork，继续加载；0 Star 也包括 | 默认不遍历全部 Fork，更不会替用户擅自下载大量 APK |
| Release 与安装包 | 复用原有 Release/安装包检查，显示扩展名与版本 | 不保证安装包能覆盖自行签名的应用 |
| 版本后缀与修改版 | 显示“完全相同 / 同基础版本 / 不同 / 未知” | `1.2.3-mod` 与 `1.2.3` 只是基础版本有关联 |
| 已安装 APK 内部链接 | Android 原生桥接按需扫描部分 DEX/资源文件，提取地址、计算单文件 SHA-256 | 不运行 APK、不上传安装包；大文件与二进制编码可能漏检 |
| 外部 APK 本地分析 | 网页及 Android WebView 可手选 APK，浏览器按 ZIP 目录扫描有限条目，得到 URL 与指纹 | 部分 WebView 不支持 `deflate-raw`；不解析二进制 AndroidManifest；不核验 V2/V3 签名 |
| 修改 APK 内容对比 | 手动选第二个 APK，对同名内部文件做 SHA-256 精确比较 | 内容一致不等于真实发布来源一致；自行重签名和重新构建会影响不同文件 |
| 迁移/历史 | GitHub API 可能自动随重定向解析已迁移仓库；README 和旧 URL 可作为人工可保存的线索 | **没有**全网历史归档数据库，无法保证查到删除、私有或未公开的项目 |

## 调查执行限制

- 所有 GitHub REST 请求使用项目既有 `runBudgetedBackground`，与额度显示及限流保护共用状态；无后台批量自动执行。
- F-Droid 和 GitLab 请求由用户主动触发，不含 GitHub Token；APK 本地解析不上传文件，也不转交第三方代理。
- 单个仓库只尝试少数典型源码文件，遇到 GitHub 限流立即停止，不继续高频重试。
- Fork 每次请求至多 50 个；作者仓库按需至多查看 50 个；APK 单文件解压上限 8 MiB、累计 20 MiB，APK 文件大小上限 130 MiB。
- 本机读取已安装 APK 要求 Android 侧 `inspectInstalledApp` 原生桥接；网页版只能手动选择 APK。
- 自动扫描到的来源、名称相近、版本号近似、更新网址或签名均**不自动标记为确认来源**。
- 研究结果默认只是暂时显示；经用户点击“保存关联”才进入本地应用库备份。用户已确认的仓库关系不被扫描覆盖。

## 官方参考

- GitHub Repository API parent/source: https://docs.github.com/en/rest/repos/repos#get-a-repository
- GitHub Rate Limit: https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api
- F-Droid API: https://f-droid.org/en/docs/All_our_APIs/
- F-Droid 元数据字段: https://f-droid.org/docs/Build_Metadata_Reference/

## 尚不能声称已完成的能力

这不是应用二进制的**自动可信同一性认证系统**：暂不能自动在数千个 Fork 中精确确定已安装 APK 的生成提交；没有 APK v2/v3 签名块和完整 Manifest 解析，没有外部软件商店全量索引、历史签名数据库、跨设备同步、持续后台监控和自动修复源码。无公网交叉验证服务时，不可能保证找到未公开发布的修改版。

## 构建验证范围

已检查 TypeScript 文件可转译，进行了基于隔离模拟的包名、版本、F-Droid、GitHub 上游、Fork、开发者和 APK ZIP/DEX 提取及指纹断言。未进行完整 `npm run build`、Android Gradle 编译、真机安装和线上 CORS 实测。上线前必须在你的环境进行完整构建与回归测试。
