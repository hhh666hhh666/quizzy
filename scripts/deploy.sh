#!/usr/bin/env bash
# 重建并重启 quizzy 的前后端容器。这是 CD 的另一半：CI 只做门禁（ADR 0013），
# 上线这一步永远由人触发。
#
#   bash scripts/deploy.sh          # 前后端都重建（默认）
#   bash scripts/deploy.sh server   # 只重建后端
#   bash scripts/deploy.sh web      # 只重建前端 nginx
#
# ⚠️ mysql 永远不在重建名单里：题库数据在宿主机的 MYSQL_DATA_DIR 上（ADR 0007），
#    重建它只是白冒风险。它最多在「没跑起来」时被拉起一次，跑着就完全不动。
# ⚠️ 本文件必须是 LF 换行，CRLF 会让 bash 报 bad interpreter（见 .gitattributes）。
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."

COMPOSE_FILE="docker-compose.prod.yml"
SCOPE="${1:-all}"

# 参数校验放最前面，这样 bash scripts/deploy.sh bogus 不需要 Docker 也能给出用法
case "$SCOPE" in
  all)    SERVICES=(server web) ;;
  server) SERVICES=(server) ;;
  web)    SERVICES=(web) ;;
  *)      echo "用法：$0 [web|server|all]" >&2; exit 1 ;;
esac

compose() { docker compose -f "$COMPOSE_FILE" "$@"; }

# 失败时统一出口：说清原因、附上容器日志尾部、给出回滚路径
fail() {
  local reason="$1"; shift
  echo "" >&2
  echo "部署失败：$reason" >&2
  echo "" >&2
  echo "=== 容器日志尾部 ===" >&2
  compose logs --tail=100 "$@" >&2 || true
  echo "" >&2
  echo "=== 怎么回滚 ===" >&2
  # 镜像 tag 固定是 0.1.0，重建时被覆盖了，所以回滚只能回到上一个能用的代码版本
  echo "  git checkout <上一个能用的 commit> && bash scripts/deploy.sh $SCOPE" >&2
  echo "想先停掉别让它反复重启（数据不受影响）：" >&2
  echo "  docker compose -f $COMPOSE_FILE stop $*" >&2
  echo "数据在宿主机 ${MYSQL_DATA_DIR:-（未设置）}，不在容器里，不会被卷走。" >&2
  exit 1
}

# 等容器 healthcheck 跑到 healthy；没有 healthcheck 的容器（web/nginx）退回看
# State.Status=running。start_period 内状态是 starting，会继续等。
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

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose 用不了：Docker Desktop 起了吗？" >&2
  exit 1
fi

if [ ! -f .env ]; then
  echo "缺少 .env，先复制一份模板再填：cp .env.example .env" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
. ./.env
set +a

MYSQL_DATA_DIR="${MYSQL_DATA_DIR:-E:/develop/docker/mysql8/var/lib/mysql}"

# QUIZZY_JWT_SECRET 是 compose 里唯一的 :? 必填项（ADR 0011），缺了 up 在读配置
# 阶段就退出。这里提前拦下来，顺便把生成命令一起给出去。
if [ -z "${QUIZZY_JWT_SECRET:-}" ]; then
  echo ".env 里缺少 QUIZZY_JWT_SECRET，而且必须是随机值——默认值已随公开仓库泄露。" >&2
  echo "生成一个填进 .env 的 QUIZZY_JWT_SECRET=" >&2
  echo '  python -c "import secrets;print(secrets.token_hex(32))"' >&2
  exit 1
fi

# ---- 2. 保证 mysql 在跑，但绝不重建它 ----
# 单独拎出来，是为了下一步能对业务容器用 --no-deps：否则 up 会因为 depends_on
# 把 mysql 一起拉进这次操作，一旦 compose 判定配置有变就会连数据库一起 recreate。
if docker ps --format '{{.Names}}' | grep -qx quizzy-mysql; then
  echo "==> mysql 已在运行，跳过（数据目录：$MYSQL_DATA_DIR）"
else
  echo "==> mysql 没在跑，拉起来"
  compose up -d mysql
fi
wait_ready quizzy-mysql 180 || fail "mysql 起不来" mysql

# ---- 3. 重建并替换业务容器 ----
# --no-deps 是关键：跳过 depends_on，确保这一步碰不到 mysql
echo "==> 重建并启动：${SERVICES[*]}"
compose up -d --build --no-deps "${SERVICES[@]}" \
  || fail "compose up 失败" "${SERVICES[@]}"

# ---- 4. 等健康检查 ----
echo "==> 等待容器就绪"
case "$SCOPE" in
  server|all) wait_ready quizzy-server 180 || fail "后端未通过健康检查" server ;;
esac
case "$SCOPE" in
  web|all)    wait_ready quizzy-web 60    || fail "前端容器没起来" web ;;
esac

# ---- 5. 探一下真实端口（只告警，不中断）----
# 用 127.0.0.1 而不是 localhost：后者在 Windows 上可能先解析到 ::1，
# nginx 只监听 IPv4 时会假失败。
if command -v curl >/dev/null 2>&1; then
  case "$SCOPE" in
    server|all)
      curl -fsS --max-time 5 -o /dev/null "http://127.0.0.1:8080/v3/api-docs" \
        && echo "    http://127.0.0.1:8080/v3/api-docs 正常" \
        || echo "    ⚠️ 8080 探不到，可能还在预热或被别的进程占了" ;;
  esac
  case "$SCOPE" in
    web|all)
      curl -fsS --max-time 5 -o /dev/null "http://127.0.0.1/" \
        && echo "    http://127.0.0.1/ 正常" \
        || echo "    ⚠️ 80 端口探不到，检查 nginx 容器日志" ;;
  esac
fi

# ---- 6. 结果 ----
echo ""
echo "==> 部署完成，当前状态"
compose ps
echo ""
echo "访问地址："
echo "  前端      http://localhost"
echo "  接口文档  http://localhost:8080/swagger-ui.html"
echo ""
echo "mysql 未参与本次操作，数据在 $MYSQL_DATA_DIR"
echo "改完前端记得硬刷新（nginx 没设 Cache-Control）"
