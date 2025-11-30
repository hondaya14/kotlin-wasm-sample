This is a Kotlin Multiplatform project targeting Web, Desktop (JVM).

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:
- for the Wasm target (faster, modern browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
    ```
- for the JS target (slower, supports older browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:jsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
    ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).

### Memory Performance Visualizer (Web)

An opt‑in developer tool to observe memory and event‑loop behavior at runtime. It samples and renders:
- Wasm memory size, JS heap (Chromium), FPS estimate, and Long Tasks.

Enable via either Gradle property or env var when running dev servers:
- Wasm: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun -Pvisualizer=true` or `VISUALIZER=1 ./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- JS: `./gradlew :composeApp:jsBrowserDevelopmentRun -Pvisualizer=true` or `VISUALIZER=1 ./gradlew :composeApp:jsBrowserDevelopmentRun`

What you get:
- Overlay panel (top‑right) with Memory and Event Loop charts.
- Controls: export/import JSON, save/restore to localStorage, clear, time‑window (zoom), pan offset, JS series toggle, add mark.
- Alerts: simple upward trend and spike detection on memory.

Browser support notes:
- JS heap via `performance.memory` is Chromium‑only; guarded when unavailable.
- Long Task API may be missing (e.g., Safari); a RAF‑delta fallback is used.

Manual validation:
- Open the app with the flag enabled; observe charts updating.
- In DevTools console, run a blocking snippet to see long tasks increase:
  `const t=Date.now(); while(Date.now()-t<200){}`
- Click Export to download a JSON session; Import to restore it.
- Optionally Save Local to persist the session and Restore Local to reload it later.
