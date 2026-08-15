import {
  ImageRun,
  Paragraph,
  Table,
  TableCell,
  TableRow,
  TextRun,
  WidthType
} from 'docx'
import { resolveImageUrl } from '@/utils/paperExportCommon'
import {
  computeDocxImageTransform,
  isInlineImageParagraph,
  readImageNaturalSize
} from '@/utils/paperExportDocxImage'

const FONT = '\u5b8b\u4f53'
const FONT_ATTR = { name: FONT, eastAsia: FONT, ascii: FONT, hAnsi: FONT }

function textRun(text, opts = {}) {
  const content = String(text || '')
  if (!content) return null
  return new TextRun({
    text: content,
    font: FONT_ATTR,
    size: opts.size || 24,
    bold: !!opts.bold,
    italics: !!opts.italic,
    color: opts.color,
    underline: opts.underline ? {} : undefined,
    subScript: !!opts.sub,
    superScript: !!opts.sup
  })
}

function lineBreakRun() {
  return new TextRun({ break: 1, font: FONT_ATTR, size: 24 })
}

function paragraphSpec(children, opts = {}) {
  return {
    type: 'paragraph',
    spacing: opts.spacing,
    alignment: opts.alignment,
    indent: opts.indent,
    children: (children || []).filter(Boolean)
  }
}

function tableSpec(table) {
  return { type: 'table', table }
}

async function fetchImageBytes(url) {
  const res = await fetch(url, { credentials: 'include' })
  if (!res.ok) throw new Error('image fetch failed')
  return new Uint8Array(await res.arrayBuffer())
}

function parseHtmlRoot(html) {
  const doc = new DOMParser().parseFromString(`<div id="qb-root">${html || ''}</div>`, 'text/html')
  return doc.getElementById('qb-root')
}

function blankWidth(node) {
  const style = node.getAttribute('style') || ''
  const match = style.match(/min-width\s*:\s*([0-9.]+)em/i)
  if (!match) return 12
  return Math.max(4, Math.round(parseFloat(match[1]) * 2))
}

async function walkInline(node, runs, opts = {}) {
  if (!node) return
  if (node.nodeType === Node.TEXT_NODE) {
    const text = (node.textContent || '').replace(/\u00a0/g, ' ')
    if (text) {
      const run = textRun(text, opts)
      if (run) runs.push(run)
    }
    return
  }
  if (node.nodeType !== Node.ELEMENT_NODE) return

  const tag = node.tagName.toLowerCase()
  if (tag === 'br') {
    runs.push(lineBreakRun())
    return
  }
  if (tag === 'img') {
    const src = resolveImageUrl(node.getAttribute('src') || '')
    if (src) {
      try {
        const data = await fetchImageBytes(src)
        const naturalSize = await readImageNaturalSize(data)
        const transform = computeDocxImageTransform(node, src, naturalSize)
        runs.push(new ImageRun({ data, transformation: transform }))
      } catch (e) { /* skip broken image */ }
    }
    return
  }
  if ((tag === 'span' && (node.classList.contains('qb-blank') || node.getAttribute('class') === 'qb-blank')) || tag === 'bk') {
    const run = textRun('_'.repeat(blankWidth(node)), { ...opts, underline: true })
    if (run) runs.push(run)
    return
  }
  if (tag === 'b' || tag === 'strong') {
    for (const child of node.childNodes) await walkInline(child, runs, { ...opts, bold: true })
    return
  }
  if (tag === 'i' || tag === 'em') {
    for (const child of node.childNodes) await walkInline(child, runs, { ...opts, italic: true })
    return
  }
  if (tag === 'sub') {
    for (const child of node.childNodes) await walkInline(child, runs, { ...opts, sub: true })
    return
  }
  if (tag === 'sup') {
    for (const child of node.childNodes) await walkInline(child, runs, { ...opts, sup: true })
    return
  }
  if (tag === 'u') {
    for (const child of node.childNodes) await walkInline(child, runs, { ...opts, underline: true })
    return
  }
  for (const child of node.childNodes) {
    await walkInline(child, runs, opts)
  }
}

async function buildTable(node) {
  const rows = []
  node.querySelectorAll('tr').forEach(tr => {
    const cells = []
    tr.querySelectorAll('th,td').forEach(td => {
      cells.push(td)
    })
    if (cells.length) rows.push(cells)
  })
  if (!rows.length) return null

  const tableRows = []
  for (const row of rows) {
    const tableCells = []
    for (const cell of row) {
      const runs = []
      for (const child of cell.childNodes) {
        await walkInline(child, runs)
      }
      tableCells.push(new TableCell({
        width: { size: 2400, type: WidthType.DXA },
        children: [new Paragraph({ children: runs.length ? runs : [textRun(' ')] })]
      }))
    }
    tableRows.push(new TableRow({ children: tableCells }))
  }
  return new Table({ rows: tableRows })
}

async function buildElementsFromNode(node, opts = {}) {
  const elements = []
  const tag = node.tagName ? node.tagName.toLowerCase() : ''

  if (tag === 'table') {
    const table = await buildTable(node)
    return table ? [tableSpec(table)] : []
  }
  if (tag === 'p' || tag === 'div') {
    const runs = []
    for (const child of node.childNodes) {
      await walkInline(child, runs, opts)
    }
    if (runs.length) elements.push(paragraphSpec(runs, opts.paragraph))
    return elements
  }

  const runs = []
  for (const child of node.childNodes) {
    if (child.nodeType === Node.ELEMENT_NODE) {
      const childTag = child.tagName.toLowerCase()
      if (childTag === 'table') {
        if (runs.length) {
          elements.push(paragraphSpec(runs, opts.paragraph))
          runs.length = 0
        }
        const table = await buildTable(child)
        if (table) elements.push(tableSpec(table))
        continue
      }
      if (childTag === 'p' || childTag === 'div') {
        if (isInlineImageParagraph(child)) {
          await walkInline(child.firstElementChild || child.querySelector('img'), runs, opts)
          continue
        }
        if (runs.length) {
          elements.push(paragraphSpec(runs, opts.paragraph))
          runs.length = 0
        }
        elements.push(...await buildElementsFromNode(child, opts))
        continue
      }
    }
    await walkInline(child, runs, opts)
  }
  if (runs.length) elements.push(paragraphSpec(runs, opts.paragraph))
  return elements
}

export async function buildDocxParagraphsFromHtml(html, opts = {}) {
  const root = parseHtmlRoot(html)
  if (!root) return [paragraphSpec([textRun(' ')])]
  const elements = await buildElementsFromNode(root, opts)
  return elements.length ? elements : [paragraphSpec([textRun(' ')])]
}

export async function buildDocxRunsFromHtml(html, opts = {}) {
  const root = parseHtmlRoot(html)
  const runs = []
  if (!root) return runs
  for (const child of root.childNodes) {
    await walkInline(child, runs, opts)
  }
  return runs
}
