<template>
  <div ref="cardRoot" class="question-card" :class="{ 'is-selected': selected, 'question-card--portal': variant === 'portal', 'is-expanded': variant === 'portal' && expanded }">
    <div
      v-if="variant !== 'portal'"
      class="card-ribbon"
      :class="{ disabled: question.status !== '0' }"
      title="加入试题篮"
      @click.stop="onAddBasket"
    >
      <i class="el-icon-collection-tag" />
    </div>

    <div class="card-top">
      <el-checkbox v-if="variant !== 'portal'" v-model="checked" class="card-check" @click.native.stop />
      <span v-if="variant === 'portal' && index != null" class="card-index-badge">{{ index }}</span>
      <div class="card-meta">
        <span class="meta-type-badge" :class="'meta-type-badge--' + typeBadgeTone">{{ typeLabel }}</span>
        <span class="meta-diff-badge" :class="'meta-diff-badge--' + difficultyLevel">{{ difficultyShort }}</span>
        <span v-if="variant !== 'portal' && !Number.isNaN(Number(question.difficulty))" class="meta-diff-value">
          ({{ Number(question.difficulty).toFixed(2) }})
        </span>
        <el-tag
          v-for="(tag, tagIdx) in displayKnowledgeTags"
          :key="tag + '-' + tagIdx"
          size="mini"
          :type="variant === 'portal' ? 'info' : 'warning'"
          effect="plain"
          :class="['meta-tag', { 'meta-tag--portal': variant === 'portal' }]"
        >{{ tag }}</el-tag>
        <el-tag v-if="question.status !== '0'" size="mini" :type="statusTagType" class="meta-tag">{{ statusText }}</el-tag>
      </div>
      <button
        v-if="variant === 'portal'"
        type="button"
        class="card-quick-add"
        :class="{ disabled: question.status !== '0' }"
        title="加入试题篮"
        @click.stop="onAddBasket"
      >
        <i class="el-icon-shopping-cart-2" />
      </button>
    </div>

    <div
      class="card-body"
      :class="{
        'is-clickable': variant === 'portal',
        'is-collapsed': variant === 'portal' && !expanded
      }"
      @click="onDetailClick"
    >
      <div class="card-content">
        <span v-if="index != null && variant !== 'portal'" class="content-no">{{ index }}.</span>
        <qb-formula-text class="content-text" block :text="displayContent" :images="imageUrls" />
      </div>
      <div
        v-if="extraImageUrls.length"
        class="card-images"
        :class="{ 'card-images--compact': variant === 'portal' && !expanded }"
      >
        <el-image
          v-for="(url, i) in extraImageUrls"
          :key="i"
          :src="resolveImageUrl(url)"
          :preview-src-list="previewSrcList"
          fit="contain"
          class="card-image"
          @click.stop
        />
      </div>
      <ul v-if="optionItems.length && showFullBody" class="card-options" :class="{ 'card-options--grid': variant === 'portal' }">
        <li v-for="opt in optionItems" :key="opt.label" class="option-item">
          <span class="option-label">{{ opt.label }}.</span>
          <qb-formula-text class="option-text" :text="opt.text" />
        </li>
      </ul>
      <div v-if="variant === 'portal' && !expanded" class="card-expand-hint">
        <span>点击展开查看选项、答案与解析</span>
        <i class="el-icon-arrow-down" />
      </div>
    </div>

    <div v-if="variant === 'portal' && expanded" class="card-detail-panel" @click.stop>
      <div v-if="detailLoading" class="detail-loading">
        <i class="el-icon-loading" />
        <span>{{ detailLabels.loading }}</span>
      </div>
      <template v-else>
        <div v-if="answerText" class="detail-block">
          <span class="detail-label">{{ detailLabels.answer }}</span>
          <qb-formula-text class="detail-text" :text="answerText" />
        </div>
        <div v-if="analysisText" class="detail-block">
          <span class="detail-label">{{ detailLabels.analysis }}</span>
          <qb-formula-text class="detail-text" block :text="analysisText" />
        </div>
        <div v-if="!answerText && !analysisText" class="detail-empty">{{ detailLabels.empty }}</div>
      </template>
    </div>

    <div class="card-footer" :class="{ 'card-footer--portal': variant === 'portal' }">
      <div class="footer-left">
        <i v-if="variant === 'portal'" class="el-icon-reading footer-icon" />
        <span class="footer-subject">{{ question.subjectName || '-' }}</span>
        <span v-if="question.chapterText" class="footer-sep">·</span>
        <span v-if="question.chapterText" class="footer-chapter">{{ question.chapterText }}</span>
      </div>
      <div class="footer-right">
        <el-button
          v-if="variant === 'portal'"
          type="primary"
          size="mini"
          round
          icon="el-icon-shopping-cart-2"
          :disabled="question.status !== '0'"
          class="footer-basket-btn"
          @click.stop="onAddBasket"
        >加入试题篮</el-button>
        <el-button
          v-else
          type="primary"
          size="mini"
          plain
          icon="el-icon-shopping-cart-2"
          :disabled="question.status !== '0'"
          @click.stop="onAddBasket"
        >加入试题篮</el-button>
        <el-button type="text" size="mini" class="footer-link-btn" @click.stop="$emit('duplicate')" v-if="canManage">查重</el-button>
        <el-button type="text" size="mini" class="footer-link-btn" @click.stop="onDetailClick">{{ detailBtnLabel }}</el-button>
        <el-button
          v-if="variant === 'portal'"
          type="text"
          size="mini"
          @click.stop="$emit('open-page')"
        >独立页</el-button>
        <el-button
          v-if="canManage"
          v-hasPermi="['education:question:edit']"
          type="text"
          size="mini"
          @click.stop="$emit('edit')"
        >编辑</el-button>
        <el-button
          v-if="canManage"
          v-hasPermi="['education:question:remove']"
          type="text"
          size="mini"
          class="btn-danger"
          @click.stop="$emit('delete')"
        >删除</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { stripLeadingQuestionNo, isQuestionHtml } from '@/utils/questionContent'
import { getReferencedImageIndices } from '@/utils/questionFormula'
import { parseQuestionOption, shouldShowQuestionOptions } from '@/utils/questionOptions'
import { formatChoiceAnswer } from '@/utils/questionAnswer'
import { getQuestionTypeLabel } from '@/utils/questionTypes'

const STATUS_MAP = {
  '0': '已通过',
  '1': '待审核',
  '2': '已退回'
}

export default {
  name: 'QuestionCard',
  props: {
    question: { type: Object, required: true },
    selected: { type: Boolean, default: false },
    index: { type: Number, default: null },
    canManage: { type: Boolean, default: false },
    variant: { type: String, default: 'default' },
    expanded: { type: Boolean, default: false },
    detailLoading: { type: Boolean, default: false },
    detail: { type: Object, default: null }
  },
  data() {
    return {
      detailLabels: {
        loading: '\u52a0\u8f7d\u4e2d...',
        answer: '\u3010\u7b54\u6848\u3011',
        analysis: '\u3010\u89e3\u6790\u3011',
        empty: '\u6682\u65e0\u7b54\u6848\u6216\u89e3\u6790'
      }
    }
  },
  computed: {
    checked: {
      get() { return this.selected },
      set(val) { this.$emit('select', val) }
    },
    previewSrcList() {
      return this.imageUrls.map(u => this.resolveImageUrl(u))
    },
    typeLabel() {
      return getQuestionTypeLabel(this.question.questionType)
    },
    typeBadgeTone() {
      const t = String(this.question.questionType || '')
      if (['single', 'multi', 'judge'].includes(t)) return 'choice'
      if (['fill', 'knowledge_fill'].includes(t)) return 'blank'
      if (['answer', 'short', 'experiment', 'comprehensive', 'reading', 'drawing'].includes(t)) return 'subjective'
      return 'default'
    },
    difficultyLevel() {
      const d = Number(this.question.difficulty)
      if (Number.isNaN(d)) return 'medium'
      if (d <= 0.35) return 'easy'
      if (d >= 0.75) return 'hard'
      return 'medium'
    },
    difficultyShort() {
      const map = { easy: '容易', medium: '适中', hard: '较难' }
      return map[this.difficultyLevel] || '适中'
    },
    difficultyText() {
      const d = Number(this.question.difficulty)
      if (Number.isNaN(d)) return '-'
      return `${this.difficultyShort} (${d.toFixed(2)})`
    },
    showFullBody() {
      return this.variant !== 'portal' || this.expanded
    },
    statusText() {
      return STATUS_MAP[this.question.status] || this.question.status
    },
    statusTagType() {
      if (this.question.status === '1') return 'warning'
      if (this.question.status === '2') return 'danger'
      return 'info'
    },
    knowledgeTags() {
      return this.parseJsonArray(this.question.knowledgePoints).slice(0, 8)
    },
    displayKnowledgeTags() {
      const tags = this.knowledgeTags
      if (this.variant === 'portal') return tags.slice(0, 2)
      return tags
    },
    displayContent() {
      const raw = (this.variant === 'portal' && this.expanded && this.detail && this.detail.content)
        ? this.detail.content
        : this.question.content
      return stripLeadingQuestionNo(raw)
    },
    contentSource() {
      return (this.variant === 'portal' && this.expanded && this.detail) ? this.detail : this.question
    },
    imageUrls() {
      return this.parseJsonArray(this.contentSource.images)
    },
    extraImageUrls() {
      if (isQuestionHtml(this.displayContent)) {
        return []
      }
      const refs = getReferencedImageIndices(this.displayContent)
      return this.imageUrls.filter((_, i) => !refs.has(i))
    },
    optionItems() {
      const arr = this.parseJsonArray(this.contentSource.options)
      if (!arr.length) return []
      if (!shouldShowQuestionOptions(this.contentSource.questionType, arr)) return []
      return arr.map((text, i) => parseQuestionOption(text, i))
    },
    answerText() {
      if (!this.detail) return ''
      const raw = this.detail.correctAnswer
      if (raw == null || raw === '') return ''
      return formatChoiceAnswer(this.detail.questionType, raw)
    },
    analysisText() {
      return (this.detail && this.detail.analysis) || ''
    },
    detailBtnLabel() {
      if (this.variant !== 'portal') return '\u8be6\u60c5'
      return this.expanded ? '\u6536\u8d77' : '\u8be6\u60c5'
    }
  },
  methods: {
    onDetailClick() {
      this.$emit('detail')
    },
    onAddBasket() {
      if (this.question.status !== '0') return
      const el = this.$refs.cardRoot || this.$el
      this.$emit('add-basket', { el })
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
    resolveImageUrl(url) {
      if (!url) return ''
      if (/^https?:\/\//i.test(url)) return url
      return process.env.VUE_APP_BASE_API + url
    }
  }
}
</script>

<style scoped lang="scss">
.question-card {
  position: relative;
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
  transition: box-shadow 0.2s, border-color 0.2s;

  &:hover {
    box-shadow: 0 4px 16px rgba(34, 50, 77, 0.1);
    border-color: #d9e6f5;
  }

  &.is-selected {
    border-color: #2563EB;
    box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.2);
  }

  &.qb-card-fly-source-hidden {
    visibility: hidden;
    pointer-events: none;
  }

  &.qb-card-fly-out {
    opacity: 0;
    transform: scale(0.94) translateY(-10px);
    max-height: 0;
    margin-bottom: 0;
    padding-top: 0;
    padding-bottom: 0;
    border-width: 0;
    overflow: hidden;
    pointer-events: none;
    transition: opacity 0.32s ease, transform 0.32s ease, max-height 0.38s ease 0.12s, margin 0.38s ease 0.12s;
  }
}

.card-ribbon {
  position: absolute;
  top: 0;
  right: 16px;
  width: 28px;
  height: 36px;
  background: linear-gradient(180deg, #f56c6c 0%, #e64a4a 100%);
  color: #fff;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 6px;
  cursor: pointer;
  clip-path: polygon(0 0, 100% 0, 100% 100%, 50% 82%, 0 100%);
  z-index: 2;
  transition: transform 0.15s;

  &:hover:not(.disabled) {
    transform: scale(1.05);
  }

  &.disabled {
    background: #dcdfe6;
    cursor: not-allowed;
  }
}

.card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px 10px;
  background: #f7f9fc;
  border-bottom: 1px solid #eef2f6;
}

.card-check {
  flex-shrink: 0;
}

.card-meta {
  flex: 1;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
  font-size: 13px;
  color: #909399;
}

.meta-type {
  color: #606266;
  font-weight: 500;
}

.meta-type-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  background: #F1F5F9;
  color: #475569;

  &--choice {
    background: #EFF6FF;
    color: #2563EB;
  }

  &--blank {
    background: #F5F3FF;
    color: #7C3AED;
  }

  &--subjective {
    background: #ECFDF5;
    color: #059669;
  }
}

.meta-diff-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;

  &--easy {
    background: #ECFDF5;
    color: #059669;
  }

  &--medium {
    background: #EFF6FF;
    color: #2563EB;
  }

  &--hard {
    background: #FFF7ED;
    color: #D97706;
  }
}

.meta-sep {
  color: #dcdfe6;
}

.meta-diff {
  color: #64748b;
}

.card-index-badge {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  color: #2563EB;
  background: linear-gradient(135deg, #EFF6FF 0%, #EDE9FE 100%);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.12);
}

.meta-diff-value {
  font-size: 12px;
  color: #94a3b8;
}

.card-expand-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #E2E8F0;
  font-size: 12px;
  color: #94a3b8;
  transition: color 0.15s ease;

  i {
    font-size: 12px;
    transition: transform 0.15s ease;
  }
}

.question-card--portal:hover .card-expand-hint {
  color: #2563EB;

  i { transform: translateY(2px); }
}

.question-card:not(.question-card--portal) .card-top {
  padding-right: 48px;
}

.card-quick-add {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #2563EB;
  background: linear-gradient(135deg, #EFF6FF 0%, #EDE9FE 100%);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
  transition: transform 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;

  i { font-size: 16px; }

  &:hover:not(.disabled) {
    transform: scale(1.06);
    background: linear-gradient(135deg, #DBEAFE 0%, #DDD6FE 100%);
    box-shadow: 0 4px 14px rgba(124, 58, 237, 0.22);
  }

  &.disabled {
    cursor: not-allowed;
    color: #94a3b8;
    background: #F1F5F9;
    box-shadow: none;
  }
}

.meta-tag {
  margin-left: 0;
}

.card-body {
  padding: 16px 20px 12px;
  cursor: pointer;
}

.card-content {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  font-size: 15px;
  line-height: 1.75;
  color: #303133;
  word-break: break-word;
}

.content-no {
  flex-shrink: 0;
  font-weight: 600;
  margin-right: 0;
}

.content-text {
  flex: 1;
  min-width: 0;
}

.card-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
  justify-content: flex-start;
}

.card-image {
  display: block;
  max-width: 260px;
  max-height: 165px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;

  ::v-deep .el-image__inner {
    max-width: 260px;
    max-height: 165px;
    width: auto;
    height: auto;
    object-fit: contain;
    vertical-align: top;
  }
}

.card-options {
  list-style: none;
  margin: 14px 0 0;
  padding: 0;
}

.card-options--grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  line-height: 1.75;
  color: #606266;
  padding: 8px 12px;
  border-radius: 10px;
  background: #F8FAFC;
  border: 1px solid #EEF2F6;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.card-options--grid .option-item:hover {
  border-color: rgba(37, 99, 235, 0.18);
  background: #F8FAFF;
}

.option-label {
  flex-shrink: 0;
  font-weight: 600;
  margin-right: 0;
  line-height: 1.9;
}

.option-item ::v-deep .option-text {
  flex: 1;
  min-width: 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-top: 1px solid #eef2f6;
  background: #fafbfc;
  flex-wrap: wrap;
  gap: 8px;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 55%;
}

.footer-icon {
  flex-shrink: 0;
  color: #94a3b8;
  font-size: 14px;
}

.footer-subject {
  font-weight: 600;
  color: #64748b;
}

.footer-chapter {
  color: #94a3b8;
}

.footer-sep {
  margin: 0 4px;
  color: #cbd5e1;
}

.footer-link-btn {
  color: #64748b !important;
  &:hover { color: #2563EB !important; }
}

.footer-basket-btn {
  font-weight: 600;
  background: linear-gradient(135deg, #0F766E, #0E7490) !important;
  border-color: transparent !important;
  box-shadow: 0 2px 10px rgba(14, 116, 144, 0.24);

  &:hover:not(.is-disabled) {
    filter: brightness(1.05);
    box-shadow: 0 4px 14px rgba(14, 116, 144, 0.3);
  }
}

.footer-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
}

.btn-danger {
  color: #f56c6c;
}

.question-card--portal {
  border-radius: 16px;
  border-color: #E2E8F0;
  margin-bottom: 18px;
  overflow: hidden;
  transition: box-shadow 0.25s ease, transform 0.25s ease, border-color 0.25s ease;

  &:hover:not(.is-expanded) {
    box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
    border-color: rgba(124, 58, 237, 0.2);
    transform: translateY(-2px);
  }

  &.is-selected {
    border-color: rgba(37, 99, 235, 0.45);
    box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.12);
  }

  &.is-expanded {
    border: 2px solid #2563EB;
    box-shadow:
      0 0 0 4px rgba(37, 99, 235, 0.1),
      0 14px 36px rgba(37, 99, 235, 0.12);
    transform: none;
    position: relative;
    z-index: 1;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 4px;
      background: linear-gradient(180deg, #7C3AED 0%, #2563EB 50%, #3B82F6 100%);
      border-radius: 14px 0 0 14px;
      z-index: 1;
    }

    .card-top {
      background: linear-gradient(90deg, #EFF6FF 0%, #F5F3FF 100%);
      border-bottom-color: rgba(37, 99, 235, 0.12);
    }

    .card-footer--portal {
      background: linear-gradient(90deg, #F8FAFF 0%, #FAF5FF 100%);
      border-top-color: rgba(37, 99, 235, 0.12);
    }
  }

  .card-body.is-clickable {
    cursor: pointer;
  }

  .card-body.is-collapsed {
    .card-content {
      position: relative;
    }

    .card-content .content-text {
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

  }

  .card-images--compact {
    margin-top: 10px;

    .card-image {
      max-width: 120px;
      max-height: 80px;

      ::v-deep .el-image__inner {
        max-width: 120px;
        max-height: 80px;
      }
    }
  }

  .card-detail-panel {
    padding: 16px 18px 18px;
    background: linear-gradient(180deg, #F8FAFF 0%, #EFF6FF 100%);
    border-top: 1px dashed rgba(37, 99, 235, 0.15);
  }

  .detail-loading {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: #64748b;
    i { color: #2563EB; }
  }

  .detail-block {
    margin-bottom: 12px;
    line-height: 1.8;
    font-size: 14px;
    color: #1E293B;
    padding: 10px 12px;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.72);
    border: 1px solid rgba(37, 99, 235, 0.08);
    &:last-child { margin-bottom: 0; }
  }

  .detail-label {
    display: inline-block;
    font-weight: 700;
    color: #2563EB;
    margin-right: 6px;
    font-size: 12px;
    letter-spacing: 0.02em;
  }

  .detail-text {
    display: inline;
  }

  .detail-empty {
    font-size: 13px;
    color: #94a3b8;
    font-style: italic;
  }

  .card-top {
    background: #FFFFFF;
    border-bottom-color: #EEF2F6;
    padding-right: 14px;
  }

  .meta-tag--portal {
    background: #F8FAFC !important;
    border-color: #E2E8F0 !important;
    color: #64748B !important;
    border-radius: 999px !important;
  }

  .card-footer--portal {
    background: #FFFFFF;
    border-top-color: #EEF2F6;
    padding: 10px 16px;
  }

  .card-content {
    font-size: 15px;
    line-height: 1.85;
    color: #1E293B;
  }

  .card-footer .el-button--text {
    color: #64748b;
    &:hover { color: #2563EB; }
  }
}

@media (max-width: 640px) {
  .card-options--grid {
    grid-template-columns: 1fr;
  }

  .footer-left {
    max-width: 100%;
    white-space: normal;
  }

  .card-footer--portal {
    flex-direction: column;
    align-items: stretch;

    .footer-right {
      justify-content: flex-end;
    }
  }
}
</style>
