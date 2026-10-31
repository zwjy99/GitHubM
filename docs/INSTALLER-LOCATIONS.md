# 安装包固定发布位置索引（应用库与 Fork 列表共用）

此模块是 `src/services/installerLocations.ts`。根据固定的 GitHub 元数据列表进行按需查询，而不是自动下载每个仓库的 APK：

1. **Releases / Assets**：GET `/repos/{owner}/{repo}/releases?per_page=100&page=1`。GitHub 每条 Release 返回 Assets 名称和下载网址，无需再逐条 GET `/releases/{id}/assets`，也不能把 GitHub 自动生成的源码 ZIP 视作安装包。最多 100 条/页，只有点击“继续检查更早发布”才翻页，最多五页，不代表所有历史。
2. **Actions Artifacts**：按需调用 GET `/repos/{owner}/{repo}/actions/artifacts?per_page=100&page=1`，跳过已过期的构建产物。只有名称暗示 APK/安装器才列出；Actions ZIP 中实际内容未经核实，标记为“可能包含安装包”。支持点击进入运行页面查证，不尝试未经授权直接下载 API ZIP。只看最新一页。
3. **常见源码内安装文件位置**：必须由用户单独点击“深度检查”，先读取仓库默认分支，再查询 Git Tree 的文件路径，筛选 `dist/`、`release/`、`build/outputs/`、`app/release/` 等常见输出目录里已经提交的安装文件。源代码存在构建脚本但没有已提交的安装包，不等于有可下载的安装包；树结果截断时显示不完整警告。
4. **GitHub 以外的 F-Droid / IzzyOnDroid / 官网**：不属于 GitHub 的统一 Release 接口，仍由现有应用来源目录和单独的软件分发查询使用；不会凭猜测把外部链接当成 GitHub Release。

## 速度与额度

- 同一账号、同一个 GitHub 仓库的各页面共用一个**内存缓存**（10 分钟），同一阶段的并发请求合并；不缓存令牌与下载二进制文件，私有数据不会跨账号复用。
- 默认只请求 Releases 一页；需要时追加 Actions 的一个列表请求；完整 Git Tree 扫描必须再次点击。不会对上千个 Fork 自动查询全部发布源。
- 后台请求继续使用 `runBudgetedBackground`，尊重 core 额度预留、速率限制、超限冷却及账号切换，不额外高并发。
- 检查结果严格区分：**已列出安装文件 / 可能含有安装文件的构建产物 / 已检查本范围但没有找到 / 尚未检查 / 查询失败**。不能把一个仓库的近期 Releases 没有附件当成全网没有 APK。
- 可以用 GitHub 真实返回的 SHA-256 与本地 APK 文件计算结果比较；重新签名的 APK 文件整体 SHA-256 会改变。文件名和版本号匹配仅供辅助判断，不自动确认来源。

## 仍需验证

静态源码和模拟 API 已检查。正式 Web/Android 构建、真机操作和真实 GitHub 账号限流联调尚未完成。历史 Actions 产物可能因保留期限而过期；`Packages` 主要属于注册表/包分发，不能统一映射为所有 Android/Windows 安装包，不应宣称固定列表穷尽了全部发布位置。

GitHub 官方接口：
- https://docs.github.com/en/rest/releases/releases#list-releases
- https://docs.github.com/en/rest/actions/artifacts#list-artifacts-for-a-repository
- https://docs.github.com/en/rest/git/trees#get-a-tree
