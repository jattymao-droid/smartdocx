/**
 * Rewrite Home.vue with smart UI (ASCII-safe). Run: node scripts/write-portal-home.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/portal/Home.vue')

const L = {
  badge: '\u6821\u672c\u667a\u6167\u9898\u5e93',
  smartBadge: 'AI \u667a\u80fd\u8d4b\u80fd',
  titlePrefix: '\u7cbe\u51c6\u9009\u9898\u00b7',
  titleEm: '\u667a\u80fd\u7ec4\u5377',
  heroDesc: '\u6309\u6559\u6750\u7ae0\u8282\u4e0e\u77e5\u8bc6\u70b9\u7b5b\u9009\uff0c\u8bd5\u9898\u7bee\u4e00\u952e\u51fa\u5377\uff0c\u8ba9\u51fa\u5377\u66f4\u9ad8\u6548',
  startPick: '\u5f00\u59cb\u9009\u9898',
  smartPaper: '\u667a\u80fd\u7ec4\u5377',
  panelLabel: '\u5e93\u5bb9\u6982\u89c8',
  panelLive: '\u5b9e\u65f6\u66f4\u65b0',
  statQuestions: '\u5df2\u5ba1\u6838\u8bd5\u9898',
  statRecent: '\u8fd17\u65e5\u65b0\u589e',
  statSubjects: '\u5b66\u79d1\u8986\u76d6',
  loginPick: '\u767b\u5f55\u540e\u9009\u9898',
  continuePick: '\u7ee7\u7eed\u9009\u9898',
  smartAssistTitle: '\u667a\u80fd\u52a9\u624b',
  smartAssistBtn: '\u7acb\u5373\u4f53\u9a8c',
  capTitle: '\u667a\u80fd\u80fd\u529b',
  capSub: 'AI \u9a71\u52a8\u7684\u6559\u5b66\u51fa\u5377\u4f53\u9a8c',
  quickTitle: '\u5feb\u901f\u5165\u53e3',
  quickSub: '\u5e38\u7528\u529f\u80fd\u4e00\u952e\u76f4\u8fbe',
  topicTitle: '\u70ed\u95e8\u4e13\u9898',
  topicSub: '\u6309\u77e5\u8bc6\u70b9\u5feb\u901f\u7b5b\u9009\u7cbe\u54c1\u8bd5\u9898',
  viewAll: '\u67e5\u770b\u5168\u90e8',
  viewQuestions: '\u67e5\u770b\u8bd5\u9898',
  examTitle: '\u5907\u8003\u8d44\u6e90',
  examSub: '\u771f\u9898\u6a21\u62df\u00b7\u5355\u5143\u6d4b\u9a8c\u00b7\u6821\u5185\u7cbe\u9009',
  ctaTitle: '\u51c6\u5907\u597d\u4e0b\u4e00\u4efd\u8bd5\u5377\u4e86\u5417\uff1f',
  ctaDesc: '\u5148\u9009\u9898\u52a0\u5165\u8bd5\u9898\u7bee\uff0c\u518d\u4e00\u952e\u751f\u6210\u8bd5\u5377\u9884\u89c8\u4e0e\u5bfc\u51fa',
  ctaPick: '\u7acb\u5373\u9009\u9898',
  ctaPaper: '\u8fdb\u5165\u7ec4\u5377',
  enter: '\u8fdb\u5165',
  smartTag: 'AI'
}

const content = `<template>
  <div class="portal-home portal-page">
    <section class="home-hero">
      <div class="hero-mesh" aria-hidden="true" />
      <div class="hero-bg-shape hero-bg-a" aria-hidden="true" />
      <div class="hero-bg-shape hero-bg-b" aria-hidden="true" />
      <div class="portal-container hero-inner">
        <div class="hero-left">
          <span class="hero-badge smart">
            <span class="pulse-dot" />
            ${L.smartBadge}
          </span>
          <h1 class="hero-title">${L.titlePrefix}<em>${L.titleEm}</em></h1>
          <p class="hero-desc">${L.heroDesc}</p>
          <div class="hero-tags">
            <span v-for="t in heroTags" :key="t" class="hero-tag">
              <i class="el-icon-magic-stick" />{{ t }}
            </span>
          </div>
          <div class="hero-actions">
            <el-button type="primary" round size="medium" icon="el-icon-search" @click="goChapter">${L.startPick}</el-button>
            <el-button round size="medium" icon="el-icon-cpu" @click="goPaper">${L.smartPaper}</el-button>
          </div>
        </div>
        <div class="hero-right">
          <div class="hero-panel portal-glass">
            <div class="panel-glow" aria-hidden="true" />
            <div class="panel-head">
              <p class="panel-label">${L.panelLabel}</p>
              <span class="panel-live"><i class="pulse-dot sm" />${L.panelLive}</span>
            </div>
            <div class="panel-stats">
              <div class="panel-stat">
                <b>{{ stats.questions }}</b>
                <span>${L.statQuestions}</span>
              </div>
              <div class="panel-stat">
                <b>{{ stats.recent }}</b>
                <span>${L.statRecent}</span>
              </div>
              <div class="panel-stat">
                <b>4+</b>
                <span>${L.statSubjects}</span>
              </div>
            </div>
            <el-button v-if="!token" type="primary" class="panel-btn" round @click="goLogin">${L.loginPick}</el-button>
            <el-button v-else type="primary" class="panel-btn" round @click="goChapter">${L.continuePick}</el-button>
          </div>
        </div>
      </div>
    </section>

    <div class="portal-container home-body">
      <section class="smart-assist portal-card">
        <div class="smart-assist-icon"><i class="el-icon-magic-stick" /></div>
        <div class="smart-assist-body">
          <div class="smart-assist-title">${L.smartAssistTitle}</div>
          <p class="smart-assist-tip">{{ smartTip }}</p>
        </div>
        <button type="button" class="smart-assist-btn" @click="goChapter">${L.smartAssistBtn}</button>
      </section>

      <div class="section-head">
        <div>
          <div class="portal-section-title tight">${L.capTitle}</div>
          <p class="section-sub">${L.capSub}</p>
        </div>
      </div>
      <div class="cap-grid">
        <div v-for="cap in capabilities" :key="cap.title" class="cap-card portal-card">
          <div class="cap-icon"><i :class="cap.icon" /></div>
          <h4>{{ cap.title }}</h4>
          <p>{{ cap.desc }}</p>
        </div>
      </div>

      <section class="workflow-strip portal-card">
        <div v-for="(step, i) in workflowSteps" :key="step.title" class="workflow-item">
          <div class="workflow-num"><i :class="step.icon" /></div>
          <div class="workflow-text">
            <h4>{{ step.title }}</h4>
            <p>{{ step.desc }}</p>
          </div>
          <i v-if="i < workflowSteps.length - 1" class="el-icon-arrow-right workflow-arrow" />
        </div>
      </section>

      <div class="section-head">
        <div>
          <div class="portal-section-title tight">${L.quickTitle}</div>
          <p class="section-sub">${L.quickSub}</p>
        </div>
      </div>
      <div class="quick-grid">
        <div v-for="item in quickEntries" :key="item.label" class="quick-card portal-card" @click="item.action()">
          <span v-if="item.smart" class="quick-smart-tag">${L.smartTag}</span>
          <div class="quick-icon" :style="{ background: item.iconBg, color: item.iconColor }"><i :class="item.icon" /></div>
          <div class="quick-text">
            <h3>{{ item.label }}</h3>
            <p>{{ item.desc }}</p>
          </div>
          <span class="quick-go">${L.enter} <i class="el-icon-right" /></span>
        </div>
      </div>

      <div class="section-head">
        <div>
          <div class="portal-section-title tight">${L.topicTitle}</div>
          <p class="section-sub">${L.topicSub}</p>
        </div>
        <a class="section-more" @click="goChapter">${L.viewAll} <i class="el-icon-arrow-right" /></a>
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
          <span class="topic-go">${L.viewQuestions} <i class="el-icon-right" /></span>
        </div>
      </div>

      <div class="section-head">
        <div>
          <div class="portal-section-title tight">${L.examTitle}</div>
          <p class="section-sub">${L.examSub}</p>
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
        <div class="cta-glow" aria-hidden="true" />
        <div class="cta-text">
          <h3>${L.ctaTitle}</h3>
          <p>${L.ctaDesc}</p>
        </div>
        <div class="cta-actions">
          <el-button type="primary" round icon="el-icon-search" @click="goChapter">${L.ctaPick}</el-button>
          <el-button round icon="el-icon-cpu" @click="goPaper">${L.ctaPaper}</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listQuestion } from '@/api/education/question'
import { goPortalLogin } from '@/utils/portalLogin'
import { homeLabels } from './portal-home-labels'

export default {
  name: 'PortalHome',
  data() {
    return {
      labels: homeLabels,
      tipIndex: 0,
      tipTimer: null,
      stats: { questions: '-', recent: '-' },
      heroTags: ['\u7ae0\u8282\u7cbe\u51c6\u7b5b\u9009', '\u8bd5\u9898\u7bee\u667a\u80fd\u7ec4\u5377', 'Word \u5bfc\u51fa'],
      capabilities: [
        { icon: 'el-icon-data-analysis', title: '\u667a\u80fd\u63a8\u8350', desc: '\u6309\u7ae0\u8282\u4e0e\u96be\u5ea6\u81ea\u52a8\u5339\u914d\u8bd5\u9898' },
        { icon: 'el-icon-connection', title: '\u77e5\u8bc6\u70b9\u805a\u5408', desc: '\u77e5\u8bc6\u70b9\u6811\u5feb\u901f\u5b9a\u4f4d\u76f8\u5173\u8bd5\u9898' },
        { icon: 'el-icon-document-copy', title: '\u4e00\u952e\u7ec4\u5377', desc: '\u8bd5\u9898\u7bee\u667a\u80fd\u7edf\u8ba1\u9898\u578b\u4e0e\u5206\u503c' }
      ],
      workflowSteps: [
        { icon: 'el-icon-folder-opened', title: '\u9009\u62e9\u7ae0\u8282\u6216\u77e5\u8bc6\u70b9', desc: '\u6309\u6559\u6750\u76ee\u5f55\u5feb\u901f\u5b9a\u4f4d' },
        { icon: 'el-icon-shopping-cart-2', title: '\u52a0\u5165\u8bd5\u9898\u7bee', desc: '\u6536\u85cf\u5019\u9009\u8bd5\u9898\u5e76\u8bbe\u7f6e\u5206\u503c' },
        { icon: 'el-icon-printer', title: '\u9884\u89c8\u5bfc\u51fa\u8bd5\u5377', desc: '\u751f\u6210\u8bd5\u5377\u5e76\u652f\u6301\u6253\u5370' }
      ],
      quickEntries: [
        { label: '\u7ae0\u8282\u9009\u9898', desc: '\u6309\u6559\u6750\u76ee\u5f55\u7cbe\u51c6\u7b5b\u9009', icon: 'el-icon-collection', iconBg: '#EFF6FF', iconColor: '#2563EB', smart: false, action: () => this.goChapter() },
        { label: '\u77e5\u8bc6\u70b9\u9009\u9898', desc: '\u8986\u76d6\u5b66\u79d1\u77e5\u8bc6\u70b9\u6811', icon: 'el-icon-price-tag', iconBg: '#EFF6FF', iconColor: '#64748B', smart: true, action: () => this.$router.push('/portal/knowledge') },
        { label: '\u667a\u80fd\u7ec4\u5377', desc: '\u8bd5\u9898\u7bee\u62fc\u88c5\u5bfc\u51fa\u8bd5\u5377', icon: 'el-icon-document-copy', iconBg: '#F5F0E1', iconColor: '#2563EB', smart: true, action: () => this.goPaper() },
        { label: '\u6821\u672c\u9898\u5e93', desc: '\u6821\u5185\u5bfc\u5165\u4e0e\u5171\u4eab', icon: 'el-icon-school', iconBg: '#DBEAFE', iconColor: '#A68B5B', smart: false, action: () => this.goChapter() }
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
  computed: {
    ...mapGetters(['token']),
    smartTip() {
      const tips = this.labels.smartTips || []
      return tips[this.tipIndex % tips.length] || ''
    }
  },
  created() {
    this.loadStats()
    this.tipTimer = setInterval(() => {
      const tips = this.labels.smartTips || []
      if (tips.length) this.tipIndex = (this.tipIndex + 1) % tips.length
    }, 4500)
  },
  beforeDestroy() {
    if (this.tipTimer) clearInterval(this.tipTimer)
  },
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
$primary: #2563EB;
$primary-light: #3B82F6;
$ink: #1E293B;
$ink-muted: #64748B;
$accent: #3B82F6;
$border: #E2E8F0;

.portal-home { padding-bottom: 48px; }

.home-hero {
  position: relative;
  padding: 36px 0 44px;
  margin-bottom: 8px;
  overflow: hidden;
}

.hero-mesh {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(125deg, rgba(37,99,235,0.04) 0%, transparent 40%),
    linear-gradient(235deg, rgba(59,130,246,0.03) 0%, transparent 45%);
  pointer-events: none;
}

.hero-bg-shape {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  animation: portal-float 8s ease-in-out infinite;
}
.hero-bg-a {
  width: 520px; height: 520px;
  top: -220px; right: -120px;
  background: radial-gradient(circle, rgba(37,99,235,0.06), transparent 68%);
}
.hero-bg-b {
  width: 360px; height: 360px;
  bottom: -160px; left: -80px;
  background: radial-gradient(circle, rgba(59,130,246,0.05), transparent 70%);
  animation-delay: -3s;
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
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: $primary;
  background: #EFF6FF;
  border: 1px solid rgba(37, 99, 235, 0.15);
  margin-bottom: 16px;
  box-shadow: none;
  &.smart {
    background: #EFF6FF;
    border-color: rgba(37, 99, 235, 0.15);
    box-shadow: none;
  }
}

.pulse-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: $accent;
  box-shadow: none;
  animation: none;
  &.sm { width: 6px; height: 6px; }
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
  color: $ink-muted;
  background: #fff;
  border-radius: 20px;
  border: 1px solid $border;
  i { font-size: 12px; color: #94a3b8; }
}

.hero-actions { display: flex; gap: 12px; flex-wrap: wrap; }

.hero-panel {
  position: relative;
  padding: 28px 24px;
  overflow: hidden;
  background: #fff;
  color: $ink;
  border: 1px solid $border;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.08);
}

.panel-glow {
  position: absolute;
  width: 180px; height: 180px;
  top: -60px; right: -40px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59,130,246,0.06), transparent 70%);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.panel-label {
  margin: 0;
  font-size: 13px;
  color: $ink-muted;
  letter-spacing: 0.5px;
}

.panel-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #64748b;
  padding: 3px 10px;
  border-radius: 12px;
  background: #F5F7FA;
  .pulse-dot { background: #52c41a; }
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
  background: #EFF6FF;
  border: 1px solid #EEF2F6;
  b { display: block; font-size: 22px; font-weight: 800; margin-bottom: 4px; color: $ink; }
  span { font-size: 11px; color: $ink-muted; line-height: 1.3; }
}

.panel-btn { width: 100%; }

.home-body { margin-top: 4px; }

.smart-assist {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #fff 0%, #FFFFFF 100%);
  border: 1px solid $border;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.06);
}

.smart-assist-icon {
  width: 48px; height: 48px;
  border-radius: 14px;
  background: #EFF6FF;
  border: 1.5px solid rgba(37, 99, 235, 0.15);
  color: $primary;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  box-shadow: none;
}

.smart-assist-body { flex: 1; min-width: 0; }
.smart-assist-title { font-size: 15px; font-weight: 700; color: $ink; margin-bottom: 4px; }
.smart-assist-tip {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  transition: opacity 0.3s;
}

.smart-assist-btn {
  flex-shrink: 0;
  padding: 10px 20px;
  border-radius: 20px;
  background: transparent;
  border: 1px solid rgba(37, 99, 235, 0.35);
  color: $primary;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: none;
  transition: background 0.15s, border-color 0.15s;
  &:hover { background: #EFF6FF; border-color: $primary-light; }
}

.cap-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.cap-card {
  padding: 20px 18px;
  text-align: center;
  border: 1px solid $border;
  transition: transform 0.2s, box-shadow 0.2s;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
    border-color: #BFDBFE;
  }
  h4 { margin: 12px 0 6px; font-size: 15px; color: $ink; font-weight: 700; }
  p { margin: 0; font-size: 12px; color: #64748b; line-height: 1.5; }
}

.cap-icon {
  width: 44px; height: 44px;
  margin: 0 auto;
  border-radius: 12px;
  background: #EFF6FF;
  border: 1px solid #EEF2F6;
  color: $ink-muted;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.workflow-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  padding: 20px 8px;
  margin-bottom: 28px;
  background: linear-gradient(135deg, #fff, #FFFFFF);
}

.workflow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 20px;
  position: relative;
}

.workflow-num {
  width: 40px; height: 40px;
  border-radius: 12px;
  background: #EFF6FF;
  border: 1px solid #EEF2F6;
  color: $ink-muted;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  i { font-size: 18px; }
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
  color: $ink-muted;
  cursor: pointer;
  white-space: nowrap;
  &:hover { color: $primary; }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 8px;
}

.quick-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 18px;
  cursor: pointer;
  min-height: 168px;
  border: 1px solid $border;
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
    border-color: #BFDBFE;
    .quick-go { color: $primary; }
  }
}

.quick-smart-tag {
  position: absolute;
  top: 12px; right: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  color: $primary;
  background: #EFF6FF;
  border: 1px solid rgba(37, 99, 235, 0.15);
  letter-spacing: 0.05em;
}

.quick-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  i { font-size: 24px; color: inherit; }
}

.quick-text {
  flex: 1;
  h3 { margin: 0 0 6px; font-size: 16px; color: $ink; font-weight: 700; }
  p { margin: 0; font-size: 12px; color: #64748b; line-height: 1.5; }
}

.quick-go {
  font-size: 12px;
  color: $ink-muted;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  transition: color 0.15s;
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
  border-top: 2px solid #EEF2F6;
  &.topic-accent-0 { border-top-color: rgba(37, 99, 235, 0.35); }
  &.topic-accent-1 { border-top-color: rgba(212, 175, 55, 0.55); }
  &.topic-accent-2 { border-top-color: #E2E8F0; }
  h3 { margin: 0 0 8px; font-size: 18px; color: $ink; font-weight: 700; }
  p { margin: 0; font-size: 13px; color: #64748b; }
  .topic-go {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    margin-top: 16px;
    font-size: 13px;
    color: $ink-muted;
    font-weight: 600;
    transition: color 0.15s;
  }
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
    .topic-go { color: $primary; }
  }
}

.topic-index {
  position: absolute;
  top: 8px; right: 14px;
  font-size: 42px;
  font-weight: 900;
  color: rgba(15, 23, 42, 0.06);
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
  border: 1px solid $border;
  &:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(15, 23, 42, 0.07); }
}

.exam-icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  background: #EFF6FF;
  border: 1px solid #EEF2F6;
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
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  margin-top: 12px;
  background: linear-gradient(135deg, #fff 0%, #FFFFFF 100%);
  border: 1px solid $border;
  overflow: hidden;
}

.cta-glow {
  position: absolute;
  width: 200px; height: 200px;
  right: -40px; top: -60px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59,130,246,0.05), transparent 70%);
  pointer-events: none;
}

.cta-text {
  position: relative;
  h3 { margin: 0 0 8px; font-size: 20px; color: $ink; font-weight: 700; }
  p { margin: 0; font-size: 14px; color: #64748b; }
}

.cta-actions { display: flex; gap: 12px; flex-shrink: 0; position: relative; }

@keyframes portal-pulse {
  0% { box-shadow: 0 0 0 0 rgba(37,99,235,0.45); }
  70% { box-shadow: 0 0 0 8px rgba(37,99,235,0); }
  100% { box-shadow: 0 0 0 0 rgba(37,99,235,0); }
}

@keyframes portal-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

@media (max-width: 1200px) {
  .quick-grid, .cap-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-inner { grid-template-columns: 1fr; }
  .hero-title { font-size: 32px; }
}

@media (max-width: 992px) {
  .workflow-strip { grid-template-columns: 1fr; }
  .workflow-arrow { display: none; }
  .topic-row, .exam-row { grid-template-columns: 1fr; }
  .cta-banner { flex-direction: column; text-align: center; }
  .cta-actions { width: 100%; justify-content: center; }
  .smart-assist { flex-wrap: wrap; }
  .smart-assist-btn { width: 100%; }
}

@media (max-width: 600px) {
  .cap-grid { grid-template-columns: 1fr; }
}
</style>
`

fs.writeFileSync(file, content, 'utf8')
console.log('wrote Home.vue (smart UI)')
