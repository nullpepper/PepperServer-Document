# Doublecheck spec

## Goal
将 PepperServer-Document 从 Docusaurus 2.4.1 迁移到 VitePress：18 个文档页面内容保持不变，重建站点配置（导航/侧边栏/搜索/深色模式/首页品牌），更新 CI 部署，验收测试保留并通过，提交推送。

## Scope
package.json 重写（VitePress 依赖与 scripts）；.vitepress/ 配置与主题；docs/ 下 18 个 md 的 frontmatter 适配；static/img 资源迁移至 public/；.github/workflows/build.yml 重写；scripts/check-docs.sh 适配；删除 Docusaurus 专属文件（docusaurus.config.js、sidebars.js、babel.config.js、src/、i18n/、docs/towns/_category_.json）。

## Acceptance criteria
1) VitePress 站点构建成功（npm run docs:build 无报错）；2) 18 个文档页面全部迁移且相对链接完整（check-docs.sh 通过）；3) 导航栏、侧边栏、内置搜索、深色模式可用；4) 首页保留原品牌（PepperCraft 标题 + 三个特性卡片）；5) GitHub Actions 部署流程正常工作（构建产物推送 gh-pages 分支）；6) 提交并成功推送 origin/main。

## Failure modes
frontmatter 不兼容导致页面丢失：逐个校验生成页面数与源文件数一致；链接断裂：check-docs.sh 与构建双重验证；搜索不可用：VitePress 内置 minisearch 配置校验；CI 部署失败：本地复现 build 命令后再推送。

## Priorities
内容保真优先（18 页文字与结构不变，仅 frontmatter/链接适配）；部署方式最小改动（沿用 gh-pages 分支 + JamesIves action 升级 v4）；框架现代化收益次之。

## Non-goals
不重写文档内容（仅迁移格式）；不引入多语言（保持中文单语）；不改站点品牌（标题/logo/首页特性）；不迁移 en locale（原站无英文文档）；不改 GitHub Pages 部署目标（docs.pcraft.eu.org）。
