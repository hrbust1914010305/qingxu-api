# 清虚后台 API

清虚后台 API 是基于 Spring Boot 的管理端后端服务，提供登录会话、用户、角色、菜单、部门、通知、权限变更推送、审计日志等能力。

## 核心功能

- 账号认证：验证码、登录、注册、登出、当前用户信息。
- 会话管理：Spring Session + Redis 存储登录态，使用 `QINGXU_SESSION` Cookie。
- 权限控制：接口登录鉴权 + `@PreAuthorize` 权限码校验。
- 用户管理：用户列表、详情、新增、修改、删除、启停、重置密码、批量分配角色、导出。
- 角色管理：角色列表、详情、新增、修改、删除、启停、分配菜单。
- 菜单管理：菜单树、菜单列表、路由树、按钮权限。
- 部门管理：部门分类、部门树、部门详情、新增、修改、删除、启停。
- 通知中心：未读数量、列表、已读、全部已读、删除。
- 实时推送：WebSocket/STOMP 推送通知和权限变更刷新消息。
- 审计日志：登录、登出、权限变更等关键操作记录。

## 从零拉取并运行项目

这一节面向不熟悉后端开发的同学。按顺序做即可，本项目是后端 API 服务，启动后默认访问地址是 `http://localhost:8081`。

### 1. 需要先安装的软件

必须安装：

- `Git`：用于从远程仓库拉代码。
- `JDK 21`：用于运行 Java 后端项目。
- `Docker Desktop`：推荐用它一键启动 PostgreSQL 和 Redis，省去手动安装数据库。

可选安装：

- `IntelliJ IDEA`：用于打开和调试项目。
- `Postman` 或 `Apifox`：用于手动测试接口。
- `Maven`：可选。项目已经带了 Maven Wrapper，Windows 下可以直接用 `mvnw.cmd`，不单独安装 Maven 也能运行。

安装后打开 PowerShell，确认命令可用：

```powershell
git --version
java -version
docker --version
```

`java -version` 需要显示 Java 21。如果不是 21，请安装 JDK 21 并配置 `JAVA_HOME`。

### 2. 拉取代码

```powershell
git clone <你的远程仓库地址>
cd qingxu-api
```

后续命令都在 `qingxu-api` 项目根目录执行。

### 3. 启动 PostgreSQL 和 Redis

本项目需要两个基础服务：

- PostgreSQL：保存用户、角色、部门等业务数据。
- Redis：保存登录 Session，登录后 Cookie 对应的会话存在 Redis 里。

推荐使用 Docker 启动：

```powershell
docker run -d --name qingxu-postgres `
  -e POSTGRES_USER=qingxu `
  -e POSTGRES_PASSWORD=123456 `
  -e POSTGRES_DB=qingxu `
  -p 5432:5432 `
  postgres:16
```

```powershell
docker run -d --name qingxu-redis `
  -p 6379:6379 `
  redis:7
```

检查容器是否启动成功：

```powershell
docker ps
```

如果以后已经创建过容器，只需要启动：

```powershell
docker start qingxu-postgres
docker start qingxu-redis
```

### 4. 确认开发环境配置

开发环境默认读取 [src/main/resources/application-dev.yml](src/main/resources/application-dev.yml)，默认数据库配置是：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/qingxu
    username: qingxu
    password: 123456
```

默认 Redis 配置是：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

如果你按第 3 步用 Docker 启动，就不需要改这些配置。

文件上传目录默认在 [src/main/resources/application.yml](src/main/resources/application.yml) 中配置为 `F:/LocalFiles/files` 和 `F:/LocalFiles/file-chunks`。如果你的电脑没有 `F:` 盘，建议在本机改成已有目录，例如：

```yaml
qingxu:
  file:
    local-root: D:/qingxu/files
    chunk-root: D:/qingxu/file-chunks
```

文件预览功能依赖本机 `kkFileView`。如果你只是先把后台 API 跑起来，可以先在本机配置里关闭：

```yaml
qingxu:
  preview:
    enabled: false
```

### 5. 启动后端服务

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux：

```bash
./mvnw spring-boot:run
```

第一次启动会下载依赖，时间会比较久。看到类似下面的信息，说明启动成功：

```text
Tomcat started on port 8081
Started QingxuApiApplication
```

启动成功后，浏览器打开：

```text
http://localhost:8081/actuator/health
```

返回 `UP` 表示服务正常。

接口文档地址：

```text
http://localhost:8081/swagger-ui/index.html
```

### 6. 初始化数据和默认账号

项目启动时会自动执行 Flyway 数据库迁移脚本，创建表和基础数据。

默认管理员账号：

```text
用户名：admin
密码：Admin@123456
```

首次登录后系统会要求修改密码。

如果需要重建 5 个部门、6 个业务角色、32 个演示用户，可以参考：

```text
scripts/rebuild-backoffice-data.md
```

演示用户默认密码：

```text
Qingxu@123456
```

### 7. 常见问题

如果启动时报数据库连接失败：

- 确认 PostgreSQL 容器已经启动：`docker ps`
- 确认端口是 `5432`
- 确认用户名是 `qingxu`
- 确认密码是 `123456`
- 确认数据库名是 `qingxu`

如果登录后马上变成未登录：

- 确认 Redis 容器已经启动。
- 确认后端连接的是 `localhost:6379`。
- 前端请求需要携带 Cookie，axios 要设置 `withCredentials: true`。

如果 8081 端口被占用：

- 关闭占用 8081 的程序。
- 或修改 [src/main/resources/application.yml](src/main/resources/application.yml) 中的 `server.port`。

如果 Maven 下载依赖很慢：

- 保持网络可访问 Maven Central。
- 或配置公司/国内 Maven 镜像。

## 启动配置

默认配置文件为 [src/main/resources/application.yml](src/main/resources/application.yml)，开发环境覆盖项在 [src/main/resources/application-dev.yml](src/main/resources/application-dev.yml)，生产环境覆盖项在 [src/main/resources/application-prod.yml](src/main/resources/application-prod.yml)。

常用启动命令：

```powershell
mvn spring-boot:run
```

编译验证：

```powershell
mvn -DskipTests clean compile
```

## 可配置项

### 服务端口

```yaml
server:
  port: 8081
```

### 数据库

开发环境支持通过环境变量覆盖：

```yaml
spring:
  datasource:
    url: ${QINGXU_DB_URL:jdbc:postgresql://localhost:5432/qingxu}
    username: ${QINGXU_DB_USERNAME:qingxu}
    password: ${QINGXU_DB_PASSWORD:123456}
```

生产环境：

```yaml
spring:
  datasource:
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
```

### Redis

```yaml
spring:
  data:
    redis:
      host: ${QINGXU_REDIS_HOST:localhost}
      port: ${QINGXU_REDIS_PORT:6379}
      password: ${QINGXU_REDIS_PASSWORD:}
      database: 0
      timeout: 3s
```

### 登录态过期时间

本项目当前使用 Session Cookie，不是 JWT token。登录态过期时间由以下配置控制：

```yaml
qingxu:
  session:
    timeout: 2h
    redis-namespace: spring:session:qingxu
```

`timeout` 会写入 Redis Session Repository 的默认最大空闲时间。可配置示例：

```yaml
qingxu:
  session:
    timeout: 30m
```

支持 Spring Boot `Duration` 格式，例如 `30m`、`2h`、`1d`。

### Session Cookie

```yaml
qingxu:
  session:
    cookie:
      name: QINGXU_SESSION
      path: /
      max-age: -1
      http-only: true
      same-site:
        dev: Lax
        prod: None
      secure:
        dev: false
        prod: true
```

说明：

- `max-age: -1` 表示浏览器会话 Cookie。
- 生产环境跨站部署时通常需要 `SameSite=None` 且 `Secure=true`。
- 前端请求需要携带 Cookie，例如 axios 设置 `withCredentials: true`。

### 公开访问白名单

```yaml
qingxu:
  security:
    public-paths:
      - /api/auth/captcha
      - /api/auth/login
      - /api/auth/register
      - /actuator/health
      - /v3/api-docs/**
      - /swagger-ui/**
      - /ws/**
```

未在白名单中的接口都需要登录。

### CORS

```yaml
qingxu:
  security:
    cors:
      allowed-origins:
        - http://localhost:5173
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowed-headers:
        - "*"
      allow-credentials: true
```

生产环境可通过环境变量指定前端域名：

```yaml
qingxu:
  security:
    cors:
      allowed-origins:
        - ${QINGXU_WEB_ORIGIN:http://localhost:5173}
```

### CSRF

```yaml
qingxu:
  security:
    csrf:
      enabled: false
```

开发环境默认关闭，生产环境默认开启：

```yaml
qingxu:
  security:
    csrf:
      enabled: true
```

### 验证码

```yaml
qingxu:
  captcha:
    width: 120
    height: 40
    code-count: 4
    interfere-count: 15
    expires-in-seconds: 120
```

### WebSocket/STOMP

```yaml
qingxu:
  websocket:
    endpoint: /ws
    broker-prefixes:
      - /queue
      - /topic
    application-destination-prefix: /app
    user-destination-prefix: /user
    heartbeat:
      - 10000
      - 10000
    heartbeat-pool-size: 2
    notification-queue: /queue/notifications
    permission-reload-queue: /queue/permission-reload
```

前端订阅路径：

- 通知：`/user/queue/notifications`
- 权限刷新：`/user/queue/permission-reload`

WebSocket Origin 默认复用 CORS `allowed-origins`，也可以扩展 `qingxu.websocket.allowed-origin-patterns` 单独配置。

### 文件上传

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB
```

### OpenAPI

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui/index.html
```

### 日志级别

```yaml
logging:
  level:
    com.qingxu.qingxuapi: info
```

开发环境可临时调高 Spring Security、Session、Web 日志；生产环境建议保持 `warn` 或按需开启。

## 前端接入要点

- 所有需要登录的接口必须带 Cookie：`withCredentials: true`。
- 部门接口 `/api/department/**` 已启用鉴权，未登录会返回 `401`，无权限会返回 `403`。
- 生产环境前端域名必须加入 `QINGXU_WEB_ORIGIN` 或 `qingxu.security.cors.allowed-origins`。
- 如果前后端跨站部署，后端 Cookie 需要 `SameSite=None`、`Secure=true`，前端必须使用 HTTPS。
- WebSocket 地址默认是 `/ws`，订阅路径保持 `/user/queue/...`。

## 验证状态

当前已通过：

```powershell
mvn -DskipTests clean compile
mvn -Dtest=SourceHygieneTest test
```

全量测试目前仍受既有 H2/Flyway 兼容问题影响：`V18__enhance_role_table.sql` 使用了 H2 不支持的多列 `ALTER TABLE ... ADD COLUMN` 写法。
