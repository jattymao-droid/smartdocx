const fs = require('fs')
const path = require('path')
const src = path.join(__dirname, '../src/views/portal/Home.vue')

const C = {
  primary: '#52aac1',
  primaryLight: '#6ebdd4',
  primaryText: '#3d7a8f',
  primarySoft: '#73b290',
  tintBorder: '#d4e8ef',
  tintActive: '#dceef5',
  tintHover: '#f0f8fb',
  heroFrom: '#6ebdd4',
  heroTo: '#52aac1',
  heroText: '#ffffff',
  accentAmber: '#e8a54b',
  accentMuted: '#b5b1c7',
  shadow: 'rgba(82, 170, 193, 0.12)'
}

const content = `<template>
  <div class="portal-home portal-page">
    <div class="portal-container">
      <section class="hero-section">
        <div class="hero-content portal-card">
          <div class="hero-badge">\u6821\u672c\u667a\u6167\u9898\u5e93</div>
          <h1 class="hero-title">\u7cbe\u51c6\u9009\u9898\u00b7\u667a\u80fd\u7ec4\u5377</h1>
          <p class="hero-desc">\u6309\u6559\u6750\u7ae0\u8282\u4e0e\u77e5\u8bc6\u70b9\u7b5b\u9009\uff0c\u8bd5\u9898\u7bee\u4e00\u952e\u51fa\u5377\uff0c\u670d\u52a1\u6821\u5185\u6559\u5b66\u5168\u6d41\u7a0b</p>
          <div class="hero-actions">
            <el-button type="primary" round size="medium" @click="goChapter">\u5f00\u59cb\u9009\u9898</el-button>
            <el-button round size="medium" @click="goPaper">\u667a\u80fd\u7ec4\u5377</el-button>
          </div>
          <div class="hero-deco" aria-hidden="true" />
        </div>
        <aside class="hero-aside">
          <div class="stats-card portal-card">
            <div class="stats-icon"><i class="el-icon-data-analysis" /></div>
            <p class="stats-label">\u5df2\u5ba1\u6838\u8bd5\u9898</p>
            <p class="stats-num">{{ stats.questions }}</p>
            <p class="stats-sub">\u8fd17\u65e5\u65b0\u589e {{ stats.recent }}</p>
            <el-button v-if="!token" type="primary" class="stats-btn" round @click="goLogin">\u767b\u5f55\u540e\u9009\u9898</el-button>
            <el-button v-else type="primary" class="stats-btn" round @click="goChapter">\u7ee7\u7eed\u9009\u9898</el-button>
          </div>
        </aside>
      </section>

      <div class="portal-section-title">\u5feb\u901f\u5165\u53e3</div>
      <div class="quick-grid">
        <div v-for="item in quickEntries" :key="item.label" class="quick-card portal-card" @click="item.action()">
          <div class="quick-icon" :style="{ background: item.iconBg }"><i :class="item.icon" /></div>
          <div class="quick-text"><h3>{{ item.label }}</h3><p>{{ item.desc }}</p></div>
          <i class="el-icon-arrow-right quick-arrow" />
        </div>
      </div>

      <div class="portal-section-title">\u70ed\u95e8\u4e13\u9898</div>
      <div class="topic-row">
        <div v-for="(t, i) in topics" :key="t.title" class="topic-card portal-card" :class="'topic-accent-' + (i % 3)" @click="goChapterWithKw(t.keyword)">
          <h3>{{ t.title }}</h3><p>{{ t.desc }}</p>
          <span class="topic-go">\u67e5\u770b\u8bd5\u9898 <i class="el-icon-right" /></span>
        </div>
      </div>

      <div class="portal-section-title">\u5907\u8003\u8d44\u6e90</div>
      <div class="exam-row">
        <div v-for="e in examCards" :key="e.title" class="exam-card portal-card" @click="goChapterWithKw(e.keyword)">
          <div class="exam-icon"><i :class="e.icon" /></div>
          <div class="exam-text"><h4>{{ e.title }}</h4><p>{{ e.desc }}</p></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listQuestion } from '@/api/education/question'
import { goPortalLogin } from '@/utils/portalLogin'

export default {
  name: 'PortalHome',
  data() {
    return {
      stats: { questions: '-', recent: '-' },
      quickEntries: [
        { label: '\u7ae0\u8282\u9009\u9898', desc: '\u6309\u6559\u6750\u76ee\u5f55\u7cbe\u51c6\u7b5b\u9009', icon: 'el-icon-collection', iconBg: 'linear-gradient(135deg,#6ebdd4,#52aac1)', action: () => this.goChapter() },
        { label: '\u77e5\u8bc6\u70b9\u9009\u9898', desc: '\u8986\u76d6\u5b66\u79d1\u77e5\u8bc6\u70b9\u6811', icon: 'el-icon-price-tag', iconBg: 'linear-gradient(135deg,#73b290,#5a9a78)', action: () => this.$router.push('/portal/knowledge') },
        { label: '\u667a\u80fd\u7ec4\u5377', desc: '\u8bd5\u9898\u7bee\u62fc\u88c5\u5bfc\u51fa\u8bd5\u5377', icon: 'el-icon-document-copy', iconBg: 'linear-gradient(135deg,#e8a54b,#f6d6b1)', action: () => this.goPaper() },
        { label: '\u6821\u672c\u9898\u5e93', desc: '\u6821\u5185\u5bfc\u5165\u4e0e\u5171\u4eab', icon: 'el-icon-school', iconBg: 'linear-gradient(135deg,#b5b1c7,#8b82b5)', action: () => this.goChapter() }
      ],
      topics: [
        { title: '\u529b\u5b66\u7efc\u5408', desc: '\u725b\u987f\u5b9a\u5f8b \u00b7 \u66f2\u7ebf\u8fd0\u52a8', keyword: '\u725b\u987f' },
        { title: '\u7535\u78c1\u5b66', desc: '\u7535\u573a \u00b7 \u78c1\u573a \u00b7 \u7535\u78c1\u611f\u5e94', keyword: '\u7535\u573a' },
        { title: '\u5b9e\u9a8c\u63a2\u7a76', desc: '\u529b\u5b66\u5b9e\u9a8c \u00b7 \u7535\u5b66\u5b9e\u9a8c', keyword: '\u5b9e\u9a8c' }
      ],
      examCards: [
        { title: '\u9ad8\u8003\u4e00\u7ad9\u5f0f', desc: '\u771f\u9898 \u00b7 \u6a21\u62df \u00b7 \u7cbe\u9009', icon: 'el-icon-medal', keyword: '\u9ad8\u8003' },
        { title: '\u771f\u9898\u89e3\u6790', desc: '\u5386\u5e74\u771f\u9898\u9010\u9898\u7cbe\u8bb2', icon: 'el-icon-reading', keyword: '\u771f\u9898' },
        { title: '\u5355\u5143\u6d4b\u9a8c', desc: '\u540c\u6b65\u6559\u5b66\u9636\u6bb5\u68c0\u6d4b', icon: 'el-icon-edit-outline', keyword: '\u5355\u5143' },
        { title: '\u9650\u65f6\u514d\u8d39', desc: '\u6821\u5185\u7cbe\u54c1\u8bd5\u5377\u9886\u53d6', icon: 'el-icon-present', keyword: '\u8bd5\u5377' }
      ]
    }
  },
  computed: { ...mapGetters(['token']) },
  created() { this.loadStats() },
  methods: {
    loadStats() {
      listQuestion({ pageNum: 1, pageSize: 1, status: '0' }).then(res => {
        this.stats.questions = (res.total || 0).toLocaleString()
        this.stats.recent = Math.min(res.total || 0, 99).toLocaleString()
      }).catch(() => {})
    },
    goLogin() { goPortalLogin(this.$router, '/portal/home') },
    goChapter() { this.$router.push('/portal/chapter') },
    goPaper() { this.$router.push('/portal/paper') },
    goChapterWithKw(kw) { this.$router.push({ path: '/portal/chapter', query: { keyword: kw } }) }
  }
}
</script>

<style scoped lang="scss">
$primary: ${C.primary};
$primary-light: ${C.primaryLight};
$ink: #1e293b;
.hero-section { display: grid; grid-template-columns: 1fr 280px; gap: 20px; margin: 24px 0 8px; }
.hero-content { position: relative; padding: 36px 40px; overflow: hidden; border: none; background: linear-gradient(135deg, ${C.heroFrom}, ${C.heroTo}); color: ${C.heroText}; }
.hero-badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; background: rgba(255,255,255,0.18); color: #fff; margin-bottom: 14px; }
.hero-title { margin: 0 0 10px; font-size: 30px; font-weight: 700; }
.hero-desc { margin: 0 0 24px; font-size: 15px; opacity: 0.92; max-width: 480px; line-height: 1.6; }
.hero-actions { display: flex; gap: 12px; .el-button--default { background: rgba(255,255,255,0.12); border-color: rgba(255,255,255,0.35); color: #fff; &:hover { background: rgba(255,255,255,0.22); color: #fff; } } }
.hero-deco { position: absolute; right: -40px; top: -40px; width: 220px; height: 220px; border-radius: 50%; background: radial-gradient(circle, rgba(110,189,212,0.45), transparent 70%); pointer-events: none; }
.hero-aside .stats-card { padding: 24px 20px; text-align: center; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.stats-icon { width: 48px; height: 48px; border-radius: 14px; background: linear-gradient(135deg, ${C.tintActive}, ${C.tintBorder}); color: ${C.primaryText}; display: flex; align-items: center; justify-content: center; font-size: 22px; margin-bottom: 12px; }
.stats-label { color: #64748b; font-size: 13px; margin: 0; }
.stats-num { font-size: 36px; font-weight: 800; color: $primary; margin: 4px 0; }
.stats-sub { font-size: 12px; color: #94a3b8; margin: 0 0 16px; }
.stats-btn { width: 100%; }
.quick-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-bottom: 8px; }
.quick-card { display: flex; align-items: center; gap: 14px; padding: 18px 20px; cursor: pointer; &:hover { transform: translateY(-2px); box-shadow: 0 12px 32px ${C.shadow}; } }
.quick-icon { width: 48px; height: 48px; border-radius: 14px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; i { font-size: 22px; color: #fff; } }
.quick-text { flex: 1; h3 { margin: 0 0 4px; font-size: 16px; color: $ink; font-weight: 600; } p { margin: 0; font-size: 13px; color: #64748b; } }
.quick-arrow { color: #cbd5e1; }
.topic-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 8px; }
.topic-card { padding: 22px; cursor: pointer; border-top: 3px solid $primary-light; &.topic-accent-1 { border-top-color: ${C.primarySoft}; } &.topic-accent-2 { border-top-color: ${C.accentMuted}; }
  h3 { margin: 0 0 8px; font-size: 17px; color: $ink; } p { margin: 0; font-size: 13px; color: #64748b; }
  .topic-go { display: inline-flex; align-items: center; gap: 4px; margin-top: 14px; font-size: 13px; color: ${C.primaryText}; font-weight: 500; }
  &:hover { transform: translateY(-3px); box-shadow: 0 12px 32px ${C.shadow}; } }
.exam-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 40px; }
.exam-card { display: flex; gap: 12px; padding: 18px; cursor: pointer; &:hover { box-shadow: 0 8px 24px ${C.shadow}; } }
.exam-icon { width: 40px; height: 40px; border-radius: 10px; background: #fff4e6; color: ${C.accentAmber}; display: flex; align-items: center; justify-content: center; flex-shrink: 0; i { font-size: 20px; } }
.exam-text { h4 { margin: 0 0 4px; font-size: 14px; color: $ink; } p { margin: 0; font-size: 12px; color: #64748b; } }
@media (max-width: 992px) { .hero-section { grid-template-columns: 1fr; } .quick-grid, .topic-row { grid-template-columns: 1fr; } .exam-row { grid-template-columns: repeat(2, 1fr); } }
</style>
`

fs.writeFileSync(src, content, 'utf8')
console.log('wrote Home.vue theme')
