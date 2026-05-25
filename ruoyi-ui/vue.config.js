'use strict'
const path = require('path')

function resolve(dir) {
  return path.join(__dirname, dir)
}

const CompressionPlugin = require('compression-webpack-plugin')

const name = process.env.VUE_APP_TITLE || '管理系统' // 网页标题

const baseUrl = process.env.VUE_APP_BASE_URL || 'http://localhost:8080' // 后端接口

const port = process.env.port || process.env.npm_config_port || 80 // 端口

// vue.config.js 配置说明
//官方vue.config.js 参考文档 https://cli.vuejs.org/zh/config/#css-loaderoptions
// 这里只列一部分，具体配置参考文档
module.exports = {
  // 部署生产环境和开发环境下的URL。
  // 默认情况下，Vue CLI 会假设你的应用是被部署在一个域名的根路径上
  // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
  publicPath: process.env.NODE_ENV === "production" ? "/" : "/",
  // 在npm run build 或 yarn build 时 ，生成文件的目录名称（要和baseUrl的生产环境路径一致）（默认dist）
  outputDir: 'dist',
  // 用于放置生成的静态资源 (js、css、img、fonts) 的；（项目打包之后，静态资源会放在这个文件夹下）
  assetsDir: 'static',
  // 如果你不需要生产环境的 source map，可以将其设置为 false 以加速生产环境构建。
  productionSourceMap: false,
  transpileDependencies: ['quill'],
  // webpack-dev-server 相关配置
  devServer: {
    host: '0.0.0.0',
    port: port,
    open: true,
    proxy: {
      // detail: https://cli.vuejs.org/config/#devserver-proxy
      [process.env.VUE_APP_BASE_API]: {
        target: baseUrl,
        changeOrigin: true,
        pathRewrite: {
          ['^' + process.env.VUE_APP_BASE_API]: ''
        }
      },
      // springdoc proxy
      '^/v3/api-docs/(.*)': {
        target: baseUrl,
        changeOrigin: true
      }
    },
    disableHostCheck: true
  },
  css: {
    loaderOptions: {
      sass: {
        sassOptions: { outputStyle: "expanded" }
      }
    }
  },
  configureWebpack: config => {
    config.name = name
    config.resolve = {
      ...config.resolve,
      alias: {
        ...((config.resolve && config.resolve.alias) || {}),
        '@': resolve('src')
      }
    }

    // 生产环境：使用 contenthash 确保文件内容不变则文件名不变（增量部署核心）
    if (process.env.NODE_ENV === 'production') {
      config.output.filename = 'static/js/[name].[contenthash:8].js'
      config.output.chunkFilename = 'static/js/[name].[contenthash:8].js'
    }

    config.plugins = [
      ...(config.plugins || []),
      // http://doc.ruoyi.vip/ruoyi-vue/other/faq.html#使用gzip解压缩静态文件
      new CompressionPlugin({
        cache: false,                                  // 不启用文件缓存
        test: /\.(js|css|html|jpe?g|png|gif|svg)?$/i,  // 压缩文件格式
        filename: '[path][base].gz[query]',            // 压缩后的文件名
        algorithm: 'gzip',                             // 使用gzip压缩
        minRatio: 0.8,                                 // 压缩比例，小于 80% 的文件不会被压缩
        deleteOriginalAssets: false                    // 压缩后删除原文件
      }),
      // 固定模块 ID，避免因模块顺序变化导致 hash 变化
      new (require('webpack')).HashedModuleIdsPlugin()
    ]
  },
  chainWebpack(config) {
    config.plugins.delete('preload') // TODO: need test
    config.plugins.delete('prefetch') // TODO: need test

    // set svg-sprite-loader
    config.module
      .rule('svg')
      .exclude.add(resolve('src/assets/icons'))
      .end()
    config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(resolve('src/assets/icons'))
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]'
      })
      .end()

    config.when(process.env.NODE_ENV !== 'development', config => {
      config
        .plugin('ScriptExtHtmlWebpackPlugin')
        .after('html')
        .use('script-ext-html-webpack-plugin', [{
          // `runtime` must same as runtimeChunk name. default is `runtime`
          inline: /runtime\..*\.js$/
        }])
        .end()

      // CSS 使用 contenthash
      config.plugin('extract-css').tap(args => {
        if (args[0]) {
          args[0].filename = 'static/css/[name].[contenthash:8].css'
          args[0].chunkFilename = 'static/css/[name].[contenthash:8].css'
        }
        return args
      })

      // 细粒度 splitChunks：将稳定的第三方库独立拆分，确保业务代码变更不影响它们的 hash
      config.optimization.splitChunks({
        chunks: 'all',
        maxInitialRequests: Infinity,
        minSize: 20000,
        cacheGroups: {
          // Vue 全家桶（vue、vue-router、vuex）—— 极少变化
          vue: {
            name: 'chunk-vue',
            test: /[\\/]node_modules[\\/](vue|vue-router|vuex)[\\/]/,
            priority: 40,
            reuseExistingChunk: true
          },
          // Element UI —— 版本固定后不会变化
          elementUI: {
            name: 'chunk-elementUI',
            test: /[\\/]node_modules[\\/]_?element-ui(.*)/,
            priority: 30,
            reuseExistingChunk: true
          },
          // Echarts —— 体积大，单独拆分
          echarts: {
            name: 'chunk-echarts',
            test: /[\\/]node_modules[\\/](echarts|zrender)(.*)/,
            priority: 30,
            reuseExistingChunk: true
          },
          // WangEditor 富文本编辑器
          wangeditor: {
            name: 'chunk-wangeditor',
            test: /[\\/]node_modules[\\/]@wangeditor(.*)/,
            priority: 30,
            reuseExistingChunk: true
          },
          // 其他第三方库
          libs: {
            name: 'chunk-libs',
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: 'initial',
            reuseExistingChunk: true
          },
          // 公共组件
          commons: {
            name: 'chunk-commons',
            test: resolve('src/components'),
            minChunks: 3,
            priority: 5,
            reuseExistingChunk: true
          }
        }
      })
      config.optimization.runtimeChunk('single')
    })
  }
}
