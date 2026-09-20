# 持续集成

GitHub Actions 对 `main`、`kihon` 的相关路径推送，以及目标为这两个分支的 PR 执行检查。
两个 workflow 均支持 `workflow_dispatch`；GitHub 的手动运行入口通常要求 workflow 已存在于默认分支。
只授予 `contents: read`，同一分支的新运行取消旧运行，第三方 Actions 固定到完整提交 SHA。
CI 不连接局域网上位机、不烧录 MCU，也不自动部署或执行真实运动。

## 上位机：Host CI

入口：`.github/workflows/host-ci.yml`。

- Python 控制服务：Ubuntu 24.04，Python 3.11 和 3.13。
- 构建 wheel 和源码包，再安装 wheel 及 `network` 依赖；测试不是从 editable 源码运行。
- 安装 NumPy、无界面 OpenCV，运行 `controller/tests` 全套测试；零测试或任何跳过项均失败。
- 运行已安装的 `smt-controller --demo` 和两个命令行入口的帮助检查。
- Java 控制界面：Temurin 17、Maven、Noto CJK 字体；运行 `console/build-frontend.sh`，包含 Checkstyle 和完整测试。
- 保存 Python 分发包、Java 测试报告、包含 JAR/依赖库/示例/许可证的桌面运行包，保留 14 天。

Python 的 CI 视觉依赖版本保存在 `tools/ci/requirements-controller.txt`。
它们是 Linux x86_64 测试环境，不能代替 ARM 板上的原生库、相机时序或实际运动验收。
桌面运行包不含 Java 运行时和板端服务安装器，不是自动部署包。
当前没有纳入未提交的网页前端或尚未实现的 Go 后端。

本地使用独立 Python 3.11/3.13 虚拟环境，在仓库根目录运行：

```bash
python -m pip install -r tools/ci/requirements-controller.txt
python -m build controller --outdir dist/controller
python -m pip install './dist/controller/smt_controller-0.1.0-py3-none-any.whl[network]'
python -m pip check
python tools/ci/test_controller.py
smt-controller --demo
./console/build-frontend.sh -Djava.awt.headless=true
```

版本更新后 wheel 文件名随之变化；workflow 自动选择本次构建的 wheel。

## 下位机：Firmware CI

入口：`.github/workflows/firmware-ci.yml`。

**当前仓库尚无可编译的下位机工程，不会产生固件。** readiness 作业在运行摘要明确写出
`Firmware is NOT BUILT`，真正的 ARM 编译作业显示为 skipped；readiness 成功仅代表入口检查完成。
若提交了 C/C++、汇编或 `.ioc` 却没有 `firmware/mainboard/CMakeLists.txt`，检查直接失败。

后续接入真实 STM32F407 工程时：

1. 提交板级 `firmware/mainboard/CMakeLists.txt`、源码、启动文件、链接脚本及必要且有许可证的依赖。
2. CMake 工程要能使用外部工具链文件，并生成至少一个后缀为 `.elf` 的固件可执行文件。
3. CI 自动进入 ARM GCC 编译作业，用 Ubuntu 24.04 软件源安装 Arm GCC、newlib、CMake 和 Ninja，打印工具版本。
4. 只有产生 32 位小端 ARM hard-float 可执行 ELF，才导出非空 BIN/HEX 并上传；不以空目录或静态库充当固件。

通用工具链位于 `firmware/cmake/arm-none-eabi-gcc.cmake`，设置 Cortex-M4F、Thumb、
FPv4-SP-D16 和 hard-float。板级工程负责 `STM32F407xx` 宏、真实时钟、引脚、Flash 分区、
启动和中断向量、SRAM/CCM 划分，以及链接库选项；CI 不猜测这些硬件参数。
未来如需要其他 MCU，应调整工具链与 workflow，不能直接沿用 F407 参数。
当前编译器随 Ubuntu 软件源更新，尚未固定整套固件 SDK 或宣称可重复生成逐字节相同的固件。

```bash
python3 -m unittest discover -s tools/ci/tests -v
python3 tools/ci/check_firmware.py
cmake -S firmware/mainboard -B build/firmware -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$PWD/firmware/cmake/arm-none-eabi-gcc.cmake" \
  -DCMAKE_BUILD_TYPE=Release -DCMAKE_EXPORT_COMPILE_COMMANDS=ON
cmake --build build/firmware --parallel 2
python3 tools/ci/collect_firmware.py build/firmware dist/firmware
```

后三条命令需先有真实板级工程及 Arm GCC。编译通过不代表板子已经能够烧录或安全运行。
