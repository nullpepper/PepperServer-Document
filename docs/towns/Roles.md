
# 职位与权限

联盟的权限体系分为两层：**联盟职位权限**（成员在联盟内能做什么）与**领地旗标权限**（在联盟领地里能做什么）。

## 联盟职位

每个联盟拥有若干职位（权限组），新联盟默认有两个：

- **盟主**：拥有全部权限，可管理联盟的一切。
- **成员**：基础权限。

### 职位管理（盟主专属）

| 指令 | 作用 |
|---|---|
| `/union rank list` | 查看所有职位 |
| `/union rank create <职位ID> <等级> [名称]` | 创建职位（等级决定权限高低） |
| `/union rank delete <职位ID>` | 删除职位 |
| `/union rank setperm <职位ID> <权限> <on\|off>` | 开关职位权限 |
| `/union rank set <玩家> <职位ID>` | 调整成员职位 |

> 等级越高的职位权限越大；只能把成员分配到**等级低于自己职位**的组。

### 联盟权限一览

| 权限 | 说明 |
|---|---|
| `union.rank.assign.N` | 分配等级 N 以下的职位（N 不高于自身级别） |
| `union.member.invite` | 邀请成员 |
| `union.member.kick` | 踢出成员 |
| `union.member.audit` | 审核入盟申请 |
| `union.account.deposit` | 向联盟账户存款 |
| `union.account.withdraw` | 联盟账户取款/拨款 |
| `union.account.give` | 把联盟资金分配给成员 |
| `union.account.log` | 查看联盟账户流水 |
| `union.config-edit` | 修改联盟配置 |
| `union.pvp.attackAlly` | 允许攻击盟友 |
| `union.pvp.attackedByAlly` | 允许被盟友攻击 |

**盟主专属权限**（不可分配给其他职位）：解散联盟（`union.leader.disband`）、转让盟主（`union.leader.transfer`）、编辑职位定义（`union.leader.rank-edit`）。

## 领地联动权限

联盟职位同时决定成员在**联盟领地**中的行为（由 PepperClaim 联动）：

| 职位 | 默认拥有的领地权限 |
|---|---|
| **成员** | `union.claim.build`（建造）、`union.claim.use`（使用物品）、`union.claim.container`（开箱）、`union.claim.move`（进入）、`union.claim.riding`（骑乘）、`union.claim.tp`（传送） |
| **盟主/管理职位** | 在成员权限基础上增加：`union.claim.create`（创建领地）、`union.claim.manage`（管理领地）、`union.claim.flag`（设置旗标）、`union.claim.sub.create`（创建子领地）、`union.claim.sub.manage`（管理子领地）、`union.claim.buy`（购买额度） |

## 领地旗标权限

联盟领地的精细保护用**旗标**控制，可按职位单独设置：

| 指令 | 作用 |
|---|---|
| `/gclaim perms <领地>` | 查看职位权限 |
| `/gclaim perm <领地> <职位> <旗标> <值>` | 给某职位设置旗标（如 `build ALLOW`） |

常用旗标与取值见[领地页面](Claims.md#旗标-细粒度保护)。
