<template>
  <div class="portal-paper portal-page">
    <div class="portal-container paper-wrap">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">{{ labels.home }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ labels.breadcrumb }}</span>
      </nav>

      <div class="paper-hero portal-card portal-card-accent">
        <div class="paper-hero-left">
          <span class="smart-badge"><i class="el-icon-cpu" /> AI</span>
          <h1>{{ labels.title }}</h1>
          <p>{{ labels.desc }}</p>
          <div class="paper-actions">
            <el-button type="primary" round icon="el-icon-view" :disabled="!basketCount" @click="goPreview">{{ labels.preview }}</el-button>
            <el-button round icon="el-icon-shopping-cart-2" :disabled="!basketCount" @click="openBasket">{{ labels.manageBasket }}</el-button>
            <el-button round icon="el-icon-search" @click="$router.push('/chapter')">{{ labels.continuePick }}</el-button>
          </div>
        </div>
        <div class="paper-hero-right">
          <div class="basket-ring" :class="{ empty: !basketCount }">
            <svg viewBox="0 0 120 120" class="ring-svg">
              <circle cx="60" cy="60" r="52" class="ring-bg" />
              <circle cx="60" cy="60" r="52" class="ring-fill" :style="ringStyle" />
            </svg>
            <div class="ring-center">
              <b>{{ basketCount }}</b>
              <span>{{ labels.basketUnit }}</span>
            </div>
          </div>
          <p class="basket-label">{{ labels.basketPrefix }}<b>{{ basketCount }}</b> {{ labels.basketUnit }}</p>
          <p v-if="basketCount" class="basket-score">{{ labels.totalScore }} <b>{{ basketTotalScore }}</b> {{ labels.scoreUnit }}</p>
        </div>
      </div>

      <div v-if="!basketCount" class="empty-hint portal-card">
        <i class="el-icon-shopping-cart-2" />
        <span>{{ labels.emptyBasket }}</span>
        <el-button type="primary" size="small" round @click="$router.push('/chapter')">{{ labels.goPick }}</el-button>
      </div>

      <template v-else>
        <div class="basket-overview portal-card portal-card-accent">
          <div class="overview-head">
            <h3>{{ labels.basketOverview }}</h3>
            <div class="overview-actions">
              <el-button size="mini" round @click="applySmartScore">{{ labels.smartScore }}</el-button>
              <el-button type="primary" size="mini" round icon="el-icon-view" @click="goPreview">{{ labels.preview }}</el-button>
            </div>
          </div>
          <div v-if="typeStats.length" class="type-chips">
            <span v-for="row in typeStats" :key="row.type" class="type-chip">
              <em>{{ row.label }}</em>
              <b>{{ row.count }}</b> 题 · {{ row.score }} 分
            </span>
          </div>
          <div class="basket-preview-list">
            <div
              v-for="(item, idx) in previewItems"
              :key="item.questionId"
              class="preview-item"
            >
              <span class="preview-no">{{ idx + 1 }}</span>
              <span class="preview-type">{{ questionTypeLabel(item.questionType) }}</span>
              <span class="preview-text">{{ item.contentBrief || briefContent(item.content) }}</span>
              <span class="preview-score">{{ item.scoreValue || 0 }}分</span>
            </div>
            <div v-if="basketCount > previewItems.length" class="preview-more">
              还有 {{ basketCount - previewItems.length }} 题，<button type="button" class="link-btn" @click="openBasket">打开试题篮查看</button>
            </div>
          </div>
        </div>
      </template>

      <div class="smart-engine portal-card">
        <div class="engine-icon"><i class="el-icon-magic-stick" /></div>
        <div class="engine-text">
          <h3>{{ labels.smartTitle }}</h3>
          <p>{{ labels.smartDesc }}</p>
        </div>
      </div>

      <div class="portal-section-title tight steps-head">{{ labels.stepsTitle }}</div>
      <div class="paper-steps portal-card portal-card-accent">
        <div v-for="(step, i) in steps" :key="step.title" class="step-item" :class="'step-item--' + i">
          <div class="step-num"><i :class="step.icon" /></div>
          <div class="step-body">
            <h4>{{ step.title }}</h4>
            <p>{{ step.desc }}</p>
          </div>
          <span v-if="i < steps.length - 1" class="step-connector" />
        </div>
      </div>
    </div>

    <question-basket-drawer v-model="basketOpen" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { goPortalLogin } from '@/utils/portalLogin'
import { contentBrief } from '@/store/modules/questionBasket'
import { groupItemsByQuestionType } from '@/utils/questionTypes'
import dynamicQuestionTypes from '@/mixins/dynamicQuestionTypes'
import QuestionBasketDrawer from '@/views/education/question-bank/components/QuestionBasketDrawer'
import { paperLabels } from './portal-paper-labels'

const SMART_SCORE_BY_TYPE = {
  single: 3,
  multi: 4,
  judge: 2,
  fill: 4,
  knowledge_fill: 4,
  short: 8,
  answer: 12,
  experiment: 10,
  comprehensive: 15,
  reading: 15,
  drawing: 8
}

export default {
  name: 'PortalPaper',
  components: { QuestionBasketDrawer },
  mixins: [dynamicQuestionTypes],
  data() {
    return {
      labels: paperLabels,
      basketOpen: false,
      steps: [
        { icon: 'el-icon-folder-opened', title: '章节 / 知识点选题', desc: '按教材章节或知识点筛选试题，加入试题篮' },
        { icon: 'el-icon-shopping-cart-2', title: '调整试题篮', desc: '排序、删题、按题型一键配分' },
        { icon: 'el-icon-printer', title: '预览导出', desc: '生成试卷预览，支持 Word / PDF 导出' }
      ]
    }
  },
  computed: {
    ...mapGetters(['questionBasketCount', 'questionBasketItems', 'questionBasketTotalScore', 'token']),
    basketCount() {
      return this.questionBasketCount
    },
    basketTotalScore() {
      return this.questionBasketTotalScore || 0
    },
    previewItems() {
      return (this.questionBasketItems || []).slice(0, 6)
    },
    typeStats() {
      return groupItemsByQuestionType(this.questionBasketItems || []).map(g => ({
        type: g.type,
        label: g.label,
        count: g.items.length,
        score: g.items.reduce((s, i) => s + (Number(i.scoreValue) || 0), 0)
      }))
    },
    ringStyle() {
      const max = Math.max(this.basketCount, 20)
      const pct = Math.min(this.basketCount / max, 1)
      const circumference = 2 * Math.PI * 52
      return {
        strokeDasharray: circumference,
        strokeDashoffset: circumference * (1 - pct)
      }
    }
  },
  methods: {
    briefContent(content) {
      return contentBrief(content, 60)
    },
    openBasket() {
      if (!this.token) {
        goPortalLogin(this.$router, '/paper')
        return
      }
      this.basketOpen = true
    },
    goPreview() {
      if (!this.token) {
        goPortalLogin(this.$router, '/paper')
        return
      }
      this.$router.push('/paper/preview')
    },
    applySmartScore() {
      if (!this.basketCount) return
      const groups = groupItemsByQuestionType(this.questionBasketItems)
      groups.forEach(g => {
        const score = SMART_SCORE_BY_TYPE[g.type] || 5
        this.$store.commit('questionBasket/SET_SCORE_BY_TYPE', {
          questionType: g.type,
          scoreValue: score
        })
      })
      this.$message.success('已按题型智能配分，可在预览页继续微调')
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #2563EB;
$primary-light: #3B82F6;
$violet: #7C3AED;
$emerald: #059669;
$amber: #D97706;

.paper-wrap { padding: 0 0 56px; }

.paper-hero {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 32px;
  align-items: center;
  padding: 32px 36px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #fff 0%, #EFF6FF 35%, #F5F3FF 65%, #ECFDF5 100%);
  border: 1px solid rgba(124, 58, 237, 0.12);
  overflow: hidden;
}

.smart-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, $violet, $primary);
  margin-bottom: 14px;
  box-shadow: 0 4px 14px rgba(124, 58, 237, 0.3);
}

.paper-hero-left {
  h1 { margin: 0 0 10px; font-size: 28px; font-weight: 800; color: #1E293B; }
  p { margin: 0 0 22px; color: #64748b; font-size: 15px; line-height: 1.6; max-width: 520px; }
}

.paper-actions { display: flex; gap: 12px; flex-wrap: wrap; }

.paper-hero-right { text-align: center; }

.basket-ring {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 10px;
  &.empty .ring-fill { stroke: #e2e8f0; }
}

.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: #EEF2F6;
  stroke-width: 8;
}

.ring-fill {
  fill: none;
  stroke: $primary;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.5s ease;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  b { font-size: 36px; font-weight: 800; background: linear-gradient(135deg, $primary, $violet); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; line-height: 1; }
  span { font-size: 13px; color: #94a3b8; margin-top: 4px; }
}

.basket-label {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  b { color: $violet; font-size: 18px; }
}

.basket-score {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
  b { color: $emerald; font-size: 20px; }
}

.empty-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #92400E;
  background: linear-gradient(135deg, #FFFBEB, #FEF3C7);
  border: 1px solid rgba(217, 119, 6, 0.25);
  i { font-size: 20px; color: $amber; }
  .el-button { margin-left: auto; }
}

.basket-overview {
  padding: 20px 24px;
  margin-bottom: 16px;
}

.overview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: #1E293B;
  }
}

.overview-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.type-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.type-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #475569;
  background: linear-gradient(135deg, #F8FAFC, #EFF6FF);
  border: 1px solid #E2E8F0;

  em {
    font-style: normal;
    font-weight: 600;
    color: $primary;
  }

  b { color: $violet; }
}

.basket-preview-list {
  border-top: 1px dashed #E2E8F0;
  padding-top: 12px;
}

.preview-item {
  display: grid;
  grid-template-columns: 28px 56px 1fr auto;
  gap: 10px;
  align-items: start;
  padding: 10px 8px;
  border-radius: 10px;
  transition: background 0.15s;

  &:hover { background: #F8FAFF; }
}

.preview-no {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: $primary;
  background: #EFF6FF;
}

.preview-type {
  font-size: 11px;
  font-weight: 600;
  color: #7C3AED;
  padding-top: 4px;
}

.preview-text {
  font-size: 13px;
  line-height: 1.55;
  color: #475569;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.preview-score {
  font-size: 12px;
  font-weight: 700;
  color: $emerald;
  white-space: nowrap;
  padding-top: 4px;
}

.preview-more {
  padding: 8px 8px 0;
  font-size: 12px;
  color: #94a3b8;
}

.link-btn {
  border: none;
  background: none;
  padding: 0;
  color: $primary;
  cursor: pointer;
  font-size: 12px;
  &:hover { text-decoration: underline; }
}

.smart-engine {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 24px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #FAF5FF 0%, #EFF6FF 55%, #fff 100%);
  border: 1px solid rgba(124, 58, 237, 0.15);
}

.engine-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, $violet, $primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(124, 58, 237, 0.3);
}

.engine-text {
  h3 { margin: 0 0 6px; font-size: 16px; font-weight: 700; color: #1E293B; }
  p { margin: 0; font-size: 13px; color: #64748b; line-height: 1.5; }
}

.steps-head { margin: 8px 0 12px; }

.paper-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  padding: 8px 16px 20px;
}

.step-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px 16px;
}

.step-num {
  width: 48px; height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  i { font-size: 22px; }
}

.step-item--0 .step-num {
  background: linear-gradient(135deg, #DBEAFE, #BFDBFE);
  color: #1D4ED8;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
}

.step-item--1 .step-num {
  background: linear-gradient(135deg, #EDE9FE, #DDD6FE);
  color: #6D28D9;
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.15);
}

.step-item--2 .step-num {
  background: linear-gradient(135deg, #D1FAE5, #A7F3D0);
  color: #047857;
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.15);
}

.step-body {
  h4 { margin: 0 0 6px; font-size: 15px; color: #1E293B; font-weight: 600; }
  p { margin: 0; font-size: 12px; color: #64748b; line-height: 1.55; }
}

.step-connector {
  position: absolute;
  top: 44px;
  right: -8%;
  width: 16%;
  height: 2px;
  background: linear-gradient(90deg, #E2E8F0, $violet, $primary-light, #E2E8F0);
}

@media (max-width: 900px) {
  .paper-hero { grid-template-columns: 1fr; text-align: center; }
  .paper-hero-left p { max-width: none; }
  .paper-actions { justify-content: center; }
  .paper-steps { grid-template-columns: 1fr; }
  .step-connector { display: none; }
  .preview-item { grid-template-columns: 28px 1fr auto; }
  .preview-type { display: none; }
}
</style>
