
# 领地

领地系统让你圈占地图上的土地，保护你的建筑与资源。领地分为两类：

- **个人领地**（`/pclaim`）：每个玩家一块**家园领地**，个人专属，永远归你所有。
- **联盟领地**（`/gclaim`）：属于[联盟](index.md)的公共领地，大家一起建设，权限按职位分享。

> 领地系统由服务器自研插件 **PepperClaim** 提供。圈地规则：领地不能重叠、出生点 **100 格**范围内无法圈地、联盟领地之间需保持间距、主城区域内无法圈地。

## 一键安家（新手推荐）

```
/pclaim auto
```

以你为中心自动圈一块 **16×16** 的家园领地（60 秒冷却），确认后立刻生效，方块、箱子、动物全部受保护。

## 手动圈地

1. 拿一把**金锄**（如果背包里没有，可以找管理员领取）。
2. **左键**点一个角（pos1），走到对角**右键**点另一个角（pos2）。
3. 看提示：绿色粒子轮廓 = 位置合法；红色 = 冲突（重叠、离出生点太近等），ActionBar 会告诉你原因。
4. 输入创建（中文名字直接输入即可，无需引号）：

```
/pclaim create 我的家
```

## 家园领地管理

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

### 进出消息

| 指令 | 作用 |
|---|---|
| `/pclaim set welcome <领地> <消息>` | 设置进入播报（≤64 字） |
| `/pclaim set farewell <领地> <消息>` | 设置离开播报（≤64 字） |
| `/pclaim set describe <领地> <文本>` | 设置描述（≤128 字） |

占位符：`%name%` = 领地名字、`%player%` = 玩家名。新领地默认自带「欢迎来到 %name% 领地！」。

### 一键权限档位（四档预设）

```
/pclaim set preset <领地> 半开放
```

| 档位 | 陌生人能做什么 |
|---|---|
| 封闭 | 什么都不能（进都进不来） |
| 半开放 | 能走进领地，但不能用东西、不能破坏 |
| 开放 | 能进出、能用公共设施（门/工作台）、能传送到领地，但不能破坏、不能开箱 |
| 亲密 | 同封闭，只给最信任的人（组）留门 |

### 旗标（细粒度保护）

```
/pclaim flag <领地> pvp DENY           # 领地内禁止玩家互殴
/pclaim flag <领地> animalkilling DENY # 禁止杀动物
/pclaim flag <领地> build ALLOW        # 允许陌生人建造（慎用）
```

常用旗标：`move`（进入）、`use`（使用物品/开关）、`build`（建造）、`container`（开箱）、`tp`（传送）、`riding`（骑乘）、`pvp`、`animalkilling`、`mobkilling`、`firespread`（火焰蔓延）……

> ⚠️ **极性注意**：`nomobs`、`pistonprotection`、`flowinprotection` 三个旗标在本服是 **`DENY` = 生效**。例如"禁怪"写 `nomobs DENY`。

### 角色组（精细授权）

```
/pclaim group <领地> create 室友       # 建组（最多 4 个）
/pclaim group <领地> assign 张三 室友   # 把玩家放进组
/pclaim group <领地> setflag 室友 container ALLOW  # 组旗标
/pclaim group <领地> list              # 查看
```

### 黑名单

```
/pclaim ban 捣蛋鬼       # 拉黑（联盟领地不能拉黑领地拥有者）
/pclaim unban 捣蛋鬼
```

## 额度与经济

领地大小受**额度**限制（方块数）：

| 指令 | 作用 |
|---|---|
| `/pclaim quota` | 查看剩余额度 |
| `/pclaim buy <块数>` | 用游戏币购买额度 |
| `/gclaim buy <块数>` | 用联盟金库购买联盟额度 |
| `/gclaim worldslot <世界>` | 购买世界专用扩展槽 |

额度不够时 `resize` / `create` 会被拒绝并提示缺口。

## 扩展与调整选区

```
/pclaim select 我的家                 # 把已有领地的范围加载为当前选区
/pclaim expand 5 north                # 朝北扩 5 格（north/south/east/west）
/pclaim expand 10 out                 # 四周各扩 10 格
/pclaim expand 10                     # 朝你面向的方向扩 10 格
/pclaim expand -3 east                # 负数是收缩（最少 1 格宽）
/pclaim resize 我的家                 # 用当前选区扩大/缩小领地
```

> ⚠️ `resize` 后 **72 小时冷却**（扩和缩都算）；扩边只扣**增量**额度。

## 联盟领地（/gclaim）

联盟领地是大家一起建的生产设施（刷怪塔、农场、市场），权限按联盟职位分享。

| 指令 | 作用 |
|---|---|
| `/gclaim create <名字>` | 按当前选区创建联盟领地（先金锄框地） |
| `/gclaim list [页数]` | 本盟领地列表 |
| `/gclaim tp [领地]` | 传送到本盟领地（不填传送到枢纽） |
| `/gclaim map` | 周边领地地图 |
| `/gclaim info [领地]` | 查看联盟领地详情 |
| `/gclaim resize <领地>` | 扩边/缩地（先 expand / 金锄框好） |
| `/gclaim set welcome <领地> <消息>` | 联盟领地进出消息（同家园） |
| `/gclaim set preset <领地> 半开放` | 联盟领地档位 |
| `/gclaim buy <块数>` | 用公会金库买联盟额度 |
| `/gclaim quota` | 联盟额度与槽位概览 |
| `/gclaim gui` | 联盟领地图形界面 |

### 槽位与子领地

- 联盟能开几块领地由**槽位**决定：初始 **3 块**，可通过繁荣度提升/购买扩展槽增加（至多 **6 块**）。
- 每块联盟领地内可划分**子领地**（功能分区），最多 **10 个**：

```
/gclaim sub create 商店区
```

### 职位权限

```
/gclaim perms <领地>                  # 查看职位权限
/gclaim perm <领地> <职位> <旗标> <值> # 给某职位开某旗标（如 build ALLOW）
```

### 繁荣度与领地之心

- 成员在联盟领地内活动（生产、狩猎、驻留）会涨**联盟活跃度**（不能买），详见[繁荣度与活跃度](Advancements.md)。
- `/gclaim prosperity` 查看繁荣度详情；`/gclaim top` 查看活跃榜。
- **领地之心**（`/gclaim heart`）：把满级信标注册为联盟灯塔，解锁传送/特效。

## 市场

> 当前服务器的租赁（`rent`）与拍卖（`auction`）功能**未开放**，如提示"该功能已在服务器配置中禁用"即属正常。
