const fs = require('fs')
const path = require('path')

const target = path.join(
  __dirname,
  '../../ruoyi-modules/ruoyi-system/src/main/java/com/ruoyi/system/service/education/support/EduQbExamPaperAnswerSupport.java'
)

let content = fs.readFileSync(target, 'utf8')
content = content.replace(
  /\/\*\*[\s\S]*?Parse trailing answer-key sections[\s\S]*?\*\//,
  '/**\n * Parse trailing answer-key sections in exam DOCX.\n */'
)
content = content.replace(
  /Matcher multi = Pattern\.compile\("\^\[A-Da-d\]\(\?:\\\\s\*\[,[\s\S]*?\)\.matcher\(value\);/,
  'Matcher multi = Pattern.compile("^[A-Da-d](?:\\\\s*[,\\\\uFF0C\\\\u3001\\\\s]\\\\s*[A-Da-d])+.*").matcher(value);'
)
fs.writeFileSync(target, content, 'utf8')
console.log('Fixed', target)
