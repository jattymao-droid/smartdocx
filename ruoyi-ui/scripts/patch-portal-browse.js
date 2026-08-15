/** ASCII-only patches for portal browse bugs (preserves UTF-8). */
const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')

function patch(rel, replacements) {
  const file = path.join(src, rel)
  let text = fs.readFileSync(file, 'utf8')
  for (const [from, to] of replacements) {
    if (!text.includes(from)) {
      console.warn('skip:', rel, from.slice(0, 60))
      continue
    }
    text = text.split(from).join(to)
  }
  fs.writeFileSync(file, text, { encoding: 'utf8' })
  console.log('patched', rel)
}

patch('views/portal/PortalBrowse.vue', [
  [`                :key="tag"
                class="tag-item"
                :class="{ active: queryParams.knowledgePoint === tag }"
                @click="pickKnowledge(tag)"
              >{{ tag }}</div>`,
   `                :key="tag.tagId || tag.tagName"
                class="tag-item"
                :class="{ active: queryParams.knowledgePoint === tag.tagName }"
                @click="pickKnowledge(tag.tagName)"
              >{{ tag.tagName }}</div>`]
])

patch('views/portal/mixins/portalBrowseMixin.js', [
  [`    onChapterChange(chapterId) {
      this.queryParams.chapterId = chapterId || undefined
      this.handleQuery()
    },`,
   `    onChapterChange(payload) {
      const data = payload || {}
      this.queryParams.chapterId = data.chapterId || undefined
      this.handleQuery()
    },`]
])

console.log('portal browse bug patches done')
