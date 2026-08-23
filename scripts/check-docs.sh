#!/usr/bin/env bash
# 文档重写验收测试：
#   1. docs 内不得存在占位符（懒得截图/未完待续/TODO 等）
#   2. 文档内的相对 Markdown 链接必须指向存在的文件
#   3. 不得残留旧仓库引用（jlxnb）
#   4. 停用文件必须已删除
set -u
cd "$(dirname "$0")/.."

fail=0
docs_files=$(find docs -name '*.md' | sort)

echo "== 1. 占位符扫描 =="
if grep -rnE '懒得截图|未完待续|TODO|FIXME|待补充|待完善' $docs_files; then
  echo "FAIL: 存在占位符"; fail=1
else
  echo "PASS: 无占位符"
fi

echo "== 2. 相对链接完整性 =="
broken=0
for f in $docs_files; do
  dir=$(dirname "$f")
  # 匹配 markdown 链接 [text](path) 且 path 不以 http/ 开头
  while IFS= read -r link; do
    [ -z "$link" ] && continue
    target="$dir/$link"
    if [ ! -e "$target" ] && [ ! -e "$link" ]; then
      echo "broken: $f -> $link"; broken=1
    fi
  done < <(grep -oE '\]\(([^)]+)\)' "$f" | sed -E 's/^\]\(//; s/\)$//' | grep -vE '^(http|#)' | grep -E '\.md$')
done
if [ "$broken" -eq 1 ]; then echo "FAIL: 存在断裂链接"; fail=1; else echo "PASS: 链接完整"; fi

echo "== 3. 旧仓库引用 =="
if grep -rn 'jlxnb' .vitepress/config.mts docs/ README.md 2>/dev/null; then
  echo "FAIL: 仍引用 jlxnb"; fail=1
else
  echo "PASS: 无 jlxnb 引用"
fi

echo "== 4. 停用文件已删除 =="
if ls docs/towns/*.disabled 2>/dev/null; then
  echo "FAIL: 停用文件仍存在"; fail=1
else
  echo "PASS: 停用文件已删除"
fi

[ "$fail" -eq 0 ] && echo "ALL PASS" || echo "HAS FAILURES"
exit $fail
