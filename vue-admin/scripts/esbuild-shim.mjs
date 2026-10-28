/**
 * esbuild 替代模块 (ESM)
 * ---------------------------------------------------------------------------
 * 仅在 esbuild 无法启动的环境中, 由 esbuild-hooks.mjs 通过 Node loader hook
 * 把裸模块说明符 "esbuild" 重定向到本文件。普通环境不会走到这里。
 *
 * Vite 的引用形式 (见 vite/dist/node/chunks/dep-*.js):
 *   import esbuild, { transform as transform$1, formatMessages, build as build$3 } from 'esbuild'
 *
 * 其中构建期真正会被调用的是 transform (vite:define 插件做 define 替换)。
 * 这里用纯 JS 实现顶替它; 其余导出原样透传, 未实现的能力会抛出清晰的错误,
 * 而不是让进程抛出一个难以理解的 spawn EPERM。
 */
import { createRequire } from 'node:module'
import { replaceDefineValues } from './js-define.mjs'

const require = createRequire(import.meta.url)

/** 真实 esbuild 模块对象 (只读取元信息, 不使用它的编译能力) */
const real = require('esbuild')

/** 构建期唯一需要的转换: define 替换 */
export function transform(input, options = {}) {
  const loader = options.loader || 'js'
  if (loader !== 'js' && loader !== 'jsx') {
    return Promise.reject(
      new Error(
        `[sandbox-compat] 当前环境无法运行 esbuild, 暂不支持 loader="${loader}" 的转换。`
      )
    )
  }
  try {
    const define = options.define
    const code =
      define && Object.keys(define).length
        ? replaceDefineValues(String(input), define)
        : String(input)
    // Vite 只读取 code 与 map (map 为空字符串表示无 sourcemap)
    return Promise.resolve({ code, map: '', warnings: [] })
  } catch (e) {
    return Promise.reject(e)
  }
}

export function transformSync(input, options = {}) {
  const loader = options.loader || 'js'
  if (loader !== 'js' && loader !== 'jsx') {
    throw new Error(`[sandbox-compat] 暂不支持 loader="${loader}" 的同步转换。`)
  }
  const define = options.define
  const code =
    define && Object.keys(define).length
      ? replaceDefineValues(String(input), define)
      : String(input)
  return { code, map: '', warnings: [] }
}

export function formatMessages() {
  return Promise.resolve([])
}

export function formatMessagesSync() {
  return []
}

function unavailable(name) {
  return function unavailableApi() {
    throw new Error(
      `[sandbox-compat] esbuild.${name}() 在当前环境不可用 (esbuild 需要子进程管道通信, ` +
        `被沙箱禁止)。生产构建不依赖该接口; 若开发服务器报此错误, 请确认 ` +
        `vite.config.js 中已设置 optimizeDeps.noDiscovery = true。`
    )
  }
}

/** Vite 的 dist/node/index.js 会 `export { version as esbuildVersion } from 'esbuild'` */
export const version = real.version

export const build = unavailable('build')
export const buildSync = unavailable('buildSync')
export const context = unavailable('context')
export const analyzeMetafile = unavailable('analyzeMetafile')
export const analyzeMetafileSync = unavailable('analyzeMetafileSync')
export const stop = () => {}
export const initialize = () => Promise.resolve()

/** 默认导出: 复制真实 esbuild 的字段, 覆盖本文件已实现的部分 */
const defaultExport = {}
for (const key of Object.keys(real)) {
  try {
    defaultExport[key] = real[key]
  } catch (e) {
    // 忽略无法读取的字段
  }
}
defaultExport.transform = transform
defaultExport.transformSync = transformSync
defaultExport.formatMessages = formatMessages
defaultExport.formatMessagesSync = formatMessagesSync
defaultExport.build = build
defaultExport.buildSync = buildSync
defaultExport.context = context
defaultExport.stop = stop
defaultExport.version = real.version

export default defaultExport
