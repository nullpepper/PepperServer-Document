# 个人领地

个人领地（家园领地）是你**唯一的居所**：每人 1 块，个人专属，保护你的建筑与资源——别人无法破坏方块、打开箱子、偷走物品。永不因联盟变动受影响。

> 由服务器自研插件 **PepperClaim** 提供。想和伙伴一起建公共设施？见[联盟领地](GuildClaims.md)。

## 圈地规则

- 领地**不能重叠**。
- 出生点 **100 格**范围内无法圈地。
- **主城区域**内无法圈地。
- 未启用圈地的世界无法圈地。
- 圈地操作有**限流**（创建冷却 30 秒），请勿连续快速操作。

## 一键安家（新手推荐）

```
/pclaim auto
```

以你为中心自动圈一块 **16×16** 的家园领地（60 秒冷却），确认后立刻生效，方块、箱子、动物全部受保护。

## 手动圈地

1. 拿一把**金锄**（如果背包里没有，可以找管理员领取）。
2. **左键**点一个角（pos1），走到对角**右键**点另一个角（pos2）。
3. 看提示：绿色粒子轮廓 = 位置合法；红色 = 冲突（重叠、离出生点太近等），ActionBar 会告诉你原因。
4. 输入创建：

```
/pclaim create 我的家
```

> **引号规则**：大部分命令直接输中文领地名即可（`tp` / `select` / `create` / `resize` / `delete` / `info` 等）；但 `rename`、`set preset`、`flag`、`group` 这几个命令的领地名参数**需要加引号**：
> ```
> /pclaim rename "我的家" 新名字
> /pclaim flag "我的家" build DENY
> ```

## 常用命令

| 指令 | 作用 |
|---|---|
| `/pclaim home` | 传送回家园领地 |
| `/pclaim list [页数]` | 我的领地列表 |
| `/pclaim info [领地]` | 查看领地详情（面积、额度、描述、进出消息） |
| `/pclaim map` | 查看周边领地的聊天色块地图 |
| `/pclaim gui` | 打开图形界面（推荐！） |
| `/pclaim guide` | 领取游戏内《领地指南》成书 |
| `/pclaim help` | 显示全部可用命令 |
| `/pclaim tp [领地]` | 传送到领地（不填默认自家） |
| `/pclaim settp` | 把当前脚下设为领地传送点 |
| `/pclaim rename <领地> <新名>` | 重命名 |
| `/pclaim delete <领地>` | 删除领地（确认后额度退还） |
| `/pclaim trapped` | 被困自救（回到安全点） |
| `/pclaim confirm` | 确认待执行操作（10 秒窗口） |

> **目标记忆**：命令不带领地名时，按「脚下领地 → 最近访问 → 自家」的顺序自动选择目标。

## 进出消息

| 指令 | 作用 |
|---|---|
| `/pclaim set welcome <领地> <消息>` | 设置进入播报（≤64 字） |
| `/pclaim set farewell <领地> <消息>` | 设置离开播报（≤64 字） |
| `/pclaim set describe <领地> <文本>` | 设置描述（≤128 字） |

占位符：`%name%` = 领地名字、`%player%` = 玩家名。新领地默认自带「欢迎来到 %name% 领地！」。

## 一键权限档位（四档预设）

```
/pclaim set preset <领地> 半开放
```

| 档位 | 陌生人能做什么 |
|---|---|
| 封闭 | 什么都不能（进都进不来） |
| 半开放 | 能走进领地，但不能用东西、不能破坏 |
| 开放 | 能进出、能用公共设施（门/工作台）、能传送到领地，但不能破坏、不能开箱 |
| 亲密 | 同封闭，只给最信任的人（组）留门 |

> 档位是"一键模板"，切换后仍可用 `/pclaim flag` 微调单个旗标，互不影响。

## 旗标（细粒度保护）

```
/pclaim flag <领地> pvp DENY           # 领地内禁止玩家互殴
/pclaim flag <领地> animalkilling DENY # 禁止杀动物
/pclaim flag <领地> build ALLOW        # 允许陌生人建造（慎用）
```

**常用旗标：**

| 旗标 | 作用 |
|---|---|
| `move` | 进入领地 |
| `use` | 使用物品/开关（门、按钮、工作台等） |
| `build` | 建造/破坏方块 |
| `container` | 打开箱子等容器 |
| `tp` | 被传送到领地 |
| `riding` | 骑乘（马、船等） |
| `leash` | 拴绳 |
| `shear` | 剪羊毛 |
| `harvest` | 收获作物 |
| `animalkilling` | 击杀动物 |
| `mobkilling` | 击杀怪物 |
| `pvp` | 玩家互殴 |
| `vehicledestroy` | 破坏载具 |
| `firespread` | 火焰蔓延 |
| `nomobs` | 禁止怪物生成 |

取值：`ALLOW`（允许）/ `DENY`（禁止）/ `unset`（恢复默认）。

> ⚠️ **极性注意**：`nomobs`、`pistonprotection`、`flowinprotection` 三个旗标在本服是 **`DENY` = 生效**。例如"禁怪"写 `nomobs DENY`。

## 角色组（精细授权）

给特定玩家开小灶——建组、把人放进组、给组设旗标：

```
/pclaim group <领地> create 室友        # 建组（每块领地最多 4 组）
/pclaim group <领地> assign 张三 室友    # 把玩家放进组
/pclaim group <领地> setflag 室友 container ALLOW  # 组旗标
/pclaim group <领地> list               # 查看
/pclaim group <领地> unassign 张三 室友  # 移出组
/pclaim group <领地> delete 室友         # 删除组
```

## 黑名单

```
/pclaim ban 捣蛋鬼       # 拉黑后无法进入你的领地
/pclaim unban 捣蛋鬼
```

## 保护机制

- **死亡掉落锁定**：在领地内死亡，你的掉落物有 **60 秒**锁定时间，只有你能拾取。
- **安全传送**：传送到领地会自动计算安全落点（避开实心方块、岩浆、虚空），计算失败会拒绝传送。
- **传送冷却**：领地传送有 30 秒冷却。

## 额度与经济

领地大小受**额度**限制（方块数）：

| 指令 | 作用 |
|---|---|
| `/pclaim quota` | 查看剩余额度 |
| `/pclaim buy <块数>` | 用游戏币购买个人额度 |

额度不够时 `resize` / `create` 会被拒绝并提示缺口。删除领地后额度**全额退还**。

## 扩展与调整选区

```
/pclaim select 我的家                 # 把已有领地的范围加载为当前选区
/pclaim expand 5 north                # 朝北扩 5 格（north/south/east/west）
/pclaim expand 10 out                 # 四周各扩 10 格
/pclaim expand 10                     # 朝你面向的方向扩 10 格
/pclaim expand -3 east                # 负数是收缩（最少 1 格宽）
/pclaim resize 我的家                 # 用当前选区扩大/缩小领地
```

> ⚠️ `resize` 后 **72 小时冷却**（扩和缩都算）；扩边只扣**增量**额度，缩地退换额度。

## 常见问题

**Q：领地被人破坏了？**
先检查 `move` / `build` / `container` 旗标是否被误设为 `ALLOW`：`/pclaim flag <领地> build DENY` 恢复。

**Q：陌生人能传送到我的领地？**
检查 `tp` 旗标（半开放档默认 `DENY`）：`/pclaim flag <领地> tp DENY`。

**Q：为什么杀不了领地里的动物？**
`animalkilling` 半开放档默认 `DENY`——这是保护。要开放：`/pclaim flag <领地> animalkilling ALLOW`。

**Q：resize 提示冷却？**
72 小时内只能改一次，等冷却结束或接受现状。

**Q：中文领地名报错？**
`rename`、`set preset`、`flag`、`group` 命令的领地名参数需要加引号：`/pclaim rename "我的家" 新名字`。

**Q：想换个档位但不想动旗标？**
档位是一键模板，切换后可用 `/pclaim flag` 微调。

**Q：伙伴进不来基地？**
把他加进角色组并给组开 `move` 旗标，或使用「半开放/开放」档位。

## 市场

> 当前服务器的租赁（`rent`）与拍卖（`auction`）功能**未开放**，如提示"该功能已在服务器配置中禁用"即属正常。
