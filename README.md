# PepperCraft 服务器文档

[点击查看](https://docs.pepper.ltd/)

## 关于本站内容

站点面向玩家，只发布玩法、命令与规则说明，不包含服务端实现细节。

- `docs/` 下的页面会被构建并公开发布。
- `docs/placeholder-engine-design.md` 与 `docs/placeholder-engine-phase2-proposal.md` 是内部设计稿（含服务端实现、数据库与插件内部细节），已在 `docs/.vitepress/config.mts` 中通过 `srcExclude` 排除，不参与构建、不在站点上公开；文件仍保留在仓库内备查。
- 新增内部文档时，请一并加入 `srcExclude`，或放在 `docs/` 之外的目录。
