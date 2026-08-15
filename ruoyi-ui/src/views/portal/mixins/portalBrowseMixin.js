import { listQuestion, getQuestion } from '@/api/education/question'
import { listSubject } from '@/api/education/subject'
import { contentBrief } from '@/store/modules/questionBasket'
import { flyToBasket, markCardFlyingOut, resetCardFlyingOut } from '@/utils/questionBasketFly'
import { loadQuestionTypeOptions } from '@/utils/questionTypes'
import { saveLastSubject } from '@/utils/questionBasketPrefs'
import { getToken } from '@/utils/auth'
import { goPortalLogin } from '@/utils/portalLogin'
import { debounce } from '@/utils'
import { getSubjectCache, setSubjectCache } from '@/utils/catalogCache'

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
      expandedQuestionId: null,
      questionDetailMap: {},
      detailLoadingId: null,
      catalog: {
        schoolStage: '高中',
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
    this.scheduleFilterQuery = debounce(() => {
      this.queryParams.pageNum = 1
      this.getList()
    }, 280)
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
      const cached = getSubjectCache()
      if (cached && cached.length) {
        this.subjectOptions = cached
        this.applyDefaultSubject()
        this.getList()
        return
      }
      listSubject({ pageNum: 1, pageSize: 100, status: '0' }).then(res => {
        this.subjectOptions = res.rows || []
        setSubjectCache(this.subjectOptions)
        this.applyDefaultSubject()
        this.getList()
      })
    },
    applyDefaultSubject() {
      if (!this.queryParams.subjectId && this.subjectOptions.length) {
        const physics = this.subjectOptions.find(s => (s.subjectName || '').includes('物理'))
        this.queryParams.subjectId = (physics || this.subjectOptions[0]).subjectId
      }
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
        this.expandedQuestionId = null
        this.detailLoadingId = null
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
      this.catalog.versionId = undefined
      this.catalog.textbookId = undefined
      saveLastSubject(id)
      this.$router.replace({
        path: this.$route.path,
        query: { ...this.$route.query, subjectId: id }
      }).catch(() => {})
      this.handleQuery()
      if (this.mode === 'knowledge') this.loadKnowledgeTags()
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
    onChapterChange(payload) {
      const data = payload || {}
      this.queryParams.chapterId = data.chapterId || undefined
      this.handleQuery()
    },
    onFilterChange({ field, value }) {
      if (field === 'questionType') {
        this.queryParams.questionType = value
      } else if (field === 'difficulty') {
        this.queryParams.difficultyMin = value && value.min
        this.queryParams.difficultyMax = value && value.max
      }
      this.scheduleFilterQuery()
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
      this.$confirm('选题与组卷需先登录，是否前往登录？', '提示', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        goPortalLogin(this.$router, this.$route.fullPath)
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
      this.$store.dispatch('questionBasket/addQuestions', [payload]).then(count => {
        if (count > 0) {
          if (el) flyToBasket(el)
          this.$message.success('已加入试题篮')
        } else {
          this.$message.warning('该题已在试题篮中')
        }
      }).catch(err => {
        if (err && err.message === 'OVER_LIMIT') {
          this.$message.warning('试题篮已满')
        }
      }).finally(() => {
        if (el) resetCardFlyingOut(el)
      })
    },
    handleViewDetail(row) {
      const id = row.questionId
      if (this.expandedQuestionId === id) {
        this.expandedQuestionId = null
        return
      }
      this.expandedQuestionId = id
      if (this.questionDetailMap[id]) return
      this.detailLoadingId = id
      getQuestion(id).then(res => {
        this.$set(this.questionDetailMap, id, res.data || {})
      }).catch(() => {
        this.$message.error('加载详情失败')
      }).finally(() => {
        if (this.detailLoadingId === id) this.detailLoadingId = null
      })
    },
    openQuestionPage(row) {
      this.$router.push({ path: '/question/' + row.questionId })
    }
  }
}
