const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')
const line = "import { goPortalLogin } from '@/utils/portalLogin'"
const dup = line + '\n' + line

const files = [
  'views/portal/Home.vue',
  'views/portal/Paper.vue',
  'views/portal/QuestionDetail.vue',
  'views/portal/PortalBrowse.vue',
  'views/portal/mixins/portalBrowseMixin.js',
  'layout-portal/components/PortalHeader.vue',
  'layout-portal/components/PortalFloatingBar.vue'
]

for (const rel of files) {
  const file = path.join(src, rel)
  if (!fs.existsSync(file)) continue
  let text = fs.readFileSync(file, 'utf8')
  if (!text.includes(dup)) {
    console.log('ok', rel)
    continue
  }
  text = text.split(dup).join(line)
  fs.writeFileSync(file, text, 'utf8')
  console.log('fixed', rel)
}
