// 模块解析钩子: 让 Node 直接加载项目源码 (把 @/ 别名, .vue, .css, element-plus 做适配)
import { fileURLToPath, pathToFileURL } from 'node:url'
import fs from 'node:fs'
import path from 'node:path'

// 项目根目录 = 本文件所在目录 (scripts/verify) 的上两级
const ROOT = process.env.MALL_PROJECT_ROOT || path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const SRC = path.join(ROOT, 'src')
const VIRTUAL_PREFIX = 'mall-virtual:'

export async function resolve(specifier, context, nextResolve) {
  // 1. @/xxx 别名 -> 项目 src/xxx
  if (specifier.startsWith('@/')) {
    const target = path.join(SRC, specifier.slice(2))
    for (const candidate of [target, target + '.js', target + '.mjs', target + '.vue', path.join(target, 'index.js')]) {
      if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
        return { url: pathToFileURL(candidate).href, shortCircuit: true }
      }
    }
  }

  // 2. element-plus (无论是否带子路径) -> 桩模块
  if (specifier === 'element-plus' || specifier.startsWith('element-plus/')) {
    return { url: VIRTUAL_PREFIX + 'element-plus', shortCircuit: true }
  }

  // 3. 样式文件 -> 空模块
  if (/\.(css|scss|less)$/.test(specifier)) {
    return { url: VIRTUAL_PREFIX + 'empty', shortCircuit: true }
  }

  // 4. .vue 单文件组件 -> 空组件桩 (仅做数据层验证, 不渲染)
  if (specifier.endsWith('.vue')) {
    return { url: VIRTUAL_PREFIX + 'empty-component', shortCircuit: true }
  }

  // 5. 相对/绝对路径的「无扩展名」导入 (源码里写的是 './request')
  if ((specifier.startsWith('./') || specifier.startsWith('../') || specifier.startsWith('/')) && context.parentURL) {
    try {
      return await nextResolve(specifier, context)
    } catch (err) {
      // 注意: 必须用 fileURLToPath 解码, 否则中文路径会变成 %XX
      const base = specifier.startsWith('/')
        ? fileURLToPath(new URL('file://' + specifier))
        : fileURLToPath(new URL(specifier, context.parentURL))
      for (const candidate of [base + '.js', base + '.mjs', path.join(base, 'index.js'), base + '.vue']) {
        if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
          return { url: candidate.endsWith('.vue') ? VIRTUAL_PREFIX + 'empty-component' : pathToFileURL(candidate).href, shortCircuit: true }
        }
      }
      throw err
    }
  }

  return nextResolve(specifier, context)
}

export async function load(url, context, nextLoad) {
  if (url === VIRTUAL_PREFIX + 'element-plus') {
    return {
      format: 'module',
      shortCircuit: true,
      source: `
export const ElMessage = Object.assign(() => {}, {
  success: () => {}, error: () => {}, warning: () => {}, info: () => {}
})
export const ElMessageBox = {
  confirm: async () => { throw new Error('cancelled-in-test') },
  alert: async () => {}, prompt: async () => ({ value: '' })
}
export const ElNotification = () => {}
export const ElLoading = { service: () => ({ close: () => {} }) }
export default { install() {} }
`
    }
  }
  if (url === VIRTUAL_PREFIX + 'empty') {
    return { format: 'module', shortCircuit: true, source: 'export default {}' }
  }
  if (url === VIRTUAL_PREFIX + 'empty-component') {
    return { format: 'module', shortCircuit: true, source: 'export default { name: "StubComponent" }' }
  }
  return nextLoad(url, context)
}
