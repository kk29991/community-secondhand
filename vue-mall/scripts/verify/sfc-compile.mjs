// 用 Vue 官方编译器 (<script setup> + template) 真实编译所有单文件组件
// 等价于 vite build 中 @vitejs/plugin-vue 的核心编译步骤, 可捕获模板/绑定/宏使用错误
// 运行: node --no-warnings scripts/verify/sfc-compile.mjs
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '..', '..')
const SRC = path.join(ROOT, 'src')

const sfc = await import(pathToFileURL(path.join(ROOT, 'node_modules', '@vue', 'compiler-sfc', 'dist', 'compiler-sfc.cjs.js')).href)
const { parse, compileScript, compileTemplate, compileStyle } = sfc.default || sfc

const problems = []
let files = 0
let templateNodes = 0

function walk(dir, filter, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full, filter, out)
    else if (filter(full)) out.push(full)
  }
  return out
}

function countNodes(ast) {
  let n = 0
  const visit = (node) => {
    if (!node || typeof node !== 'object') return
    if (node.type === 1 || node.type === 2) n += 1
    for (const key of ['children', 'branches', 'items']) {
      const v = node[key]
      if (Array.isArray(v)) v.forEach(visit)
    }
  }
  visit(ast)
  return n
}

for (const file of walk(SRC, (f) => f.endsWith('.vue'))) {
  const rel = path.relative(ROOT, file)
  files += 1
  const source = fs.readFileSync(file, 'utf8')
  const id = path.basename(file)

  const { descriptor, errors } = parse(source, { filename: file })
  if (errors && errors.length) {
    for (const err of errors) problems.push(rel + ': SFC 解析错误 -> ' + err.message)
    continue
  }

  // ---- script / script setup ----
  let bindings
  let compiledScript
  if (descriptor.scriptSetup || descriptor.script) {
    try {
      compiledScript = compileScript(descriptor, { id, inlineTemplate: false, isProd: false, genDefaultAs: undefined })
      bindings = compiledScript.bindings
    } catch (err) {
      problems.push(rel + ': <script setup> 编译错误 -> ' + err.message)
    }
  } else {
    problems.push(rel + ': 缺少 script 块')
  }

  // ---- template ----
  if (descriptor.template) {
    try {
      const res = compileTemplate({
        id,
        filename: file,
        source: descriptor.template.content,
        compilerOptions: {
          bindingMetadata: bindings,
          mode: 'module',
          prefixIdentifiers: true
        }
      })
      if (res.errors && res.errors.length) {
        for (const err of res.errors) problems.push(rel + ': 模板编译错误 -> ' + (err.message || err))
      }
      if (res.tips && res.tips.length) {
        for (const tip of res.tips) console.log('  [tip] ' + rel + ': ' + (tip.message || tip))
      }
      if (res.ast) templateNodes += countNodes(res.ast)
      if (!/function render|export function render/.test(res.code || '')) {
        problems.push(rel + ': 模板未生成 render 函数')
      }
    } catch (err) {
      problems.push(rel + ': 模板编译异常 -> ' + err.message)
    }
  } else {
    problems.push(rel + ': 缺少 template 块')
  }

  // ---- styles ----
  for (const style of descriptor.styles || []) {
    try {
      const res = compileStyle({ id, filename: file, source: style.content, scoped: !!style.scoped })
      if (res.errors && res.errors.length) {
        for (const err of res.errors) problems.push(rel + ': 样式编译错误 -> ' + (err.message || err))
      }
    } catch (err) {
      problems.push(rel + ': 样式编译异常 -> ' + err.message)
    }
  }
}

console.log('编译单文件组件: ' + files + ' 个, 模板节点 ' + templateNodes + ' 个')
if (problems.length) {
  console.log('')
  console.log('发现 ' + problems.length + ' 个编译问题:')
  for (const p of problems) console.log(' - ' + p)
  process.exit(1)
}
console.log('全部 .vue 组件编译通过 (script setup + template + scoped style 均无错误)。')
