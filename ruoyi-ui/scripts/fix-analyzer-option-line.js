const fs = require('fs')
const path = require('path')

const target = path.join(
  __dirname,
  '../../ruoyi-modules/ruoyi-system/src/main/java/com/ruoyi/system/service/education/support/EduQbExamPaperBlockAnalyzer.java'
)

let content = fs.readFileSync(target, 'utf8')
content = content.replace(
  /private static final Pattern OPTION_LINE = Pattern\.compile\([\s\S]*?\);/,
  'private static final Pattern OPTION_LINE = Pattern.compile(\n            "^[A-Ha-d][\\\\.\\\\uFF0E\\\\u3001\\\\u3002\\\\)\\\\uFF09:\\\\uFF1A]\\\\s*|^[A-Ha-d]\\\\s+\\\\S");'
)
fs.writeFileSync(target, content, 'utf8')
console.log('Fixed', target)
