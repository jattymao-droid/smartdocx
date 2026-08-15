<template>
  <div class="portal-browse portal-page">
    <div class="portal-container browse-wrap">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="sep">/</span>
        <span class="current">{{ modeLabel }}</span>
      </nav>

      <div v-if="!token" class="login-hint portal-card portal-login-hint">
        <i class="el-icon-info" />
        <span>{{ labels.loginHint }}</span>
        <el-button type="primary" size="small" @click="goLogin">{{ labels.loginBtn }}</el-button>
      </div>

      <div class="browse-workspace portal-card portal-card-accent" :class="'browse-workspace--' + mode">
        <header class="ws-head">
          <div class="ws-head-main">
            <span class="ws-mode-icon" :class="'mode-' + mode">
              <i :class="modeIcon" />
            </span>
            <div class="ws-head-text">
              <h2 class="ws-title">{{ modeLabel }}</h2>
              <p class="ws-desc">{{ modeDesc }}</p>
            </div>
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

        <div class="ws-catalog">
          <subject-tag-bar
            v-model="queryParams.subjectId"
            :options="subjectOptions"
            @change="onSubjectChange"
          />
          <template v-if="catalogOpen && mode !== 'knowledge'">
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
          </template>
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
                  @open-page="openQuestionPage(item)"
                />
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
import SubjectTagBar from '@/views/education/question-bank/components/SubjectTagBar'
import TextbookSelectorBar from '@/views/education/question-bank/components/TextbookSelectorBar'
import { goPortalLogin } from '@/utils/portalLogin'

export default {
  name: 'PortalBrowse',
  components: {
    ChapterSidebar,
    QuestionFilterBar,
    QuestionCard,
    StageSelectorBar,
    SubjectTagBar,
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
        { id: 'all', label: '全部试卷', keyword: '', icon: 'el-icon-files' },
        { id: 'gaokao', label: '高考真题', keyword: '高考', icon: 'el-icon-medal' },
        { id: 'mock', label: '模拟试卷', keyword: '模拟', icon: 'el-icon-document' },
        { id: 'unit', label: '单元测验', keyword: '单元', icon: 'el-icon-edit-outline' },
        { id: 'mid', label: '期中考试', keyword: '期中', icon: 'el-icon-date' },
        { id: 'final', label: '期末考试', keyword: '期末', icon: 'el-icon-tickets' },
        { id: 'monthly', label: '月考试卷', keyword: '月考', icon: 'el-icon-notebook-1' }
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
      this.$router.push('/paper')
    },
    goPickFirst() {
      const path = this.mode === 'exam' ? '/exam' : '/chapter'
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
$primary-light: #3B82F6;
$primary-dark: #1D4ED8;
$ink: #0F172A;
$ink-muted: #64748B;
$border: #E2E8F0;
$rail-w: 280px;

.browse-wrap {
  padding: 0 0 48px;
}

.login-hint {
  i { color: $primary; font-size: 20px; }
  .el-button { margin-left: auto; }
}

.browse-workspace {
  overflow: hidden;
  border-radius: 16px;
  background: #fff;

  &--chapter::after { background: linear-gradient(90deg, $primary, #60A5FA); }
  &--knowledge::after { background: linear-gradient(90deg, #7C3AED, #A78BFA); }
  &--exam::after { background: linear-gradient(90deg, #D97706, #FBBF24); }

  &--knowledge .rail-head::before { background: linear-gradient(180deg, #A78BFA, #7C3AED); }
  &--exam .rail-head::before { background: linear-gradient(180deg, #FBBF24, #D97706); }

  &--knowledge .rail-item.active {
    background: linear-gradient(90deg, #FAF5FF, rgba(237, 233, 254, 0.5));
    box-shadow: inset 3px 0 0 #7C3AED;
    &.active i, &--icon.active i { color: #7C3AED; }
  }

  &--exam .rail-item.active {
    background: linear-gradient(90deg, #FFFBEB, rgba(254, 243, 199, 0.5));
    box-shadow: inset 3px 0 0 #D97706;
    &.active i, &--icon.active i { color: #D97706; }
  }
}

.ws-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  border-bottom: 1px solid #EEF2F6;
  background: linear-gradient(135deg, #FAFBFC 0%, #F8FAFF 50%, #fff 100%);
}

.ws-head-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1;
}

.ws-head-text {
  min-width: 0;
}

.ws-title {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 700;
  color: $ink;
  letter-spacing: -0.01em;
  line-height: 1.3;
}

.ws-mode-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: $primary;
  background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
  border: 1px solid rgba(37, 99, 235, 0.15);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.1);

  &.mode-knowledge {
    color: #7C3AED;
    background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
    border-color: rgba(124, 58, 237, 0.15);
    box-shadow: 0 2px 8px rgba(124, 58, 237, 0.08);
  }

  &.mode-exam {
    color: #B45309;
    background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%);
    border-color: rgba(180, 83, 9, 0.15);
    box-shadow: 0 2px 8px rgba(180, 83, 9, 0.08);
  }
}

.ws-desc {
  margin: 0;
  font-size: 13px;
  color: $ink-muted;
  line-height: 1.55;
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
  padding: 8px 14px;
  border: 1px solid $border;
  border-radius: 10px;
  background: #fff;
  font-size: 12px;
  font-weight: 500;
  color: $ink-muted;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s, border-color 0.15s, color 0.15s, box-shadow 0.15s;

  &:hover {
    color: $primary;
    border-color: rgba(37, 99, 235, 0.3);
    background: #EFF6FF;
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
  }
}

.ws-smart-btn {
  background: linear-gradient(135deg, #CCFBF1 0%, #CFFAFE 100%);
  border-color: rgba(14, 116, 144, 0.24);
  color: #0E7490;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(14, 116, 144, 0.1);

  &:hover {
    color: #0C4A6E;
    border-color: rgba(14, 116, 144, 0.4);
    background: linear-gradient(135deg, #ECFEFF, #CCFBF1);
    box-shadow: 0 4px 12px rgba(14, 116, 144, 0.15);
  }

  i { color: inherit; }
}

.ws-catalog {
  padding: 14px 20px;
  background: #FAFBFC;
  border-bottom: 1px solid #EEF2F6;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.textbook-bar { width: 100%; }

.ws-body {
  display: flex;
  align-items: stretch;
  min-height: calc(100vh - 260px);
  background: #fff;
}

.ws-rail {
  width: $rail-w;
  flex-shrink: 0;
  padding: 18px 16px;
  background: #FAFBFC;
  border-right: 1px solid #EEF2F6;
  overflow: hidden;
  max-height: calc(100vh - 120px);
  min-height: calc(100vh - 120px);
  position: static;
  align-self: flex-start;
  display: flex;
  flex-direction: column;

  > .chapter-sidebar {
    flex: 1;
    min-height: 0;
  }
}

.rail-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid #EEF2F6;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.rail-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: $ink;
  margin: 0;
  padding: 12px 14px;
  background: linear-gradient(180deg, #F8FAFC 0%, #fff 100%);
  border-bottom: 1px solid #EEF2F6;

  &::before {
    content: '';
    width: 3px;
    height: 14px;
    border-radius: 2px;
    background: linear-gradient(180deg, #3B82F6, $primary);
    flex-shrink: 0;
  }
}

.rail-search {
  padding: 10px 12px;
  border-bottom: 1px solid #EEF2F6;

  ::v-deep .el-input__inner {
    border-radius: 8px;
    border-color: #E2E8F0;
    &:focus { border-color: $primary; }
  }
}

.rail-list {
  padding: 8px;
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.rail-item {
  padding: 9px 12px;
  font-size: 13px;
  color: #475569;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: background 0.12s, color 0.12s;

  &:hover { background: #EFF6FF; color: $ink-muted; }

  &.active {
    background: linear-gradient(90deg, #EFF6FF 0%, rgba(239, 246, 255, 0.5) 100%);
    color: $ink;
    font-weight: 600;
    box-shadow: inset 3px 0 0 $primary;
    padding-left: 9px;
  }

  &--icon {
    display: flex;
    align-items: center;
    gap: 10px;
    i { font-size: 15px; color: #94a3b8; width: 18px; text-align: center; }
    &.active i { color: $primary; }
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
  position: static;
  z-index: 3;
  border-bottom: 1px solid #EEF2F6;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
}

.ws-filter-zone {
  padding: 14px 20px 10px;
  background: linear-gradient(180deg, #FAFBFC 0%, #F8FAFF 100%);
  border-bottom: 1px solid #F1F5F9;
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
  padding: 10px 20px 14px;
  background: #fff;
}

.ws-result-divider {
  width: 1px;
  height: 18px;
  background: #E2E8F0;
  flex-shrink: 0;
}

.ws-sort {
  display: flex;
  gap: 2px;
  padding: 3px;
  border-radius: 10px;
  background: #F1F5F9;
  border: 1px solid #EEF2F6;
}

.ws-sort-btn {
  border: none;
  background: transparent;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, box-shadow 0.15s;

  &.active {
    background: linear-gradient(135deg, #fff, #EFF6FF);
    color: $primary;
    font-weight: 600;
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
  }
}

.ws-count {
  flex: 1;
  font-size: 13px;
  color: #94a3b8;

  b {
    font-size: 17px;
    font-weight: 800;
    margin: 0 3px;
    background: linear-gradient(135deg, $primary, #7C3AED);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.ws-paper-btn {
  flex-shrink: 0;
  font-weight: 600;
  border-radius: 10px !important;
  background: linear-gradient(135deg, #0F766E, #0E7490) !important;
  border-color: transparent !important;
  box-shadow: 0 4px 12px rgba(14, 116, 144, 0.24);

  &:hover {
    background: linear-gradient(135deg, #0D9488, #0891B2) !important;
    box-shadow: 0 6px 16px rgba(14, 116, 144, 0.3);
  }
}

.ws-list-area {
  flex: 1;
  padding: 20px 24px 28px;
  min-height: 280px;
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%, rgba(124, 58, 237, 0.06), transparent 55%),
    linear-gradient(180deg, #F8FAFF 0%, #F1F5F9 45%, #EEF2FF 100%);
}

.ws-list {
  min-height: 200px;
  max-width: 960px;
  margin: 0 auto;
}

.ws-pager {
  padding: 16px 0 4px;

  ::v-deep .el-pagination {
    font-weight: 500;
  }
}

.ws-catalog ::v-deep .textbook-selector .tag-item,
.ws-catalog ::v-deep .subject-tag-bar .tag-item,
.ws-catalog ::v-deep .stage-selector .pill-item,
.portal-browse ::v-deep .ws-filter-bar .filter-tag {
  border: 1px solid transparent;
  border-radius: 20px;
  font-weight: 500;
  transition: background 0.12s, color 0.12s, border-color 0.12s;
}

.ws-catalog ::v-deep .textbook-selector .tag-item:hover,
.ws-catalog ::v-deep .subject-tag-bar .tag-item:hover,
.ws-catalog ::v-deep .stage-selector .pill-item:hover,
.portal-browse ::v-deep .ws-filter-bar .filter-tag:hover {
  color: $primary;
  background: #EFF6FF;
}

.ws-catalog ::v-deep .textbook-selector .tag-item.active,
.ws-catalog ::v-deep .subject-tag-bar .tag-item.active,
.ws-catalog ::v-deep .stage-selector .pill-item.active,
.portal-browse ::v-deep .ws-filter-bar .filter-tag.active {
  background: #EFF6FF;
  color: $primary-dark;
  border-color: rgba(37, 99, 235, 0.25);
  font-weight: 600;
}

.portal-browse ::v-deep .question-card--portal {
  border-radius: 16px;
  border-color: #E2E8F0;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #fff 0%, #FAFBFF 100%);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  transition: border-color 0.22s ease, box-shadow 0.22s ease, transform 0.22s ease;

  &:hover:not(.is-expanded) {
    border-color: rgba(124, 58, 237, 0.22);
    box-shadow: 0 12px 32px rgba(124, 58, 237, 0.1);
    transform: translateY(-2px);
  }
}

.portal-browse ::v-deep .question-card--portal.is-expanded {
  margin-bottom: 20px;
  transform: none;
}

@media (max-width: 992px) {
  .ws-head { flex-direction: column; align-items: flex-start; }
  .ws-head-actions { width: 100%; justify-content: flex-start; }
  .ws-body { flex-direction: column; min-height: 0; }
  .ws-toolbar { position: static; }
  .ws-rail {
    width: 100%;
    max-height: 360px;
    min-height: 280px;
    position: static;
    border-right: none;
    border-bottom: 1px solid #EEF2F6;
  }
}
</style>
