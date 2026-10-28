/**
 * 纯 JS 版 define 替换
 * ---------------------------------------------------------------------------
 * 语义对齐 esbuild 的 define 选项 (也就是 Vite 的 vite:define 插件用法):
 *   - 先对源码做词法扫描, 标记字符串 / 模板字符串 / 注释 / 正则字面量区间;
 *   - 只在真正的代码区间里替换, 不会误改字符串和注释里的内容;
 *   - 按 key 长度从长到短匹配, 保证 `process.env.NODE_ENV` 优先于 `process.env`;
 *   - 校验标识符边界, 避免 `myprocess.env` / `process.envX` 这类误匹配。
 */

const IDENT_CHAR = /[A-Za-z0-9_$]/

function regexAllowedAfter(ch, word) {
  if (!ch) return true
  if ('(,=:[!&|?{};+-*%~^<>'.includes(ch)) return true
  return [
    'return',
    'typeof',
    'case',
    'in',
    'of',
    'delete',
    'void',
    'instanceof',
    'new',
    'do',
    'else',
    'yield',
    'await'
  ].includes(word)
}

/**
 * 与源码等长; 1 表示该位置在字面量/注释内部
 */
export function buildLiteralMask(code) {
  const n = code.length
  const mask = new Uint8Array(n)
  let state = 'code'
  let i = 0
  const templateExprDepths = []
  let braceDepth = 0
  let lastCodeChar = ''
  let lastWord = ''

  const mark = (from, to) => {
    for (let k = from; k < to && k < n; k++) mask[k] = 1
  }

  while (i < n) {
    const c = code[i]
    const c2 = code[i + 1]

    if (state === 'code') {
      if (c === '/' && c2 === '/') {
        const start = i
        while (i < n && code[i] !== '\n') i++
        mark(start, i)
        continue
      }
      if (c === '/' && c2 === '*') {
        const start = i
        i += 2
        while (i < n && !(code[i] === '*' && code[i + 1] === '/')) i++
        i = Math.min(n, i + 2)
        mark(start, i)
        continue
      }
      if (c === '"') {
        state = 'dquote'
        mask[i] = 1
        i++
        continue
      }
      if (c === "'") {
        state = 'squote'
        mask[i] = 1
        i++
        continue
      }
      if (c === '`') {
        state = 'template'
        mask[i] = 1
        i++
        continue
      }
      if (c === '/' && regexAllowedAfter(lastCodeChar, lastWord)) {
        let j = i + 1
        let closed = false
        let inClass = false
        while (j < n) {
          const d = code[j]
          if (d === '\\') {
            j += 2
            continue
          }
          if (d === '\n') break
          if (d === '[') inClass = true
          else if (d === ']') inClass = false
          else if (d === '/' && !inClass) {
            closed = true
            j++
            break
          }
          j++
        }
        if (closed) {
          while (j < n && /[a-z]/i.test(code[j])) j++
          mark(i, j)
          i = j
          lastCodeChar = '/'
          lastWord = ''
          continue
        }
        lastCodeChar = c
        lastWord = ''
        i++
        continue
      }
      if (c === '{') {
        braceDepth++
        lastCodeChar = c
        lastWord = ''
        i++
        continue
      }
      if (c === '}') {
        if (
          templateExprDepths.length &&
          braceDepth === templateExprDepths[templateExprDepths.length - 1]
        ) {
          templateExprDepths.pop()
          state = 'template'
          mask[i] = 1
          i++
          continue
        }
        if (braceDepth > 0) braceDepth--
        lastCodeChar = c
        lastWord = ''
        i++
        continue
      }
      if (!/\s/.test(c)) {
        if (IDENT_CHAR.test(c)) lastWord = IDENT_CHAR.test(lastCodeChar) ? lastWord + c : c
        else lastWord = ''
        lastCodeChar = c
      }
      i++
      continue
    }

    if (state === 'squote' || state === 'dquote') {
      const quote = state === 'squote' ? "'" : '"'
      if (c === '\\') {
        mask[i] = 1
        if (i + 1 < n) mask[i + 1] = 1
        i += 2
        continue
      }
      mask[i] = 1
      if (c === quote) {
        state = 'code'
        lastCodeChar = quote
        lastWord = ''
      } else if (c === '\n') {
        state = 'code'
      }
      i++
      continue
    }

    if (state === 'template') {
      if (c === '\\') {
        mask[i] = 1
        if (i + 1 < n) mask[i + 1] = 1
        i += 2
        continue
      }
      if (c === '`') {
        mask[i] = 1
        state = 'code'
        lastCodeChar = '`'
        lastWord = ''
        i++
        continue
      }
      if (c === '$' && c2 === '{') {
        mask[i] = 1
        mask[i + 1] = 1
        templateExprDepths.push(braceDepth)
        state = 'code'
        i += 2
        continue
      }
      mask[i] = 1
      i++
      continue
    }
  }

  return mask
}

function escapeRegExp(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 执行 define 替换
 * @param {string} code
 * @param {Record<string,string>} define key -> 替换后的 JS 代码片段
 */
export function replaceDefineValues(code, define) {
  const keys = Object.keys(define || {})
  if (!keys.length) return code

  keys.sort((a, b) => b.length - a.length)

  const mask = buildLiteralMask(code)
  const pattern = new RegExp(keys.map(escapeRegExp).join('|'), 'g')

  const edits = []
  let m
  while ((m = pattern.exec(code)) !== null) {
    const start = m.index
    const end = start + m[0].length
    const key = m[0]

    let inside = false
    for (let k = start; k < end; k++) {
      if (mask[k]) {
        inside = true
        break
      }
    }
    if (inside) continue

    const prev = start > 0 ? code[start - 1] : ''
    const next = end < code.length ? code[end] : ''
    if (prev && IDENT_CHAR.test(prev)) continue
    if (next && IDENT_CHAR.test(next)) continue

    edits.push({ start, end, value: String(define[key]) })
    if (pattern.lastIndex === m.index) pattern.lastIndex++
  }

  if (!edits.length) return code

  let out = code
  for (let k = edits.length - 1; k >= 0; k--) {
    const e = edits[k]
    out = out.slice(0, e.start) + e.value + out.slice(e.end)
  }
  return out
}

export default replaceDefineValues
