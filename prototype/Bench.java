import com.pepper.placeholder.Engine;
import com.pepper.placeholder.Engine.Registry;

import java.util.function.Supplier;

import static com.pepper.placeholder.Engine.Kind.SYNC;

/**
 * spec 验收标准 3 的性能基准（注册表模型）。
 *
 * <p>用法：{@code java Bench <场景>} 单场景运行（独立 JVM，避免各场景互相干扰 JIT）；
 * 不带参数则跑全表。</p>
 */
public final class Bench {
    static final int ITER = 120_000;

    public static void main(String[] args) {
        Engine e = new Engine();
        Registry global = e.global();
        Registry extra = e.newRegistry("extra");
        Registry last = e.newRegistry("last");

        global.register("guild", SYNC, (params, ctx) -> switch (params) {
            case "name" -> "Pepper联盟";
            case "config_pvp" -> "配置[pvp]=on";
            case "Vx" -> "G1";
            default -> null;
        });
        global.register("player", SYNC, (params, ctx) -> "level".equals(params) ? "V" : null);
        global.register("lit", SYNC, (params, ctx) -> "${player_level}");
        global.register("res", SYNC, (params, ctx) -> "${player_level}", Engine.Reinject.RESOLVE);
        extra.register("mid", SYNC, (params, ctx) -> "MID");
        last.register("far", SYNC, (params, ctx) -> "FAR");
        // 公平对照：同一 handler 与模板，只变注册表数量
        global.register("k", SYNC, (params, ctx) -> "K");
        Registry r2 = e.newRegistry("r2");
        Registry r3 = e.newRegistry("r3");
        Registry r4 = e.newRegistry("r4");
        r4.register("deep", SYNC, (params, ctx) -> "DEEP");

        if (args.length > 0) {
            String which = args[0];
            long v = switch (which) {
                case "plain" -> ns(() -> e.resolve("欢迎来到 PepperCraft 服务器（无占位符）"));
                case "static" -> ns(() -> e.resolve("联盟 ${guild_name} 等级 ${player_level}"));
                case "nested" -> ns(() -> e.resolve("联盟 ${guild_${player_level}x}"));
                case "params" -> ns(() -> e.resolve("设置 ${guild_config_pvp}"));
                case "reinject" -> ns(() -> e.resolve("等级 ${res}"));
                case "multi-hit" -> ns(() -> e.resolve("联盟 ${guild_name}", global, extra, last));
                case "multi-far" -> ns(() -> e.resolve("联盟 ${far}", global, extra, last));
                case "multi-miss" -> ns(() -> e.resolve("联盟 ${nobody}", global, extra, last));
                case "explicit" -> ns(() -> e.resolve("联盟 ${guild_name}", global));
                // 公平对照：同一 handler/模板，只变注册表数量
                case "reg1" -> ns(() -> e.resolve("X ${k}", global));
                case "reg2" -> ns(() -> e.resolve("X ${k}", global, r2));
                case "reg4" -> ns(() -> e.resolve("X ${k}", global, r2, r3, r4));
                case "deep4" -> ns(() -> e.resolve("X ${deep}", global, r2, r3, r4));
                case "miss4" -> ns(() -> e.resolve("X ${zzz}", global, r2, r3, r4));
                case "miss1" -> ns(() -> e.resolve("X ${zzz}", global));
                default -> -1;
            };
            System.out.printf("%s = %d ns%n", which, v);
            return;
        }

        System.out.printf("%-46s %9s %s%n", "场景", "ns/op", "验收线");
        System.out.println("-".repeat(74));
        row("① 无占位符（预检直接返回）", ns(() -> e.resolve("欢迎来到 PepperCraft 服务器（无占位符）")), "—");
        row("② 静态占位符 ×2（默认 global）", ns(() -> e.resolve("联盟 ${guild_name} 等级 ${player_level}")), "≤ 200");
        row("③ 动态 key（params 预解析）", ns(() -> e.resolve("联盟 ${guild_${player_level}x}")), "≤ 500");
        row("④ 参数式（不透明 params）", ns(() -> e.resolve("设置 ${guild_config_pvp}")), "—");
        row("⑤ 回灌（RESOLVE 值含变量名）", ns(() -> e.resolve("等级 ${res}")), "—");
        row("⑥ 显式传注册表", ns(() -> e.resolve("联盟 ${guild_name}", global)), "—");
        row("⑦ 多注册表·目标在第 1 个", ns(() -> e.resolve("联盟 ${guild_name}", global, extra, last)), "—");
        row("⑧ 多注册表·目标在第 3 个", ns(() -> e.resolve("联盟 ${far}", global, extra, last)), "—");
        row("⑨ 多注册表·全未命中", ns(() -> e.resolve("联盟 ${nobody}", global, extra, last)), "—");
        System.out.println("-".repeat(74));
        System.out.printf("%-46s %9d %s%n", "参照：MiniPlaceholders deserialize", 3637, "基线");
        System.out.printf("%-46s %9d %s%n", "参照：PAPI 纯字符串替换", 192, "对照");
    }

    static void row(String name, long value, String limit) {
        System.out.printf("%-46s %9d %s%n", name, value, limit);
    }

    static long ns(Supplier<?> fn) {
        for (int i = 0; i < 20_000; i++) {
            fn.get();
        }
        long best = Long.MAX_VALUE;
        for (int round = 0; round < 5; round++) {
            long start = System.nanoTime();
            for (int i = 0; i < ITER; i++) {
                fn.get();
            }
            best = Math.min(best, (System.nanoTime() - start) / ITER);
        }
        return best;
    }
}