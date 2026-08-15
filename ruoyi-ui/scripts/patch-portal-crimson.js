/**
 * Patch portal write scripts + components to school crimson palette.
 */
const fs = require('fs')
const path = require('path')

const root = path.join(__dirname, '..')

const replacements = [
  ['#52aac1', '#991B1B'],
  ['#52AAC1', '#991B1B'],
  ['#6ebdd4', '#B83232'],
  ['#6EBDD4', '#B83232'],
  ['#3d7a8f', '#7A1515'],
  ['#3d94ab', '#8B2020'],
  ['#d4e8ef', '#E8D5C4'],
  ['#f0f8fb', '#FBF6ED'],
  ['#dceef5', '#F5EDE3'],
  ['#eef4f7', '#F0E8E0'],
  ['#f6f9fb', '#FAF8F5'],
  ['#f8fcfe', '#FDF9F6'],
  ['#fafcfd', '#FDF9F6'],
  ['#e8a54b', '#D4AF37'],
  ['#f0c078', '#E8D5A3'],
  ['#fff8ed', '#FBF6ED'],
  ['#fde8c8', '#F0E0C8'],
  ['#fff4e6', '#FBF6ED'],
  ['#1e293b', '#2C1810'],
  ['rgba(82,170,193', 'rgba(153,27,27'],
  ['rgba(82, 170, 193', 'rgba(153, 27, 27'],
  ['rgba(110,189,212', 'rgba(184,50,50'],
  ['rgba(110, 189, 212', 'rgba(184, 50, 50'],
  ['#409eff', '#991B1B'],
  ['#ecf5ff', '#FBF6ED'],
  ['#4a9fff', '#B83232'],
  ['Nile Blue', 'School Crimson'],
  ['������', 'У�պ�']
]

const files = [
  'scripts/write-portal-shell.js',
  'scripts/write-portal-nav.js',
  'scripts/write-portal-browse.js',
  'scripts/write-portal-home.js',
  'scripts/write-portal-paper.js',
  'scripts/write-portal-float-bar.js',
  'scripts/write-portal-auth-dialog.js',
  'src/views/education/question-bank/components/QuestionCard.vue',
  'src/views/education/question-bank/components/ChapterSidebar.vue',
  'src/layout-portal/index.vue'
]

for (const rel of files) {
  const f = path.join(root, rel)
  if (!fs.existsSync(f)) continue
  let text = fs.readFileSync(f, 'utf8')
  let changed = false
  for (const [from, to] of replacements) {
    if (text.includes(from)) {
      text = text.split(from).join(to)
      changed = true
    }
  }
  if (changed) {
    fs.writeFileSync(f, text, 'utf8')
    console.log('patched', rel)
  }
}
