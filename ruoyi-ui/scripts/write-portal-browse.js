/**
 * Rewrite PortalBrowse.vue - workspace layout (ASCII-safe). Run: node scripts/write-portal-browse.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/portal/PortalBrowse.vue')

const content = `<template>
  <div class="portal-browse portal-page">
    <div class="portal-container browse-wrap">
      <div v-if="!token" class="login-hint portal-card">
        <i class="el-icon-info" />
        <span>{{ labels.loginHint }}</span>
        <el-button type="primary" size="small" @click="goLogin">{{ labels.loginBtn }}</el-button>
      </div>

      <div class="browse-workspace portal-card">
        <header class="ws-head">
          <div class="ws-head-main">
            <span class="ws-mode-icon" :class="'mode-' + mode">
              <i :class="modeIcon" />
            </span>
            <p class="ws-desc">{{ modeDesc }}</p>
          </div>
          <div class="ws-head-actions">
            <button type="button" class="ws-smart-btn" @click="applySmartPreset">
              <i class="el-icon-magic-stick" />
              <span>{{ labels.smartAction }}</span>
            </button>
            <button
              v-if="mode !== 'knowledge'"
              type="button"
              class="ws-toggle-btn"
              @click="catalogOpen = !catalogOpen"
            >
              <i :class="catalogOpen ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" />
              {{ catalogOpen ? labels.catalogCollapse : labels.catalogExpand }}
            </button>
          </div>
        </header>

        <div v-show="catalogOpen && mode !== 'knowledge'" class="ws-catalog">
          <stage-selector-bar v-model="catalog.schoolStage" @change="onStageChange" />
          <textbook-selector-bar
            v-if="mode === 'chapter'"
            class="textbook-bar"
            :subject-id="queryParams.subjectId"
            :school-stage="catalog.schoolStage"
            :version-id.sync="catalog.versionId"
            :textbook-id.sync="catalog.textbookId"
            @change="onCatalogChange"
          />
        </div>

        <div class="ws-body">
          <aside class="ws-rail">
            <chapter-sidebar
              v-if="mode === 'chapter'"
              variant="portal"
              :subject-id="queryParams.subjectId"
              :textbook-id="catalog.textbookId"
              :chapter-id="queryParams.chapterId"
              @chapter-change="onChapterChange"
            />
            <div v-else-if="mode === 'knowledge'" class="rail-panel">
              <div class="rail-head">{{ labels.knowledge }}</div>
              <div class="rail-search">
                <el-input
                  v-model="tagKeyword"
                  size="small"
                  clearable
                  :placeholder="labels.searchKnowledgePh"
                  prefix-icon="el-icon-search"
                  @keyup.enter.native="loadKnowledgeTags"
                  @clear="loadKnowledgeTags"
                />
              </div>
              <div v-loading="tagLoading" class="rail-list">
                <div
                  class="rail-item"
                  :class="{ active: !queryParams.knowledgePoint }"
                  @click="pickKnowledge('')"
                >{{ labels.all }}</div>
                <div
                  v-for="tag in knowledgeTags"
                  :key="tag.tagId || tag.tagName"
                  class="rail-item"
                  :class="{ active: queryParams.knowledgePoint === tag.tagName }"
                  @click="pickKnowledge(tag.tagName)"
                >{{ tag.tagName }}</div>
                <el-empty v-if="!tagLoading && !knowledgeTags.length" :image-size="48" :description="labels.noKnowledge" />
              </div>
            </div>
            <div v-else-if="mode === 'exam'" class="rail-panel">
              <div class="rail-head">{{ labels.examSideTitle }}</div>
              <div class="rail-list">
                <div
                  v-for="cat in examCategories"
                  :key="cat.id"
                  class="rail-item rail-item--icon"
                  :class="{ active: activeExamCat === cat.id }"
                  @click="pickExamCategory(cat)"
                >
                  <i :class="cat.icon" />
                  <span>{{ cat.label }}</span>
                </div>
              </div>
            </div>
          </aside>

          <main class="ws-main">
            <div class="ws-toolbar">
              <div class="ws-filter-zone">
                <question-filter-bar
                  class="ws-filter-bar"
                  :question-type="queryParams.questionType"
                  :difficulty-min="queryParams.difficultyMin"
                  :difficulty-max="queryParams.difficultyMax"
                  :question-type-options="questionTypeOptions"
                  @change="onFilterChange"
                />
              </div>

              <div class="ws-result-bar">
                <div class="ws-sort">
                  <button
                    type="button"
                    class="ws-sort-btn"
                    :class="{ active: sortBy === 'default' }"
                    @click="setSortBy('default')"
                  >{{ labels.sortDefault }}</button>
                  <button
                    type="button"
                    class="ws-sort-btn"
                    :class="{ active: sortBy === 'latest' }"
                    @click="setSortBy('latest')"
                  >{{ labels.sortLatest }}</button>
                </div>
                <span class="ws-result-divider" aria-hidden="true" />
                <div class="ws-count">{{ labels.resultPrefix }} <b>{{ total }}</b> {{ labels.resultSuffix }}</div>
                <template v-if="token">
                  <el-button
                    v-if="questionBasketCount"
                    type="primary"
                    size="small"
                    class="ws-paper-btn"
                    @click="goPaper"
                  >{{ labels.oneKeyPaper }}</el-button>
                  <el-button v-else size="small" class="ws-paper-btn" @click="goPickFirst">{{ labels.goPickFirst }}</el-button>
                </template>
              </div>
            </div>

            <div class="ws-list-area">
              <div v-loading="loading" class="ws-list">
                <transition-group name="q-fade" tag="div">
                  <question-card
                    v-for="(item, idx) in questionList"
                    :key="item.questionId"
                    variant="portal"
                    :question="item"
                    :index="cardIndex(idx)"
                    :can-manage="false"
                    :expanded="expandedQuestionId === item.questionId"
                    :detail="questionDetailMap[item.questionId]"
                    :detail-loading="detailLoadingId === item.questionId"
                    @add-basket="payload => handleAddToBasket(item, payload && payload.el)"
                    @detail="handleViewDetail(item)"
                  />
                </transition-group>
                <el-empty
                  v-if="!loading && !questionList.length"
                  :description="token ? labels.noQuestion : labels.loginToView"
                />
              </div>

              <pagination
                v-show="total > 0"
                class="ws-pager"
                :total="total"
                :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize"
                @pagination="getList"
              />
            </div>
          </main>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import portalBrowseMixin from './mixins/portalBrowseMixin'
import { browseLabels } from './portal-labels'
import { listKnowledgeTags } from '@/api/education/question'
import ChapterSidebar from '@/views/education/question-bank/components/ChapterSidebar'
import QuestionFilterBar from '@/views/education/question-bank/components/QuestionFilterBar'
import QuestionCard from '@/views/education/question-bank/components/QuestionCard'
import StageSelectorBar from '@/views/education/question-bank/components/StageSelectorBar'
import TextbookSelectorBar from '@/views/education/question-bank/components/TextbookSelectorBar'
import { goPortalLogin } from '@/utils/portalLogin'

export default {
  name: 'PortalBrowse',
  components: {
    ChapterSidebar,
    QuestionFilterBar,
    QuestionCard,
    StageSelectorBar,
    TextbookSelectorBar
  },
  mixins: [portalBrowseMixin],
  props: {
    mode: {
      type: String,
      default: 'chapter'
    }
  },
  data() {
    return {
      labels: browseLabels,
      catalogOpen: true,
      activeExamCat: 'all',
      examCategories: [
        { id: 'all', label: '\u5168\u90e8\u8bd5\u5377', keyword: '', icon: 'el-icon-files' },
        { id: 'gaokao', label: '\u9ad8\u8003\u771f\u9898', keyword: '\u9ad8\u8003', icon: 'el-icon-medal' },
        { id: 'mock', label: '\u6a21\u62df\u8bd5\u5377', keyword: '\u6a21\u62df', icon: 'el-icon-document' },
        { id: 'unit', label: '\u5355\u5143\u6d4b\u9a8c', keyword: '\u5355\u5143', icon: 'el-icon-edit-outline' },
        { id: 'mid', label: '\u671f\u4e2d\u8003\u8bd5', keyword: '\u671f\u4e2d', icon: 'el-icon-date' },
        { id: 'final', label: '\u671f\u672b\u8003\u8bd5', keyword: '\u671f\u672b', icon: 'el-icon-tickets' },
        { id: 'monthly', label: '\u6708\u8003\u8bd5\u5377', keyword: '\u6708\u8003', icon: 'el-icon-notebook-1' }
      ],
      tagKeyword: '',
      tagLoading: false,
      knowledgeTags: []
    }
  },
  computed: {
    ...mapGetters(['token', 'questionBasketCount']),
    modeLabel() {
      if (this.mode === 'knowledge') return browseLabels.knowledgePick
      if (this.mode === 'exam') return browseLabels.examPick
      return browseLabels.chapterPick
    },
    modeDesc() {
      if (this.mode === 'knowledge') return browseLabels.knowledgeDesc
      if (this.mode === 'exam') return browseLabels.examDesc
      return browseLabels.chapterDesc
    },
    modeIcon() {
      if (this.mode === 'knowledge') return 'el-icon-price-tag'
      if (this.mode === 'exam') return 'el-icon-document'
      return 'el-icon-folder-opened'
    }
  },
  watch: {
    'queryParams.subjectId'() {
      if (this.mode === 'knowledge') this.loadKnowledgeTags()
    },
    token(val) {
      if (val) this.getList()
    }
  },
  mounted() {
    if (this.mode === 'knowledge') this.loadKnowledgeTags()
    if (this.mode === 'exam') this.syncExamFromQuery()
  },
  methods: {
    goLogin() {
      goPortalLogin(this.$router, this.$route.fullPath)
    },
    goPaper() {
      this.$router.push('/portal/paper')
    },
    goPickFirst() {
      const path = this.mode === 'exam' ? '/portal/exam' : '/portal/chapter'
      this.$router.push(path)
    },
    applySmartPreset() {
      this.onFilterChange({ field: 'difficulty', value: { min: 0.36, max: 0.74 } })
      this.$message.success(this.labels.smartAction)
    },
    syncExamFromQuery() {
      const kw = (this.$route.query.keyword || '').trim()
      if (!kw) return
      const hit = this.examCategories.find(c => c.keyword === kw)
      if (hit) {
        this.activeExamCat = hit.id
        this.queryParams.keyword = hit.keyword
      }
    },
    pickExamCategory(cat) {
      this.activeExamCat = cat.id
      this.queryParams.keyword = cat.keyword || undefined
      this.queryParams.chapterId = undefined
      this.queryParams.knowledgePoint = undefined
      this.handleQuery()
    },
    loadKnowledgeTags() {
      if (!this.queryParams.subjectId || !this.token) return
      this.tagLoading = true
      listKnowledgeTags({
        subjectId: this.queryParams.subjectId,
        keyword: this.tagKeyword || undefined
      }).then(res => {
        this.knowledgeTags = res.data || []
      }).finally(() => { this.tagLoading = false })
    },
    pickKnowledge(tag) {
      this.queryParams.knowledgePoint = tag || undefined
      this.handleQuery()
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #2563EB;
$ink: #1E293B;
$ink-muted: #64748B;
$border: #E2E8F0;
$rail-bg: #F8FAFC;
$rail-w: 272px;

.browse-wrap {
  padding: 16px 20px 40px;
}

.login-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #475569;
  border: 1px solid $border;
  i { color: $primary; font-size: 18px; }
  .el-button { margin-left: auto; }
}

/* ---- workspace shell ---- */
.browse-workspace {
  overflow: hidden;
  border-radius: 14px;
  border-color: $border;
  box-shadow: none !important;
  &:hover {
    box-shadow: none !important;
    transform: none;
  }
}

.ws-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #EEF2F6;
  background: #fff;
}

.ws-head-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.ws-mode-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: $primary;
  background: #EFF6FF;
  border: 1px solid rgba(37, 99, 235, 0.12);
  &.mode-exam { color: #2563EB; border-color: rgba(212, 175, 55, 0.3); }
}

.ws-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.ws-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.ws-smart-btn,
.ws-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border: 1px solid $border;
  border-radius: 8px;
  background: #fff;
  font-size: 12px;
  color: $ink-muted;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
  &:hover {
    color: $primary;
    border-color: rgba(37, 99, 235, 0.3);
    background: #EFF6FF;
  }
}

.ws-catalog {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #EEF2F6;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.textbook-bar { width: 100%; }

.ws-body {
  display: flex;
  align-items: stretch;
  min-height: 480px;
  background: #fff;
}

.ws-rail {
  width: $rail-w;
  flex-shrink: 0;
  padding: 16px;
  background: #fff;
  border-right: 1px solid #EEF2F6;
  overflow: auto;
  max-height: calc(100vh - 180px);
  position: sticky;
  top: 88px;
  align-self: flex-start;
}

.rail-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid #EEF2F6;
  border-radius: 8px;
}

.rail-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: $ink;
  margin: 0;
  padding: 10px 12px;
  background: #F8FAFC;
  border: none;
  border-bottom: 1px solid #EEF2F6;
  border-radius: 0;
  box-shadow: none;
  &::before {
    content: '';
    width: 3px;
    height: 14px;
    border-radius: 2px;
    background: #2563EB;
    opacity: 0.4;
    flex-shrink: 0;
  }
}

.rail-search {
  padding: 10px 12px;
  background: transparent;
  border: none;
  border-bottom: 1px solid #EEF2F6;
  border-radius: 0;
  box-shadow: none;
}

.rail-list {
  margin-top: 0;
  padding: 6px 6px 10px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
  background: transparent;
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.rail-item {
  padding: 8px 10px;
  font-size: 13px;
  color: #475569;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: background 0.12s;
  &:hover { background: #EFF6FF; color: $ink-muted; }
  &.active {
    background: #EFF6FF;
    color: $ink;
    font-weight: 600;
    border-left: 3px solid $primary;
    padding-left: 7px;
  }
  &--icon {
    display: flex;
    align-items: center;
    gap: 8px;
    i { font-size: 15px; color: #94a3b8; width: 16px; text-align: center; }
    &.active i { color: $ink-muted; }
  }
}

.ws-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.ws-toolbar {
  flex-shrink: 0;
  background: #fff;
  position: sticky;
  top: 88px;
  z-index: 3;
  border-bottom: 1px solid #EEF2F6;
}

.ws-filter-zone {
  padding: 12px 16px 8px;
}

.ws-filter-bar {
  border: none !important;
  margin: 0 !important;
  padding: 0 !important;
  border-radius: 0 !important;
  background: transparent !important;
}

.ws-result-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 8px 16px 12px;
  background: #fff;
}

.ws-result-divider {
  width: 1px;
  height: 16px;
  background: #E2E8F0;
  flex-shrink: 0;
}

.ws-sort {
  display: flex;
  gap: 4px;
  padding: 2px;
  border-radius: 8px;
  background: #F1F5F9;
}

.ws-sort-btn {
  border: none;
  background: transparent;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  &.active {
    background: #fff;
    color: $primary;
    font-weight: 600;
    border: 1px solid rgba(37, 99, 235, 0.15);
  }
}

.ws-count {
  flex: 1;
  font-size: 13px;
  color: #94a3b8;
  b { color: $ink-muted; font-size: 15px; font-weight: 700; }
}

.ws-paper-btn { flex-shrink: 0; }

.ws-list-area {
  flex: 1;
  padding: 16px;
  min-height: 240px;
  background: #F8FAFC;
  border-top: 1px solid #EEF2F6;
}

.ws-list {
  min-height: 200px;
}

.ws-pager {
  padding: 12px 0 0;
}

/* ---- unified pill filters ---- */
.ws-catalog ::v-deep .textbook-selector .tag-item,
.ws-catalog ::v-deep .stage-selector .pill-item,
.portal-browse ::v-deep .ws-filter-bar .filter-tag {
  border: 1px solid transparent;
  border-radius: 16px;
  transition: background 0.12s, color 0.12s, border-color 0.12s;
}

.ws-catalog ::v-deep .textbook-selector .tag-item:hover,
.ws-catalog ::v-deep .stage-selector .pill-item:hover,
.portal-browse ::v-deep .ws-filter-bar .filter-tag:hover {
  color: $primary;
  background: #EFF6FF;
}

.ws-catalog ::v-deep .textbook-selector .tag-item.active,
.ws-catalog ::v-deep .stage-selector .pill-item.active,
.portal-browse ::v-deep .ws-filter-bar .filter-tag.active {
  background: #EFF6FF;
  color: $primary;
  border-color: rgba(37, 99, 235, 0.2);
  font-weight: 600;
}

.portal-browse ::v-deep .question-card--portal {
  border-radius: 12px;
  border-color: #EEF2F6;
  margin-bottom: 12px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.portal-browse ::v-deep .question-card--portal.is-expanded {
  margin-bottom: 16px;
}

.q-fade-enter-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.q-fade-enter { opacity: 0; transform: translateY(8px); }

@media (max-width: 992px) {
  .ws-head { flex-direction: column; align-items: flex-start; }
  .ws-head-actions { width: 100%; justify-content: flex-start; }
  .ws-body { flex-direction: column; min-height: 0; }
  .ws-toolbar { position: static; }
  .ws-rail {
    width: 100%;
    max-height: 280px;
    position: static;
    border-right: none;
    border-bottom: 1px solid #EEF2F6;
  }
  .ws-paper-btn { margin-left: 0; }
}
</style>
`

fs.writeFileSync(file, content, 'utf8')
console.log('wrote PortalBrowse.vue (workspace layout)')
