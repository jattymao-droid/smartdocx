const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')

function write(rel, content) {
  const file = path.join(src, rel)
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, content, 'utf8')
  console.log('wrote', rel)
}

write('views/portal/PortalBrowse.vue', `<template>
  <div class="portal-browse portal-page">
    <div class="portal-container browse-wrap">
      <div class="breadcrumb">
        <router-link to="/portal/home">\u9996\u9875</router-link>
        <span class="sep">></span>
        <span>{{ modeLabel }}</span>
      </div>

      <div v-if="!token" class="login-hint portal-card">
        <i class="el-icon-info" />
        <span>\u6d4f\u89c8\u8bd5\u9898\u9700\u5148\u767b\u5f55\u3002\u767b\u5f55\u540e\u53ef\u7b5b\u9009\u3001\u52a0\u7bee\u3001\u7ec4\u5377\u3002</span>
        <el-button type="primary" size="small" @click="goLogin">\u7acb\u5373\u767b\u5f55</el-button>
      </div>

      <div class="catalog-bar portal-card">
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

      <div class="browse-layout">
        <aside class="browse-side portal-card">
          <chapter-sidebar
            v-if="mode === 'chapter'"
            :subject-id="queryParams.subjectId"
            :textbook-id="catalog.textbookId"
            :chapter-id="queryParams.chapterId"
            @chapter-change="onChapterChange"
          />
          <div v-else class="knowledge-side">
            <div class="side-title">\u77e5\u8bc6\u70b9</div>
            <el-input
              v-model="tagKeyword"
              size="small"
              clearable
              placeholder="\u641c\u7d22\u77e5\u8bc6\u70b9"
              prefix-icon="el-icon-search"
              @keyup.enter.native="loadKnowledgeTags"
              @clear="loadKnowledgeTags"
            />
            <div v-loading="tagLoading" class="tag-list">
              <div
                class="tag-item"
                :class="{ active: !queryParams.knowledgePoint }"
                @click="pickKnowledge('')"
              >\u5168\u90e8</div>
              <div
                v-for="tag in knowledgeTags"
                :key="tag"
                class="tag-item"
                :class="{ active: queryParams.knowledgePoint === tag }"
                @click="pickKnowledge(tag)"
              >{{ tag }}</div>
              <el-empty v-if="!tagLoading && !knowledgeTags.length" :image-size="48" description="\u6682\u65e0\u77e5\u8bc6\u70b9" />
            </div>
          </div>
        </aside>

        <section class="browse-main">
          <div class="filter-tabs portal-card">
            <span class="tab active">\u8bd5\u9898</span>
            <span class="tab disabled">\u540c\u6b65\u8bd5\u5377</span>
          </div>

          <question-filter-bar
            class="portal-card filter-panel"
            :question-type="queryParams.questionType"
            :difficulty-min="queryParams.difficultyMin"
            :difficulty-max="queryParams.difficultyMax"
            :question-type-options="questionTypeOptions"
            @change="onFilterChange"
          />

          <div class="result-bar portal-card">
            <div class="result-sort">
              <span class="sort-item" :class="{ active: sortBy === 'default' }" @click="setSortBy('default')">\u7efc\u5408</span>
              <span class="sort-item" :class="{ active: sortBy === 'latest' }" @click="setSortBy('latest')">\u6700\u65b0</span>
            </div>
            <el-input
              v-model="queryParams.keyword"
              clearable
              size="small"
              class="result-search"
              placeholder="\u5728\u7ed3\u679c\u4e2d\u641c\u7d22\u9898\u5e72"
              prefix-icon="el-icon-search"
              @keyup.enter.native="handleQuery"
              @clear="handleQuery"
            />
            <div class="result-count">\u5171\u8ba1 <b>{{ total }}</b> \u9053\u8bd5\u9898</div>
            <el-button
              v-if="token"
              type="primary"
              size="small"
              class="paper-btn"
              :disabled="!questionBasketCount"
              @click="goPaper"
            >\u4e00\u952e\u7ec4\u5377</el-button>
          </div>

          <div v-loading="loading" class="question-list">
            <question-card
              v-for="(item, idx) in questionList"
              :key="item.questionId"
              :question="item"
              :index="cardIndex(idx)"
              :can-manage="false"
              @add-basket="payload => handleAddToBasket(item, payload && payload.el)"
              @detail="handleViewDetail(item)"
            />
            <el-empty
              v-if="!loading && !questionList.length"
              :description="token ? '\u6682\u65e0\u8bd5\u9898' : '\u8bf7\u767b\u5f55\u540e\u67e5\u770b\u8bd5\u9898'"
            />
          </div>

          <pagination
            v-show="total > 0"
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="getList"
          />
        </section>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import portalBrowseMixin from './mixins/portalBrowseMixin'
import { listKnowledgeTags } from '@/api/education/question'
import ChapterSidebar from '@/views/education/question-bank/components/ChapterSidebar'
import QuestionFilterBar from '@/views/education/question-bank/components/QuestionFilterBar'
import QuestionCard from '@/views/education/question-bank/components/QuestionCard'
import StageSelectorBar from '@/views/education/question-bank/components/StageSelectorBar'
import TextbookSelectorBar from '@/views/education/question-bank/components/TextbookSelectorBar'

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
      tagKeyword: '',
      tagLoading: false,
      knowledgeTags: []
    }
  },
  computed: {
    ...mapGetters(['token', 'questionBasketCount']),
    modeLabel() {
      return this.mode === 'knowledge' ? '\u77e5\u8bc6\u70b9\u9009\u9898' : '\u7ae0\u8282\u9009\u9898'
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
  },
  methods: {
    goLogin() {
      this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
    },
    goPaper() {
      this.$router.push('/portal/paper')
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
.browse-wrap {
  padding: 12px 16px 32px;
}

.breadcrumb {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
  a { color: #2877ff; text-decoration: none; }
  .sep { margin: 0 6px; }
}

.login-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
  background: #f5f9ff;
  i { color: #2877ff; font-size: 16px; }
  .el-button { margin-left: auto; }
}

.catalog-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.divider {
  width: 1px;
  height: 20px;
  background: #e8edf3;
}

.textbook-bar {
  flex: 1;
  min-width: 280px;
}

.browse-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 12px;
  align-items: start;
}

.browse-side {
  min-height: 480px;
  overflow: hidden;
}

.knowledge-side {
  padding: 12px;
}

.side-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.tag-list {
  margin-top: 10px;
  max-height: 520px;
  overflow-y: auto;
}

.tag-item {
  padding: 8px 10px;
  font-size: 13px;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  &:hover { background: #f5f9ff; color: #2877ff; }
  &.active { background: #ecf3ff; color: #2877ff; font-weight: 600; }
}

.filter-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 0;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  .tab {
    padding: 10px 24px;
    font-size: 14px;
    color: #606266;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    &.active {
      color: #2877ff;
      font-weight: 600;
      border-bottom-color: #2877ff;
    }
    &.disabled { color: #c0c4cc; cursor: not-allowed; }
  }
}

.filter-panel {
  border-radius: 0;
  border-top: none;
}

.result-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 10px 16px;
  margin-bottom: 12px;
}

.result-sort {
  display: flex;
  gap: 16px;
}

.sort-item {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  &.active { color: #2877ff; font-weight: 600; }
}

.result-search {
  width: 220px;
}

.result-count {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
  b { color: #2877ff; font-size: 16px; }
}

.paper-btn {
  margin-left: 8px;
}

.question-list {
  min-height: 200px;
}

@media (max-width: 992px) {
  .browse-layout { grid-template-columns: 1fr; }
}
</style>
`)

write('views/portal/QuestionDetail.vue', `<template>
  <div class="portal-detail portal-page">
    <div class="portal-container" v-loading="loading">
      <div class="breadcrumb">
        <router-link to="/portal/home">\u9996\u9875</router-link>
        <span class="sep">></span>
        <router-link to="/portal/chapter">\u7ae0\u8282\u9009\u9898</router-link>
        <span class="sep">></span>
        <span>\u8bd5\u9898\u8be6\u60c5</span>
      </div>
      <div v-if="row" class="detail-panel portal-card">
        <div class="detail-meta">
          <el-tag size="small">{{ typeLabel }}</el-tag>
          <span>\u96be\u5ea6 {{ row.difficulty }}</span>
          <span>{{ row.chapterText }}</span>
        </div>
        <qb-formula-text class="detail-stem" block :text="row.content" :images="imageUrls" />
        <ul v-if="optionItems.length" class="detail-options">
          <li v-for="opt in optionItems" :key="opt.label">
            <span class="opt-label">{{ opt.label }}.</span>
            <qb-formula-text :text="opt.text" />
          </li>
        </ul>
        <div class="detail-actions">
          <el-button type="warning" icon="el-icon-shopping-cart-2" @click="addBasket">\u52a0\u5165\u8bd5\u9898\u7bee</el-button>
          <el-button @click="$router.back()">\u8fd4\u56de</el-button>
        </div>
      </div>
      <el-empty v-else-if="!loading && needLogin" description="\u8bf7\u767b\u5f55\u540e\u67e5\u770b\u8bd5\u9898\u8be6\u60c5">
        <el-button type="primary" size="small" @click="goLogin">\u53bb\u767b\u5f55</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'
import { getQuestion } from '@/api/education/question'
import { contentBrief } from '@/store/modules/questionBasket'
import { parseQuestionOption, shouldShowQuestionOptions } from '@/utils/questionOptions'
import { getQuestionTypeLabel } from '@/utils/questionTypes'

export default {
  name: 'PortalQuestionDetail',
  data() {
    return {
      loading: false,
      row: null,
      needLogin: false
    }
  },
  computed: {
    typeLabel() {
      return this.row ? getQuestionTypeLabel(this.row.questionType) : ''
    },
    imageUrls() {
      return this.parseJson(this.row && this.row.images)
    },
    optionItems() {
      const arr = this.parseJson(this.row && this.row.options)
      if (!arr.length || !shouldShowQuestionOptions(this.row.questionType, arr)) return []
      return arr.map((text, i) => parseQuestionOption(text, i))
    }
  },
  created() {
    this.load()
  },
  methods: {
    parseJson(raw) {
      if (!raw) return []
      if (Array.isArray(raw)) return raw
      try {
        const arr = JSON.parse(raw)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    goLogin() {
      this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
    },
    load() {
      const id = this.$route.params.id
      if (!id) return
      if (!getToken()) {
        this.needLogin = true
        return
      }
      this.loading = true
      getQuestion(id).then(res => {
        this.row = res.data || res
      }).catch(() => {
        this.needLogin = true
      }).finally(() => { this.loading = false })
    },
    addBasket() {
      if (!getToken()) {
        this.goLogin()
        return
      }
      if (!this.row) return
      this.$store.dispatch('questionBasket/addQuestions', [{
        questionId: this.row.questionId,
        questionCode: this.row.questionCode,
        content: this.row.content,
        contentBrief: contentBrief(this.row.content),
        questionType: this.row.questionType,
        difficulty: this.row.difficulty,
        options: this.row.options,
        correctAnswer: this.row.correctAnswer,
        scoreValue: 5
      }]).then(() => this.$message.success('\u5df2\u52a0\u5165\u8bd5\u9898\u7bee'))
    }
  }
}
</script>

<style scoped lang="scss">
.portal-detail {
  padding: 16px 0 40px;
}

.breadcrumb {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
  a { color: #2877ff; text-decoration: none; }
  .sep { margin: 0 6px; }
}

.detail-panel {
  padding: 24px;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  font-size: 13px;
  color: #909399;
}

.detail-stem {
  margin-bottom: 16px;
}

.detail-options {
  list-style: none;
  padding: 0;
  margin: 0 0 20px;
  li {
    display: flex;
    gap: 8px;
    padding: 6px 0;
    font-size: 14px;
  }
  .opt-label { font-weight: 600; color: #606266; }
}

.detail-actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
}
</style>
`)

write('views/portal/Paper.vue', `<template>
  <div class="portal-paper portal-page">
    <div class="portal-container paper-wrap">
      <div class="paper-head portal-card">
        <h2>\u667a\u80fd\u7ec4\u5377</h2>
        <p>\u4ece\u8bd5\u9898\u7bee\u9009\u62e9\u8bd5\u9898\u540e\uff0c\u53ef\u5728\u6b64\u9884\u89c8\u5e76\u5bfc\u51fa\u8bd5\u5377\u3002</p>
        <div class="paper-actions">
          <el-button type="primary" :disabled="!basketCount" @click="goPreview">\u8fdb\u5165\u7ec4\u5377\u9884\u89c8</el-button>
          <el-button @click="$router.push('/portal/chapter')">\u7ee7\u7eed\u9009\u9898</el-button>
        </div>
        <p class="paper-tip">\u5f53\u524d\u8bd5\u9898\u7bee\uff1a<b>{{ basketCount }}</b> \u9898</p>
      </div>

      <div class="paper-steps portal-card">
        <div v-for="(step, i) in steps" :key="step.title" class="step-item">
          <div class="step-num">{{ i + 1 }}</div>
          <div class="step-body">
            <h4>{{ step.title }}</h4>
            <p>{{ step.desc }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'PortalPaper',
  data() {
    return {
      steps: [
        { title: '\u7ae0\u8282 / \u77e5\u8bc6\u70b9\u9009\u9898', desc: '\u6309\u6559\u6750\u7ae0\u8282\u6216\u77e5\u8bc6\u70b9\u7b5b\u9009\u8bd5\u9898\uff0c\u52a0\u5165\u8bd5\u9898\u7bee' },
        { title: '\u8c03\u6574\u8bd5\u9898\u7bee', desc: '\u53f3\u4fa7\u6d6e\u52a8\u680f\u6253\u5f00\u8bd5\u9898\u7bee\uff0c\u53ef\u6392\u5e8f\u3001\u5220\u9898\u3001\u8bbe\u5206\u503c' },
        { title: '\u9884\u89c8\u5bfc\u51fa', desc: '\u751f\u6210\u8bd5\u5377\u9884\u89c8\uff0c\u652f\u6301 Word \u5bfc\u51fa\u4e0e\u6253\u5370' }
      ]
    }
  },
  computed: {
    ...mapGetters(['questionBasketCount', 'token']),
    basketCount() {
      return this.questionBasketCount
    }
  },
  methods: {
    goPreview() {
      if (!this.token) {
        this.$router.push({ path: '/login', query: { redirect: '/portal/paper' } })
        return
      }
      this.$router.push('/question-bank-center/question-bank/paper/preview')
    }
  }
}
</script>

<style scoped lang="scss">
.paper-wrap {
  padding: 24px 16px 40px;
}

.paper-head {
  padding: 32px;
  text-align: center;
  margin-bottom: 16px;
  h2 { margin: 0 0 8px; color: #303133; }
  p { color: #909399; margin: 0 0 20px; }
}

.paper-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.paper-tip {
  margin-top: 16px;
  font-size: 14px;
  b { color: #2877ff; font-size: 18px; }
}

.paper-steps {
  padding: 24px 32px;
}

.step-item {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f2f5;
  &:last-child { border-bottom: none; }
}

.step-num {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #ecf3ff;
  color: #2877ff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.step-body {
  h4 { margin: 0 0 4px; font-size: 15px; color: #303133; }
  p { margin: 0; font-size: 13px; color: #909399; }
}
</style>
`)

console.log('part3 done')
