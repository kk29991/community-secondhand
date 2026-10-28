/**
 * 生产构建入口
 * ---------------------------------------------------------------------------
 * 为什么不直接用 `vite build`:
 *   Vite CLI 读取配置文件时会先用 esbuild 打包 vite.config.js; 在禁止子进程管道
 *   stdio 的环境(如本项目的构建沙箱)中会直接报 `spawn EPERM` —— 即使配置里写了
 *   esbuild: false 也没用, 因为配置本身还没被读出来就失败了。
 *
 * 处理方式(条件式):
 *   1. sandbox-compat.mjs: 跳过 Windows `net use` 探测; esbuild 不可用时把裸模块
 *      "esbuild" 重定向为纯 JS 实现(供 Vite 的 vite:define 使用);
 *   2. 用 Node 原生 import 读配置, 以 `configFile: false` 调用 Vite JS API;
 *   3. vite.config.js 在 esbuild 不可用时关闭内置转换并改用 terser 压缩。
 *
 * 在 esbuild 正常的普通环境里, `npx vite build` 与 `npm run build` 等价, 共用同一份配置。
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
