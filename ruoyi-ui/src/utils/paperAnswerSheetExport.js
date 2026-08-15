import html2pdf from 'html2pdf.js'
import {
  escapeHtml,
  exportPaperDisplayTitle,
  exportPaperFilename,
  iterPaperQuestions,
  parseJsonArray,
  resolveExportVolumes,
  teacherDetail
} from '@/utils/paperExportCommon'
import { formatChoiceAnswer } from '@/utils/questionAnswer'
import {
  isFillQuestionType,
  isJudgeQuestionType,
  isPaperChoiceVolumeType
} from '@/utils/questionTypes'
import { OPTION_LABELS } from '@/utils/questionOptions'

export const DEFAULT_ANSWER_SHEET_OPTIONS = {
  showScore: true,
  choicePerRow: 5,
  judgePerRow: 10,
  showExamNumber: true,
  includeObjective: true,
  includeFill: true,
  includeSubjective: true,
  sheetMode: 'student',
  style: 'standard'
}

function waitForLayout() {
  return new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))
}

function mergeOptions(opts) {
  return { ...DEFAULT_ANSWER_SHEET_OPTIONS, ...(opts || {}) }
}

function chunkArray(list, size) {
  const chunks = []
  for (let i = 0; i < list.length; i += size) {
    chunks.push(list.slice(i, i + size))
  }
  return chunks
}

function optionCount(item) {
  if (isJudgeQuestionType(item.questionType)) return 2
  const arr = parseJsonArray(item.options)
  return Math.max(arr.length, 4)
}

function optionLabels(item) {
  if (isJudgeQuestionType(item.questionType)) {
    return ['\u221a', '\u00d7']
  }
  return OPTION_LABELS.slice(0, optionCount(item))
}

function getAnswerArea(vm, questionId) {
  return vm.answerAreas?.[questionId] || null
}

function classifyQuestions(questions) {
  const bubble = []
  const fill = []
  const subjective = []
  questions.forEach(q => {
    if (isPaperChoiceVolumeType(q.questionType)) {
      bubble.push(q)
    } else if (isFillQuestionType(q.questionType)) {
      fill.push(q)
    } else {
      subjective.push(q)
    }
  })
  return { bubble, fill, subjective }
}

export function getAnswerSheetStats(vm) {
  const questions = iterPaperQuestions(vm)
  const { bubble, fill, subjective } = classifyQuestions(questions)
  const totalScore = questions.reduce((s, q) => s + (Number(q.scoreValue) || 0), 0)
  return {
    total: questions.length,
    objective: bubble.length,
    fill: fill.length,
    subjective: subjective.length,
    totalScore
  }
}

function resolveTeacherAnswer(vm, item) {
  const d = teacherDetail(vm, item)
  const raw = d.correctAnswer
  if (raw == null || raw === '') return '\u2014'
  const formatted = formatChoiceAnswer(item.questionType, raw)
  if (formatted) return formatted
  return String(raw).replace(/\s+/g, ' ').trim() || '\u2014'
}

function buildExamNumberGrid() {
  const digits = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']
  const rows = []
  for (let pos = 1; pos <= 10; pos += 1) {
    const cells = digits.map(d =>
      `<td class="as-exam-cell" style="border:1px solid #bbb;text-align:center;padding:3px 2px;"><span class="as-exam-bubble" style="display:inline-flex;align-items:center;justify-content:center;width:16px;height:16px;border:1px solid #333;border-radius:50%;font-size:9px;">${d}</span></td>`
    ).join('')
    rows.push(`<tr>
      <td class="as-exam-pos" style="border:1px solid #bbb;text-align:center;padding:3px 2px;background:#f5f5f5;font-weight:600;">\u7b2c${pos}\u4f4d</td>${cells}
    </tr>`)
  }
  return `<div class="as-exam-block" style="margin-bottom:16px;border:1px solid #666;padding:8px 10px;">
    <div class="as-exam-label" style="font-weight:600;margin-bottom:6px;font-size:11px;">\u8003\u53f7\u586b\u6d82\uff08\u8bf7\u7528 2B \u94c5\u7b14\u6d82\u6ee1\u5706\u5708\uff09</div>
    <table class="as-exam-table" style="width:100%;border-collapse:collapse;table-layout:fixed;"><tbody>${rows.join('')}</tbody></table>
  </div>`
}

function buildBubbleBlock(chunk, opts) {
  if (!chunk.length) return ''
  const labelSet = new Set()
  chunk.forEach(q => optionLabels(q).forEach(l => labelSet.add(l)))
  const labels = OPTION_LABELS.filter(l => labelSet.has(l))
  if (labelSet.has('\u221a')) {
    labels.length = 0
    labels.push('\u221a', '\u00d7')
  }

  const headerCells = chunk.map(q =>
    `<th class="as-grid-num" style="border:1px solid #666;background:#f0f0f0;font-weight:700;font-size:11px;padding:4px 2px;">${q.globalNo}${opts.showScore ? `<i style="display:block;font-style:normal;font-weight:400;font-size:9px;color:#666;">${Number(q.scoreValue) || 0}\u5206</i>` : ''}</th>`
  ).join('')

  const bodyRows = labels.map(label => {
    const cells = chunk.map(q => {
      const qLabels = optionLabels(q)
      if (!qLabels.includes(label)) {
        return '<td class="as-grid-cell as-grid-empty"></td>'
      }
      return `<td class="as-grid-cell" style="border:1px solid #666;text-align:center;padding:5px 2px;"><span class="as-bubble-inner" style="display:inline-flex;align-items:center;justify-content:center;width:20px;height:20px;border:1.5px solid #222;border-radius:50%;font-size:10px;font-weight:700;">${label}</span></td>`
    }).join('')
    return `<tr><th class="as-grid-opt" style="border:1px solid #666;background:#f0f0f0;font-weight:700;width:28px;">${label}</th>${cells}</tr>`
  }).join('')

  return `<table class="as-grid-table" style="width:100%;border-collapse:collapse;margin-bottom:12px;table-layout:fixed;">
    <thead><tr><th class="as-grid-corner" style="border:1px solid #666;background:#f0f0f0;width:28px;"></th>${headerCells}</tr></thead>
    <tbody>${bodyRows}</tbody>
  </table>`
}

function buildBubbleSection(questions, opts, title) {
  if (!questions.length) return ''
  const chunks = chunkArray(questions, opts.choicePerRow)
  const grids = chunks.map(chunk => buildBubbleBlock(chunk, opts)).join('')
  return `<div class="as-section">
    <div class="as-section-title">${title}</div>
    ${grids}
  </div>`
}

function buildBubbleSections(questions, opts) {
  if (!questions.length) return ''
  const judge = questions.filter(q => isJudgeQuestionType(q.questionType))
  const choice = questions.filter(q => !isJudgeQuestionType(q.questionType))
  const parts = []
  if (choice.length) {
    const hint = choice.some(q => q.questionType === 'multi')
      ? '\uff08\u591a\u9009\u9898\u53ef\u586b\u6d82\u591a\u4e2a\u9009\u9879\uff09'
      : ''
    parts.push(buildBubbleSection(
      choice,
      opts,
      '\u5ba2\u89c2\u9898\u586b\u6d82\u533a\uff08\u8bf7\u5728\u76f8\u5e94\u9009\u9879\u4e0a\u6d82\u6ee1\u5708\uff09' + hint
    ))
  }
  if (judge.length) {
    parts.push(buildBubbleSection(
      judge,
      { ...opts, choicePerRow: opts.judgePerRow },
      '\u5224\u65ad\u9898\u586b\u6d82\u533a\uff08\u221a \u00d7 \u4e8c\u9009\u4e00\uff09'
    ))
  }
  return parts.join('')
}

function buildFillSection(questions, opts) {
  if (!questions.length) return ''
  const rows = questions.map(q => {
    const score = opts.showScore
      ? `<span class="as-fill-score">${Number(q.scoreValue) || 0}\u5206</span>`
      : ''
    return `<div class="as-fill-row" style="display:flex;align-items:flex-end;gap:8px;margin:8px 0;">
      <span class="as-fill-no" style="font-weight:700;min-width:28px;">${q.globalNo}.</span>
      <span class="as-fill-line" style="flex:1;border-bottom:1px solid #333;height:20px;"></span>
      ${score}
    </div>`
  }).join('')
  return `<div class="as-section" style="margin-bottom:16px;">
    <div class="as-section-title" style="font-weight:600;margin-bottom:8px;font-size:12px;">\u586b\u7a7a\u9898\uff08\u8bf7\u5728\u6a2a\u7ebf\u4e0a\u4f5c\u7b54\uff09</div>
    <div class="as-fill-list" style="border:1px solid #666;padding:8px 12px;">${rows}</div>
  </div>`
}

function buildSubjectiveSection(questions, vm, opts) {
  if (!questions.length) return ''
  const rows = questions.map(q => {
    const area = getAnswerArea(vm, q.questionId)
    const lines = area?.lines || 4
    const isBlank = area?.style === 'blank'
    const score = opts.showScore
      ? `<span class="as-sub-score">${Number(q.scoreValue) || 0}\u5206</span>`
      : ''
    let body
    if (isBlank) {
      body = `<div class="as-sub-blank" style="min-height:${Math.max(56, lines * 24)}px"></div>`
    } else {
      body = Array.from({ length: lines }, () => '<div class="as-blank-line"></div>').join('')
      body = `<div class="as-sub-ruled">${body}</div>`
    }
    return `<div class="as-sub-row">
      <div class="as-sub-head"><span class="as-sub-no">${q.globalNo}.</span>${score}</div>
      ${body}
    </div>`
  }).join('')
  return `<div class="as-section">
    <div class="as-section-title">\u975e\u9009\u62e9\u9898\uff08\u8bf7\u5728\u7b54\u9898\u533a\u4f5c\u7b54\uff09</div>
    <div class="as-sub-list">${rows}</div>
  </div>`
}

function buildAnswerKeySection(vm, opts) {
  const questions = iterPaperQuestions(vm)
  if (!questions.length) return ''
  const rows = questions.map(q => {
    const ans = escapeHtml(resolveTeacherAnswer(vm, q))
    const score = opts.showScore ? `<span class="as-key-score">${Number(q.scoreValue) || 0}\u5206</span>` : ''
    return `<tr>
      <td class="as-key-no">${q.globalNo}</td>
      <td class="as-key-ans">${ans}</td>
      <td class="as-key-score-cell">${score}</td>
    </tr>`
  }).join('')
  return `<div class="as-answer-key page-break-before">
    <div class="as-section-title">\u53c2\u8003\u7b54\u6848\uff08\u6559\u5e08\u7528\uff0c\u8bf7\u52ff\u53d1\u653e\u7ed9\u5b66\u751f\uff09</div>
    <table class="as-key-table">
      <thead><tr>
        <th>\u9898\u53f7</th><th>\u7b54\u6848</th><th>\u5206\u503c</th>
      </tr></thead>
      <tbody>${rows}</tbody>
    </table>
  </div>`
}

function buildVolumeContent(questions, vm, opts) {
  const { bubble, fill, subjective } = classifyQuestions(questions)
  const parts = []
  if (opts.includeObjective && bubble.length) {
    parts.push(buildBubbleSections(bubble, opts))
  }
  if (opts.includeFill && fill.length) {
    parts.push(buildFillSection(fill, opts))
  }
  if (opts.includeSubjective && subjective.length) {
    parts.push(buildSubjectiveSection(subjective, vm, opts))
  }
  return parts.join('')
}

function buildSectionsHtml(vm, opts) {
  const volumes = resolveExportVolumes(vm)
  const parts = []
  volumes.forEach(vol => {
    if (vol.showTitle && vol.title) {
      parts.push(`<div class="as-volume-title">${escapeHtml(vol.title)}</div>`)
    }
    vol.sections.forEach(section => {
      const questions = section.items || []
      if (!questions.length) return
      if (section.title) {
        parts.push(`<div class="as-part-title">${escapeHtml(section.title)}</div>`)
      }
      parts.push(buildVolumeContent(questions, vm, opts))
    })
  })
  const html = parts.length ? parts.join('') : buildVolumeContent(iterPaperQuestions(vm), vm, opts)
  if (html.includes('as-section') || html.includes('as-grid-table') || html.includes('as-fill-list')) {
    return html
  }
  return buildVolumeContent(iterPaperQuestions(vm), vm, opts)
}

function buildSummaryHtml(vm) {
  const questions = iterPaperQuestions(vm)
  const totalScore = questions.reduce((s, q) => s + (Number(q.scoreValue) || 0), 0)
  return `\u5171 ${questions.length} \u9898\uff0c\u6ee1\u5206 ${totalScore} \u5206`
}

function buildStylesCss(opts) {
  const compact = opts.style === 'compact'
  const bubble = compact ? '17px' : '20px'
  const font = compact ? '11px' : '12px'
  const title = compact ? '17px' : '20px'
  return `
    .answer-sheet-export-root {
      font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
      font-size: ${font};
      color: #1a1a1a;
      line-height: 1.45;
      padding: 10px 12px 16px;
      background: #fff;
      box-sizing: border-box;
    }
    .page-break-before { page-break-before: always; break-before: page; }
    .as-main-title { text-align: center; font-size: ${title}; font-weight: 700; margin: 0 0 6px; letter-spacing: 2px; }
    .as-sub-title { text-align: center; font-size: 14px; margin: 0 0 10px; color: #333; font-weight: 600; }
    .as-summary { text-align: center; font-size: ${font}; color: #555; margin-bottom: 14px; }
    .as-meta {
      display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px 20px;
      margin-bottom: 14px; padding: 10px 12px; border: 1px solid #333;
    }
    .as-meta-item u {
      display: inline-block; min-width: 100px; border-bottom: 1px solid #333;
      text-decoration: none; height: 18px; vertical-align: bottom;
    }
    .as-exam-block { margin-bottom: 16px; border: 1px solid #666; padding: 8px 10px; }
    .as-exam-label { font-weight: 600; margin-bottom: 6px; font-size: 11px; color: #333; }
    .as-exam-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
    .as-exam-table td { border: 1px solid #bbb; text-align: center; padding: 3px 2px; font-size: 10px; }
    .as-exam-pos { width: 42px; background: #f5f5f5; font-weight: 600; white-space: nowrap; }
    .as-exam-bubble {
      display: inline-flex; align-items: center; justify-content: center;
      width: 16px; height: 16px; border: 1px solid #333; border-radius: 50%; font-size: 9px;
    }
    .as-volume-title {
      text-align: center; font-size: 14px; font-weight: 700; margin: 14px 0 8px;
      padding: 4px 0; border-top: 2px solid #333; border-bottom: 1px solid #333;
    }
    .as-part-title { font-size: 12px; font-weight: 600; margin: 10px 0 6px; color: #333; }
    .as-section { margin-bottom: 16px; page-break-inside: avoid; }
    .as-section-title { font-weight: 600; margin-bottom: 8px; font-size: 12px; }
    .as-grid-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; table-layout: fixed; }
    .as-grid-table th, .as-grid-table td { border: 1px solid #666; text-align: center; vertical-align: middle; }
    .as-grid-corner { width: 28px; background: #f0f0f0; }
    .as-grid-opt { width: 28px; background: #f0f0f0; font-weight: 700; }
    .as-grid-num { background: #f0f0f0; font-weight: 700; font-size: 11px; padding: 4px 2px; }
    .as-grid-num i { display: block; font-style: normal; font-weight: 400; font-size: 9px; color: #666; margin-top: 2px; }
    .as-grid-cell { padding: ${compact ? '3px 1px' : '5px 2px'}; }
    .as-grid-empty { background: #fafafa; }
    .as-bubble-inner {
      display: inline-flex; align-items: center; justify-content: center;
      width: ${bubble}; height: ${bubble}; border: 1.5px solid #222; border-radius: 50%;
      font-size: 10px; font-weight: 700;
    }
    .as-fill-list { border: 1px solid #666; padding: 8px 12px; }
    .as-fill-row { display: flex; align-items: flex-end; gap: 8px; margin: 8px 0; }
    .as-fill-no { font-weight: 700; min-width: 28px; }
    .as-fill-line { flex: 1; border-bottom: 1px solid #333; height: 20px; }
    .as-fill-score { font-size: 11px; color: #555; white-space: nowrap; }
    .as-sub-list { border: 1px solid #666; padding: 10px 12px; }
    .as-sub-row { margin-bottom: 14px; page-break-inside: avoid; }
    .as-sub-row:last-child { margin-bottom: 0; }
    .as-sub-head { display: flex; justify-content: space-between; margin-bottom: 4px; }
    .as-sub-no { font-weight: 700; }
    .as-sub-score { font-size: 11px; color: #555; }
    .as-sub-ruled { min-height: 48px; }
    .as-blank-line { border-bottom: 1px solid #999; height: 22px; margin-bottom: 4px; }
    .as-sub-blank { border: 1px dashed #888; background: #fafafa; }
    .as-key-table { width: 100%; border-collapse: collapse; margin-top: 8px; }
    .as-key-table th, .as-key-table td { border: 1px solid #666; padding: 5px 8px; text-align: left; }
    .as-key-table th { background: #f0f0f0; text-align: center; }
    .as-key-no { width: 48px; text-align: center; font-weight: 700; }
    .as-key-score-cell { width: 56px; text-align: center; white-space: nowrap; }
    .as-key-score { font-size: 11px; color: #555; }
    .as-footer {
      margin-top: 12px; padding-top: 8px; border-top: 1px dashed #aaa;
      font-size: 10px; color: #666; text-align: center; line-height: 1.6;
    }
  `
}

function attachAnswerSheetStyles(opts) {
  const styleEl = document.createElement('style')
  styleEl.setAttribute('data-answer-sheet-export', '1')
  styleEl.textContent = buildStylesCss(opts)
  document.head.appendChild(styleEl)
  return () => {
    if (styleEl.parentNode) styleEl.parentNode.removeChild(styleEl)
  }
}

function buildAnswerSheetMarkup(vm, options) {
  const title = exportPaperDisplayTitle(vm)
  const header = vm.form?.header || {}
  const school = escapeHtml(header.schoolName || '')
  const subject = escapeHtml(header.subjectName || '')
  const duration = escapeHtml(header.duration || '')
  const sectionsHtml = buildSectionsHtml(vm, options)
  const examHtml = options.showExamNumber ? buildExamNumberGrid() : ''
  const answerKeyHtml = options.sheetMode === 'teacher' ? buildAnswerKeySection(vm, options) : ''
  const subTitle = options.sheetMode === 'teacher'
    ? '\u7b54 \u9898 \u5361\uff08\u6559\u5e08\u53c2\u8003\u7248\uff09'
    : '\u7b54 \u9898 \u5361'
  return `
  <h1 class="as-main-title">${escapeHtml(title)}</h1>
  <p class="as-sub-title">${subTitle}</p>
  <div class="as-summary">${buildSummaryHtml(vm)}</div>
  <div class="as-meta">
    <span class="as-meta-item">\u5b66\u6821\uff1a<u>${school || '&nbsp;'}</u></span>
    <span class="as-meta-item">\u59d3\u540d\uff1a<u>&nbsp;</u></span>
    <span class="as-meta-item">\u73ed\u7ea7\uff1a<u>&nbsp;</u></span>
    <span class="as-meta-item">\u8003\u53f7\uff1a<u>&nbsp;</u></span>
    ${subject ? `<span class="as-meta-item">\u79d1\u76ee\uff1a${subject}</span>` : ''}
    ${duration ? `<span class="as-meta-item">\u65f6\u957f\uff1a${duration}</span>` : ''}
  </div>
  ${examHtml}
  ${sectionsHtml}
  ${answerKeyHtml}
  <div class="as-footer">
    \u6ce8\u610f\u4e8b\u9879\uff1a1.\u8bf7\u4f7f\u7528 2B \u94c5\u7b14\u586b\u6d82\uff1b2.\u6d82\u5361\u8981\u6d82\u6ee1\u6d82\u9ed1\uff1b3.\u4fdd\u6301\u5361\u9762\u5e73\u6574\u6d01\uff1b4.\u9009\u62e9\u9898\u53ea\u80fd\u6d82\u6ee1\u4e00\u4e2a\u9009\u9879\uff08\u591a\u9009\u9898\u9664\u5916\uff09\u3002
  </div>`
}

export function buildAnswerSheetElement(vm, opts, embedStyle = false) {
  const options = mergeOptions(opts)
  let cleanup = null
  let stylePrefix = ''
  if (embedStyle) {
    stylePrefix = `<style>${buildStylesCss(options)}</style>`
  } else if (typeof document !== 'undefined') {
    cleanup = attachAnswerSheetStyles(options)
  }
  const root = document.createElement('div')
  root.className = 'answer-sheet-export-root'
  root.innerHTML = stylePrefix + buildAnswerSheetMarkup(vm, options)
  return { root, cleanup }
}

export function buildAnswerSheetHtml(vm, opts) {
  const { root } = buildAnswerSheetElement(vm, opts, true)
  return root.innerHTML
}

async function prepareAnswerSheetData(vm, opts) {
  if (typeof vm.ensureExportDetails === 'function') {
    await vm.ensureExportDetails()
  }
  const options = mergeOptions(opts)
  if (options.sheetMode === 'teacher' && typeof vm.ensureTeacherDetails === 'function') {
    await vm.ensureTeacherDetails()
  }
  return options
}

function resolvePageSize(vm) {
  const pageLayout = (vm.pageLayout || vm.form?.templateCode || 'A4').toUpperCase()
  const pageSize = pageLayout.includes('A3') ? 'a3' : 'a4'
  const pageWidth = pageSize === 'a3' ? '1123px' : '794px'
  return { pageSize, pageWidth }
}

function answerSheetFilename(vm, opts) {
  const base = exportPaperFilename(vm, 'pdf').replace(/\.pdf$/i, '')
  const suffix = mergeOptions(opts).sheetMode === 'teacher' ? '\u7b54\u9898\u5361-\u6559\u5e08\u7248' : '\u7b54\u9898\u5361'
  return `${base}-${suffix}.pdf`
}

function canvasHasInk(canvas) {
  if (!canvas || canvas.width < 2 || canvas.height < 2) return false
  const ctx = canvas.getContext('2d')
  if (!ctx) return false
  const { width, height } = canvas
  const stepX = Math.max(1, Math.floor(width / 24))
  const stepY = Math.max(1, Math.floor(height / 24))
  for (let y = 0; y < height; y += stepY) {
    for (let x = 0; x < width; x += stepX) {
      const pixel = ctx.getImageData(x, y, 1, 1).data
      if (pixel[0] < 248 || pixel[1] < 248 || pixel[2] < 248) return true
    }
  }
  return false
}

function hasAnswerSheetBody(root) {
  if (!root) return false
  return !!(
    root.querySelector('.as-section') ||
    root.querySelector('.as-grid-table') ||
    root.querySelector('.as-fill-list') ||
    root.querySelector('.as-sub-list') ||
    root.querySelector('.as-exam-block')
  )
}

async function createAnswerSheetCaptureNode(vm, opts) {
  const options = await prepareAnswerSheetData(vm, opts)
  const html = buildAnswerSheetHtml(vm, options)
  const { pageWidth } = resolvePageSize(vm)
  const host = document.createElement('div')
  host.setAttribute('data-answer-sheet-capture', '1')
  host.style.cssText = [
    'position:fixed',
    'left:0',
    'top:0',
    `width:${pageWidth}`,
    `max-width:${pageWidth}`,
    'background:#fff',
    'z-index:99999',
    'opacity:1',
    'visibility:visible',
    'overflow:visible',
    'pointer-events:none',
    'box-sizing:border-box'
  ].join(';')
  host.innerHTML = `<div class="answer-sheet-export-root" style="width:${pageWidth};box-sizing:border-box;background:#fff;color:#1a1a1a;font-family:Microsoft YaHei,PingFang SC,sans-serif;font-size:12px;line-height:1.45;padding:10px 12px 16px;">${html}</div>`
  document.body.appendChild(host)
  await waitForLayout()
  if (document.fonts && document.fonts.ready) {
    await document.fonts.ready
  }
  await new Promise(resolve => setTimeout(resolve, 180))
  const root = host.querySelector('.answer-sheet-export-root')
  const questionCount = iterPaperQuestions(vm).length
  const text = (root?.textContent || '').replace(/\s+/g, ' ').trim()
  if (!root || !text || (questionCount > 0 && !hasAnswerSheetBody(root))) {
    host.remove()
    throw new Error('answer sheet content empty')
  }
  root.style.height = Math.max(root.scrollHeight, root.offsetHeight, 200) + 'px'
  return { host, root, options, cleanup: () => host.remove() }
}

async function captureAnswerSheetPdf(element, filename, pageSize, pageWidth) {
  const width = element.scrollWidth || parseInt(pageWidth, 10) || 794
  const height = Math.max(element.scrollHeight, element.offsetHeight, 200)
  try {
    const html2canvas = (await import(/* webpackChunkName: "html2canvas" */ 'html2canvas')).default
    const { jsPDF } = await import(/* webpackChunkName: "jspdf" */ 'jspdf')
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#ffffff',
      logging: false,
      scrollX: 0,
      scrollY: 0,
      windowWidth: width,
      width,
      height,
      windowHeight: height,
      onclone: (_doc, node) => {
        node.style.transform = 'none'
        node.style.opacity = '1'
        node.style.visibility = 'visible'
        node.style.background = '#fff'
      }
    })
    if (!canvasHasInk(canvas)) {
      throw new Error('answer sheet render blank')
    }
    const pdf = new jsPDF({ unit: 'mm', format: pageSize, orientation: 'portrait' })
    const pageWidthMm = pdf.internal.pageSize.getWidth()
    const pageHeightMm = pdf.internal.pageSize.getHeight()
    const margin = 10
    const contentWidth = pageWidthMm - margin * 2
    const contentHeight = pageHeightMm - margin * 2
    const imgWidth = contentWidth
    const imgHeight = (canvas.height * imgWidth) / canvas.width
    const imgData = canvas.toDataURL('image/jpeg', 0.98)
    let heightLeft = imgHeight
    let offsetY = margin
    pdf.addImage(imgData, 'JPEG', margin, offsetY, imgWidth, imgHeight)
    heightLeft -= contentHeight
    while (heightLeft > 0) {
      offsetY = margin - (imgHeight - heightLeft)
      pdf.addPage()
      pdf.addImage(imgData, 'JPEG', margin, offsetY, imgWidth, imgHeight)
      heightLeft -= contentHeight
    }
    pdf.save(filename)
    return
  } catch (err) {
    if (err && err.message === 'answer sheet render blank') throw err
  }
  await html2pdf().set({
    margin: [10, 10, 10, 10],
    filename,
    image: { type: 'jpeg', quality: 0.98 },
    html2canvas: {
      scale: 2,
      useCORS: true,
      allowTaint: true,
      logging: false,
      backgroundColor: '#ffffff',
      scrollX: 0,
      scrollY: 0,
      windowWidth: width,
      width
    },
    jsPDF: { unit: 'mm', format: pageSize, orientation: 'portrait' },
    pagebreak: { mode: ['css', 'legacy'] }
  }).from(element).save()
}

export async function exportAnswerSheetClient(vm, options) {
  const { root, options: opts, cleanup } = await createAnswerSheetCaptureNode(vm, options)
  const { pageSize, pageWidth } = resolvePageSize(vm)
  try {
    await captureAnswerSheetPdf(root, answerSheetFilename(vm, opts), pageSize, pageWidth)
  } finally {
    cleanup()
  }
}

export async function printAnswerSheetClient(vm, options) {
  const opts = await prepareAnswerSheetData(vm, options)
  const html = buildAnswerSheetHtml(vm, opts)
  const { pageWidth } = resolvePageSize(vm)
  const win = window.open('', '_blank')
  if (!win) throw new Error('print blocked')
  win.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>\u7b54\u9898\u5361</title>
    <style>body{margin:0;padding:12px;background:#fff;} @media print { body { padding: 0; } }</style>
    </head><body style="width:${pageWidth}"><div class="answer-sheet-export-root" style="width:${pageWidth};box-sizing:border-box;background:#fff;color:#1a1a1a;">${html}</div></body></html>`)
  win.document.close()
  win.focus()
  setTimeout(() => {
    win.print()
    win.close()
  }, 400)
}

export async function buildAnswerSheetPreviewHtml(vm, options) {
  const opts = await prepareAnswerSheetData(vm, options)
  return buildAnswerSheetHtml(vm, opts)
}
