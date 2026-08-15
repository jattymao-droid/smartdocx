import katex from 'katex'
import { ImportedXmlComponent } from 'docx'
import { isExportableDocxLatex } from '@/utils/questionFormula'

/** С�ĺ� = 12pt; docx uses half-points */
export const DOCX_BODY_FONT_SIZE = 24

let convertMathMl2Math = null
let mathArgPr = null

function getMathArgPr() {
  if (!mathArgPr) {
    mathArgPr = ImportedXmlComponent.fromXmlString(
      `<m:argPr xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math">` +
      `<m:argSz m:val="${DOCX_BODY_FONT_SIZE}"/></m:argPr>`
    )
  }
  return mathArgPr
}

/** Align inline Office Math base size with body text (С��). */
export function applyDocxMathFontSize(math) {
  if (!math || !Array.isArray(math.root)) return math
  const hasArgPr = math.root.some(child => child && child.rootKey === 'm:argPr')
  if (!hasArgPr) math.root.unshift(getMathArgPr())
  return math
}

async function ensureConverter() {
  if (!convertMathMl2Math) {
    const mod = await import(
      /* webpackChunkName: "docx-math-converter" */ '@micromatrix.org/docx-math-converter'
    )
    convertMathMl2Math = mod.convertMathMl2Math
  }
}

/** Convert LaTeX to native Word Office Math (editable in Word). */
export async function latexToDocxMath(latex) {
  const body = String(latex || '').trim()
  if (!body || !isExportableDocxLatex(body)) return null
  await ensureConverter()
  try {
    const mathml = katex.renderToString(body, {
      output: 'mathml',
      throwOnError: false,
      strict: 'ignore',
      trust: true,
      displayMode: false
    })
    return applyDocxMathFontSize(convertMathMl2Math(mathml))
  } catch (e) {
    return null
  }
}
