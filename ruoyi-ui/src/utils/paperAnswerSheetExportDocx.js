import {
  AlignmentType,
  BorderStyle,
  Document,
  Packer,
  PageOrientation,
  Paragraph,
  Table,
  TableCell,
  TableRow,
  TextRun,
  VerticalAlign,
  WidthType
} from 'docx'
import { saveAs } from 'file-saver'
import { exportPaperFilename } from '@/utils/paperExportCommon'
import {
  DEFAULT_ANSWER_SHEET_OPTIONS,
  buildAnswerSheetHtml
} from '@/utils/paperAnswerSheetExport'

const FONT = '\u5b8b\u4f53'
const FONT_ATTR = { name: FONT, eastAsia: FONT, ascii: FONT, hAnsi: FONT }
const FONT_SIZE = 21
const SMALL_SIZE = 18

const CELL_BORDER = {
  top: { style: BorderStyle.SINGLE, size: 1, color: '666666' },
  bottom: { style: BorderStyle.SINGLE, size: 1, color: '666666' },
  left: { style: BorderStyle.SINGLE, size: 1, color: '666666' },
  right: { style: BorderStyle.SINGLE, size: 1, color: '666666' }
}

function mergeOptions(opts) {
  return { ...DEFAULT_ANSWER_SHEET_OPTIONS, ...(opts || {}) }
}

function textRun(text, opts = {}) {
  return new TextRun({
    text: String(text || ''),
    font: FONT_ATTR,
    size: opts.size || FONT_SIZE,
    bold: !!opts.bold,
    color: opts.color
  })
}

function bodyParagraph(opts) {
  return new Paragraph({
    alignment: opts.alignment,
    spacing: opts.spacing,
    indent: opts.indent,
    children: opts.children || [textRun(' ')]
  })
}

function pageProps(pageLayout) {
  const isA3 = String(pageLayout || 'A4').toUpperCase().includes('A3')
  return {
    page: {
      size: {
        orientation: PageOrientation.PORTRAIT,
        width: isA3 ? 16838 : 11906,
        height: isA3 ? 23811 : 16838
      },
      margin: { top: 900, bottom: 900, left: 900, right: 900 }
    }
  }
}

function answerSheetDocxFilename(vm, opts) {
  const base = exportPaperFilename(vm, 'docx').replace(/\.docx$/i, '')
  const suffix = mergeOptions(opts).sheetMode === 'teacher'
    ? '\u7b54\u9898\u5361-\u6559\u5e08\u7248'
    : '\u7b54\u9898\u5361'
  return `${base}-${suffix}.docx`
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

function nodeText(node) {
  return (node?.textContent || '').replace(/\s+/g, ' ').trim()
}

function tableCell(text, opts = {}) {
  return new TableCell({
    borders: CELL_BORDER,
    width: opts.width ? { size: opts.width, type: WidthType.DXA } : undefined,
    verticalAlign: VerticalAlign.CENTER,
    shading: opts.shading,
    children: [new Paragraph({
      alignment: opts.center ? AlignmentType.CENTER : AlignmentType.LEFT,
      children: [textRun(text, { bold: opts.bold, size: opts.size })]
    })]
  })
}

function htmlTableToDocx(tableEl) {
  if (!tableEl) return null
  const rows = []
  tableEl.querySelectorAll('tr').forEach(tr => {
    const cells = []
    tr.querySelectorAll('th,td').forEach(td => {
      const text = nodeText(td)
      const isHeader = td.tagName.toLowerCase() === 'th'
      cells.push(tableCell(text || ' ', {
        bold: isHeader || td.classList.contains('as-grid-opt') || td.classList.contains('as-exam-pos'),
        center: true,
        size: SMALL_SIZE,
        shading: isHeader || td.classList.contains('as-grid-num') || td.classList.contains('as-exam-pos')
          ? { fill: 'F0F0F0' }
          : undefined,
        width: td.classList.contains('as-grid-empty') ? 600 : undefined
      }))
    })
    if (cells.length) rows.push(new TableRow({ children: cells }))
  })
  if (!rows.length) return null
  return new Table({ rows, width: { size: 100, type: WidthType.PERCENTAGE } })
}

function fillLineParagraph(no, scoreText) {
  const children = [textRun(`${no} `, { bold: true })]
  children.push(textRun('_'.repeat(42), { size: FONT_SIZE }))
  if (scoreText) children.push(textRun(`  ${scoreText}`, { size: SMALL_SIZE }))
  return bodyParagraph({ spacing: { before: 60, after: 60 }, children })
}

function subjectiveBlock(no, scoreText, lineCount, isBlank) {
  const elements = []
  const headRuns = [textRun(`${no}`, { bold: true })]
  if (scoreText) headRuns.push(textRun(`    ${scoreText}`, { size: SMALL_SIZE }))
  elements.push(bodyParagraph({ spacing: { before: 80, after: 40 }, children: headRuns }))
  if (isBlank) {
    elements.push(bodyParagraph({
      spacing: { after: 120 },
      children: [textRun('_'.repeat(50))]
    }))
    return elements
  }
  for (let i = 0; i < lineCount; i += 1) {
    elements.push(bodyParagraph({
      spacing: { after: 50 },
      children: [textRun('_'.repeat(50))]
    }))
  }
  return elements
}

function convertHtmlToDocxChildren(html) {
  const doc = new DOMParser().parseFromString(`<div id="as-root">${html || ''}</div>`, 'text/html')
  const root = doc.getElementById('as-root')
  const children = []
  if (!root) return children

  root.childNodes.forEach(node => {
    if (node.nodeType !== Node.ELEMENT_NODE) return
    const el = node
    const tag = el.tagName.toLowerCase()
    const cls = el.className || ''

    if (tag === 'style') return

    if (tag === 'h1' || cls.includes('as-main-title')) {
      children.push(bodyParagraph({
        alignment: AlignmentType.CENTER,
        spacing: { after: 100 },
        children: [textRun(nodeText(el), { bold: true, size: 28 })]
      }))
      return
    }

    if (tag === 'p' || cls.includes('as-sub-title')) {
      children.push(bodyParagraph({
        alignment: AlignmentType.CENTER,
        spacing: { after: 80 },
        children: [textRun(nodeText(el), { bold: true, size: 24 })]
      }))
      return
    }

    if (cls.includes('as-summary')) {
      children.push(bodyParagraph({
        alignment: AlignmentType.CENTER,
        spacing: { after: 120 },
        children: [textRun(nodeText(el))]
      }))
      return
    }

    if (cls.includes('as-meta')) {
      el.querySelectorAll('.as-meta-item').forEach(item => {
        const label = nodeText(item).replace(/\s+/g, '')
        children.push(bodyParagraph({
          spacing: { after: 40 },
          children: [textRun(label), textRun('_'.repeat(18), { size: FONT_SIZE })]
        }))
      })
      children.push(bodyParagraph({ spacing: { after: 80 }, children: [textRun(' ')] }))
      return
    }

    if (cls.includes('as-exam-block')) {
      const label = el.querySelector('.as-exam-label')
      if (label) {
        children.push(bodyParagraph({
          spacing: { before: 80, after: 60 },
          children: [textRun(nodeText(label), { bold: true, size: SMALL_SIZE })]
        }))
      }
      const table = htmlTableToDocx(el.querySelector('table'))
      if (table) {
        children.push(table)
        children.push(bodyParagraph({ spacing: { after: 120 }, children: [textRun(' ')] }))
      }
      return
    }

    if (cls.includes('as-volume-title') || cls.includes('as-part-title')) {
      children.push(bodyParagraph({
        alignment: cls.includes('as-volume-title') ? AlignmentType.CENTER : AlignmentType.LEFT,
        spacing: { before: 120, after: 80 },
        children: [textRun(nodeText(el), { bold: true })]
      }))
      return
    }

    if (cls.includes('as-section')) {
      const title = el.querySelector('.as-section-title')
      if (title) {
        children.push(bodyParagraph({
          spacing: { before: 80, after: 60 },
          children: [textRun(nodeText(title), { bold: true, size: SMALL_SIZE })]
        }))
      }
      el.querySelectorAll('table.as-grid-table').forEach(tableEl => {
        const table = htmlTableToDocx(tableEl)
        if (table) {
          children.push(table)
          children.push(bodyParagraph({ spacing: { after: 80 }, children: [textRun(' ')] }))
        }
      })
      el.querySelectorAll('.as-fill-row').forEach(row => {
        const no = nodeText(row.querySelector('.as-fill-no'))
        const score = nodeText(row.querySelector('.as-fill-score'))
        children.push(fillLineParagraph(no, score))
      })
      el.querySelectorAll('.as-sub-row').forEach(row => {
        const no = nodeText(row.querySelector('.as-sub-no'))
        const score = nodeText(row.querySelector('.as-sub-score'))
        const blank = !!row.querySelector('.as-sub-blank')
        const lines = row.querySelectorAll('.as-blank-line').length || 4
        children.push(...subjectiveBlock(no, score, lines, blank))
      })
      return
    }

    if (cls.includes('as-answer-key')) {
      const title = el.querySelector('.as-section-title')
      if (title) {
        children.push(bodyParagraph({
          spacing: { before: 200, after: 80 },
          children: [textRun(nodeText(title), { bold: true })]
        }))
      }
      const table = htmlTableToDocx(el.querySelector('table'))
      if (table) children.push(table)
      return
    }

    if (cls.includes('as-footer')) {
      children.push(bodyParagraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 160, after: 80 },
        children: [textRun(nodeText(el), { size: SMALL_SIZE })]
      }))
      return
    }

    if (tag === 'table') {
      const table = htmlTableToDocx(el)
      if (table) children.push(table)
    }
  })

  return children
}

export async function exportAnswerSheetDocxClient(vm, options) {
  const opts = await prepareAnswerSheetData(vm, options)
  const html = buildAnswerSheetHtml(vm, opts)
  const children = convertHtmlToDocxChildren(html)
  if (!children.length) {
    throw new Error('answer sheet content empty')
  }
  const pageLayout = vm.pageLayout || vm.form?.templateCode || 'A4'
  const doc = new Document({
    styles: {
      default: {
        document: {
          run: { size: FONT_SIZE, font: FONT_ATTR }
        }
      }
    },
    sections: [{ properties: pageProps(pageLayout), children }]
  })
  const blob = await Packer.toBlob(doc)
  saveAs(blob, answerSheetDocxFilename(vm, opts))
}
