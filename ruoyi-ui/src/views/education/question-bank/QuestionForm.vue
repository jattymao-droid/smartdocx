<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="88px" size="small" v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="学科" prop="subjectId">
          <el-select v-model="form.subjectId" filterable placeholder="请选择学科" style="width:100%" @change="onSubjectChange">
            <el-option v-for="item in subjectOptions" :key="item.subjectId" :label="item.subjectName" :value="item.subjectId" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="学段" prop="schoolStage">
          <el-select v-model="form.schoolStage" placeholder="请选择学段" style="width:100%" @change="onStageChange">
            <el-option :label="'初中'" value="初中" />
            <el-option :label="'高中'" value="高中" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="教材版本" prop="versionId">
          <el-select
            v-model="form.versionId"
            filterable
            placeholder="请选择教材版本"
            style="width:100%"
            :loading="loadingVersions"
            @change="onVersionChange"
          >
            <el-option v-for="item in versions" :key="item.versionId" :label="item.versionName" :value="item.versionId" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="教材" prop="textbookId">
          <el-select
            v-model="form.textbookId"
            filterable
            placeholder="请选择教材"
            style="width:100%"
            :loading="loadingTextbooks"
            :disabled="!form.versionId"
            @change="onTextbookChange"
          >
            <el-option v-for="item in textbooks" :key="item.textbookId" :label="item.textbookName" :value="item.textbookId" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="章节" prop="chapterPath">
          <el-cascader
            v-model="form.chapterPath"
            :options="chapterOptions"
            :props="cascaderProps"
            filterable
            clearable
            style="width:100%"
            placeholder="请选择章节"
            :disabled="!form.textbookId"
            @change="onChapterChange"
          />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="知识点" prop="knowledgePoints">
          <el-select
            v-model="form.knowledgePoints"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入后回车添加，最多10个"
            style="width:100%"
            @change="onKnowledgeChange"
          >
            <el-option v-for="tag in tagSuggestions" :key="tag.tagName" :label="tag.tagName" :value="tag.tagName" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="难度" prop="difficulty">
          <div class="difficulty-row">
            <el-slider v-model="form.difficulty" :min="0.1" :max="1" :step="0.1" :format-tooltip="formatDifficulty" style="flex:1;margin-right:12px" />
            <el-button-group>
              <el-button size="mini" :type="form.difficulty === 0.3 ? 'primary' : 'default'" @click="form.difficulty = 0.3">易</el-button>
              <el-button size="mini" :type="form.difficulty === 0.5 ? 'primary' : 'default'" @click="form.difficulty = 0.5">中</el-button>
              <el-button size="mini" :type="form.difficulty === 0.8 ? 'primary' : 'default'" @click="form.difficulty = 0.8">难</el-button>
            </el-button-group>
          </div>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="题型" prop="questionType">
          <div class="type-tags">
            <span
              v-for="item in effectiveQuestionTypeOptions"
              :key="item.value"
              class="type-tag"
              :class="{ active: form.questionType === item.value }"
              @click="pickQuestionType(item.value)"
            >{{ item.label }}</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="题干" prop="content">
          <qb-stem-editor
            v-model="form.content"
            :min-height="180"
            :formula-category="formulaCategory"
            placeholder="请输入题干，点击工具栏「Σ」可插入公式"
            @change="scheduleDuplicateCheck"
          />
          <div v-if="form.content" class="content-formula-preview">
            <div class="preview-label">排版预览</div>
            <qb-formula-text :text="form.content" />
          </div>
        </el-form-item>
      </el-col>
      <el-col v-if="duplicateHintVisible" :span="24">
        <el-alert
          :title="duplicateAlertTitle"
          type="warning"
          :closable="false"
          show-icon
          class="duplicate-alert"
        >
          <div v-if="duplicateResult.exactMatches && duplicateResult.exactMatches.length" class="dup-list">
            <div v-for="item in duplicateResult.exactMatches" :key="'e-' + item.questionId" class="dup-item">
              <el-tag size="mini" type="danger">完全相同</el-tag>
              <span class="dup-code">{{ item.questionCode }}</span>
              <span class="dup-content">{{ briefContent(item.content) }}</span>
            </div>
          </div>
          <div v-if="duplicateResult.similarMatches && duplicateResult.similarMatches.length" class="dup-list">
            <div v-for="item in duplicateResult.similarMatches" :key="'s-' + item.questionId" class="dup-item">
              <el-tag size="mini" type="warning">相似 {{ formatSimilarity(item.similarity) }}</el-tag>
              <span class="dup-code">{{ item.questionCode }}</span>
              <span class="dup-content">{{ briefContent(item.content) }}</span>
            </div>
          </div>
        </el-alert>
      </el-col>
      <el-col v-if="isChoiceType" :span="24">
        <el-form-item label="选项" prop="optionList">
          <div v-for="(opt, idx) in form.optionList" :key="idx" class="option-row">
            <span class="option-label">{{ opt.label }}</span>
            <div class="option-formula-wrap">
              <ocr-formula-mathfield
                :value="opt.text"
                @input="val => onOptionTextInput(idx, val)"
              />
            </div>
            <el-button v-if="form.optionList.length > 2" type="text" icon="el-icon-delete" @click="removeOption(idx)" />
          </div>
          <el-button v-if="form.optionList.length < 8" type="text" icon="el-icon-plus" @click="addOption">添加选项</el-button>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="标准答案" prop="answerField">
          <template v-if="currentAnswerMode === 'choice'">
            <el-radio-group v-model="form.answerSingle">
              <el-radio v-for="opt in form.optionList" :key="opt.label" :label="opt.label">{{ opt.label }}</el-radio>
            </el-radio-group>
          </template>
          <template v-else-if="currentAnswerMode === 'multi'">
            <el-checkbox-group v-model="form.answerMulti">
              <el-checkbox v-for="opt in form.optionList" :key="opt.label" :label="opt.label">{{ opt.label }}</el-checkbox>
            </el-checkbox-group>
          </template>
          <template v-else-if="currentAnswerMode === 'judge'">
            <el-radio-group v-model="form.answerJudge">
              <el-radio label="true">对</el-radio>
              <el-radio label="false">错</el-radio>
            </el-radio-group>
          </template>
          <template v-else-if="currentAnswerMode === 'fill'">
            <el-input v-model="form.answerFill" placeholder="填空答案，多空用 | 分隔，如 3|5" />
          </template>
          <template v-else-if="isSubjectiveType">
            <el-input v-model="form.answerShort" type="textarea" :rows="3" placeholder="参考答案" />
          </template>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="3" placeholder="选填，支持公式自动排版，教师版导出用" />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="配图">
          <image-upload v-model="form.imageUrls" :limit="5" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script>
import { getQuestion, addQuestion, updateQuestion, listKnowledgeTags, checkDuplicates } from '@/api/education/question'
import { listTextbookVersions, listTextbooks, getTextbookChapterTree } from '@/api/education/textbook'
import QbStemEditor from './components/QbStemEditor'
import OcrFormulaMathfield from './import/OcrFormulaMathfield'
import { OPTION_LABELS, parseQuestionOption, formatQuestionOption } from '@/utils/questionOptions'
import { buildMultiAnswerJson, parseMultiAnswerLetters } from '@/utils/questionAnswer'
import { getFormulaCategoryForSubject } from '@/utils/mathliveLocale'
import {
  QUESTION_TYPE_OPTIONS,
  getAnswerModeForType,
  isChoiceQuestionType,
  isSubjectiveQuestionType,
  isFillQuestionType,
  isJudgeQuestionType,
  loadQuestionTypeOptions,
  getDefaultQuestionType
} from '@/utils/questionTypes'

const DEFAULT_OPTIONS = () => OPTION_LABELS.slice(0, 4).map(label => ({ label, text: '' }))

export default {
  name: 'QuestionForm',
  components: { QbStemEditor, OcrFormulaMathfield },
  props: {
    subjectOptions: { type: Array, default: () => [] },
    questionTypeOptions: { type: Array, default: () => [] }
  },
  data() {
    const validateKnowledge = (rule, value, callback) => {
      if (!value || !value.length) {
        callback(new Error('请至少添加一个知识点'))
      } else if (value.length > 10) {
        callback(new Error('知识点不能超过10个'))
      } else if (value.some(t => t && t.length > 20)) {
        callback(new Error('单个知识点不超过20字'))
      } else {
        callback()
      }
    }
    const validateOptions = (rule, value, callback) => {
      if (!this.isChoiceType) {
        callback()
        return
      }
      const filled = (value || []).filter(o => o.text && o.text.trim())
      if (filled.length < 2) {
        callback(new Error('选择题至少填写2个选项'))
      } else {
        callback()
      }
    }
    const validateChapter = (rule, value, callback) => {
      if (!value || !value.length) {
        callback(new Error('请选择章节'))
      } else {
        callback()
      }
    }
    const validateAnswer = (rule, value, callback) => {
      const type = this.form.questionType
      const mode = getAnswerModeForType(type)
      if (mode === 'choice' && !this.form.answerSingle) {
        callback(new Error('请选择正确答案'))
      } else if (mode === 'multi' && (!this.form.answerMulti || !this.form.answerMulti.length)) {
        callback(new Error('请至少选择一个正确答案'))
      } else if (mode === 'judge' && !this.form.answerJudge) {
        callback(new Error('请选择对错'))
      } else if (mode === 'fill' && !this.form.answerFill) {
        callback(new Error('请填写填空答案'))
      } else if (this.currentAnswerMode === 'subjective' && !this.form.answerShort) {
        callback(new Error('请填写参考答案'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      loadingVersions: false,
      loadingTextbooks: false,
      loadingChapters: false,
      versions: [],
      textbooks: [],
      chapterTree: [],
      chapterOptions: [],
      cascaderProps: { value: 'value', label: 'label', children: 'children', emitPath: true },
      tagSuggestions: [],
      duplicateChecking: false,
      duplicateTimer: null,
      duplicateResult: { exactMatches: [], similarMatches: [] },
      pendingChapterId: undefined,
      localQuestionTypeOptions: [],
      form: this.emptyForm(),
      rules: {
        subjectId: [{ required: true, message: '请选择学科', trigger: 'change' }],
        schoolStage: [{ required: true, message: '请选择学段', trigger: 'change' }],
        versionId: [{ required: true, message: '请选择教材版本', trigger: 'change' }],
        textbookId: [{ required: true, message: '请选择教材', trigger: 'change' }],
        chapterPath: [{ validator: validateChapter, trigger: 'change' }],
        knowledgePoints: [{ validator: validateKnowledge, trigger: 'change' }],
        difficulty: [{ required: true, message: '请设定难度', trigger: 'change' }],
        questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
        content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
        optionList: [{ validator: validateOptions, trigger: 'blur' }],
        answerField: [{ validator: validateAnswer, trigger: 'change' }]
      }
    }
  },
  computed: {
    effectiveQuestionTypeOptions() {
      if (this.questionTypeOptions && this.questionTypeOptions.length) {
        return this.questionTypeOptions
      }
      if (this.localQuestionTypeOptions.length) {
        return this.localQuestionTypeOptions
      }
      return QUESTION_TYPE_OPTIONS
    },
    currentAnswerMode() {
      return getAnswerModeForType(this.form.questionType)
    },
    duplicateHintVisible() {
      const r = this.duplicateResult || {}
      return (r.exactMatches && r.exactMatches.length) || (r.similarMatches && r.similarMatches.length)
    },
    duplicateAlertTitle() {
      const exact = (this.duplicateResult.exactMatches || []).length
      const similar = (this.duplicateResult.similarMatches || []).length
      if (exact > 0) {
        const suffix = similar ? '\uff0c\u53e6\u6709 ' + similar + ' \u9053\u76f8\u4f3c\u9898' : ''
        return '\u53d1\u73b0 ' + exact + ' \u9053\u5b8c\u5168\u76f8\u540c\u9898\u76ee' + suffix
      }
      return '\u53d1\u73b0 ' + similar + ' \u9053\u76f8\u4f3c\u9898\u76ee\uff08\u8bf7\u6838\u5bf9\u662f\u5426\u91cd\u590d\u5f55\u5165\uff09'
    },
    isChoiceType() {
      if (this.currentAnswerMode) {
        return this.currentAnswerMode === 'choice' || this.currentAnswerMode === 'multi'
      }
      return isChoiceQuestionType(this.form.questionType)
    },
    isSubjectiveType() {
      if (this.currentAnswerMode) {
        return this.currentAnswerMode === 'subjective'
      }
      return isSubjectiveQuestionType(this.form.questionType)
    },
    formulaCategory() {
      const subject = (this.subjectOptions || []).find(s => s.subjectId === this.form.subjectId)
      return getFormulaCategoryForSubject(subject && subject.subjectName)
    }
  },
  created() {
    if (!this.questionTypeOptions || !this.questionTypeOptions.length) {
      loadQuestionTypeOptions().then(options => {
        this.localQuestionTypeOptions = options
      })
    }
  },
  methods: {
    emptyForm() {
      return {
        questionId: undefined,
        subjectId: undefined,
        schoolStage: '高中',
        versionId: undefined,
        textbookId: undefined,
        chapterPath: [],
        chapterId: undefined,
        chapterText: '',
        knowledgePoints: [],
        difficulty: 0.5,
        questionType: getDefaultQuestionType(),
        content: '',
        optionList: DEFAULT_OPTIONS(),
        answerSingle: '',
        answerMulti: [],
        answerJudge: 'true',
        answerFill: '',
        answerShort: '',
        analysis: '',
        imageUrls: ''
      }
    },
    reset(defaults) {
      this.form = this.emptyForm()
      this.versions = []
      this.textbooks = []
      this.chapterTree = []
      this.chapterOptions = []
      this.tagSuggestions = []
      this.duplicateResult = { exactMatches: [], similarMatches: [] }
      const d = defaults || {}
      if (d.subjectId) this.form.subjectId = d.subjectId
      if (d.schoolStage) this.form.schoolStage = d.schoolStage
      if (d.versionId) this.form.versionId = d.versionId
      if (d.textbookId) this.form.textbookId = d.textbookId
      if (d.chapterId) this.pendingChapterId = d.chapterId
      else this.pendingChapterId = undefined
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
        if (this.form.subjectId) {
          this.loadTagSuggestions(this.form.subjectId)
          this.loadVersions(true).then(() => {
            if (this.form.textbookId) {
              return this.loadChapterTree(true)
            }
            return null
          })
        }
      })
    },
    formatDifficulty(val) {
      return Number(val).toFixed(1)
    },
    pickQuestionType(value) {
      if (this.form.questionType === value) return
      this.form.questionType = value
      this.onTypeChange()
    },
    onSubjectChange(subjectId) {
      this.form.versionId = undefined
      this.form.textbookId = undefined
      this.clearChapter()
      this.textbooks = []
      this.loadVersions()
      this.loadTagSuggestions(subjectId)
      this.scheduleDuplicateCheck()
    },
    onStageChange() {
      this.form.versionId = undefined
      this.form.textbookId = undefined
      this.clearChapter()
      this.textbooks = []
      this.loadVersions()
    },
    onVersionChange() {
      this.form.textbookId = undefined
      this.clearChapter()
      this.loadTextbooks()
    },
    onTextbookChange() {
      this.clearChapter()
      this.loadChapterTree()
    },
    clearChapter() {
      this.form.chapterPath = []
      this.form.chapterId = undefined
      this.form.chapterText = ''
      this.chapterOptions = []
      this.chapterTree = []
    },
    loadVersions(keepSelection) {
      if (!this.form.subjectId || !this.form.schoolStage) {
        this.versions = []
        return Promise.resolve()
      }
      this.loadingVersions = true
      return listTextbookVersions(this.form.subjectId, this.form.schoolStage).then(res => {
        this.versions = res.data || []
        if (!keepSelection || !this.versions.some(v => v.versionId === this.form.versionId)) {
          this.form.versionId = this.versions[0] && this.versions[0].versionId
        }
        return this.loadTextbooks(keepSelection)
      }).finally(() => { this.loadingVersions = false })
    },
    loadTextbooks(keepSelection) {
      if (!this.form.versionId) {
        this.textbooks = []
        return Promise.resolve()
      }
      this.loadingTextbooks = true
      return listTextbooks(this.form.versionId).then(res => {
        this.textbooks = res.data || []
        if (!keepSelection || !this.textbooks.some(t => t.textbookId === this.form.textbookId)) {
          this.form.textbookId = this.textbooks[0] && this.textbooks[0].textbookId
        }
        if (this.form.textbookId) {
          return this.loadChapterTree(keepSelection)
        }
        return null
      }).finally(() => { this.loadingTextbooks = false })
    },
    loadChapterTree(keepSelection) {
      if (!this.form.textbookId) {
        this.chapterOptions = []
        return Promise.resolve()
      }
      this.loadingChapters = true
      return getTextbookChapterTree(this.form.textbookId, this.form.subjectId).then(res => {
        this.chapterTree = res.data || []
        this.chapterOptions = this.buildCascaderOptions(this.chapterTree)
        const targetId = keepSelection ? (this.pendingChapterId || this.form.chapterId) : undefined
        if (targetId) {
          const path = this.findChapterPath(this.chapterTree, targetId)
          if (path) {
            this.form.chapterPath = path
            this.applyChapterFromPath(path)
          }
          this.pendingChapterId = undefined
        }
      }).finally(() => { this.loadingChapters = false })
    },
    buildCascaderOptions(nodes) {
      return (nodes || [])
        .filter(n => n.id !== 'all')
        .map(n => {
          const children = (n.children || []).filter(c => c.id !== 'all')
          const item = { value: n.id, label: n.label }
          if (children.length) {
            item.children = children.map(c => ({ value: c.id, label: c.label }))
          }
          return item
        })
    },
    findChapterPath(nodes, targetId, path) {
      const acc = path || []
      for (const n of nodes || []) {
        if (n.id === 'all') continue
        const cur = acc.concat([n.id])
        if (String(n.id) === String(targetId)) return cur
        if (n.children && n.children.length) {
          const found = this.findChapterPath(n.children, targetId, cur)
          if (found) return found
        }
      }
      return null
    },
    findNodeById(nodes, targetId) {
      for (const n of nodes || []) {
        if (String(n.id) === String(targetId)) return n
        if (n.children && n.children.length) {
          const found = this.findNodeById(n.children, targetId)
          if (found) return found
        }
      }
      return null
    },
    onChapterChange(path) {
      if (!path || !path.length) {
        this.form.chapterId = undefined
        this.form.chapterText = ''
        return
      }
      this.applyChapterFromPath(path)
    },
    applyChapterFromPath(path) {
      const chapterId = path[path.length - 1]
      this.form.chapterId = chapterId
      if (path.length === 1) {
        const node = this.findNodeById(this.chapterTree, chapterId)
        this.form.chapterText = node ? node.label : ''
        return
      }
      const parent = this.findNodeById(this.chapterTree, path[0])
      const leaf = parent && parent.children
        ? parent.children.find(c => String(c.id) === String(chapterId))
        : this.findNodeById(this.chapterTree, chapterId)
      const parentLabel = parent ? parent.label : ''
      const leafLabel = leaf ? leaf.label : ''
      this.form.chapterText = parentLabel && leafLabel ? parentLabel + ' > ' + leafLabel : (leafLabel || parentLabel)
    },
    briefContent(text) {
      if (!text) return ''
      const s = String(text).replace(/\s+/g, ' ').trim()
      return s.length > 80 ? s.slice(0, 80) + '\u2026' : s
    },
    formatSimilarity(val) {
      if (val == null) return ''
      return Math.round(val * 100) + '%'
    },
    scheduleDuplicateCheck() {
      if (this.duplicateTimer) clearTimeout(this.duplicateTimer)
      this.duplicateTimer = setTimeout(() => this.runDuplicateCheck(), 400)
    },
    runDuplicateCheck() {
      if (!this.form.subjectId || !this.form.content || !this.form.content.trim()) {
        this.duplicateResult = { exactMatches: [], similarMatches: [] }
        return Promise.resolve()
      }
      this.duplicateChecking = true
      return checkDuplicates({
        subjectId: this.form.subjectId,
        content: this.form.content.trim(),
        questionId: this.form.questionId
      }).then(res => {
        const data = res.data || {}
        this.duplicateResult = {
          exactMatches: data.exactMatches || [],
          similarMatches: data.similarMatches || []
        }
      }).catch(() => {
        this.duplicateResult = { exactMatches: [], similarMatches: [] }
      }).finally(() => { this.duplicateChecking = false })
    },
    confirmDuplicateSave() {
      const exact = (this.duplicateResult.exactMatches || []).length
      if (!exact) return Promise.resolve()
      return this.$modal.confirm('当前学科已存在完全相同题干，确认仍要保存？')
    },
    onKnowledgeChange(tags) {
      if (tags.length > 10) {
        this.form.knowledgePoints = tags.slice(0, 10)
        this.$message.warning('知识点最多10个')
      }
    },
    onTypeChange() {
      this.form.answerSingle = ''
      this.form.answerMulti = []
      this.form.answerJudge = 'true'
      this.form.answerFill = ''
      this.form.answerShort = ''
      if (this.isChoiceType && this.form.optionList.length < 2) {
        this.form.optionList = DEFAULT_OPTIONS()
      }
    },
    onOptionTextInput(idx, val) {
      if (!this.form.optionList[idx]) return
      this.$set(this.form.optionList[idx], 'text', val || '')
    },
    addOption() {
      const next = OPTION_LABELS[this.form.optionList.length]
      if (next) this.form.optionList.push({ label: next, text: '' })
    },
    removeOption(idx) {
      const removed = this.form.optionList[idx].label
      this.form.optionList.splice(idx, 1)
      this.form.optionList.forEach((o, i) => { o.label = OPTION_LABELS[i] })
      if (this.form.answerSingle === removed) this.form.answerSingle = ''
      this.form.answerMulti = this.form.answerMulti.filter(l => l !== removed)
    },
    loadTagSuggestions(subjectId, keyword) {
      if (!subjectId) {
        this.tagSuggestions = []
        return
      }
      listKnowledgeTags({ subjectId, keyword }).then(res => {
        this.tagSuggestions = res.data || []
      })
    },
    load(questionId) {
      this.reset()
      if (!questionId) return Promise.resolve()
      this.loading = true
      return getQuestion(questionId).then(res => {
        const data = res.data || {}
        const catalog = data.params || {}
        this.form.questionId = data.questionId
        this.form.subjectId = data.subjectId
        if (catalog.schoolStage) this.form.schoolStage = catalog.schoolStage
        if (catalog.versionId) this.form.versionId = catalog.versionId
        if (catalog.textbookId) this.form.textbookId = catalog.textbookId
        this.form.chapterText = data.chapterText || ''
        this.pendingChapterId = data.chapterId
        this.form.knowledgePoints = this.parseJsonArray(data.knowledgePoints)
        this.form.difficulty = data.difficulty != null ? Number(data.difficulty) : 0.5
        this.form.questionType = data.questionType || getDefaultQuestionType()
        this.form.content = data.content || ''
        this.form.analysis = data.analysis || ''
        this.form.optionList = this.parseOptions(data.options)
        this.applyAnswerFromApi(data.correctAnswer, data.questionType)
        this.form.imageUrls = this.parseImagesToUpload(data.images)
        this.loadTagSuggestions(data.subjectId)
        return this.loadVersions(true).then(() => {
          if (this.form.textbookId) {
            return this.loadChapterTree(true)
          }
          return null
        })
      }).finally(() => { this.loading = false })
    },
    parseJsonArray(raw) {
      if (!raw) return []
      if (Array.isArray(raw)) return raw
      try {
        const arr = JSON.parse(raw)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    parseOptions(raw) {
      const arr = this.parseJsonArray(raw)
      if (!arr.length) return DEFAULT_OPTIONS()
      return arr.map((text, i) => parseQuestionOption(text, i))
    },
    parseImagesToUpload(raw) {
      const arr = this.parseJsonArray(raw)
      return arr.join(',')
    },
    applyAnswerFromApi(raw, type) {
      if (raw == null || raw === '') return
      let val = raw
      try { val = JSON.parse(raw) } catch (e) { /* plain */ }
      const mode = getAnswerModeForType(type)
      if (mode === 'choice') {
        this.form.answerSingle = String(val).replace(/"/g, '')
      } else if (mode === 'multi') {
        this.form.answerMulti = parseMultiAnswerLetters(val)
      } else if (mode === 'judge') {
        this.form.answerJudge = String(val) === 'false' ? 'false' : 'true'
      } else if (mode === 'fill') {
        this.form.answerFill = Array.isArray(val) ? val.join('|') : String(val)
      } else if (isSubjectiveQuestionType(type)) {
        this.form.answerShort = String(val)
      }
    },
    buildOptionsJson() {
      if (!this.isChoiceType) return null
      const arr = this.form.optionList
        .filter(o => o.text && o.text.trim())
        .map((o, i) => formatQuestionOption(o.label, o.text, i))
      return JSON.stringify(arr)
    },
    buildAnswerJson() {
      const mode = getAnswerModeForType(this.form.questionType)
      if (mode === 'choice') return JSON.stringify(this.form.answerSingle)
      if (mode === 'multi') return buildMultiAnswerJson(this.form.answerMulti)
      if (mode === 'judge') return JSON.stringify(this.form.answerJudge)
      if (mode === 'fill') {
        const parts = this.form.answerFill.split('|').map(s => s.trim()).filter(Boolean)
        return parts.length > 1 ? JSON.stringify(parts) : JSON.stringify(parts[0] || '')
      }
      if (isSubjectiveQuestionType(this.form.questionType)) return JSON.stringify(this.form.answerShort)
      return JSON.stringify(this.form.answerShort)
    },
    buildImagesJson() {
      if (!this.form.imageUrls) return null
      const urls = this.form.imageUrls.split(',').map(s => s.trim()).filter(Boolean)
      return urls.length ? JSON.stringify(urls) : null
    },
    buildPayload() {
      return {
        questionId: this.form.questionId,
        subjectId: this.form.subjectId,
        chapterId: this.form.chapterId,
        textbookId: this.form.textbookId,
        chapterText: this.form.chapterText,
        knowledgePoints: JSON.stringify(this.form.knowledgePoints),
        difficulty: this.form.difficulty,
        questionType: this.form.questionType,
        content: this.form.content,
        options: this.buildOptionsJson(),
        correctAnswer: this.buildAnswerJson(),
        analysis: this.form.analysis,
        images: this.buildImagesJson(),
        sourceType: 'manual'
      }
    },
    submit() {
      return new Promise((resolve, reject) => {
        this.$refs.form.validate(valid => {
          if (!valid) {
            reject(new Error('validation'))
            return
          }
          this.runDuplicateCheck().then(() => this.confirmDuplicateSave()).then(() => {
            const payload = this.buildPayload()
            const req = payload.questionId ? updateQuestion(payload) : addQuestion(payload)
            return req.then(() => resolve(payload))
          }).catch(err => reject(err))
        })
      })
    }
  }
}
</script>

<style scoped>
.difficulty-row {
  display: flex;
  align-items: center;
}
.type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.type-tag {
  display: inline-block;
  padding: 4px 12px;
  line-height: 20px;
  border-radius: 14px;
  color: #606266;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
}
.type-tag:hover {
  color: #409eff;
  background: #f0f7ff;
}
.type-tag.active {
  color: #fff;
  background: #409eff;
}
.option-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
}
.option-label {
  width: 28px;
  font-weight: 600;
  flex-shrink: 0;
  line-height: 40px;
}
.option-formula-wrap {
  flex: 1;
  min-width: 0;
  margin-right: 8px;
}
.duplicate-alert {
  margin-bottom: 12px;
}
.dup-list {
  margin-top: 4px;
}
.dup-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
}
.dup-code {
  color: #606266;
  flex-shrink: 0;
}
.dup-content {
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.content-formula-preview {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fafbfc;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
}
.preview-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
</style>
