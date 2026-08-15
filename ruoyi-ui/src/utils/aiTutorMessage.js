import {
  isAiFormulaBlockLine,
  renderAiFormulaBlock,
  renderBoldSegments
} from '@/utils/aiTutorFormula'

/** Normalize \( \) / \[ \] delimiters to $...$ / $$...$$ */
function normalizeAiDelimiters(text) {
  return String(text)
    .replace(/\\\(([\s\S]+?)\\\)/g, '$$$1$')
    .replace(/\\\[([\s\S]+?)\\\]/g, '$$$$$1$$$$')
}

function renderInline(text) {
  return renderBoldSegments(normalizeAiDelimiters(text))
}

/**
 * Light markdown + KaTeX for AI tutor chat bubbles.
 */
export function renderAiTutorMessage(text) {
  if (text == null || text === '') return ''
  const lines = String(text).replace(/\r\n/g, '\n').split('\n')
  const htmlParts = []
  let listType = null

  const closeList = () => {
    if (listType === 'ul') htmlParts.push('</ul>')
    if (listType === 'ol') htmlParts.push('</ol>')
    listType = null
  }

  lines.forEach(line => {
    const trimmed = line.trim()
    if (!trimmed) {
      closeList()
      return
    }

    if (isAiFormulaBlockLine(trimmed)) {
      closeList()
      htmlParts.push(renderAiFormulaBlock(trimmed))
      return
    }

    const heading = trimmed.match(/^#{1,3}\s+(.+)$/)
    if (heading) {
      closeList()
      htmlParts.push(`<p class="ai-md-heading">${renderInline(heading[1])}</p>`)
      return
    }

    const bullet = trimmed.match(/^[-*\u2022]\s+(.+)$/)
    if (bullet) {
      if (listType !== 'ul') {
        closeList()
        htmlParts.push('<ul class="ai-md-list">')
        listType = 'ul'
      }
      htmlParts.push(`<li>${renderInline(bullet[1])}</li>`)
      return
    }

    const numbered = trimmed.match(/^\d+[.\u3001\uFF0E]\s+(.+)$/)
    if (numbered) {
      if (listType !== 'ol') {
        closeList()
        htmlParts.push('<ol class="ai-md-list">')
        listType = 'ol'
      }
      htmlParts.push(`<li>${renderInline(numbered[1])}</li>`)
      return
    }

    closeList()
    htmlParts.push(`<p class="ai-md-p">${renderInline(trimmed)}</p>`)
  })

  closeList()
  return htmlParts.join('')
}
