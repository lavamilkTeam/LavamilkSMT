# 适配器接入位置

当前只有 simulation，可离线演示，不会打开摄像头、串口或网络端口。

后续增加：
- usb_camera：实现 Camera，统一持有相机，采集线程和有界缓冲。
- serial_controller / tcp_controller：实现 MotionController，区分 ACK 与完成反馈。
- opencv_locator：实现 Locator，将流水线提交到受限 worker 执行。
- storage：保存任务、配方和标定数据，实测数据写入仓库外或根目录 local/。

真实设备初始化、显式回零、停止/取消、状态重同步和异常恢复确定前，不启用真实驱动。
不得将模拟驱动的 sleep 或固定识别结果用于真实设备。
