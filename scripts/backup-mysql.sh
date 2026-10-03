#!/usr/bin/env bash
# 导出 quizzy 库到 $BACKUP_DIR，并清掉超过 $BACKUP_KEEP_DAYS 天的旧文件。
#
#   bash scripts/backup-mysql.sh
#
# 需要 mysql 容器正在运行。备份位置读 .env 的 BACKUP_DIR，默认是项目内 .backup/。
# 云端 BACKUP_DIR 指向 /srv/quizzy/backup（见仓库 deploy/.env.cloud.example）。
# ⚠️ 本机默认位置与 MySQL 数据目录同在 E 盘，属于同一故障域，只能算复制不算备份；
#    最终该挪到别的盘或对象存储，见 docs/todo/2026-09-20-TODO-数据库备份.md。
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."

# ENV_FILE 可由环境变量覆盖：云端由 server-deploy.sh 传 /srv/quizzy/.env 进来
# （compose 目录下的脚本用相对路径找不到它）。本机不传就还是项目根的 .env。
ENV_FILE="${ENV_FILE:-.env}"
if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$ENV_FILE"
  set +a
fi

CONTAINER="${MYSQL_CONTAINER:-quizzy-mysql}"
DB="${MYSQL_DATABASE:-quizzy}"
ROOT_PW="${MYSQL_ROOT_PASSWORD:-123456}"
BACKUP_DIR="${BACKUP_DIR:-./.backup}"
KEEP_DAYS="${BACKUP_KEEP_DAYS:-7}"

mkdir -p "$BACKUP_DIR"
OUT="${BACKUP_DIR}/quizzy-$(date +%F-%H%M%S).sql"

# ⚠️ 这里刻意**不加** `2>/dev/null`：本脚本在**每次部署前**都会跑（ADR 0016 的第一道保险），
#    把 stderr 丢掉会让「Access denied」「容器没跑」这类真因彻底不可见，只剩一句「备份失败」。
# ⚠️ 也刻意不只靠「文件为空」来判错：`set -e` 会让 docker exec 非零时**当场退出**，
#    护栏根本执行不到（那一段曾经是死代码）。所以出口码与空文件两条路都要显式判。
if ! docker exec "$CONTAINER" mysqldump -uroot -p"$ROOT_PW" \
  --databases "$DB" \
  --default-character-set=utf8mb4 \
  --single-transaction --routines --triggers --events \
  --set-gtid-purged=OFF --hex-blob >"$OUT"; then
  echo "备份失败：mysqldump 退出码非零（上面是它的原文报错）。$CONTAINER 在跑吗？密码对吗？" >&2
  rm -f "$OUT"
  exit 1
fi

if [ ! -s "$OUT" ]; then
  echo "备份失败：$OUT 是空的——mysqldump 没报错却没吐出内容，先查 $CONTAINER 的状态" >&2
  rm -f "$OUT"
  exit 1
fi

echo "已导出 $OUT（$(wc -c <"$OUT" | tr -d ' ') 字节）"

find "$BACKUP_DIR" -name 'quizzy-*.sql' -mtime "+${KEEP_DAYS}" -delete 2>/dev/null || true
echo "已清理 ${KEEP_DAYS} 天前的旧备份，当前保留："
ls -1 "$BACKUP_DIR" | grep -c '^quizzy-.*\.sql$' || true
