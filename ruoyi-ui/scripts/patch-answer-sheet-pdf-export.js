/**
 * Fix blank answer-sheet PDF: move styles to document.head (like paper export)
 * and pass explicit html2canvas height. Run: node scripts/patch-answer-sheet-pdf-export.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/utils/paperAnswerSheetExport.js')
let text = fs.readFileSync(file, 'utf8')

const oldBuildStylesStart = 'function buildStyles(opts) {'
const oldExportStart = 'export function buildAnswerSheetElement(vm, opts) {'

if (!text.includes(oldBuildStylesStart) || !text.includes(oldExportStart)) {
  console.error('markers not found')
  process.exit(1)
}

const styleIdx = text.indexOf(oldBuildStylesStart)
const exportIdx = text.indexOf(oldExportStart)

const beforeStyles = text.slice(0, styleIdx)
const afterExport = text.slice(exportIdx)

const middleAndExport = `function buildStylesCss(opts) {
  const compact = opts.style === 'compact'
  const bubble = compact ? '17px' : '20px'
  const font = compact ? '11px' : '12px'
  const title = compact ? '17px' : '20px'
  return \`
    .answer-sheet-export-root {
      font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
      font-size: \${font};
      color: #1a1a1a;
      line-height: 1.45;
      padding: 10px 12px 16px;
      background: #fff;
      box-sizing: border-box;
    }
    .page-break-before { page-break-before: always; break-before: page; }
    .as-main-title { text-align: center; font-size: \${title}; font-weight: 700; margin: 0 0 6px; letter-spacing: 2px; }
    .as-sub-title { text-align: center; font-size: 14px; margin: 0 0 10px; color: #333; font-weight: 600; }
    .as-summary { text-align: center; font-size: \${font}; color: #555; margin-bottom: 14px; }
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
    .as-grid-cell { padding: \${compact ? '3px 1px' : '5px 2px'}; }
    .as-grid-empty { background: #fafafa; }
    .as-bubble-inner {
      display: inline-flex; align-items: center; justify-content: center;
      width: \${bubble}; height: \${bubble}; border: 1.5px solid #222; border-radius: 50%;
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
  \`
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
    ? '\\u7b54 \\u9898 \\u5361\\uff08\\u6559\\u5e08\\u53c2\\u8003\\u7248\\uff09'
    : '\\u7b54 \\u9898 \\u5361'
  return \`
  <h1 class="as-main-title">\${escapeHtml(title)}</h1>
  <p class="as-sub-title">\${subTitle}</p>
  <div class="as-summary">\${buildSummaryHtml(vm)}</div>
  <div class="as-meta">
    <span class="as-meta-item">\\u5b66\\u6821\\uff1a<u>\${school || '&nbsp;'}</u></span>
    <span class="as-meta-item">\\u59d3\\u540d\\uff1a<u>&nbsp;</u></span>
    <span class="as-meta-item">\\u73ed\\u7ea7\\uff1a<u>&nbsp;</u></span>
    <span class="as-meta-item">\\u8003\\u53f7\\uff1a<u>&nbsp;</u></span>
    \${subject ? \`<span class="as-meta-item">\\u79d1\\u76ee\\uff1a\${subject}</span>\` : ''}
    \${duration ? \`<span class="as-meta-item">\\u65f6\\u957f\\uff1a\${duration}</span>\` : ''}
  </div>
  \${examHtml}
  \${sectionsHtml}
  \${answerKeyHtml}
  <div class="as-footer">
    \\u6ce8\\u610f\\u4e8b\\u9879\\uff1a1.\\u8bf7\\u4f7f\\u7528 2B \\u94c5\\u7b14\\u586b\\u6d82\\uff1b2.\\u6d82\\u5361\\u8981\\u6d82\\u6ee1\\u6d82\\u9ed1\\uff1b3.\\u4fdd\\u6301\\u5361\\u9762\\u5e73\\u6574\\u6d01\\uff1b4.\\u9009\\u62e9\\u9898\\u53ea\\u80fd\\u6d82\\u6ee1\\u4e00\\u4e2a\\u9009\\u9879\\uff08\\u591a\\u9009\\u9898\\u9664\\u5916\\uff09\\u3002
  </div>\`
}

export function buildAnswerSheetElement(vm, opts, embedStyle = false) {
  const options = mergeOptions(opts)
  let cleanup = null
  let stylePrefix = ''
  if (embedStyle) {
    stylePrefix = \`<style>\${buildStylesCss(options)}</style>\`
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
`

// Replace from buildStyles through start of prepareAnswerSheetData (exclusive of duplicate)
const exportEnd = afterExport.indexOf('async function prepareAnswerSheetData')
if (exportEnd < 0) {
  console.error('prepareAnswerSheetData not found')
  process.exit(1)
}

const tail = afterExport.slice(exportEnd)

// Remove old buildAnswerSheetHtml if still in tail
const tailClean = tail.replace(
  /export function buildAnswerSheetHtml\(vm, opts\) \{\s*return buildAnswerSheetElement\(vm, opts\)\.innerHTML\s*\}\s*\n/,
  ''
)

const renderOld = `async function renderAnswerSheetToDom(vm, opts) {
  const options = await prepareAnswerSheetData(vm, opts)
  const root = buildAnswerSheetElement(vm, options)
  const { pageWidth } = resolvePageSize(vm)
  root.style.position = 'fixed'
  root.style.left = '0'
  root.style.top = '0'
  root.style.width = pageWidth
  root.style.maxWidth = pageWidth
  root.style.zIndex = '99999'
  root.style.pointerEvents = 'none'
  root.style.background = '#fff'
  document.body.appendChild(root)
  await waitForLayout()
  if (document.fonts && document.fonts.ready) {
    await document.fonts.ready
  }
  const questionCount = iterPaperQuestions(vm).length
  const text = (root.textContent || '').replace(/\\s+/g, ' ').trim()
  if (!text || (questionCount > 0 && text.length < 8)) {
    if (root.parentNode) root.parentNode.removeChild(root)
    throw new Error('answer sheet content empty')
  }
  return { root, options }
}`

const renderNew = `async function renderAnswerSheetToDom(vm, opts) {
  const options = await prepareAnswerSheetData(vm, opts)
  const { root, cleanup } = buildAnswerSheetElement(vm, options, false)
  const { pageWidth } = resolvePageSize(vm)
  root.style.position = 'fixed'
  root.style.left = '0'
  root.style.top = '0'
  root.style.width = pageWidth
  root.style.maxWidth = pageWidth
  root.style.zIndex = '99999'
  root.style.pointerEvents = 'none'
  root.style.background = '#fff'
  root.style.overflow = 'visible'
  document.body.appendChild(root)
  await waitForLayout()
  if (document.fonts && document.fonts.ready) {
    await document.fonts.ready
  }
  await new Promise(resolve => setTimeout(resolve, 80))
  const captureHeight = Math.max(root.scrollHeight, root.offsetHeight, 400)
  root.style.minHeight = captureHeight + 'px'
  const questionCount = iterPaperQuestions(vm).length
  const text = (root.textContent || '').replace(/\\s+/g, ' ').trim()
  if (!text || (questionCount > 0 && text.length < 8)) {
    if (root.parentNode) root.parentNode.removeChild(root)
    if (cleanup) cleanup()
    throw new Error('answer sheet content empty')
  }
  return { root, options, cleanup, captureHeight }
}`

let full = beforeStyles + middleAndExport + tailClean
if (!full.includes(renderOld)) {
  console.error('renderAnswerSheetToDom block not found')
  process.exit(1)
}
full = full.replace(renderOld, renderNew)

const exportOld = `export async function exportAnswerSheetClient(vm, options) {
  const { root, options: opts } = await renderAnswerSheetToDom(vm, options)
  const { pageSize, pageWidth } = resolvePageSize(vm)
  try {
    await html2pdf().set({
      margin: [10, 10, 10, 10],
      filename: answerSheetFilename(vm, opts),
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: {
        scale: 2,
        useCORS: true,
        allowTaint: true,
        logging: false,
        backgroundColor: '#ffffff',
        scrollX: 0,
        scrollY: -window.scrollY,
        windowWidth: root.scrollWidth || parseInt(pageWidth, 10),
        width: root.scrollWidth || parseInt(pageWidth, 10)
      },
      jsPDF: { unit: 'mm', format: pageSize, orientation: 'portrait' },
      pagebreak: { mode: ['css', 'legacy'], before: '.as-volume-title, .page-break-before' }
    }).from(root).save()
  } finally {
    if (root.parentNode) root.parentNode.removeChild(root)
  }
}`

const exportNew = `export async function exportAnswerSheetClient(vm, options) {
  const { root, options: opts, cleanup, captureHeight } = await renderAnswerSheetToDom(vm, options)
  const { pageSize, pageWidth } = resolvePageSize(vm)
  const canvasWidth = root.scrollWidth || parseInt(pageWidth, 10)
  const canvasHeight = captureHeight || root.scrollHeight || 1123
  try {
    await html2pdf().set({
      margin: [10, 10, 10, 10],
      filename: answerSheetFilename(vm, opts),
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: {
        scale: 2,
        useCORS: true,
        allowTaint: true,
        logging: false,
        backgroundColor: '#ffffff',
        scrollX: 0,
        scrollY: 0,
        windowWidth: canvasWidth,
        windowHeight: canvasHeight,
        width: canvasWidth,
        height: canvasHeight
      },
      jsPDF: { unit: 'mm', format: pageSize, orientation: 'portrait' },
      pagebreak: { mode: ['css', 'legacy'], before: '.as-volume-title, .page-break-before' }
    }).from(root).save()
  } finally {
    if (root.parentNode) root.parentNode.removeChild(root)
    if (cleanup) cleanup()
  }
}`

if (!full.includes(exportOld)) {
  console.error('exportAnswerSheetClient block not found')
  process.exit(1)
}
full = full.replace(exportOld, exportNew)

const printOld = `export async function printAnswerSheetClient(vm, options) {
  const { root } = await renderAnswerSheetToDom(vm, options)
  const { pageWidth } = resolvePageSize(vm)
  try {
    const win = window.open('', '_blank')
    if (!win) throw new Error('print blocked')
    win.document.write(\`<!DOCTYPE html><html><head><meta charset="utf-8"><title>\\u7b54\\u9898\\u5361</title>
      <style>body{margin:0;padding:12px;background:#fff;} @media print { body { padding: 0; } }</style>
      </head><body style="width:\${pageWidth}">\${root.innerHTML}</body></html>\`)
    win.document.close()
    win.focus()
    setTimeout(() => {
      win.print()
      win.close()
    }, 400)
  } finally {
    if (root.parentNode) root.parentNode.removeChild(root)
  }
}`

const printNew = `export async function printAnswerSheetClient(vm, options) {
  const { root, cleanup } = await renderAnswerSheetToDom(vm, options)
  const { pageWidth } = resolvePageSize(vm)
  const opts = mergeOptions(options)
  try {
    const win = window.open('', '_blank')
    if (!win) throw new Error('print blocked')
    win.document.write(\`<!DOCTYPE html><html><head><meta charset="utf-8"><title>\\u7b54\\u9898\\u5361</title>
      <style>body{margin:0;padding:12px;background:#fff;} @media print { body { padding: 0; } }</style>
      <style>\${buildStylesCss(opts)}</style>
      </head><body style="width:\${pageWidth}"><div class="answer-sheet-export-root">\${root.innerHTML}</div></body></html>\`)
    win.document.close()
    win.focus()
    setTimeout(() => {
      win.print()
      win.close()
    }, 400)
  } finally {
    if (root.parentNode) root.parentNode.removeChild(root)
    if (cleanup) cleanup()
  }
}`

if (!full.includes(printOld)) {
  console.error('printAnswerSheetClient block not found')
  process.exit(1)
}
full = full.replace(printOld, printNew)

fs.writeFileSync(file, full, 'utf8')
console.log('patched', file)
