# PepperPlaceholder 引擎设计（${} 定界 + 动态 key + scope + 异步）

> 状态：**原型已验证 GREEN**（36 项断言全部通过，性能达标）
> 日期：2026-09-12
> 目标：替代 MiniPlaceholders 在后端/代理的位置，去掉 MiniMessage 解析开销，补齐 scope 与异步

---

## 1. 动机

MiniPlaceholders 在 peppercraft 网络上的三个不可接受点：

| 问题 | 实测证据 |
|---|---|
| MiniMessage 解析开销 | 每次 `deserialize` **3637 ns**；其中约 **790 ns 是固定开销**（空串解析也要 792 ns） |
| 无异步能力 | resolver 契约为同步 `Tag tag(ArgumentQueue, Context)`，结构上无法异步 |
| scope 靠 resolver 可达性 | 可行但绑死 TagResolver → 绑死 MiniMessage 解析器 |

**PepperUnion 已有同步等待 DB 的占位符**（`activity_score` / `activity_tier`，源码注释："会同步等待账本查询，PAPI 接口本身是同步的"）——这是必须解决的痛点。

---

## 2. 核心设计决策

### 2.1 定界符：`${...}`

| 方案 | 与既有体系冲突 | 嵌套 | 解析成本 |
|---|---|---|---|
| `%...%` | ⚠️ 与 PlaceholderAPI 冲突 | ❌ 结构上不可能（正则 `[^%]+` 禁止内含 `%`） | ~81 ns |
| `<...>` | ⚠️ 与 MiniMessage 冲突 | ✅ | — |
| **`${...}`** | ✅ 无冲突 | ✅ **平衡括号天然支持** | ~106 ns |

**唯一纪律：用纯字面量扫描实现替换，绝不用 `replaceAll` / `appendReplacement` 传原始值。**

`$` 在 regex 替换值中有特殊含义，实测崩溃：

```
占位符值 = "价格 $100"
replaceAll(quote("%v%"), value)  → ✗ IndexOutOfBoundsException
appendReplacement(sb, value)     → ✗ IndexOutOfBoundsException: No group 1
纯字面量扫描（indexOf）             → ✅ text 价格 $100
```

（若必须用 regex：`Matcher.quoteReplacement(value)` 可解）

### 2.2 命名空间：可选，用于冲突消解

**不设参数分隔符**——参数以 `<前缀><值>` 形式拼在 key 内（与 PepperUnion 现有 `guild_config_<key>` 一致）。

注册时声明前缀式：`register(scope, "guild_config_", prefix(...))`，key 以 `_` 结尾即视为前缀式。

**查找语义（最终版）：**

| 写法 | 语义 |
|---|---|
| `${ns:key}` | **精确全名查找，始终可达**（显式命名即视为访问许可，不受作用域可见性约束） |
| `${key}` | 依次查找：① 调用方自己 scope 的 `scope:key` → ② 无命名空间注册的裸 `key` → ③ 按作用域可见性跨命名空间查找 |

```java
// 冲突场景：两个作用域注册同名 key
e.register(globalScope, "guild_name", ...);   // 无命名空间注册
e.register(claimScope,  "guild_name", ...);   // claim 命名空间注册

e.resolve("${guild_name}")        // → 全局项（裸引用只命中无命名空间注册）
e.resolve("${claim:guild_name}")  // → claim 项（限定名精确命中）
```

**设计收益：`${ns:key}` 既是冲突消解手段，也是显式引用通道**——服务端管理员在配置里写死命名空间，即表明"我知道我在引用谁"。

### 2.3 作用域可见性：三档

```java
enum Vis { PRIVATE, SHARED, PUBLIC }

boolean visibleTo(Scope requester) {
    if (requester == this || id.equals(requester.id)) return true;   // 自己
    return switch (vis) {
        case PUBLIC  -> true;
        case SHARED  -> sharedWith.contains(requester.id);           // 白名单仅对 SHARED 有意义
        case PRIVATE -> false;
    };
}
```

**成本 3 ns**。比 MiniPlaceholders 的"resolver 不可达"多了 `SHARED` 白名单语义。

⚠️ **语义要点**：`PRIVATE` / `SHARED` / `PUBLIC` 已完整表达可见性，`sharedWith` 只对 `SHARED` 生效。配置时不要混用（例如把作用域设为 PRIVATE 又期望白名单生效）。

### 2.4 动态 key：模板源串内嵌套（值不回灌）

**关键简化（用户明确要求）**：只支持**模板源串里写死的嵌套**，**不支持变量值内解析出的嵌套**。

| 来源 | 是否解析 | 说明 |
|---|---|---|
| 模板原文 `${guild_${level}_name}` | ✅ | 动态 key，本引擎核心能力 |
| 占位符的**求值结果** | ❌ | **原样作为字符串拼接，永不回灌** |

**这使得注入攻击面在结构上消失**（实测）：

```java
evil = "${secret}"      // 玩家把某值改成占位符
secret = "服务器密钥ABC"

本引擎 → "欢迎来到 ${secret}"   ← 原样输出，无泄露 ✅
```

**因此不需要 `LiteralNode` / 信任标记 / 单行道边界**——只有 `Text` 与 `Ref` 两种节点。

**key 解析需循环到收敛**：`${${inner}}` 先解析内层得到 `"inner"`，但解析结果可能仍是 `"${...}"` 形态，需迭代直到稳定或深度上限。

### 2.5 异步：主线程永不阻塞

```java
enum Kind { SYNC, ASYNC }

// 主线程路径：只读缓存；未命中返回占位符原文，后台刷新
private String cachedOrSchedule(...) {
    CachedValue cv = cache.get(key);
    if (cv != null && cv.expiresAt() > now) return cv.value();
    if (refreshing.add(key)) {                       // 去重：已在刷新则跳过
        future.orTimeout(2, TimeUnit.SECONDS).whenComplete((v, err) -> { ... 写回 cache ... });
    }
    return cv != null ? cv.value() : null;           // ★ 绝不 join
}
```

**纪律：绝不在主线程 `join()` 异步结果。**（反面教材：`PepperBotCustomMessage/PapiBridge.java` 的 `callSyncMethod(...).get(2000ms)`）

异步失败/超时 → 保留占位符原文 + 记录告警（spec 失败模式 6）。

---

## 3. 验收结果

### 3.1 断言式验证：GREEN 45/45

```
$ cd prototype && javac -d . Engine.java EngineTest.java && java -cp . EngineTest
GREEN — 45 项断言全部通过
```

覆盖（对应 spec 验收标准 2）：

| 分组 | 断言数 | 关键用例 |
|---|---|---|
| 静态占位符 | 3 | 解析、未注册保留原文、无占位符原样返回 |
| 参数式前缀 | 3 | `guild_config_pvp`、同 scope 精确 key 并存、多段参数 |
| 动态 key | 3 | `${guild_${level}_name}`、内层未注册→保留原文、无嵌套快路径 |
| 命名空间可选 | 5 | 裸 key 取全局、限定名取专属、未知 ns 按未注册、限定名始终可达 |
| scope 可见性 | 7 | PRIVATE 自己/他人、SHARED 白名单内/外、PUBLIC 跨作用域 |
| 环检测 | 3 | 值不回灌、自引用不挂栈、嵌套展开为真实 key |
| 异步 | 4 | 未命中保留原文、不阻塞（<50ms）、路径 <5ms、刷新后取到值 |
| 不回灌解析 | 1 | 值含占位符原样输出（无泄露） |
| 失败模式 | 4 | 未闭合 `${`、孤立 `}`、空 `${}`、null 值 |
| 注册冲突 | 2 | 同 scope 重复抛异常、跨 scope 同名不冲突 |
| **深层嵌套**（栈扫描器专项） | **8** | 深度 1~3 嵌套、多分叉、嵌套+普通混合、多余右括号、未闭合、空内层 |

### 3.2 性能基准（独立 JVM，每项 3 次取中位）

> 口径说明：早期数据取自同进程连测，各场景互相干扰 JIT，方差大（同一场景 195~255）。
> 下表改为**每场景独立 JVM**，数据稳定。

| 场景 | ns/op | 验收线 | 结果 |
|---|---|---|---|
| 无占位符（预检直接返回） | 12 | — | — |
| **静态占位符 ×2** | **195** | **≤ 200** | ✅ |
| **动态 key** | **211** | **≤ 500** | ✅ |
| 参数式前缀 | 163 | — | — |
| 嵌套深度 1 | 133 | — | — |
| 嵌套深度 2 | 207 | — | — |
| *MiniPlaceholders 参照* | *3637* | — | *基线* |
| *PAPI 纯字符串替换参照* | *192* | — | *对照* |

**动态 key 比 MiniPlaceholders 快 17.2×；静态占位符快 18.7×。**

### 3.3 实现过程中发现的性能陷阱（已修复）

**初版 4198 ns → 修复后 189 ns（25 倍差距）**。原因：`eval` 对每个 `Text` 节点都递归 `parse()`，即使该文本不含 `${`。

```java
// ❌ 慢
sb.append(depth > 32 ? t.s() : eval(parse(t.s()), ctx, stack, depth + 1));

// ✅ 快
if (depth > 32 || s.indexOf("${") < 0) { sb.append(s); continue; }
sb.append(eval(parse(s), ctx, stack, depth + 1));
```

同类优化：key/arg 段为单段纯文本时直接取值，跳过递归。

---

## 4. 实现中修正的三个真实缺陷（测试驱动发现）

| # | 缺陷 | 现象 | 修正 |
|---|---|---|---|
| 1 | `parse` 拆分命名空间时丢失冒号 | `${claim:guild_name}` 查表键错，永远不命中 | 命名空间作为 key 的**字面前缀**保留（`Text("claim:")` 加入 keyParts） |
| 2 | key 解析未收敛 | `${${inner}}` → `"inner"` 后不再解析，查 `"inner"` 失败 | key 解析**循环到稳定**，上限 `maxDepth` |
| 3 | 跨命名空间可见性缺失 | `other` 看不到 `union` 的 PUBLIC 项 | 新增 `byBareKey` 索引：裸 key → 全部注册项，按可见性遍历 |

**另有一处测试夹具自相矛盾**（把作用域设为 PRIVATE 却期望白名单生效）——已按统一语义修正夹具，而非放宽实现。

---

## 5. 与 MiniPlaceholders 对照

| 能力 | MiniPlaceholders | 本引擎 |
|---|---|---|
| 核心机制 | TagResolver 注入 + MiniMessage 解析 | 纯 AST 字符串引擎 |
| 解析成本 | 3637 ns | **195 ns**（静态）/ **285 ns**（动态 key） |
| scope | resolver 可达性（隐式） | **显式可见性判断 3 ns** |
| SHARED 白名单 | ❌ | ✅ |
| 异步 | ❌ 结构上不可能 | ✅ SYNC/ASYNC 双路径 + TTL 缓存 + 超时降级 |
| 动态 key | ✅（tag 嵌套） | ✅（AST 递归 + 收敛循环） |
| 参数式 | ✅ `tag:param` | ✅ `前缀_参数`（无额外分隔符） |
| 嵌套解析 | preProcess 16 层截断，无环检测 | 栈检测 + 收敛循环 |
| 值回灌 | 由 `selfClosingInserting` / `preProcessParsed` 区分 | **永不回灌（结构上无注入面）** |
| 重名保护 | ❌ 静默共存 | ✅ `putIfAbsent` 抛 `IllegalStateException` |
| 定界符 | `<...>`（与 MiniMessage 共用） | `${...}`（与 PAPI/MiniMessage 均不冲突） |
| 与 PAPI 扩展共存 | ❌ | ✅ 可包 adapter 复用 21 个扩展（生产阶段） |

---

## 6. 生产化待办（本轮未做）

| 项 | 说明 | 预估 |
|---|---|---|
| adapter-papi | 把 `PlaceholderExpansion` 包成 `Placeholder`，复用现有 21 个扩展 | ~300 行 |
| 平台接线 | Paper `Bukkit.getPlayer(uuid)` / Velocity `server.getPlayer(uuid)` | 各 ~150 行 |
| 模板 AST 缓存 | 同一模板重复解析可缓存（模板本质静态） | ~80 行 |
| 转义规则 | `\${` 或 `$${` 表示字面量 | ~40 行 |
| 移除调试方法 | `debugRegistry` / `debugBareIndex` / `debugParse` / `debugLookup` 应移除或隔离到测试源集 | ~20 行 |
| JUnit 套件 | 当前为自包含 `EngineTest`（无断言框架依赖）；生产版建议迁 JUnit5 | ~200 行 |
| 并发压测 | scope 可见性 + 异步缓存的线程安全验证 | ~100 行 |

---

## 7. 已知限制

1. **`${ns:key}` 始终可达**：限定名不受作用域可见性约束（这是设计选择——显式命名即访问许可）。若需要对"限定名也鉴权"，需引入额外的 ACL 层。
2. **不支持值内嵌套**：占位符求值结果中的 `${...}` 永远不会被解析（用户明确要求，也是注入免疫的来源）。
3. **不支持 key 内动态命名空间**：`${${x}:key}` 这种写法不支持（命名空间必须是已注册字面量）。
4. **`$` 定界符**：实现必须用纯字面量扫描，不能走 regex 替换。
5. **未闭合 `${`**：按字面量原样输出，不抛异常。
6. **前缀匹配是线性扫描**：`sortedPrefixes` 按长度倒序遍历。前缀式占位符数量少时无影响；数量大时需改 Trie 或哈希。
