# GitHubM 最终整合源码清单（2026-10-31）

这是由用户上传的 GitHubM 源码快照逐步合并的工程，不保证与网络上 `main` 后续提交相同。

## 登录

- PAT 令牌登录是**默认选项**，保留原来的多账号与额度功能。
- Android 原生 OAuth 浏览器授权是用户主动选择的可选项：用户自行注册 GitHub OAuth App 并在本机输入 Client ID / Client Secret，GitHubM 不附带任何第三方 OAuth 身份；Client Secret 在 Android Keystore 下加密保存，仅由 Android 原生代码交换令牌。
- 网页版可以继续使用 PAT；网页版 OAuth 必须额外配置维护者可信的令牌交换服务，不能在浏览器里明文存储 OAuth Client Secret。
- 未在实际 GitHub OAuth App 与手机上完成登录端到端测试；自配登录仍需验证。

## 与上版保持不变的模块

- 独立应用库及系统/普通应用扫描，用户可关闭“显示系统应用”；保存不同的匹配来源状态和独立筛选。
- 包名、版本、签名、README/F-Droid/上游 Fork、安装包等多线索匹配的**现有实现**。对于无法确认的来源仍应显示“待确认”，不作保证。
- Fork 列表搜索和安装包发现；创建 Fork 的显式确认与状态复查。
- 统一仓库详情、Fork 来源应用内跳转、Github 网页链接接收、双代理公开文件下载。
- 快捷构建工作流、运行日志与产物入口、上传 ZIP 差异预览、贡献者参考项目名单。
- API 额度与错误提示机制、现有 AI 助手代码。

## 验证边界

- 已检查 ZIP 文件列表和变更文件，已进行相关 TSX 转译及代码结构检查。
- **未完成**带完整前端依赖的 `vite build`、Android Gradle 打包、真实 OAuth 回调、真机应用扫描、实际 API 限流/Fork 创建联调。
- 不包含 `.env` 凭据、签名 Keystore 和生成的 APK。使用本源码部署前先比较在线仓库的较新提交，不建议整个 ZIP 无差异覆盖当前 `main`。

## 保密提醒

无论仓库是否公开，都不得在源码中提交用户的 OAuth Client Secret、PAT、Android 签名密钥或其他令牌。Android Keystore 只能保护应用私有设备存储，并非对已被攻破的设备提供绝对保障。
