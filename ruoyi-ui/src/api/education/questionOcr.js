import request from '@/utils/request'

export function recognizeOcr(data) {
  const isForm = typeof FormData !== 'undefined' && data instanceof FormData
  return request({
    url: '/system/education/question/ocr/recognize',
    method: 'post',
    data,
    timeout: 120000,
    headers: isForm ? { repeatSubmit: false, 'Content-Type': 'multipart/form-data' } : undefined
  })
}

export function listOcrDrafts(query) {
  return request({
    url: '/system/education/question/ocr/drafts',
    method: 'get',
    params: query
  })
}

export function getOcrDraft(draftId) {
  return request({
    url: '/system/education/question/ocr/draft/' + draftId,
    method: 'get'
  })
}

export function commitOcr(data) {
  return request({
    url: '/system/education/question/ocr/commit',
    method: 'post',
    data
  })
}

export function uploadFigureImage(formData) {
  return request({
    url: '/system/common/upload',
    method: 'post',
    data: formData,
    timeout: 60000,
    headers: { repeatSubmit: false, 'Content-Type': 'multipart/form-data' }
  })
}

export function saveDraftFigure(draftId, figurePath) {
  return request({
    url: '/system/education/question/ocr/draft/' + draftId + '/figure',
    method: 'post',
    data: { figurePath }
  })
}
