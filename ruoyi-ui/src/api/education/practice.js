import request from '@/utils/request'

export function checkPracticeAnswer(data) {
  return request({
    url: '/system/education/practice/check',
    method: 'post',
    data,
    timeout: 30000
  })
}

export function submitPractice(data) {
  return request({
    url: '/system/education/practice/submit',
    method: 'post',
    data
  })
}

export function listPracticeSessions(params) {
  return request({
    url: '/system/education/practice/session/list',
    method: 'get',
    params
  })
}

export function getPracticeStats(params) {
  return request({
    url: '/system/education/practice/stats',
    method: 'get',
    params
  }).catch(() => aggregatePracticeStats(params))
}

function aggregatePracticeStats(params) {
  return listPracticeSessions({
    subjectId: params && params.subjectId,
    pageNum: 1,
    pageSize: 1000
  }).then(res => {
    const rows = res.rows || []
    const sessionCount = res.total != null ? res.total : rows.length
    let totalQuestions = 0
    let totalCorrect = 0
    let totalChoice = 0
    rows.forEach(s => {
      totalQuestions += Number(s.totalCount) || 0
      totalCorrect += Number(s.correctCount) || 0
      totalChoice += Number(s.choiceCount) || 0
    })
    const avgChoiceRate = totalChoice > 0
      ? Math.round((totalCorrect / totalChoice) * 1000) / 10
      : 0
    return {
      data: {
        sessionCount,
        totalQuestions,
        totalCorrect,
        totalChoice,
        avgChoiceRate
      }
    }
  })
}

export function getPracticeSession(sessionId) {
  return request({
    url: '/system/education/practice/session/' + sessionId,
    method: 'get'
  })
}

export function listWrongBook(params) {
  return request({
    url: '/system/education/practice/wrong-book/list',
    method: 'get',
    params
  })
}

export function getWrongBookStats(params) {
  return request({
    url: '/system/education/practice/wrong-book/stats',
    method: 'get',
    params
  }).catch(() => aggregateWrongBookStats(params))
}

function aggregateWrongBookStats(params) {
  const activeReq = listWrongBook({
    subjectId: params && params.subjectId,
    mastered: '0',
    pageNum: 1,
    pageSize: 1
  })
  const masteredReq = listWrongBook({
    subjectId: params && params.subjectId,
    mastered: '1',
    pageNum: 1,
    pageSize: 1
  })
  const attemptsReq = listWrongBook({
    subjectId: params && params.subjectId,
    mastered: '0',
    pageNum: 1,
    pageSize: 500
  })
  return Promise.all([activeReq, masteredReq, attemptsReq]).then(([activeRes, masteredRes, attemptsRes]) => {
    const activeRows = attemptsRes.rows || []
    const totalWrongAttempts = activeRows.reduce((sum, row) => sum + (Number(row.wrongCount) || 0), 0)
    return {
      data: {
        activeCount: activeRes.total || 0,
        masteredCount: masteredRes.total || 0,
        totalWrongAttempts
      }
    }
  })
}

export function markWrongMastered(wrongId) {
  return request({
    url: '/system/education/practice/wrong-book/' + wrongId + '/master',
    method: 'put'
  })
}

export function restoreWrongBook(wrongId) {
  return request({
    url: '/system/education/practice/wrong-book/' + wrongId + '/restore',
    method: 'put'
  })
}

export function batchMarkWrongMastered(wrongIds) {
  return request({
    url: '/system/education/practice/wrong-book/batch/master',
    method: 'put',
    data: { wrongIds }
  })
}

export function deleteWrongBook(wrongId) {
  return request({
    url: '/system/education/practice/wrong-book/' + wrongId,
    method: 'delete'
  })
}

export function batchDeleteWrongBook(wrongIds) {
  return request({
    url: '/system/education/practice/wrong-book/batch',
    method: 'delete',
    data: { wrongIds }
  })
}

export function wrongComposePaper(data) {
  return request({
    url: '/system/education/practice/wrong-compose',
    method: 'post',
    data
  })
}

export function listWeakPoints(params) {
  return request({
    url: '/system/education/practice/weak-points',
    method: 'get',
    params
  })
}

export function weakComposePaper(data) {
  return request({
    url: '/system/education/practice/weak-compose',
    method: 'post',
    data
  })
}
