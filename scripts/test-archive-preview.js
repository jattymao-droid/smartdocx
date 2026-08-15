const http = require('http')

function req(url) {
  return new Promise((resolve, reject) => {
    const u = new URL(url)
    const started = Date.now()
    http.get({ hostname: u.hostname, port: u.port, path: u.pathname + u.search }, (res) => {
      const chunks = []
      res.on('data', (c) => chunks.push(c))
      res.on('end', () => {
        resolve({ status: res.statusCode, body: Buffer.concat(chunks), ms: Date.now() - started })
      })
    }).on('error', reject)
  })
}

function b64(text) {
  return Buffer.from(text, 'utf8').toString('base64')
}

async function main() {
  const fk = '\u7269\u7406\u5047\u671f\u4f5c\u4e1a16-18_20260710204803A002.zip'
  const innerPath = `\u7269\u7406\u5047\u671f\u4f5c\u4e1a16-18_20260710204803A002.zip_/\u7269\u7406\u5047\u671f\u4f5c\u4e1a16-18/\u7269\u7406\u5047\u671f\u4f5c\u4e1a16\u53c2\u8003\u7b54\u6848.docx`
  const kkBase = 'http://127.0.0.1:8012/'
  const innerUrl = `${kkBase.replace(/\/$/, '')}/${innerPath}?fileKey=${encodeURIComponent(fk)}`
  const warmup = `${kkBase.replace(/\/$/, '')}/onlinePreview?url=${encodeURIComponent(b64(innerUrl))}&officePreviewType=pdf`

  console.log('innerUrl sample', innerUrl.slice(0, 80))

  console.log('\nA) archive-warmup via gateway')
  const warmProxy = await req(`http://localhost:8080/system/portal/banner/archive-warmup?url=${encodeURIComponent(warmup)}`)
  console.log(' status', warmProxy.status, 'ms', warmProxy.ms)

  const pdfPath = innerPath.replace(/\.docx$/i, '.pdf')
  const fileUrl = `${kkBase.replace(/\/$/, '')}/${pdfPath.split('/').map(encodeURIComponent).join('/')}`
  const media = `http://localhost:8080/system/portal/banner/media?url=${encodeURIComponent(fileUrl)}`

  const probe = async (label) => {
    const r = await req(media)
    const sig = r.body.slice(0, 4).toString()
    console.log(` ${label}:`, r.status, r.body.length, sig)
    return sig === '%PDF'
  }

  await probe('media immediately after warmup')

  console.log('\nB) direct kkFileView warmup')
  const direct = await req(warmup)
  console.log(' status', direct.status, 'ms', direct.ms, 'html', direct.body.length)
  await probe('media after direct warmup')

  console.log('\nC) poll media up to 60s')
  for (let i = 1; i <= 12; i++) {
    await new Promise((r) => setTimeout(r, 5000))
    if (await probe(`t+${i * 5}s`)) break
  }
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
