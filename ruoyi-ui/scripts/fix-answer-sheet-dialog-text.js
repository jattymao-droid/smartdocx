const fs = require('fs')
const path = require('path')
const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8')
text = text.replace(
  /@click="answerSheetOpen = false">\\u53d6\\u6d88<\/el-button>/,
  '@click="answerSheetOpen = false">\u53d6\u6d88</el-button>'
)
text = text.replace(
  /@click="confirmAnswerSheetExport">\\u751f\\u6210 PDF<\/el-button>/,
  '@click="confirmAnswerSheetExport">\u751f\u6210 PDF</el-button>'
)
text = text.replace(
  /<el-radio :label="5">5 \\u9898\/\\u884c<\/el-radio>/,
  '<el-radio :label="5">5 \u9898/\u884c</el-radio>'
)
text = text.replace(
  /<el-radio :label="10">10 \\u9898\/\\u884c<\/el-radio>/,
  '<el-radio :label="10">10 \u9898/\u884c</el-radio>'
)
fs.writeFileSync(file, text, 'utf8')
console.log('fixed answer sheet dialog labels')
