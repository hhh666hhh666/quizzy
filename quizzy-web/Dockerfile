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

# 版本与构建信息在构建期注入（ADR 0015）：值由 .github/workflows/release.yml 从 git tag 算出，
# 经 build-args 传进来，vite.config.ts 再 define 成 __APP_*__ 常量打进产物。
# ⚠️ 必须声明在 npm ci 那一层**之后**、npm run build **之前**：ARG 值一变，它之后的层
#    缓存全失效，放到前面会让每次发版都白重装一遍依赖。
ARG APP_VERSION=dev
ARG APP_AHEAD=0
ARG APP_COMMIT=unknown
ARG APP_BUILD_TIME=unknown

RUN npm run build

# ---- 移动端 H5（uni-app）构建阶段 ----
# 源码来自**命名 build context** `mobile`：本镜像的 context 是 ./quizzy-web（单目录），
# 够不到同级的 quizzy-mobile；命名 context 是「一个 Dockerfile 构建两个工程」标准手段
# （BuildKit 特性）。release.yml 用 `contexts: mobile=./quizzy-mobile` 传入；
# 本地验证用 `docker build --build-context mobile=./quizzy-mobile ...`。
# ⚠️ 传统 legacy builder 不支持命名 context——全链路走 BuildKit（CI buildx / 本机 Docker 23+ 默认）。
FROM node:22-slim AS mobile-build
WORKDIR /app

ARG NPM_REGISTRY=https://registry.npmmirror.com

COPY --from=mobile package.json package-lock.json ./
RUN npm ci --registry="$NPM_REGISTRY" --no-audit --no-fund

COPY --from=mobile . .

# 与 web 阶段同一套构建期注入（值由 release.yml 从 git tag 算出，build-args 对全阶段生效）。
# 移动端此刻还没在页面里使用这些常量（见其 vite.config.ts），保留是为了接上时零改动。
ARG APP_VERSION=dev
ARG APP_AHEAD=0
ARG APP_COMMIT=unknown
ARG APP_BUILD_TIME=unknown

RUN npm run build:h5

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
# 移动端挂子路径 /m/（同域，无需新增 CORS）；uni-app 的 H5 产物在 dist/build/h5
COPY --from=mobile-build /app/dist/build/h5 /usr/share/nginx/html/m
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
