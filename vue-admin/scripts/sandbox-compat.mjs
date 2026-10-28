/**
 * 构建环境兼容处理 (沙箱适配)
 * ===========================================================================
 * 让 Vite 的构建 / 开发服务器可以运行在 "禁止子进程使用管道 stdio" 的环境中
 * (例如本项目的构建沙箱)。所有处理都是**条件式**的: 只有在检测到 esbuild
 * 确实无法启动时才会启用替代方案, 普通开发环境不做任何改变。
 *
 * 【问题 1】Vite 的 Windows 网络驱动器探测
 *   Vite 在 Windows 上会执行一次 exec("net use") 探测网络驱动器映射。
 *   child_process.exec 使用管道 stdio, 沙箱会同步抛出 EPERM, 而且异常发生在
 *   exec 回调之前, 导致 Vite 源码中的 `if (error) return` 容错来不及生效。
 *   处理: 让 fs.realpathSync.native 在首次被调用 (即 Vite 的探测点) 时抛出
 *   EISDIR。Vite 捕获后会立即退回纯 JS 的 fs.realpathSync, 从而跳过 net use。
 *   该退回只影响 realpath 的实现选择, 功能等价。
 *
 * 【问题 2】esbuild 无法启动
 *   esbuild 的 JS API 必须 spawn 一个子进程并通过管道通信 —— 连 buildSync /
 *   transformSync 的 worker 线程方案, 最终也是由 worker 再去 spawn 那个子进程,
 *   因此在沙箱中一律 spawn EPERM。
 *   而 Vite 的 vite:define 插件在任何一次 build 中都会调用 esbuild.transform
 *   做 define 替换 (process.env / import.meta.env), 所以无法绕开。
 *   处理: 用 module.register() 注册一个 ESM loader hook, 把裸说明符 "esbuild"
 *   重定向到 scripts/esbuild-shim.mjs (纯 JS 实现 define 替换)。
 *   Vite 的配置加载本身则由 scripts/build.mjs 用 `configFile: false` 直接传对象,
 *   不经过 esbuild 打包。
 */

import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath, pathToFileURL } from 'node:url'

const require = createRequire(import.meta.url)
const here = path.dirname(fileURLToPath(import.meta.url))

let applied = false

/* ------------------------------------------------------------------ 探测 */

/** esbuild 能否在当前环境正常启动 */
function isEsbuildUsable() {
  let esbuild
  try {
    esbuild = require('esbuild')
  } catch (e) {
    return false
  }
  try {
    const r = esbuild.transformSync('const __probe = 1', { loader: 'js' })
    return !!(r && typeof r.code === 'string')
  } catch (e) {
    return false
  }
}

/* ------------------------------------------------- 1. Windows realpath 探测 */

function installRealpathCompat() {
  if (process.platform !== 'win32') return

  const nativeRealpath = fs.realpathSync.native
  let armed = true

  fs.realpathSync.native = function patchedRealpathNative(...args) {
    if (armed) {
      let target = ''
      try {
        target = path.resolve(String(args[0]))
      } catch (e) {
        target = ''
      }
      // Vite 的探测点正是 fs.realpathSync.native(path.resolve("./"))
      if (target && target === process.cwd()) {
        armed = false
        const err = new Error('EISDIR: illegal operation on a directory, realpath')
        err.code = 'EISDIR'
        err.errno = -4068
        err.syscall = 'realpath'
        throw err
      }
    }
    return nativeRealpath.apply(this, args)
  }
}

/* --------------------------------------------------- 2. esbuild loader hook */

function registerEsbuildShim() {
  let register
  try {
    ;({ register } = require('node:module'))
  } catch (e) {
    return false
  }
  if (typeof register !== 'function') return false

  try {
    register(pathToFileURL(path.join(here, 'esbuild-hooks.mjs')).href, import.meta.url)
    return true
  } catch (e) {
    return false
  }
}

/* ------------------------------------------------------------- 对外入口 */

/**
 * 必须在 import('vite') 之前调用
 * @returns {{esbuildShim: boolean}} 诊断信息
 */
export function applySandboxCompat() {
  if (applied) return { esbuildShim: false }
  applied = true

  installRealpathCompat()

  const usable = isEsbuildUsable()
  let esbuildShim = false
  if (!usable) {
    esbuildShim = registerEsbuildShim()
    if (esbuildShim && process.env.DSH_VERBOSE) {
      console.log('[sandbox-compat] esbuild 不可用, 已启用纯 JS define 替代实现')
    }
  }
  return { esbuildShim }
}
