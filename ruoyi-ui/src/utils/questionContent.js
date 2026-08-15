/**
 * Normalize question stem text (strip leading question numbers, etc.).
 */
import { resolvePortalMediaUrl } from '@/utils/portalBanner'

export function isQuestionHtml(text) {
  const s = String(text || '')
  if (!s.includes('<')) return false
  if (/<(table|img|p|div|span|tbody|tr|td|th|sub|sup|br|i|em|b|strong|bk|ul|li)(\s|>|\/)/i.test(s)) {
    return true
  }
  if (/<\s*\/\s*(p|div|span|table|tr|td|th)\s*>/i.test(s)) {
    return true
  }
  return false
}

export function normalizeQuestionContent(text) {
  if (!text) return ''
  return String(text).replace(/<br\s*\/?>/gi, '\n')
}

/**
 * Rewrite relative upload paths; proxy external http(s) images via gateway media proxy.
 */
export function resolveMediaProxyUrl(url, apiBase = process.env.VUE_APP_BASE_API) {
  return resolvePortalMediaUrl(url, apiBase)
}

/**
 * Convert legacy option tables (A/B/C/D rows) into vertical list markup for display.
 */
export function normalizeLegacyOptionTables(html) {
  if (!html || !html.includes('<table')) return html
  if (typeof document === 'undefined') return html

  const wrap = document.createElement('div')
  wrap.innerHTML = String(html)
  const optionPattern = /^[A-Ha-h][．\.、\)）:：\s]/

  wrap.querySelectorAll('table').forEach(table => {
    const cells = [...table.querySelectorAll('td')]
    if (cells.length < 2) return

    const optionCells = cells.filter(td => optionPattern.test((td.textContent || '').trim()))
    if (optionCells.length < 2 || optionCells.length !== cells.length) return

    const ul = document.createElement('ul')
    ul.className = 'qb-options'

    optionCells.forEach(td => {
      const plain = (td.textContent || '').trim()
      const match = plain.match(/^([A-Ha-h])[．\.、\)）:：\s]+([\s\S]*)$/)
      const li = document.createElement('li')
      li.className = 'qb-option-item'

      const label = document.createElement('span')
      label.className = 'qb-option-label'
      label.textContent = `${(match ? match[1] : '').toUpperCase()}.`

      const body = document.createElement('span')
      body.className = 'qb-option-text'
      let inner = td.innerHTML.trim()
      if (match) {
        inner = inner.replace(/^\s*[A-Ha-h][．\.、\)）:：\s]+/i, '').trim()
      }
      body.innerHTML = inner

      li.appendChild(label)
      li.appendChild(body)
      ul.appendChild(li)
    })

    table.replaceWith(ul)
  })

  return wrap.innerHTML
}

export function resolveQuestionHtml(html, apiBase = process.env.VUE_APP_BASE_API) {
  if (!html) return ''
  const base = String(apiBase || '').replace(/\/$/, '')
  let result = normalizeLegacyOptionTables(String(html))
  return result.replace(
    /(<img\b[^>]*\ssrc=["'])([^"']+)(["'])/gi,
    (match, prefix, src, suffix) => {
      const raw = String(src || '').trim()
      if (raw.startsWith('/profile/')) {
        return `${prefix}${base}/system${raw}${suffix}`
      }
      return `${prefix}${resolveMediaProxyUrl(raw, apiBase)}${suffix}`
    }
  )
}

export function htmlToPlainText(html) {
  if (!html) return ''
  return String(html)
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<\/p>/gi, ' ')
    .replace(/<\/tr>/gi, ' ')
    .replace(/<[^>]+>/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

export function stripLeadingQuestionNo(text) {
  if (!text) return ''
  if (isQuestionHtml(text)) {
    return text
  }
  let result = String(text).replace(/\r/g, '').trim()
  if (!result) return ''
  const pattern = /^[\s\u3000]*(?:第\s*\d+\s*题[\.．、:：\-—]?\s*|第\s*[一二三四五六七八九十百千万]+\s*题[\.．、:：\-—]?\s*|[\(（]\s*\d+\s*[\)）]\s*[\.．、]?\s*|\d+[\.．、:：\-—]\s*|[一二三四五六七八九十百千万]+[、．.]\s*)/
  let prev
  do {
    prev = result
    result = result.replace(pattern, '').trim()
  } while (result !== prev)
  return result
}
