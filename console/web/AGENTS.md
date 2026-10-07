# Web module boundaries

Apply `$code-boundary-standards` to coding tasks.

Keep the existing Feature First layout. `app`, each `features/<name>`,
`shared/ui`, and `shared/lib` are modules. Root files within each module are
focused public entry points; any nested implementation directory is private.
Import another module only through its public root files. Tests use these same
entry points; do not import another module's test files or private directories.

Dependencies point from `app` to features/shared and from features to shared.
Explicit feature-to-feature public imports are allowed without cycles.
Web must work in an ordinary browser without Wails bindings or desktop imports.
Generated shadcn components keep their framework-owned flat file layout.

`npm run lint:boundaries` enforces these rules. `npm run check` combines the
boundary check, existing frontend tests, type checking and production build.
The new boundary configuration still needs a negative-import check during the
next validation phase; don't claim it has been validated until that is done.
