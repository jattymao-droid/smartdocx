import { renderFormulaText, splitContentImageParts } from '@/utils/questionFormula'
import { stripLeadingQuestionNo, isQuestionHtml, resolveQuestionHtml } from '@/utils/questionContent'
import { loadLastSubjectName } from '@/utils/questionBasketPrefs'
import { OPTION_LABELS, parseQuestionOption, shouldShowQuestionOptions } from '@/utils/questionOptions'
import { formatChoiceAnswer } from '@/utils/questionAnswer'

export { OPTION_LABELS }

export function escapeHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

export function formulaHtml(text) {
  return renderFormulaText(text || '')
}

function safeFormulaHtml(text) {
  try {
    return formulaHtml(text)
  } catch (e) {
    return escapeHtml(String(text || ''))
  }
}

export function parseJsonArray(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

export function resolveImageUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  if (/^data:/i.test(url) || /^blob:/i.test(url)) return url
  let path = String(url).trim()
  if (!path.startsWith('/')) {
    path = '/' + path
  }
  if (!path.startsWith('/profile/') && !path.startsWith('/statics/')) {
    path = '/profile' + path
  }
  const base = process.env.VUE_APP_BASE_API || ''
  if (path.startsWith('/profile/') || path.startsWith('/statics/')) {
    return base + '/system' + path
  }
  return base + path
}

export function resolveExportField(item, detailMap, field) {
  const detail = detailMap && item && item.questionId ? detailMap[item.questionId] : null
  const fromDetail = detail && detail[field]
  if (fromDetail != null && fromDetail !== '') return fromDetail
  return item ? item[field] : undefined
}

export function displayContent(item, detailMap) {
  const raw = resolveExportField(item, detailMap, 'content') || item.contentBrief || ''
  return stripLeadingQuestionNo(raw)
}

export function resolveExportSubjectName(vm) {
  const header = vm.form?.header || {}
  const fromHeader = String(header.subjectName || '').trim()
  if (fromHeader) return fromHeader
  const items = vm.sortedItems || vm.questionBasketItems || []
  for (const item of items) {
    const detail = vm.detailMap?.[item.questionId]
    const name = detail?.subjectName
    if (name) return String(name).trim()
  }
  return loadLastSubjectName()
}

/** 导出标题：在日期后插入科目名，如「2026年07月01日物理作业」 */
export function exportPaperDisplayTitle(vm) {
  const title = String(vm.form?.paperTitle || '试卷').trim() || '试卷'
  const subject = resolveExportSubjectName(vm)
  if (!subject || title.includes(subject)) return title
  const dateMatch = title.match(/^(\d{4}年\d{1,2}月\d{1,2}日)(.*)$/)
  if (dateMatch) {
    return `${dateMatch[1]}${subject}${dateMatch[2] || ''}`
  }
  return `${subject}${title}`
}

export function exportPaperFilename(vm, ext) {
  const base = exportPaperDisplayTitle(vm).replace(/[\\/:*?"<>|]/g, '_')
  return `${base}.${ext}`
}

export function optionItems(item, detailMap) {
  const arr = parseJsonArray(resolveExportField(item, detailMap, 'options') ?? item.options)
  if (!arr.length || !shouldShowQuestionOptions(item.questionType, arr)) return []
  return arr.map((text, i) => parseQuestionOption(text, i))
}

export function imageUrls(item, detailMap) {
  return parseJsonArray(resolveExportField(item, detailMap, 'images') ?? item.images)
}

export function teacherDetail(vm, item) {
  return vm.detailMap[item.questionId] || {}
}

export function teacherAnswer(vm, item) {
  const d = teacherDetail(vm, item)
  const raw = d.correctAnswer
  if (raw == null || raw === '') return ''
  return formatChoiceAnswer(item.questionType, raw)
}

export function teacherAnalysis(vm, item) {
  return teacherDetail(vm, item).analysis || ''
}

export function shouldExportAnswers(vm) {
  return vm.form?.exportMode === 'teacher'
}

export function isAnswerInline(vm) {
  return (vm.form?.answerLayout || 'inline') !== 'end'
}

export function iterPaperQuestions(vm) {
  const result = []
  for (const vol of vm.paperVolumes || []) {
    for (const section of vol.sections || []) {
      for (const q of section.items || []) {
        result.push(q)
      }
    }
  }
  if (result.length) return result
  const flat = vm.sortedItems || vm.questionBasketItems || []
  return flat.map((item, idx) => ({ ...item, globalNo: idx + 1 }))
}

export function resolveExportVolumes(vm) {
  const volumes = vm.paperVolumes || []
  if (volumes.length) return volumes
  const flat = iterPaperQuestions(vm)
  if (!flat.length) return []
  return [{
    key: 'flat',
    title: '',
    showTitle: false,
    sections: [{ key: 'flat', title: '', items: flat }]
  }]
}

/** Build HTML for one question stem (text, formulas, inline images). */
export function buildQuestionStemHtml(item, detailMap) {
  const qContent = displayContent(item, detailMap)
  if (!qContent) return ''
  if (isQuestionHtml(qContent)) {
    return resolveQuestionHtml(qContent)
  }
  const parts = splitContentImageParts(qContent, imageUrls(item, detailMap))
  const htmlParts = []
  parts.forEach(part => {
    if (part.type === 'text' && part.content) {
      htmlParts.push(safeFormulaHtml(part.content))
    } else if (part.type === 'image' && part.url) {
      const src = resolveImageUrl(part.url)
      if (src) {
        htmlParts.push(`<img src="${escapeHtml(src)}" crossorigin="anonymous" alt="figure"/>`)
      }
    }
  })
  return htmlParts.join('')
}

/** Build HTML for one option line. */
export function buildOptionStemHtml(opt) {
  if (isQuestionHtml(opt.text)) {
    return resolveQuestionHtml(opt.text)
  }
  return safeFormulaHtml(opt.text)
}

function appendInlineAnswerParts(parts, vm, q) {
  const ans = teacherAnswer(vm, q)
  const analysis = teacherAnalysis(vm, q)
  if (ans) parts.push(`<div class="teacher-row">\u3010\u7b54\u6848\u3011${safeFormulaHtml(ans)}</div>`)
  if (analysis) parts.push(`<div class="teacher-row">\u3010\u89e3\u6790\u3011${safeFormulaHtml(analysis)}</div>`)
}

function appendAnswerSheetParts(parts, vm) {
  const items = iterPaperQuestions(vm).filter(q => teacherAnswer(vm, q) || teacherAnalysis(vm, q))
  if (!items.length) return
  parts.push('<div class="answer-sheet-title">\u53c2\u8003\u7b54\u6848</div>')
  items.forEach(q => {
    parts.push('<div class="answer-item">')
    parts.push(`<span class="answer-no">${q.globalNo}.</span>`)
    const ans = teacherAnswer(vm, q)
    const analysis = teacherAnalysis(vm, q)
    if (ans) parts.push(`<span class="answer-text">\u3010\u7b54\u6848\u3011${safeFormulaHtml(ans)}</span>`)
    if (analysis) parts.push(`<span class="analysis-text">\u3010\u89e3\u6790\u3011${safeFormulaHtml(analysis)}</span>`)
    parts.push('</div>')
  })
}

export function buildPaperExportElement(vm) {
  const root = document.createElement('div')
  root.className = 'paper-export-document'
  const pageSize = (vm.pageLayout || 'A4').toUpperCase()
  const showTeacher = shouldExportAnswers(vm)
  const answerInline = isAnswerInline(vm)
  const header = (vm.form && vm.form.header) || {}
  const parts = []
  const volumes = resolveExportVolumes(vm)
  const detailMap = vm.detailMap || {}

  root.style.cssText = [
    'font-family:SimSun,serif',
    'font-size:12pt',
    'line-height:1.75',
    'color:#000',
    'padding:12px 16px',
    'background:#fff',
    'width:794px',
    'box-sizing:border-box'
  ].join(';')

  const styleEl = document.createElement('style')
  styleEl.setAttribute('data-paper-export', '1')
  styleEl.textContent = `
.paper-export-document .doc-title{text-align:center;font-size:16pt;font-weight:bold;margin:0 0 10px;}
.paper-export-document .doc-meta{text-align:center;margin-bottom:16px;padding-bottom:8px;border-bottom:1px solid #333;}
.paper-export-document .volume-title{text-align:center;font-weight:bold;margin:14px 0 8px;}
.paper-export-document .section-title{font-weight:bold;margin:12px 0 8px;}
.paper-export-document .question{margin:10px 0;}
.paper-export-document .q-no,.paper-export-document .q-text{display:inline;}
.paper-export-document .question-images img{display:block;max-width:320px;max-height:240px;margin:6px 0 6px 20px;}
.paper-export-document .option-item{margin:4px 0 4px 20px;}
.paper-export-document .option-label,.paper-export-document .option-text{display:inline;}
.paper-export-document .teacher-row{margin:4px 0 4px 20px;color:#c00;font-size:11pt;}
.paper-export-document .answer-sheet-title{text-align:center;font-weight:bold;font-size:14pt;margin:24px 0 12px;padding-top:12px;border-top:1px solid #333;}
.paper-export-document .answer-item{margin:8px 0 8px 20px;line-height:1.75;}
.paper-export-document .answer-no{font-weight:bold;margin-right:6px;}
.paper-export-document .answer-text,.paper-export-document .analysis-text{display:inline;margin-right:12px;color:#c00;}
.paper-export-document .answer-line{border-bottom:1px solid #999;height:28px;margin:6px 0 6px 20px;}
.paper-export-document .answer-blank{border:1px dashed #bbb;min-height:48px;margin:6px 0 6px 20px;}
.paper-export-document .question img{display:inline-block;vertical-align:middle;max-width:320px;max-height:240px;margin:4px 6px 4px 0;}
.paper-export-document .question table{border-collapse:collapse;margin:8px 0;max-width:100%;}
.paper-export-document .question td,.paper-export-document .question th{border:1px solid #333;padding:4px 8px;text-align:center;vertical-align:middle;}
.paper-export-document .option-text img{display:inline-block;vertical-align:middle;max-width:240px;max-height:180px;margin:2px 4px;}
.paper-export-document .qb-blank{display:inline-block;border-bottom:1px solid #303133;min-width:2em;padding:0 2px;}
.paper-export-document .katex{font-size:1.05em;}
`
  document.head.appendChild(styleEl)

  parts.push(`<div class="doc-title">${escapeHtml(exportPaperDisplayTitle(vm))}</div>`)
  const metaBits = []
  if (header.subjectName) metaBits.push('\u79d1\u76ee\uff1a' + escapeHtml(header.subjectName))
  if (header.duration) metaBits.push('\u65f6\u95f4\uff1a' + escapeHtml(header.duration))
  metaBits.push('\u6ee1\u5206\uff1a' + vm.basketTotalScore)
  parts.push(`<div class="doc-meta">${metaBits.join('&nbsp;&nbsp;&nbsp;')}</div>`)

  volumes.forEach(vol => {
    if (vol.showTitle && vol.title) {
      parts.push(`<div class="volume-title">${escapeHtml(vol.title)}</div>`)
    }
    vol.sections.forEach(section => {
      if (section.title) {
        parts.push(`<div class="section-title">${escapeHtml(section.title)}</div>`)
      }
      section.items.forEach(q => {
        parts.push('<div class="question">')
        parts.push(`<span class="q-no">${q.globalNo}.</span>`)
        const stemHtml = buildQuestionStemHtml(q, detailMap)
        parts.push(`<span class="q-text">${stemHtml || safeFormulaHtml(String(q.contentBrief || ''))}</span>`)
        optionItems(q, detailMap).forEach(opt => {
          parts.push('<div class="option-item">')
          parts.push(`<span class="option-label">${escapeHtml(opt.label)}.</span>`)
          parts.push(`<span class="option-text">${buildOptionStemHtml(opt)}</span>`)
          parts.push('</div>')
        })
        const area = vm.answerAreas[q.questionId]
        if (area && area.lines > 0) {
          if (area.style === 'blank') {
            const h = Math.max(48, area.lines * 28)
            parts.push(`<div class="answer-blank" style="min-height:${h}px"></div>`)
          } else {
            for (let i = 0; i < area.lines; i += 1) {
              parts.push('<div class="answer-line"></div>')
            }
          }
        }
        if (showTeacher && answerInline) {
          appendInlineAnswerParts(parts, vm, q)
        }
        parts.push('</div>')
      })
    })
  })

  if (showTeacher && !answerInline) {
    appendAnswerSheetParts(parts, vm)
  }

  root.innerHTML = parts.join('')
  return {
    root,
    pageSize,
    cleanup() {
      if (styleEl.parentNode) styleEl.parentNode.removeChild(styleEl)
    }
  }
}

export async function waitForImages(container) {
  const imgs = Array.from(container.querySelectorAll('img'))
  await Promise.all(imgs.map(img => {
    if (img.complete) return Promise.resolve()
    return new Promise(resolve => {
      img.onload = resolve
      img.onerror = resolve
    })
  }))
}
