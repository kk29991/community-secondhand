// 通过 Node 注册解析钩子 (由 --import 指定本文件)
import { register } from 'node:module'

register(new URL('./loader.mjs', import.meta.url).href)
