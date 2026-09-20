#!/usr/bin/env bash
# 导出 quizzy 库到 $BACKUP_DIR，并清掉超过 $BACKUP_KEEP_DAYS 天的旧文件。
#
#   bash scripts/backup-mysql.sh
#
# 需要 mysql 容器正在运行。备份位置读 .env 的 BACKUP_DIR，默认是项目内 .backup/。
# ⚠️ 默认位置与 MySQL 数据目录同在 E 盘，属于同一故障域，只能算复制不算备份；
#    最终该挪到别的盘，见 TODO/2026-09-20-TODO-数据库备份.md。
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

CONTAINER="${MYSQL_CONTAINER:-quizzy-mysql}"
DB="${MYSQL_DATABASE:-quizzy}"
ROOT_PW="${MYSQL_ROOT_PASSWORD:-123456}"
BACKUP_DIR="${BACKUP_DIR:-./.backup}"
KEEP_DAYS="${BACKUP_KEEP_DAYS:-7}"

mkdir -p "$BACKUP_DIR"
OUT="${BACKUP_DIR}/quizzy-$(date +%F-%H%M%S).sql"

docker exec "$CONTAINER" mysqldump -uroot -p"$ROOT_PW" \
  --databases "$DB" \
  --default-character-set=utf8mb4 \
  --single-transaction --routines --triggers --events \
  --set-gtid-purged=OFF --hex-blob >"$OUT" 2>/dev/null

if [ ! -s "$OUT" ]; then
  echo "备份失败：$OUT 是空的，检查 $CONTAINER 是否在运行" >&2
  rm -f "$OUT"
  exit 1
fi

echo "已导出 $OUT（$(wc -c <"$OUT" | tr -d ' ') 字节）"

find "$BACKUP_DIR" -name 'quizzy-*.sql' -mtime "+${KEEP_DAYS}" -delete 2>/dev/null || true
echo "已清理 ${KEEP_DAYS} 天前的旧备份，当前保留："
ls -1 "$BACKUP_DIR" | grep -c '^quizzy-.*\.sql$' || true
