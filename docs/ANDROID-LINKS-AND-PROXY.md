# Android 默认打开 GitHub 仓库链接 / 双通道公开代理

## 操作路径

1. 使用本源码重新编译并安装 **Android APK**（修改源代码不会改变已安装的旧 APK）。
2. GitHubM → 设置 → 默认打开 GitHub 链接 → 打开系统设置。
3. 系统可能显示 `github.com` 和 `www.github.com`，若允许则开启“在此应用中打开支持的链接”。
4. 用其他应用打开 `https://github.com/TrianguloY/URLCheck`，目标是复用 GitHubM 的 `/repos/TrianguloY/URLCheck` 页面。也可从分享面板发送 GitHub 链接，或用 `githubm://repo/TrianguloY/URLCheck`。
5. Android 12+ 不会把未通过网站所有权验证的 github.com 视为 GitHubM 的已验证域名，可能依然由浏览器打开。无法通过修改 APK 强制改变这点。

直接打开仓库首页时，在应用内复用仓库详情；Issues、Releases、文件等子页面保留各自的浏览器访问含义。未登录时保存待打开仓库，登录成功后继续跳转。

## 代理与安全

GitHubM → 设置 → 访问与下载代理（双通道）：**访问代理**（公开 Raw 文件）和 **下载代理**（公开 Release/APK/ZIP）可以各选节点和总开关，默认关闭。仅对公开仓库使用；登录、OAuth、私有仓库和 GitHub API 一律不经过第三方代理。第三方服务可记录或篡改文件，安装 APK 前请核实来源。

## 不要上传私钥或服务密钥

源码包特意不包含上传原档中的 `.env`（含服务端密钥）和 `android/release.keystore`（APK 私钥）。这些都不是 GitHub 代码的一部分；上传公共仓库前应重新检查历史提交有没有泄露。`.env.example` 只提供可公开的配置项。

原有 `scripts/setup-keystore-secrets.sh` 不再包含硬编码签名密码。要保持旧版 APK 可覆盖安装，请单独备份你自己的原证书，在本地将 `KEYSTORE_PATH`、`RELEASE_KEY_ALIAS`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_PASSWORD` 设为环境变量后执行脚本。没有稳定证书时，CI 会临时生成证书，**不同构建之间不能直接覆盖安装**。

## Android 官方资料

- https://developer.android.com/training/app-links/add-applinks
- https://developer.android.com/training/app-links/verify-applinks
- https://developer.android.com/reference/android/provider/Settings#ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
