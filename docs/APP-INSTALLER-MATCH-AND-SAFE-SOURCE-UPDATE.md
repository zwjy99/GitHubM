# GitHubM：应用库 × Fork 安装包匹配、源码 ZIP 安全融合（2026-10-31）

## 本次修改的基础

基于对话中用户上传的 GitHubM 源码及后续“仓库链接唤起与双代理修正版”，叠加了独立应用库多线索调查工作树。**不是从网络拉取的最新远端主分支**；上传时应先与仓库在线 HEAD 比较。

## 应用库安装包匹配

- 应用库 → 已导入应用 → 搜索候选仓库，可对单个仓库点击“检查发布包”。
- 先点“调查上游/源码”，如果 GitHub 显示根仓库（source）或直接上游（parent），可选择“查根仓库 Fork（每页50个）”；列表内增加“批量检查3个 Fork 的安装包”，也可逐项检查。
- 分类规则与原 Fork 列表共用 `collectForkInstallers()`。检查真实 Release 附件，不把 GitHub 自动生成的源码 ZIP 或 AAB 当作可直接安装的 APK。
- 匹配证据依次包含：本地手动导入 APK 的 SHA-256 与 Release 附件 `digest=sha256:...` 一致；发布标签和应用版本一致；同基础版本（修改后缀）；应用名称与文件名匹配。只有**文件级哈希相同**才显示强文件证据；依然不能证明项目源码和当前安装环境绝对可信。
- 自行签名或重新构建会改变整个 APK 的哈希。可以在“APK 线索”里手动选择第二个 APK 比较少量 DEX 和资源哈希；没有自动下载上千个 Fork 的 APK。
- APK 全文件 SHA-256 仅对手选的 64 MB 内 APK 在本地计算，不上传 APK。已安装应用 APK 原生分析仍只是内部内容指纹/网址，不能伪装成已获得其安装包整体哈希。
- 每次检查 3 个 Fork，顺序调用 GitHub REST API，遇到错误/限流停止；默认检查各 Fork 最近30条可见 Release，并保留“未检查/当前范围没发现”的区分。结果是临时调查证据，只有点击“记录此安装包关联”才写入独立应用库的本地关联记录。

## 安全上传与源码更新

仓库详情 → 上传文件，保留原有逐文件上传功能，并增加：

1. 如果当前 GitHub 仓库是真实 Fork 且可获得上游信息，提供“同步上游”按钮。使用 GitHub 官方 `POST /repos/{owner}/{repo}/merge-upstream`，仅在用户确认时运行。冲突返回失败，不会强制推送覆盖。
2. 上传本地源代码 ZIP，在浏览器本地限量解压。仅导入文件，自动剥离单层仓库根目录，忽略 `.env`、签名密钥、`node_modules` 和构建产物等。ZIP 最多 32 MB、单个文件最多 1 MB、最多 750 个文件，总解压数据最多 24 MB（其余限制会拒绝导入或明确跳过）。Android WebView 需支持 `DecompressionStream('deflate-raw')` 才能处理 DEFLATE ZIP。
3. 点击“读取当前分支并预览差异”：用 GitHub Git tree 获取该分支完整文件列表。若树截断则中止。以 Git blob SHA 对比本地文件，分成新增、完全相同、冲突。不会仅按文件名判断内容相同。
4. 默认只创建新增文件、跳过完全相同内容；**同路径不同内容不做伪三方合并**。只有用户勾选“允许替换冲突文件”才能使用 SHA 乐观并发锁覆盖原文件；其它人修改过的 SHA 将阻止覆盖。
5. 最多每次提交20个文件，通过 GitHub Contents API 串行逐文件提交。发生中断可能已经提交一部分，需要查看远端仓库、重新预览。**这不是单次原子提交**，也不负责删除远端源文件。对于需要处理两边改动的同一文件，应通过真实 Git 三方合并/Pull Request 解决。

## 权限、额度、错误

- GitHub API 请求继续经过原有额度与限流守卫。上传和 Sync fork 需对目标仓库有 Contents: write；修改 `.github/workflows` 需要额外的 Workflows 权限。受保护分支可能禁止直接写入。
- 代理不参与令牌交换和这些 API；Release 公共下载按现有公开下载代理设置处理，私有内容不通过第三方代理。
- 不会因为版本号相同、同包名、Fork 或 README 提及上游就自动将其绑定为确认来源。

## 尚未实现/未验证

- GitHub 全网 APK 内容爬取与自动比对、自动扫描数千 Fork 的所有历史 Release、APK 签名 v2/v3 的完整验证；**这些会产生大量请求和下载，不能伪称完成**。
- 无 Git 共同祖先时对修改文件自动做正确三方合并，或自动修复编译冲突；仍需人工确认/PR。
- 本环境未完成 React/Vite 正式构建、Android APK 编译与在线登录的端到端测试。已完成关键模块的 TS 语法转译、ZIP 解析与安全过滤、哈希、安装包匹配的本地测试。

## GitHub 官方文档

- [Release API](https://docs.github.com/en/rest/releases/releases)
- [GitHub 分支 Sync fork](https://docs.github.com/en/rest/branches/branches#sync-a-fork-branch-with-the-upstream-repository)
- [Git tree API](https://docs.github.com/en/rest/git/trees)
- [Contents API 和写入权限](https://docs.github.com/en/rest/repos/contents#create-or-update-file-contents)
