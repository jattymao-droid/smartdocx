<template>
  <div class="paper-practice-page">
    <div class="practice-wrap">
      <header v-if="loaded" class="practice-head portal-card">
        <div class="head-main">
          <h1>{{ paperTitle }}</h1>
          <p class="head-meta">
            <span>{{ labels.progressPrefix }} {{ currentIndex + 1 }} / {{ items.length }} {{ labels.progressSuffix }}</span>
            <span v-if="elapsedText" class="meta-sep">{{ timeLabel }} {{ elapsedText }}</span>
          </p>
          <el-progress
            :percentage="progressPercent"
            :stroke-width="8"
            :show-text="false"
            color="#2563eb"
            class="head-progress"
          />
        </div>
        <div class="head-actions">
          <el-button size="small" @click="goBack">{{ backLabel }}</el-button>
          <el-button v-if="finished" type="primary" size="small" @click="restart">{{ labels.restart }}</el-button>
        </div>
      </header>

      <div v-if="loaded && !finished && items.length" class="q-nav portal-card">
        <button
          v-for="(item, idx) in items"
          :key="item.questionId || idx"
          type="button"
          class="q-nav-btn"
          :class="navBtnClass(idx)"
          @click="jumpTo(idx)"
        >
          {{ idx + 1 }}
        </button>
      </div>

      <div v-loading="loading" class="practice-body">
        <el-empty v-if="!loading && !items.length" :description="labels.empty" />

        <template v-else-if="!finished && currentItem">
          <div class="question-card portal-card">
            <div class="q-meta">
              <el-tag size="mini">{{ typeLabel(currentItem.questionType) }}</el-tag>
              <span v-if="currentItem.scoreValue" class="q-score">{{ currentItem.scoreValue }} {{ labels.scoreUnit }}</span>
            </div>
            <qb-formula-text class="q-stem" block :text="displayContent(currentItem)" :images="currentItem.images" />

            <!-- ??? -->
            <div v-if="answerMode === 'single' && optionList.length" class="q-options">
              <button
                v-for="opt in optionList"
                :key="opt.label"
                type="button"
                class="opt-btn"
                :class="optionClass(opt.label)"
                :disabled="revealed"
                @click="pickOption(opt.label)"
              >
                <span class="opt-label">{{ opt.label }}.</span>
                <qb-formula-text :text="opt.text" />
              </button>
            </div>

            <!-- ??? -->
            <div v-else-if="answerMode === 'multi' && optionList.length" class="q-options">
              <button
                v-for="opt in optionList"
                :key="opt.label"
                type="button"
                class="opt-btn"
                :class="multiOptionClass(opt.label)"
                :disabled="revealed"
                @click="toggleMulti(opt.label)"
              >
                <span class="opt-label">{{ opt.label }}.</span>
                <qb-formula-text :text="opt.text" />
              </button>
              <p v-if="!revealed" class="multi-hint">{{ labels.multiHint }}</p>
            </div>

            <!-- ???? -->
            <div v-else-if="answerMode === 'judge'" class="judge-options">
              <button
                type="button"
                class="judge-btn"
                :class="{ active: picked === 'true', correct: revealed && judgeCorrectValue === true, wrong: revealed && picked === 'true' && judgeCorrectValue !== true }"
                :disabled="revealed"
                @click="pickJudge('true')"
              >
                {{ labels.judgeTrue }}
              </button>
              <button
                type="button"
                class="judge-btn"
                :class="{ active: picked === 'false', correct: revealed && judgeCorrectValue === false, wrong: revealed && picked === 'false' && judgeCorrectValue !== false }"
                :disabled="revealed"
                @click="pickJudge('false')"
              >
                {{ labels.judgeFalse }}
              </button>
            </div>

            <!-- ??? -->
            <div v-else-if="answerMode === 'fill'" class="fill-area">
              <el-input
                v-model="fillAnswer"
                :disabled="revealed"
                :placeholder="labels.fillPh"
                clearable
                @keyup.enter.native="checkAnswer"
              />
            </div>

            <div v-if="revealed" class="q-result" :class="resultClass">
              <template v-if="answerMode === 'subjective'">
                <span>{{ labels.subjectivePrefix }}</span>
                <qb-formula-text :text="correctAnswerText" />
              </template>
              <template v-else-if="lastCorrect">
                <span>{{ labels.correct }}</span>
              </template>
              <template v-else>
                <span>{{ labels.wrongPrefix }}{{ correctAnswerText }}</span>
              </template>
            </div>

            <div v-if="revealed && analysisText" class="q-analysis">
              <div class="analysis-label">{{ labels.analysis }}</div>
              <qb-formula-text block :text="analysisText" />
            </div>

            <div v-if="revealed && answerMode === 'subjective' && !subjectiveGraded" class="subjective-grade">
              <span class="grade-label">{{ labels.selfGrade }}</span>
              <el-button size="small" type="success" plain @click="gradeSubjective(true)">{{ labels.selfCorrect }}</el-button>
              <el-button size="small" type="danger" plain @click="gradeSubjective(false)">{{ labels.selfWrong }}</el-button>
            </div>
          </div>

          <div class="practice-toolbar portal-card">
            <el-button :disabled="currentIndex === 0" @click="prevQuestion">{{ labels.prev }}</el-button>
            <el-button v-if="!revealed && !examMode" type="primary" :loading="checkingAnswer" @click="checkAnswer">{{ labels.check }}</el-button>
            <el-button
              v-else-if="!revealed && examMode"
              type="primary"
              @click="saveExamAnswerAndNext"
            >
              {{ currentIndex >= items.length - 1 ? labels.finish : labels.next }}
            </el-button>
            <el-button
              v-else
              type="primary"
              :disabled="answerMode === 'subjective' && !subjectiveGraded"
              @click="nextQuestion"
            >
              {{ currentIndex >= items.length - 1 ? labels.finish : labels.next }}
            </el-button>
          </div>
        </template>

        <div v-else-if="finished" class="summary-card portal-card">
          <div class="summary-score">
            <b>{{ scoreSummary.correct }}</b>
            <span>/ {{ scoreSummary.gradedTotal }} {{ labels.summarySuffix }}</span>
          </div>
          <p v-if="scoreSummary.choiceTotal">{{ labels.choiceRate }} {{ scoreSummary.rate }}%</p>
          <p v-if="scoreSummary.wrongTotal" class="summary-wrong">{{ labels.wrongCountPrefix }}{{ scoreSummary.wrongTotal }}{{ labels.wrongCountSuffix }}</p>
          <p v-if="elapsedText" class="summary-hint">{{ labels.totalTime }} {{ elapsedText }}</p>
          <p v-if="scoreSummary.subjectiveTotal" class="summary-hint">
            {{ labels.subjectiveHintPrefix }} {{ scoreSummary.subjectiveTotal }} {{ labels.subjectiveHintSuffix }}
          </p>
          <p v-if="!token" class="login-hint">{{ labels.loginHint }}</p>

          <div v-if="summaryRows.length" class="summary-list">
            <div
              v-for="row in summaryRows"
              :key="row.questionId"
              class="summary-row"
              :class="'status-' + row.status"
            >
              <span class="row-index">{{ row.index }}</span>
              <span class="row-type">{{ typeLabel(row.questionType) }}</span>
              <span class="row-content">{{ contentBrief(row.content) }}</span>
              <el-tag :type="row.tagType" size="mini">{{ row.statusLabel }}</el-tag>
            </div>
          </div>

          <div class="summary-actions">
            <el-button v-if="scoreSummary.wrongTotal" type="warning" @click="retryWrongOnly">{{ labels.retryWrong }}</el-button>
            <el-button type="primary" @click="goBack">{{ backLabel }}</el-button>
            <el-button plain @click="restart">{{ labels.restart }}</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getQuestion } from '@/api/education/question'
import { checkPracticeAnswer, submitPractice } from '@/api/education/practice'
import { getPaperShare } from '@/api/education/paper'
import { loadPaperShare } from '@/utils/questionBasketPrefs'
import { isServerShareId } from '@/utils/paperShare'
import { shouldShowQuestionOptions, parseQuestionOptions } from '@/utils/questionOptions'
import { parseMultiAnswerLetters, formatChoiceAnswer } from '@/utils/questionAnswer'
import {
  evaluateAnswer,
  formatPickedForSubmit,
  isJudgeAnswerCorrect,
  resolveQuestionAnswerMode
} from '@/utils/practiceAnswer'
import { getQuestionTypeLabel } from '@/utils/questionTypes'
import { contentBrief } from '@/store/modules/questionBasket'
import { mapGetters } from 'vuex'
import QbFormulaText from '@/components/QbFormulaText'

const PRACTICE_STORAGE_KEY = 'edu_qb_practice_snapshot'

const LABELS = {
  progressPrefix: '\u7b2c',
  progressSuffix: '\u9898',
  elapsed: '\u7528\u65f6',
  backPreview: '\u8fd4\u56de\u9884\u89c8',
  backWrongBook: '\u8fd4\u56de\u9519\u9898\u672c',
  backHistory: '\u8fd4\u56de\u7ec3\u4e60\u5386\u53f2',
  backHub: '\u8fd4\u56de\u7ec3\u4e60\u4e2d\u5fc3',
  examRemain: '\u5269\u4f59',
  restart: '\u91cd\u65b0\u7ec3\u4e60',
  empty: '\u6682\u65e0\u7ec3\u4e60\u9898\u76ee\uff0c\u8bf7\u4ece\u7ec4\u5377\u9884\u89c8\u8fdb\u5165\u5728\u7ebf\u7ec3\u4e60',
  scoreUnit: '\u5206',
  correct: '\u56de\u7b54\u6b63\u786e',
  wrongPrefix: '\u56de\u7b54\u9519\u8bef\uff0c\u6b63\u786e\u7b54\u6848\uff1a',
  subjectivePrefix: '\u53c2\u8003\u7b54\u6848\uff1a',
  analysis: '\u89e3\u6790',
  prev: '\u4e0a\u4e00\u9898',
  check: '\u67e5\u770b\u7b54\u6848',
  next: '\u4e0b\u4e00\u9898',
  finish: '\u67e5\u770b\u6210\u7ee9',
  summarySuffix: '\u9898\u6b63\u786e',
  choiceRate: '\u9009\u62e9\u9898\u6b63\u786e\u7387',
  wrongCountPrefix: '\u9519\u8bef ',
  wrongCountSuffix: ' \u9898',
  totalTime: '\u603b\u7528\u65f6',
  subjectiveHintPrefix: '\u542b',
  subjectiveHintSuffix: '\u9053\u4e3b\u89c2\u9898\uff0c\u8bf7\u81ea\u884c\u6838\u5bf9',
  selfGrade: '\u8bf7\u81ea\u8bc4\u4f30\u672c\u9898\u662f\u5426\u505a\u5bf9\uff1a',
  selfCorrect: '\u6211\u505a\u5bf9\u4e86',
  selfWrong: '\u6211\u505a\u9519\u4e86',
  multiHint: '\u53ef\u591a\u9009\uff0c\u9009\u597d\u540e\u70b9\u51fb\u67e5\u770b\u7b54\u6848',
  judgeTrue: '\u6b63\u786e',
  judgeFalse: '\u9519\u8bef',
  fillPh: '\u8bf7\u8f93\u5165\u7b54\u6848',
  saveHint: '\u7ec3\u4e60\u8bb0\u5f55\u5df2\u4fdd\u5b58',
  saveFailed: '\u7ec3\u4e60\u8bb0\u5f55\u4fdd\u5b58\u5931\u8d25\uff0c\u672c\u5730\u6210\u7ee9\u4ecd\u53ef\u67e5\u770b',
  viewHistory: '\u67e5\u770b\u7ec3\u4e60\u5386\u53f2',
  viewWrongBook: '\u67e5\u770b\u9519\u9898\u672c',
  loginHint: '\u767b\u5f55\u540e\u53ef\u81ea\u52a8\u4fdd\u5b58\u7ec3\u4e60\u8bb0\u5f55\u5e76\u6536\u96c6\u9519\u9898',
  retryWrong: '\u91cd\u7ec3\u9519\u9898',
  defaultTitle: '\u5728\u7ebf\u7ec3\u4e60',
  loadFailed: '\u52a0\u8f7d\u7ec3\u4e60\u8bd5\u5377\u5931\u8d25',
  pickFirst: '\u8bf7\u5148\u5b8c\u6210\u7b54\u9898',
  gradeFirst: '\u8bf7\u5148\u81ea\u8bc4\u4f30\u672c\u9898',
  detailFailedPrefix: '\u6709',
  detailFailedSuffix: '\u9053\u9898\u76ee\u8be6\u60c5\u52a0\u8f7d\u5931\u8d25\uff0c\u9009\u9879\u6216\u7b54\u6848\u53ef\u80fd\u4e0d\u5b8c\u6574',
  statusCorrect: '\u6b63\u786e',
  statusWrong: '\u9519\u8bef',
  statusSubjective: '\u4e3b\u89c2\u9898',
  statusUnanswered: '\u672a\u7b54'
}

export default {
  name: 'PaperPractice',
  components: { QbFormulaText },
  data() {
    return {
      labels: LABELS,
      loading: true,
      loaded: false,
      paperTitle: LABELS.defaultTitle,
      items: [],
      detailMap: {},
      practiceSource: 'preview',
      currentIndex: 0,
      picked: '',
      pickedMulti: [],
      fillAnswer: '',
      revealed: false,
      subjectiveGraded: false,
      lastCorrect: false,
      finished: false,
      results: {},
      practiceStartedAt: null,
      timerTick: 0,
      timerHandle: null,
      practiceSubmitted: false,
      snapSubjectId: null,
      snapShareId: null,
      examMode: false,
      examMinutes: 90,
      examEndsAt: null,
      checkingAnswer: false
    }
  },
  computed: {
    ...mapGetters(['token']),
    backLabel() {
      return LABELS.backPreview
    },
    currentItem() {
      return this.items[this.currentIndex] || null
    },
    currentDetail() {
      const id = this.currentItem && this.currentItem.questionId
      return id ? this.detailMap[id] : null
    },
    answerMode() {
      const item = this.currentItem
      return item ? resolveQuestionAnswerMode(item.questionType) : 'subjective'
    },
    optionList() {
      const item = this.currentItem
      const detail = this.currentDetail
      if (!item || !shouldShowQuestionOptions(item.questionType)) return []
      const raw = (detail && detail.options) || item.options
      return parseQuestionOptions(raw, item.questionType)
    },
    correctAnswerText() {
      const detail = this.currentDetail
      if (!detail) return ''
      return formatChoiceAnswer(detail.questionType, detail.correctAnswer) || String(detail.correctAnswer || '')
    },
    judgeCorrectValue() {
      const detail = this.currentDetail
      if (!detail) return null
      return isJudgeAnswerCorrect('true', detail.correctAnswer)
    },
    analysisText() {
      const detail = this.currentDetail
      return (detail && detail.analysis) || ''
    },
    resultClass() {
      if (this.answerMode === 'subjective') return 'subjective'
      return { correct: this.lastCorrect, wrong: !this.lastCorrect }
    },
    progressPercent() {
      if (!this.items.length) return 0
      return Math.round(((this.currentIndex + 1) / this.items.length) * 100)
    },
    timeLabel() {
      return this.examMode ? LABELS.examRemain : LABELS.elapsed
    },
    elapsedText() {
      void this.timerTick
      if (this.examMode && this.examEndsAt) {
        const sec = Math.max(0, Math.round((this.examEndsAt - Date.now()) / 1000))
        if (sec <= 0) return '0\u79d2'
        if (sec < 60) return sec + '\u79d2'
        const m = Math.floor(sec / 60)
        const s = sec % 60
        return s ? (m + '\u5206' + s + '\u79d2') : (m + '\u5206\u949f')
      }
      if (!this.practiceStartedAt) return ''
      const sec = Math.max(0, Math.round((Date.now() - this.practiceStartedAt) / 1000))
      if (sec < 60) return sec + '\u79d2'
      const m = Math.floor(sec / 60)
      const s = sec % 60
      return s ? (m + '\u5206' + s + '\u79d2') : (m + '\u5206\u949f')
    },
    scoreSummary() {
      const total = this.items.length
      let choiceTotal = 0
      let subjectiveTotal = 0
      let correct = 0
      let wrongTotal = 0
      let gradedTotal = 0
      this.items.forEach(item => {
        const saved = this.results[item.questionId]
        const mode = resolveQuestionAnswerMode(item.questionType)
        if (mode === 'subjective') {
          subjectiveTotal++
          if (saved && saved.graded) {
            gradedTotal++
            if (saved.correct) correct++
            else wrongTotal++
          }
        } else {
          choiceTotal++
          if (saved && saved.revealed) {
            gradedTotal++
            if (saved.correct) correct++
            else wrongTotal++
          }
        }
      })
      const rate = choiceTotal ? Math.round((correct / choiceTotal) * 100) : 0
      return { total, choiceTotal, subjectiveTotal, correct, wrongTotal, gradedTotal, rate }
    },
    summaryRows() {
      return this.items.map((item, idx) => {
        const saved = this.results[item.questionId]
        const mode = resolveQuestionAnswerMode(item.questionType)
        const detail = this.detailMap[item.questionId]
        let status = 'unanswered'
        let statusLabel = LABELS.statusUnanswered
        let tagType = 'info'
        if (mode === 'subjective') {
          if (saved && saved.graded) {
            status = saved.correct ? 'correct' : 'wrong'
            statusLabel = LABELS.statusSubjective
            tagType = saved.correct ? 'success' : 'warning'
          }
        } else if (saved && saved.revealed) {
          status = saved.correct ? 'correct' : 'wrong'
          statusLabel = saved.correct ? LABELS.statusCorrect : LABELS.statusWrong
          tagType = saved.correct ? 'success' : 'danger'
        }
        return {
          index: idx + 1,
          questionId: item.questionId,
          questionType: item.questionType,
          content: (detail && detail.content) || item.content || '',
          status,
          statusLabel,
          tagType
        }
      })
    }
  },
  created() {
    this.bootstrap()
  },
  beforeDestroy() {
    this.stopTimer()
  },
  methods: {
    contentBrief,
    typeLabel(type) {
      return getQuestionTypeLabel(type)
    },
    displayContent(item) {
      const detail = this.detailMap[item.questionId]
      return (detail && detail.content) || item.content || ''
    },
    startTimer() {
      this.stopTimer()
      this.timerHandle = setInterval(() => {
        this.timerTick += 1
        if (this.examMode && this.examEndsAt && Date.now() >= this.examEndsAt && !this.finished) {
          this.$message.warning('\u8003\u8bd5\u65f6\u95f4\u5230\uff0c\u81ea\u52a8\u4ea4\u5377')
          this.finishExam()
        }
      }, 1000)
    },
    stopTimer() {
      if (this.timerHandle) {
        clearInterval(this.timerHandle)
        this.timerHandle = null
      }
    },
    async bootstrap() {
      this.loading = true
      try {
        const shareId = this.$route.query.share
        const local = this.$route.query.local
        let snap = null
        if (shareId) {
          try {
            const res = await getPaperShare(String(shareId))
            snap = res.data
          } catch (e) {
            if (!isServerShareId(shareId)) {
              snap = loadPaperShare(shareId)
            }
            if (!snap) throw e
          }
        } else if (local === '1') {
          try {
            const raw = sessionStorage.getItem(PRACTICE_STORAGE_KEY)
            snap = raw ? JSON.parse(raw) : null
          } catch (e) {
            snap = null
          }
        }
        if (snap && snap.items && snap.items.length) {
          this.items = snap.items.map((item, idx) => ({
            ...item,
            orderNum: item.orderNum || idx + 1
          }))
          if (snap.form && snap.form.paperTitle) {
            this.paperTitle = snap.form.paperTitle
          }
          if (snap.form && snap.form.subjectId) {
            this.snapSubjectId = snap.form.subjectId
          }
          if (snap.practiceSource) {
            this.practiceSource = snap.practiceSource
          }
        }
        this.snapShareId = this.$route.query.share ? String(this.$route.query.share) : null
        if (this.$route.query.mode === 'exam') {
          this.examMode = true
          const mins = Number(this.$route.query.minutes) || 90
          this.examMinutes = mins
          this.examEndsAt = Date.now() + mins * 60 * 1000
        }
        this.practiceStartedAt = Date.now()
        this.startTimer()
        await this.prefetchDetails()
        this.loaded = true
      } catch (e) {
        this.$message.error(LABELS.loadFailed)
      } finally {
        this.loading = false
      }
    },
    async prefetchDetails() {
      const ids = this.items.map(i => i.questionId).filter(Boolean)
      const fetchOne = getQuestion
      let failed = 0
      await Promise.all(ids.map(id => {
        if (this.detailMap[id]) return Promise.resolve()
        return fetchOne(id).then(res => {
          if (res.data) this.$set(this.detailMap, id, res.data)
        }).catch(() => { failed += 1 })
      }))
      if (failed > 0) {
        this.$message.warning(`${LABELS.detailFailedPrefix} ${failed} ${LABELS.detailFailedSuffix}`)
      }
    },
    navBtnClass(idx) {
      const item = this.items[idx]
      const saved = item ? this.results[item.questionId] : null
      const mode = item ? resolveQuestionAnswerMode(item.questionType) : 'subjective'
      const answered = mode === 'subjective' ? (saved && saved.graded) : (saved && saved.revealed)
      return {
        active: idx === this.currentIndex,
        answered,
        correct: answered && saved.correct,
        wrong: answered && !saved.correct
      }
    },
    jumpTo(idx) {
      if (idx < 0 || idx >= this.items.length) return
      this.currentIndex = idx
      this.resetQuestionState()
      this.restoreQuestionState()
    },
    pickOption(label) {
      if (this.revealed) return
      this.picked = label
    },
    toggleMulti(label) {
      if (this.revealed) return
      const upper = String(label).toUpperCase()
      const idx = this.pickedMulti.indexOf(upper)
      if (idx >= 0) {
        this.pickedMulti.splice(idx, 1)
      } else {
        this.pickedMulti.push(upper)
        this.pickedMulti.sort()
      }
    },
    pickJudge(value) {
      if (this.revealed) return
      this.picked = value
    },
    optionClass(label) {
      const letters = parseMultiAnswerLetters(this.currentDetail && this.currentDetail.correctAnswer)
      return {
        active: this.picked === label,
        correct: this.revealed && letters.includes(String(label).toUpperCase()),
        wrong: this.revealed && this.picked === label && !letters.includes(String(label).toUpperCase())
      }
    },
    multiOptionClass(label) {
      const letters = parseMultiAnswerLetters(this.currentDetail && this.currentDetail.correctAnswer)
      const upper = String(label).toUpperCase()
      const picked = this.pickedMulti.includes(upper)
      return {
        active: picked,
        correct: this.revealed && letters.includes(upper),
        wrong: this.revealed && picked && !letters.includes(upper)
      }
    },
    hasCurrentAnswer() {
      if (this.answerMode === 'multi') return this.pickedMulti.length > 0
      if (this.answerMode === 'fill') return String(this.fillAnswer || '').trim().length > 0
      if (this.answerMode === 'judge' || this.answerMode === 'single') return !!this.picked
      return true
    },
    buildPickedPayload(item) {
      if (!item) return ''
      if (this.answerMode === 'fill') return this.fillAnswer
      return formatPickedForSubmit(item.questionType, this.picked, this.pickedMulti)
    },
    mergeCheckResult(questionId, data) {
      const existing = this.detailMap[questionId] || {}
      this.$set(this.detailMap, questionId, {
        ...existing,
        correctAnswer: data.correctAnswer,
        analysis: data.analysis
      })
    },
    async requestPracticeCheck(item, pickedAnswer, options = {}) {
      const res = await checkPracticeAnswer({
        questionId: item.questionId,
        questionType: item.questionType,
        pickedAnswer,
        subjective: options.subjective,
        selfCorrect: options.selfCorrect
      })
      const data = res.data || {}
      this.mergeCheckResult(item.questionId, data)
      return data
    },
    async checkAnswer() {
      const item = this.currentItem
      if (!item) return
      if (this.answerMode !== 'subjective' && !this.hasCurrentAnswer()) {
        this.$message.warning(LABELS.pickFirst)
        return
      }
      this.checkingAnswer = true
      try {
        const picked = this.buildPickedPayload(item)
        const data = await this.requestPracticeCheck(item, picked)
        if (this.answerMode === 'subjective') {
          this.lastCorrect = false
          this.subjectiveGraded = false
        } else {
          this.lastCorrect = data.correct === true
        }
      } catch (e) {
        this.$message.error('\u7b54\u6848\u6821\u9a8c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5')
        return
      } finally {
        this.checkingAnswer = false
      }
      this.revealed = true
      this.saveCurrentResult()
    },
    async gradeSubjective(correct) {
      const item = this.currentItem
      if (!item) return
      try {
        await this.requestPracticeCheck(item, '', { subjective: true, selfCorrect: correct })
      } catch (e) {
        this.$message.error('\u7b54\u6848\u6821\u9a8c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5')
        return
      }
      this.lastCorrect = correct
      this.subjectiveGraded = true
      this.$set(this.results, item.questionId, {
        correct,
        picked: '',
        subjective: true,
        graded: true,
        revealed: true
      })
    },
    saveExamAnswerDraft() {
      const item = this.currentItem
      if (!item) return
      const subjective = this.answerMode === 'subjective'
      const picked = subjective ? '' : this.buildPickedPayload(item)
      this.$set(this.results, item.questionId, {
        correct: null,
        picked,
        pickedMulti: [...this.pickedMulti],
        fillAnswer: this.fillAnswer,
        subjective,
        graded: false,
        revealed: false
      })
    },
    saveExamAnswerAndNext() {
      this.saveExamAnswerDraft()
      if (this.currentIndex >= this.items.length - 1) {
        this.finishExam()
        return
      }
      this.currentIndex += 1
      this.resetQuestionState()
      this.restoreQuestionState()
    },
    async revealAllExamAnswers() {
      for (const item of this.items) {
        const saved = this.results[item.questionId]
        if (!saved) continue
        try {
          const data = await this.requestPracticeCheck(
            item,
            saved.picked || '',
            saved.subjective ? { subjective: true, selfCorrect: saved.correct === true } : {}
          )
          this.$set(this.results, item.questionId, {
            ...saved,
            correct: saved.subjective ? (saved.graded ? saved.correct === true : data.correct === true) : data.correct === true,
            graded: true,
            revealed: true
          })
        } catch (e) {
          this.$set(this.results, item.questionId, { ...saved, revealed: true, graded: true })
        }
      }
    },
    async finishExam() {
      if (this.finished) return
      this.saveExamAnswerDraft()
      this.loading = true
      try {
        await this.revealAllExamAnswers()
        this.finished = true
        this.stopTimer()
        this.trySubmitPractice()
      } finally {
        this.loading = false
      }
    },
    saveCurrentResult() {
      const item = this.currentItem
      if (!item) return
      const subjective = this.answerMode === 'subjective'
      const picked = subjective
        ? ''
        : (this.answerMode === 'fill'
          ? this.fillAnswer
          : formatPickedForSubmit(item.questionType, this.picked, this.pickedMulti))
      this.$set(this.results, item.questionId, {
        correct: subjective ? null : this.lastCorrect,
        picked,
        pickedMulti: [...this.pickedMulti],
        fillAnswer: this.fillAnswer,
        subjective,
        graded: subjective ? false : true,
        revealed: true
      })
    },
    prevQuestion() {
      if (this.currentIndex <= 0) return
      this.currentIndex -= 1
      this.resetQuestionState()
      this.restoreQuestionState()
    },
    nextQuestion() {
      if (this.answerMode === 'subjective' && this.revealed && !this.subjectiveGraded) {
        this.$message.warning(LABELS.gradeFirst)
        return
      }
      if (this.currentIndex >= this.items.length - 1) {
        this.finished = true
        this.stopTimer()
        this.trySubmitPractice()
        return
      }
      this.currentIndex += 1
      this.resetQuestionState()
      this.restoreQuestionState()
    },
    resetQuestionState() {
      this.picked = ''
      this.pickedMulti = []
      this.fillAnswer = ''
      this.revealed = false
      this.subjectiveGraded = false
      this.lastCorrect = false
    },
    restoreQuestionState() {
      const item = this.currentItem
      if (!item) return
      const saved = this.results[item.questionId]
      if (!saved) return
      this.picked = saved.picked || ''
      this.pickedMulti = saved.pickedMulti ? [...saved.pickedMulti] : parseMultiAnswerLetters(saved.picked)
      this.fillAnswer = saved.fillAnswer || saved.picked || ''
      this.revealed = !!saved.revealed
      this.subjectiveGraded = !!saved.graded
      this.lastCorrect = saved.correct === true
    },
    restart() {
      this.finished = false
      this.currentIndex = 0
      this.results = {}
      this.practiceSubmitted = false
      this.practiceStartedAt = Date.now()
      this.startTimer()
      this.resetQuestionState()
    },
    retryWrongOnly() {
      const wrongItems = this.items.filter(item => {
        const saved = this.results[item.questionId]
        const mode = resolveQuestionAnswerMode(item.questionType)
        if (!saved) return false
        if (mode === 'subjective') return saved.graded && !saved.correct
        return saved.revealed && !saved.correct
      })
      if (!wrongItems.length) return
      this.items = wrongItems.map((item, idx) => ({ ...item, orderNum: idx + 1 }))
      this.paperTitle = this.paperTitle + ' - ' + LABELS.retryWrong
      this.restart()
    },
    resolveSubjectId() {
      if (this.snapSubjectId) return this.snapSubjectId
      const ids = Object.keys(this.detailMap || {})
      for (let i = 0; i < ids.length; i++) {
        const detail = this.detailMap[ids[i]]
        if (detail && detail.subjectId) return detail.subjectId
      }
      return null
    },
    buildSubmitItems() {
      return this.items.map(item => {
        const saved = this.results[item.questionId] || {}
        const mode = resolveQuestionAnswerMode(item.questionType)
        const subjective = mode === 'subjective'
        const picked = subjective
          ? ''
          : formatPickedForSubmit(item.questionType, saved.picked, saved.pickedMulti)
        return {
          questionId: item.questionId,
          questionType: item.questionType,
          pickedAnswer: picked,
          correct: subjective ? (saved.graded ? saved.correct === true : null) : saved.correct === true,
          subjective
        }
      })
    },
    trySubmitPractice() {
      if (!this.token || this.practiceSubmitted) return
      const items = this.buildSubmitItems().filter(it => {
        const saved = this.results[it.questionId]
        return saved && (saved.revealed || saved.graded)
      })
      if (!items.length) return
      const durationSec = this.practiceStartedAt
        ? Math.max(1, Math.round((Date.now() - this.practiceStartedAt) / 1000))
        : null
      submitPractice({
        subjectId: this.resolveSubjectId(),
        paperTitle: this.paperTitle,
        shareId: this.snapShareId,
        durationSec,
        items
      }).then(() => {
        this.practiceSubmitted = true
        this.$message.success(LABELS.saveHint)
      }).catch(() => {
        this.$message.warning(LABELS.saveFailed)
      })
    },
    goBack() {
      const query = {}
      if (this.$route.query.share) {
        query.share = this.$route.query.share
      } else if (this.$route.query.local) {
        query.local = this.$route.query.local
      }
      this.$router.push({
        path: '/admin/question-bank-center/question-bank/paper/preview',
        query
      })
    }
  }
}
</script>

<style scoped lang="scss">
.paper-practice-page {
  min-height: calc(100vh - 84px);
  background: #f8fafc;
  padding: 16px 20px 40px;

  &--portal {
    min-height: calc(100vh - 120px);
  }
}

.practice-wrap {
  max-width: 880px;
  margin: 0 auto;
}

.practice-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 12px;

  h1 {
    margin: 0 0 6px;
    font-size: 18px;
    color: #1e293b;
  }
}

.head-meta {
  margin: 0 0 10px;
  font-size: 13px;
  color: #64748b;
}

.meta-sep {
  margin-left: 12px;
}

.head-progress {
  max-width: 320px;
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.q-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.q-nav-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;

  &.active {
    border-color: #2563eb;
    color: #2563eb;
    font-weight: 600;
  }

  &.answered.correct {
    background: #f0fdf4;
    border-color: #86efac;
    color: #15803d;
  }

  &.answered.wrong {
    background: #fef2f2;
    border-color: #fca5a5;
    color: #b91c1c;
  }
}

.question-card {
  padding: 20px;
  margin-bottom: 12px;
}

.q-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.q-score {
  font-size: 12px;
  color: #94a3b8;
}

.q-stem {
  margin-bottom: 16px;
}

.q-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.multi-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.opt-btn,
.judge-btn {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;

  &:hover:not(:disabled) {
    border-color: #93c5fd;
    background: #f8fafc;
  }

  &.active {
    border-color: #2563eb;
    background: #eff6ff;
  }

  &.correct {
    border-color: #22c55e;
    background: #f0fdf4;
  }

  &.wrong {
    border-color: #ef4444;
    background: #fef2f2;
  }

  &:disabled {
    cursor: default;
  }
}

.judge-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.judge-btn {
  justify-content: center;
  font-weight: 600;
}

.fill-area {
  margin-top: 4px;
}

.opt-label {
  flex-shrink: 0;
  font-weight: 600;
  color: #475569;
}

.q-result {
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;

  &.correct {
    background: #f0fdf4;
    color: #15803d;
  }

  &.wrong {
    background: #fef2f2;
    color: #b91c1c;
  }

  &.subjective {
    background: #f8fafc;
    color: #475569;
  }
}

.q-analysis {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
}

.analysis-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
}

.subjective-grade {
  margin-top: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.grade-label {
  font-size: 13px;
  color: #64748b;
}

.practice-toolbar {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 14px 16px;
}

.summary-card {
  padding: 28px 20px;
  text-align: center;
}

.summary-score {
  margin-bottom: 10px;

  b {
    font-size: 42px;
    color: #2563eb;
    margin-right: 8px;
  }

  span {
    font-size: 16px;
    color: #64748b;
  }
}

.summary-wrong {
  margin: 0 0 8px;
  color: #ea580c;
  font-size: 14px;
}

.summary-hint,
.login-hint {
  margin: 0 0 8px;
  font-size: 13px;
  color: #64748b;
}

.login-hint {
  color: #2563eb;
}

.summary-list {
  margin: 20px 0;
  text-align: left;
  max-height: 280px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.summary-row {
  display: grid;
  grid-template-columns: 36px 72px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;

  &:last-child {
    border-bottom: none;
  }
}

.row-index {
  color: #94a3b8;
  font-weight: 600;
}

.row-type {
  color: #64748b;
}

.row-content {
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.portal-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
}
</style>
