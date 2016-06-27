module.exports = {
  entry: [
    './src/index.js'
  ],
  output: {
    filename: '../public/index.js'
  },
  module: {
    loaders: [{
      test: /\.elm$/,
      exclude: [/elm-stuff/, /node_modules/],
      loader: 'elm-webpack'
    }]
  }
};
