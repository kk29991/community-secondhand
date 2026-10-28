/**
 * Node ESM loader hook: 把 "esbuild" 重定向到本地的纯 JS 替代模块。
 * 由 scripts/sandbox-compat.mjs 通过 module.register() 注册, 且仅在检测到
 * esbuild 无法在当前环境启动时才会注册。
 */
import { fileURLToPath, pathToFileURL } from 'node:url'
import path from 'node:path'

const SHIM_URL = pathToFileURL(
  path.join(path.dirname(fileURLToPath(import.meta.url)), 'esbuild-shim.mjs')
).href

export async function resolve(specifier, context, nextResolve) {
  if (specifier === 'esbuild') {
    return { url: SHIM_URL, format: 'module', shortCircuit: true }
  }
  return nextResolve(specifier, context)
}
