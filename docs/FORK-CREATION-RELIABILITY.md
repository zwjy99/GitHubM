# 创建 Fork：按钮、可靠性与错误处理

这项改动复用现有 GitHubM 仓库详情页及 AI 风格 API 安全提示，未新建仓库详情页面。

- 他人仓库详情：明显的「复刻仓库」按钮（创建 Fork）与「查看 Forks」（浏览 Fork 列表）分别显示；自己的仓库没有复刻到自身的操作。
- 搜索和仓库列表里的「Fork 仓库」统一打开详情页的创建确认弹窗，不直接 POST。
- 创建前查询用户账号下的同名仓库；已存在时核对 `parent` / `source` 关系，避免仅凭同名声称成功。
- 默认复刻所有分支；可选只复制默认分支。
- 创建 API `POST /repos/{owner}/{repo}/forks` 返回 `202 Accepted`，仅表示已受理；之后由用户主动点击「检查创建状态」确认目标仓库已可读及上游关系，才显示「打开我的 Fork」。不自动轮询 / 重试 POST。
- POST 超过 30 秒或 GET 超过 15 秒会中止等待。特别是 POST 超时意味着结果未知：不可据此立刻重新提交，以免后台实际上已接受请求。
- 创建前后使用已有共享 API 额度与 secondary rate limit 保护，不绕过 `Retry-After`；单个会话内相同 Fork 请求合并以避免双击重复。
- 401、403、422、429、网络异常分情况提示，并提供 GitHub 官方链接。403 本身不代表账号被标记，422 也不必然代表已存在 Fork。
- sessionStorage 只保存短期 Fork 已受理时间和仓库标识，不保存 token；浏览器禁用会话存储时仍可使用。

## 已知限制

- GitHub 创建 Fork 是异步的，官方说明超过 5 分钟仍不能访问 git 对象应联系 GitHub Support。
- 用户已有不同名称的某个 Fork，GitHub 可能不接受再次 Fork；界面不能据此自动推断正确仓库。
- 对于组织 Fork、细粒度 PAT、仓库禁用 Fork 的情形，权限和 GitHub 策略仍由服务端决定；本 UI 不绕过限制。
- 修改是源代码级测试，必须在实际 APK / Web 环境验证真实令牌、网路代理和 GitHub 账号情况。

官方资料：
- https://docs.github.com/en/rest/repos/forks#create-a-fork
- https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api
- https://docs.github.com/en/rest/using-the-rest-api/troubleshooting-the-rest-api
- https://support.github.com/contact
