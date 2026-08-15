<template>
  <el-dialog title="试题详情" :visible.sync="visible" width="960px" append-to-body custom-class="qb-detail-dialog">
    <div v-loading="loading">
      <div v-if="row" class="detail-body">
        <div class="detail-meta">
          <div class="detail-meta-item"><label>编号</label><span>{{ row.questionCode }}</span></div>
          <div class="detail-meta-item"><label>学科</label><span>{{ row.subjectName || '-' }}</span></div>
          <div class="detail-meta-item"><label>题型</label><span>{{ typeLabel(row.questionType) }}</span></div>
          <div class="detail-meta-item"><label>难度</label><span>{{ row.difficulty }}</span></div>
          <div class="detail-meta-item detail-meta-chapter"><label>章节</label><span>{{ row.chapterText || '-' }}</span></div>
          <div v-if="knowledgeTags.length" class="detail-meta-item detail-meta-tags">
            <label>知识点</label>
            <span>
              <el-tag v-for="tag in knowledgeTags" :key="tag" size="mini" type="warning" effect="plain" class="detail-tag">{{ tag }}</el-tag>
            </span>
          </div>
        </div>
        <div class="detail-question-panel">
          <qb-formula-text class="detail-stem" block :text="row.content" :images="rawImageUrls" />
          <div v-if="extraImageUrls.length" class="detail-images">
            <el-image v-for="(url, i) in extraImageUrls" :key="i" :src="url" :preview-src-list="imageUrls" fit="contain" class="detail-image" />
          </div>
          <ul v-if="optionItems.length" class="detail-options">
            <li v-for="opt in optionItems" :key="opt.label" class="detail-option-item">
              <span class="option-label">{{ opt.label }}.</span>
              <qb-formula-text class="option-text" :text="opt.text" />
            </li>
          </ul>
        </div>
        <div v-if="answerText || row.analysis" class="detail-footer">
          <div v-if="answerText" class="detail-footer-row detail-answer-row">
            <span class="footer-label">【答案】</span>
            <qb-formula-text class="detail-answer" :text="answerText" />
          </div>
          <div v-if="row.analysis" class="detail-footer-row detail-analysis-row">
            <span class="footer-label">【详解】</span>
            <qb-formula-text class="detail-analysis" :text="row.analysis" />
          </div>
        </div>
      </div>
    </div>
    <div slot="footer">
      <el-button v-if="canManage" type="warning" plain size="small" @click="goEdit">修改题目</el-button>
      <el-button type="primary" size="small" @click="visible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getQuestion } from '@/api/education/question'
import { parseQuestionOption, shouldShowQuestionOptions } from '@/utils/questionOptions'
import { formatChoiceAnswer } from '@/utils/questionAnswer'
import dynamicQuestionTypes from '@/mixins/dynamicQuestionTypes'
import { getQuestionTypeLabel } from '@/utils/questionTypes'
import { getReferencedImageIndices } from '@/utils/questionFormula'
import { isQuestionHtml } from '@/utils/questionContent'

export default {
  name: 'PaperQuestionDetailDialog',
  mixins: [dynamicQuestionTypes],
  props: {
    value: { type: Boolean, default: false },
    questionId: { type: [Number, String], default: null }
  },
  data() {
    return { loading: false, row: null, canManage: false }
  },
  computed: {
    visible: {
      get() { return this.value },
      set(val) { this.$emit('input', val) }
    },
    knowledgeTags() { return this.parseJsonArray(this.row && this.row.knowledgePoints) },
    rawImageUrls() { return this.parseJsonArray(this.row && this.row.images) },
    imageUrls() {
      return this.rawImageUrls.map(u => this.resolveImageUrl(u))
    },
    extraImageUrls() {
      if (isQuestionHtml(this.row && this.row.content)) {
        return []
      }
      const refs = getReferencedImageIndices(this.row && this.row.content)
      return this.imageUrls.filter((_, i) => !refs.has(i))
    },
    optionItems() {
      if (!this.row) return []
      const arr = this.parseJsonArray(this.row.options)
      if (!arr.length || !shouldShowQuestionOptions(this.row.questionType, arr)) return []
      return arr.map((text, i) => parseQuestionOption(text, i))
    },
    answerText() {
      if (!this.row || this.row.correctAnswer == null || this.row.correctAnswer === '') return ''
      return formatChoiceAnswer(this.row.questionType, this.row.correctAnswer)
    },
  },
  watch: {
    visible(val) {
      if (val && this.questionId) this.loadDetail()
    },
    questionId(val) {
      if (this.visible && val) this.loadDetail()
    }
  },
  methods: {
    typeLabel(type) { return getQuestionTypeLabel(type) },
    parseJsonArray(raw) {
      if (!raw) return []
      if (Array.isArray(raw)) return raw
      try {
        const arr = JSON.parse(raw)
        return Array.isArray(arr) ? arr : []
      } catch (e) { return [] }
    },
    resolveImageUrl(url) {
      if (!url) return ''
      if (/^https?:\/\//i.test(url)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    loadDetail() {
      this.loading = true
      getQuestion(this.questionId).then(res => {
        this.row = res.data || null
        this.canManage = !!(this.row && this.row.params && this.row.params.canManage)
      }).finally(() => { this.loading = false })
    },
    goEdit() {
      if (!this.row) return
      this.visible = false
      this.$router.push({ path: '/admin/question-bank', query: { editId: this.row.questionId } })
    }
  }
}
</script>

<style scoped lang="scss">
.detail-body { font-size: 14px; }
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 24px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #606266;
}
.detail-meta-item label { color: #909399; margin-right: 6px; }
.detail-meta-tags .detail-tag { margin-right: 4px; }
.detail-stem {
  display: block;
  max-width: 100%;
  line-height: 1.7;
  margin-bottom: 12px;
  color: #303133;
  word-break: break-word;
  overflow-wrap: anywhere;
}
.detail-images { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; }
.detail-image { width: 280px; height: 175px; border: 1px solid #ebeef5; border-radius: 4px; }
.detail-options { list-style: none; padding: 0; margin: 0 0 12px; }
.detail-option-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 6px 0;
  line-height: 1.6;
}
.option-label { flex-shrink: 0; font-weight: 600; margin-right: 0; }
.detail-option-item ::v-deep .option-text { flex: 1; min-width: 0; }
.detail-footer { border-top: 1px solid #ebeef5; padding-top: 12px; line-height: 1.7; }
.footer-label { color: #409eff; font-weight: 600; margin-right: 6px; }
.detail-analysis { white-space: pre-wrap; color: #606266; }
</style>

<style lang="scss">
.qb-detail-dialog {
  max-width: calc(100vw - 48px);
}
.qb-detail-dialog .el-dialog__body {
  overflow-x: hidden;
}
</style>
