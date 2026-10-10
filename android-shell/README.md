# android-shell · Quizzy 的 Android 壳

一个极简 WebView 壳：加载线上移动端（`app/src/main/res/values/strings.xml` 的 `app_url`，当前为
`https://11-2.cn/m/`）。**不嵌 H5 产物、直接加载线上地址**——网站更新后 App 无需重发。

路线与取舍（2026-10-09 定）：自写壳 + 本地构建（不用 HBuilderX 云打包、不用 Capacitor）。
它是「自用可安装」路线；以后要上架 / 加原生能力，再评估官方打包路线（不冲突）。

## 构建

前置（本机已就绪）：

- JDK 17+（`E:\jdk21`）、Gradle 8.9（`E:\develop\gradle-8.9`）、Android SDK
  （`E:\develop\android-sdk`：platform-tools + platforms;android-34 + build-tools;34.0.0）
- `local.properties`（**不入库**）：`sdk.dir` 与签名四件套
  （`quizzy.storeFile` / `quizzy.storePassword` / `quizzy.keyAlias` / `quizzy.keyPassword`）。
  keystore 在本仓 `.workbuddy/android-secrets/`（**不入库**），说明见该目录 README。

```bash
export JAVA_HOME=E:/jdk21
"E:/develop/gradle-8.9/bin/gradle" assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

依赖拉取：`settings.gradle` 里阿里云镜像优先、google/central 兜底；
`gradle.properties` 配了 v2rayN 代理（兜底请求走代理分流）。

## 验证

```bash
"E:/develop/android-sdk/build-tools/34.0.0/aapt2.exe" dump badging app/build/outputs/apk/release/app-release.apk
```

## 已知边界

- 安装需允许「未知来源」（自签名）；上架需换正式签名。
- ⚠️ keystore 丢了就没法覆盖安装升级——备份 `.workbuddy/android-secrets/`。
- WebView 的返回键语义：页面内可后退则后退，到根页退出（见 `MainActivity`）。
