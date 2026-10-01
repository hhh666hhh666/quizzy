#!/usr/bin/env bash
# 校验「版本号只有一个真相源」：git tag 与三处手写点是否一致（ADR 0015）。
# 用作 CI 门禁（推 tag 时跑，见 .github/workflows/ci.yml 的「版本 · tag 与三处对齐」job）
# 与本地提交前自检。
#
#   bash scripts/check-version.sh            # 期望版本取自 HEAD 上的 tag，没有就跳过
#   bash scripts/check-version.sh v1.1.0     # 显式指定（CI 传 github.ref_name）
#
# 为什么不要求「一直相等」：HEAD 通常领先 tag 好几个提交，那时标示的版本号仍是最近那个
# 已发布版本，属于**正常**状态。所以只在打 tag 的那一刻比对，平时不报——误报会让人养成
# 「红着也照合」的习惯，那比不检查更糟（与 scripts/check-doc-links.sh 同一个取舍）。
#
# ⚠️ 本文件必须是 LF 换行，CRLF 会让 bash 报 bad interpreter（见 .gitattributes）。
# ⚠️ 提取版本一律用 sed 按缩进定位，不引 XML/JSON 解析器：这是要跑在 CI 上的门禁，
#    少一个依赖就少一处失败可能。各文件的缩进是有意义的定位依据，别顺手改成更宽松的模式。
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."

RAW="${1:-}"
if [ -z "$RAW" ]; then
  RAW="$(git tag --points-at HEAD 2>/dev/null | grep -E '^v[0-9]' | head -1 || true)"
fi

if [ -z "$RAW" ]; then
  echo "==> 当前提交不在任何 v* tag 上，跳过版本号比对"
  exit 0
fi

# git tag 带 v 前缀（v1.1.0）；包版本与镜像 tag 不带（1.1.0）
EXPECT="${RAW#v}"
FAILED=0

check() {
  local what="$1" actual="$2"
  if [ -z "$actual" ]; then
    printf '  [FAIL] %s：取不到值（sed 模式没匹配上？）\n' "$what" >&2
    FAILED=$((FAILED + 1))
  elif [ "$actual" = "$EXPECT" ]; then
    printf '  [ok]   %s = %s\n' "$what" "$actual"
  else
    printf '  [FAIL] %s：期望 %s，实际 %s\n' "$what" "$EXPECT" "$actual" >&2
    FAILED=$((FAILED + 1))
  fi
}

echo "==> 校验 $RAW 与三处手写点（期望版本 $EXPECT）"

# 1. 前端包版本
check 'quizzy-web/package.json' \
  "$(sed -n 's/^  "version": "\(.*\)",$/\1/p' quizzy-web/package.json | tr -d '\r')"

# 2. 后端包版本。只认 4 空格缩进那一个 —— parent 是 8 空格、依赖里是 12 空格。
#    保留 -SNAPSHOT 是刻意的（见 pom.xml 里的注释），比对前剥掉。
POM_VERSION="$(sed -n 's/^    <version>\(.*\)<\/version>$/\1/p' quizzy-server/pom.xml | tr -d '\r')"
check 'quizzy-server/pom.xml' "${POM_VERSION%-SNAPSHOT}"

# 3. compose 里两个镜像 tag（它们必须是字面量，理由见 ADR 0015）
check 'docker-compose.prod.yml (server)' \
  "$(sed -n 's/^ *image: quizzy-server:\(.*\)$/\1/p' docker-compose.prod.yml | tr -d '\r')"
check 'docker-compose.prod.yml (web)' \
  "$(sed -n 's/^ *image: quizzy-web:\(.*\)$/\1/p' docker-compose.prod.yml | tr -d '\r')"

# 4. CHANGELOG 里有没有对应的版本段 —— 打 tag 前它该从 [Unreleased] 固化下来了。
#    点号在正则里要转义，否则 1.1.0 会连 1x1y0 也算命中。
if grep -qE "^## \[${EXPECT//./\\.}\]" CHANGELOG.md; then
  printf '  [ok]   CHANGELOG.md 有 ## [%s] 段\n' "$EXPECT"
else
  printf '  [FAIL] CHANGELOG.md 里没有 ## [%s] 段：打 tag 前先把 [Unreleased] 固化成版本段\n' "$EXPECT" >&2
  FAILED=$((FAILED + 1))
fi

echo ""
if [ "$FAILED" -gt 0 ]; then
  echo "==> ${FAILED} 处与 $RAW 不一致，先对齐再打 tag" >&2
  exit 1
fi

echo "==> 通过：四处与 $RAW 一致"
