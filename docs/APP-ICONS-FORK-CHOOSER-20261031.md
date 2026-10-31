# GitHubM 应用图标 / Fork 安装包查询 / 链接打开方式更新

- 应用库已安装应用的图标由 Android PackageManager 提供；仅当滚动到可见项时，前端请求最多 24 个缩略图，图片只保存在当前页面的内存缓存，不写入同步记录、GitHub 或 JSON 备份。网页版显示占位图标。
- Fork 安装包检测增加可选每批 3/8/15 个，仍只在用户点击后串行调用 GitHub Releases API，遵守既有共享 API 额度与冷却机制。检查结果在当前运行期间暂存 8 分钟；并非扫描 GitHub Actions 产物、旧发布历史或外部网站。批量 15 次请求仍会占用 API 配额，不应误称实时全量检索。
- Android 收到 `https://github.com/owner/repo` 链接时提供“在 GitHubM 中打开 / 选择其他应用 / 取消”；使用系统 chooser，不强制用户改默认客户端。Android 系统若已经将 GithubM 设为默认接管链接，仍需用户自行在系统“默认打开”关闭；软件不能强行清除系统默认项。
- Android 公开链接外部打开的 chooser 避免递归包含 GitHubM 自己。`githubm://` 专用路由仍直接进入应用，PAT/OAuth 回调保持原流程。
- `withPackageSource` 对本地已匹配到同版本索引的应用避免再次计算。若离线索引有新版本会继续重算。
- 本次包含静态源码检查和数据一致性检查；未完成 Android Gradle、Web Vite 全量编译或真机安装联调。
