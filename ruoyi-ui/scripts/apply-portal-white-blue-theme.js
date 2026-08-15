/**
 * Replace crimson/warm portal colors with white + blue theme in generated sources.
 * Run: node scripts/apply-portal-white-blue-theme.js
 */
const fs = require('fs')
const path = require('path')

const root = path.join(__dirname, '..')

const REPLACEMENTS = [
  ['#991B1B', '#2563EB'],
  ['#991b1b', '#2563eb'],
  ['#B83232', '#3B82F6'],
  ['#b83232', '#3b82f6'],
  ['#7A1515', '#1D4ED8'],
  ['#7a1515', '#1d4ed8'],
  ['#FBF6ED', '#EFF6FF'],
  ['#FAF8F5', '#F8FAFC'],
  ['#FDF9F6', '#FFFFFF'],
  ['#F5EDE3', '#DBEAFE'],
  ['#F5F0EB', '#F1F5F9'],
  ['#F0E0C8', '#BFDBFE'],
  ['#E8D5C4', '#E2E8F0'],
  ['#F0E8E0', '#EEF2F6'],
  ['#2C1810', '#1E293B'],
  ['#6B5344', '#64748B'],
  ['#D4AF37', '#3B82F6'],
  ['#D4CCC4', '#CBD5E1'],
  ['#9A7B2E', '#2563EB'],
  ['#E8D5A3', '#93C5FD'],
  ['rgba(153, 27, 27,', 'rgba(37, 99, 235,'],
  ['rgba(153,27,27,', 'rgba(37,99,235,'],
  ['rgba(44, 24, 16,', 'rgba(15, 23, 42,'],
  ['rgba(232, 165, 75,', 'rgba(59, 130, 246,'],
  ['School crimson & gold', 'White primary, blue accent'],
  ['School crimson', 'Portal blue']
]

const FILES = [
  'scripts/write-portal-browse.js',
  'scripts/write-portal-home.js',
  'scripts/write-portal-nav.js',
  'scripts/write-portal-paper.js',
  'scripts/write-portal-float-bar.js',
  'scripts/write-portal-auth-dialog.js',
  'scripts/write-portal-shell.js',
  'src/layout-portal/index.vue',
  'src/views/education/question-bank/components/QuestionCard.vue',
  'src/views/education/question-bank/components/ChapterSidebar.vue',
  'src/views/education/question-bank/paper/preview.vue',
  'src/views/portal/QuestionDetail.vue'
]

function patchFile(rel) {
  const f = path.join(root, rel)
  if (!fs.existsSync(f)) {
    console.warn('skip (missing):', rel)
    return
  }
  let text = fs.readFileSync(f, 'utf8')
  let changed = false
  REPLACEMENTS.forEach(([from, to]) => {
    if (text.includes(from)) {
      text = text.split(from).join(to)
      changed = true
    }
  })
  if (changed) {
    fs.writeFileSync(f, text, 'utf8')
    console.log('patched', rel)
  }
}

FILES.forEach(patchFile)
console.log('white-blue theme patches done')
