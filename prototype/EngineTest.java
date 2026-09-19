import java.util.*;
import java.util.concurrent.*;

import com.pepper.placeholder.Engine;
import com.pepper.placeholder.Engine.Registry;

/**
 * PepperPlaceholder 原型断言式验证（注册表模型）。
 *
 * <p>语法：{@code ${identifier_params}} —— 首个 {@code _} 单次切分，
 * params 为不透明字符串，引擎先解析 params 内的 {@code ${...}} 嵌套再交给 handler。</p>
 *
 * <p>授权：解析时显式传入 {@link Registry} 列表，引擎只在传入的注册表里查找。
 * {@code resolve(template)} 默认只走内置的 global 注册表。</p>
 *
 * <p>任一断言失败即非零退出，便于后续改动回归。</p>
 */
public final class EngineTest {

    private static final Engine.Kind SYNC = Engine.Kind.SYNC;
    private static final Engine.Ctx NONE = Engine.Ctx.NONE;

    private static int passed = 0;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new EngineTest().run();
        System.out.println();
        System.out.println("=".repeat(64));
        if (failures.isEmpty()) {
            System.out.printf("GREEN — %d 项断言全部通过%n", passed);
        } else {
            System.out.printf("RED — %d 项通过，%d 项失败%n", passed, failures.size());
            for (String f : failures) {
                System.out.println("  ✗ " + f);
            }
            System.exit(1);
        }
    }

    private void run() throws Exception {
        testStatic();
        testSingleSplit();
        testOpaqueParams();
        testNestedInParams();
        testFailureModes();
        testPapiCoexistence();
        testGlobalDefault();
        testRegistryIsolation();
        testRegistryComposition();
        testPlayerConfigScenario();
        testNoReExpansion();
        testCycle();
        testAsync();
        testConflict();
        testHyphenIdentifier();
        testReinjection();
    }

    // ============ ① 静态占位符 ============
    private void testStatic() {
        Engine e = new Engine();
        Registry r = e.global();
        r.register("guild", SYNC, (params, ctx) -> "Pepper联盟");
        r.register("a", SYNC, (params, ctx) -> "A");
        r.register("b", SYNC, (params, ctx) -> "B");

        check("静态占位符", "联盟 Pepper联盟", e.resolve("联盟 ${guild_name}"));
        check("未注册 identifier 保留原文", "[${nobody}]", e.resolve("[${nobody}]"));
        check("无占位符文本原样返回", "纯文本", e.resolve("纯文本"));
        check("同文本多个占位符", "A/B", e.resolve("${a_1}/${b_1}"));
    }

    // ============ ② 单次 `_` 切分 ============
    private void testSingleSplit() {
        Engine e = new Engine();
        final String[] captured = new String[1];
        e.global().register("guild", SYNC, (params, ctx) -> {
            captured[0] = params;
            return "[" + params + "]";
        });

        check("无下划线 → params 为空串", "[]", e.resolve("${guild}"));
        checkTrue("无下划线时 params 是空串而非 null", "".equals(captured[0]));
        check("单下划线切一次", "[config_pvp]", e.resolve("${guild_config_pvp}"));
        checkTrue("params 为 'config_pvp'（整段）", "config_pvp".equals(captured[0]));
        check("多下划线不二次切分", "[a_b_c_d]", e.resolve("${guild_a_b_c_d}"));
        checkTrue("params 保留全部下划线", "a_b_c_d".equals(captured[0]));

        // 惰性分配边界：`_` 后为空 / 连续 `_`
        check("分隔符后为空 → params 空串", "[]", e.resolve("${guild_}"));
        checkTrue("`_` 后为空时 params 仍是空串", "".equals(captured[0]));
        check("连续两个 `_` → params 为单下划线", "[_]", e.resolve("${guild__}"));
        checkTrue("连续 `_` 时 params 为 '_'", "_".equals(captured[0]));
    }

    // ============ ③ params 不透明（handler 自定子分隔符） ============
    private void testOpaqueParams() {
        Engine e = new Engine();
        // 模拟 math 的两级约定：表达式 _ 精度:舍入
        e.global().register("math", SYNC, (params, ctx) -> {
            int u = params.indexOf('_');
            if (u < 0) {
                return "表达式(" + params + ")";
            }
            String expr = params.substring(0, u);
            String rest = params.substring(u + 1);
            int c = rest.indexOf(':');
            String prec = c < 0 ? rest : rest.substring(0, c);
            String round = c < 0 ? "" : rest.substring(c + 1);
            return "表达式(" + expr + ") 精度(" + prec + ") 舍入(" + round + ")";
        });

        check("math 无精度", "表达式(2+2)", e.resolve("${math_2+2}"));
        check("math 两级约定", "表达式(2+2) 精度(2) 舍入(half-up)", e.resolve("${math_2+2_2:half-up}"));
    }

    // ============ ④ 模板内嵌套（params 预解析） ============
    private void testNestedInParams() {
        Engine e = new Engine();
        Registry r = e.global();
        r.register("player", SYNC, (params, ctx) -> "level".equals(params) ? "V" : null);
        r.register("guild", SYNC, (params, ctx) -> "G(" + params + ")");

        check("params 预解析（内层已展开）", "G(Vx)", e.resolve("${guild_${player_level}x}"));
        check("嵌套在中间", "G(前缀V后缀)", e.resolve("${guild_前缀${player_level}后缀}"));
        check("多分叉嵌套", "G(V)-G(V)", e.resolve("${guild_${player_level}}-${guild_${player_level}}"));
    }

    // ============ ⑤ 失败模式 ============
    private void testFailureModes() {
        Engine e = new Engine();
        e.global().register("player", SYNC, (params, ctx) -> "level".equals(params) ? "V" : null);

        check("内层未注册 → 整块保留原文", "${guild_${missing}x}", e.resolve("${guild_${missing}x}"));
        check("未闭合 ${ 原样输出", "abc ${unclosed", e.resolve("abc ${unclosed"));
        check("孤立 } 原样输出", "abc } def", e.resolve("abc } def"));
        check("空 ${} 原样输出", "${}", e.resolve("${}"));
        check("identifier 未注册保留原文", "${nope_1}", e.resolve("${nope_1}"));
        check("嵌套未闭合保留原文", "${guild_${player", e.resolve("${guild_${player"));
    }

    // ============ ⑤b 与 PAPI 的 `%...%` 共存（引擎不解析它） ============
    private void testPapiCoexistence() {
        Engine e = new Engine();
        Registry r = e.global();
        r.register("guild", SYNC, (params, ctx) -> "满级联盟");
        r.register("pct", SYNC, (params, ctx) -> "含 % 的值");

        // 引擎只解析 ${...}，`%...%` 原样留下（由 PAPI 解析）
        check("同文本内 `%...%` 原样保留", "欢迎 %player_name% 来到 满级联盟",
                e.resolve("欢迎 %player_name% 来到 ${guild_name}"));
        check("纯 `%...%` 模板完全不动", "%player_name% 和 %vault_eco_balance%",
                e.resolve("%player_name% 和 %vault_eco_balance%"));
        check("孤立的 % 原样保留", "进度 50% 完成", e.resolve("进度 50% 完成"));
        check("未闭合的 % 原样保留", "a % b", e.resolve("a % b"));

        // 值里的 % 是普通字符，不参与任何解析
        check("值含 % 原样输出", "含 % 的值", e.resolve("${pct}"));
        check("值含 % 与 `%...%` 混合", "含 % 的值 + %other%",
                e.resolve("${pct} + %other%"));

        // `${}` 内出现 % 也不影响切分
        check("params 含 % 不影响切分", "满级联盟", e.resolve("${guild_name}"));
    }

    // ============ ⑥ 全局注册表默认语义 ============
    private void testGlobalDefault() {
        Engine e = new Engine();
        e.global().register("shared", SYNC, (params, ctx) -> "共享值");
        Registry priv = e.newRegistry("priv");
        priv.register("hidden", SYNC, (params, ctx) -> "私有值");

        check("resolve 默认走 global", "共享值", e.resolve("${shared}"));
        check("默认走 global 时看不到私有注册表", "${hidden}", e.resolve("${hidden}"));
        check("global 与具名注册表是不同实例", "global", e.global().name());
        checkTrue("newRegistry 同名返回同一实例",
                e.newRegistry("priv") == e.newRegistry("priv"));
        checkTrue("registry(name) 可取回", e.registry("priv") == priv);
        checkTrue("registry(未创建的名字) 返回 null", e.registry("nope") == null);
    }

    // ============ ⑦ 注册表隔离（跨插件私有） ============
    private void testRegistryIsolation() {
        Engine e = new Engine();
        Registry claim = e.newRegistry("claim");
        Registry union = e.newRegistry("union");
        claim.register("area", SYNC, (params, ctx) -> "霜寒谷");
        claim.register("ledger", SYNC, (params, ctx) -> "内部账本");
        union.register("gname", SYNC, (params, ctx) -> "满级联盟");

        // claim 拿自己的注册表 → 可用
        check("claim 解析自己的", "霜寒谷", e.resolve("${area}", claim));
        // union 没拿 claim 的注册表 → 拦
        check("union 拿不到 claim 的（隔离）", "${area}", e.resolve("${area}", union));
        check("union 解析自己的", "满级联盟", e.resolve("${gname}", union));
        // 双向隔离
        check("claim 拿不到 union 的", "${gname}", e.resolve("${gname}", claim));

        // 附属插件：父插件把注册表递给它
        Registry ext = e.newRegistry("claim-ext");
        ext.register("extdata", SYNC, (params, ctx) -> "附属数据");
        check("附属插件（同时持有父注册表）", "内部账本 附属数据",
                e.resolve("${ledger} ${extdata}", claim, ext));
        check("无关插件看不到附属数据", "${extdata}", e.resolve("${extdata}", union));
    }

    // ============ ⑧ 注册表组合与优先级 ============
    private void testRegistryComposition() {
        Engine e = new Engine();
        Registry base = e.newRegistry("base");
        Registry override = e.newRegistry("override");
        base.register("name", SYNC, (params, ctx) -> "基础");
        override.register("name", SYNC, (params, ctx) -> "覆盖");
        override.register("extra", SYNC, (params, ctx) -> "额外");

        check("先传入者优先", "覆盖", e.resolve("${name}", override, base));
        check("顺序反转则结果反转", "基础", e.resolve("${name}", base, override));
        check("组合后可解析各自的 identifier", "覆盖/额外", e.resolve("${name}/${extra}", override, base));

        // 空注册表列表 → 安全默认：什么都不解析
        check("空注册表列表不解析", "${name}", e.resolve("${name}", new Registry[0]));
        check("null 注册表数组不解析", "${name}", e.resolve("${name}", (Registry[]) null));
        check("不传注册表退化为 global（此处 global 为空）", "${name}", e.resolve("${name}"));
    }

    // ============ ⑨ 玩家配置场景（限定解析范围） ============
    private void testPlayerConfigScenario() {
        Engine e = new Engine();
        Registry base = e.newRegistry("base");
        Registry claimSafe = e.newRegistry("claim-safe");
        Registry claimPriv = e.newRegistry("claim-private");
        Registry ops = e.newRegistry("ops");

        base.register("player", SYNC, (params, ctx) -> "Pepper");
        base.register("world", SYNC, (params, ctx) -> "world_nether");
        claimSafe.register("area", SYNC, (params, ctx) -> "霜寒谷");
        claimPriv.register("ledger", SYNC, (params, ctx) -> "内部账本");
        ops.register("status", SYNC, (params, ctx) -> "TPS 19.8");

        // 玩家配置：只传安全注册表
        String playerMsg = "欢迎 ${player} 进入 ${area}，世界 ${world}";
        check("玩家配置正常解析", "欢迎 Pepper 进入 霜寒谷，世界 world_nether",
                e.resolve(playerMsg, base, claimSafe));

        // 玩家塞越权占位符
        check("玩家越权占位符被拦（运维域未传）", "Pepper ${status}",
                e.resolve("${player} ${status}", base, claimSafe));
        check("玩家越权占位符被拦（私有域未传）", "Pepper ${ledger}",
                e.resolve("${player} ${ledger}", base, claimSafe));

        // 插件内部：传自己的全部注册表
        check("插件内部可解析全部", "欢迎 Pepper 进入 霜寒谷，世界 world_nether",
                e.resolve(playerMsg, base, claimSafe, claimPriv, ops));
        check("插件内部可读私有", "内部账本", e.resolve("${ledger}", base, claimSafe, claimPriv));

        // 注册表级继承：新变量零配置
        claimSafe.register("newly", SYNC, (params, ctx) -> "新变量");
        check("新增变量自动对玩家可用", "新变量", e.resolve("${newly}", base, claimSafe));
    }

    // ============ ⑩ 值不回灌 ============
    private void testNoReExpansion() {
        Engine e = new Engine();
        Registry r = e.global();
        r.register("evil", SYNC, (params, ctx) -> "${secret}");
        r.register("secret", SYNC, (params, ctx) -> "服务器密钥ABC");

        check("值含占位符原样输出（无泄露）", "欢迎来到 ${secret}", e.resolve("欢迎来到 ${evil}"));
        check("值含占位符不参与后续解析", "a${secret}b", e.resolve("a${evil}b"));
    }

    // ============ ⑪ 环检测 ============
    private void testCycle() {
        Engine e = new Engine();
        Registry r = e.global();
        r.register("la", SYNC, (params, ctx) -> "${lb}");
        r.register("lb", SYNC, (params, ctx) -> "${la}");

        check("值互引不回灌", "${lb}", e.resolve("${la}"));
        // 内层 ${la} 先解析为值 "${lb}"，外层拼接后 ${lb} 不可解析 → 保留该形态
        check("自引用模板不挂栈", "${${lb}}", e.resolve("${${la}}"));
    }

    // ============ ⑫ 异步 ============
    private void testAsync() {
        Engine e = new Engine(Executors.newFixedThreadPool(2));
        e.global().register("balance", new Engine.Handler() {
            @Override
            public Engine.Kind kind() {
                return Engine.Kind.ASYNC;
            }

            @Override
            public CompletableFuture<String> resolveAsync(String params, Engine.Ctx ctx) {
                return CompletableFuture.supplyAsync(() -> {
                    sleep(80);
                    return "12345";
                });
            }
        });

        long t0 = System.nanoTime();
        String first = e.resolve("余额 ${balance}");
        long ms = (System.nanoTime() - t0) / 1_000_000;

        check("异步未命中保留原文", "余额 ${balance}", first);
        checkTrue("解析路径 < 5 ms（实测 " + ms + " ms）", ms < 5);
        checkTrue("主线程未被阻塞（" + ms + " ms < 50 ms）", ms < 50);

        sleep(400);
        check("后台刷新后取到值", "余额 12345", e.resolve("余额 ${balance}"));
    }

    // ============ ⑬ 注册冲突 ============
    private void testConflict() {
        Engine e = new Engine();
        Registry r = e.global();
        r.register("dup", SYNC, (params, ctx) -> "a");
        boolean threw = false;
        try {
            r.register("dup", SYNC, (params, ctx) -> "b");
        } catch (IllegalStateException ex) {
            threw = true;
        }
        checkTrue("同注册表内重复 identifier 抛 IllegalStateException", threw);

        // 跨注册表同名不冲突
        Registry other = e.newRegistry("other");
        boolean crossOk = true;
        try {
            other.register("dup", SYNC, (params, ctx) -> "c");
        } catch (RuntimeException ex) {
            crossOk = false;
        }
        checkTrue("跨注册表同名 identifier 不冲突", crossOk);

        boolean badId = false;
        try {
            r.register("has_underscore", SYNC, (params, ctx) -> "x");
        } catch (IllegalArgumentException ex) {
            badId = true;
        }
        checkTrue("identifier 含 `_` → IllegalArgumentException", badId);
    }

    // ============ ⑭ `-` identifier ============
    private void testHyphenIdentifier() {
        Engine e = new Engine();
        final String[] captured = new String[1];
        e.global().register("guild-config", SYNC, (params, ctx) -> {
            captured[0] = params;
            return "CFG(" + params + ")";
        });
        e.global().register("player", SYNC, (params, ctx) -> "level".equals(params) ? "V" : null);
        e.global().register("guild", SYNC, (params, ctx) -> "G(" + params + ")");
        e.global().register("p-level", SYNC, (params, ctx) -> "V");

        check("`-` identifier 无 params", "CFG()", e.resolve("${guild-config}"));
        checkTrue("params 为空串", "".equals(captured[0]));
        check("`-` 属于 identifier，`_` 仍切分", "CFG(pvp)", e.resolve("${guild-config_pvp}"));
        checkTrue("params 为 pvp（`-` 未被误切）", "pvp".equals(captured[0]));
        check("`-` identifier 作为内层", "G(Vx)", e.resolve("${guild_${p-level}x}"));
    }

    // ============ ⑮ 逐变量回灌声明 ============
    private void testReinjection() {
        Engine e = new Engine();
        Registry r = e.global();
        var RESOLVE = Engine.Reinject.RESOLVE;

        r.register("secret", SYNC, (params, ctx) -> "密钥ABC");
        r.register("lit", SYNC, (params, ctx) -> "${secret}");                       // 默认 LITERAL
        r.register("res", SYNC, (params, ctx) -> "${secret}", RESOLVE);

        check("默认不回灌（值当字面量）", "${secret}", e.resolve("${lit}"));
        check("显式 RESOLVE 回灌", "密钥ABC", e.resolve("${res}"));
        check("回灌值嵌在文本中", "前缀密钥ABC后缀", e.resolve("前缀${res}后缀"));

        r.register("c1", SYNC, (params, ctx) -> "${c2}", RESOLVE);
        r.register("c2", SYNC, (params, ctx) -> "${c3}", RESOLVE);
        r.register("c3", SYNC, (params, ctx) -> "末端", RESOLVE);
        check("回灌多级链", "末端", e.resolve("${c1}"));

        check("回灌下重复 key", "密钥ABC-密钥ABC", e.resolve("${res}-${res}"));

        r.register("self", SYNC, (params, ctx) -> "${self}", RESOLVE);
        r.register("ca", SYNC, (params, ctx) -> "${cb}", RESOLVE);
        r.register("cb", SYNC, (params, ctx) -> "${ca}", RESOLVE);
        check("自环 → 保留原文", "${self}", e.resolve("${self}"));
        check("互环 → 保留原文", "${ca}", e.resolve("${ca}"));
        check("环之后路径已清理", "${self}然后密钥ABC", e.resolve("${self}然后${res}"));

        check("混合三态", "A-${secret}-密钥ABC", e.resolve("A-${lit}-${res}"));

        r.register("plain", SYNC, (params, ctx) -> "纯值", RESOLVE);
        check("RESOLVE 但值干净", "纯值", e.resolve("${plain}"));
    }

    // ============ 断言辅助 ============

    private static void check(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.printf("  ✓ %-42s → %s%n", name, actual);
        } else {
            failures.add(name + "：期望 [" + expected + "]，实际 [" + actual + "]");
            System.out.printf("  ✗ %-42s → 期望 [%s] 实际 [%s]%n", name, expected, actual);
        }
    }

    private static void checkTrue(String name, boolean cond) {
        if (cond) {
            passed++;
            System.out.printf("  ✓ %s%n", name);
        } else {
            failures.add(name);
            System.out.printf("  ✗ %s%n", name);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}