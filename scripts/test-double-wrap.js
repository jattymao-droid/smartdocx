const http = require('http')

function req(url) {
  return new Promise((resolve, reject) => {
    const u = new URL(url)
    http.get({ hostname: u.hostname, port: u.port, path: u.pathname + u.search }, (res) => {
      const chunks = []
      res.on('data', (c) => chunks.push(c))
      res.on('end', () => {
        resolve({ status: res.statusCode, body: Buffer.concat(chunks) })
      })
    }).on('error', reject)
  })
}

function resolvePortalMediaUrl(url, base = '/dev-api') {
  if (!url) return ''
  const src = String(url).trim()
  const apiBase = (base || '').replace(/\/$/, '')

  if (src.includes('/system/portal/banner/media?')) {
    if (src.startsWith('http://') || src.startsWith('https://')) return src
    if (apiBase && src.startsWith(apiBase + '/')) return src
    if (src.startsWith('/')) return apiBase + src
  }

  if (/^https?:\/\//i.test(src)) {
    return apiBase + '/system/portal/banner/media?url=' + encodeURIComponent(src)
  }
  if (src.startsWith('/')) return apiBase + src
  return src
}

function buildPortalPdfFetchUrl(url, base = '/dev-api') {
  const src = resolvePortalMediaUrl(url, base)
  const q = src.indexOf('?')
  const path = q >= 0 ? src.slice(0, q) : src
  const query = q >= 0 ? src.slice(q) : ''
  return path.replace(/\+/g, '%2B') + query
}

async function main() {
  const fileUrl = 'http://127.0.0.1:8012/' + [
    '\u7269\u7406\u5047\u671f\u4f5c\u4e1a16-18_20260710204803A002.zip_',
    '\u7269\u7406\u5047\u671f\u4f5c\u4e1a16-18',
    '\u7269\u7406\u5047\u671f\u4f5c\u4e1a16\u53c2\u8003\u7b54\u6848.pdf'
  ].map(encodeURIComponent).join('/')

  const once = buildPortalPdfFetchUrl(fileUrl)
  const twice = buildPortalPdfFetchUrl(once)
  console.log('once', once.includes('/dev-api/dev-api') ? 'BAD double' : 'OK', once.slice(0, 90))
  console.log('twice', twice.includes('/dev-api/dev-api') ? 'BAD double' : 'OK', twice.slice(0, 90))
  console.log('twice equals once', twice === once ? 'OK idempotent' : 'DIFF')

  const devOnce = 'http://localhost:8081' + once
  const r = await req(devOnce)
  console.log('fetch via dev-server', r.status, r.body.slice(0, 4).toString(), r.body.length)
}

main().catch(console.error)
