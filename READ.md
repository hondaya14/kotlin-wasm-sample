使い方（Memory Performance Visualizer）

- Visualizer を有効化して Web アプリ（Wasm/JS）を起動します。
  - Wasm: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun -Pvisualizer=true`
  - もしくは: `VISUALIZER=1 ./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
  - JS: `./gradlew :composeApp:jsBrowserDevelopmentRun -Pvisualizer=true`
  - もしくは: `VISUALIZER=1 ./gradlew :composeApp:jsBrowserDevelopmentRun`

できること
- メモリ（Wasm、Chromium では JS Heap）、FPS、Long Task をオーバーレイで可視化
- 操作: Start/Stop、Interval（ms）変更、Window（s）変更、Pan offset（s）でスクロール、JS系列の表示切替
- Export/Import（JSON）、Save/Restore Local（localStorage 保存/復元）、Mark 追加
- アラート: メモリの上昇トレンド、スパイク検出

検証手順（手動）
- 有効化して起動し、グラフが更新されることを確認
- DevTools コンソールでブロッキングを実行し Long Task 増加を確認:
  `const t=Date.now(); while(Date.now()-t<200){}`
- Export で JSON をダウンロード → Import で復元
- Save Local → Restore Local で localStorage 復元

注意事項
- JS Heap は Chromium 系のみ。Long Task API が無い環境では RAF の遅延から代替検出
- Wasm メモリはグローバル `wasmMemory` が利用可能な場合にサンプリング（未提供環境では非表示）
