// Inject __VISUALIZER__ flag into the build from environment
// Usage: VISUALIZER=1 ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
;(function(config) {
  const webpack = require('webpack')
  const enabled = process.env.VISUALIZER === '1' || process.env.VISUALIZER === 'true'
  config.plugins = config.plugins || []
  config.plugins.push(new webpack.DefinePlugin({
    __VISUALIZER__: JSON.stringify(!!enabled),
  }))
})(config);

