# PepperPlaceholder 现阶段方案（${} 定界 + `_` 切分 + params 预解析）

> 状态：**已落地并验证**（注册表模型，断言 GREEN 75/75，性能达标）
> 日期：2026-09-12
> 前序：`placeholder-engine-design.md`（原型已验证）；本文件为**语法层修正后的现阶段方案**

---

## 1. 为什么改语法

之前的原型（AST + 命名空间 + 前缀声明 + 方括号）在性能和安全上都已验证，但**语法层与 PAPI 生态不兼容**：

| 问题 | 说明 |
|---|---|
| 语法不统一 | `${ns:cmd[arg][arg]}` 需要迁移所有现有写法 |
| 适配成本 | `args[]` → PAPI `params` 需要重组 |
| 生态割裂 | 21 个现有 PAPI 扩展无法平滑接入 |

**实测结论**：三种语法（`_` 平铺 / `[]` 方括号 / `.` 点号）**还原 PAPI params 均无损**，所以"语法影响兼容"不成立——真正该对齐的是 **identifier + params 这个抽象**。

---

## 2. 核心语义（现阶段定稿）

### 2.1 定界符与切分

```
${identifier_params}
```

| 规则 | 说明 |
|---|---|
| 定界符 | `${...}`（与 PAPI 的 `%...%` 并存，互不干扰） |
| 切分 | **第一个 `_` 切一刀** → `identifier` + `params` |
| params | 余下**整段**，不做二次切分，由 handler 自己解释 |
| 无 `_` 时 | identifier 为整段，params 为空串 |

```java
int u = body.indexOf('_');
String identifier = u < 0 ? body : body.substring(0, u);
String params     = u < 0 ? ""   : body.substring(u + 1);
```

**这套切分与 PAPI 的 `CharsReplacer` 完全一致**（框架只切一次，参数原样交给扩展）。

### 2.2 params 预解析（本次选定）

**引擎先解析 params 里的 `${...}` 嵌套，再把结果交给 handler**：

```
${guild_${player_level}pvp}
  ① 引擎识别出 params 段含嵌套：${player_level}pvp
  ② 解析内层 → player_level 的值 "5"
  ③ params 变为 "5pvp"
  ④ handler.handle("guild", "5pvp")  ← 拿到已解析的 params
```

**收益**：handler 不用自己处理嵌套，动态 key 在框架层完成。
**代价**：见 §4 注入面。

### 2.3 与前一版原型的差异

| 维度 | 前一版（AST/方括号） | **本方案** |
|---|---|---|
| 语法 | `${ns:cmd[arg][arg]}` | **`${identifier_params}`** |
| 切分规则 | 注册时声明前缀 + 方括号 | **第一个 `_`** |
| 参数形态 | 结构化 `args[]` | **不透明 params 字符串** |
| 嵌套位置 | key 段 | **params 段** |
| 命名空间 | `ns:cmd` 显式 | **identifier 内隐含**（`union` 即 identifier） |
| handler 接口 | `(ctx, args[])` | **`(ctx, params)`** |
| 性能（静态） | 152 ~ 198 ns | **82 ns** |

---

## 2.5 授权模型：注册表即能力

**注册表（`Registry`）是唯一的隔离与授权单元**。解析时显式传入注册表列表，
引擎只在传入的注册表里查找 —— **手里有什么注册表，就能解析什么**。

```java
// 全局注册表：PAPI 等价的共享语义，resolve(template) 默认只走它
Registry global = engine.global();
global.register("player", SYNC, (params, ctx) -> "Pepper");

// 私有注册表：不递给别人，别人就解析不到
Registry claimPriv = engine.newRegistry("claim-private");
claimPriv.register("ledger", SYNC, (params, ctx) -> "内部账本");

// 解析：显式传入能力集
engine.resolve(text);                                  // → global
engine.resolve(text, global, claimPriv);               // → 两者都可解析
engine.resolve(text, safeRegistry);                    // → 只有 safe 里的
engine.resolve(text, new Registry[0]);                 // → 什么都不解析（安全默认）
```

**查找按传入顺序，先传入者优先** —— 可用前置注册表做覆盖。

### 2.5.1 为什么用注册表而不是作用域白名单

| | 作用域/域白名单 | **注册表（本方案）** |
|---|---|---|
| 隔离单元 | 域名字符串（全局命名空间） | **注册表对象** |
| 授权方式 | `Ctx` 里带域白名单 | **直接传注册表** |
| 跨插件重名 | ⚠️ 需协调域名 | ✅ 对象各自独立 |
| 忘了限制 | ⚠️ 默认不限 → 洞 | ✅ **必须传，传什么用什么** |
| 附属插件 | `SHARED` + `sharedWith` 白名单 | **把注册表递给它** |
| 引擎概念数 | Scope + Vis + allowedDomains | **只有 Registry** |

**核心优势：授权从「声明式白名单」变成「能力传递」**——持有注册表即拥有能力，
引擎不维护任何可见性规则。`Scope` / `Vis` / `visibleTo` 已全部删除。

### 2.5.2 覆盖两条原始需求

| 需求 | 注册表模型的表达 |
|---|---|
| 跨插件私有（仅本插件及附属可用） | 私有注册表**只递给自己和附属** |
| 玩家配置限定范围 | 玩家模板**只传安全子集注册表** |

### 2.5.3 逐变量回灌的耦合

`Reinject` 声明挂在注册表内的绑定上。回灌递归时**沿用同一注册表列表**，
因此回灌不会越权解析未传入注册表的变量。

## 3. 实测性能（注册表模型）

独立 JVM，每场景 3 次取中位（同进程连测会互相干扰 JIT，方差可达 30%）：

| 场景 | ns/op | 验收线 |
|---|---|---|
| 无占位符（预检直接返回） | 19 | — |
| **静态占位符 ×2（默认 global）** | **145** | **≤ 200** ✅ |
| **动态 key（params 预解析）** | **141** | **≤ 500** ✅ |
| 参数式（不透明 params） | 111 | — |
| 回灌（RESOLVE 值含变量名） | 135 | — |

**注册表数量的影响**（同一 handler、同一模板，只变传入的注册表个数）：

| 传入注册表数 | 命中位置 | ns/op |
|---|---|---|
| 1 | 第 1 个 | 73 |
| 2 | 第 1 个 | 62 |
| 4 | 第 1 个 | 62 |
| 4 | **第 4 个** | 60 |
| 1 | 未命中 | 61 |
| 4 | 未命中 | 67 |

**结论：注册表数量对命中路径无影响**（60~73 全在噪声内，命中即返回）；
未命中时每多查一个注册表约 +2 ns。**能力模型几乎零成本。**

**参照**：MiniPlaceholders `deserialize` 3637 ns（基线）、PAPI 纯字符串替换 192 ns。

## 4. ⚠️ 注入面（本方案的安全代价）

### 4.1 风险机制

params 预解析意味着**解析结果会回灌进参数位置**：

```
${admin_${input}}
  ① 解析内层 ${input} → 得到某值 V
  ② V 被拼进 params
  ③ handler("admin", V) 被调用
```

**若 V 的来源是玩家可控数据**，且 handler 内部会用它拼接后续查找，则存在越权读取的路径。

### 4.2 实测结果：未能构造出有效攻击

我用当前实现尝试构造：

```
${input}          → ${player_level}      （input 的返回值是占位符字面量）
${admin_${input}} → ${admin_${input}}    （未泄露，保留原文）
```

**攻击未生效**。原因：`input` 的返回值 `${player_level}` 作为**数据**被拼进 params 后，`resolveParams` 的收敛循环已结束（值不回灌），不会再次展开为 `5`。

### 4.3 但有条件的风险路径

攻击成立需要**同时满足三个条件**：

1. 玩家能控制某个占位符的**返回字符串**
2. 该占位符被用作另一个占位符的 **params 位置**
3. 目标 handler 会**用 params 去查表或拼接敏感查询**

**判定**：你服务器上目前**不满足条件 3**（`PepperClaim`/`PepperUnion` 的 handler 不会把 params 当 key 去查敏感表），所以当前无实际风险。但**设计上留了这个口子**，新增 handler 时必须注意。

### 4.4 三条缓解措施（按推荐度）

| 措施 | 说明 | 成本 |
|---|---|---|
| **① handler 侧白名单校验** | params 用于查表前校验取值域（如 `config_pvp` 只接受枚举内的 key） | 每个 handler ~5 行 |
| **② 区分"可信 params"与"数据拼接"** | 引擎只解析**模板源串里写死的**嵌套；handler 自己拼接的值不参与解析 | 架构上更严谨 |
| **③ 记录告警** | params 预解析后若结果仍含 `${`，记 warning 便于排查 | ~3 行 |

**建议至少做 ①**——它同时解决参数合法性校验（不仅是安全，也是健壮性）。

---

## 5. 与 PAPI 的关系（不做 `%...%` 适配）

**决策：引擎只支持 `${...}` 定界，不解析 `%...%` 模板。**

### 5.1 这意味着什么

| 项 | 状态 |
|---|---|
| 现有 15 个 PAPI 消费者（TAB / TrChat / HuskHomes…） | **继续走 PAPI，零改动** |
| 现有 10 个 PAPI 扩展 | **继续注册在 PAPI，零改动** |
| 引擎 | 只服务**新代码**的 `${...}` 模板 |
| `%...%` → `${...}` 语法转换 | **不做** |
| 替换式 PAPI shim | **不做** |

### 5.2 为什么可以不做

PAPI 的 `%...%` 与我们的 `${...}` 在**文本层面互不干扰**：

```
"欢迎 %player_name% 来到 ${guild_name}"
        └─ PAPI 解析        └─ 引擎解析
```

两套定界符可以**共存于同一段文本**，各解析各的。因此不需要转换、不需要拦截、
不需要替换 PAPI —— 迁移是**增量的**：新代码用 `${...}`，旧配置保持 `%...%`。

### 5.3 唯一保留的接口约定：切分规则对齐

虽然不解析 `%...%`，但**切分规则与 PAPI 的 `CharsReplacer` 完全一致**
（首个 `_` 切一刀，params 整段不透明），这带来两个好处：

1. **将来若要复用 PAPI 扩展**，注册时 `params` 可原样转发：
   ```java
   global.register(expansion.getIdentifier(), SYNC,
       (params, ctx) -> expansion.onRequest(ctx.player(), params));   // 零转换
   ```
2. **心智模型一致** —— 写惯了 `%math_2+2_2:half-up%` 的人不需要重新理解 `${math_2+2_2:half-up}`

扩展自己定的子分隔符（如 math 的 `_` + `:`）同样保留，引擎不介入。

### 5.4 若将来需要复用 PAPI 扩展

唯一可行且低风险的路径是**注册桥**（真 PAPI 保留）：

```java
@EventHandler
public void onExpansionRegister(ExpansionRegisterEvent e) {
    PlaceholderExpansion exp = e.getExpansion();
    engine.global().register(exp.getIdentifier(), SYNC,
        (params, ctx) -> exp.onRequest(ctx.player(), params));
}
```

这样引擎能解析 PAPI 扩展的数据（写作 `${player_name}`），而消费者仍走 PAPI。
**不做替换式 shim** —— 成本高（需复刻 ~50 个方法 + 扩展加载器 + TAB 依赖的内部类）、
与 PAPI 互斥、且丢失 eCloud 功能，收益仅为架构统一。

---

## 6. 实现清单

| 项 | 状态 |
|---|---|
| `${}` 平衡解析 | ✅ |
| `_` 单次切分（identifier + params） | ✅ |
| params 不透明（handler 自定子分隔符） | ✅ |
| params 预解析（栈扫描内层弹帧即写回上层） | ✅ |
| handler 契约 `(params, ctx)` | ✅ |
| 注册表隔离与组合 | ✅ |
| 栈式单次扫描（惰性帧栈） | ✅ |
| 注册表模型（global + 具名） | ✅ |
| 异步 SYNC/ASYNC 双路径 + TTL 缓存 | ✅ |
| 值不回灌 | ✅ |
| 断言式测试（75 项 GREEN） | ✅ |
| 独立 JVM 基准（每场景 3 次取中位） | ✅ |
| `%...%` 适配 / PAPI shim | ❌ 不做（§5） |
| 注册桥（复用 PAPI 扩展，可选） | ⬜ 低风险，按需 |
| 关系型占位符（viewer/target） | ⬜ 按需 |
| Paper / Velocity 平台接线 | ⬜ 下一阶段 |

### 6.0 扫描帧的分配优化（惰性 `params`）

`Frame` 的两个 `StringBuilder` 中，`params` 原先在构造时**无条件分配**，但
`${balance}`、`${guild-config}` 这类无 `_` 的占位符永远用不到它。改为**遇首个 `_` 时才创建**：

```java
private static final class Frame {
    final StringBuilder id = new StringBuilder(16);
    StringBuilder params;        // ← 惰性：遇首个 `_` 才建
    StringBuilder cur = id;
    boolean hasSeparator;
}
// 不变式：hasSeparator == true ⟺ params != null（两处使用点均由 hasSeparator 守卫）
```

**真实引擎级分配量对照**（同进程 A/B，20 万次/轮）：

| 场景 | 急切分配 | 惰性分配 | 节省 |
|---|---|---|---|
| 无 params `${balance}` | 328 B/op | **272 B/op** | **17%** |
| 有 params `${guild_name}` | 488 B/op | 488 B/op | 0% |
| 嵌套 `${guild_${level}x}` | 640 B/op | **584 B/op** | **9%** |

**被否掉的替代方案**：用单个 `StringBuilder` 装整段、闭合时再 `indexOf('_')` 切分。
实测**更差**——分配 528 vs 512 B/op（多 3%）、耗时 105 vs 99 ns（慢 6%），
因为闭合时要额外做 `toString()` + 2 次 `substring`（3 次 String 分配）。
`cur` 指针的价值正是**避免这三次分配**。

### 6.0b 逐变量回灌声明（`Reinject`）

**值永不回灌**是默认语义（防注入）。但确有变量其值**就是另一个变量名**（如"间接引用"型
占位符）。为此在**注册时逐变量声明**，而非全局开关：

```java
public enum Reinject {
    LITERAL,   // 默认：值当字面量输出，永不二次解析
    RESOLVE    // 值可能含 ${...}，解析后继续展开（带环检测）
}

register(scope, "guild_name", handler);                    // LITERAL
register(scope, "indirect",   handler, Reinject.RESOLVE);  // 可回灌
```

**实现要点**（`Engine.evaluate`）：

```java
String value = ...handler 求值...;

// 三条件短路：非 RESOLVE 或值干净 → 零开销返回
if (value == null
        || entry.reinject() != Reinject.RESOLVE
        || value.indexOf(OPEN) < 0) {
    return value;
}

Deque<String> chain = path;                    // 惰性：首层回灌才分配 ArrayDeque
if (chain == null) chain = new ArrayDeque<>(4);
else if (chain.contains(key)) return null;     // 成环 → 保留原文
chain.push(key);
try {
    return scan(value, ctx, chain);            // 递归展开
} finally {
    chain.pop();                               // ★ 离开时移除，保证路径干净
}
```

**环检测必须是"路径"而非"已解析集合"**：早期版本用 `Set.add` 且从不移除，
导致 `${a} ${a}` 中第二个 `${a}` 被误判成环。`push`/`pop` 成对才能保证
"环之后同模板内其他变量仍正常解析"。

**实测**（独立 JVM，每项 3 次取中位）：

| 场景 | ns/op | 分配 B/op |
|---|---|---|
| 静态占位符 ×2（LITERAL） | 142 | — |
| 参数式（LITERAL） | 110 | 376 |
| LITERAL 但值含 `${`（不回灌） | — | 272 |
| **RESOLVE 值含 `${`（回灌）** | **135** | **704** |

**默认路径零影响**：LITERAL 变量在 `entry.reinject() != RESOLVE` 处短路，
不分配任何回灌结构。只有显式声明 RESOLVE 的变量承担 432 B/op 的递归开销。

### 6.1 identifier 命名约束与 `-` 方案

**约束**：identifier 不能含 `_` —— 首个 `_` 即 identifier/params 切分点。这与 PAPI 的
`%identifier_params%` 约定一致（切分逻辑完全对齐 `CharsReplacer`）。

**多词 identifier 用 `-`**：

| 写法 | identifier | params |
|---|---|---|
| `${guild-config}` | `guild-config` | `""` |
| `${guild-config_pvp}` | `guild-config` | `pvp` |
| `${union_guild_name}` | `union` | `guild_name` |
| `${union_guild_config_pvp}` | `union` | `guild_config_pvp` |

**`-` 与 `_` 语义不冲突**：`-` 属于 identifier 名（不参与切分），`_` 是切分点。
`-` 也可用于内层嵌套：`${guild_${p-level}x}` → 内层解析后外层查表。

**真实迁移面（已核对现有插件）**：

| 插件 | 现有 identifier | 影响 |
|---|---|---|
| PepperUnion | `union` | **零改动**：`%union_guild_name%` → `${union_guild_name}` |
| PepperClaim | `pepperclaim` | **零改动** |

即：`-` 方案主要为**引擎内部/测试夹具**服务（如 `p-key`、`loop-a`），现有 21 个扩展的
identifier 均不含 `_`，无需改名。

**注册期校验**：identifier 含 `_` → `IllegalArgumentException`（启动期快速失败，附修复提示）。

## 7. 待确认

1. **identifier 冲突处理**：`union` 与 `pepperclaim` 各自注册 `identifier`，若两个插件用同一 identifier 怎么办？
   - 选项 A：注册即报错（快速失败）
   - 选项 B：加命名空间前缀 `${ns:identifier_params}`（重新引入命名空间）
   - **当前建议 A**（生态内 identifier 天然唯一，因 PAPI 也要求唯一）

2. **scope 语义如何落到无命名空间的语法上**：
   - 裸引用 `${guild_name}` 受作用域可见性约束（跨插件的私有占位符互不可见）
   - 但 `%...%` 那侧由 PAPI 解析，不受本引擎 scope 约束

3. **params 预解析的深度上限**：当前 8 轮，是否需要可配置？
