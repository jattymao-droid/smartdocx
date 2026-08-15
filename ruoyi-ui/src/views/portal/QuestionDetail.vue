<template>
  <div class="portal-detail portal-page">
    <div class="portal-container" v-loading="loading">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="sep">/</span>
        <router-link to="/chapter">章节选题</router-link>
        <span class="sep">/</span>
        <span class="current">试题详情</span>
      </nav>
      <div v-if="row" class="detail-panel portal-card portal-card-accent portal-gradient-panel">
        <div class="detail-meta">
          <span class="meta-tag meta-tag--type">{{ typeLabel }}</span>
          <span class="meta-tag meta-tag--diff">难度 <b>{{ row.difficulty }}</b></span>
          <span class="meta-tag meta-tag--chapter">{{ row.chapterText }}</span>
        </div>
        <qb-formula-text class="detail-stem" block :text="row.content" :images="imageUrls" />
        <ul v-if="optionItems.length" class="detail-options">
          <li v-for="opt in optionItems" :key="opt.label">
            <span class="opt-label">{{ opt.label }}.</span>
            <qb-formula-text :text="opt.text" />
          </li>
        </ul>
        <div class="detail-actions">
          <el-button type="primary" icon="el-icon-shopping-cart-2" @click="addBasket">加入试题篮</el-button>
          <el-button plain @click="$router.back()">返回</el-button>
        </div>
      </div>
      <el-empty v-else-if="!loading && needLogin" description="请登录后查看试题详情">
        <el-button type="primary" size="small" @click="goLogin">去登录</el-button>
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
import { goPortalLogin } from '@/utils/portalLogin'

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
      goPortalLogin(this.$router, this.$route.fullPath)
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
      }]).then(() => this.$message.success('已加入试题篮'))
    }
  }
}
</script>

<style scoped lang="scss">

$primary: #2563EB;
$violet: #7C3AED;
$emerald: #059669;
$ink: #0F172A;
$muted: #64748B;
$border: #E2E8F0;

.portal-detail { padding: 16px 0 40px; }

.detail-panel {
  padding: 26px 28px;
  border-radius: 16px;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    width: 120px;
    height: 120px;
    top: -40px;
    right: -20px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(124, 58, 237, 0.08), transparent 70%);
    pointer-events: none;
  }
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 18px;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  background: #F1F5F9;
  border: 1px solid #E2E8F0;

  b { color: $ink; font-weight: 800; }

  &--type {
    color: #1D4ED8;
    background: linear-gradient(135deg, #EFF6FF, #DBEAFE);
    border-color: rgba(37, 99, 235, 0.2);
  }

  &--diff {
    color: #6D28D9;
    background: linear-gradient(135deg, #FAF5FF, #EDE9FE);
    border-color: rgba(124, 58, 237, 0.2);
  }

  &--chapter {
    color: #047857;
    background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
    border-color: rgba(5, 150, 105, 0.2);
  }
}

.detail-stem {
  margin-bottom: 16px;
  line-height: 1.7;
  padding: 16px 18px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff 0%, #F8FAFF 100%);
  border: 1px solid #EEF2F6;
}

.detail-options {
  list-style: none;
  padding: 0;
  margin: 0 0 20px;
  li {
    display: flex;
    gap: 8px;
    padding: 10px 14px;
    font-size: 14px;
    border-radius: 10px;
    margin-bottom: 6px;
    border: 1px solid transparent;
    transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
    &:hover {
      background: linear-gradient(135deg, #F8FAFF, #F5F3FF);
      border-color: rgba(124, 58, 237, 0.15);
      box-shadow: 0 2px 8px rgba(124, 58, 237, 0.06);
    }
  }
  .opt-label {
    font-weight: 700;
    min-width: 24px;
    background: linear-gradient(135deg, $primary, $violet);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.detail-actions {
  display: flex;
  gap: 12px;
  padding-top: 18px;
  border-top: 1px solid #F1F5F9;
}

</style>
