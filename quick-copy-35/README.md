# 5美元/35元小服务执行包

目标：用微信收款码卖一个低门槛小服务，先拿到第一笔 35 元左右的订单。

已生成文件：
- `index.html`：可直接打开的销售页。
- `styles.css`：销售页样式。
- `assets/wechat-pay.png`：微信收款码。
- `poster.html`：适合截图转发的朋友圈海报页。
- `.github/ISSUE_TEMPLATE/order.md`：公开页面的需求提交模板。

已发布入口：
- GitHub 公开接单 Issue：https://github.com/Cappuccinor/fabric-calculator/issues/1
- GitHub 仓库文件：https://github.com/Cappuccinor/fabric-calculator/tree/main/quick-copy-35
- GitHub Pages 页面：https://cappuccinor.github.io/fabric-calculator/quick-copy-35/
- 公开海报图片：https://raw.githubusercontent.com/Cappuccinor/fabric-calculator/main/quick-copy-35/poster-share.png
- `发布文案.md`：朋友圈、微信群、小红书、闲鱼可用文案。
- `接单话术.md`：私信成交话术。
- `交付模板.md`：拿到需求后的交付格式。

最快执行方式：
1. 把 `发布文案.md` 的短版发到朋友圈/微信群。
2. 有人回复后，让对方先发任务，不要先收钱。
3. 能做的小任务再收 35 元。
4. 把对方需求发给 Codex，用 `交付模板.md` 输出成品。

如果要公开链接：
- 当前优先发布到 `Cappuccinor/fabric-calculator` 的 `quick-copy-35/` 子目录。
- 若 GitHub token 没有写入权限，先运行 `gh auth refresh -h github.com -s repo` 并在浏览器确认授权，再运行 `powershell -ExecutionPolicy Bypass -File .\publish-to-github.ps1`。
- 也可以把 `index.html` 发给熟人，或者截图后配合收款码发朋友圈。

边界：
- 不接违法、刷单、虚假评价、考试代写、伪造材料。
- 太大的任务不要按 35 元硬接，拆成一个小交付。
