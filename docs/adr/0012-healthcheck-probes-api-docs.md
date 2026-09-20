# 后端健康检查探 /v3/api-docs，不引 actuator

Status: accepted

给 `server` 加了 healthcheck，`web` 的 `depends_on` 相应改成 `condition: service_healthy`。探测端点选的是 springdoc 的 `/v3/api-docs`——因为项目没有 `spring-boot-starter-actuator`，Spring Boot 本身不提供 `/actuator/health`，而 `/v3/api-docs` 一定返回 200，且足以证明整个应用上下文已经初始化完毕。为一个探测端点去加一个依赖，不划算。

## Considered Options

- **引 actuator，用 `/actuator/health`**：语义最正、端点最轻。代价是多一个依赖、多一处配置，还得决定向外暴露哪些端点。
- **bash 的 `/dev/tcp` 探端口**：零开销、不产生任何业务请求，但只能证明 Tomcat 在监听，不如 HTTP 探测直观，写法也偏冷门。
- **`/v3/api-docs`**（选定）：零新增依赖，能证明上下文真的起来了。代价是每次探测会真的生成一遍 API 文档，比专门的健康端点重。

## Consequences

- 探测频率别调太高，`interval: 10s` 已经够用；调到 1s 会明显浪费 CPU。
- 单测或 CI 里如果关掉 springdoc（`springdoc.api-docs.enabled=false`），这个探测会一直失败，容器会被判 unhealthy。
- healthcheck 只影响**启动顺序**和**可观测性**，**不会**让 unhealthy 的容器自动重启——`restart: unless-stopped` 只在进程退出时生效。别指望它能自愈。
- 启动顺序这件事本身收益有限：`nginx.conf` 用 `resolver` + 变量 `proxy_pass` 已经兜住了后端没就绪那几秒。加 healthcheck 主要是买个体温计，让 `docker compose ps` 能直接看出后端是不是真活着。
