
# 命令速查

联盟与领地的完整命令速查。`<>` 表示必填参数，`[ ]` 表示可选参数。

## 联盟（/union）

| 指令 | 作用 |
|---|---|
| `/union help` | 查看帮助 |
| `/union create <名字>` | 创建联盟（64 钻石，需 `/union confirm` 确认） |
| `/union join <联盟编号>` | 加入开放联盟 |
| `/union apply <联盟编号> <留言>` | 申请加入联盟 |
| `/union audit <list\|accept\|reject>` | 审核入盟申请 |
| `/union accept <联盟编号>` | 接受邀请 |
| `/union invite <玩家>` | 邀请成员 |
| `/union kick <玩家>` | 踢出成员 |
| `/union ban <玩家> [原因]` | 拉黑玩家 |
| `/union unban <玩家>` | 解除拉黑 |
| `/union leave` | 退出联盟 |
| `/union list` | 联盟列表 |
| `/union info [联盟编号]` | 联盟详情 |
| `/union gui` | 联盟图形界面 |
| `/union chat <消息>` | 联盟聊天 |
| `/union rename <新名字>` | 改名（盟主） |
| `/union transfer <玩家>` | 转让盟主（盟主） |
| `/union disband` | 解散联盟（盟主，需确认） |
| `/union rank <create\|delete\|list\|setperm\|set>` | 职位管理（盟主） |
| `/union config <list\|get\|set>` | 联盟配置（盟主） |
| `/union account <balance\|deposit\|withdraw\|give\|log>` | 联盟账户 |
| `/union logs [条数]` | 联盟操作日志 |
| `/union setitem` | 设置联盟头像（盟主） |
| `/union confirm` | 确认待执行操作（5 秒窗口） |
| `/union reload` | 重载配置（管理员） |

## 个人领地（/pclaim）

| 指令 | 作用 |
|---|---|
| `/pclaim auto` | 一键自动安家（16×16，60 秒冷却） |
| `/pclaim create <名字>` | 按金锄选区创建领地 |
| `/pclaim home` | 传送回家园领地 |
| `/pclaim tp [领地]` | 传送到领地 |
| `/pclaim settp` | 设置脚下为领地传送点 |
| `/pclaim list [页数]` | 领地列表 |
| `/pclaim info [领地]` | 领地详情 |
| `/pclaim map` | 领地地图 |
| `/pclaim gui` | 图形界面 |
| `/pclaim select [领地]` | 加载领地范围为选区 |
| `/pclaim expand <格数> [方向]` | 扩展选区（负数收缩） |
| `/pclaim resize <领地>` | 按选区扩边/缩地（72 小时冷却） |
| `/pclaim rename <领地> <新名>` | 重命名 |
| `/pclaim delete <领地>` | 删除领地 |
| `/pclaim set preset <领地> <封闭\|半开放\|开放\|亲密>` | 一键权限档位 |
| `/pclaim set describe <领地> <文本>` | 设置描述 |
| `/pclaim set welcome <领地> <消息>` | 设置进入语 |
| `/pclaim set farewell <领地> <消息>` | 设置离开语 |
| `/pclaim flag <领地> <旗标> <ALLOW\|DENY\|unset>` | 设置旗标 |
| `/pclaim ban <玩家> [领地]` | 拉黑玩家 |
| `/pclaim unban <玩家> [领地]` | 解除拉黑 |
| `/pclaim group <领地> <create\|assign\|setflag\|list\|...>` | 角色组管理 |
| `/pclaim quota` | 额度概览 |
| `/pclaim buy <块数>` | 购买额度 |
| `/pclaim trapped` | 被困自救 |
| `/pclaim confirm` | 确认待执行操作（10 秒窗口） |
| `/pclaim guide` | 游戏内指南书 |

## 联盟领地（/gclaim）

| 指令 | 作用 |
|---|---|
| `/gclaim create <名字>` | 按选区创建联盟领地 |
| `/gclaim list [页数]` | 本盟领地列表 |
| `/gclaim tp [领地]` | 传送到联盟领地 |
| `/gclaim map` | 领地地图 |
| `/gclaim info [领地]` | 领地详情 |
| `/gclaim resize <领地>` | 扩边/缩地 |
| `/gclaim set preset <领地> <档位>` | 一键权限档位 |
| `/gclaim set welcome <领地> <消息>` | 进入语 |
| `/gclaim perms <领地>` | 查看职位权限 |
| `/gclaim perm <领地> <职位> <旗标> <值>` | 设置职位旗标 |
| `/gclaim sub create <名字>` | 创建子领地 |
| `/gclaim quota` | 额度与槽位概览 |
| `/gclaim buy <块数>` | 用金库买额度 |
| `/gclaim worldslot <世界>` | 购买世界扩展槽 |
| `/gclaim prosperity` | 繁荣度详情 |
| `/gclaim top` | 活跃榜 |
| `/gclaim heart <register\|unregister\|effect\|info\|public>` | 领地之心 |
| `/gclaim gui` | 图形界面 |
