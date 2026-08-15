const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')

function write(rel, content) {
  const file = path.join(src, rel)
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, content, 'utf8')
  console.log('wrote', rel)
}

write('assets/styles/portal.scss', `/* User portal */
$portal-blue: #2877ff;
$portal-blue-dark: #1a5fd4;
$portal-orange: #ff6a00;
$portal-bg: #f4f6f9;
$portal-border: #e8edf3;

.portal-page {
  min-height: calc(100vh - 120px);
  background: $portal-bg;
}

.portal-container {
  width: 1200px;
  max-width: 100%;
  margin: 0 auto;
  padding: 0 16px;
}

.portal-card {
  background: #fff;
  border: 1px solid $portal-border;
  border-radius: 4px;
}

.portal-link {
  color: $portal-blue;
  cursor: pointer;
  &:hover { color: $portal-orange; }
}
`)

write('views/portal/mixins/portalBrowseMixin.js', `import { listQuestion } from '@/api/education/question'
import { listSubject } from '@/api/education/subject'
import { contentBrief } from '@/store/modules/questionBasket'
import { flyToBasket, markCardFlyingOut, resetCardFlyingOut } from '@/utils/questionBasketFly'
import { loadQuestionTypeOptions } from '@/utils/questionTypes'
import { saveLastSubject } from '@/utils/questionBasketPrefs'
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      loading: false,
      listRequestSeq: 0,
      questionList: [],
      total: 0,
      subjectOptions: [],
      questionTypeOptions: [],
      sortBy: 'default',
      catalog: {
        schoolStage: '\u9ad8\u4e2d',
        versionId: undefined,
        textbookId: undefined
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        subjectId: undefined,
        chapterId: undefined,
        questionType: undefined,
        difficultyMin: undefined,
        difficultyMax: undefined,
        keyword: undefined,
        knowledgePoint: undefined,
        status: '0'
      }
    }
  },
  created() {
    this.initFromRoute()
    this.loadSubjects()
    loadQuestionTypeOptions().then(opts => { this.questionTypeOptions = opts })
  },
  watch: {
    '$route.query.subjectId'(val) {
      const id = val ? Number(val) : undefined
      if (id === this.queryParams.subjectId) return
      this.queryParams.subjectId = id
      this.queryParams.chapterId = undefined
      if (id) saveLastSubject(id)
      this.handleQuery()
    }
  },
  methods: {
    initFromRoute() {
      const q = this.$route.query || {}
      if (q.subjectId) this.queryParams.subjectId = Number(q.subjectId)
      if (q.keyword) this.queryParams.keyword = q.keyword
      if (q.knowledgePoint) this.queryParams.knowledgePoint = q.knowledgePoint
    },
    loadSubjects() {
      listSubject({ pageNum: 1, pageSize: 100, status: '0' }).then(res => {
        this.subjectOptions = res.rows || []
        if (!this.queryParams.subjectId && this.subjectOptions.length) {
          const physics = this.subjectOptions.find(s => (s.subjectName || '').includes('\u7269\u7406'))
          this.queryParams.subjectId = (physics || this.subjectOptions[0]).subjectId
        }
        this.getList()
      })
    },
    getList() {
      if (!getToken()) {
        this.questionList = []
        this.total = 0
        return
      }
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
      }).catch(() => {
        if (seq === this.listRequestSeq) {
          this.questionList = []
          this.total = 0
        }
      }).finally(() => {
        if (seq === this.listRequestSeq) this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    onSubjectChange(id) {
      this.queryParams.subjectId = id
      this.queryParams.chapterId = undefined
      saveLastSubject(id)
      this.handleQuery()
    },
    onStageChange(stage) {
      this.catalog.schoolStage = stage
      this.catalog.versionId = undefined
      this.catalog.textbookId = undefined
      this.queryParams.chapterId = undefined
      this.handleQuery()
    },
    onCatalogChange() {
      this.queryParams.chapterId = undefined
      this.handleQuery()
    },
    onChapterChange(chapterId) {
      this.queryParams.chapterId = chapterId || undefined
      this.handleQuery()
    },
    onFilterChange({ field, value }) {
      if (field === 'questionType') {
        this.queryParams.questionType = value
      } else if (field === 'difficulty') {
        this.queryParams.difficultyMin = value && value.min
        this.queryParams.difficultyMax = value && value.max
      }
      this.handleQuery()
    },
    setSortBy(mode) {
      if (this.sortBy === mode) return
      this.sortBy = mode
      this.handleQuery()
    },
    cardIndex(idx) {
      return (this.queryParams.pageNum - 1) * this.queryParams.pageSize + idx + 1
    },
    ensureLogin() {
      if (getToken()) return true
      this.$confirm('\u9009\u9898\u4e0e\u7ec4\u5377\u9700\u5148\u767b\u5f55\uff0c\u662f\u5426\u524d\u5f80\u767b\u5f55\uff1f', '\u63d0\u793a', {
        confirmButtonText: '\u53bb\u767b\u5f55',
        cancelButtonText: '\u53d6\u6d88',
        type: 'info'
      }).then(() => {
        this.$router.push({ path: '/login', query: { redirect: this.$route.fullPath } })
      }).catch(() => {})
      return false
    },
    handleAddToBasket(row, el) {
      if (!this.ensureLogin()) return
      const payload = {
        questionId: row.questionId,
        questionCode: row.questionCode,
        content: row.content,
        contentBrief: contentBrief(row.content),
        questionType: row.questionType,
        difficulty: row.difficulty,
        options: row.options,
        correctAnswer: row.correctAnswer,
        scoreValue: 5
      }
      if (el) markCardFlyingOut(el)
      this.$store.dispatch('questionBasket/addQuestions', [payload]).then(() => {
        if (el) flyToBasket(el)
        this.$message.success('\u5df2\u52a0\u5165\u8bd5\u9898\u7bee')
      }).catch(err => {
        if (err && err.message === 'OVER_LIMIT') {
          this.$message.warning('\u8bd5\u9898\u7bee\u5df2\u6ee1')
        }
      }).finally(() => {
        if (el) resetCardFlyingOut(el)
      })
    },
    handleViewDetail(row) {
      this.$router.push({ path: '/portal/question/' + row.questionId })
    }
  }
}
`)

write('views/portal/Home.vue', `<template>
  <div class="portal-home portal-page">
    <div class="portal-container">
      <div class="hero-row">
        <div class="hero-main portal-card">
          <el-carousel height="280px" indicator-position="outside">
            <el-carousel-item v-for="(b, i) in banners" :key="i">
              <div class="banner-slide" :style="{ background: b.bg }">
                <div class="banner-text">
                  <h2>{{ b.title }}</h2>
                  <p>{{ b.desc }}</p>
                  <el-button type="warning" round @click="goChapter">\u7acb\u5373\u9009\u9898</el-button>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>
        <div class="hero-side">
          <div class="promo-stack">
            <div
              v-for="p in promoTiles"
              :key="p.title"
              class="promo-tile portal-card"
              :style="{ background: p.bg }"
              @click="p.action && p.action()"
            >
              <span class="promo-title">{{ p.title }}</span>
              <small>{{ p.desc }}</small>
            </div>
          </div>
          <div class="side-card portal-card stats-card">
            <p class="stats-title">\u5e73\u53f0\u6570\u636e</p>
            <p class="stats-num">{{ stats.questions }}</p>
            <p class="stats-label">\u8bd5\u9898\u603b\u91cf</p>
            <p class="stats-sub">\u8fd17\u65e5\u65b0\u589e {{ stats.recent }}</p>
            <el-button v-if="!token" type="primary" class="stats-btn" @click="goLogin">\u767b\u5f55 / \u6ce8\u518c</el-button>
            <el-button v-else type="primary" class="stats-btn" @click="goChapter">\u5f00\u59cb\u9009\u9898</el-button>
          </div>
        </div>
      </div>

      <div class="feature-grid portal-card">
        <div v-for="col in featureCols" :key="col.title" class="feature-col">
          <div class="feature-head">
            <i :class="col.icon" />
            <span>{{ col.title }}</span>
          </div>
          <div class="feature-links">
            <router-link
              v-for="link in col.links"
              :key="link.label"
              :to="link.to"
              class="feature-link"
            >{{ link.label }}</router-link>
          </div>
        </div>
      </div>

      <div class="special-row portal-card">
        <div
          v-for="item in specialItems"
          :key="item.label"
          class="special-item"
          @click="item.action()"
        >
          <i :class="item.icon" />
          <span>{{ item.label }}</span>
        </div>
      </div>

      <div class="section-title">\u70ed\u95e8\u4e13\u9898</div>
      <div class="topic-row">
        <div
          v-for="t in topics"
          :key="t.title"
          class="topic-card portal-card"
          @click="goChapterWithKw(t.keyword)"
        >
          <h3>{{ t.title }}</h3>
          <p>{{ t.desc }}</p>
          <span class="topic-go">\u8fdb\u5165 \u2192</span>
        </div>
      </div>

      <div class="section-title">\u5907\u8003\u4e13\u533a</div>
      <div class="exam-row">
        <div
          v-for="e in examCards"
          :key="e.title"
          class="exam-card portal-card"
          @click="goChapterWithKw(e.keyword)"
        >
          <i :class="e.icon" />
          <div class="exam-text">
            <h4>{{ e.title }}</h4>
            <p>{{ e.desc }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listQuestion } from '@/api/education/question'

export default {
  name: 'PortalHome',
  data() {
    return {
      stats: { questions: '-', recent: '-' },
      banners: [
        { title: '\u7ae0\u8282\u540c\u6b65\u9009\u9898', desc: '\u6309\u6559\u6750\u7ae0\u8282\u7cbe\u51c6\u7b5b\u9009\uff0c\u652f\u6301\u591a\u7ef4\u5ea6\u8fc7\u6ee4', bg: 'linear-gradient(120deg,#2877ff,#5ab0ff)' },
        { title: '\u77e5\u8bc6\u70b9\u7ec4\u5377', desc: '\u8986\u76d6\u5168\u5b66\u6bb5\u77e5\u8bc6\u70b9\u6811\uff0c\u5feb\u901f\u5b9a\u4f4d\u8584\u5f31\u9879', bg: 'linear-gradient(120deg,#ff6a00,#ffb347)' },
        { title: '\u6821\u672c\u9898\u5e93\u5171\u5efa', desc: '\u5bfc\u5165\u3001\u5ba1\u6838\u3001\u53d1\u5e03\u4e00\u4f53\u5316\u7ba1\u7406', bg: 'linear-gradient(120deg,#36cfc9,#2877ff)' }
      ],
      promoTiles: [
        { title: '\u4e00\u8f6e\u8003\u70b9\u4e13\u7ec3', desc: '\u9ad8\u9891\u8003\u70b9\u96c6\u4e2d\u7a81\u7834', bg: 'linear-gradient(135deg,#fff7e6,#fff)', action: null },
        { title: '\u53d8\u5f0f\u9898\u8bad\u7ec3', desc: '\u4e3e\u4e00\u53cd\u4e09\u5de9\u56fa\u63d0\u5347', bg: 'linear-gradient(135deg,#ecf3ff,#fff)', action: null },
        { title: '\u667a\u80fd\u7ec4\u5377', desc: '\u4e00\u952e\u751f\u6210\u8bd5\u5377', bg: 'linear-gradient(135deg,#e6fffb,#fff)', action: () => this.goPaper() }
      ],
      featureCols: [
        { title: '\u540c\u6b65\u6559\u5b66', icon: 'el-icon-notebook-2', links: [
          { label: '\u7ae0\u8282\u9009\u9898', to: '/portal/chapter' },
          { label: '\u77e5\u8bc6\u70b9\u9009\u9898', to: '/portal/knowledge' },
          { label: '\u540c\u6b65\u8bd5\u5377', to: '/portal/chapter' }
        ]},
        { title: '\u9636\u6bb5\u6d4b\u8bd5', icon: 'el-icon-document', links: [
          { label: '\u5355\u5143\u6d4b\u9a8c', to: '/portal/chapter' },
          { label: '\u671f\u4e2d\u590d\u4e60', to: '/portal/chapter' },
          { label: '\u671f\u672b\u51b2\u523a', to: '/portal/chapter' }
        ]},
        { title: '\u5347\u5b66\u5907\u8003', icon: 'el-icon-trophy', links: [
          { label: '\u771f\u9898\u6c47\u7f16', to: '/portal/chapter' },
          { label: '\u6a21\u62df\u9884\u6d4b', to: '/portal/chapter' }
        ]},
        { title: '\u7279\u8272\u529f\u80fd', icon: 'el-icon-magic-stick', links: [
          { label: '\u667a\u80fd\u7ec4\u5377', to: '/portal/paper' },
          { label: '\u8bd5\u9898\u7bee', to: '/portal/chapter' }
        ]}
      ],
      specialItems: [
        { label: '\u5e73\u884c\u7ec4\u5377', icon: 'el-icon-copy-document', action: () => this.goPaper() },
        { label: '\u7ae0\u8282\u9009\u9898', icon: 'el-icon-collection', action: () => this.goChapter() },
        { label: '\u77e5\u8bc6\u70b9\u9009\u9898', icon: 'el-icon-price-tag', action: () => this.$router.push('/portal/knowledge') },
        { label: '\u7cbe\u54c1\u4e13\u9898', icon: 'el-icon-star-on', action: () => this.goChapterWithKw('\u4e13\u9898') }
      ],
      topics: [
        { title: '\u529b\u5b66\u7efc\u5408', desc: '\u725b\u987f\u5b9a\u5f8b \u00b7 \u66f2\u7ebf\u8fd0\u52a8', keyword: '\u725b\u987f' },
        { title: '\u7535\u78c1\u5b66', desc: '\u7535\u573a \u00b7 \u78c1\u573a \u00b7 \u7535\u78c1\u611f\u5e94', keyword: '\u7535\u573a' },
        { title: '\u5b9e\u9a8c\u63a2\u7a76', desc: '\u529b\u5b66\u5b9e\u9a8c \u00b7 \u7535\u5b66\u5b9e\u9a8c', keyword: '\u5b9e\u9a8c' }
      ],
      examCards: [
        { title: '\u9ad8\u8003\u4e00\u7ad9\u5f0f', desc: '\u771f\u9898 \u00b7 \u6a21\u62df \u00b7 \u538b\u9898', icon: 'el-icon-medal', keyword: '\u9ad8\u8003' },
        { title: '\u771f\u9898\u89e3\u6790', desc: '\u5386\u5e74\u771f\u9898\u9010\u9898\u7cbe\u8bb2', icon: 'el-icon-reading', keyword: '\u771f\u9898' },
        { title: '\u9ad8\u8003\u4f5c\u6587', desc: '\u7d20\u6750\u79ef\u7d2f\u4e0e\u8303\u6587', icon: 'el-icon-edit-outline', keyword: '\u4f5c\u6587' },
        { title: '\u9650\u65f6\u514d\u8d39', desc: '\u7cbe\u54c1\u8bd5\u5377\u9650\u65f6\u9886\u53d6', icon: 'el-icon-present', keyword: '\u8bd5\u5377' }
      ]
    }
  },
  computed: {
    ...mapGetters(['token'])
  },
  created() {
    this.promoTiles[0].action = () => this.goChapter()
    this.promoTiles[1].action = () => this.goChapterWithKw('\u53d8\u5f0f')
    this.loadStats()
  },
  methods: {
    loadStats() {
      listQuestion({ pageNum: 1, pageSize: 1, status: '0' }).then(res => {
        this.stats.questions = (res.total || 0).toLocaleString()
        this.stats.recent = Math.min(res.total || 0, 99).toLocaleString()
      }).catch(() => {})
    },
    goLogin() {
      this.$router.push({ path: '/login', query: { redirect: '/portal/home' } })
    },
    goChapter() {
      this.$router.push('/portal/chapter')
    },
    goPaper() {
      this.$router.push('/portal/paper')
    },
    goChapterWithKw(kw) {
      this.$router.push({ path: '/portal/chapter', query: { keyword: kw } })
    }
  }
}
</script>

<style scoped lang="scss">
.hero-row {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 16px;
  margin: 20px 0;
}

.banner-slide {
  height: 280px;
  display: flex;
  align-items: center;
  padding: 0 48px;
  color: #fff;
  h2 { font-size: 28px; margin: 0 0 12px; }
  p { font-size: 15px; opacity: 0.92; margin-bottom: 20px; }
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.promo-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.promo-tile {
  padding: 10px 14px;
  cursor: pointer;
  transition: transform 0.15s;
  .promo-title { display: block; font-weight: 600; font-size: 13px; color: #303133; }
  small { color: #909399; font-size: 11px; }
  &:hover { transform: translateX(-2px); }
}

.stats-card {
  padding: 16px;
  text-align: center;
}

.stats-title { color: #909399; font-size: 13px; margin: 0; }
.stats-num { font-size: 32px; font-weight: 700; color: #2877ff; margin: 6px 0 4px; }
.stats-label { color: #606266; margin: 0; font-size: 13px; }
.stats-sub { font-size: 12px; color: #909399; margin: 6px 0 12px; }
.stats-btn { width: 100%; }

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  margin-bottom: 12px;
  overflow: hidden;
}

.feature-col {
  padding: 16px 18px;
  border-right: 1px solid #f0f2f5;
  &:last-child { border-right: none; }
}

.feature-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #2877ff;
  margin-bottom: 12px;
  i { font-size: 18px; }
}

.feature-link {
  display: block;
  font-size: 13px;
  color: #606266;
  padding: 4px 0;
  text-decoration: none;
  &:hover { color: #2877ff; }
}

.special-row {
  display: flex;
  justify-content: space-around;
  padding: 14px 8px;
  margin-bottom: 24px;
}

.special-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
  font-size: 13px;
  i { font-size: 28px; color: #2877ff; }
  &:hover { color: #2877ff; }
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 14px;
}

.topic-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.topic-card {
  padding: 20px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  h3 { margin: 0 0 8px; font-size: 16px; color: #303133; }
  p { margin: 0; font-size: 13px; color: #909399; }
  .topic-go { display: inline-block; margin-top: 12px; font-size: 13px; color: #2877ff; }
  &:hover {
    box-shadow: 0 6px 20px rgba(40, 119, 255, 0.12);
    transform: translateY(-2px);
  }
}

.exam-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 32px;
}

.exam-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s;
  i { font-size: 32px; color: #ff6a00; flex-shrink: 0; }
  h4 { margin: 0 0 4px; font-size: 14px; color: #303133; }
  p { margin: 0; font-size: 12px; color: #909399; }
  &:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); }
}

@media (max-width: 992px) {
  .hero-row { grid-template-columns: 1fr; }
  .feature-grid { grid-template-columns: repeat(2, 1fr); }
  .topic-row { grid-template-columns: 1fr; }
  .exam-row { grid-template-columns: repeat(2, 1fr); }
  .special-row { flex-wrap: wrap; gap: 16px; }
}
</style>
`)

console.log('part2 done')
