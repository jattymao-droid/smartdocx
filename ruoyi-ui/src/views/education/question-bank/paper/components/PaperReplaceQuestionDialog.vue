<template>
  <el-dialog
    title="换题"
    :visible.sync="visible"
    width="920px"
    append-to-body
    custom-class="paper-replace-dialog"
    @open="onOpen"
  >
    <div v-if="currentDetail" class="replace-current">
      <span class="label">当前题目：</span>
      <qb-formula-text class="text" :text="displayContent(currentDetail)" />
      <el-tag size="mini" type="info">{{ typeLabel(currentDetail.questionType) }}</el-tag>
    </div>

    <el-tabs v-model="activeTab" class="replace-tabs" @tab-click="onTabChange">
      <el-tab-pane label="相似题推荐" name="similar" />
      <el-tab-pane label="题库搜索" name="search" />
    </el-tabs>

    <div v-show="activeTab === 'search'" class="replace-filters">
      <div class="filter-row">
        <span class="filter-label">学科</span>
        <span class="filter-subject">{{ subjectName || '-' }}</span>
        <span class="filter-label">版本</span>
        <el-select
          v-model="versionId"
          size="small"
          placeholder="选择版本"
          :loading="loadingVersions"
          filterable
          clearable
          class="filter-select"
          @change="onVersionChange"
        >
          <el-option v-for="v in versions" :key="v.versionId" :label="v.versionName" :value="v.versionId" />
        </el-select>
        <span class="filter-label">教材</span>
        <el-select
          v-model="textbookId"
          size="small"
          placeholder="选择教材"
          :loading="loadingTextbooks"
          :disabled="!versionId"
          filterable
          clearable
          class="filter-select"
          @change="onTextbookChange"
        >
          <el-option v-for="t in textbooks" :key="t.textbookId" :label="t.textbookName" :value="t.textbookId" />
        </el-select>
        <span class="filter-label">章节</span>
        <el-select
          v-model="chapterId"
          size="small"
          placeholder="全部章节"
          :loading="loadingChapters"
          :disabled="!textbookId"
          filterable
          clearable
          class="filter-select filter-select-wide"
          @change="onChapterChange"
        >
          <el-option v-for="c in chapterOptions" :key="c.id" :label="c.label" :value="c.id" />
        </el-select>
      </div>
    </div>

    <div v-show="activeTab === 'search'" class="replace-search">
      <el-input
        v-model="keyword"
        clearable
        size="small"
        placeholder="搜索替换题目（同题型）"
        prefix-icon="el-icon-search"
        @keyup.enter.native="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" size="small" :loading="loading" @click="handleSearch">搜索</el-button>
    </div>

    <div v-show="activeTab === 'similar'" v-loading="loadingSimilar" class="replace-list similar-list">
      <div
        v-for="item in similarCandidates"
        :key="item.questionId"
        class="replace-item"
        :class="{ selected: selectedId === item.questionId }"
        @click="selectedId = item.questionId"
      >
        <div class="item-head">
          <span class="code">{{ item.questionCode }}</span>
          <el-tag size="mini" type="warning">相似度 {{ formatSimilarity(item.similarity) }}</el-tag>
          <el-tag size="mini" effect="plain">难度 {{ item.difficulty }}</el-tag>
        </div>
        <qb-formula-text class="item-stem" :text="displayContent(item)" />
      </div>
      <el-empty v-if="!loadingSimilar && !similarCandidates.length" description="暂无相似题推荐" />
    </div>

    <div v-show="activeTab === 'search'" v-loading="loading" class="replace-list">
      <div
        v-for="item in candidates"
        :key="item.questionId"
        class="replace-item"
        :class="{ selected: selectedId === item.questionId }"
        @click="selectedId = item.questionId"
      >
        <div class="item-head">
          <span class="code">{{ item.questionCode }}</span>
          <el-tag size="mini" effect="plain">难度 {{ item.difficulty }}</el-tag>
          <span v-if="item.chapterText" class="item-chapter">{{ item.chapterText }}</span>
        </div>
        <qb-formula-text class="item-stem" :text="displayContent(item)" />
        <div v-if="imageUrls(item).length" class="item-images">
          <el-image
            v-for="(url, i) in imageUrls(item)"
            :key="i"
            :src="resolveImageUrl(url)"
            :preview-src-list="previewSrcList(item)"
            fit="contain"
            class="item-image"
            @click.stop
          />
        </div>
        <ul v-if="optionItems(item).length" class="item-options">
          <li v-for="opt in optionItems(item)" :key="opt.label" class="option-item">
            <span class="option-label">{{ opt.label }}.</span>
            <qb-formula-text class="option-text" :text="opt.text" />
          </li>
        </ul>
      </div>
      <el-empty v-if="!loading && !candidates.length" description="暂无可替换题目" />
    </div>
    <pagination
      v-show="activeTab === 'search' && total > 0"
      :total="total"
      :page.sync="pageNum"
      :limit.sync="pageSize"
      layout="total, prev, pager, next"
      @pagination="loadList"
    />
    <div slot="footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedId" @click="confirmReplace">确认换题</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { listQuestion, getQuestion, getQuestionDuplicates } from '@/api/education/question'
import { listTextbookVersions, listTextbooks, getTextbookChapterTree } from '@/api/education/textbook'
import { stripLeadingQuestionNo } from '@/utils/questionContent'
import { parseQuestionOption, shouldShowQuestionOptions } from '@/utils/questionOptions'
import { getQuestionTypeLabel } from '@/utils/questionTypes'

export default {
  name: 'PaperReplaceQuestionDialog',
  props: {
    value: { type: Boolean, default: false },
    currentQuestion: { type: Object, default: null },
    excludeIds: { type: Array, default: () => [] },
    schoolStage: { type: String, default: '高中' },
    initialTab: { type: String, default: 'search' }
  },
  data() {
    return {
      activeTab: 'search',
      loadingSimilar: false,
      similarCandidates: [],
      loading: false,
      loadingVersions: false,
      loadingTextbooks: false,
      loadingChapters: false,
      keyword: '',
      candidates: [],
      selectedId: null,
      total: 0,
      pageNum: 1,
      pageSize: 5,
      currentDetail: null,
      subjectId: undefined,
      subjectName: '',
      activeSchoolStage: '高中',
      versionId: undefined,
      textbookId: undefined,
      chapterId: undefined,
      chapterText: '',
      versions: [],
      textbooks: [],
      chapterOptions: []
    }
  },
  computed: {
    visible: {
      get() { return this.value },
      set(val) { this.$emit('input', val) }
    }
  },
  methods: {
    typeLabel(type) { return getQuestionTypeLabel(type) },
    displayContent(item) {
      return stripLeadingQuestionNo(item.content || item.contentBrief || '')
    },
    parseJsonArray(raw) {
      if (!raw) return []
      if (Array.isArray(raw)) return raw
      try {
        const arr = JSON.parse(raw)
        return Array.isArray(arr) ? arr : []
      } catch (e) { return [] }
    },
    imageUrls(item) {
      return this.parseJsonArray(item.images)
    },
    previewSrcList(item) {
      return this.imageUrls(item).map(u => this.resolveImageUrl(u))
    },
    optionItems(item) {
      const arr = this.parseJsonArray(item.options)
      if (!arr.length || !shouldShowQuestionOptions(item.questionType, arr)) return []
      return arr.map((text, i) => parseQuestionOption(text, i))
    },
    resolveImageUrl(url) {
      if (!url) return ''
      if (/^https?:\/\//i.test(url)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    flattenChapters(nodes, prefix) {
      const list = []
      ;(nodes || []).forEach(node => {
        if (!node || node.id === 'all') return
        const label = prefix ? `${prefix} > ${node.label}` : node.label
        list.push({ id: node.id, label })
        if (node.children && node.children.length) {
          list.push(...this.flattenChapters(node.children, label))
        }
      })
      return list
    },
    onOpen() {
      this.activeTab = this.initialTab || 'search'
      this.selectedId = null
      this.similarCandidates = []
      this.pageNum = 1
      this.keyword = ''
      this.versionId = undefined
      this.textbookId = undefined
      this.chapterId = undefined
      this.chapterText = ''
      this.chapterOptions = []
      this.textbooks = []
      this.currentDetail = this.currentQuestion ? { ...this.currentQuestion } : null
      if (!this.currentQuestion) return
      getQuestion(this.currentQuestion.questionId).then(res => {
        const data = res.data
        if (!data) return
        this.currentDetail = data
        this.subjectId = data.subjectId
        this.subjectName = data.subjectName || ''
        const catalog = data.params || {}
        if (catalog.schoolStage) this.activeSchoolStage = catalog.schoolStage
        else this.activeSchoolStage = this.schoolStage
        if (catalog.versionId) this.versionId = catalog.versionId
        if (catalog.textbookId) this.textbookId = catalog.textbookId
        if (data.chapterId) {
          this.chapterId = data.chapterId
          this.chapterText = data.chapterText || ''
        }
        this.loadVersions().then(() => {
          if (this.activeTab === 'similar') this.loadSimilar()
          else this.loadList()
        })
      }).catch(() => {
        this.activeSchoolStage = this.schoolStage
        this.loadVersions().then(() => {
          if (this.activeTab === 'similar') this.loadSimilar()
          else this.loadList()
        })
      })
    },
    onTabChange() {
      this.selectedId = null
      if (this.activeTab === 'similar') this.loadSimilar()
      else this.loadList()
    },
    formatSimilarity(val) {
      const n = Number(val)
      if (!Number.isFinite(n)) return '-'
      return (n * 100).toFixed(0) + '%'
    },
    loadSimilar() {
      if (!this.currentQuestion) return
      this.loadingSimilar = true
      getQuestionDuplicates(this.currentQuestion.questionId).then(res => {
        const data = res.data || {}
        const exclude = new Set([...(this.excludeIds || []), this.currentQuestion.questionId])
        const sameType = (this.currentQuestion.questionType || '').trim()
        const pool = [
          ...(data.exactMatches || []),
          ...(data.similarMatches || [])
        ]
        const seen = new Set()
        this.similarCandidates = pool.filter(item => {
          if (!item || exclude.has(item.questionId) || seen.has(item.questionId)) return false
          if (sameType && item.questionType !== sameType) return false
          seen.add(item.questionId)
          return true
        })
      }).finally(() => { this.loadingSimilar = false })
    },
    loadVersions() {
      if (!this.subjectId) return Promise.resolve()
      this.loadingVersions = true
      return listTextbookVersions(this.subjectId, this.activeSchoolStage).then(res => {
        this.versions = res.data || []
        if (this.versions.length && !this.versionId) {
          this.versionId = this.versions[0].versionId
        }
        return this.loadTextbooks()
      }).finally(() => { this.loadingVersions = false })
    },
    loadTextbooks() {
      if (!this.versionId) {
        this.textbooks = []
        this.textbookId = undefined
        this.chapterOptions = []
        this.chapterId = undefined
        this.chapterText = ''
        return Promise.resolve()
      }
      this.loadingTextbooks = true
      return listTextbooks(this.versionId).then(res => {
        this.textbooks = res.data || []
        const keep = this.textbooks.some(t => t.textbookId === this.textbookId)
        if (!keep) {
          this.textbookId = this.textbooks.length ? this.textbooks[0].textbookId : undefined
        }
        return this.loadChapters()
      }).finally(() => { this.loadingTextbooks = false })
    },
    loadChapters() {
      if (!this.textbookId) {
        this.chapterOptions = []
        return Promise.resolve()
      }
      this.loadingChapters = true
      return getTextbookChapterTree(this.textbookId, this.subjectId).then(res => {
        this.chapterOptions = this.flattenChapters(res.data || [])
        const keep = this.chapterOptions.some(c => c.id === this.chapterId)
        if (!keep) {
          this.chapterId = undefined
          this.chapterText = ''
        }
      }).finally(() => { this.loadingChapters = false })
    },
    onVersionChange() {
      this.textbookId = undefined
      this.chapterId = undefined
      this.chapterText = ''
      this.pageNum = 1
      this.loadTextbooks().then(() => this.loadList())
    },
    onTextbookChange() {
      this.chapterId = undefined
      this.chapterText = ''
      this.pageNum = 1
      this.loadChapters().then(() => this.loadList())
    },
    onChapterChange(val) {
      if (!val) {
        this.chapterText = ''
      } else {
        const hit = this.chapterOptions.find(c => c.id === val)
        this.chapterText = hit ? hit.label : ''
      }
      this.pageNum = 1
      this.loadList()
    },
    handleSearch() {
      this.pageNum = 1
      this.loadList()
    },
    loadList() {
      if (!this.currentQuestion) return
      this.loading = true
      const params = {
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        questionType: this.currentQuestion.questionType,
        status: '0',
        keyword: this.keyword || undefined
      }
      if (this.subjectId) params.subjectId = this.subjectId
      if (this.chapterId) {
        params.chapterId = this.chapterId
      } else if (this.chapterText) {
        params.chapterText = this.chapterText
      }
      listQuestion(params).then(res => {
        const rows = res.rows || []
        const exclude = new Set([...(this.excludeIds || []), this.currentQuestion.questionId])
        this.candidates = rows.filter(r => !exclude.has(r.questionId))
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    confirmReplace() {
      if (!this.selectedId) return
      getQuestion(this.selectedId).then(res => {
        const data = res.data
        if (!data) {
          this.$modal.msgError('获取题目失败')
          return
        }
        this.$emit('replace', { oldQuestionId: this.currentQuestion.questionId, newQuestion: data })
        this.visible = false
        this.$modal.msgSuccess('换题成功')
      })
    }
  }
}
</script>

<style scoped lang="scss">
.replace-current {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  background: #f5f9ff;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 13px;
  line-height: 1.6;
  .label { color: #909399; flex-shrink: 0; }
  .text { flex: 1; min-width: 200px; color: #303133; }
}
.replace-filters {
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #fafbfc;
  border: 1px solid #eef2f6;
  border-radius: 4px;
}
.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 10px;
}
.filter-label {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
.filter-subject {
  font-size: 13px;
  color: #303133;
  font-weight: 600;
  margin-right: 8px;
}
.filter-select {
  width: 140px;
  &.filter-select-wide { width: 200px; }
}
.replace-search {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.replace-list {
  min-height: 280px;
  max-height: 480px;
  overflow-y: auto;
}
.replace-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
  &:hover { border-color: #c6e2ff; background: #f7fbff; }
  &.selected {
    border-color: #409eff;
    background: #ecf5ff;
    box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.2);
  }
}
.item-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  .code { font-size: 12px; color: #909399; }
  .item-chapter {
    flex: 1;
    min-width: 120px;
    font-size: 12px;
    color: #909399;
    text-align: right;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.item-stem {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}
.item-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.item-image {
  width: 160px;
  height: 108px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}
.item-options {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
}
.option-item {
  display: inline-flex;
  align-items: flex-start;
  gap: 4px;
  font-size: 13px;
  min-width: 22%;
  line-height: 1.6;
}
.option-label { font-weight: 600; color: #409eff; }
</style>
