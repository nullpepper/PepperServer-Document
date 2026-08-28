#!/usr/bin/env bash
# 活跃度机制文档内容验收（doublecheck-spec：活跃度机制补充到 PepperCraft docs）。
# 断言三处页面包含规格要求的机制内容；任一缺失即失败。
set -u
cd "$(dirname "$0")/.."

fail=0

check() { # <file> <fixed-string> <label>
  if grep -qF -- "$2" "$1"; then
    echo "PASS: $3"
  else
    echo "FAIL: $3（$1 缺少：$2）"
    fail=1
  fi
}

# --- Advancements.md：五通道速率与上限 ---
check docs/towns/Advancements.md '每分钟 +0.5' '基线通道速率'
check docs/towns/Advancements.md '每 10 个方块 +1' '生产通道速率'
check docs/towns/Advancements.md '每 5 只 +1' '狩猎通道速率'
check docs/towns/Advancements.md '每分钟 +1' '驻留通道速率'
check docs/towns/Advancements.md '每位访客 +5' '接待通道速率'
check docs/towns/Advancements.md '每人 20 / 每盟 60' '驻留双上限'
# --- Advancements.md：衰减 / 防滥用 / 档位 / 指令 ---
check docs/towns/Advancements.md '7 日滚动窗口' '7 日滚动衰减'
check docs/towns/Advancements.md 'AFK 判定' '防滥用-AFK'
check docs/towns/Advancements.md '| **铜锭** | 铜锭 | 200 |' '档位-铜锭 200'
check docs/towns/Advancements.md '| **铁锭** | 铁锭 | 450 |' '档位-铁锭 450'
check docs/towns/Advancements.md '| **金锭** | 金锭 | 800 |' '档位-金锭 800'
check docs/towns/Advancements.md '| **下界之星** | 下界之星 | 1200 |' '档位-下界之星 1200'
check docs/towns/Advancements.md '/gclaim prosperity' '查看指令 prosperity'
check docs/towns/Advancements.md '/gclaim top' '查看指令 top'
check docs/towns/Advancements.md '/gclaim heart public on/off' '领地之心指令 public'
# --- quickref.md：速查表同步 ---
check docs/quickref.md '/gclaim prosperity' 'quickref 补 prosperity'
check docs/quickref.md '/gclaim top' 'quickref 补 top'
# --- GuildClaims.md：来源表述同步（含基线/接待） ---
check docs/towns/GuildClaims.md '任意位置在线的基线' 'GuildClaims 补基线'
check docs/towns/GuildClaims.md '接待' 'GuildClaims 补接待'

[ "$fail" -eq 0 ] && echo "ALL PASS" || echo "HAS FAILURES"
exit $fail
