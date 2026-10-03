#!/usr/bin/env bash
# 回滚到 ACR 里某个已发布的版本。
#
#   bash server-rollback.sh <版本号>      # 不带 v，如 1.1.0；不传则打印当前版本后退出
#
# 实现上刻意只是「把版本号交给 server-deploy.sh 重跑一遍」——回滚因此与部署共享同一份逻辑
# （同样会先备份、先 pull、再等健康），不会两处漂移。
#
# ⚠️ docs/adr/0016 的纪律是「迁移只做向后兼容」，所以**回退应用 ≠ 回退数据库**。
#    回滚前确认那个版本能兼容当前的库结构；破坏性迁移不能靠回滚兜底。
# ⚠️ 本文件必须是 LF 换行（见 .gitattributes）。
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SERVER_ROOT="${SERVER_ROOT:-/srv/quizzy}"
VERSION_FILE="${VERSION_FILE:-$SERVER_ROOT/compose/.current-version}"

TARGET="${1:-}"
if [ -z "$TARGET" ]; then
  echo "用法：$0 <要回滚到的版本号，如 1.1.0>" >&2
  [ -f "$VERSION_FILE" ] && echo "当前已上线版本：$(cat "$VERSION_FILE")" >&2
  echo "用 ACR 里已有的版本 tag（每个已发版本都保留着，见 docs/operations/deployment.md）。" >&2
  exit 1
fi

echo "==> 回滚到 $TARGET"
exec bash ./server-deploy.sh "${TARGET#v}" all
