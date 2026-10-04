#!/usr/bin/env bash
# 断言「每个带 Controller 的业务模块都配了测试类」——ADR 0021 里那条机械守卫。
#
#   bash scripts/check-module-tests.sh
#
# 为什么要有它：靠自觉的测试纪律活不下来。这个项目里活下来的约定（文档链接自检、
# 版本号对齐、types 逐字一致）**都因为「有脚本兜着」**。这条守卫防的是「新加了模块、忘了配测试」，
# 以及「有人把某个模块的测试删了」。
#
# ⚠️ 局限写在明处：它**只验「有没有」，验不了「断言到没到点子上」**。
#    它是防遗忘的兜底，不是质量保证——质量仍由评审与《测试系统说明》里那张
#    「改动 → 补哪层」的表兜。别指望它替人想清楚该测什么。
#
# ⚠️ 本文件必须是 LF 换行，CRLF 会让 bash 报 bad interpreter（见 .gitattributes）。
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."

MAIN_ROOT="quizzy-server/src/main/java/com/quizzy/module"
TEST_ROOT="quizzy-server/src/test/java/com/quizzy/module"

[ -d "$MAIN_ROOT" ] || { echo "找不到 $MAIN_ROOT" >&2; exit 1; }

MISSING=0

for module_dir in "$MAIN_ROOT"/*/; do
  module="$(basename "$module_dir")"

  # 只盯有 Controller 的模块：纯支撑性包（没有对外接口）不该被这条要求约束。
  [ -d "$module_dir/controller" ] || continue

  # 模块自己那棵测试树下（含子目录）有没有任何测试类。
  # 单元测试（*Test）与接口测试（*IT）都算——两层都行，只要求「有人管这个模块」。
  if find "$TEST_ROOT/$module" \( -name '*Test.java' -o -name '*IT.java' \) -print 2>/dev/null | grep -q .; then
    echo "  ok   $module"
  else
    echo "MISSING: 模块 $module 有 Controller，但一个测试类都没有（*Test.java / *IT.java 都没找到）" >&2
    MISSING=$((MISSING + 1))
  fi
done

if [ "$MISSING" -gt 0 ]; then
  echo "" >&2
  echo "==> ${MISSING} 个模块缺测试。该补哪一层看 docs/testing/系统说明.md 的「改动 → 补哪层」。" >&2
  exit 1
fi

echo "==> 通过：每个带 Controller 的模块都至少有一个测试类"
