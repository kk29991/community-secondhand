/**
 * 开发服务器入口 (端口 5174)
 * ---------------------------------------------------------------------------
 * 为什么不直接用 `vite` 命令:
 *   Vite CLI 读取配置文件时会先用 esbuild 打包 vite.config.js, 而 esbuild 的 JS API
 *   需要子进程通过管道 (stdio: 'pipe') 通信。在禁止子进程管道 stdio 的环境(如本项目
 *   的构建沙箱)中会直接报 `spawn EPERM`。
 *
 * 处理方式(条件式, 普通环境不受影响):
 *   1. sandbox-compat.mjs: 让 Vite 跳过 Windows 下的 `net use` 探测;
 *   2. 用 Node 原生 import 读配置, 以 `configFile: false` 调用 Vite JS API。
 */
import { applySandboxCompat } from './sandbox-compat.mjs'

applySandboxCompat()

const { createServer } = await import('vite')
const { default: config } = await import('../vite.config.js')

const server = await createServer({ ...config, configFile: false })

await server.listen()
server.printUrls()
