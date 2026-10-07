// Feature First: each module's root files are focused public entry points.
// Nested directories are private; no new barrels or alias workarounds required.
const modules = ["app", "features/camera", "features/machine", "features/job",
  "features/configuration", "features/logs", "shared/ui", "shared/lib"];
const tests = String.raw`(?:\.(?:ui\.)?test\.[cm]?[jt]sx?$|/tests/)`;

module.exports = {
  forbidden: [
    ...modules.map((name) => ({
      name: `public-entrypoints-${name.replaceAll("/", "-")}`,
      severity: "error",
      from: { pathNot: `^src/${name}/` },
      to: { path: `^src/${name}/[^/]+/` },
    })),
    ...modules.map((name) => ({
      name: `tests-use-public-api-${name.replaceAll("/", "-")}`,
      severity: "error",
      from: { path: `^src/${name}/.*${tests}` },
      to: { path: `^src/${name}/[^/]+/`, pathNot: `^src/${name}/tests/` },
    })),
    ...modules.map((name) => ({
      name: `no-cross-module-tests-${name.replaceAll("/", "-")}`,
      severity: "error",
      from: { pathNot: `^src/${name}/` },
      to: { path: `^src/${name}/.*${tests}` },
    })),
    { name: "tests-are-private", severity: "error",
      from: { pathNot: tests }, to: { path: tests } },
    { name: "shared-is-independent", severity: "error",
      from: { path: "^src/shared/" }, to: { path: "^src/(app|features)/" } },
    { name: "features-do-not-import-app", severity: "error",
      from: { path: "^src/features/" }, to: { path: "^src/app/" } },
    { name: "no-wails-runtime-in-shared-web", severity: "error",
      from: {}, to: { path: "(^|/)(wailsjs|desktop)(/|$)" } },
    { name: "no-circular", severity: "error", from: {}, to: { circular: true } },
    { name: "no-unresolved", severity: "error", from: {}, to: { couldNotResolve: true } },
  ],
  options: {
    doNotFollow: { path: "node_modules" },
    tsConfig: { fileName: "tsconfig.json" },
    tsPreCompilationDeps: true,
    enhancedResolveOptions: { extensions: [".ts", ".tsx", ".js", ".jsx", ".json", ".css"] },
  },
};
