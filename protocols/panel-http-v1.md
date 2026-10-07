# Console 页面服务 HTTP v1

实现入口：`console/backend/cmd/smt-panel`；静态资源实现：`console/internal/webui`。
本协议只覆盖页面服务，不提供机器业务 API、任务提交或运行状态同步。

## 访问地址

开发默认 `127.0.0.1:8080`；systemd 模板显式设置 `0.0.0.0:8080`。
监听地址由 `--listen` 设置。此服务没有 TLS 或用户登录，当前只提供同一份模拟 UI 与静态资源。
不得把此状态作为后续运动控制 API 可以直接开放的依据。

## 路由

| 方法 | 路径 | 结果 |
| --- | --- | --- |
| GET / HEAD | `/` | React 入口 HTML |
| GET / HEAD | `/assets/<构建文件名>` | 构建生成的 JS/CSS |
| GET / HEAD | `/openpnp-test.png`、`/OpenPnP-LICENSE.txt` | 测试图和来源许可证 |
| GET / HEAD | `/healthz` | 页面服务描述，非设备就绪证明 |

`GET /healthz` 响应为 `200 application/json`：

```json
{"service":"smt-panel","mode":"simulation","machine_connected":false}
```

HEAD 不返回正文。所有响应设置 `Cache-Control: no-cache` 和
`X-Content-Type-Options: nosniff`。未实现的 API、缺失文件、目录列表请求返回 404；
POST/PUT/DELETE 等非只读方法返回 405，`Allow: GET, HEAD`。
没有历史路由的 HTML 回退，错误资源请求不会被静默变成 200 页面。

Wails 使用相同资源处理器，但其资源协议不是此 HTTP 端口，不提供局域网监听。
相机画面仍由前端直接读取已有 8766 预览服务，详见 [预览协议](preview-http-v1.md)。
这次没有增加 HTTP 到 Python TCP 的转换器，真实业务接口在后续明确后另行实现。

2026-10-07：代码已编写，按用户要求暂未执行新接口测试或板端部署。
