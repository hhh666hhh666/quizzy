#!/usr/bin/env bash
# quizzy 云上部署：拉取 ACR 里指定版本的镜像，重启 server / web。
# 由 .github/workflows/release.yml 经 SSH 调用；也可在服务器上手动跑（回滚就是手动跑一个旧版本）。
#
#   bash server-deploy.sh <版本号> [web|server|all]      # 版本号不带 v，如 1.1.0
#
# 从已删除的本机版部署脚本（deploy.sh）继承的设计（要改这里之前先读那一条的教训）：
#   * mysql 永不重建：用 --no-deps 绕开 depends_on，它最多在「没在跑」时被拉起一次；
#   * up -d 之前先强制备份一次（docs/adr/0016 记的第一道保险）；
#   * 等 healthcheck（无 healthcheck 的容器退回看 State.Status）；
#   * 失败时打印日志尾部与回滚命令。
#
# ⚠️ 服务器上没有 git，版本号一律从参数拿。
# ⚠️ 必须显式 --env-file：云端 .env 在 /srv/quizzy/，而 compose 默认只找自己所在的目录。
# ⚠️ 本文件必须是 LF 换行，CRLF 会让 bash 报 bad interpreter（见 .gitattributes）。
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

COMPOSE_FILE="${COMPOSE_FILE:-../docker-compose.prod.yml}"
SERVER_ROOT="${SERVER_ROOT:-/srv/quizzy}"
ENV_FILE="${ENV_FILE:-$SERVER_ROOT/.env}"
VERSION_FILE="${VERSION_FILE:-$SERVER_ROOT/compose/.current-version}"

RAW_VERSION="${1:-}"
SCOPE="${2:-all}"

case "$SCOPE" in
  all)    SERVICES=(server web) ;;
  server) SERVICES=(server) ;;
  web)    SERVICES=(web) ;;
  *)      echo "用法：$0 <版本号，如 1.1.0> [web|server|all]" >&2; exit 1 ;;
esac

if [ -z "$RAW_VERSION" ]; then
  echo "用法：$0 <版本号，如 1.1.0> [web|server|all]" >&2
  echo "服务器上没有 git，版本号必须显式给（CI 传 tag 去掉 v 前缀之后的值）。" >&2
  exit 1
fi

# 镜像 tag 不带 v（git tag 带）。compose 不支持 ${VAR#v} 截断，所以剥前缀只能发生在这里。
APP_VERSION="${RAW_VERSION#v}"
export APP_VERSION

compose() { docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"; }

# 失败时统一出口：说清原因、附上容器日志尾部、给出回滚命令
fail() {
  local reason="$1"; shift
  echo "" >&2
  echo "部署失败：$reason" >&2
  echo "" >&2
  echo "=== 容器日志尾部 ===" >&2
  compose logs --tail=100 "$@" >&2 || true
  echo "" >&2
  echo "=== 怎么回滚 ===" >&2
  local prev=""
  [ -f "$VERSION_FILE" ] && prev="$(cat "$VERSION_FILE")"
  if [ -n "$prev" ]; then
    echo "  回到上一个已上线版本（镜像仍在 ACR，不需要重新构建）：" >&2
    echo "    bash $0 $prev" >&2
  else
    echo "  没有记录到上一个版本（可能是首次部署）。手动给一个 ACR 里存在的 tag：" >&2
    echo "    bash $0 <旧版本号>" >&2
  fi
  echo "  数据在宿主机的 MYSQL_DATA_DIR（见 $ENV_FILE），不在容器里，不会被卷走。" >&2
  exit 1
}

# 等 healthcheck 跑到 healthy；没有 healthcheck 的容器（web/nginx）退回看 State.Status=running。
wait_ready() {
  local name="$1" limit="${2:-180}" elapsed=0 status=""
  while [ "$elapsed" -lt "$limit" ]; do
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$name" 2>/dev/null || echo missing)"
    case "$status" in
      healthy|running) echo "    $name 就绪（$status，等了 ${elapsed}s）"; return 0 ;;
      missing)         echo "    $name 不存在" >&2; return 1 ;;
    esac
    sleep 5; elapsed=$((elapsed + 5))
  done
  echo "    等了 ${limit}s，$name 仍是 $status" >&2
  return 1
}

# ---- 1. 前置检查 ----
docker compose version >/dev/null 2>&1 || { echo "docker compose 用不了" >&2; exit 1; }
[ -f "$COMPOSE_FILE" ] || { echo "找不到 $COMPOSE_FILE" >&2; exit 1; }
[ -f "$ENV_FILE" ] || { echo "找不到 $ENV_FILE（云端 .env，模板见仓库 deploy/.env.cloud.example）" >&2; exit 1; }

echo "==> 部署 $APP_VERSION（范围：${SERVICES[*]}）"

# ---- 2. 保证 mysql 在跑，但绝不重建 ----
# 用 docker ps 判断而不是 compose：避免 up 因为 depends_on 把 mysql 一起拉进这次操作。
if docker ps --format '{{.Names}}' | grep -qx quizzy-mysql; then
  echo "==> mysql 已在运行，跳过"
else
  echo "==> mysql 没在跑，拉起来"
  compose up -d mysql || fail "mysql 起不来" mysql
fi
wait_ready quizzy-mysql 180 || fail "mysql 起不来" mysql

# ---- 3. up -d 之前强制备份一次（ADR 0016 的第一道保险）----
echo "==> 部署前备份"
ENV_FILE="$ENV_FILE" bash ./backup-mysql.sh || fail "备份失败，中止部署（不带着没备份的库去跑迁移）"

# ---- 4. 拉取新镜像并替换业务容器 ----
# 先 pull 再 up：把「拉不到镜像」这类失败挡在替换之前。
echo "==> 拉取镜像 $APP_VERSION"
compose pull "${SERVICES[@]}" || fail "拉取镜像失败（ACR 上有 $APP_VERSION 这个 tag 吗？）" "${SERVICES[@]}"

# --no-deps 是关键：跳过 depends_on，确保这一步碰不到 mysql
echo "==> 重建并启动：${SERVICES[*]}"
compose up -d --no-deps "${SERVICES[@]}" || fail "compose up 失败" "${SERVICES[@]}"

# ---- 5. 等健康检查 ----
echo "==> 等待容器就绪"
case "$SCOPE" in
  server|all) wait_ready quizzy-server 180 || fail "后端未通过健康检查" server ;;
esac
case "$SCOPE" in
  web|all)    wait_ready quizzy-web 60    || fail "前端容器没起来" web ;;
esac

# ---- 6. 探一下真实端口（只告警，不中断）----
# 后端不发布端口，所以进容器里探；前端探宿主回环上的 8081。
if command -v curl >/dev/null 2>&1; then
  case "$SCOPE" in
    server|all)
      docker exec quizzy-server curl -fsS --max-time 5 -o /dev/null http://127.0.0.1:8080/v3/api-docs \
        && echo "    后端 /v3/api-docs 正常" \
        || echo "    ⚠️ 后端探测失败，看容器日志" ;;
  esac
  case "$SCOPE" in
    web|all)
      curl -fsS --max-time 5 -o /dev/null http://127.0.0.1:8081/ \
        && echo "    前端 127.0.0.1:8081 正常" \
        || echo "    ⚠️ 8081 探不到，检查 web 容器与宿主 nginx" ;;
  esac
fi

# ---- 7. 记下已上线版本，供回滚读取 ----
mkdir -p "$(dirname "$VERSION_FILE")"
echo "$APP_VERSION" > "$VERSION_FILE"

echo ""
echo "==> 部署完成，当前状态"
compose ps
echo "已上线版本：$APP_VERSION（记录在 $VERSION_FILE）"
echo "对外入口是宿主 nginx（宝塔）反代到 127.0.0.1:8081，模板见仓库 deploy/nginx/quizzy-site.conf。"
