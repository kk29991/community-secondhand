// 静态校验脚本 (无需构建工具)
// 1) 逐个解析 src/**/*.vue 的 script 块, 用 vm.SourceTextModule 做真实 ESM 语法检查
// 2) 校验 template 中使用的 Element Plus 图标是否真实存在
// 3) 校验 src/router/index.js 中动态 import 的视图文件是否存在
// 4) 校验 src/**/*.js 的 ESM 语法
import fs from 'node:fs'
import path from 'node:path'
import vm from 'node:vm'
import { fileURLToPath, pathToFileURL } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '..', '..')
const SRC = path.join(ROOT, 'src')

const problems = []
const stats = { vue: 0, js: 0, iconUses: 0 }

function walk(dir, filter, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full, filter, out)
    else if (filter(full)) out.push(full)
  }
  return out
}

const VOID_TAGS = new Set([
  'br', 'hr', 'img', 'input', 'meta', 'link', 'source', 'area',
  'base', 'col', 'embed', 'param', 'track', 'wbr'
])

const KNOWN_COMPONENTS = new Set([
  'component', 'template', 'router-view', 'router-link', 'transition',
  'keep-alive', 'slot', 'teleport', 'suspense'
])

const vueFiles = walk(SRC, (f) => f.endsWith('.vue'))

for (const file of vueFiles) {
  const rel = path.relative(ROOT, file)
  const source = fs.readFileSync(file, 'utf8')
  stats.vue += 1

  // ---- script 块语法检查 ----
  const scriptMatches = [...source.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)]
  if (!scriptMatches.length) {
    problems.push(rel + ': 缺少 <script> 块')
  }
  for (const m of scriptMatches) {
    const code = m[2]
    try {
      new vm.SourceTextModule(code, { identifier: rel })
    } catch (err) {
      problems.push(rel + ': script 语法错误 -> ' + err.message)
    }
  }

  // ---- template 标签配对 ----
  const tplMatch = source.match(/<template>([\s\S]*?)<\/template>\s*(?=<style|$)/)
  if (tplMatch) {
    const tpl = tplMatch[1]
    const stack = []
    const tagRe = /<(\/?)([a-zA-Z][a-zA-Z0-9-]*)((?:"[^"]*"|'[^']*'|[^>"'])*?)(\/?)>/g
    let m
    while ((m = tagRe.exec(tpl))) {
      const closing = m[1]
      const name = m[2].toLowerCase()
      const selfClose = m[4]
      if (VOID_TAGS.has(name) || selfClose === '/') continue
      if (closing) {
        const last = stack.pop()
        if (last !== name) {
          problems.push(rel + ': template 标签不匹配 -> 期望 </' + (last || '?') + '> 实际 </' + name + '>')
          break
        }
      } else {
        stack.push(name)
      }
    }
    if (stack.length) {
      problems.push(rel + ': template 存在未闭合标签 -> ' + stack.join(', '))
    }
  }
}

// ---- 图标使用校验 ----
const iconsEntry = path.join(ROOT, 'node_modules', '@element-plus', 'icons-vue', 'dist', 'index.js')
let iconNames = new Set()
if (fs.existsSync(iconsEntry)) {
  const mod = await import(pathToFileURL(iconsEntry).href)
  iconNames = new Set(Object.keys(mod))
} else {
  problems.push('未找到 @element-plus/icons-vue, 跳过图标校验')
}

if (iconNames.size) {
  for (const file of vueFiles) {
    const rel = path.relative(ROOT, file)
    const source = fs.readFileSync(file, 'utf8')
    const tplMatch = source.match(/<template>([\s\S]*?)<\/template>\s*(?=<style|$)/)
    const tpl = tplMatch ? tplMatch[1] : ''
    if (!tpl) continue
    const scriptPart = source.replace(tpl, '')
    const used = new Set()
    for (const m of tpl.matchAll(/<([A-Z][A-Za-z0-9]*)[\s/>]/g)) used.add(m[1])
    // 动态组件字符串写法: <component :is="isCollected ? 'StarFilled' : 'Star'" />
    for (const m of tpl.matchAll(/'([A-Z][A-Za-z0-9]*)'/g)) used.add(m[1])
    for (const name of used) {
      if (KNOWN_COMPONENTS.has(name.toLowerCase())) continue
      if (iconNames.has(name)) {
        stats.iconUses += 1
        continue
      }
      // 局部组件 (已在 script 中导入/定义) 或动态 is 绑定
      if (new RegExp('\\b' + name + '\\b').test(scriptPart)) continue
      if (tpl.includes(':is="' + name + '"') || tpl.includes("'" + name + "'")) continue
      problems.push(rel + ': 使用了未注册的组件/图标 <' + name + '>')
    }
  }
}

// ---- 路由视图存在性 ----
const routerFile = path.join(SRC, 'router', 'index.js')
if (fs.existsSync(routerFile)) {
  const routerSrc = fs.readFileSync(routerFile, 'utf8')
  for (const m of routerSrc.matchAll(/import\(\s*['"]@\/([^'"]+)['"]\s*\)/g)) {
    const target = path.join(SRC, m[1])
    const ok = fs.existsSync(target) || fs.existsSync(target + '.vue') || fs.existsSync(target + '.js')
    if (!ok) problems.push('router: 视图文件不存在 -> src/' + m[1])
  }
}

// ---- .js 文件 ESM 语法 ----
for (const file of walk(SRC, (f) => f.endsWith('.js'))) {
  const rel = path.relative(ROOT, file)
  stats.js += 1
  try {
    new vm.SourceTextModule(fs.readFileSync(file, 'utf8'), { identifier: rel })
  } catch (err) {
    problems.push(rel + ': ESM 语法错误 -> ' + err.message)
  }
}

console.log('检查 .vue 文件: ' + stats.vue + ' 个')
console.log('检查 .js  文件: ' + stats.js + ' 个')
console.log('校验图标使用: ' + stats.iconUses + ' 处 (图库共 ' + iconNames.size + ' 个图标)')
if (problems.length) {
  console.log('')
  console.log('发现 ' + problems.length + ' 个问题:')
  for (const p of problems) console.log(' - ' + p)
  process.exit(1)
}
console.log('')
console.log('静态校验通过: 无 JS/ESM 语法错误, 无未注册图标, 路由视图文件齐全。')
