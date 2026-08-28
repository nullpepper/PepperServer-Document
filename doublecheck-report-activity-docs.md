# Doublecheck report

> Verdict: **green**

## Spec
- Goal: 把联盟活跃度机制完整、准确地补充到 PepperCraft 文档站（PepperServer-Document）：以 docs/towns/Advancements.md 为主体写成完整机制页，并同步修正 quickref.md 与 GuildClaims.md 的相关表述。
- Scope: 改动三个文件：docs/towns/Advancements.md（重写为完整机制页：五通道来源表含速率与每日上限、防滥用规则、7 日滚动衰减、档位阈值表、领地之心档位效果表、查看指令、与联盟等级关系）；docs/quickref.md（「联盟与领地」表补 /gclaim prosperity、/gclaim top 两行）；docs/towns/GuildClaims.md（「繁荣度与领地之心」节更新活跃度来源表述，含基线/接待通道）。其余页面与仓库不动。
- Acceptance criteria: ① Advancements.md 覆盖全部五通道（基线/生产/狩猎/驻留/接待）及各自速率与每日上限，数值与 ProsperitySettings.defaults() 一致（基线 0.5/分×每人10、生产 10 块+1×每盟30、狩猎 5 只+1×每盟30、驻留 1/分×每人20/每盟60、接待 5/访客×每盟30）；② 覆盖防滥用规则（AFK 5 分钟、领地限定、击杀归属、访客去重）与 7 日滚动衰减、降档不没收；③ 档位阈值 200/450/800/1200 及沉眠，领地之心效果槽/范围/光柱与 HeartPolicy 默认一致；④ 指令名与代码一致（/gclaim prosperity、/gclaim top、/gclaim heart register|unregister|effect|info|public on/off）；⑤ quickref.md 与 GuildClaims.md 完成同步修正；⑥ 在 PepperServer-Document 运行 npm test（check-docs.sh）全部 PASS（无占位符、相对链接完整、锚点完整）。
- Failure modes: 数值或指令与实现不符 → 全部以 PepperClaim 代码（ProsperitySettings/ClaimCommandRegistry/HeartPolicy）为唯一事实源核对后再落笔；链接/锚点断裂或占位符 → check-docs.sh 与 check-anchors.mjs 扫描失败即修复；档位效果表与实现不一致 → 以 HeartPolicy/ProsperitySettings 默认值为准。
- Priorities: 准确性优先于篇幅：写死的数值必须与代码默认值一致；保持玩家向语气，不引入内部实现术语；内容完整度（用户选定「完整机制页」）优先于页面简洁；数值不标注「可调」（用户选定「直接写死不提示」）。
- Non-goals: 不写入内部机制（activity-mode LOCAL/REPORT、batchId 幂等、SQL cap 截断、分钟级 flush 等）；不修改其他功能页面；不做多语言版本；不动 PepperCraftWeb；不改服务器配置或插件代码。

## Test evidence
- failing runs: 0
- passing runs: 1

- [spec] 把联盟活跃度机制完整、准确地补充到 PepperCraft 文档站（PepperServer-Document）：以 docs/towns/Advancements.md 为主体写成完整机制页，并同步修正 quickref.md 与 Gui…
- [green] bash scripts/check-activity-docs.sh && echo "---" && npm test

## Adversary review
No adversary review ran for this session.

## Verification
Not run.

## Delivery
- implementation edits: 7
