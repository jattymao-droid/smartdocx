/**
 * Patch preview.vue: answer sheet dialog + makeSheet wiring.
 * Run: node scripts/patch-preview-answer-sheet.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n')

function mustReplace(label, from, to) {
  if (!text.includes(from)) {
    console.error('MISSING:', label)
    process.exit(1)
  }
  text = text.replace(from, to)
}

if (text.includes('answerSheetOpen')) {
  console.log('preview answer-sheet already patched')
  process.exit(0)
}

const dialogBlock = `
    <el-dialog :title="'\\u7b54\\u9898\\u5361\\u8bbe\\u7f6e'" :visible.sync="answerSheetOpen" width="420px" append-to-body>
      <el-form label-width="96px" size="small">
        <el-form-item :label="'\\u663e\\u793a\\u5206\\u503c'">
          <el-switch v-model="answerSheetOptions.showScore" />
        </el-form-item>
        <el-form-item :label="'\\u586b\\u6d82\\u5217\\u6570'">
          <el-radio-group v-model="answerSheetOptions.choicePerRow">
            <el-radio :label="5">5 \\u9898/\\u884c</el-radio>
            <el-radio :label="10">10 \\u9898/\\u884c</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="'\\u8003\\u53f7\\u586b\\u6d82'">
          <el-switch v-model="answerSheetOptions.showExamNumber" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="answerSheetOpen = false">\\u53d6\\u6d88</el-button>
        <el-button type="primary" size="small" :loading="sheetLoading" @click="confirmAnswerSheetExport">\\u751f\\u6210 PDF</el-button>
      </div>
    </el-dialog>
`

mustReplace(
  'dialog insert',
  '    <paper-question-detail-dialog v-model="detailOpen" :question-id="detailQuestionId" />',
  dialogBlock + '    <paper-question-detail-dialog v-model="detailOpen" :question-id="detailQuestionId" />'
)

mustReplace(
  'data fields',
  '      savedPaperId: null\n    }\n  },',
  `      savedPaperId: null,
      answerSheetOpen: false,
      answerSheetOptions: {
        showScore: true,
        choicePerRow: 5,
        judgePerRow: 10,
        showExamNumber: true
      }
    }
  },`
)

mustReplace(
  'makeSheet handler',
  "handler: () => this.stubAction('\u5236\u4f5c\u7b54\u9898\u5361')",
  'handler: () => this.openAnswerSheetDialog()'
)

mustReplace(
  'sheet handler',
  'handler: () => this.downloadAnswerSheet()',
  'handler: () => this.openAnswerSheetDialog()'
)

mustReplace(
  'downloadAnswerSheet method',
  `    downloadAnswerSheet() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      this.sheetLoading = true
      exportAnswerSheetClient(this).then(() => {
        this.$modal.msgSuccess('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u6210\\u529f')
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u5931\\u8d25\\uFF0C\\u8bf7\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },`,
  `    openAnswerSheetDialog() {
      if (!this.canPreview) {
        this.$modal.msgWarning('\\u8bf7\\u5148\\u52a0\\u5165\\u8bd5\\u9898\\u5e76\\u8bbe\\u7f6e\\u5206\\u503c')
        return
      }
      this.answerSheetOpen = true
    },
    confirmAnswerSheetExport() {
      if (!this.canPreview) return
      this.sheetLoading = true
      exportAnswerSheetClient(this, { ...this.answerSheetOptions }).then(() => {
        this.$modal.msgSuccess('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u6210\\u529f')
        this.answerSheetOpen = false
        this.saveDraft(false)
      }).catch(err => {
        console.error('answer sheet export failed', err)
        this.$modal.msgError('\\u7b54\\u9898\\u5361\\u5bfc\\u51fa\\u5931\\u8d25\\uFF0C\\u8bf7\\u7a0d\\u540e\\u91cd\\u8bd5')
      }).finally(() => { this.sheetLoading = false })
    },`
)

fs.writeFileSync(file, text.replace(/\n/g, '\r\n'), 'utf8')
console.log('preview answer-sheet patched OK')
