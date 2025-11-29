# Memory Performance Visualizer — WebAssembly Spec

## Overview
A developer tool embedded in the Web (Wasm/JS) build of this project to observe memory behavior at runtime. It samples and visualizes heap size, Wasm memory growth, and event‑loop stalls, helping detect leaks, unexpected growth, and performance regressions.

## Goals
- Low‑overhead, opt‑in metrics for Web targets (Wasm preferred, JS fallback).
- Real‑time charts with 1–5s latency; exportable session data.
- Simple integration with existing `composeApp` and no server dependency.

## Non‑Goals
- Native/JVM profilers; production user analytics; precise allocation tracing.

## Target Environment
- Browser running `:composeApp:wasmJsBrowserDevelopmentRun` or `:composeApp:jsBrowserDevelopmentRun`.
- Modern Chromium/Firefox/Safari. Use feature detection to guard experimental APIs.

## Architecture
- Collection layer (platform specific):
  - Wasm: read `WebAssembly.Memory.buffer.byteLength`; observe memory growth via periodic polling.
  - JS fallback: `performance.memory.usedJSHeapSize` (Chromium only) and `performance.measureUserAgentSpecificMemory()` when available.
  - Event‑loop stalls: Long Task API via `PerformanceObserver('longtask')`; fallback to frame delta (`requestAnimationFrame`).
- Event bus: in‑memory ring buffers (typed arrays) with backpressure and sampling control.
- UI: Compose Multiplatform views in `composeApp/src/webMain/kotlin/.../visualizer/` rendering charts (line/area) and stats.
- Storage/export: JSON export/import; optional `localStorage` session cache.

## Metrics
- wasmMemoryBytes: current Wasm memory size.
- jsHeapUsedBytes: current JS heap (if available).
- longTaskMs: duration of long tasks (>50ms).
- fpsEstimate: rolling FPS from RAF deltas.
- marks: user/time‑based annotations (e.g., navigation, feature toggles).

## Sampling & Overhead Budgets
- Default interval: 500ms; configurable 100–2000ms.
- Max CPU overhead: <2% on mid‑range laptops; Memory overhead: <10MB buffers.
- Ring buffer capacity: 5,000 samples per metric; older entries dropped.

## UI/UX
- Panels: Memory (Wasm + JS), Event Loop (stalls/FPS), Annotations.
- Interactions: zoom/pan, hover values, toggle series, add mark, clear.
- Alerts: upward trend detection (sustained slope > X over Y seconds) and sudden spikes (>20%).
- Controls: start/stop, sampling rate, export/import JSON.

## Build & Runtime Configuration
- Enable via Gradle property `-Pvisualizer=true` or env `VISUALIZER=1`.
- Webpack define (via `composeApp/webpack.config.d/*.js`): inject `__VISUALIZER__` boolean.
- Kotlin wiring:
  - `expect object VisualizerConfig { val enabled: Boolean }` in `commonMain`.
  - `actual` in `webMain` reads `js("__VISUALIZER__ === true")` or safe default.
  - Visualizer UI attached behind `if (VisualizerConfig.enabled)` in web entrypoint.

## Data Model & Export Format
- Sample schema (newline‑delimited JSON or single JSON blob):
```json
{
  "version": 1,
  "startedAt": 1712345678901,
  "intervalMs": 500,
  "series": {
    "wasmMemoryBytes": [[t0,b0],[t1,b1]],
    "jsHeapUsedBytes": [[t0,h0]],
    "longTaskMs": [[t0,dur]],
    "fpsEstimate": [[t0,fps]]
  },
  "marks": [{"t": tMark, "label": "opened screen"}]
}
```

## Testing & Validation
- Unit: parsers, ring buffer, trend detector, JSON export/import (common tests).
- Web E2E (manual): open visualizer, trigger navigation/animations, verify charts update and export re‑imports.
- Performance: measure overhead with and without visualizer at 500ms sampling; ensure budgets are met.

## Risks & Limitations
- `performance.memory` not standardized; guard with feature detection.
- Precise GC/alloc tracing unavailable; rely on trends and stalls.
- Safari lacks Long Task API; fallback to RAF deltas.

## Milestones
1. Minimal collector + inline charts (Wasm memory + RAF FPS).
2. Long Task observer + JSON export.
3. UI polish, annotations, trend alerts.
4. JS heap/WASM memory unification and docs.
