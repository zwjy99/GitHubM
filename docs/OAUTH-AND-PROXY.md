# GitHubM · Github King 授权与代理功能参考实现

本版本以用户上传的 **GitHubM 完整源码增强版** 为基础，参考 Github King 1.0.1.apk 的*产品交互方式*，没有复制第三方 OAuth 身份、私有 API 密钥或代理服务。

## 本次最终登录方式（优先阅读）

- **默认：PAT 令牌登录**。不需要注册 OAuth App，也不依赖额外服务。
- **可选：Android 自配 OAuth 浏览器登录**。没有内置其他应用作者的 Client ID 或 Secret，第一次使用时由用户自己注册 OAuth App，在 GitHubM 原生弹窗填写 **Client ID 和 Client Secret**，回调 URL 设为 `githubm://oauth`。后续可以直接从登录页使用系统浏览器授权，并在“更换 / 清除我自己的 OAuth 配置”中修改配置。
- 自配 Client Secret 经 Android Keystore AES-GCM 加密后保存在当前设备的应用私有数据中；**不会放在 WebView 的 localStorage、APK、GitHub 仓库或第三方下载代理中**。但本机应用被攻破或设备被 root 时不存在绝对保密保证。只有 Android 原生层直接向 `https://github.com/login/oauth/access_token` 发送交换请求。为降低授权码劫持风险保留 PKCE S256 与 state 校验。
- **网页版**仍以 PAT 登录为默认方案。如果网站维护者选择开启 OAuth，则需要配置可信服务端换取 Token；**不能把 Client Secret 放到网页源代码或 Vite 变量中**。
- 原有的预置 Client ID + 自有 Worker 配置是给站点维护者的**另一可选方式**，并非普通 Android 用户必须部署。
- 未配置或授权失败时不会自动循环重试，PAT 仍能使用。当前项目未做完整在线登录联调，发布前应使用测试 OAuth App 验证真机回调、token 生命周期和用户切换。

使用者的注册步骤：
1. 在 `https://github.com/settings/developers` 选择 OAuth Apps → New OAuth App。
2. Application name 可用 `GitHubM Personal`，Homepage URL 可填自己控制的网址；**Authorization callback URL 必须是 `githubm://oauth`**（准确匹配）。
3. 回到 GitHubM → 登录 → GitHub 官方授权（可选）→ 输入 GitHub 提供的 Client ID 和 Client Secret → 保存并授权。
4. Android 将打开系统浏览器，授权后通过 `githubm://oauth` 返回 GitHubM；如果不想使用浏览器授权，直接切换“令牌登录”。

> 以上是 Android 自配置模式，不是把别人 OAuth App 的配置搬过来；GitHub 官方链接中的 GitHubM 身份取决于你自己注册的 OAuth App 名称。若出现回调无法打开，请确认已安装**本次构建的新 APK**，并且回调地址与注册值一致。

## 已接入

- **登录页**：保留 PAT 登录，新增 GitHub OAuth App 的授权入口。采用 `state` 防 CSRF 和 PKCE S256，单次回调有效期不超过 10 分钟。使用 GitHubM 本身的 `AuthContext.login()`，兼容原有多账号、额度计数和 AI 助手。
- **可选的网页服务端交换**：`workers/githubm-oauth/worker.js` 提供 Cloudflare Worker 示例。若采用此网页方案，`GITHUB_OAUTH_CLIENT_SECRET` 必须仅保存在 Worker 的 Secret 中。纯 GitHub Pages 无法安全地在客户端存储 Secret 或完成令牌交换。Android 的用户自配 OAuth 通过原生 HTTPS 直接交换，不依赖 Worker。
- **下载代理**：设置页的「公开文件访问与代理下载」，分别启用下载代理和 Raw 访问代理，均默认关闭。模板形式 `https://YOUR-PROXY.example/?url={url}`，`{url}` 会被 URL 编码。
- **调用位置**：公开仓库的 Release 文件「代理下载」按钮、应用库/ Fork 文件代理下载链接；公开源代码文件可通过右键菜单复制 Raw 代理地址。
- **安全边界**：公开状态验证失败时拒绝代理；私有仓库、API 请求、登录令牌、Actions 私有产物绝不走第三方下载代理。普通「下载」按钮仍按旧有方式工作。

## 可选的网页站点 OAuth 配置（由网站维护者完成一次）

1. 在 [GitHub Developer Settings](https://github.com/settings/developers) 新建 **OAuth App**，确保应用属于你自己的 GitHub 账户或组织。选择「Web application flow」。在「Authorization callback URL」填写实际部署 URL，例如 `https://YOURNAME.github.io/GitHubM/`。GitHubM 使用 HashRouter，GitHub 把 `code` 放在 `?code=` 查询参数，不能把 `#/login` 填作回调地址。
   如需同时使用 Android，OAuth App 还需登记第二个回调地址 `githubm://oauth`；请为 GitHubM 原生 APK 使用自己的应用标识（如正式发布后，使用验证过的 HTTPS App Links 比自定义 scheme 更安全）。
2. 在可信 Cloudflare 账户中部署 `workers/githubm-oauth/worker.js`，将 `wrangler.toml.example` 复制成 `wrangler.toml` 并填写 `GITHUB_OAUTH_CLIENT_ID`、`GITHUB_OAUTH_REDIRECT_URI`、`GITHUB_OAUTH_NATIVE_REDIRECT_URI=githubm://oauth`、`ALLOWED_ORIGIN`（例如 `https://YOURNAME.github.io`）。通过 `wrangler secret put GITHUB_OAUTH_CLIENT_SECRET` 设置服务端密钥。切勿把私钥、客户端密钥放在 `.env`、GitHub Actions 输出、静态网站或 APK 内。
3. GitHub 仓库 `Settings → Secrets and variables → Actions → Variables` 添加：
   - `GITHUBM_OAUTH_CLIENT_ID`：你的 OAuth App Client ID（公开值）；
   - `GITHUBM_OAUTH_EXCHANGE_URL`：你部署的 Worker URL，例如 `https://your-worker.workers.dev/oauth/exchange`。
4. Web 自动部署工作流会把这两个**公开**配置变量注入 Vite 打包。也可以将它们放进本地 `.env.local`，变量名使用 `.env.oauth.example`。**绝不要把 OAuth client secret 填到 VITE_ 开头的变量**。
5. 未完成以上**网页**配置时，Web 端的授权按钮会禁用；Android 端仍可使用用户自行输入的 OAuth App 凭据，PAT 在两个平台均可正常使用。部署后请检查 OAuth 回调 URL、CSRF State、GitHub 权限提示和登录后的 `/user` 数据。

### 平台限制

除了 HTTPS 网页，本版本还增加了 GitHubM **自己**的 Android PKCE 深链接 (`githubm://oauth`)、系统浏览器授权、原生令牌交换和 JS Bridge 回调实现，未复用 Github King 的 Client ID、密钥或 `gk://login`。这是**源码实现**，尚未通过 Android 真机、签名 APK 或 OAuth App 实际授权验证。原生端可以（1）使用用户自行在 Android 弹窗输入的 OAuth App Client ID / Client Secret，直接与 GitHub 官方令牌接口交换，或（2）由站点维护者预先配置自有 Worker 的方式；两种方式都必须正确注册 Android 回调。无需 OAuth 的用户直接使用默认 PAT。

注意：自定义 Scheme 理论上可能被其他安装应用声明抢占，虽然 PKCE 阻止无 verifier 的令牌兑换，高安全正式发布建议使用 HTTPS App Links（需要你掌控的域名和 assetlinks.json），并在 Android KeyStore 中安全存储 token。现有 GitHubM JS AuthContext 仍在 localStorage 中保存 Token，不能将它称为已实现系统 KeyStore 安全存储。

采用 OAuth App 会请求 `repo read:user notifications` 范围。由于 GitHubM 存在私有仓库管理能力，`repo` 权限覆盖较广；授权前应仔细阅读 GitHub 权限页。后续可以改为 GitHub App 最小权限模型与短期用户令牌。

**重要：** 当前 `AuthContext` 原本就将 Token 保存在 `localStorage` 用于多账号，这是现有项目的存储方式，不等于实现了硬件级安全存储；高安全部署建议使用后端 Session/BFF 与服务端加密 Token。Worker 仍应通过 Cloudflare 的 WAF/Rate Limiting 配置限制 `/oauth/exchange` 的滥用，不应记录访问令牌。

## 下载代理设置

默认关闭，也**没有内置 Github King 的代理节点**。用户自行选择可信 HTTPS 代理。比如模板：

`https://trusted.example/proxy?url={url}`

如果代理使用路径前缀模式，请自行根据代理说明调整模板。代理可能有网络失效、缓存过期、内容篡改和访问记录风险；建议校验 GitHub Release 的 SHA-256 校验值或作者签名。`代理下载` 只适用于公开资源，不会用于私有 API 或自动后台请求。

## 验证

- 本地 TypeScript 的单文件转译检查、代理 URL 白名单/权限逻辑测试及 OAuth Worker 请求过滤测试。
- 未对真实 GitHub 账号执行授权、没有真实 OAuth Client Secret、没有连接/部署任何第三方代理。Android PKCE 深链接及 Kotlin 网络代码尚未编译、真机测试。
- 当前环境未安装完整前端依赖，不能保证生产 `vite build` 与 Android 构建通过。上线前必须运行 `pnpm install && pnpm exec vite build`、项目单元测试及 Android 构建，并在测试账号验证登录/下载。

## 相关官方资料

- OAuth App：[Authorizing OAuth Apps](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps)
- OAuth 安全：[Best practices for creating OAuth apps](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/best-practices-for-creating-an-oauth-app)
- Releases 下载：[Release assets REST API](https://docs.github.com/en/rest/releases/assets)
