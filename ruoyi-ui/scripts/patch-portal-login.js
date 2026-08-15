/**
 * Patch portal files for goPortalLogin (ASCII-only edits, preserves UTF-8 Chinese).
 * Idempotent: safe to run multiple times.
 */
const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')
const goImport = "import { goPortalLogin } from '@/utils/portalLogin'"

function patch(rel, replacements) {
  const file = path.join(src, rel)
  let text = fs.readFileSync(file, 'utf8')
  let changed = false
  for (const [from, to] of replacements) {
    if (!text.includes(from)) continue
    if (from === to) continue
    text = text.split(from).join(to)
    changed = true
  }
  if (changed) {
    fs.writeFileSync(file, text, { encoding: 'utf8' })
    console.log('patched', rel)
  } else {
    console.log('skip (already ok):', rel)
  }
}

function addImportAfter(text, anchor, importLine) {
  if (text.includes(importLine)) return text
  if (!text.includes(anchor)) return text
  return text.replace(anchor, anchor + '\n' + importLine)
}

function patchImport(rel, anchor) {
  const file = path.join(src, rel)
  let text = fs.readFileSync(file, 'utf8')
  const next = addImportAfter(text, anchor, goImport)
  if (next !== text) {
    fs.writeFileSync(file, next, { encoding: 'utf8' })
    console.log('import added', rel)
  }
}

patch('layout-portal/components/PortalHeader.vue', [
  ["this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })", 'goPortalLogin(this.$router, this.$route.fullPath)']
])
patchImport('layout-portal/components/PortalHeader.vue', "import PortalNav from './PortalNav'")

patch('layout-portal/components/PortalFloatingBar.vue', [
  ["this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })", 'goPortalLogin(this.$router, this.$route.fullPath)']
])
patchImport('layout-portal/components/PortalFloatingBar.vue', "import QuestionBasketDrawer from '@/views/education/question-bank/components/QuestionBasketDrawer'")

patch('views/portal/mixins/portalBrowseMixin.js', [
  ["this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })", 'goPortalLogin(this.$router, this.$route.fullPath)']
])
patchImport('views/portal/mixins/portalBrowseMixin.js', "import { getToken } from '@/utils/auth'")

patch('views/portal/Home.vue', [
  ["this.$router.push({ path: '/login', query: { redirect: '/portal/home' } })", "goPortalLogin(this.$router, '/portal/home')"]
])
patchImport('views/portal/Home.vue', "import { listQuestion } from '@/api/education/question'")

patch('views/portal/PortalBrowse.vue', [
  ["this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })", 'goPortalLogin(this.$router, this.$route.fullPath)']
])
patchImport('views/portal/PortalBrowse.vue', "import TextbookSelectorBar from '@/views/education/question-bank/components/TextbookSelectorBar'")

patch('views/portal/QuestionDetail.vue', [
  ["this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })", 'goPortalLogin(this.$router, this.$route.fullPath)']
])
patchImport('views/portal/QuestionDetail.vue', "import { getQuestionTypeLabel } from '@/utils/questionTypes'")

patch('views/portal/Paper.vue', [
  ["this.$router.push({ path: '/login', query: { redirect: '/portal/paper' } })", "goPortalLogin(this.$router, '/portal/paper')"]
])
patchImport('views/portal/Paper.vue', "import { mapGetters } from 'vuex'")

console.log('portal login patches done')
