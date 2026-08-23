# Doublecheck spec

## Goal
在 PepperServer-Document wiki 中完成三项修改并推送：删除管理命令页面 admin.md（管理命令内容全部随之移除）；移除未开启的领地转让（/pclaim home transfer）相关内容，保留联盟盟主转让（/union transfer）；新增 PepperMinecart 矿车玩法页面（形式自定）。

## Scope
docs/admin.md（删除）、docs/towns/Claims.md 与 Commands.md（领地转让小节）、docs/intro.md（导航）、docs/special.md（sidebar 顺位）、新增 docs/minecart.md；资料依据：PepperMinecart 源码 README 与服务器实际 config.yml。

## Acceptance criteria
1) docs/admin.md 已从仓库删除，且无任何页面链接指向它；2) Claims.md 与 Commands.md 中 /pclaim home transfer 相关内容已移除，联盟盟主转让（/union transfer）内容保留；3) 新增 docs/minecart.md 页面（PepperMinecart 玩法），已接入导航；4) scripts/check-docs.sh 全部通过；5) npm run build 成功；6) 提交并成功推送到 origin/main（CI 自动部署）。

## Failure modes
构建失败/链接断裂：按报错修复直至通过；推送遇 GnuTLS 代理握手失败：用 git -c http.proxy= 直连重试；minecart 功能描述与服务器配置不符：以服务器实际配置（config.yml）为准，本服关闭的 vanilla-carts 取下等细节明确标注。

## Priorities
按用户指令忠实执行；minecart 页面形式自定但须与服务器实际配置一致（本服策略优先于 README 默认值）；修改范围最小化。

## Non-goals
不修改联盟系统文档中的盟主转让功能；不把管理命令迁移到其他页面；不改动矿车插件的服务器配置；不动其他无关页面内容。
