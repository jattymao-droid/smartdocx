/**
 * Client-side question content parser for exam paper smart marking.
 */

const OPTION_PREFIX = /^[A-Ha-h][.\uFF0E\u3001\u3002)\uFF09:\uFF1A\s]/
const OPTION_PREFIX_SPACE = /^[A-Ha-h]\s+\S/
const ANSWER_LINE = /^[\s\u3000]*(?:\u3010\u7b54\u6848\u3011|\u3010\u7b54\u3011|\u7b54\u6848|\u53c2\u8003\u7b54\u6848)[:\s\uFF1A]+(.+)$/
const ANALYSIS_LINE = /^[\s\u3000]*(?:\u3010\u89e3\u6790\u3011|\u3010\u8be6\u89e3\u3011|\u89e3\u6790|\u8bd5\u9898\u89e3\u6790)[:\s\uFF1A]?(.*)$/

export function isOptionLine(text) {
  const line = String(text || '').trim()
  return OPTION_PREFIX.test(line) || OPTION_PREFIX_SPACE.test(line)
}

export function splitInlineOptions(line) {
  const text = String(line || '').replace(/[\uFEFF\u200B]/g, '').replace(/\s+/g, ' ').trim()
  if (!text) return []
  const parts = text
    .split(/(?=(?:^|\s)[A-Ha-h][.\uFF0E\u3001\u3002)\uFF09:\uFF1A\s])/)
    .map(s => s.trim())
    .filter(Boolean)
  const options = parts.filter(p => isOptionLine(p))
  if (options.length >= 2) return options
  if (isOptionLine(text)) return [text]
  return [text]
}

function detectTypeFromSection(sectionName) {
  if (!sectionName) return null
  const section = String(sectionName).replace(/\n/g, ' ').trim()
  if (section.includes('\u591a\u9009') || section.includes('\u591a\u9879\u9009\u62e9')) return 'multi'
  if (section.includes('\u5355\u9009') || section.includes('\u9009\u62e9\u9898')) return 'single'
  if (section.includes('\u5224\u65ad')) return 'judge'
  if (section.includes('\u77e5\u8bc6\u70b9\u586b\u7a7a') || section.includes('\u77e5\u8bc6\u586b\u7a7a')) return 'knowledge_fill'
  if (section.includes('\u586b\u7a7a')) return 'fill'
  if (section.includes('\u5b9e\u9a8c')) return 'experiment'
  if (section.includes('\u4f5c\u56fe')) return 'drawing'
  if (section.includes('\u9605\u8bfb')) return 'reading'
  if (section.includes('\u7efc\u5408')) return 'comprehensive'
  if (section.includes('\u89e3\u7b54') || section.includes('\u8ba1\u7b97') || section.includes('\u8bc1\u660e')) return 'answer'
  if (section.includes('\u7b80\u7b54')) return 'short'
  return null
}

function detectTypeFromContent(text, optionCount) {
  const full = text || ''
  if (full.includes('\u591a\u9009') || full.includes('\u591a\u9879\u9009\u62e9')) return 'multi'
  if (optionCount >= 2) return 'single'
  if (/\u5224\u65ad|\u5bf9\u9519|\u221a|\u00d7/.test(full)) return 'judge'
  if (full.includes('\u77e5\u8bc6\u70b9\u586b\u7a7a') || full.includes('\u77e5\u8bc6\u586b\u7a7a')) return 'knowledge_fill'
  if (/\u5b9e\u9a8c|\u6d4b\u5b9a|\u88c5\u7f6e\u56fe/.test(full)) return 'experiment'
  if (/\u4f5c\u56fe|\u753b\u51fa/.test(full)) return 'drawing'
  if (full.includes('\u9605\u8bfb\u7406\u89e3') || full.includes('\u9605\u8bfb\u9898') || (full.includes('\u9605\u8bfb') && full.length > 120)) return 'reading'
  if (full.includes('\u7efc\u5408')) return 'comprehensive'
  if (/\u89e3\u7b54\u9898|\u8ba1\u7b97\u9898|\u8bc1\u660e\u9898/.test(full)) return 'answer'
  if (/_{2,}|\uff3f{2,}|\uff08\s*\uff09|\u3010\s*\u3011/.test(full)) return 'fill'
  return 'short'
}

export function isMultiChoiceAnswer(raw) {
  if (!raw) return false
  try {
    const value = String(JSON.parse(raw)).replace(/[^A-Ha-h]/gi, '').toUpperCase()
    return value.length > 1
  } catch (e) {
    const value = String(raw).replace(/[^A-Ha-h]/gi, '').toUpperCase()
    return value.length > 1
  }
}

export function countOptionLines(text) {
  if (!text) return 0
  let count = 0
  for (const line of String(text).split(/\r?\n/)) {
    splitInlineOptions(line).forEach(part => {
      if (isOptionLine(part)) count++
    })
  }
  return count
}

export function detectQuestionType(text, optionsJson, sectionName, correctAnswer) {
  const fromSection = detectTypeFromSection(sectionName)
  if (fromSection) return fromSection
  let optionCount = 0
  if (optionsJson) {
    try {
      optionCount = JSON.parse(optionsJson).length
    } catch (e) {
      optionCount = 0
    }
  }
  if (optionCount < 2) optionCount = countOptionLines(text)
  let type = detectTypeFromContent(text, optionCount)
  if (type === 'single' && isMultiChoiceAnswer(correctAnswer)) return 'multi'
  return type
}

export function formatAnswerJson(rawAnswer, questionType) {
  if (!rawAnswer) return null
  const value = String(rawAnswer).trim()
  if (questionType === 'judge') {
    if (/\u6b63\u786e|\u5bf9|\u221a/i.test(value) || value === 'T') return JSON.stringify('true')
    if (/\u9519\u8bef|\u9519|\u00d7/i.test(value) || value === 'F') return JSON.stringify('false')
  }
  if (/^[A-Ha-h](?:[.\uFF0E\u3001)\uFF09].*)?$/.test(value)) {
    return JSON.stringify(value.charAt(0).toUpperCase())
  }
  if (/^[A-Ha-h](?:\s*[,\uFF0C\u3001\s]\s*[A-Ha-h])+/.test(value)) {
    return JSON.stringify(value.replace(/[^A-Ha-h]/gi, '').toUpperCase())
  }
  return JSON.stringify(value)
}

export function parseQuestionContent(raw) {
  const result = { stem: '', optionsJson: null, correctAnswer: null, analysis: null }
  if (!raw) return result
  const stemLines = []
  const optionLines = []
  const analysisBuf = []
  let inAnalysis = false

  for (const line of String(raw).split(/\r?\n/)) {
    const pieces = splitInlineOptions(line)
    for (const text of pieces) {
      if (!text) continue
      const answerMatch = text.match(ANSWER_LINE)
      if (answerMatch) {
        inAnalysis = false
        result.correctAnswer = formatAnswerJson(answerMatch[1].trim(), null)
        continue
      }
      const analysisMatch = text.match(ANALYSIS_LINE)
      if (analysisMatch) {
        inAnalysis = true
        if (analysisMatch[1]) analysisBuf.push(analysisMatch[1].trim())
        continue
      }
      if (inAnalysis) {
        analysisBuf.push(text)
        continue
      }
      if (isOptionLine(text)) optionLines.push(text)
      else stemLines.push(text)
    }
  }

  result.stem = stemLines.join('\n').trim() || String(raw).trim()
  if (optionLines.length) result.optionsJson = JSON.stringify(optionLines)
  if (analysisBuf.length) result.analysis = analysisBuf.join('\n').trim()
  return result
}

export function applyParsedToMarkItem(item, raw) {
  const parsed = parseQuestionContent(raw)
  if (parsed.stem) item.content = parsed.stem
  if (parsed.optionsJson) item.options = parsed.optionsJson
  if (parsed.correctAnswer) item.correctAnswer = parsed.correctAnswer
  if (parsed.analysis) item.analysis = parsed.analysis
  item.questionType = detectQuestionType(raw, parsed.optionsJson, item.sectionName, parsed.correctAnswer)
  return item
}
