# 构建镜像必须基于 glibc，且构建上下文必须排除宿主机 node_modules

Status: accepted

前端构建镜像用 `node:22-slim` 而不是更小的 `node:22-alpine`，同时前后端 Dockerfile 都配了 `.dockerignore`。这两件事同源于一个约束：**代码在 Windows 宿主机上，构建却发生在 Linux 容器里**。alpine 是 musl，而 esbuild 没有 x86_64-musl 的官方二进制，`vite build` 会直接报「You installed esbuild for another platform」；没有 `.dockerignore` 时，`COPY . .` 会把宿主机那份只含 `@esbuild/win32-x64`、`rollup-win32-x64-*` 的 node_modules 覆盖进容器，`npm run build` 同样必崩。不要为了省几十 MB 把它改回 alpine。

## Considered Options

- **`node:22-alpine`**：镜像最小，也是 Node 官方的常见推荐。但 esbuild 无 musl 发行版，构建过不去。
- **alpine + `esbuild-wasm` 兜底**：能让 alpine 跑起来，代价是构建明显变慢，还得多引一个依赖换一个更小的镜像，不划算。
- **`node:22-slim`**（选定）：Debian glibc，esbuild / rollup 的官方二进制直接可用。它只作用于 build 阶段，运行阶段仍是 `nginx:alpine`，最终产物体积基本不受影响。

## Consequences

- build 阶段镜像比 alpine 大约多 100MB，但不会进最终产物，不必为此优化。
- `.dockerignore` 是硬依赖而不是优化项：删掉它构建立刻失败，而报错信息（找不到对应平台的二进制）不看这条记录很难联想到真实原因。
