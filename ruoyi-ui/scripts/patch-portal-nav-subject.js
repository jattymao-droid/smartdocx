const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/layout-portal/components/PortalNav.vue')
let text = fs.readFileSync(file, 'utf8')

const oldBlock = `    pickSubject(item) {
      this.$emit('update:subjectId', item.subjectId)
      this.panelOpen = false
      this.$router.push({ path: '/portal/chapter', query: { subjectId: item.subjectId } })
    }`

const newBlock = `    pickSubject(item) {
      this.$emit('update:subjectId', item.subjectId)
      this.panelOpen = false
      const path = this.$route.path
      const target = path === '/portal/knowledge' ? '/portal/knowledge' : '/portal/chapter'
      this.$router.push({
        path: target,
        query: { ...this.$route.query, subjectId: item.subjectId }
      })
    }`

if (text.includes(oldBlock)) {
  fs.writeFileSync(file, text.split(oldBlock).join(newBlock), 'utf8')
  console.log('patched PortalNav pickSubject')
} else {
  console.log('PortalNav already patched or format changed')
}
