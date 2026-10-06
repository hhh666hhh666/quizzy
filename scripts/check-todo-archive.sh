#!/usr/bin/env bash
# 断言「已结案的待办都已归档」，用作 CI 门禁与本地提交前自检。
#
#   bash scripts/check-todo-archive.sh
#
# 规则（见 docs/todo/archive/README.md 与 docs/adr/0025-archive-resolved-todos.md）：
# 一条待办的 `状态` 变成 `已解决` 之后，必须 git mv 进 docs/todo/archive/，
# 并从 docs/todo/README.md 的主索引表移到「已归档」区。
# 这条规则此前只靠人记——而「规则写了却没人执行」正是本项目反复踩的坑
# （worklog 的「惰性归档」就是一例：上线后一次都没跑过），所以把它变成机械守卫。
#
# 查两类：
#   1. 主索引表（「## 索引」到下一个「## 」标题之间）里不得还留着「已解决」的行；
#   2. docs/todo/ 下（不含 archive/）的待办文件头不得仍是「已解决」。
#
# ⚠️ 本文件必须是 LF 换行，CRLF 会让 bash 报 bad interpreter（见 .gitattributes）。
# ⚠️ 扫的是工作区路径，不依赖 git（与 check-doc-links.sh 不同，那个要 git ls-files）。
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."

TODO_DIR='docs/todo'
INDEX="$TODO_DIR/README.md"
BROKEN=0

report() {
  printf 'BROKEN: %s\n' "$1" >&2
  BROKEN=$((BROKEN + 1))
}

# ---- 1. 主索引表里不得还留着「已解决」的行 ----
echo "==> 检查主索引表里有没有漏归档的「已解决」行"
if [ -f "$INDEX" ]; then
  while IFS= read -r hit; do
    if [ -n "$hit" ]; then
      report "$INDEX 主索引表仍有「已解决」：第 ${hit%%:*} 行 → ${hit#*: }"
    fi
  done < <(
    awk '
      /^## / { in_index = ($0 ~ /^##[[:space:]]*索引/) }
      in_index && /已解决/ { printf "%d: %s\n", FNR, $0 }
    ' "$INDEX"
  )
else
  report "找不到 $INDEX"
fi

# ---- 2. docs/todo/ 下的待办文件（不含 archive/）头部不得仍是「已解决」 ----
echo "==> 检查 $TODO_DIR/ 下有没有状态仍为「已解决」的待办"
for f in "$TODO_DIR"/*.md; do
  [ -e "$f" ] || continue
  if [ "$f" = "$INDEX" ]; then continue; fi
  # 只看头 5 行：状态写在文件头那一行，正文里引用「已解决」三个字不该被误伤。
  header="$(sed -n '1,5p' "$f")"
  case "$header" in
    *"状态：已解决"*)
      report "$f 状态是「已解决」——应 git mv 进 $TODO_DIR/archive/（见 $TODO_DIR/archive/README.md）"
      ;;
  esac
done

# ---- 3. 结论 ----
if [ "$BROKEN" -gt 0 ]; then
  echo "" >&2
  echo "==> 发现 ${BROKEN} 处「已结案却没归档」，按 $TODO_DIR/archive/README.md 归档后重试" >&2
  exit 1
fi

echo "==> 通过：已结案的待办都已归档"
