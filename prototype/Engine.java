package com.pepper.placeholder;

import java.util.*;
import java.util.concurrent.*;

/**
 * PepperPlaceholder 引擎原型（注册表模型）。
 *
 * <p><b>契约</b>：{@code ${identifier_params}} —— 首个 {@code _} 单次切分，
 * params 为不透明字符串（子分隔符由 handler 自定，与 PAPI 的 CharsReplacer 一致）。</p>
 *
 * <p><b>授权模型：注册表即能力</b>。解析时显式传入 {@link Registry} 列表，
 * 引擎只在传入的注册表里查找 —— 手里有什么注册表，就能解析什么。
 * 跨插件私有、玩家配置限定范围等需求都由「给不给注册表」这一动作表达，
 * 引擎不维护任何可见性规则。</p>
 *
 * <p>{@link #global()} 是内置的全局注册表，承载 PAPI 等价的共享语义：
 * {@code resolve(template)} 默认只走它。</p>
 *
 * <p><b>扫描</b>：栈式单次线性扫描，零回溯；嵌套深度不增加扫描次数。</p>
 */
public final class Engine {

    private static final String OPEN = "${";
    private static final char SEPARATOR = '_';

    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger("PepperPlaceholder");

    // ==================== 公共类型 ====================

    /** 占位符种类。 */
    public enum Kind { SYNC, ASYNC }

    /**
     * 变量值的可解析性声明（注册时逐变量指定）。
     *
     * <p>决定 handler 返回的字符串里若出现 {@code ${...}} 是否会被继续解析。</p>
     */
    public enum Reinject {
        /** 值当字面量输出，永不二次解析（默认；防注入）。 */
        LITERAL,
        /** 值可能含其他变量名，解析后继续展开（带环检测）。 */
        RESOLVE
    }

    /**
     * 解析上下文：引擎不解释其内容，只透传给 handler。
     *
     * <p>平台适配层用它携带平台数据（Paper 下放 {@code Player}/{@code OfflinePlayer}，
     * Velocity 下放 {@code Player}）。引擎自身不读取 {@link #platform()}。</p>
     */
    public record Ctx(Object platform) {
        /** 无平台数据的上下文。 */
        public static final Ctx NONE = new Ctx(null);
    }

    /** handler 契约：拿到不透明 params，自己解释；不支持该 params 时返回 null。 */
    public interface Handler {
        Kind kind();

        /** 同步求值；不适用返回 null。 */
        default String resolve(String params, Ctx ctx) {
            return null;
        }

        /** 异步求值；不适用返回 null。 */
        default CompletableFuture<String> resolveAsync(String params, Ctx ctx) {
            return null;
        }
    }

    /** 同步 handler 函数式接口。 */
    @FunctionalInterface
    public interface SyncFn {
        String apply(String params, Ctx ctx);
    }

    // ==================== 注册表 ====================

    private record Entry(Handler handler, Reinject reinject) {
    }

    /**
     * 注册表：一组 identifier → handler 的绑定，也是**唯一的隔离与授权单元**。
     *
     * <p>把注册表递给谁，谁就能解析它里面的占位符。附属插件、安全子集等场景
     * 都通过「递给谁」表达，引擎不做可见性判断。</p>
     */
    public static final class Registry {

        private final String name;
        private final Map<String, Entry> entries = new ConcurrentHashMap<>();

        Registry(String name) {
            this.name = name;
        }

        /** 注册表名（仅用于异步缓存 key 与调试）。 */
        public String name() {
            return name;
        }

        /** 注册同步 handler，值按字面量处理（{@link Reinject#LITERAL}）。 */
        public Registry register(String identifier, Kind kind, SyncFn fn) {
            return register(identifier, syncHandler(kind, fn), Reinject.LITERAL);
        }

        /** 注册同步 handler，并声明其值是否可能含其他变量名。 */
        public Registry register(String identifier, Kind kind, SyncFn fn, Reinject reinject) {
            return register(identifier, syncHandler(kind, fn), reinject);
        }

        /** 注册 handler，值按字面量处理。 */
        public Registry register(String identifier, Handler handler) {
            return register(identifier, handler, Reinject.LITERAL);
        }

        /**
         * 注册 handler，并声明其值是否可能含其他变量名。
         *
         * <p>identifier <b>不能含 {@code _}</b>（首个 {@code _} 即 identifier/params 切分点）；
         * 多词 identifier 用 {@code -}，例如 {@code guild-config}。</p>
         */
        public Registry register(String identifier, Handler handler, Reinject reinject) {
            Objects.requireNonNull(identifier, "identifier");
            Objects.requireNonNull(handler, "handler");
            Objects.requireNonNull(reinject, "reinject");
            if (identifier.isEmpty()) {
                throw new IllegalArgumentException("identifier must not be empty");
            }
            if (identifier.indexOf(SEPARATOR) >= 0) {
                throw new IllegalArgumentException("identifier must not contain '" + SEPARATOR
                        + "' (it is the identifier/params split point); use '-' instead: " + identifier);
            }
            if (entries.putIfAbsent(identifier, new Entry(handler, reinject)) != null) {
                throw new IllegalStateException("duplicate placeholder identifier: " + identifier);
            }
            return this;
        }

        /** 本注册表是否含该 identifier。 */
        public boolean isRegistered(String identifier) {
            return entries.containsKey(identifier);
        }

        /** 本注册表内的 identifier 快照。 */
        public Set<String> identifiers() {
            return Set.copyOf(entries.keySet());
        }

        /** 本注册表内的绑定数。 */
        public int size() {
            return entries.size();
        }

        Entry entry(String identifier) {
            return entries.get(identifier);
        }
    }

    /** 同步 handler 工厂。 */
    private static Handler syncHandler(Kind kind, SyncFn fn) {
        Objects.requireNonNull(fn, "fn");
        return new Handler() {
            @Override
            public Kind kind() {
                return kind;
            }

            @Override
            public String resolve(String params, Ctx ctx) {
                return fn.apply(params, ctx);
            }
        };
    }

    // ==================== 引擎状态 ====================

    private final Registry global = new Registry("global");
    private final Map<String, Registry> registries = new ConcurrentHashMap<>();

    private final Map<String, CachedValue> cache = new ConcurrentHashMap<>();
    private final Set<String> refreshing = ConcurrentHashMap.newKeySet();

    private final Executor asyncPool;
    private final long asyncTtlMillis;

    public Engine() {
        this(null, 30_000L);
    }

    public Engine(Executor asyncPool) {
        this(asyncPool, 30_000L);
    }

    public Engine(Executor asyncPool, long asyncTtlMillis) {
        this.asyncPool = asyncPool != null ? asyncPool : Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "pepper-placeholder-async");
            t.setDaemon(true);
            return t;
        });
        this.asyncTtlMillis = asyncTtlMillis;
    }

    /** 内置全局注册表：{@code resolve(template)} 的默认查找范围（PAPI 等价的共享语义）。 */
    public Registry global() {
        return global;
    }

    /**
     * 取得（必要时创建）具名注册表。
     *
     * <p>同名重复调用返回同一实例，便于跨调用点共享。</p>
     */
    public Registry newRegistry(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("registry name must not be empty");
        }
        return registries.computeIfAbsent(name, Registry::new);
    }

    /** 已创建的具名注册表（不含 {@link #global()}）。 */
    public Registry registry(String name) {
        return registries.get(name);
    }

    /** 清空异步缓存。 */
    public void clearCache() {
        cache.clear();
    }

    // ==================== 解析 ====================

    /** 在全局注册表中解析。 */
    public String resolve(String template) {
        return resolve(template, Ctx.NONE, global);
    }

    /** 在全局注册表中解析，携带平台上下文。 */
    public String resolve(String template, Ctx ctx) {
        return resolve(template, ctx, global);
    }

    /** 在给定的注册表中解析（不传注册表则只走全局）。 */
    public String resolve(String template, Registry... regs) {
        return resolve(template, Ctx.NONE, regs);
    }

    /**
     * 在给定的注册表中解析，携带平台上下文。
     *
     * <p>查找按传入顺序进行，<b>先传入者优先</b>（可用前置注册表做覆盖）。</p>
     *
     * @param regs 可用的注册表；为 {@code null} 或空表示不解析任何占位符（安全默认）
     */
    public String resolve(String template, Ctx ctx, Registry... regs) {
        if (template == null) {
            return null;
        }
        if (regs == null || regs.length == 0 || template.indexOf(OPEN) < 0) {
            return template;
        }
        return scan(template, ctx == null ? Ctx.NONE : ctx, regs, null);
    }

    /** 扫描帧：一个未闭合的 `${...}` 内累积的 identifier 与 params。 */
    private static final class Frame {
        final StringBuilder id = new StringBuilder(16);
        StringBuilder params;            // 惰性分配：遇首个 `_` 才建
        StringBuilder cur = id;          // 当前写入目标（先 id，切分后转 params）
        boolean hasSeparator;
    }

    /**
     * 栈式单次扫描。
     *
     * <p>遇 {@code ${} 压帧、遇首个 {@code _} 切段、遇 {@code }} 弹帧求值并把结果写回上层。
     * 内层弹帧时结果立即可用，故嵌套深度不增加扫描次数。</p>
     *
     * @param path 回灌路径（环检测）；{@code null} 表示当前尚未进入任何回灌链
     */
    private String scan(String text, Ctx ctx, Registry[] regs, Deque<String> path) {
        int open = text.indexOf(OPEN);
        if (open < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + 32);
        Deque<Frame> frames = null;                  // 惰性创建：无嵌套时不分配
        int pos = 0;
        int i = open;

        while (i >= 0) {
            out.append(text, pos, i);
            Frame root = new Frame();
            boolean closed = false;
            int j = i + OPEN.length();

            while (j < text.length()) {
                if (text.startsWith(OPEN, j)) {       // 内层 `${` → 压帧
                    if (frames == null) {
                        frames = new ArrayDeque<>(4);
                    }
                    frames.push(root);
                    root = new Frame();
                    j += OPEN.length();
                    continue;
                }
                char c = text.charAt(j);
                if (c == '}') {                       // 弹帧
                    String value = evaluate(root, ctx, regs, path);
                    if (frames == null || frames.isEmpty()) {
                        out.append(value != null ? value : rebuild(root));
                        pos = j + 1;
                        closed = true;
                        break;
                    }
                    if (value == null) {              // 内层不可解析 → 整块保留原文
                        out.append(text, i, j + 1);
                        pos = j + 1;
                        closed = true;
                        break;
                    }
                    root = frames.pop();
                    root.cur.append(value);           // 结果立即写入上层
                    j++;
                    continue;
                }
                if (c == SEPARATOR && !root.hasSeparator) {
                    root.hasSeparator = true;
                    if (root.params == null) {
                        root.params = new StringBuilder(16);   // 惰性：此刻才分配
                    }
                    root.cur = root.params;
                } else {
                    root.cur.append(c);
                }
                j++;
            }

            if (!closed) {                            // 未闭合 → 原样输出剩余
                out.append(text, i, text.length());
                return out.toString();
            }
            i = text.indexOf(OPEN, pos);
        }
        out.append(text, pos, text.length());
        return out.toString();
    }

    /**
     * 求值一个完成的帧。
     *
     * <p>params 内的 {@code ${...}} 在此刻已由栈扫描展开（内层弹帧时写回本帧），
     * 因此 handler 拿到的是**已解析的 params**。</p>
     *
     * <p>返回值若含 {@code ${} 且该变量声明为 {@link Reinject#RESOLVE}，则递归展开；
     * 路径 {@code path} 用于环检测（进入时 push、离开时 pop）。</p>
     */
    private String evaluate(Frame f, Ctx ctx, Registry[] regs, Deque<String> path) {
        String identifier = f.id.toString();
        if (identifier.isEmpty()) {
            return null;                              // `${}` → 保留原文
        }

        Entry entry = null;
        String owner = null;
        for (Registry r : regs) {
            Entry e = r.entry(identifier);
            if (e != null) {
                entry = e;
                owner = r.name();
                break;                                // 先传入者优先
            }
        }
        if (entry == null) {
            return null;                              // 未在任何传入注册表中 → 保留原文
        }

        String params = f.hasSeparator ? f.params.toString() : "";
        if (params.indexOf(OPEN) >= 0) {
            return null;                              // params 未收敛（内层未解析）→ 保留原文
        }

        String value = entry.handler().kind() == Kind.ASYNC
                ? cachedOrSchedule(owner, identifier, params, entry, ctx)
                : entry.handler().resolve(params, ctx);

        // 逐变量声明：只有 RESOLVE 的变量其值才继续展开
        if (value == null
                || entry.reinject() != Reinject.RESOLVE
                || value.indexOf(OPEN) < 0) {
            return value;
        }

        String key = f.hasSeparator ? identifier + SEPARATOR + params : identifier;
        Deque<String> chain = path;
        if (chain == null) {
            chain = new ArrayDeque<>(4);              // 首层回灌才分配
        } else if (chain.contains(key)) {
            return null;                              // 成环 → 保留原文
        }
        chain.push(key);
        try {
            return scan(value, ctx, regs, chain);
        } finally {
            chain.pop();                              // 离开时移除，保证路径干净
        }
    }

    /** 重建未解析块的原文字面量。 */
    private static String rebuild(Frame f) {
        StringBuilder sb = new StringBuilder(32).append(OPEN).append(f.id);
        if (f.hasSeparator) {
            sb.append(SEPARATOR).append(f.params);
        }
        return sb.append('}').toString();
    }

    // ==================== 异步 ====================

    private record CachedValue(String value, long expiresAt) {
    }

    /**
     * 异步：只读缓存；未命中返回 null（保留原文），后台刷新。主线程绝不阻塞。
     *
     * <p>缓存 key 含注册表名 —— 不同注册表可有同名 identifier，不含则串味。</p>
     */
    private String cachedOrSchedule(String owner, String identifier, String params,
                                    Entry entry, Ctx ctx) {
        String key = owner + ':' + identifier + SEPARATOR + params;
        CachedValue cv = cache.get(key);
        long now = System.currentTimeMillis();
        if (cv != null && cv.expiresAt() > now) {
            return cv.value();
        }
        if (refreshing.add(key)) {
            CompletableFuture<String> future;
            try {
                future = entry.handler().resolveAsync(params, ctx);
            } catch (RuntimeException ex) {
                refreshing.remove(key);
                LOG.warning("async placeholder failed: " + key + " — " + ex);
                return cv != null ? cv.value() : null;
            }
            if (future == null) {
                refreshing.remove(key);
                return cv != null ? cv.value() : null;
            }
            future.orTimeout(2, TimeUnit.SECONDS).whenComplete((value, error) -> {
                try {
                    if (error != null) {
                        LOG.warning("async placeholder failed: " + key + " — " + error);
                    } else if (value != null) {
                        cache.put(key, new CachedValue(value, System.currentTimeMillis() + asyncTtlMillis));
                    }
                } finally {
                    refreshing.remove(key);
                }
            });
        }
        return cv != null ? cv.value() : null;
    }
}