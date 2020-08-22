const path = require('path');

module.exports = {
  mode: "development",
  devtool: "inline-source-map",
  entry: "./js/index.ts",
  output: {
    publicPath: 'public',
    filename: "bundle.js",
    path: path.resolve(__dirname, 'public')
  },
  resolve: {
    // Add `.ts` as a resolvable extension.
    extensions: [".ts", ".js"],
    alias: {
      templates: __dirname + '/templates'
    }
  },
  module: {
    rules: [
      // all files with a `.ts` extension will be handled by `ts-loader`
      { test: /\.ts$/, loader: "ts-loader" },
      { test: /\.hbs/,
        loader: "handlebars-loader",
        query: { 
          helperDirs: [
            __dirname + "/templates/helpers"
          ]
        }
      },
      {
        test: /\.css$/i,
        use: ['style-loader', 'css-loader'],
      },
    ]
  },
};