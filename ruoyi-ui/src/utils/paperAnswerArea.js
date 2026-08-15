const SUBJECTIVE_TYPES = ['fill', 'knowledge_fill', 'experiment', 'answer', 'comprehensive', 'reading', 'drawing', 'short']

export function suggestAnswerAreaLines(questionType) {
  if (questionType === 'fill' || questionType === 'knowledge_fill') return 2
  if (questionType === 'drawing') return 6
  if (questionType === 'reading') return 8
  if (SUBJECTIVE_TYPES.includes(questionType)) return 5
  return 3
}

export function isSubjectiveType(questionType) {
  return SUBJECTIVE_TYPES.includes(questionType)
}
