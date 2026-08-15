import {
  loadQuestionTypeOptions,
  getQuestionTypeLabel,
  getQuestionTypeOrder,
  getDefaultQuestionType,
  getCachedQuestionTypeOptions
} from '@/utils/questionTypes'

export default {
  data() {
    return {
      dynamicQuestionTypeOptions: []
    }
  },
  computed: {
    resolvedQuestionTypeOptions() {
      if (this.dynamicQuestionTypeOptions.length) {
        return this.dynamicQuestionTypeOptions
      }
      return getCachedQuestionTypeOptions()
    },
    questionTypeOrder() {
      return this.resolvedQuestionTypeOptions.map(item => item.value)
    }
  },
  created() {
    this.refreshDynamicQuestionTypes()
  },
  methods: {
    refreshDynamicQuestionTypes(force = false) {
      return loadQuestionTypeOptions(force).then(options => {
        this.dynamicQuestionTypeOptions = options
        return options
      })
    },
    questionTypeLabel(type) {
      return getQuestionTypeLabel(type)
    },
    defaultQuestionType() {
      return this.resolvedQuestionTypeOptions.length
        ? this.resolvedQuestionTypeOptions[0].value
        : getDefaultQuestionType()
    },
    orderedTypeCodes(codes) {
      const order = this.questionTypeOrder.length ? this.questionTypeOrder : getQuestionTypeOrder()
      const rank = {}
      order.forEach((code, index) => {
        rank[code] = index
      })
      return [...codes].sort((a, b) => {
        const ra = rank[a] != null ? rank[a] : 9999
        const rb = rank[b] != null ? rank[b] : 9999
        return ra - rb
      })
    }
  }
}
