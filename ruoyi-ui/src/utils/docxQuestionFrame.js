const QUESTION_START = /^[\s\u3000]*(?:\(\d+\)|\uff08\d+\uff09|\d+[.\uFF0E\u3001)\uFF09]|\u7b2c\d+\u9898)/
const SECTION_HEADER = /^[\s\u3000]*(?:[\u4e00-\u9fa5]{1,6}[\u3001\.\uFF0E\uFF0E]|\u7b2c[\u4e00-\u9fa5\d]+[\u90e8\u5206\u8282])/
const OPTION_LINE = /^[A-Ha-h][.\uFF0E\u3001\u3002)\uFF09:\uFF1A]\s*|^[A-Ha-h]\s+\S/
const QUESTION_NUMBER = /^[\s\u3000]*(?:\((\d+)\)|\uff08(\d+)\uff09|(\d+)[.\uFF0E\u3001)\uFF09]|\u7b2c(\d+)\u9898)/

export function normalizeLineText(text) {
  return String(text || '').replace(/\s+/g, ' ').trim()
}

export function parseQuestionNumber(text) {
  const line = normalizeLineText(text)
  const match = line.match(QUESTION_NUMBER)
  if (!match) return null
  const raw = match[1] || match[2] || match[3] || match[4]
  const num = parseInt(raw, 10)
  return Number.isNaN(num) ? null : num
}

export function isQuestionStart(text) {
  return parseQuestionNumber(text) != null
}

export function isSectionHeader(text) {
  const line = normalizeLineText(text)
  if (!line || line.length > 40) return false
  if (SECTION_HEADER.test(line)) return true
  return /\u9898/.test(line) && line.length < 20 && !parseQuestionNumber(line)
}

export function isOptionLine(text) {
  return OPTION_LINE.test(normalizeLineText(text))
}

function splitInlineOptions(line) {
  const text = normalizeLineText(line)
  if (!text) return []
  const parts = text.split(/(?=(?:^|\s)[A-Ha-h][.\uFF0E\u3001\u3002)\uFF09:\uFF1A\s])/).map(s => s.trim()).filter(Boolean)
  const options = parts.filter(p => isOptionLine(p))
  if (options.length >= 2) return options
  if (isOptionLine(text)) return [text]
  return [text]
}

function nodeContainsOption(node) {
  const text = getNodeText(node)
  if (isOptionLine(text)) return true
  return splitInlineOptions(text).filter(isOptionLine).length > 0
}

function expandRangeWithMedia(range, allNodes) {
  if (!range.length) return range
  const set = new Set(range)
  const top = range[0].getBoundingClientRect().top
  const bottom = range[range.length - 1].getBoundingClientRect().bottom
  allNodes.forEach(node => {
    if (!isMediaNode(node) || set.has(node)) return
    const rect = node.getBoundingClientRect()
    const cy = rect.top + rect.height / 2
    if (cy >= top - 10 && cy <= bottom + 10) {
      set.add(node)
    }
  })
  return allNodes.filter(node => set.has(node))
}

export function getNodeText(node) {
  if (!node) return ''
  return normalizeLineText(node.innerText || node.textContent || '')
}

export function isMediaNode(node) {
  const tag = (node.tagName || '').toUpperCase()
  return tag === 'IMG' || tag === 'SVG'
}

export function collectContentNodes(root) {
  if (!root) return []
  const selector = [
    'section.docx-visual p',
    'section.docx-visual table',
    'section.docx-visual img',
    'section.docx-visual svg',
    '.docx-visual-wrapper p',
    '.docx-visual-wrapper table',
    '.docx-visual-wrapper img',
    '.docx-visual-wrapper svg'
  ].join(', ')
  const nodes = Array.from(root.querySelectorAll(selector))
    .filter(node => {
      const text = getNodeText(node)
      const tag = (node.tagName || '').toUpperCase()
      if (isMediaNode(node)) return (node.getBoundingClientRect().width || 0) > 8
      return text.length > 0 || tag === 'TABLE'
    })
  const unique = []
  nodes.forEach(node => {
    if (!unique.some(item => item !== node && item.contains(node))) {
      unique.push(node)
    }
  })
  unique.sort((a, b) => {
    const ra = a.getBoundingClientRect()
    const rb = b.getBoundingClientRect()
    if (Math.abs(ra.top - rb.top) > 2) return ra.top - rb.top
    return ra.left - rb.left
  })
  return unique
}

export function findNodeIndexAtPoint(nodes, x, y) {
  let best = -1
  let bestArea = Infinity
  nodes.forEach((node, index) => {
    const rect = node.getBoundingClientRect()
    if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
      const area = rect.width * rect.height
      if (area < bestArea) {
        bestArea = area
        best = index
      }
    }
  })
  if (best >= 0) return best
  let nearest = 0
  let nearestDist = Infinity
  nodes.forEach((node, index) => {
    const rect = node.getBoundingClientRect()
    const cx = rect.left + rect.width / 2
    const cy = rect.top + rect.height / 2
    const dist = (cx - x) * (cx - x) + (cy - y) * (cy - y)
    if (dist < nearestDist) {
      nearestDist = dist
      nearest = index
    }
  })
  return nearest
}

function verticalGap(prevNode, nextNode) {
  if (!prevNode || !nextNode) return Infinity
  return nextNode.getBoundingClientRect().top - prevNode.getBoundingClientRect().bottom
}

function findLastOptionIndex(nodes, from, to) {
  let last = -1
  for (let i = from; i <= to; i++) {
    if (isOptionLine(getNodeText(nodes[i]))) last = i
  }
  return last
}

function shouldStopAfterOptions(nodes, lastOptionIndex, i) {
  if (lastOptionIndex < 0 || i <= lastOptionIndex) return false
  const node = nodes[i]
  const text = getNodeText(node)
  if (!text && !isMediaNode(node)) return true
  if (isMediaNode(node)) {
    return verticalGap(nodes[lastOptionIndex], node) > 20
  }
  if (isOptionLine(text)) return false
  return true
}

export function detectQuestionRange(nodes, hitIndex) {
  if (!nodes.length) return []
  const index = Math.max(0, Math.min(hitIndex, nodes.length - 1))

  let start = index
  let questionNo = null
  for (let i = index; i >= 0; i--) {
    const text = getNodeText(nodes[i])
    const num = parseQuestionNumber(text)
    if (num != null) {
      start = i
      questionNo = num
      break
    }
    if (i < index && isSectionHeader(text)) {
      start = Math.min(nodes.length - 1, i + 1)
      break
    }
    start = i
  }

  if (questionNo == null) {
    questionNo = parseQuestionNumber(getNodeText(nodes[start]))
  }

  let end = index
  let lastOptionIndex = findLastOptionIndex(nodes, start, index)

  for (let i = index + 1; i < nodes.length; i++) {
    const text = getNodeText(nodes[i])
    const num = parseQuestionNumber(text)

    if (num != null && questionNo != null && num !== questionNo) break
    if (num != null && i > start) break
    if (isSectionHeader(text)) break
    if (shouldStopAfterOptions(nodes, lastOptionIndex, i)) break

    end = i
    if (isOptionLine(text)) lastOptionIndex = i
    else if (nodeContainsOption(nodes[i])) lastOptionIndex = i
  }

  return expandRangeWithMedia(nodes.slice(start, end + 1), nodes)
}

export function unionClientRect(nodes) {
  const rects = nodes
    .map(node => node.getBoundingClientRect())
    .filter(rect => rect.width > 0 && rect.height > 0)
  if (!rects.length) return null
  const left = Math.min(...rects.map(r => r.left))
  const top = Math.min(...rects.map(r => r.top))
  const right = Math.max(...rects.map(r => r.right))
  const bottom = Math.max(...rects.map(r => r.bottom))
  return {
    left,
    top,
    right,
    bottom,
    width: right - left,
    height: bottom - top
  }
}

export function clientRectToCanvasRect(clientRect, canvasEl) {
  if (!clientRect || !canvasEl) return null
  const canvasRect = canvasEl.getBoundingClientRect()
  const padX = 6
  const padTop = 6
  const padBottom = 2
  return {
    left: Math.max(0, clientRect.left - canvasRect.left + canvasEl.scrollLeft - padX),
    top: Math.max(0, clientRect.top - canvasRect.top + canvasEl.scrollTop - padTop),
    width: clientRect.width + padX * 2,
    height: clientRect.height + padTop + padBottom
  }
}

export function detectQuestionRectAtPoint(root, canvasEl, x, y) {
  const nodes = collectContentNodes(root)
  if (!nodes.length) return null
  const hitIndex = findNodeIndexAtPoint(nodes, x, y)
  const range = detectQuestionRange(nodes, hitIndex)
  const clientRect = unionClientRect(range)
  return clientRectToCanvasRect(clientRect, canvasEl)
}

export function detectQuestionRectByText(root, canvasEl, content) {
  const lines = String(content || '').split(/\r?\n/).map(s => s.trim()).filter(Boolean)
  const needle = lines[0] || ''
  if (needle.length < 4) return null
  const shortNeedle = needle.length > 36 ? needle.slice(0, 36) : needle
  const nodes = collectContentNodes(root)
  let hitIndex = nodes.findIndex(node => getNodeText(node).includes(shortNeedle))
  if (hitIndex < 0) {
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
    let node
    while ((node = walker.nextNode())) {
      const text = normalizeLineText(node.textContent)
      if (!text.includes(shortNeedle)) continue
      let el = node.parentElement
      while (el && el !== root && (el.getBoundingClientRect().height || 0) < 8) {
        el = el.parentElement
      }
      hitIndex = nodes.findIndex(item => item === el || item.contains(el))
      if (hitIndex >= 0) break
    }
  }
  if (hitIndex < 0) return null
  const range = detectQuestionRange(nodes, hitIndex)
  const clientRect = unionClientRect(range)
  return clientRectToCanvasRect(clientRect, canvasEl)
}
