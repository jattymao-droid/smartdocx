const fs = require('fs')
const path = require('path')
const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8')
text = text.replace('  exportAnswerSheetDocxClient,\n', '')
if (!text.includes('paperAnswerSheetExportDocx')) {
  text = text.replace(
    "} from '@/utils/paperAnswerSheetExport'",
    "} from '@/utils/paperAnswerSheetExport'\nimport { exportAnswerSheetDocxClient } from '@/utils/paperAnswerSheetExportDocx'"
  )
}
fs.writeFileSync(file, text, 'utf8')
console.log('import fixed')
