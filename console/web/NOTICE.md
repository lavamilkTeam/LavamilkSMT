# 第三方素材与组件

- `public/openpnp-test.png` 原样复制自已导入的 OpenPnP `src/main/resources/samples/pnp-test/pnp-test.png`，仅作为非实时测试画面使用。上游为 <https://github.com/openpnp/openpnp>，导入提交与来源见 [../UPSTREAM.md](../UPSTREAM.md)。保留上游许可证副本于 `public/OpenPnP-LICENSE.txt`。
- `src/shared/ui/` 组件由官方 shadcn CLI 生成，来源 <https://github.com/shadcn-ui/ui>。其上游采用 MIT 许可证；组件的 npm 依赖许可证以各包声明为准。
- 本次前端采用新写的 React 组件；没有把原 Java 应用的任务执行和视觉实现迁移到浏览器。
