/**
 * 生产构建入口
 * ---------------------------------------------------------------------------
 * 为什么不用 `vite build` 命令直接跑?
 *
 * Vite CLI 在读取配置文件时, 会先用 esbuild 把 vite.config.js 打包一遍
 * (见 vite/dist/node/chunks/dep-*.js 的 bundleConfigFile)。而 esbuild 的
 * JS API 需要子进程通过管道 (stdio: 'pipe') 通信, 当前构建环境的沙箱禁止
 * 子进程使用管道 stdio, 因此 `vite build` 会直接报 `spawn EPERM` —— 即使
 * 配置里写了 esbuild: false 也没用, 因为配置本身还没被读出来就失败了。
 *
 * 解决办法:
 *   1. sandbox-compat.mjs: 让 Vite 跳过 Windows 下的 `net use` 探测 (同样依赖管道);
 *   2. 用 Node 原生 import 读取配置 (不经 esbuild), 再以 `configFile: false`
 *      调用 Vite 的 JS API, 把配置对象直接传进去。
 *
 * 这样整条构建链路完全不经过 esbuild: 单文件组件由 @vue/compiler-sfc 编译,
 * 代码压缩由 terser 完成。
 *
 * 在 esbuild 可以正常工作的普通环境里, 直接执行 `npx vite build` 或
 * `npm run build:vite` 同样可以构建成功, 两者共用同一份 vite.config.js。
 */
import { applySandboxCompat } from './sandbox-compat.mjs'

applySandboxCompat()

const { build } = await import('vite')
const { default: config } = await import('../vite.config.js')

try {
  await build({ ...config, configFile: false })
} catch (e) {
  console.error('\n[build] 构建失败:', e && e.message ? e.message : e)
  if (e && e.stack) console.error(e.stack)
  process.exit(1)
}
