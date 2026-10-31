# GitHubM v1.0.20261031.6 — 修复与验证记录

本包**直接来自用户上传的 `GitHubM-v1.0.20261031.6-source(1).zip`**，并非以此前更旧的修改版为基础。

## 已修改的代码

1. 仓库右上角「在 GitHub 中查看」、README 的「在浏览器查看」、Homepage：Android 版通过原生桥接调用 ACTION_VIEW 打开系统的浏览器选择器；Web 版打开新标签，不会覆盖 GitHubM 当前页面。不能保证每个第三方浏览器都能打开任意网页。
2. Actions：保留原有工作流列表与手动运行；工作流为空时增加解释和引导；起步构建模板增加 Flutter、Rust、Tauri，原有 Gradle / npm / pnpm 仍保留。仅能根据明确构建配置识别，不保证全部项目可构建，需审核 YAML。
3. 应用库来源：离线索引已经包括 URLCheck、Komi Store 和 LibChecker 的包名数据。补充用户提供 Github King APK 中核对出的包名 `com.peonyking.gitapp` 与 `PeonyJun/GithubKing` **候选项目关联**，未验证其 APK 就是该仓库的发行件。离线索引未命中时不再直接暗示项目不开源。扫描不会自动访问上千个 Fork。
4. APK 广告 SDK 线索：有限扫描 `classes.dex` 等 APK 内部文件里的典型 SDK 字符串。标注 Google Mobile Ads、Meta Audience Network、Unity Ads、AppLovin、ironSource、Pangle、腾讯广告等**静态库特征**；不能证明广告展示、网络流量、权限滥用或追踪行为。无命中不证明「无广告」，混淆与分包可能漏检。参考 LibChecker/LibChecker 与 LibChecker-Rules 的规则化标记思路，并未照搬其规则数据库。
5. 源码 ZIP：修复单文件超过 Contents API 1 MB 无法成功融合、导致重要来源索引丢失的问题；改用 Git Data API 创建 Blob/Tree/Commit 与非强制 ref 更新。每批最多 20 个文件，共同作为一个 Git 提交，变更被别人抢先推送时停止。未尝试自动三方合并有冲突的文件；默认仍不覆盖冲突。
6. 仍会忽略 `.env` 真密钥、签名 keystore 等敏感文件；绝不为「让源码完整」而上传签名私钥。新增 ZIP 测试确实解析了上传的 694 个可导入文件，包括 7,065,546 字节的来源索引；其中 1 个项目依规则排除。

## 仍待正式验证与局限

- `npm run build`、Android Gradle 完整编译及实际 GitHub 上传均**未在当前无联网依赖环境完成**。不同 Android WebView、GitHub 私有仓库授权与 API 二级限流会影响结果。
- 没有自动检查全部广告网络请求或将 APK 动态反编译；不会依据字符串强行标记应用「有广告」。
- 复杂工程 Tauri / Flutter 构建取决于项目自身版本、依赖、工作流权限及 Runner；生成的是审核用的工作流草案。
- 大 ZIP 的单文件仍有 12 MB 安全上限、总解压 64 MB 上限；超过限制将提示，不会悄悄丢掉文件再提交。
- 上传源码前建议先创建新分支，审查每批修改及提交；对源仓库的同步推荐使用 Git 官方分支同步与 PR。
