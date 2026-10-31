# GitHubM：Fork 身份、署名证据和安装包扫描复用（2026-10-31）

## 目标及边界

本轮以《固定发布位置安装包索引》完整源码为基础，完善来源调查与查询复用，**不删除原有应用库数据与绑定**。

- GitHub 官方 `fork: true`、`parent`、`source` 是仓库关系证据。通过主仓库的官方 Fork 列表获取的结果也会记录“在哪个上游网络中被发现”；注意这未必是直接 parent。
- 提交的 GitHub 作者账号、原始 Git author 署名、`Co-authored-by` 协作署名是不同信息。最近 20 条提交未出现仓库所有者，不等于“它一定是 Fork”，也不代表该软件原作者是谁。
- Android 的 `applicationId` 或 Manifest `package` 与已装 APK 包名一致，可以增加该仓库作为候选的可信程度；但无法单独证明安装文件由该仓库构建。Gradle `namespace` 常用于代码命名空间，不等同 APK 身份。
- Releases 明确列出的 APK/EXE/MSI 等附件是安装文件存在的证据；Actions 产物名称和压缩包名称只能视为候选，未下载和检查压缩包内容就不能断言其中有 APK。`signed` 等文件名也不能作为加密签名已验证的证据。

## 本轮调整

1. **Fork 列表缓存复用**：应用库和 Fork 页面统一为每页 100 条，按账号、上游、排序、页码隔离缓存 10 分钟，重叠请求合并；保存临时的 GitHub 官方 Fork 观察关系，避免两处再次请求同页。仍可自主查看更多页面，不在启动时枚举所有 Fork。
2. **提交作者调查**：按用户点击、每批至多 5 个仓库，抽样每仓库最近 20 条提交。结果显示 GitHub 账号、无法链接到 GitHub 账号的 Git author 和协作署名；所有结论均明确是抽样弱线索。
3. **源码固定位置**：按需读取 README、Gradle、Android Manifest、pubspec，并提取 `applicationId`、`namespace`、`versionName`、Flutter `name/version` 等特征；对照当前应用的包名与版本，识别 `-mod` 等基础版本关联。
4. **安装文件名特征**：从已有 Release 文件名识别 ARM ABI、x86、debug/release、signed/unsigned 和版本提示。只处理文件名，不新增下载请求，不将文件名当成身份验证。
5. **候选优先级**：应用库检查 Fork Release 时优先版本线索，再参考最后代码推送时间；不因为 Star 少、Fork 没有个人提交而隐藏它。
6. **已有发布位置共用**：复用 `installerLocations.ts` 中按账号缓存的 Releases / Actions / 已提交构建目录结果，不从头重新抓取，更不为了搜索 APK 下载 APK 文件本体。

## 隐含限制

- GitHub 官方不存在一个可以一次查询“某仓库全部 Fork 的 APK”的完整集合接口；未缓存的候选仍需请求各自的 Release 列表。
- 未提交到仓库的本地修改、仅在外部网站提供的安装包无法保证发现。
- 如果当前仓库是镜像、模板或从 ZIP 复制的代码，GitHub 可能不存在官方 Fork 关系，需要其他证据辅助调查。
- 移动端和网页版的网络、跨域和账号权限不同，F-Droid/API 请求可能失败，需要保留“未检查/无法读取”状态。
- 账号权限不足或 GitHub 返回二级限流时停止批量请求，继续遵循项目的 API 安全保护；没有为了提速增加无界并发。

## 验证说明

新源码经过涉及文件的 TypeScript 转译检查、共享 Fork 缓存并发/账号隔离模拟、作者归因及 Android/Flutter 标识解析模拟、文件保留和 ZIP 完整性检查。完整 Vite 和 Android APK 编译及真实 GitHub API 联调**尚未完成**。

## 官方资料

- [GitHub repository fork parent/source](https://docs.github.com/en/rest/repos/repos#get-a-repository)
- [List forks](https://docs.github.com/en/rest/repos/forks#list-forks)
- [Compare commits across forks](https://docs.github.com/en/rest/commits/commits#compare-two-commits)
- [List releases](https://docs.github.com/en/rest/releases/releases#list-releases)
- [REST rate limits](https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api)
