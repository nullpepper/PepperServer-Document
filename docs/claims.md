# 6. 个人领地 PepperClaim

## 6.1 基本规则

| 项目 | 规则 |
| --- | --- |
| 每人可拥有个人领地 | **1 块** |
| 初始额度 | 1,024 格 |
| 额度恢复 | 每小时 +256 格，上限 4,096 |
| 可购买额度 | 最多 2,048 格；**当前实际 5 钻石/格**（注释 0.05，见 5.4 价格提示） |
| 最小边长 | 5 格 |
| 最大边长 | 64 格 |
| 长宽比 | 不超过 4:1 |
| 创建冷却 | 30 秒 |
| 重划冷却 | 72 小时 |
| 转让冷却 | 7 天 |
| 领地传送冷却 | 30 秒，免费 |
| 主城保护 | Spawn 世界全境不可圈地（WorldGuard 保护区） |
| 出生点保护 | 每个世界出生点 100 格内不能完整圈地 |

领地是 **2D 全高** 的：从基岩到天空都属于同一块领地，不能只圈地下或只圈地面。

## 6.2 创建领地

### 金锄手选创建

1. 手持**普通金锄**（不需要插件发放，自己合成即可）。
2. 左键点第一个角，右键点对角。
3. 输入：

```text
/pclaim create <领地名>
```

选区必须满足最小/最大边长和长宽比；领地不能与已有领地重叠，也不能建在主城（Spawn 世界）全境，或整个落在任一世界出生点 100 格内。创建后有 30 秒冷却。

## 6.3 额度与扩容

```text
/pclaim quota              查看已用/剩余额度
/pclaim buy <格数>         用余额购买额度（当前实际 5 钻石/格，见 5.4）
/pclaim expand <格数> [north|south|east|west|out]
/pclaim resize <领地名>    用金锄重新选区后应用
```

- 初始 1,024 格，约等于 32×32。
- 想建 64×64（4,096 格）需要先扩容到上限。
- 扩容和重划共用 72 小时冷却；缩小领地会按规则返还额度。
- 额度每小时恢复 256 格，封顶 4,096。

## 6.4 领地保护与旗标

新领地默认规则：

| 行为 | 默认 | 说明 |
| --- | --- | --- |
| 外人进入（move） | 允许 | 领地不是封闭盒子，别人可以走进来 |
| 破坏/放置（build） | 禁止 | 包括挖掘、放置、使用桶等 |
| 打开容器（container） | 禁止 | 箱子、桶、潜影盒等 |
| 使用按钮/门/拉杆（use） | 禁止 | 红石交互也受控 |
| 骑乘（riding） | 禁止 | 不能骑领地内的动物/载具 |
| 击杀动物（animalkilling） | 禁止 | 保护养殖 |
| 领地内 PvP | 默认关闭 | 可在领地内开启 |
| 传送进入（tp） | 禁止 | 防止他人直接传进领地 |

常用旗标命令：

```text
/pclaim set flag <领地> <旗标> <on|off|clear>   设置整个领地
/pclaim group <领地> setflag <组> <旗标> <值>   设置某个成员组
/pclaim explain <领地> <旗标>                   查看当前值和来源
```

常用旗标：`build`、`container`、`use`、`move`、`pvp`、`tp`、`riding`、`animalkilling`、`mobkilling`、`ignite`、`destroy`、`harvest`、`hook`、`leash`、`shear`。

> 有非屋主在领地内时，屋主不能把 `pvp` 改成开启（防“骗进来再开 PvP”）。需要先让对方离开。

## 6.5 成员与权限组

每个领地最多 **4 个自定义组**。新建组后，需要给组设置旗标，再把玩家分配进去。

```text
/pclaim group <领地> list                         查看现有组
/pclaim group <领地> create <组名>                新建组
/pclaim group <领地> assign <玩家> <组名> [时长]  把玩家加入组
/pclaim group <领地> unassign <玩家>              移出组
/pclaim group <领地> setflag <组> <旗标> <值>     设置该组权限
/pclaim group <领地> delete <组名>                删除组
```

给朋友开放建造的典型步骤：

```text
/pclaim group myhome create friend
/pclaim group myhome setflag friend build on
/pclaim group myhome setflag friend container on
/pclaim group myhome setflag friend use on
/pclaim group myhome assign Steve friend
```

也可以直接打开 `/pclaim gui`，在图形界面里完成大部分成员和旗标操作。

## 6.6 领地传送与转让

```text
/pclaim settp              在当前位置设置领地传送点
/pclaim home               回自己的领地（优先使用已设的领地传送点）
/pclaim home transfer <玩家>   把领地转让给对方
/pclaim home accept        接受别人转来的领地
```

- 领地传送免费，冷却 30 秒。
- 转让有 7 天冷却；对方必须输入 `/pclaim home accept` 才算完成。
- 转让后原屋主失去全部权限，操作前确认清楚。

## 6.7 封禁、删除与查询

```text
/pclaim ban <玩家> [领地]      禁止某人进入/使用领地
/pclaim unban <玩家> [领地]    解除封禁
/pclaim info [领地]            查看领地信息
/pclaim list                   列出自己的领地
/pclaim map                    在聊天栏显示附近领地地图
/pclaim borders                开关领地边界显示
/pclaim select <领地>          选中已有领地的范围
/pclaim trapped                卡在领地内时脱困
```

删除领地：

```text
/pclaim delete <领地>
```

需要在 10 秒内再次确认。删除不可恢复，里面的建筑不会返还。

## 6.8 个人领地限制一览

| 限制 | 数值 |
| --- | --- |
| 每人领地数 | 1 |
| 初始额度 | 1,024 |
| 额度上限 | 4,096 |
| 每小时恢复 | 256 |
| 购买价格 | 当前实际 5 钻石/格（注释 0.05，见 5.4） |
| 最多购买 | 2,048 格 |
| 最小/最大边长 | 5 / 64 |
| 最大长宽比 | 4:1 |
| 创建冷却 | 30 秒 |
| 扩容冷却 | 72 小时 |
| 转让冷却 | 7 天 |
| 传送冷却 | 30 秒 |
| 自定义组上限 | 4 |
| 领地维护税 | **无**（当前关闭） |
| 领地出租/拍卖 | **未开放** |
