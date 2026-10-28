/**
 * 开发服务器入口 (端口 5175)
 * ---------------------------------------------------------------------------
 * 与 scripts/build.mjs 同理: 先做沙箱兼容处理, 再用 Node 原生 import 读配置,
 * 以 `configFile: false` 调用 Vite 的 JS API, 避免 Vite CLI 用 esbuild 打包
 * 配置文件而触发 spawn EPERM。
 *
 * vite.config.js 中设置了 optimizeDeps.noDiscovery = true, 跳过依赖预构建
 * (预构建同样依赖 esbuild), 依赖由 Vite 直接以 ESM 源码提供给浏览器。
 */
import { applySandboxCompat } from './sandbox-compat.mjs'

applySandboxCompat()

const { createServer } = await import('vite')
const { default: config } = await import('../vite.config.js')

const server = await createServer({ ...config, configFile: false })

await server.listen()
server.printUrls()
