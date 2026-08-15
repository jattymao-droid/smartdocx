/**
 * DOM capture helpers for DocxVisualCanvas �� text lines, media, option splitting.
 */

import { parseQuestionContent } from '@/utils/examPaperParse'

const OPTION_PREFIX = /^[A-Ha-h][.\uFF0E\u3001\u3002)\uFF09:��\s]/
const OPTION_PREFIX_SPACE = /^[A-Ha-h]\s+\S/

export function normalizeCapturedText(text) {
  return String(text || '')
    .replace(/[\uFEFF\u200B]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

export function splitInlineOptions(line) {
  const text = normalizeCapturedText(line)
  if (!text) return []
  const parts = text
    .split(/(?=(?:^|\s)[A-Ha-h][.\uFF0E\u3001\u3002)\uFF09:��\s])/)
    .map(s => s.trim())
    .filter(Boolean)
  const options = parts.filter(p => OPTION_PREFIX.test(p) || OPTION_PREFIX_SPACE.test(p))
  if (options.length >= 2) return options
  if (OPTION_PREFIX.test(text) || OPTION_PREFIX_SPACE.test(text)) return [text]
  return [text]
}

export function expandLinesWithInlineOptions(lines) {
  const out = []
  ;(lines || []).forEach(line => {
    splitInlineOptions(line).forEach(part => out.push(part))
  })
  return out
}

function pushLine(rows, el, text) {
  const normalized = normalizeCapturedText(text)
  if (!normalized) return
  const top = el.getBoundingClientRect().top
  rows.push({ top, text: normalized })
}

export function collectTextLineRows(root, elementInSelection, sel) {
  const rows = []
  if (!root || !sel) return rows

  const blockTags = ['P', 'LI', 'H1', 'H2', 'H3', 'H4']
  const blocks = Array.from(root.querySelectorAll(blockTags.join(',')))
  const matchedBlocks = blocks.filter(el => elementInSelection(el, sel))
  const leafBlocks = matchedBlocks.filter(
    el => !matchedBlocks.some(other => other !== el && other.contains(el))
  )
  leafBlocks.forEach(el => pushLine(rows, el, el.innerText || el.textContent))

  root.querySelectorAll('table').forEach(table => {
    if (!elementInSelection(table, sel)) return
    table.querySelectorAll('tr').forEach(tr => {
      const cells = Array.from(tr.querySelectorAll('td, th'))
      if (cells.length > 1) {
        cells.forEach(cell => {
          if (elementInSelection(cell, sel) || elementInSelection(tr, sel)) {
            pushLine(rows, cell, cell.innerText || cell.textContent)
          }
        })
      } else {
        pushLine(rows, tr, tr.innerText || tr.textContent)
      }
    })
  })

  rows.sort((a, b) => a.top - b.top)
  const merged = []
  rows.forEach(item => {
    if (!merged.length || merged[merged.length - 1] !== item.text) {
      merged.push(item.text)
    }
  })
  return merged
}

export function buildCaptureContent(rawLines) {
  const expanded = expandLinesWithInlineOptions(rawLines)
  return expanded.join('\n')
}

export function parseCapturedContent(rawLines) {
  const content = buildCaptureContent(rawLines)
  return parseQuestionContent(content)
}

export function isChartLike(rect) {
  if (!rect) return false
  const area = rect.width * rect.height
  return area >= 1200 || Math.max(rect.width, rect.height) >= 48
}

export function mediaInSelection(node, sel, overlapArea, relativeRect) {
  const rect = relativeRect(node)
  if (!rect || rect.width < 1 || rect.height < 1) return false
  const overlap = overlapArea(sel, rect)
  if (overlap <= 0) return false

  const elArea = rect.width * rect.height
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  const centerInside = cx >= sel.left && cx <= sel.right && cy >= sel.top && cy <= sel.bottom

  const vertOverlap = Math.max(0, Math.min(sel.bottom, rect.bottom) - Math.max(sel.top, rect.top))
  const vertRatio = rect.height > 0 ? vertOverlap / rect.height : 0

  if (isChartLike(rect)) {
    return centerInside || vertRatio >= 0.4 || overlap / elArea >= 0.25
  }

  if (elArea < 400 && Math.min(rect.width, rect.height) < 20) return false
  return centerInside || overlap / elArea >= 0.4 || vertRatio >= 0.55
}

export function queryMediaNodes(root) {
  if (!root) return []
  const imgs = Array.from(root.querySelectorAll('img'))
  const svgs = Array.from(root.querySelectorAll('section.docx-visual svg, .docx-visual-wrapper svg'))
    .filter(svg => (svg.getBoundingClientRect().width || 0) > 8)
  return [...imgs, ...svgs]
}

export async function rasterizeSvgToBlob(svg) {
  const rect = svg.getBoundingClientRect()
  const width = Math.max(1, Math.round(rect.width || svg.clientWidth || 200))
  const height = Math.max(1, Math.round(rect.height || svg.clientHeight || 150))
  const clone = svg.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  if (!clone.getAttribute('viewBox')) {
    clone.setAttribute('viewBox', `0 0 ${width} ${height}`)
  }
  clone.setAttribute('width', String(width))
  clone.setAttribute('height', String(height))
  const svgText = new XMLSerializer().serializeToString(clone)
  const svgBlob = new Blob([svgText], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(svgBlob)

  try {
    const img = await new Promise((resolve, reject) => {
      const image = new Image()
      image.onload = () => resolve(image)
      image.onerror = reject
      image.src = url
    })
    const canvas = document.createElement('canvas')
    const scale = 2
    canvas.width = width * scale
    canvas.height = height * scale
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
    return await new Promise(resolve => canvas.toBlob(resolve, 'image/png'))
  } finally {
    URL.revokeObjectURL(url)
  }
}
