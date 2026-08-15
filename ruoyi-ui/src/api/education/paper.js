import request from '@/utils/request'

export function previewPaper(data) {
  return request({
    url: '/system/education/paper/preview',
    method: 'post',
    data: data
  })
}

export function exportPaperPdf(data) {
  return request({
    url: '/system/education/paper/export/pdf',
    method: 'post',
    data: data
  })
}

export function exportPaperHtml(data) {
  return request({
    url: '/system/education/paper/export/html',
    method: 'post',
    data: data
  })
}

export function exportPaperDocx(data) {
  return request({
    url: '/system/education/paper/export/docx',
    method: 'post',
    data: data
  })
}

export function listMyPapers(query) {
  return request({
    url: '/system/education/paper/mine/list',
    method: 'get',
    params: query
  })
}

export function getMyPaper(paperId) {
  return request({
    url: '/system/education/paper/mine/' + paperId,
    method: 'get'
  })
}

export function saveMyPaper(data) {
  return request({
    url: '/system/education/paper/mine/save',
    method: 'post',
    data: data
  })
}

export function deleteMyPaper(paperId) {
  return request({
    url: '/system/education/paper/mine/' + paperId,
    method: 'delete'
  })
}

export function createPaperShare(snapshot) {
  return request({
    url: '/system/education/paper/share',
    method: 'post',
    headers: { repeatSubmit: false },
    data: { snapshot }
  })
}

export function getPaperShare(shareId) {
  return request({
    url: '/system/education/paper/share/' + encodeURIComponent(shareId),
    method: 'get',
    headers: { isToken: false }
  })
}

export function publishSchoolPaper(data) {
  return request({
    url: '/system/education/paper/publish-school',
    method: 'post',
    data: data
  })
}
