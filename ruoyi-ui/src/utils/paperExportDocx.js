import {
  AlignmentType,
  Document,
  ImageRun,
  Packer,
  PageOrientation,
  Paragraph,
  TextRun
} from 'docx'
import { saveAs } from 'file-saver'
import { splitTextIntoDocxParts } from '@/utils/questionFormula'
import { isQuestionHtml } from '@/utils/questionContent'
import { DOCX_BODY_FONT_SIZE, latexToDocxMath } from '@/utils/paperExportDocxMath'
import { buildDocxParagraphsFromHtml, buildDocxRunsFromHtml } from '@/utils/paperExportDocxHtml'
import { computeDocxImageTransform, readImageNaturalSize } from '@/utils/paperExportDocxImage'
import {
  displayContent,
  exportPaperDisplayTitle,
  exportPaperFilename,
  imageUrls,
  isAnswerInline,
  iterPaperQuestions,
  optionItems,
  resolveExportVolumes,
  resolveImageUrl,
  shouldExportAnswers,
  teacherAnalysis,
  teacherAnswer
} from '@/utils/paperExportCommon'

const FONT = '\u5b8b\u4f53'
const FONT_SIZE = DOCX_BODY_FONT_SIZE
const FONT_ATTR = { name: FONT, eastAsia: FONT, ascii: FONT, hAnsi: FONT }

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
    children: opts.children
  })
}

async function buildDocxRunsFromFormulaText(text, opts = {}) {
  const raw = String(text || '')
  if (isQuestionHtml(raw) || /<\s*(img|p|span|div|table|br)\b/i.test(raw)) {
    return buildDocxRunsFromHtml(raw, opts)
  }
  const parts = splitTextIntoDocxParts(text)
  const runs = []
  for (const part of parts) {
    if (part.type === 'text') {
      if (part.content) runs.push(textRun(part.content, opts))
      continue
    }
    const math = await latexToDocxMath(part.content)
    if (math) {
      runs.push(math)
    } else if (part.content) {
      runs.push(textRun(part.content, opts))
    }
  }
  return runs
}

async function fetchImageBytes(url) {
  const res = await fetch(url, { credentials: 'include' })
  if (!res.ok) throw new Error('image fetch failed')
  return new Uint8Array(await res.arrayBuffer())
}

function pageProps(pageLayout) {
  const isA3 = String(pageLayout || 'A4').toUpperCase() === 'A3'
  return {
    page: {
      size: {
        orientation: PageOrientation.PORTRAIT,
        width: isA3 ? 16838 : 11906,
        height: isA3 ? 23811 : 16838
      },
      margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 }
    }
  }
}

async function appendInlineAnswerDocx(children, vm, q) {
  const ans = teacherAnswer(vm, q)
  const analysis = teacherAnalysis(vm, q)
  if (ans) {
    const ansRuns = await buildDocxRunsFromFormulaText(ans, { color: 'CC0000' })
    children.push(bodyParagraph({
      indent: { left: 480 },
      spacing: { after: 40 },
      children: [textRun('\u3010\u7b54\u6848\u3011', { color: 'CC0000' }), ...ansRuns]
    }))
  }
  if (analysis) {
    const anaRuns = await buildDocxRunsFromFormulaText(analysis, { color: 'CC0000' })
    children.push(bodyParagraph({
      indent: { left: 480 },
      spacing: { after: 80 },
      children: [textRun('\u3010\u89e3\u6790\u3011', { color: 'CC0000' }), ...anaRuns]
    }))
  }
}

async function appendAnswerSheetDocx(children, vm) {
  const items = iterPaperQuestions(vm).filter(q => teacherAnswer(vm, q) || teacherAnalysis(vm, q))
  if (!items.length) return
  children.push(bodyParagraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 320, after: 160 },
    children: [textRun('\u53c2\u8003\u7b54\u6848', { bold: true, size: FONT_SIZE })]
  }))
  for (const q of items) {
    const ans = teacherAnswer(vm, q)
    const analysis = teacherAnalysis(vm, q)
    const runs = [textRun(`${q.globalNo}. `, { bold: true })]
    if (ans) {
      runs.push(textRun('\u3010\u7b54\u6848\u3011', { color: 'CC0000' }))
      runs.push(...await buildDocxRunsFromFormulaText(ans, { color: 'CC0000' }))
    }
    if (analysis) {
      if (ans) runs.push(textRun('  '))
      runs.push(textRun('\u3010\u89e3\u6790\u3011', { color: 'CC0000' }))
      runs.push(...await buildDocxRunsFromFormulaText(analysis, { color: 'CC0000' }))
    }
    children.push(bodyParagraph({
      indent: { left: 480 },
      spacing: { after: 80 },
      children: runs
    }))
  }
}

function appendStemElements(children, stemElements, questionNo, defaultSpacing) {
  stemElements.forEach((el, index) => {
    if (el.type === 'table') {
      children.push(el.table)
      return
    }
    const runs = index === 0
      ? [textRun(`${questionNo}. `), ...el.children]
      : el.children
    children.push(bodyParagraph({
      spacing: el.spacing || defaultSpacing,
      alignment: el.alignment,
      indent: el.indent,
      children: runs.length ? runs : [textRun(`${questionNo}. `)]
    }))
  })
}

async function buildDocxChildren(vm) {
  const showTeacher = shouldExportAnswers(vm)
  const answerInline = isAnswerInline(vm)
  const header = (vm.form && vm.form.header) || {}
  const children = []
  const volumes = resolveExportVolumes(vm)
  const detailMap = vm.detailMap || {}

  children.push(bodyParagraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 200 },
    children: [textRun(exportPaperDisplayTitle(vm), { size: FONT_SIZE, bold: true })]
  }))

  const metaParts = []
  if (header.subjectName) metaParts.push('\u79d1\u76ee\uff1a' + header.subjectName)
  if (header.duration) metaParts.push('\u65f6\u95f4\uff1a' + header.duration)
  metaParts.push('\u6ee1\u5206\uff1a' + vm.basketTotalScore)
  children.push(bodyParagraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 240 },
    children: [textRun(metaParts.join('    '))]
  }))

  for (const vol of volumes) {
    if (vol.showTitle && vol.title) {
      children.push(bodyParagraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 160, after: 120 },
        children: [textRun(vol.title, { bold: true })]
      }))
    }
    for (const section of vol.sections) {
      if (section.title) {
        children.push(bodyParagraph({
          spacing: { before: 120, after: 80 },
          children: [textRun(section.title, { bold: true })]
        }))
      }
      for (const q of section.items) {
        const stemContent = displayContent(q, detailMap)
        const stemSpacing = { before: 100, after: 80 }
        if (isQuestionHtml(stemContent)) {
          const stemElements = await buildDocxParagraphsFromHtml(stemContent, {
            paragraph: { spacing: stemSpacing }
          })
          if (stemElements.length) {
            appendStemElements(children, stemElements, q.globalNo, stemSpacing)
          } else {
            const fallbackRuns = await buildDocxRunsFromFormulaText(q.contentBrief || stemContent)
            children.push(bodyParagraph({
              spacing: stemSpacing,
              children: [textRun(`${q.globalNo}. `), ...fallbackRuns]
            }))
          }
        } else {
          const stemRuns = await buildDocxRunsFromFormulaText(stemContent)
          const questionRuns = [textRun(`${q.globalNo}. `), ...stemRuns]
          if (questionRuns.length <= 1 && q.contentBrief) {
            questionRuns.push(...await buildDocxRunsFromFormulaText(q.contentBrief))
          }
          children.push(bodyParagraph({
            spacing: stemSpacing,
            children: questionRuns.length ? questionRuns : [textRun(`${q.globalNo}. `)]
          }))

          for (const url of imageUrls(q, detailMap)) {
            const src = resolveImageUrl(url)
            if (!src) continue
            try {
              const data = await fetchImageBytes(src)
              const naturalSize = await readImageNaturalSize(data)
              const transform = computeDocxImageTransform(null, src, naturalSize, { formula: false })
              children.push(bodyParagraph({
                indent: { left: 480 },
                spacing: { after: 80 },
                children: [new ImageRun({ data, transformation: transform })]
              }))
            } catch (e) { /* skip */ }
          }
        }

        for (const opt of optionItems(q, detailMap)) {
          const optRuns = await buildDocxRunsFromFormulaText(opt.text)
          children.push(bodyParagraph({
            indent: { left: 480 },
            spacing: { after: 60 },
            children: [textRun(`${opt.label}. `), ...optRuns]
          }))
        }

        const area = vm.answerAreas[q.questionId]
        if (area && area.lines > 0) {
          const lineCount = area.style === 'blank' ? 1 : area.lines
          for (let i = 0; i < lineCount; i += 1) {
            children.push(bodyParagraph({
              indent: { left: 480 },
              spacing: { after: 60 },
              children: [textRun('_'.repeat(50))]
            }))
          }
        }

        if (showTeacher && answerInline) {
          await appendInlineAnswerDocx(children, vm, q)
        }
      }
    }
  }

  if (showTeacher && !answerInline) {
    await appendAnswerSheetDocx(children, vm)
  }
  return children
}

export async function exportPaperDocxClient(vm) {
  const pageLayout = vm.pageLayout || 'A4'
  const children = await buildDocxChildren(vm)
  if (!children.length) {
    throw new Error('export content empty')
  }
  const doc = new Document({
    styles: {
      default: {
        document: {
          run: {
            size: FONT_SIZE,
            font: FONT_ATTR
          }
        }
      }
    },
    sections: [{ properties: pageProps(pageLayout), children }]
  })
  const blob = await Packer.toBlob(doc)
  const filename = exportPaperFilename(vm, 'docx')
  saveAs(blob, filename)
}
