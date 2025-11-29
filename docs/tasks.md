# Memory Performance Visualizer — Task Plan

Purpose: implement the Web (Wasm/JS) memory and event‑loop visualizer described in docs/SPEC.md, delivered in scoped milestones with clear acceptance criteria, file locations, and run/test commands.

## Milestones Overview
- M1: Minimal collector + inline charts (Wasm memory + RAF FPS)
- M2: Long Task observer + JSON export/import
- M3: UI polish, annotations, trend alerts
- M4: JS heap/Wasm memory unification and docs

## Conventions
- Module: `composeApp`
- Package: `co.hondaya.kotlin_wasm_sample`
- Web UI code: `composeApp/src/webMain/kotlin/.../visualizer/`
- Common code: `composeApp/src/commonMain/kotlin/.../visualizer/`
- Tests: `composeApp/src/commonTest/kotlin/.../visualizer/`
- Enable flag: Gradle `-Pvisualizer=true` → Webpack define `__VISUALIZER__` → Kotlin `VisualizerConfig.enabled`

## Build & Run
- Wasm dev run: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- JS dev run: `./gradlew :composeApp:jsBrowserDevelopmentRun`
- Enable visualizer: append `-Pvisualizer=true`
- All checks: `./gradlew :composeApp:check`

## Milestone 1 — Minimal Collector + Inline Charts
Scope focuses on: Wasm memory sampling, FPS estimate via `requestAnimationFrame`, ring buffers, and a basic Compose UI panel; behind a feature flag.

Implementation tasks
- Config wiring
  - Add `expect object VisualizerConfig { val enabled: Boolean }` in `commonMain`.
  - Add `actual object VisualizerConfig` in `webMain` using `js("typeof __VISUALIZER__ !== 'undefined' && __VISUALIZER__ === true")` with safe default `false`.
  - Create Webpack define file `composeApp/webpack.config.d/visualizer-define.js` that injects `__VISUALIZER__` based on Gradle property `visualizer`.
- Data structures
  - Implement lock‑free ring buffers backed by typed arrays for numeric series: capacity 5,000; drop oldest on overflow.
  - Define metric enums/ids and a data model for samples and marks.
- Collectors (webMain)
  - Wasm: periodically read `WebAssembly.Memory.buffer.byteLength` (guard access), sampling interval default 500ms, configurable.
  - FPS estimate: compute rolling FPS from RAF deltas.
- UI (webMain)
  - Add minimal panel composables: Memory (Wasm), Event Loop (FPS). Simple line/area chart rendering using Canvas/SVG in Compose for Web.
  - Add start/stop and interval control; attach to web entrypoint behind `if (VisualizerConfig.enabled)`.
- Entry integration
  - Wire visualizer UI into `composeApp/src/webMain/kotlin/.../main.kt` as an overlay or route.

Acceptance criteria
- When run with `-Pvisualizer=true`, a panel renders with two live‑updating charts: Wasm memory (bytes) and FPS.
- Default sampling interval is 500ms; can be changed between 100–2000ms.
- Ring buffer holds 5,000 samples per series and drops oldest when full.
- Disabled by default (no overhead, no UI) when flag is off.

## Milestone 2 — Long Tasks + Export/Import
Add event‑loop stall detection and JSON session export/import.

Implementation tasks
- Long task collection
  - Use `PerformanceObserver({type: 'longtask'})` when available to capture `duration` (>50ms).
  - Fallback: detect stalls by RAF delta outliers (e.g., > 80ms) and record a `longTaskMs` event.
- Export/Import
  - Define JSON schema per SPEC: version, start time, interval, series arrays, marks.
  - Implement export to single JSON blob; implement import to refill buffers and re‑render.
- UI
  - Add buttons for Export (download JSON) and Import (file input), and a clear action for buffers.

Acceptance criteria
- Long tasks appear as discrete events with durations when the page is stressed (e.g., blocking loop in console).
- Export produces a JSON file matching the schema; Import restores the session and charts reflect imported data.

## Milestone 3 — UI Polish, Annotations, Trend Alerts
Enhance ergonomics and add mark/alert features.

Implementation tasks
- Panels & interactions
  - Split into panels: Memory (Wasm + JS if present), Event Loop (stalls + FPS), Annotations.
  - Add zoom/pan, hover values, and series toggles.
- Annotations
  - Allow adding user marks with labels and timestamps; render vertical markers; include in export.
- Trend detection
  - Implement upward trend detection and sudden spike alerts per SPEC thresholds (configurable).
- Storage
  - Optional `localStorage` cache of recent session with opt‑in restore.

Acceptance criteria
- Users can add/clear marks; marks are exported/imported.
- Zoom/pan works on time series; hovering shows values at cursor.
- Trend alert UI indicates sustained upward growth and spikes with configurable thresholds.
- Optional local session restore works when enabled.

## Milestone 4 — JS Heap + Unification + Docs
Surface JS heap metrics when available, unify view, and document limitations.

Implementation tasks
- JS heap metrics
  - Chromium: `performance.memory.usedJSHeapSize` (guarded); experimental: `performance.measureUserAgentSpecificMemory()`.
  - Feature detect and record `jsHeapUsedBytes` samples when supported.
- Unification
  - Present Wasm and JS memory series in a unified Memory panel with toggles; consistent units/legends.
- Documentation
  - Add README section describing feature flag, supported browsers, limitations, and manual validation steps.

Acceptance criteria
- On Chromium, JS heap series appears when available and can be toggled; on other browsers it’s safely absent.
- Docs explain enablement, browser support, and caveats (e.g., Safari lacks Long Task API).

## File & Code Map (planned)
- Config
  - `composeApp/src/commonMain/kotlin/co/hondaya/kotlin_wasm_sample/visualizer/VisualizerConfig.kt` (expect)
  - `composeApp/src/webMain/kotlin/co/hondaya/kotlin_wasm_sample/visualizer/VisualizerConfig.web.kt` (actual)
  - `composeApp/webpack.config.d/visualizer-define.js` (inject `__VISUALIZER__`)
- Data & buffers (common)
  - `RingBuffer.kt`, `Metrics.kt`, `SessionModel.kt`, `TrendDetector.kt`, `JsonCodec.kt`
- Collectors (webMain)
  - `WasmMemoryCollector.kt`, `FpsCollector.kt`, `LongTaskCollector.kt`, `JsHeapCollector.kt`
- UI (webMain)
  - `VisualizerPanel.kt`, `MemoryPanel.kt`, `EventLoopPanel.kt`, `AnnotationsPanel.kt`, `Charts.kt`
- Entry
  - `composeApp/src/webMain/kotlin/co/hondaya/kotlin_wasm_sample/main.kt` (attach behind flag)
- Tests (commonTest)
  - `RingBufferTest.kt`, `TrendDetectorTest.kt`, `JsonCodecTest.kt`

## Testing & Validation
- Unit tests: ring buffer behavior, trend detection math, JSON export/import round‑trip.
- Manual web E2E: open visualizer, navigate UI, induce long tasks (console `while(Date.now()-t<200){}`), verify charts update and export/import integrity.
- Performance checks: measure overhead with/without visualizer at 500ms; target <2% CPU and <10MB memory for buffers.

## Risks & Mitigations
- `performance.memory` non‑standard → guard with feature detection; don’t crash when absent.
- Safari lacks Long Task API → fall back to RAF stall detection.
- Limited access to raw GC/alloc data → focus on trends and stalls; document limitations.

## Definition of Done
- All milestone acceptance criteria met.
- Feature flag default off; no regressions when disabled.
- Tests pass (`./gradlew :composeApp:check`).
- README updated with usage and browser support.
- Progress notes added in `docs/progress/` for the work performed.

## Quick Start (dev)
- Wasm: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun -Pvisualizer=true`
- JS: `./gradlew :composeApp:jsBrowserDevelopmentRun -Pvisualizer=true`

