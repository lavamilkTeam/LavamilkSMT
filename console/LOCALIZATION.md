# 简体中文本土化

控制端默认面向中国大陆用户。按贴片机操作语境编写和校订中文，统一操作名称、
错误原因、处理步骤及参数说明，避免逐词照搬英文和中英重复标题。

## 覆盖范围

- 英文主资源的 4,382 个键均有简体中文对应值；包含新增文案及显示名称。
- 菜单、任务和单板操作、相机预览、点动控制、飞达及执行器配置向导。
- 基准点定位、取料与贴装、反向间隙、接触探测、吸嘴头和相机校准提示。
- 视觉流程的步骤名称、注解说明、参数显示名和枚举选项。
- 问题与解决方案、校准帮助和结果报告、运行状态、导入及异常提示。
- 新用户的中文默认值、中文欢迎说明和 Swing 内置控件的语言初始化顺序。

用户自定义名称、部件 ID、类名、配置属性、G 代码及其注释、协议和导入格式字段保留原文。
视觉步骤同时显示中文用途和原类名，便于查阅参数与对照流程 XML。
上游 README、许可证、历史发布说明和外链 Wiki 保留原文；欢迎页提供中文版本。
资源键覆盖率不等同于逐页实机验收，实际硬件和第三方驱动返回的文本仍取决于其实现。

## 术语

| 上游术语 | 中文用法 |
| --- | --- |
| Job / Placement | 贴装任务 / 贴装项（动作目标使用“贴装位置”） |
| Board / Panel | 单板 / 拼板 |
| Feeder / Tape / Sprocket hole | 飞达 / 料带 / 定位孔 |
| Head | 贴装头 |
| Nozzle / NozzleTip | 吸嘴组件 / 吸嘴头；一般取放动作可简称“吸嘴” |
| Fiducial | 基准点（Mark） |
| Bottom vision / Down-looking camera | 上视对位 / 下视相机 |
| Home / Jog / Park | 回零 / 点动 / 停靠 |
| Enable / Disable | 使能 / 禁用（设备电源通断另行区分） |
| Safe Z / Safe Z Zone | Z 轴安全高度 / Z 轴安全区间 |
| Units per pixel | 像素当量 |
| Backlash / Runout / Jerk | 反向间隙 / 径向偏心或跳动 / 加加速度 |
| Pipeline / Stage | 视觉流程 / 处理步骤 |
| Dismiss / Reopen | 忽略 / 重新打开 |

## 实现边界

英文与中文资源位于 `frontend/src/main/resources/org/openpnp/`。
已有资源键保留；从硬编码提取的文案使用 `Local.<英文原文 SHA-256 前 16 位>`，
英文主资源保存原文，中文资源保存对应译文。同一原文可复用同一键。
新增独立功能可使用 `Job.Status.*` 这样的具名键。

`Translations.getString()` 按当前语言加载已缓存的资源包，避免在用户偏好加载前
永久锁定系统语言。`format()` 保留格式参数；`translate()` 在注解显示时查找对应文案。

诊断信息使用 `Translations.Message` 同时保留原文与显示文本。
`Solutions.Issue` 用原文计算指纹，中文显示不会改变已解决／已忽略问题的身份。
带条件分支的诊断消息可嵌套，原文参数同样保持稳定。

下拉框和表格只转换枚举的显示名称，不修改枚举实例、`name()`、XML 值或驱动指令。
视觉参数只设置 `PropertyDescriptor.displayName`，属性名及读写方法保持原样。
没有改动运动轨迹、坐标算法、视觉算法或板端通信协议。

## 修改后的检查

在仓库根目录运行（可将本地 Java 路径换成已安装的 `java`）：

```bash
toolchains/openpnp/jdk/bin/java console/tools/NormalizeTranslations.java \
  console/frontend/src/main/resources/org/openpnp/translations.properties \
  console/frontend/src/main/resources/org/openpnp/translations_zh_CN.properties
./console/build-frontend.sh -Dtest=ChineseLocalizationTest,LocalisationTest
./console/build-frontend.sh
```

检查涵盖重复或缺失资源键、格式参数、代码中的资源引用、语言切换、
诊断指纹稳定性、枚举显示与模型值分离，以及视觉步骤注解和参数名覆盖。
字体检查需要系统具备中文字体，Linux 可使用 Noto CJK。

视觉验收记录位于工作区的 `artifacts/localization/`，不纳入版本控制。
重新构建后重启 `console/dev-frontend.sh`，浏览器重新连接现有开发预览地址即可。

2026-09-14 验证：最终完整构建通过，259 项测试全部通过，无跳过项；
另检查了中文主窗口、问题与解决方案页和视觉流程编辑器的渲染结果。
首次完整运行曾出现脚本并发测试获取 Python 引擎失败，针对性复跑与最终完整运行均通过，
未修改该测试或脚本引擎工厂来规避失败。
