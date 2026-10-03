#!/usr/bin/env bash
# 在服务器上跑 docker compose 的包装：自动补上 APP_VERSION，再原样转发给 compose。
#
#   bash server-compose.sh ps
#   bash server-compose.sh logs --tail=100 server
#   bash server-compose.sh config
#
# 为什么需要它：compose **每次调用都会把整份文件插值一遍**，而 services.server / web 的
# `image:` 带着 `${APP_VERSION:?...}` 这个必填断言 —— 于是连 `docker compose ps` / `logs`
# 这种只读命令也会因为缺 APP_VERSION 而直接退出，报错长得像「compose 文件坏了」。
# CI 部署时 server-deploy.sh 会 export 它，但人手敲命令时没人管，所以给运维一个免记的入口。
#
# 版本号来源是部署脚本写下的 $VERSION_FILE（不是 .env —— .env 是密钥与本机配置的地盘）。
# ⚠️ 本文件必须是 LF 换行（见 .gitattributes）。
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

COMPOSE_FILE="${COMPOSE_FILE:-../docker-compose.prod.yml}"
SERVER_ROOT="${SERVER_ROOT:-/srv/quizzy}"
ENV_FILE="${ENV_FILE:-$SERVER_ROOT/.env}"
VERSION_FILE="${VERSION_FILE:-$SERVER_ROOT/compose/.current-version}"

if [ -f "$VERSION_FILE" ]; then
  APP_VERSION="$(cat "$VERSION_FILE")"
else
  # 还没成功部署过：给个占位，够 compose 插值过去；此时本来也没有容器可看。
  APP_VERSION="${APP_VERSION:-0.0.0}"
  echo "（提示：$VERSION_FILE 不存在，说明还没成功部署过；APP_VERSION 暂用 $APP_VERSION）" >&2
fi
export APP_VERSION

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
