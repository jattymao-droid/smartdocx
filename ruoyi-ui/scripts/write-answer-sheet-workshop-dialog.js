/**
 * Fix garbled Chinese in answer-sheet workshop dialog (preview.vue).
 * Run: node scripts/write-answer-sheet-workshop-dialog.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/education/question-bank/paper/preview.vue')
let text = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n')

const startMarker = '    <el-dialog\n      :title="answerSheetWorkshopMode'
const endMarker = '    <paper-question-detail-dialog'

const startIdx = text.indexOf(startMarker)
const endIdx = text.indexOf(endMarker)
if (startIdx < 0 || endIdx < 0) {
  console.error('dialog markers not found', startIdx, endIdx)
  process.exit(1)
}

const dialog = `    <el-dialog
      :title="answerSheetWorkshopMode ? '${'\u5236\u4f5c\u7b54\u9898\u5361'}' : '${'\u7b54\u9898\u5361\u8bbe\u7f6e'}'"
      :visible.sync="answerSheetOpen"
      width="960px"
      append-to-body
      class="answer-sheet-workshop-dialog"
      @open="onAnswerSheetDialogOpen"
    >
      <div class="as-workshop">
        <div class="as-workshop-left">
          <div class="as-stats-bar">
            <span>${'\u5171'} {{ answerSheetStats.total }} ${'\u9898'}</span>
            <span>${'\u5ba2\u89c2'} {{ answerSheetStats.objective }}</span>
            <span>${'\u586b\u7a7a'} {{ answerSheetStats.fill }}</span>
            <span>${'\u4e3b\u89c2'} {{ answerSheetStats.subjective }}</span>
            <span>${'\u6ee1\u5206'} {{ answerSheetStats.totalScore }}</span>
          </div>
          <el-form label-width="96px" size="small">
            <el-form-item label="${'\u5361\u7247\u7c7b\u578b'}">
              <el-radio-group v-model="answerSheetOptions.sheetMode">
                <el-radio label="student">${'\u5b66\u751f\u586b\u6d82\u5361'}</el-radio>
                <el-radio label="teacher">${'\u6559\u5e08\u53c2\u8003\u7248'}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="${'\u7248\u9762\u98ce\u683c'}">
              <el-radio-group v-model="answerSheetOptions.style">
                <el-radio label="standard">${'\u6807\u51c6'}</el-radio>
                <el-radio label="compact">${'\u7d27\u51d1'}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="${'\u5305\u542b\u533a\u57df'}">
              <el-checkbox v-model="answerSheetOptions.includeObjective">${'\u5ba2\u89c2\u9898\u586b\u6d82'}</el-checkbox>
              <el-checkbox v-model="answerSheetOptions.includeFill">${'\u586b\u7a7a\u9898'}</el-checkbox>
              <el-checkbox v-model="answerSheetOptions.includeSubjective">${'\u4e3b\u89c2\u9898\u4f5c\u7b54'}</el-checkbox>
            </el-form-item>
            <el-form-item label="${'\u663e\u793a\u5206\u503c'}">
              <el-switch v-model="answerSheetOptions.showScore" />
            </el-form-item>
            <el-form-item label="${'\u586b\u6d82\u5217\u6570'}">
              <el-radio-group v-model="answerSheetOptions.choicePerRow">
                <el-radio :label="5">5 ${'\u9898'}/${'\u884c'}</el-radio>
                <el-radio :label="10">10 ${'\u9898'}/${'\u884c'}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="${'\u8003\u53f7\u586b\u6d82'}">
              <el-switch v-model="answerSheetOptions.showExamNumber" />
            </el-form-item>
          </el-form>
        </div>
        <div v-loading="answerSheetPreviewLoading" class="as-workshop-right">
          <div class="as-preview-toolbar">
            <span>${'\u5b9e\u65f6\u9884\u89c8'}</span>
            <el-button type="text" size="mini" icon="el-icon-refresh" @click="refreshAnswerSheetPreview">${'\u5237\u65b0'}</el-button>
          </div>
          <div class="as-preview-scroll">
            <div v-if="answerSheetPreviewHtml" class="as-preview-paper" v-html="answerSheetPreviewHtml" />
            <el-empty v-else :description="'${'\u9884\u89c8\u52a0\u8f7d\u4e2d...'}'" />
          </div>
        </div>
      </div>
      <div slot="footer">
        <el-button size="small" @click="answerSheetOpen = false">${'\u53d6\u6d88'}</el-button>
        <el-button size="small" icon="el-icon-printer" :loading="sheetLoading" @click="printAnswerSheet">${'\u6253\u5370'}</el-button>
        <el-button type="primary" size="small" icon="el-icon-download" :loading="sheetLoading" @click="confirmAnswerSheetExport">${'\u4e0b\u8f7d'} PDF</el-button>
      </div>
    </el-dialog>

`

text = text.slice(0, startIdx) + dialog + text.slice(endIdx)
fs.writeFileSync(file, text.replace(/\n/g, '\r\n'), 'utf8')
console.log('answer sheet workshop dialog rewritten (UTF-8)')
