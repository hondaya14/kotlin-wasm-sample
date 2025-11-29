# Repository Guidelines

## Project Structure & Module Organization
- Single Gradle project with one module: `composeApp`.
- Shared Kotlin code lives in `composeApp/src/commonMain/kotlin/`.
- Platform code:
  - JVM (Desktop): `composeApp/src/jvmMain/kotlin/`
  - JS (browser): `composeApp/src/jsMain/kotlin/`
  - Wasm (browser): `composeApp/src/wasmJsMain/kotlin/`
  - Web resources (HTML/CSS): `composeApp/src/webMain/resources/`
- Entry points: Desktop `co.hondaya.kotlin_wasm_sample.MainKt`; Web `composeApp/src/webMain/kotlin/.../main.kt`.

## Build, Test, and Development Commands
- Desktop (JVM) run: `./gradlew :composeApp:run`
- Web (Wasm, modern browsers): `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- Web (JS, wider browser support): `./gradlew :composeApp:jsBrowserDevelopmentRun`
- Build all targets: `./gradlew :composeApp:build`
- Run checks/tests: `./gradlew :composeApp:check`

## Coding Style & Naming Conventions
- Kotlin style: 4‑space indent, no tabs; keep lines ≲120 chars.
- Packages use `co.hondaya.kotlin_wasm_sample`; match directory structure.
- Names: classes/objects `PascalCase`, functions/props `camelCase`, constants `UPPER_SNAKE_CASE`.
- Filenames match top‑level declarations (e.g., `Greeting.kt`).
- Compose: prefer state hoisting and previewable small composables; keep platform APIs in platform source sets.

## Testing Guidelines
- Framework: `kotlin.test` (configured in `commonTest`).
- Locations: `composeApp/src/commonTest/kotlin/` for shared tests; platform tests under `jvmTest`, `jsTest`, `wasmJsTest` if added.
- Naming: files end with `Test.kt`; tests use `@Test` with descriptive method names.
- Commands: `./gradlew :composeApp:check`, or target‑specific like `:composeApp:jvmTest` / `:composeApp:jsTest`.

## Commit & Pull Request Guidelines
- Commits: imperative mood, concise subject, optional scope prefix.
  - Example: `composeApp: add Greeting preview for JVM`
- Include rationale and screenshots for UI changes (Desktop/Web).
- PRs: clear description, reproduction/run commands, linked issues (e.g., `Closes #123`), and platform(s) affected.

## Environment & Tooling
- Use the Gradle wrapper; Java 17+ recommended. IntelliJ IDEA/Android Studio with Kotlin Multiplatform support works best.
- Wasm requires a modern browser. Local config lives in `gradle.properties`. Optional tool versions can be managed via `mise.toml`.

## Agent Workflow & Docs
- Before starting any task, read `docs/SPEC.md` (project specification).
- Save task progress notes as plain text under `docs/progress/`.
- Prefer one file per task; include context, decisions, and next steps.
- Reference the spec and progress notes when making changes and in PRs.

## Git Workflow
- After completing a task, stage and commit changes, then push.
- Follow the commit message style in "Commit & Pull Request Guidelines" above.
- Keep commits atomic per task; avoid bundling unrelated changes.
- Typical flow:
  - `git add -A`
  - `git commit -m "composeApp: <concise change summary>"`
  - `git push` (to the current branch or feature branch as appropriate)
- If a task spans multiple logical milestones, make incremental, coherent commits per milestone.
