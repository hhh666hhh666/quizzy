# 用 slim（Debian/glibc）而非 alpine：esbuild 没有 x64-musl 的官方二进制，
# alpine 下 vite build 会报 "You installed esbuild for another platform"。
FROM node:22-slim AS build
WORKDIR /app

# NPM_REGISTRY 默认 npmmirror：官方源本机实测能通但慢约 4 倍，默认仍取国内源
# （ADR 0010 Amendment 1）；CI 在境外 runner 上传 https://registry.npmjs.org。
# ⚠️ ARG 要声明在用到它的这个 stage 里、且排在用到它的 RUN 之前；
#    写到 FROM nginx:alpine 之后它就看不到了。
ARG NPM_REGISTRY=https://registry.npmmirror.com

# 依赖层：npm ci 严格按 package-lock.json 安装
COPY package.json package-lock.json ./
RUN npm ci --registry="$NPM_REGISTRY" --no-audit --no-fund

COPY . .

# 版本与构建信息在构建期注入（ADR 0015）：值由 scripts/deploy.sh 从 git tag 算出，
# 经 compose 的 build.args 传进来，vite.config.ts 再 define 成 __APP_*__ 常量打进产物。
# ⚠️ 必须声明在 npm ci 那一层**之后**、npm run build **之前**：ARG 值一变，它之后的层
#    缓存全失效，放到前面会让每次发版都白重装一遍依赖。
ARG APP_VERSION=dev
ARG APP_AHEAD=0
ARG APP_COMMIT=unknown
ARG APP_BUILD_TIME=unknown

RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
