# Wails 桌面壳与单应用全屏会话

本目录是 Linux 上位机的 Wails v2.16.0 入口，显示 `console/web` 的 React 界面。
默认普通窗口，`--kiosk` 使用无边框全屏窗口。当前 UI 仍是模拟原型，
不是完整的 OpenPnP Web 移植；真实运动、持久工程和控制 API 尚未接入。

## 运行边界

```text
console/web ──构建──► internal/webui/dist（生成文件，不提交）
                           ├── desktop：Wails + 系统 WebKit
                           └── backend/cmd/smt-panel：HTTP 静态服务

systemd ── smt-panel：浏览器访问入口
        ├─ smt-kiosk：Xorg → D-Bus → Openbox → Wails
        └─ controller / preview / updater：独立服务
```

Go 模块位于 `console/go.mod`，两个程序通过 `internal/webui.Handler()` 复用静态资源。
浏览器页面不导入 Wails runtime，不绑定 Go 方法；两种入口使用同一份前端源代码。
Wails 自带资源协议并不等于可供手机访问的 HTTP 端口，因此另设独立 `smt-panel`。
启动、关闭或重启面板不会启动/停止 Python、操作电机、恢复任务或执行升级。
后续业务接口仍归面板后端，机器执行权仍归 Python controller。

## 构建

构建机需要 Go 1.26+、Node.js 22.12+、npm。Linux 桌面构建还需要 C 编译器、
pkg-config、GTK3 和 WebKitGTK 开发库。Debian 13 / Ubuntu 24.04 示例：

```bash
sudo apt-get install --no-install-recommends build-essential pkg-config libgtk-3-dev libwebkit2gtk-4.1-dev
./console/build-panel.sh all
```

Ubuntu 20.04 若使用发行版提供的 WebKitGTK 4.0，开发包改为
`libwebkit2gtk-4.0-dev`。脚本优先选择 4.1，缺少时选择 4.0；不会混用发行版软件源。
脚本使用已锁定的 Go/npm 依赖，执行前端检查、构建、资源复制和 Go 编译。
采用 Wails 官方的手动构建方式，不需要全局安装 Wails CLI，也不生成 JS bindings。
`--kiosk` 是运行参数，不是构建参数。

```bash
./console/build-panel.sh web       # 仅静态 Web 服务，不需要 GTK/WebKit
./console/build-panel.sh desktop   # 仅 Wails，需图形开发库
./console/run-desktop.sh           # 在已有图形会话中打开窗口
./console/run-desktop.sh --kiosk   # 在已有图形会话中全屏
./console/run-desktop.sh --kiosk --software-rendering
./console/build/bin/smt-panel --listen 0.0.0.0:8080
```

输出在 `console/build/bin/`。发布时无需 Node.js；桌面程序仍依赖目标机 GTK/WebKit 运行库。
`build-panel.sh web` 支持通过 `GOOS=linux GOARCH=arm GOARM=7` 交叉编译 HTTP 服务。
Wails 使用 CGO，不能仅设置 GOARCH 就得到可用 ARM 程序；应在匹配目标系统 ABI 的
ARM 构建环境编译，或准备匹配的交叉 C 工具链与 sysroot。不要把开发机 amd64 二进制放上板运行。

## 无完整桌面的本机显示

`deploy/kiosk/` 提供 **Xorg + Openbox 最小窗口管理器** 的会话配置。
它不是 GNOME/KDE 桌面，也不是 Xvfb 虚拟屏；要在 HDMI/触摸屏显示，必须先有可用的
真实显示驱动。Openbox 负责全屏窗口管理，不启动桌面、任务栏、终端快捷键或应用菜单。
`dbus-run-session` 提供独立会话；Wails 或窗口管理器退出都会结束这次显示会话，
systemd 间隔 5 秒重启，120 秒内最多尝试 5 次，避免依赖缺失时无限快速重启。
这属于专用面板会话，不是对恶意本地使用者的安全隔离。

目标机器需要 `xserver-xorg-core`、`xinit`、`xauth`、`openbox`、`dbus`、`libpam-systemd`、
`x11-xserver-utils`、中文字体，以及构建时对应的 GTK3/WebKitGTK 运行库。
显示驱动按板型安装：仅有 framebuffer 时可能需要 `xserver-xorg-video-fbdev`，
是否可用取决于厂商 framebuffer 接口，不能仅凭 `/dev/fb0` 存在判断。
老式 Xorg 可能还需要发行版的 `xserver-xorg-legacy`；不要通过给所有用户显示设备权限来绕过故障。

代码已提供安装入口，**安装默认不会启动或启用服务**，也不会替换现有 Swing 服务：

```bash
# 在目标系统安装匹配架构的构建产物，--bin-dir 可指向发布包的 bin 目录
sudo ./tools/install_panel.sh --bin-dir /path/to/native/bin --kiosk

# 无屏设备只装 Web 服务时省略 --kiosk
sudo systemctl enable --now smt-panel.service
# 浏览器打开 http://上位机IP:8080/
```

本地显示使用 `:0` 和 `tty7`。确认设备没有其他显示管理器占用它们、Xorg 驱动可用后，
再启用 `sudo systemctl enable --now smt-kiosk.service`。安装器不自动关闭已有显示管理器。
`smt-kiosk` 使用独立无登录用户；PAM/logind 负责会话，video 组用于设备访问。
这套 service 的物理 seat/VT 权限仍需在目标系统验收。

若驱动不能提供 WebKit GPU 加速，将 `/etc/smt/kiosk.env` 中
`SMT_KIOSK_SOFTWARE_RENDERING=1`，再重启 kiosk。软件渲染会增加 CPU 开销，
不能代替一个正常工作的 Xorg 驱动，也不能使旧 WebKit 支持缺失的 Web 特性。

## 更新、维护与回退

安装器在替换二进制时先写临时文件再重命名，已运行进程在显式重启前继续使用旧程序；
它不是带事务回滚的 host-updater。发布前自行保留上一版 `bin/`。
重复安装保留 `/etc/smt/kiosk.env` 和 Openbox 配置，本次模板调整需人工合并。

```bash
sudo systemctl restart smt-panel smt-kiosk
journalctl -u smt-panel -u smt-kiosk -n 100 --no-pager
sudo systemctl reset-failed smt-kiosk  # 修复失败原因后重置启动限额
sudo systemctl stop smt-kiosk         # 临时停止本地界面
sudo systemctl disable --now smt-kiosk smt-panel  # 取消新面板自启动
```

回退版本：使用上一版 `bin/` 重新运行安装器，再显式重启新面板服务。
原有 `smt-console.service`（Swing/noVNC）及 Python 服务不受这些命令管理。
浏览器和桌面实例当前各自持有模拟状态，不同步任务；后续接入服务端状态后再统一。
触摸屏校准、屏幕旋转和屏幕键盘不属于这次接入，需按实际面板补充。

## 验证状态

2026-10-07：按用户要求优先完善代码，**尚未执行本次构建、测试和 kiosk 实机验收**，未部署新服务。
只读查看目标板得到：armv7l、厂商 4.4.35 内核、`/dev/fb0` 和 `/dev/fb1`，未见 `/dev/dri`；
GTK/WebKit/Xorg 运行条件尚未准备。不能据此声称 HDMI 已能显示 Wails。

下一阶段需执行：`npm run check`、新增导入边界规则的正反例检查、Go HTTP 测试和 vet、
桌面编译、实际 WebKit 中的中文/CSV 文件选择与下载/相机画面检查，以及整机启动、
进程异常恢复、SSH 停止服务和物理显示验收。新 Go 测试需先构建/复制 Web 资源。
前端使用 Tailwind 4 等现代 CSS，旧系统 WebKit 的兼容性尤其需要实际验收。

参考：[Wails 手动构建](https://wails.io/docs/guides/manual-builds/)、
[Wails Linux 依赖](https://wails.io/docs/guides/linux-distro-support/)。
