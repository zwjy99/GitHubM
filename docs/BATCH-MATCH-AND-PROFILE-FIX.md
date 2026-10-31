# 批量匹配与作者主页修复

版本 1.0.20261031.4。

## 应用库

- 扫描结果直接对整份列表按完整 Android 包名匹配，无须逐项打开应用。
- 内置 F-Droid / IzzyOnDroid 来源元数据快照，共 5818 个包名；无需 GitHub API 请求。索引文件保留了来源 URL、抓取时间和原始数据 SHA-256。
- 显示匹配到的源码链接，包括 GitHub、GitLab、Codeberg 等；可筛选匹配到来源、索引未收录和尚未检查。系统应用开关仍生效。
- 旧应用库、扫描导入、JSON 导入和编辑包名都会更新匹配信息，保留用户原来的备注及手动关联。
- 匹配的是目录记载的项目来源，并非本机 APK 的签名或文件同源性认证。索引未收录不意味着应用一定闭源；原有仓库候选搜索仍可使用。
- 更新索引：先从 src/data/app-source-index.json 的来源 URL 下载对应索引，再执行 scripts/build-app-source-index.py，重新构建 APK。

本地打包可通过 Gradle 属性 `-PwebAssetsDir=/绝对路径/dist` 直接使用本次 Web 构建目录，避免旧资源混入。默认仍使用 `android/app/src/main/assets`，兼容原有 CI。

## 贡献者

- 主链接指向作者 GitHub 主页，不指向头像或参考项目仓库。
- Android 新窗口处理从实际导航请求取得目标 URL，移除了把头像 hit-test 地址当成链接的逻辑。
- 未提供公开主页的作者不创建猜测链接。

## 验证

- 完整前端测试 136 项通过，含批量扫描 → 自动匹配 → 筛选 → 批量导入的组件回归测试，以及头像外层链接的目标测试。
- 生产 Web 构建成功；Android Release 编译、签名和资源校验见构建交付。
- 继承上一个 v1.0.20261031.3 的签名，versionCode 递增，支持其覆盖更新。
- TypeScript 全量检查仍有原包已有的 4 项错误（apkEvidence BufferSource 类型、publicProxy 的 3 处 replaceAll），与原包复查结果一致，本次未增加类型错误。
- 未完成 Android 真机点击验证；未使用真实账号执行网络搜索或修改仓库。
