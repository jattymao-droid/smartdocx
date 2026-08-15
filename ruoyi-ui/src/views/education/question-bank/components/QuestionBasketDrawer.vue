<template>
  <el-drawer
    :visible.sync="visible"
    direction="rtl"
    size="880px"
    :with-header="true"
    :append-to-body="true"
    :custom-class="drawerClass"
    @open="onOpen"
    @close="handleClose"
  >
    <div slot="title" class="basket-header">
      <span class="header-title">试题篮 <em>共{{ localItems.length }}道 · {{ basketTotalScore }}分</em></span>
      <div class="header-toggle">
        <el-switch v-model="autoAddToPaper" size="small" @change="saveAutoPaperPref" />
        <span class="toggle-label">加入试题篮的题目同时加入当前组卷</span>
      </div>
    </div>

    <div v-if="typeTags.length" class="type-tags">
      <span v-for="tag in typeTags" :key="tag.type" class="type-tag">
        {{ tag.label }} {{ tag.selected }}/{{ tag.count }}
        <i class="el-icon-close" @click="removeByType(tag.type)" />
      </span>
    </div>

    <div class="basket-body">
      <div v-for="(group, gIdx) in typeGroups" :key="group.type" class="type-section">
        <div class="section-head">
          <el-checkbox
            :value="isGroupAllSelected(group)"
            :indeterminate="isGroupIndeterminate(group)"
            @change="val => toggleGroup(group, val)"
          />
          <span class="section-title">{{ sectionLabel(gIdx) }}{{ group.label }}</span>
        </div>
        <div class="section-body">
          <div
            v-for="(item, idx) in group.items"
            :key="item.questionId"
            class="basket-item"
            :class="{ 'is-checked': selectedIds.includes(item.questionId) }"
            @click="onItemClick(item.questionId, $event)"
          >
            <el-checkbox
              class="item-check"
              :value="selectedIds.includes(item.questionId)"
              @click.native.stop
              @change="val => toggleSelect(item.questionId, val)"
            />
            <div class="item-body">
              <div class="item-stem">
                <span class="item-no">{{ idx + 1 }}.</span>
                <qb-formula-text class="item-text" :text="displayContent(item)" />
              </div>
              <div class="item-meta">
                <span class="item-diff">{{ difficultyLabel(item) }}</span>
                <span class="item-score">
                  <el-input-number
                    :value="item.scoreValue || 0"
                    :min="0"
                    :max="100"
                    :step="1"
                    size="mini"
                    controls-position="right"
                    @click.native.stop
                    @change="val => setItemScore(item.questionId, val)"
                  />
                  <em>分</em>
                </span>
              </div>
              <div v-if="imageUrls(item).length" class="item-images">
                <el-image
                  v-for="(url, i) in imageUrls(item)"
                  :key="i"
                  :src="resolveImageUrl(url)"
                  :preview-src-list="previewSrcList(item)"
                  fit="contain"
                  class="item-image"
                />
              </div>
              <ul v-if="optionItems(item).length" class="item-options">
                <li v-for="opt in optionItems(item)" :key="opt.label" class="option-item">
                  <span class="option-label">{{ opt.label }}.</span>
                  <qb-formula-text class="option-text" :text="opt.text" />
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!localItems.length" description="暂无试题，从列表加入试题篮" />
    </div>

    <div class="basket-footer">
      <div v-if="hasPaperDraft" class="footer-draft">
        <el-button type="text" class="draft-link" @click="continueDraft">有组卷草稿未完成 继续编辑</el-button>
      </div>
      <div class="footer-bar">
        <div class="footer-left">
          <el-checkbox
            :value="isAllSelected"
            :indeterminate="isIndeterminate"
            @change="toggleSelectAll"
          >全选</el-checkbox>
          <span class="footer-selected">已选{{ selectedIds.length }}题 · 总分{{ selectedTotalScore }}分</span>
        </div>
        <div class="footer-score-tools">
          <span class="score-tool-label">统一分值</span>
          <el-input-number v-model="uniformScore" :min="0" :max="100" size="mini" controls-position="right" />
          <el-button size="mini" @click="applyUniformScore">应用</el-button>
        </div>
        <div class="footer-right">
          <el-button
            size="small"
            icon="el-icon-delete"
            :disabled="!selectedIds.length"
            @click="deleteSelected"
          >删除</el-button>
          <el-button
            size="small"
            icon="el-icon-download"
            :disabled="!localItems.length"
            @click="quickDownload"
          >快速下载</el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!localItems.length"
            @click="goCompose"
          >去组卷</el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import { mapGetters } from 'vuex'
import { MAX_SIZE } from '@/store/modules/questionBasket'
import { stripLeadingQuestionNo } from '@/utils/questionContent'
import { PAPER_DRAFT_KEY, AUTO_PAPER_KEY } from '@/utils/questionBasketPrefs'
import { parseQuestionOption, shouldShowQuestionOptions } from '@/utils/questionOptions'
import dynamicQuestionTypes from '@/mixins/dynamicQuestionTypes'
import { groupItemsByQuestionType } from '@/utils/questionTypes'
import { isPortalPath } from '@/constants/routes'

const SECTION_NUMS = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']

export default {
  name: 'QuestionBasketDrawer',
  mixins: [dynamicQuestionTypes],
  props: {
    value: { type: Boolean, default: false }
  },
  data() {
    return {
      localItems: [],
      selectedIds: [],
      autoAddToPaper: false,
      hasPaperDraft: false,
      uniformScore: 5
    }
  },
  computed: {
    ...mapGetters(['questionBasketItems', 'questionBasketTotalScore']),
    drawerClass() {
      return this.isPortalRoute ? 'qb-basket-drawer qb-basket-drawer--portal' : 'qb-basket-drawer'
    },
    isPortalRoute() {
      const path = this.$route && this.$route.path
      return path && isPortalPath(path)
    },
    basketTotalScore() {
      return this.questionBasketTotalScore || 0
    },
    selectedTotalScore() {
      const set = new Set(this.selectedIds)
      return this.localItems
        .filter(i => set.has(i.questionId))
        .reduce((s, i) => s + (Number(i.scoreValue) || 0), 0)
    },
    visible: {
      get() { return this.value },
      set(val) { this.$emit('input', val) }
    },
    typeGroups() {
      return groupItemsByQuestionType(this.localItems)
    },
    typeTags() {
      return this.typeGroups.map(g => ({
        type: g.type,
        label: g.label,
        count: g.items.length,
        selected: g.items.filter(i => this.selectedIds.includes(i.questionId)).length
      }))
    },
    allQuestionIds() {
      return this.localItems.map(i => i.questionId)
    },
    isAllSelected() {
      return this.localItems.length > 0 && this.selectedIds.length === this.localItems.length
    },
    isIndeterminate() {
      return this.selectedIds.length > 0 && this.selectedIds.length < this.localItems.length
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.onOpen()
      }
    },
    questionBasketItems: {
      handler() {
        this.syncFromStore()
      },
      deep: true,
      immediate: true
    }
  },
  methods: {
    resolvePaperPreviewRoute(query) {
      const portal = isPortalPath(this.$route.path)
      const path = portal
        ? '/paper/preview'
        : '/admin/question-bank-center/question-bank/paper/preview'
      return { path, query: query || {} }
    },
    onOpen() {
      this.syncFromStore()
      this.loadPrefs()
      this.checkPaperDraft()
    },
    syncFromStore() {
      this.localItems = this.questionBasketItems.map(i => ({ ...i }))
      const idSet = new Set(this.allQuestionIds)
      this.selectedIds = this.selectedIds.filter(id => idSet.has(id))
      if (!this.selectedIds.length && this.localItems.length) {
        this.selectedIds = this.allQuestionIds.slice()
      }
    },
    loadPrefs() {
      try {
        this.autoAddToPaper = localStorage.getItem(AUTO_PAPER_KEY) === '1'
      } catch (e) {
        this.autoAddToPaper = false
      }
    },
    saveAutoPaperPref(val) {
      try {
        localStorage.setItem(AUTO_PAPER_KEY, val ? '1' : '0')
      } catch (e) { /* ignore */ }
    },
    checkPaperDraft() {
      try {
        this.hasPaperDraft = !!localStorage.getItem(PAPER_DRAFT_KEY)
      } catch (e) {
        this.hasPaperDraft = false
      }
    },
    sectionLabel(index) {
      const num = SECTION_NUMS[index] || String(index + 1)
      return `${num}、`
    },
    displayContent(item) {
      const text = item.content || item.contentBrief || ''
      return stripLeadingQuestionNo(text)
    },
    difficultyLabel(item) {
      const d = Number(item.difficulty)
      if (Number.isNaN(d)) return '难度 -'
      if (d <= 0.35) return '容易'
      if (d >= 0.75) return '较难'
      return '适中'
    },
    setItemScore(questionId, scoreValue) {
      this.$store.commit('questionBasket/SET_SCORE', { questionId, scoreValue })
      this.syncFromStore()
    },
    applyUniformScore() {
      const score = Number(this.uniformScore) || 0
      const targets = this.selectedIds.length ? this.selectedIds : this.allQuestionIds
      targets.forEach(id => {
        this.$store.commit('questionBasket/SET_SCORE', { questionId: id, scoreValue: score })
      })
      this.syncFromStore()
      this.$modal.msgSuccess(`已将 ${targets.length} 题设为 ${score} 分`)
    },
    parseJsonArray(raw) {
      if (!raw) return []
      if (Array.isArray(raw)) return raw
      try {
        const arr = JSON.parse(raw)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    imageUrls(item) {
      return this.parseJsonArray(item.images)
    },
    previewSrcList(item) {
      return this.imageUrls(item).map(u => this.resolveImageUrl(u))
    },
    optionItems(item) {
      const arr = this.parseJsonArray(item.options)
      if (!arr.length) return []
      if (!shouldShowQuestionOptions(item.questionType, arr)) return []
      return arr.map((text, i) => parseQuestionOption(text, i))
    },
    resolveImageUrl(url) {
      if (!url) return ''
      if (/^https?:\/\//i.test(url)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    toggleSelect(questionId, checked) {
      if (checked) {
        if (!this.selectedIds.includes(questionId)) {
          this.selectedIds = [...this.selectedIds, questionId]
        }
      } else {
        this.selectedIds = this.selectedIds.filter(id => id !== questionId)
      }
    },
    onItemClick(questionId, event) {
      if (event.target.closest('.item-image') || event.target.closest('.el-image__preview')) {
        return
      }
      this.toggleSelect(questionId, !this.selectedIds.includes(questionId))
    },
    toggleSelectAll(checked) {
      this.selectedIds = checked ? this.allQuestionIds.slice() : []
    },
    isGroupAllSelected(group) {
      return group.items.length > 0 && group.items.every(i => this.selectedIds.includes(i.questionId))
    },
    isGroupIndeterminate(group) {
      const selected = group.items.filter(i => this.selectedIds.includes(i.questionId)).length
      return selected > 0 && selected < group.items.length
    },
    toggleGroup(group, checked) {
      const ids = group.items.map(i => i.questionId)
      if (checked) {
        const set = new Set(this.selectedIds)
        ids.forEach(id => set.add(id))
        this.selectedIds = Array.from(set)
      } else {
        const remove = new Set(ids)
        this.selectedIds = this.selectedIds.filter(id => !remove.has(id))
      }
    },
    removeByType(questionType) {
      const label = this.questionTypeLabel(questionType)
      this.$modal.confirm(`确认从试题篮移除所有${label}？`).then(() => {
        this.$store.commit('questionBasket/REMOVE_BY_TYPE', questionType)
        this.syncFromStore()
      }).catch(() => {})
    },
    deleteSelected() {
      if (!this.selectedIds.length) return
      this.$modal.confirm(`确认删除已选的 ${this.selectedIds.length} 道试题？`).then(() => {
        this.$store.commit('questionBasket/REMOVE_BATCH', [...this.selectedIds])
        this.selectedIds = []
        this.syncFromStore()
      }).catch(() => {})
    },
    continueDraft() {
      this.visible = false
      this.$router.push(this.resolvePaperPreviewRoute({ draft: '1' }))
    },
    quickDownload() {
      this.visible = false
      this.$router.push(this.resolvePaperPreviewRoute({ quick: '1' }))
    },
    goCompose() {
      if (!this.localItems.length) return
      if (this.localItems.length > MAX_SIZE) {
        this.$modal.msgWarning(`试题篮最多 ${MAX_SIZE} 题`)
        return
      }
      this.visible = false
      this.$router.push(this.resolvePaperPreviewRoute())
    },
    handleClose() {
      this.visible = false
    }
  }
}
</script>

<style scoped lang="scss">
::v-deep .qb-basket-drawer .el-drawer__header {
  margin-bottom: 0;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #eef2f6;
}
::v-deep .qb-basket-drawer .el-drawer__body {
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}
.basket-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 24px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  em {
    font-style: normal;
    font-weight: 400;
    color: #909399;
    margin-left: 4px;
  }
}
.header-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toggle-label {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}
.type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid #eef2f6;
  background: #fafbfc;
}
.type-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  font-size: 12px;
  color: #606266;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  .el-icon-close {
    font-size: 12px;
    color: #c0c4cc;
    cursor: pointer;
    &:hover { color: #f56c6c; }
  }
}
.basket-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px 130px;
}
.type-section {
  margin-bottom: 20px;
}
.section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0 10px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.section-body {
  background: #f5f8fc;
  border-radius: 6px;
  padding: 4px 12px;
}
.basket-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 2px -6px;
  padding: 12px 10px;
  border-radius: 6px;
  border-bottom: 1px dashed #dce3ed;
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
  &:last-child {
    border-bottom: none;
  }
  &:hover {
    background: rgba(64, 158, 255, 0.07);
    border-left-color: rgba(64, 158, 255, 0.45);
    box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.12);
  }
  &.is-checked {
    background: rgba(64, 158, 255, 0.11);
    border-left-color: #409eff;
    box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.18);
    &:hover {
      background: rgba(64, 158, 255, 0.15);
    }
  }
}
.item-check {
  flex-shrink: 0;
  margin-top: 3px;
  cursor: pointer;
}
.item-body {
  flex: 1;
  min-width: 0;
}
.item-stem {
  font-size: 14px;
  line-height: 1.75;
  color: #303133;
  word-break: break-word;
}
.item-no {
  font-weight: 600;
  margin-right: 4px;
}
.item-text {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}
.item-diff {
  font-size: 12px;
  color: #94a3b8;
  padding: 2px 8px;
  border-radius: 999px;
  background: #F8FAFC;
}
.item-score {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  em {
    font-style: normal;
    font-size: 12px;
    color: #64748b;
  }
}
.item-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
.item-image {
  display: block;
  max-width: 46%;
  max-height: 160px;
  border-radius: 4px;
  background: #fff;
  ::v-deep .el-image__inner {
    max-width: 100%;
    max-height: 160px;
    width: auto;
    height: auto;
    object-fit: contain;
    vertical-align: top;
  }
}
.item-options {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px 28px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}
.option-item {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  font-size: 14px;
  line-height: 1.6;
  white-space: nowrap;
  .option-label {
    flex-shrink: 0;
    color: #303133;
  }
  .option-text {
    color: #303133;
  }
}
.basket-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #ebeef5;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}
.footer-draft {
  padding: 8px 16px 0;
  text-align: right;
  border-bottom: 1px dashed #eef2f6;
}
.draft-link {
  padding: 0;
  font-size: 13px;
  color: #409eff;
}
.footer-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
  padding: 12px 16px;
}
.footer-score-tools {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  justify-content: center;
  min-width: 200px;
}
.score-tool-label {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}
.footer-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.footer-selected {
  font-size: 13px;
  color: #606266;
}
.footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
}

::v-deep .qb-basket-drawer--portal .el-drawer__header {
  background: linear-gradient(90deg, #EFF6FF, #F5F3FF);
}
::v-deep .qb-basket-drawer--portal .section-body {
  background: #F8FAFF;
}
::v-deep .qb-basket-drawer--portal .basket-item.is-checked {
  border-left-color: #2563EB;
  background: rgba(37, 99, 235, 0.08);
}
::v-deep .qb-basket-drawer--portal .footer-bar .el-button--primary {
  background: linear-gradient(135deg, #7C3AED, #2563EB);
  border-color: transparent;
}
</style>
