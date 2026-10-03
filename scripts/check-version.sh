#!/usr/bin/env bash
# 校验「版本号只有一个真相源」：git tag 与三处手写点一致，compose 的 image 用的是变量，
# 且 package-lock.json 没有与 package.json 漂开（ADR 0015 及其 Amendment 1、2）。
# 用作 CI 门禁（推 tag 时跑，见 .github/workflows/release.yml 的「版本」job）与本地提交前自检。
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

echo "==> 校验 $RAW 与三处手写点 + compose 结构（期望版本 $EXPECT）"

# 1. 前端包版本（手写点之一）
PKG_VERSION="$(sed -n 's/^  "version": "\(.*\)",$/\1/p' quizzy-web/package.json | head -1 | tr -d '\r')"
check 'quizzy-web/package.json' "$PKG_VERSION"

# 1b. package-lock.json 必须与 package.json 一致 —— 这是**派生一致性断言**，不是第三个手写点。
#     真相源是 package.json，lockfile 只是它的快照，所以比的是「两者相等」而不是「也等于 tag」。
#     补这条是因为它曾经是盲区：只改 package.json 时，lockfile 里那份会静默留在旧值；
#     而下一次 `npm install` 又会把它悄悄刷回来，凭空多出一个与发版无关的改动。
#     只取顶层那一处即可——`packages[""]` 里那份是同一个值（缩进 6 空格，不会被这个模式命中）。
LOCK_VERSION="$(sed -n 's/^  "version": "\(.*\)",$/\1/p' quizzy-web/package-lock.json | head -1 | tr -d '\r')"
if [ -n "$PKG_VERSION" ] && [ "$LOCK_VERSION" = "$PKG_VERSION" ]; then
  printf '  [ok]   quizzy-web/package-lock.json = %s（与 package.json 一致）\n' "$LOCK_VERSION"
else
  printf '  [FAIL] quizzy-web/package-lock.json：取到「%s」，而 package.json 是「%s」——派生文件必须一致\n' \
    "${LOCK_VERSION:-空}" "${PKG_VERSION:-空}" >&2
  FAILED=$((FAILED + 1))
fi

# 1c. 移动端包版本（手写点之二）。移动端目前没有版本显示 UI，这个手写点是
#     为了「将来接上注入通道」提前对齐——不写它就会立刻开始漂（ADR 0015 Amendment 2）。
MOBILE_PKG_VERSION="$(sed -n 's/^  "version": "\(.*\)",$/\1/p' quizzy-mobile/package.json | head -1 | tr -d '\r')"
check 'quizzy-mobile/package.json' "$MOBILE_PKG_VERSION"

# 1d. 移动端 lock 与 package.json 的派生一致性断言（同 1b）。
MOBILE_LOCK_VERSION="$(sed -n 's/^  "version": "\(.*\)",$/\1/p' quizzy-mobile/package-lock.json | head -1 | tr -d '\r')"
if [ -n "$MOBILE_PKG_VERSION" ] && [ "$MOBILE_LOCK_VERSION" = "$MOBILE_PKG_VERSION" ]; then
  printf '  [ok]   quizzy-mobile/package-lock.json = %s（与 package.json 一致）\n' "$MOBILE_LOCK_VERSION"
else
  printf '  [FAIL] quizzy-mobile/package-lock.json：取到「%s」，而 package.json 是「%s」——派生文件必须一致\n' \
    "${MOBILE_LOCK_VERSION:-空}" "${MOBILE_PKG_VERSION:-空}" >&2
  FAILED=$((FAILED + 1))
fi

# 2. 后端包版本。只认 4 空格缩进那一个 —— parent 是 8 空格、依赖里是 12 空格。
#    保留 -SNAPSHOT 是刻意的（见 pom.xml 里的注释），比对前剥掉。
POM_VERSION="$(sed -n 's/^    <version>\(.*\)<\/version>$/\1/p' quizzy-server/pom.xml | tr -d '\r')"
check 'quizzy-server/pom.xml' "${POM_VERSION%-SNAPSHOT}"

# 3. compose 里两个 image 必须是**变量**，不能写死版本 tag（ADR 0015 Amendment 1）。
#    云上镜像来自 ACR、tag 由部署时传入；写死会毁掉「版本 tag 即回滚落点」这件事。
#    这里做结构断言而不是比值 —— 量变了：不再是「三处都比对值」，而是「两处比值 + 这里比形状」。
#    ⚠️ ${APP_VERSION} 要按字面量匹配，所以用单引号拼串，别整段改成双引号。
#    ⚠️ 放行 ${APP_VERSION:?提示语} 这种必填写法（compose 支持 `:?` 带错误信息），
#       否则「加了提示语」会被误判成「写死了 tag」。
#    移动端本轮没有镜像与 compose 服务，所以这里仍只有 server 与 web 两个。
check_compose_image() {
  local svc="$1"
  local pattern='^[[:space:]]*image:.*/quizzy-'"$svc"':\$\{APP_VERSION(:[^}]*)?\}[[:space:]]*$'
  if grep -qE "$pattern" docker-compose.prod.yml; then
    printf '  [ok]   %s\n' "docker-compose.prod.yml (${svc}) 的 image 用的是 \${APP_VERSION}"
  else
    printf '  [FAIL] docker-compose.prod.yml (%s)：image 必须写成 .../quizzy-%s:${APP_VERSION}，不要写死版本 tag\n' \
      "$svc" "$svc" >&2
    FAILED=$((FAILED + 1))
  fi
}
check_compose_image server
check_compose_image web

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

echo "==> 通过：三处手写点对齐、compose 结构正确、lockfile 未漂"
