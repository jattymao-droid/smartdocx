<template>
  <div class="app-container education-page qb-page">
    <div class="qb-filter-panel">
      <div class="filter-header">
        <div class="filter-header-left">
          <stage-selector-bar
            v-model="catalog.schoolStage"
            @change="onStageChange"
          />
          <span class="filter-divider" />
          <subject-selector-bar
            v-model="queryParams.subjectId"
            :options="subjectOptions"
            @change="onSubjectChange"
          />
          <span class="qb-mode-tag">章节选题</span>
        </div>
      </div>
      <div class="filter-body">
        <textbook-selector-bar
          :subject-id="queryParams.subjectId"
          :school-stage="catalog.schoolStage"
          :version-id.sync="catalog.versionId"
          :textbook-id.sync="catalog.textbookId"
          @change="onCatalogChange"
        />
      </div>
    </div>

    <div class="qb-layout">
      <chapter-sidebar
        :subject-id="queryParams.subjectId"
        :textbook-id="catalog.textbookId"
        :chapter-id="queryParams.chapterId"
        @chapter-change="onChapterChange"
      />

      <div class="qb-main">
        <div class="qb-toolbar">
          <div class="toolbar-right">
            <el-button type="success" plain icon="el-icon-plus" size="mini" :disabled="multiple" @click="handleAddToBasket">加入试题篮</el-button>
            <el-button v-hasPermi="['education:question:remove']" type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete">批量删除</el-button>
          </div>
        </div>

        <question-filter-bar
          :question-type="queryParams.questionType"
          :difficulty-min="queryParams.difficultyMin"
          :difficulty-max="queryParams.difficultyMax"
          :question-type-options="questionTypeOptions"
          @change="onFilterChange"
        />

        <div class="qb-result-bar">
          <div class="result-sort">
            <span class="sort-item" :class="{ active: sortBy === 'default' }" @click="setSortBy('default')">综合</span>
            <span class="sort-item" :class="{ active: sortBy === 'latest' }" @click="setSortBy('latest')">最新</span>
          </div>
          <div class="result-search">
            <el-input
              v-model="queryParams.keyword"
              clearable
              size="small"
              placeholder="在结果中搜索题干"
              prefix-icon="el-icon-search"
              @keyup.enter.native="handleQuery"
              @clear="handleQuery"
            />
          </div>
          <div class="result-count">共计 <b>{{ total }}</b> 道试题</div>
          <el-checkbox
            class="result-select-all"
            :indeterminate="isIndeterminate"
            :value="isAllSelected"
            @change="handleSelectAll"
          >全选本页</el-checkbox>
        </div>

        <div v-loading="loading" class="question-card-list">
          <question-card
            v-for="(item, idx) in questionList"
            :key="item.questionId"
            :question="item"
            :index="cardIndex(idx)"
            :selected="ids.includes(item.questionId)"
            :can-manage="canManageRow(item)"
            @select="val => toggleSelect(item, val)"
            @add-basket="payload => handleAddToBasket(item, payload && payload.el)"
            @duplicate="handleViewDuplicates(item)"
            @detail="handleViewDetail(item)"
            @edit="handleUpdate(item)"
            @delete="handleDelete(item)"
          />
          <el-empty v-if="!loading && !questionList.length" description="暂无试题" />
        </div>

        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </div>
    </div>

    <el-dialog :title="dialogTitle" :visible.sync="dialogOpen" width="900px" append-to-body :close-on-click-modal="false" @close="onDialogClose">
      <question-form ref="questionForm" :subject-options="subjectOptions" :question-type-options="questionTypeOptions" />
      <div slot="footer">
        <el-button @click="dialogOpen = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="相似题检测" :visible.sync="duplicateDialogOpen" width="640px" append-to-body>
      <div v-loading="duplicateLoading">
        <div v-if="duplicateViewRow" class="dup-source">
          当前：{{ duplicateViewRow.questionCode }} · {{ duplicateBrief(duplicateViewRow.content) }}
        </div>
        <el-empty v-if="!duplicateLoading && !(duplicateViewResult.exactMatches || []).length && !(duplicateViewResult.similarMatches || []).length" description="未发现相似题" />
        <div v-for="item in duplicateViewResult.exactMatches || []" :key="'ex-' + item.questionId" class="dup-row">
          <el-tag size="mini" type="danger">完全相同</el-tag>
          <span>{{ item.questionCode }}</span>
          <span class="dup-text">{{ duplicateBrief(item.content) }}</span>
        </div>
        <div v-for="item in duplicateViewResult.similarMatches || []" :key="'sm-' + item.questionId" class="dup-row">
          <el-tag size="mini" type="warning">相似 {{ duplicateSim(item.similarity) }}</el-tag>
          <span>{{ item.questionCode }}</span>
          <span class="dup-text">{{ duplicateBrief(item.content) }}</span>
        </div>
      </div>
    </el-dialog>

    <el-dialog title="试题详情" :visible.sync="detailOpen" width="960px" append-to-body custom-class="qb-detail-dialog">
      <div v-if="detailRow" class="detail-body">
        <div class="detail-meta">
          <div class="detail-meta-item">
            <label>编号</label><span>{{ detailRow.questionCode }}</span>
          </div>
          <div class="detail-meta-item">
            <label>学科</label><span>{{ detailRow.subjectName }}</span>
          </div>
          <div class="detail-meta-item">
            <label>题型</label><span>{{ questionTypeLabel(detailRow.questionType) }}</span>
          </div>
          <div class="detail-meta-item">
            <label>难度</label><span>{{ detailRow.difficulty }}</span>
          </div>
          <div class="detail-meta-item detail-meta-chapter">
            <label>章节</label><span>{{ detailRow.chapterText || '-' }}</span>
          </div>
          <div class="detail-meta-item">
            <label>审核</label><span>{{ statusLabel(detailRow.status) }}</span>
          </div>
          <div v-if="detailKnowledgeTags.length" class="detail-meta-item detail-meta-tags">
            <label>知识点</label>
            <span>
              <el-tag v-for="tag in detailKnowledgeTags" :key="tag" size="mini" type="warning" effect="plain" class="detail-tag">{{ tag }}</el-tag>
            </span>
          </div>
        </div>
        <div class="detail-question-panel">
          <qb-formula-text class="detail-stem" block :text="detailRow.content" :images="detailImageUrls" />
          <div v-if="detailExtraImageUrls.length" class="detail-images">
            <el-image
              v-for="(url, i) in detailExtraImageUrls"
              :key="i"
              :src="resolveImageUrl(url)"
              :preview-src-list="detailImagePreviewList"
              fit="contain"
              class="detail-image"
            />
          </div>
          <ul v-if="detailOptionItems.length" class="detail-options">
            <li v-for="opt in detailOptionItems" :key="opt.label" class="detail-option-item">
              <span class="option-label">{{ opt.label }}.</span>
              <qb-formula-text class="option-text" :text="opt.text" />
            </li>
          </ul>
        </div>
        <div v-if="detailAnswerText || detailAnalysis" class="detail-footer">
          <div v-if="detailAnswerText" class="detail-footer-row detail-answer-row">
            <span class="footer-label">答案</span>
            <qb-formula-text class="detail-answer" :text="detailAnswerText" />
          </div>
          <div v-if="detailAnalysis" class="detail-footer-row detail-analysis-row">
            <span class="footer-label">解析</span>
            <qb-formula-text class="detail-analysis" :text="detailAnalysis" />
          </div>
        </div>
      </div>
    </el-dialog>

    <question-basket-float />
  </div>
</template>

<script>
import { listQuestion, delQuestion, getQuestion, getQuestionDuplicates } from '@/api/education/question'
import { listSubject } from '@/api/education/subject'
import QuestionForm from './QuestionForm'
import QuestionCard from './components/QuestionCard'
import QuestionBasketFloat from './components/QuestionBasketFloat'
import ChapterSidebar from './components/ChapterSidebar'
import QuestionFilterBar from './components/QuestionFilterBar'
import SubjectSelectorBar from './components/SubjectSelectorBar'
import StageSelectorBar from './components/StageSelectorBar'
import TextbookSelectorBar from './components/TextbookSelectorBar'
import { contentBrief } from '@/store/modules/questionBasket'
import { MAX_SIZE } from '@/store/modules/questionBasket'
import { saveLastSubject } from '@/utils/questionBasketPrefs'
import { parseQuestionOption, shouldShowQuestionOptions } from '@/utils/questionOptions'
import { formatChoiceAnswer } from '@/utils/questionAnswer'
import { flyToBasket, markCardFlyingOut, resetCardFlyingOut } from '@/utils/questionBasketFly'
import { loadQuestionTypeOptions, getQuestionTypeLabel } from '@/utils/questionTypes'
import { getReferencedImageIndices } from '@/utils/questionFormula'
import { isQuestionHtml } from '@/utils/questionContent'

export default {
  name: 'QuestionBank',
  components: {
    QuestionForm, QuestionCard, QuestionBasketFloat, ChapterSidebar,
    QuestionFilterBar, SubjectSelectorBar, StageSelectorBar, TextbookSelectorBar
  },
  data() {
    return {
      loading: false,
      submitLoading: false,
      dialogOpen: false,
      detailOpen: false,
      detailRow: null,
      duplicateDialogOpen: false,
      duplicateLoading: false,
      duplicateViewResult: { exactMatches: [], similarMatches: [] },
      duplicateViewRow: null,
      dialogTitle: '',
      total: 0,
      questionList: [],
      subjectOptions: [],
      ids: [],
      multiple: true,
      sortBy: 'default',
      listRequestSeq: 0,
      basketHiddenCache: {},
      prevBasketQuestionIds: [],
      catalog: {
        schoolStage: '高中',
        versionId: undefined,
        textbookId: undefined
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        subjectId: undefined,
        scenario: undefined,
        questionType: undefined,
        category: undefined,
        keyword: undefined,
        chapterId: undefined,
        chapterText: undefined,
        knowledgePoint: undefined,
        difficultyMin: undefined,
        difficultyMax: undefined,
        examYear: undefined,
        region: undefined,
        grade: undefined,
        semester: undefined,
        examMethod: undefined,
        onlyNew: false,
        status: '0',
        sourceType: undefined
      },
      statusOptions: [
        { label: '已通过', value: '0' },
        { label: '待审核', value: '1' },
        { label: '已退回', value: '2' }
      ],
      questionTypeOptions: []
    }
  },
  computed: {
    isAllSelected() { return this.questionList.length > 0 && this.ids.length === this.questionList.length },
    isIndeterminate() { return this.ids.length > 0 && this.ids.length < this.questionList.length },
    detailAnalysis() { return this.detailRow && this.detailRow.analysis ? this.detailRow.analysis : '' },
    detailKnowledgeTags() { return this.parseJsonArray(this.detailRow && this.detailRow.knowledgePoints) },
    detailImageUrls() { return this.parseJsonArray(this.detailRow && this.detailRow.images) },
    detailExtraImageUrls() {
      if (isQuestionHtml(this.detailRow && this.detailRow.content)) {
        return []
      }
      const refs = getReferencedImageIndices(this.detailRow && this.detailRow.content)
      return this.detailImageUrls.filter((_, i) => !refs.has(i))
    },
    detailImagePreviewList() { return this.detailImageUrls.map(u => this.resolveImageUrl(u)) },
    detailOptionItems() {
      if (!this.detailRow) return []
      const arr = this.parseJsonArray(this.detailRow.options)
      if (!arr.length) return []
      if (!shouldShowQuestionOptions(this.detailRow.questionType, arr)) return []
      return arr.map((text, i) => parseQuestionOption(text, i))
    },
    detailAnswerText() {
      if (!this.detailRow || this.detailRow.correctAnswer == null || this.detailRow.correctAnswer === '') return ''
      return formatChoiceAnswer(this.detailRow.questionType, this.detailRow.correctAnswer)
    }
  },

  watch: {
    '$store.state.questionBasket.items': {
      handler(items) {
        this.onBasketItemsChanged(items)
      },
      deep: true
    }
  },
  created() {
    this.prevBasketQuestionIds = (this.$store.state.questionBasket.items || []).map(i => i.questionId)
    this.loadSubjects()
    loadQuestionTypeOptions().then(options => {
      this.questionTypeOptions = options
    })
    const editId = this.$route.query.editId
    if (editId) {
      this.$nextTick(() => this.handleUpdate({ questionId: Number(editId) }))
    }
  },
  methods: {
    cardIndex(idx) { return (this.queryParams.pageNum - 1) * this.queryParams.pageSize + idx + 1 },
    onStageChange() {
      this.queryParams.chapterId = undefined
      this.queryParams.chapterText = undefined
      this.catalog.versionId = undefined
      this.catalog.textbookId = undefined
      this.handleQuery()
    },
    onSubjectChange() {
      this.queryParams.chapterId = undefined
      this.queryParams.chapterText = undefined
      this.catalog.versionId = undefined
      this.catalog.textbookId = undefined
      const subject = this.subjectOptions.find(s => s.subjectId === this.queryParams.subjectId)
      saveLastSubject(subject)
      this.handleQuery()
    },
    onCatalogChange() {
      this.queryParams.chapterId = undefined
      this.queryParams.chapterText = undefined
      this.handleQuery()
    },
    onChapterChange(payload) {
      const data = payload || {}
      this.queryParams.chapterId = data.chapterId || undefined
      this.queryParams.chapterText = undefined
      this.handleQuery()
    },
    onFilterChange({ field, value }) {
      if (field === 'difficulty') {
        this.queryParams.difficultyMin = value ? value.min : undefined
        this.queryParams.difficultyMax = value ? value.max : undefined
      } else {
        this.queryParams[field] = value
      }
      this.handleQuery()
    },
    statusLabel(status) {
      const item = this.statusOptions.find(i => i.value === status)
      return item ? item.label : status
    },
    isApproved(row) { return row && row.status === '0' },
    loadSubjects() {
      listSubject({ pageNum: 1, pageSize: 500 }).then(res => {
        this.subjectOptions = (res.rows || []).filter(s => s.subjectName !== '总分')
        if (!this.queryParams.subjectId && this.subjectOptions.length) {
          const physics = this.subjectOptions.find(s => s.subjectName === '物理')
          this.queryParams.subjectId = physics ? physics.subjectId : this.subjectOptions[0].subjectId
        }
        const current = this.subjectOptions.find(s => s.subjectId === this.queryParams.subjectId)
        saveLastSubject(current)
        this.getList()
      })
    },
    questionTypeLabel(type) {
      return getQuestionTypeLabel(type)
    },
    parseJsonArray(raw) {
      if (!raw) return []
      if (Array.isArray(raw)) return raw
      try { const arr = JSON.parse(raw); return Array.isArray(arr) ? arr : [] } catch (e) { return [] }
    },
    resolveImageUrl(url) {
      if (!url) return ''
      if (/^https?:\/\//i.test(url)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    canManageRow(row) {
      const roles = this.$store.getters.roles || []
      if (roles.includes('admin') || roles.includes('edu_admin')) return true
      return row.createBy && row.createBy === this.$store.getters.name
    },
    setSortBy(mode) {
      if (this.sortBy === mode) return
      this.sortBy = mode
      this.handleQuery()
    },
    getList() {
      this.loading = true
      const seq = ++this.listRequestSeq
      const params = { ...this.queryParams }
      if (this.sortBy === 'latest') {
        params.params = Object.assign({}, params.params || {}, { orderBy: 'latest' })
      }
      listQuestion(params).then(res => {
        if (seq !== this.listRequestSeq) return
        this.questionList = res.rows || []
        this.total = res.total || 0
        this.ids = []
        this.multiple = true
      }).catch(() => {
        if (seq !== this.listRequestSeq) return
        this.$modal.msgError('加载试题列表失败')
      }).finally(() => {
        if (seq === this.listRequestSeq) this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    toggleSelect(row, checked) {
      const id = row.questionId
      if (checked) { if (!this.ids.includes(id)) this.ids.push(id) }
      else { this.ids = this.ids.filter(i => i !== id) }
      this.multiple = !this.ids.length
    },
    handleSelectAll(checked) {
      this.ids = checked ? this.questionList.map(q => q.questionId) : []
      this.multiple = !this.ids.length
    },
    handleUpdate(row) { this.dialogTitle = '编辑试题'; this.dialogOpen = true; this.$nextTick(() => this.$refs.questionForm.load(row.questionId)) },
    handleViewDetail(row) { this.detailRow = row; this.detailOpen = true },
    onDialogClose() { this.$refs.questionForm && this.$refs.questionForm.reset() },
    submitForm() {
      this.submitLoading = true
      this.$refs.questionForm.submit().then(() => {
        this.$modal.msgSuccess('保存成功')
        this.dialogOpen = false
        this.getList()
      }).catch(() => {}).finally(() => { this.submitLoading = false })
    },
    handleViewDuplicates(row) {
      this.duplicateViewRow = row
      this.duplicateDialogOpen = true
      this.duplicateLoading = true
      this.duplicateViewResult = { exactMatches: [], similarMatches: [] }
      getQuestionDuplicates(row.questionId).then(res => {
        this.duplicateViewResult = res.data || { exactMatches: [], similarMatches: [] }
      }).finally(() => { this.duplicateLoading = false })
    },
    duplicateBrief(text) {
      if (!text) return ''
      const s = String(text).replace(/\s+/g, ' ').trim()
      return s.length > 100 ? s.slice(0, 100) + '\u2026' : s
    },
    duplicateSim(val) { return val == null ? '' : Math.round(val * 100) + '%' },
    async handleAddToBasket(row, sourceEl) {
      const rows = row && row.questionId ? [row] : this.questionList.filter(q => this.ids.includes(q.questionId))
      if (!rows.length) { this.$modal.msgWarning('请先选择试题'); return }
      const blocked = rows.filter(r => !this.isApproved(r))
      if (blocked.length) { this.$modal.msgWarning('仅已通过审核的试题可加入试题篮'); return }
      const payload = rows.map(r => ({
        questionId: r.questionId, questionCode: r.questionCode, content: r.content,
        contentBrief: contentBrief(r.content), questionType: r.questionType, difficulty: r.difficulty,
        options: r.options, images: r.images
      }))
      const basketItems = this.$store.state.questionBasket.items || []
      const newCount = payload.filter(q => !basketItems.some(i => i.questionId === q.questionId)).length
      if (basketItems.length + newCount > MAX_SIZE) {
        this.$modal.msgWarning(`试题篮最多 ${MAX_SIZE} 题`)
        return
      }
      if (newCount === 0) {
        this.$modal.msgWarning('所选试题已在试题篮中')
        return
      }
      const singleFly = !!(sourceEl && rows.length === 1)
      try {
        if (singleFly) await flyToBasket(sourceEl)
        const count = await this.$store.dispatch('questionBasket/addQuestions', payload)
        if (count > 0) {
          this.removeQuestionsFromList(rows.map(r => r.questionId))
          if (!singleFly) {
            this.$modal.msgSuccess(`已加入 ${count} 道试题`)
          }
        }
      } catch (err) {
        resetCardFlyingOut(sourceEl)
        if (err && err.message === 'OVER_LIMIT') this.$modal.msgWarning(`试题篮最多 ${MAX_SIZE} 题`)
      }
    },

    onBasketItemsChanged(items) {
      const list = items || []
      const newIds = list.map(i => i.questionId)
      const newIdSet = new Set(newIds)
      const removedIds = this.prevBasketQuestionIds.filter(id => !newIdSet.has(id))
      this.prevBasketQuestionIds = newIds
      if (removedIds.length) {
        this.restoreQuestionsFromBasket(removedIds)
      }
    },
    matchesListFilters(row) {
      if (!row) return false
      const p = this.queryParams
      if (p.subjectId && row.subjectId !== p.subjectId) return false
      if (p.chapterId && row.chapterId !== p.chapterId) return false
      if (p.questionType && row.questionType !== p.questionType) return false
      if (p.status && String(row.status) !== String(p.status)) return false
      const kw = p.keyword ? String(p.keyword).trim().toLowerCase() : ''
      if (kw && !String(row.content || '').toLowerCase().includes(kw)) return false
      if (p.difficultyMin != null && p.difficultyMin !== '' && Number(row.difficulty) < Number(p.difficultyMin)) return false
      if (p.difficultyMax != null && p.difficultyMax !== '' && Number(row.difficulty) > Number(p.difficultyMax)) return false
      return true
    },
    async restoreQuestionsFromBasket(questionIds) {
      const ids = Array.isArray(questionIds) ? questionIds : [questionIds]
      let restored = 0
      for (const id of ids) {
        if (this.questionList.some(q => q.questionId === id)) {
          if (this.basketHiddenCache[id]) this.$delete(this.basketHiddenCache, id)
          continue
        }
        let row = this.basketHiddenCache[id]
        if (!row) {
          try {
            const res = await getQuestion(id)
            row = res.data
          } catch (e) {
            continue
          }
        }
        if (row && this.matchesListFilters(row)) {
          this.questionList.push({ ...row })
          restored += 1
        }
        if (this.basketHiddenCache[id]) this.$delete(this.basketHiddenCache, id)
      }
      if (restored > 0) this.total += restored
    },
    removeQuestionsFromList(questionIds) {
      const idSet = new Set(Array.isArray(questionIds) ? questionIds : [questionIds])
      let removed = 0
      this.questionList = this.questionList.filter(q => {
        if (idSet.has(q.questionId)) {
          this.$set(this.basketHiddenCache, q.questionId, { ...q })
          removed += 1
          return false
        }
        return true
      })
      if (removed > 0) this.total = Math.max(0, this.total - removed)
      this.ids = this.ids.filter(id => !idSet.has(id))
      this.multiple = !this.ids.length
    },
    handleDelete(row) {
      const ids = (row && row.questionId) ? row.questionId : this.ids
      const idList = Array.isArray(ids) ? ids.filter(Boolean) : (ids ? [ids] : [])
      if (!idList.length) {
        this.$modal.msgWarning('请先选择要删除的试题')
        return
      }
      const payload = idList.length === 1 ? idList[0] : idList
      this.$modal.confirm('确认删除所选试题？').then(() => delQuestion(payload)).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
.education-page { background: #f4f8fd; min-height: calc(100vh - 84px); }
.qb-page { padding-bottom: 20px; }
.qb-filter-panel {
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 8px;
  overflow: hidden;
}
.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid #eef2f6;
  background: linear-gradient(180deg, #fafbfc 0%, #fff 100%);
}
.filter-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  min-width: 0;
}
.filter-header-right {
  flex-shrink: 0;
}
.filter-divider {
  width: 1px;
  height: 20px;
  background: #e4e7ed;
  flex-shrink: 0;
}
.filter-body {
  padding: 4px 16px 8px;
}
.qb-mode-tag {
  font-size: 13px;
  color: #909399;
  padding-left: 4px;
  white-space: nowrap;
  &::before {
    content: '';
    display: inline-block;
    width: 4px;
    height: 4px;
    margin-right: 8px;
    border-radius: 50%;
    background: #c0c4cc;
    vertical-align: middle;
  }
}
.qb-layout { display: flex; align-items: flex-start; gap: 16px; }
.qb-layout > .chapter-sidebar { flex-shrink: 0; }
.qb-main { flex: 1; min-width: 0; }
.qb-toolbar {
  display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px;
  margin-bottom: 12px; padding: 10px 14px; background: #fff; border: 1px solid #e8edf3; border-radius: 8px;
}
.toolbar-left, .toolbar-right { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.qb-result-bar {
  display: flex; align-items: center; flex-wrap: wrap; gap: 12px 20px; margin-bottom: 12px;
  padding: 10px 14px; background: #fff; border: 1px solid #e8edf3; border-radius: 8px;
}
.result-sort { display: flex; gap: 16px; }
.sort-item { font-size: 13px; color: #909399; cursor: pointer; &.active { color: #409eff; font-weight: 600; } }
.result-search { flex: 1; min-width: 200px; max-width: 320px; }
.result-count { font-size: 13px; color: #606266; b { color: #409eff; font-size: 16px; margin: 0 2px; } }
.result-select-all { margin-left: auto; }
.question-card-list { min-height: 200px; }
.dup-source { margin-bottom: 12px; color: #606266; font-size: 13px; }
.dup-row { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 10px; font-size: 13px; }
.dup-text { flex: 1; color: #606266; word-break: break-all; }
.detail-body { font-size: 13px; min-width: 0; }
.detail-meta {
  display: flex; flex-wrap: wrap; gap: 10px 20px;
  padding-bottom: 14px; margin-bottom: 14px;
  border-bottom: 1px solid #eef2f6;
}
.detail-meta-item { label { color: #909399; margin-right: 6px; } span { color: #303133; } }
.detail-meta-chapter { flex: 1 1 100%; }
.detail-meta-tags { width: 100%; }
.detail-tag { margin-right: 6px; margin-bottom: 4px; }
.detail-question-panel {
  padding: 14px 16px;
  background: #fafbfc;
  border: 1px solid #eef2f6;
  border-radius: 8px;
  min-width: 0;
  overflow: hidden;
}
.detail-stem {
  display: block;
  max-width: 100%;
  font-size: 14px;
  line-height: 1.85;
  color: #303133;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}
.detail-images {
  display: flex; flex-wrap: wrap; gap: 10px;
  margin-top: 14px; padding-top: 14px;
  border-top: 1px dashed #e8edf3;
}
.detail-image {
  display: inline-block;
  width: 280px;
  height: 175px;
  border: 1px solid #e8edf3;
  border-radius: 6px;
  background: #fff;
  ::v-deep .el-image__inner {
    max-width: 100%;
    max-height: 100%;
    width: auto;
    height: auto;
    object-fit: contain;
  }
}
.detail-options {
  margin: 14px 0 0; padding: 12px 0 0; list-style: none;
  border-top: 1px dashed #e8edf3;
}
.detail-option-item {
  display: flex; align-items: flex-start; gap: 8px;
  margin-bottom: 8px; line-height: 1.7; font-size: 14px;
  &:last-child { margin-bottom: 0; }
  .option-label { flex-shrink: 0; font-weight: 600; color: #409eff; }
  .option-text { flex: 1; min-width: 0; color: #303133; word-break: break-word; }
}
.detail-footer {
  margin-top: 14px; padding-top: 14px;
  border-top: 1px solid #eef2f6;
}
.detail-footer-row {
  display: flex; align-items: flex-start; gap: 10px;
  &:not(:last-child) { margin-bottom: 12px; }
}
.footer-label {
  flex-shrink: 0; width: 36px; line-height: 1.7;
  color: #909399; font-weight: 600;
}
.detail-answer { font-size: 15px; font-weight: 700; color: #67c23a; line-height: 1.7; }
.detail-analysis {
  flex: 1; line-height: 1.75; color: #606266;
  white-space: pre-wrap; word-break: break-word;
}
</style>

<style lang="scss">
.qb-detail-dialog {
  max-width: calc(100vw - 48px);
}
.qb-detail-dialog .el-dialog__body {
  overflow-x: hidden;
}
</style>
