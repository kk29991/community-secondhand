import { fileURLToPath, URL } from 'node:url'
import { createRequire } from 'node:module'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 购物商城系统 - 后台管理端
// 开发端口 5175, 代理 /api -> http://localhost:9090

const require = createRequire(import.meta.url)

/**
 * 检测 esbuild 在当前环境能否正常启动。
 *
 * esbuild 的 JS API 必须启动一个子进程并通过管道 (stdio: 'pipe') 通信。
 * 某些受限环境 (例如本项目的构建沙箱) 会禁止子进程使用管道, 导致 esbuild
 * 一律抛出 `spawn EPERM` —— 连 buildSync / transformSync 的 worker 线程方案
 * 最终也是由 worker 再去 spawn 子进程, 同样不可用。
 *
 * 这个检测是精确的: 直接试一次 esbuild 转换, 成功即认为可用。
 * 普通开发机上检测通过, 下面的兼容性设置不生效, 项目使用 Vite 默认行为;
 * 受限环境中检测失败, 则改用 terser 压缩 + 关闭 esbuild 转换与依赖预构建,
 * 配合 scripts/sandbox-compat.mjs 提供的纯 JS define 实现。
 */
function isEsbuildUsable() {
  try {
    const esbuild = require('esbuild')
    const r = esbuild.transformSync('const __vite_probe = 1', { loader: 'js' })
    return !!(r && typeof r.code === 'string')
  } catch (e) {
    return false
  }
}

const esbuildUsable = isEsbuildUsable()

/** 公共配置 */
const config = {
  plugins: [vue()],
  resolve: {
    alias: [
      // dayjs 默认入口与 plugin/*.js 均为 UMD 格式, 浏览器无法直接执行。
      // 关闭依赖预构建(受限环境)后会被原样返回, 报
      //   "does not provide an export named 'default'"
      // 导致 element-plus 的日期/时间组件加载失败。统一指向 dayjs 自带的 ESM 版本。
      { find: /^dayjs$/, replacement: 'dayjs/esm/index.js' },
      { find: /^dayjs\/plugin\/(.+)\.js$/, replacement: 'dayjs/esm/plugin/$1/index.js' },
      {
        find: '@',
        replacement: fileURLToPath(new URL('./src', import.meta.url))
      }
    ]
  },
  build: {
    chunkSizeWarningLimit: 1500
  },
  server: {
    port: 5175,
    host: '127.0.0.1',
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true
      }
    }
  }
}

/** esbuild 不可用时的兜底配置: 整条链路完全不依赖 esbuild */
if (!esbuildUsable) {
  // 关闭 Vite 内置的 esbuild 转换 (单文件组件由 @vue/compiler-sfc 编译)
  config.esbuild = false
  // 改用 terser 压缩 (esbuild 压缩不可用)
  config.build.minify = 'terser'
  config.build.cssMinify = false
  config.build.target = 'esnext'
  config.build.terserOptions = {
    compress: { drop_console: false, drop_debugger: true },
    format: { comments: false }
  }
  // 依赖预构建同样依赖 esbuild; 本项目依赖均为 ESM,
  // 关闭自动发现后 Vite 会直接按源码把依赖提供给浏览器。
  config.optimizeDeps = {
    noDiscovery: true,
    include: []
  }
  // 受限环境(如本项目沙箱)下 Vite 的 HTML 监听器会因监视临时文件抛 EBUSY
  // 而把 dev server 整个带崩, 表现为浏览器加载到一半白屏。
  // 这里关闭文件监听, 改为手动刷新页面即可。
  config.server.watch = null
}

export default defineConfig(config)
