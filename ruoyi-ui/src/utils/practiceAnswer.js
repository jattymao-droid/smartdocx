import { parseMultiAnswerLetters, formatChoiceAnswer } from '@/utils/questionAnswer'
import { isChoiceQuestionType, isJudgeQuestionType, isFillQuestionType } from '@/utils/questionTypes'

function coerceAnswerValue(raw) {
  if (raw == null || raw === '') return null
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    } catch (e) {
      return raw
    }
  }
  return raw
}

function normalizeText(s) {
  return String(s || '')
    .trim()
    .replace(/\s+/g, ' ')
    .toLowerCase()
}

export function isSingleChoiceCorrect(picked, correctAnswer) {
  const letters = parseMultiAnswerLetters(correctAnswer)
  return letters.includes(String(picked || '').toUpperCase())
}

export function isMultiChoiceCorrect(pickedList, correctAnswer) {
  const picked = parseMultiAnswerLetters(pickedList)
  const correct = parseMultiAnswerLetters(correctAnswer)
  if (!picked.length || picked.length !== correct.length) return false
  return picked.every(l => correct.includes(l))
}

export function isJudgeAnswerCorrect(picked, correctAnswer) {
  const raw = coerceAnswerValue(correctAnswer)
  const correctTrue = raw === true || String(raw).toLowerCase() === 'true' || raw === '1' || raw === '\u6b63\u786e'
  const pick = String(picked || '').toLowerCase()
  const pickedTrue = pick === 'true' || pick === '1' || pick === '\u6b63\u786e'
  const pickedFalse = pick === 'false' || pick === '0' || pick === '\u9519\u8bef'
  if (pickedTrue) return correctTrue
  if (pickedFalse) return !correctTrue
  return false
}

export function isFillAnswerCorrect(picked, correctAnswer) {
  const user = normalizeText(picked)
  if (!user) return false
  const val = coerceAnswerValue(correctAnswer)
  if (Array.isArray(val)) {
    return val.some(a => normalizeText(a) === user)
  }
  if (val != null && normalizeText(val) === user) return true
  const display = formatChoiceAnswer('fill', correctAnswer)
  return normalizeText(display) === user
}

export function formatPickedForSubmit(questionType, picked, pickedMulti) {
  if (questionType === 'multi') {
    return parseMultiAnswerLetters(pickedMulti || picked).join(',')
  }
  return picked || ''
}

export function resolveQuestionAnswerMode(questionType) {
  if (isJudgeQuestionType(questionType)) return 'judge'
  if (questionType === 'multi') return 'multi'
  if (isFillQuestionType(questionType)) return 'fill'
  if (isChoiceQuestionType(questionType)) return 'single'
  return 'subjective'
}

export function evaluateAnswer(questionType, picked, pickedMulti, correctAnswer) {
  const mode = resolveQuestionAnswerMode(questionType)
  if (mode === 'single') return isSingleChoiceCorrect(picked, correctAnswer)
  if (mode === 'multi') return isMultiChoiceCorrect(pickedMulti || picked, correctAnswer)
  if (mode === 'judge') return isJudgeAnswerCorrect(picked, correctAnswer)
  if (mode === 'fill') return isFillAnswerCorrect(picked, correctAnswer)
  return null
}
