// 锚点链接校验：模拟 VitePress 的 github-slugger 规则生成标题 slug，
// 校验文档内 (#锚点) 与 (文件.md#锚点) 链接是否指向真实标题。
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, dirname } from 'node:path'

const root = process.cwd()
const docsDir = join(root, 'docs')

// VitePress 标题 slug 规则（github-slugger）：小写后提取所有
// Unicode 字母/数字连续块，用 '-' 连接（标点符号是块分隔符）
function slugify(s) {
  const blocks = s.toLowerCase().match(/[\p{L}\p{N}]+/gu) || []
  return blocks.join('-')
}

function collectMd(dir) {
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name)
    return statSync(p).isDirectory() ? collectMd(p) : (name.endsWith('.md') ? [p] : [])
  })
}

function headingIds(text) {
  const ids = new Set()
  for (const line of text.split('\n')) {
    const m = line.match(/^(#{1,6})\s+(.+)$/)
    if (m) ids.add(slugify(m[2]))
  }
  return ids
}

const files = collectMd(docsDir)
const fileSet = new Set(files)
let fail = 0

for (const file of files) {
  const text = readFileSync(file, 'utf8')
  const ids = headingIds(text)
  const rel = file.replace(docsDir + '/', '')
  const re = /\]\(([^)#]+?)(?:\.md)?#([^)]+)\)/g
  let match
  while ((match = re.exec(text))) {
    const target = match[1]
    const anchor = match[2].trim()
    const targetFile = target ? join(dirname(file), target + '.md') : file
    if (!targetFile.endsWith('.md')) continue
    if (!fileSet.has(targetFile)) {
      console.log(`BROKEN target: ${rel} -> ${target}.md`)
      fail = 1
      continue
    }
    const tIds = headingIds(readFileSync(targetFile, 'utf8'))
    if (!tIds.has(anchor)) {
      console.log(`BROKEN anchor: ${rel} -> ${target || '(same page)'}#${anchor}`)
      fail = 1
    }
  }
}

if (fail) {
  console.log('ANCHOR CHECK FAILED')
  process.exit(1)
}
console.log('anchors OK')
