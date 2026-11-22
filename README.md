# 第 01 周 · 任务台

基于 Spring Boot 3、Java 21 和 Maven 的内存任务管理项目。首页支持新增、查看、编辑、删除、状态筛选和进度统计。

## 启动与验收

在 PowerShell 中执行以下命令。若 Maven 还没加入 PATH，可以用 `& 'D:\后端软件\apache-maven-3.10.0\bin\mvn.cmd' clean package` 执行同一构建。

```powershell
mvn clean package
java -jar target/week-01-0.0.1-SNAPSHOT.jar
```

浏览器打开 [任务台](http://localhost:8080/)。也可以用 `mvn spring-boot:run` 直接启动。

默认加载 5 条演示任务。任务保存在 JVM 内存中，重启后会重新加载演示数据。设置 `TASKS_SEED_DEMO=false` 可以从空列表启动，设置 `PORT` 可以更改默认的 8080 端口。配置位于 `src/main/resources/application.yml`。

## REST API

以下接口用于本地演示，访问时无需认证。

| 方法 | 路径 | 成功 | 常见错误 |
| --- | --- | --- | --- |
| GET | `/api/health` | `200 OK`，返回 `{"status":"UP"}` | — |
| GET | `/api/tasks` | `200 OK`，按 ID 倒序返回任务数组，空列表为 `[]` | — |
| GET | `/api/tasks/{id}` | `200 OK`，返回任务详情 | `400`、`404` |
| POST | `/api/tasks` | `201 Created`，返回新任务，`Location` 指向详情地址 | `400`、`415` |
| PUT | `/api/tasks/{id}` | `200 OK`，返回完整更新后的任务 | `400`、`404`、`415` |
| DELETE | `/api/tasks/{id}` | `204 No Content`，响应体为空 | `400`、`404` |

路径参数 `id` 是整数。POST 和 PUT 接受 `application/json`。两者使用相同的请求字段。

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `title` | 字符串 | 必填，非空白，最多 80 字 |
| `description` | 字符串 | 可选，最多 500 字。省略或传 `null` 时保存为空字符串 |
| `status` | 字符串 | 必填，值为 `TODO`、`IN_PROGRESS` 或 `DONE` |

POST、详情 GET 和 PUT 的任务响应采用同一结构。时间字段为 UTC ISO 8601 字符串。

```json
{
  "id": 6,
  "title": "Prepare demo",
  "description": "Review the endpoints",
  "status": "TODO",
  "createdAt": "2026-11-22T12:00:00Z",
  "updatedAt": "2026-11-22T12:00:00Z"
}
```

无效 JSON、字段校验失败或无效 ID 返回 `400 Bad Request`，任务不存在返回 `404 Not Found`。错误响应使用 `application/problem+json`，字段校验失败时额外返回 `errors` 对象。

### PowerShell 接口演示

```powershell
$base = 'http://localhost:8080'
curl.exe -i "$base/api/health"
curl.exe -i "$base/api/tasks"

$body = @{ title = 'Prepare demo'; description = 'Review the endpoints'; status = 'TODO' } | ConvertTo-Json -Compress
$task = $body | curl.exe --fail-with-body -sS -X POST "$base/api/tasks" -H 'Content-Type: application/json' --data-binary '@-' | ConvertFrom-Json
$url = "$base/api/tasks/$($task.id)"

curl.exe -i $url
$updated = @{ title = 'Demo ready'; description = 'REST flow verified'; status = 'DONE' } | ConvertTo-Json -Compress
$updated | curl.exe -i -X PUT $url -H 'Content-Type: application/json' --data-binary '@-'
curl.exe -i -X DELETE $url
curl.exe -i $url  # 应返回 404
```

## 项目结构

- `task/TaskController` 处理 HTTP 路由、请求校验和响应状态。
- `task/TaskService` 管理内存任务、ID、状态和时间戳，通过构造器注入到 Controller。
- `task/TaskRequest`、`task/TaskResponse` 定义 JSON 数据结构。
- `task/TaskProperties` 通过 `@ConfigurationProperties` 绑定 YAML 中的演示数据开关。
- `static/` 包含由 Spring Boot 直接提供的页面、样式和脚本。
- `TaskApiTest` 使用 JUnit 和 MockMvc 覆盖首页、健康检查、CRUD 和错误响应，在 `mvn clean package` 时自动运行。
