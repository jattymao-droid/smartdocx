/**
 * Portal layout enhancements. Run: node scripts/apply-portal-layout.js
 */
const fs = require('fs')
const path = require('path')

const src = path.join(__dirname, '../src')

function write(rel, content) {
  const f = path.join(src, rel)
  fs.mkdirSync(path.dirname(f), { recursive: true })
  fs.writeFileSync(f, content, 'utf8')
  console.log('wrote', rel)
}

function patchStyle(rel, newStyle) {
  const f = path.join(src, rel)
  let text = fs.readFileSync(f, 'utf8')
  const re = /<style scoped lang="scss">[\s\S]*<\/style>/
  if (!re.test(text)) {
    console.warn('no style:', rel)
    return
  }
  text = text.replace(re, `<style scoped lang="scss">\n${newStyle}\n</style>`)
  fs.writeFileSync(f, text, 'utf8')
  console.log('styled', rel)
}

function patchBetween(rel, marker, insert) {
  const f = path.join(src, rel)
  let text = fs.readFileSync(f, 'utf8')
  if (text.includes(insert.trim().slice(0, 40))) {
    console.log('skip insert', rel)
    return
  }
  if (!text.includes(marker)) {
    console.warn('marker missing', rel)
    return
  }
  text = text.replace(marker, marker + insert)
  fs.writeFileSync(f, text, 'utf8')
  console.log('patched', rel)
}

// --- Home.vue ---
write('views/portal/Home.vue', `<template>
  <div class="portal-home portal-page">
    <section class="home-hero">
      <div class="hero-bg-shape hero-bg-a" aria-hidden="true" />
      <div class="hero-bg-shape hero-bg-b" aria-hidden="true" />
      <div class="portal-container hero-inner">
        <div class="hero-left">
          <span class="hero-badge">\u6821\u672c\u667a\u6167\u9898\u5e93</span>
          <h1 class="hero-title">\u7cbe\u51c6\u9009\u9898\u00b7<em>\u667a\u80fd\u7ec4\u5377</em></h1>
          <p class="hero-desc">\u6309\u6559\u6750\u7ae0\u8282\u4e0e\u77e5\u8bc6\u70b9\u7b5b\u9009\uff0c\u8bd5\u9898\u7bee\u4e00\u952e\u51fa\u5377\uff0c\u8ba9\u51fa\u5377\u66f4\u9ad8\u6548</p>
          <div class="hero-tags">
            <span v-for="t in heroTags" :key="t" class="hero-tag"><i class="el-icon-check" />{{ t }}</span>
          </div>
          <div class="hero-actions">
            <el-button type="primary" round size="medium" icon="el-icon-search" @click="goChapter">\u5f00\u59cb\u9009\u9898</el-button>
            <el-button round size="medium" icon="el-icon-document-copy" @click="goPaper">\u667a\u80fd\u7ec4\u5377</el-button>
          </div>
        </div>
        <div class="hero-right">
          <div class="hero-panel portal-card">
            <div class="panel-glow" aria-hidden="true" />
            <p class="panel-label">\u5e93\u5bb9\u6982\u89c8</p>
            <div class="panel-stats">
              <div class="panel-stat">
                <b>{{ stats.questions }}</b>
                <span>\u5df2\u5ba1\u6838\u8bd5\u9898</span>
              </div>
              <div class="panel-stat">
                <b>{{ stats.recent }}</b>
                <span>\u8fd17\u65e5\u65b0\u589e</span>
              </div>
              <div class="panel-stat">
                <b>4+</b>
                <span>\u5b66\u79d1\u8986\u76d6</span>
              </div>
            </div>
            <el-button v-if="!token" type="primary" class="panel-btn" round @click="goLogin">\u767b\u5f55\u540e\u9009\u9898</el-button>
            <el-button v-else type="primary" class="panel-btn" round @click="goChapter">\u7ee7\u7eed\u9009\u9898</el-button>
          </div>
        </div>
      </div>
    </section>

    <div class="portal-container home-body">
      <section class="workflow-strip portal-card">
        <div v-for="(step, i) in workflowSteps" :key="step.title" class="workflow-item">
          <div class="workflow-num">{{ i + 1 }}</div>
          <div class="workflow-text">
            <h4>{{ step.title }}</h4>
            <p>{{ step.desc }}</p>
          </div>
          <i v-if="i < workflowSteps.length - 1" class="el-icon-arrow-right workflow-arrow" />
        </div>
      </section>

      <div class="section-head">
        <div>
          <div class="portal-section-title tight">\u5feb\u901f\u5165\u53e3</div>
          <p class="section-sub">\u5e38\u7528\u529f\u80fd\u4e00\u952e\u76f4\u8fbe</p>
        </div>
      </div>
      <div class="quick-grid">
        <div v-for="item in quickEntries" :key="item.label" class="quick-card portal-card" @click="item.action()">
          <div class="quick-icon" :style="{ background: item.iconBg }"><i :class="item.icon" /></div>
          <div class="quick-text">
            <h3>{{ item.label }}</h3>
            <p>{{ item.desc }}</p>
          </div>
          <span class="quick-go">\u8fdb\u5165 <i class="el-icon-right" /></span>
        </div>
      </div>

      <div class="section-head">
        <div>
          <div class="portal-section-title tight">\u70ed\u95e8\u4e13\u9898</div>
          <p class="section-sub">\u6309\u77e5\u8bc6\u70b9\u5feb\u901f\u7b5b\u9009\u7cbe\u54c1\u8bd5\u9898</p>
        </div>
        <a class="section-more" @click="goChapter">\u67e5\u770b\u5168\u90e8 <i class="el-icon-arrow-right" /></a>
      </div>
      <div class="topic-row">
        <div
          v-for="(t, i) in topics"
          :key="t.title"
          class="topic-card portal-card"
          :class="'topic-accent-' + (i % 3)"
          @click="goChapterWithKw(t.keyword)"
        >
          <span class="topic-index">0{{ i + 1 }}</span>
          <h3>{{ t.title }}</h3>
          <p>{{ t.desc }}</p>
          <span class="topic-go">\u67e5\u770b\u8bd5\u9898 <i class="el-icon-right" /></span>
        </div>
      </div>

      <div class="section-head">
        <div>
          <div class="portal-section-title tight">\u5907\u8003\u8d44\u6e90</div>
          <p class="section-sub">\u771f\u9898\u6a21\u62df\u00b7\u5355\u5143\u6d4b\u9a8c\u00b7\u6821\u5185\u7cbe\u9009</p>
        </div>
      </div>
      <div class="exam-row">
        <div v-for="e in examCards" :key="e.title" class="exam-card portal-card" @click="goChapterWithKw(e.keyword)">
          <div class="exam-icon"><i :class="e.icon" /></div>
          <div class="exam-text">
            <h4>{{ e.title }}</h4>
            <p>{{ e.desc }}</p>
          </div>
        </div>
      </div>

      <section class="cta-banner portal-card">
        <div class="cta-text">
          <h3>\u51c6\u5907\u597d\u4e0b\u4e00\u4efd\u8bd5\u5377\u4e86\u5417\uff1f</h3>
          <p>\u5148\u9009\u9898\u52a0\u5165\u8bd5\u9898\u7bee\uff0c\u518d\u4e00\u952e\u751f\u6210\u8bd5\u5377\u9884\u89c8\u4e0e\u5bfc\u51fa</p>
        </div>
        <div class="cta-actions">
          <el-button type="primary" round @click="goChapter">\u7acb\u5373\u9009\u9898</el-button>
          <el-button round @click="goPaper">\u8fdb\u5165\u7ec4\u5377</el-button>
        </div>
      </section>
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
      heroTags: ['\u7ae0\u8282\u7cbe\u51c6\u7b5b\u9009', '\u8bd5\u9898\u7bee\u667a\u80fd\u7ec4\u5377', 'Word \u5bfc\u51fa'],
      workflowSteps: [
        { title: '\u9009\u62e9\u7ae0\u8282\u6216\u77e5\u8bc6\u70b9', desc: '\u6309\u6559\u6750\u76ee\u5f55\u5feb\u901f\u5b9a\u4f4d' },
        { title: '\u52a0\u5165\u8bd5\u9898\u7bee', desc: '\u6536\u85cf\u5019\u9009\u8bd5\u9898\u5e76\u8bbe\u7f6e\u5206\u503c' },
        { title: '\u9884\u89c8\u5bfc\u51fa\u8bd5\u5377', desc: '\u751f\u6210\u8bd5\u5377\u5e76\u652f\u6301\u6253\u5370' }
      ],
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
$primary: #52aac1;
$primary-light: #6ebdd4;
$ink: #1e293b;
$accent: #e8a54b;

.portal-home { padding-bottom: 48px; }

.home-hero {
  position: relative;
  padding: 32px 0 40px;
  margin-bottom: 8px;
  overflow: hidden;
}

.hero-bg-shape {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.hero-bg-a {
  width: 520px; height: 520px;
  top: -220px; right: -120px;
  background: radial-gradient(circle, rgba(110,189,212,0.22), transparent 68%);
}
.hero-bg-b {
  width: 360px; height: 360px;
  bottom: -160px; left: -80px;
  background: radial-gradient(circle, rgba(232,165,75,0.1), transparent 70%);
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 28px;
  align-items: center;
}

.hero-badge {
  display: inline-block;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #3d7a8f;
  background: rgba(255,255,255,0.85);
  border: 1px solid #d4e8ef;
  margin-bottom: 16px;
}

.hero-title {
  margin: 0 0 14px;
  font-size: 38px;
  font-weight: 800;
  line-height: 1.2;
  color: $ink;
  em { font-style: normal; color: $primary; }
}

.hero-desc {
  margin: 0 0 18px;
  font-size: 16px;
  line-height: 1.7;
  color: #64748b;
  max-width: 520px;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 24px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 13px;
  color: #3d7a8f;
  background: #f0f8fb;
  border-radius: 20px;
  i { font-size: 12px; color: $primary; }
}

.hero-actions { display: flex; gap: 12px; flex-wrap: wrap; }

.hero-panel {
  position: relative;
  padding: 28px 24px;
  overflow: hidden;
  border: none;
  background: linear-gradient(145deg, #6ebdd4, #52aac1);
  color: #fff;
}

.panel-glow {
  position: absolute;
  width: 180px; height: 180px;
  top: -60px; right: -40px;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
}

.panel-label {
  margin: 0 0 18px;
  font-size: 13px;
  opacity: 0.9;
  letter-spacing: 0.5px;
}

.panel-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.panel-stat {
  text-align: center;
  padding: 12px 8px;
  border-radius: 12px;
  background: rgba(255,255,255,0.14);
  b { display: block; font-size: 22px; font-weight: 800; margin-bottom: 4px; }
  span { font-size: 11px; opacity: 0.9; line-height: 1.3; }
}

.panel-btn { width: 100%; }

.home-body { margin-top: 4px; }

.workflow-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  padding: 20px 8px;
  margin-bottom: 28px;
}

.workflow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 20px;
  position: relative;
}

.workflow-num {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #dceef5, #d4e8ef);
  color: #3d7a8f;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.workflow-text {
  flex: 1;
  h4 { margin: 0 0 4px; font-size: 15px; color: $ink; font-weight: 600; }
  p { margin: 0; font-size: 12px; color: #94a3b8; }
}

.workflow-arrow {
  position: absolute;
  right: -4px;
  color: #cbd5e1;
  font-size: 16px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 28px 0 14px;
  gap: 16px;
}

.section-sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.section-more {
  font-size: 13px;
  color: $primary;
  cursor: pointer;
  white-space: nowrap;
  &:hover { color: $primary-light; }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 8px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 18px;
  cursor: pointer;
  min-height: 168px;
  border: 1px solid #d4e8ef;
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 16px 40px rgba(82,170,193,0.15);
    border-color: $primary-light;
  }
}

.quick-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  i { font-size: 24px; color: #fff; }
}

.quick-text {
  flex: 1;
  h3 { margin: 0 0 6px; font-size: 16px; color: $ink; font-weight: 700; }
  p { margin: 0; font-size: 12px; color: #64748b; line-height: 1.5; }
}

.quick-go {
  font-size: 12px;
  color: $primary;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.topic-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 8px;
}

.topic-card {
  position: relative;
  padding: 24px 22px 20px;
  cursor: pointer;
  overflow: hidden;
  border-top: 3px solid $primary-light;
  &.topic-accent-1 { border-top-color: #73b290; }
  &.topic-accent-2 { border-top-color: #b5b1c7; }
  h3 { margin: 0 0 8px; font-size: 18px; color: $ink; font-weight: 700; }
  p { margin: 0; font-size: 13px; color: #64748b; }
  .topic-go {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    margin-top: 16px;
    font-size: 13px;
    color: #3d7a8f;
    font-weight: 600;
  }
  &:hover { transform: translateY(-3px); box-shadow: 0 14px 36px rgba(82,170,193,0.14); }
}

.topic-index {
  position: absolute;
  top: 8px; right: 14px;
  font-size: 42px;
  font-weight: 900;
  color: rgba(82,170,193,0.1);
  line-height: 1;
}

.exam-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 28px;
}

.exam-card {
  display: flex;
  gap: 12px;
  padding: 18px;
  cursor: pointer;
  align-items: center;
  &:hover { transform: translateY(-2px); box-shadow: 0 10px 28px rgba(82,170,193,0.12); }
}

.exam-icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  background: #fff4e6;
  color: $accent;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  i { font-size: 20px; }
}

.exam-text {
  h4 { margin: 0 0 4px; font-size: 14px; color: $ink; font-weight: 600; }
  p { margin: 0; font-size: 12px; color: #64748b; }
}

.cta-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  margin-top: 12px;
  background: linear-gradient(135deg, #f0f8fb 0%, #fff 50%, #fff8ed 100%);
  border: 1px solid #d4e8ef;
}

.cta-text {
  h3 { margin: 0 0 8px; font-size: 20px; color: $ink; font-weight: 700; }
  p { margin: 0; font-size: 14px; color: #64748b; }
}

.cta-actions { display: flex; gap: 12px; flex-shrink: 0; }

@media (max-width: 1100px) {
  .quick-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-inner { grid-template-columns: 1fr; }
  .hero-title { font-size: 32px; }
}

@media (max-width: 992px) {
  .workflow-strip { grid-template-columns: 1fr; }
  .workflow-arrow { display: none; }
  .topic-row, .exam-row { grid-template-columns: 1fr; }
  .cta-banner { flex-direction: column; text-align: center; }
  .cta-actions { width: 100%; justify-content: center; }
}
</style>
`)

// --- PortalHeader slogan ---
patchBetween(
  'layout-portal/components/PortalHeader.vue',
  '<span class="logo-text">\u667a\u6167\u9898\u5e93</span>',
  `\n            <span class="logo-slogan">\u6559\u5e08\u9009\u9898\u7ec4\u5377</span>`
)

patchStyle('layout-portal/components/PortalHeader.vue', `
$primary: #52aac1;
$primary-light: #6ebdd4;

.portal-topbar {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #d4e8ef;
  padding: 14px 0;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.topbar-inner {
  display: flex;
  align-items: center;
  gap: 28px;
}

.topbar-left { flex-shrink: 0; }

.portal-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: #1e293b;
}

.logo-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, $primary, $primary-light);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 14px rgba(82, 170, 193, 0.2);
}

.logo-text {
  display: block;
  font-weight: 800;
  font-size: 18px;
  line-height: 1.2;
}

.logo-slogan {
  display: block;
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;
  letter-spacing: 0.5px;
}

.topbar-search {
  flex: 1;
  min-width: 0;
  max-width: 560px;
  margin: 0 auto;
}

.topbar-search ::v-deep .el-input__inner {
  border-radius: 22px 0 0 22px;
  height: 42px;
  border-color: #d4e8ef;
}

.topbar-search ::v-deep .el-input-group__append {
  border-radius: 0 22px 22px 0;
  overflow: hidden;
  border-color: #d4e8ef;
  .el-button { border-radius: 0 22px 22px 0; padding: 0 20px; }
}

.hot-words { margin-top: 8px; font-size: 12px; color: #94a3b8; }
.hot-word {
  margin-right: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  color: $primary;
  cursor: pointer;
  &:hover { background: #f0f8fb; color: $primary-light; }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  flex-shrink: 0;
}

.user-name { color: #64748b; font-size: 13px; margin-right: 4px; }

@media (max-width: 900px) {
  .topbar-inner { flex-wrap: wrap; }
  .topbar-search { order: 3; flex: 1 1 100%; max-width: none; margin: 8px 0 0; }
  .logo-slogan { display: none; }
}
`)

// --- PortalBrowse: use write-portal-browse.js (full UTF-8 rewrite) ---

// --- Footer ---
write('layout-portal/index.vue', `<template>
  <div class="portal-layout">
    <portal-header />
    <main class="portal-main">
      <router-view />
    </main>
    <portal-floating-bar />
    <footer class="portal-footer">
      <div class="portal-container footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">\u667a\u6167\u9898\u5e93</span>
          <p>\u6821\u5185\u6559\u5e08\u9009\u9898\u7ec4\u5377\u5e73\u53f0</p>
        </div>
        <div class="footer-links">
          <router-link to="/portal/chapter">\u7ae0\u8282\u9009\u9898</router-link>
          <router-link to="/portal/knowledge">\u77e5\u8bc6\u70b9\u9009\u9898</router-link>
          <router-link to="/portal/paper">\u667a\u80fd\u7ec4\u5377</router-link>
          <router-link to="/" class="admin">\u7ba1\u7406\u540e\u53f0</router-link>
        </div>
        <div class="footer-copy">
          <span>&copy; {{ year }} \u667a\u6167\u9898\u5e93</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script>
import PortalHeader from './components/PortalHeader'
import PortalFloatingBar from './components/PortalFloatingBar'

export default {
  name: 'PortalLayout',
  components: { PortalHeader, PortalFloatingBar },
  computed: {
    year() { return new Date().getFullYear() }
  }
}
</script>

<style scoped lang="scss">
.portal-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f6f9fb;
}

.portal-main { flex: 1; }

.portal-footer {
  background: #fff;
  border-top: 1px solid #d4e8ef;
  padding: 32px 0 24px;
  margin-top: 24px;
}

.footer-inner {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 20px;
}

.footer-brand {
  .footer-logo { font-size: 16px; font-weight: 800; color: #1e293b; }
  p { margin: 6px 0 0; font-size: 12px; color: #94a3b8; }
}

.footer-links {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex-wrap: wrap;
  a {
    font-size: 13px;
    color: #64748b;
    text-decoration: none;
    &:hover { color: #52aac1; }
    &.admin { color: #52aac1; }
  }
}

.footer-copy {
  text-align: right;
  font-size: 12px;
  color: #cbd5e1;
}

@media (max-width: 768px) {
  .footer-inner { grid-template-columns: 1fr; text-align: center; }
  .footer-links { justify-content: center; }
  .footer-copy { text-align: center; }
}
</style>
`)

// --- portal.scss utilities ---
const portalScss = fs.readFileSync(path.join(src, 'assets/styles/portal.scss'), 'utf8')
if (!portalScss.includes('.portal-section-title.tight')) {
  fs.writeFileSync(
    path.join(src, 'assets/styles/portal.scss'),
    portalScss + `
.portal-section-title.tight {
  margin: 0;
}

.portal-section-title.no-margin {
  margin-bottom: 0;
}
`,
    'utf8'
  )
  console.log('extended portal.scss')
}

console.log('portal layout complete')
