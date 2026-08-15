<template>
  <div class="app-container education-page ocr-import-page">
    <div class="page-header">
      <div>
        <div class="page-title">OCR 拍照导入</div>
        <div class="page-desc">上传后先框选题目区域再识别；默认使用 PaddleOCR 识别中文题干与选项；含大量公式时可改用 mixed 模式（Pix2Text）</div>
      </div>
      <el-button icon="el-icon-back" size="small" @click="goBack">返回新增试题</el-button>
    </div>

    <el-alert
      v-if="stubWarning"
      :title="stubWarning"
      type="warning"
      show-icon
      :closable="false"
      class="stub-alert"
    />

    <el-alert
      v-if="qualityHint"
      :title="qualityHint"
      type="info"
      show-icon
      :closable="true"
      class="quality-alert"
    />

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never" class="upload-card">
          <el-form :inline="true" size="small">
            <el-form-item label="学科">
              <el-select v-model="subjectId" filterable placeholder="请选择学科" style="width:160px">
                <el-option v-for="item in subjectOptions" :key="item.subjectId" :label="item.subjectName" :value="item.subjectId" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-upload
                action="#"
                :show-file-list="false"
                :http-request="() => {}"
                :before-upload="openCropDialog"
                accept="image/*"
              >
                <el-button type="primary" icon="el-icon-camera" :loading="uploading">选择图片</el-button>
              </el-upload>
              <span class="upload-tip">建议框选仅含题干与选项的区域</span>
            </el-form-item>
          </el-form>
          <div v-if="imageUrl" class="preview-box">
            <img :src="imagePreviewUrl" alt="ocr" class="preview-img" />
            <div class="meta-row">
              <el-tag size="mini" type="info">OCR: {{ provider }}</el-tag>
              <el-tag size="mini" :type="confidenceTagType">平均置信度 {{ avgConfidenceText }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card v-if="lines.length" shadow="never">
          <div slot="header" class="card-header">
            <span>识别结果校对</span>
            <span class="hint">与新增试题相同编辑器；低置信度行高亮；点击工具栏 Σ 可插入公式</span>
          </div>
          <div class="line-list">
            <div
              v-for="(line, idx) in lines"
              :key="idx"
              class="line-item"
              :class="{ low: isLowConfidence(line.confidence) }"
            >
              <span class="conf">{{ formatConfidence(line.confidence) }}</span>
              <div class="line-body">
                <qb-stem-editor
                  v-model="line.text"
                  :min-height="lineEditorMinHeight"
                  :formula-category="formulaCategory"
                  placeholder="请校对识别内容，点击工具栏「Σ」可插入公式"
                />
              </div>
            </div>
          </div>
        </el-card>

        <el-card v-if="draftId" shadow="never" class="meta-card">
          <div slot="header">标注入库</div>
          <el-form ref="metaForm" :model="meta" :rules="metaRules" label-width="80px" size="small">
            <el-form-item label="题型" prop="questionType">
              <el-select v-model="meta.questionType" placeholder="题型" style="width:160px">
                <el-option v-for="item in questionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-tag v-if="predictedType" size="mini" type="success" style="margin-left:8px">预判: {{ typeLabel(predictedType) }}</el-tag>
            </el-form-item>
            <el-form-item label="难度" prop="difficulty">
              <el-slider v-model="meta.difficulty" :min="0.1" :max="1" :step="0.1" show-input style="width:280px" />
            </el-form-item>
            <el-form-item label="版本" prop="versionId">
              <el-select
                v-model="meta.versionId"
                filterable
                placeholder="请选择版本"
                style="width:100%"
                :loading="loadingVersions"
                :disabled="!subjectId"
                @change="onVersionChange"
              >
                <el-option v-for="item in versions" :key="item.versionId" :label="item.versionName" :value="item.versionId" />
              </el-select>
            </el-form-item>
            <el-form-item label="教材" prop="textbookId">
              <el-select
                v-model="meta.textbookId"
                filterable
                placeholder="请选择教材"
                style="width:100%"
                :loading="loadingTextbooks"
                :disabled="!meta.versionId"
                @change="onTextbookChange"
              >
                <el-option v-for="item in textbooks" :key="item.textbookId" :label="item.textbookName" :value="item.textbookId" />
              </el-select>
            </el-form-item>
            <el-form-item label="章节" prop="chapterPath">
              <div class="chapter-row">
                <el-cascader
                  v-model="meta.chapterPath"
                  :options="chapterOptions"
                  :props="cascaderProps"
                  filterable
                  clearable
                  style="width:100%"
                  placeholder="请选择章节"
                  :disabled="!meta.textbookId"
                  @change="onChapterChange"
                />
                <el-button size="small" type="primary" plain :disabled="!meta.textbookId" :loading="matchingChapter" @click="autoMatchOcrChapter">自动匹配</el-button>
              </div>
            </el-form-item>
            <el-form-item label="知识点" prop="knowledgeTags">
              <el-select v-model="meta.knowledgeTags" multiple filterable allow-create default-first-option placeholder="回车添加" style="width:100%">
                <el-option v-for="tag in meta.knowledgeTags" :key="tag" :label="tag" :value="tag" />
              </el-select>
            </el-form-item>
            <el-form-item label="题干" prop="content">
              <qb-stem-editor
                v-model="meta.content"
                :min-height="180"
                :formula-category="formulaCategory"
                placeholder="请输入题干，点击工具栏「Σ」可插入公式"
              />
              <div v-if="meta.content" class="content-formula-preview">
                <div class="preview-label">排版预览</div>
                <qb-formula-text :text="meta.content" />
              </div>
              <div class="stem-toolbar">
                <el-button size="mini" type="primary" plain icon="el-icon-refresh" @click="syncStemFromLines">从校对结果同步题干</el-button>
                <span v-if="stemOutOfSync" class="stem-sync-hint">校对行已修改，题干未同步</span>
              </div>
              <div v-if="imagePreviewUrl" class="stem-figure-box">
                <div class="stem-figure-toolbar">
                  <span class="stem-figure-label">题目插图</span>
                  <el-button size="mini" type="primary" plain icon="el-icon-crop" @click="openFigureCropDialog">手动截取插图</el-button>
                  <el-button v-if="figureImageUrl" size="mini" type="text" @click="clearFigureImage">清除插图</el-button>
                </div>
                <img v-if="figurePreviewUrl" :src="figurePreviewUrl" alt="figure" class="stem-figure-img" />
                <el-tag v-if="figureImageUrl" size="mini" type="success" class="figure-saved-tag">已截取插图</el-tag>
                <div v-else class="stem-figure-placeholder">
                  <img :src="imagePreviewUrl" alt="source" class="stem-figure-dim" />
                  <p>请框选不倒翁、几何图等示意图区域，入库后在题库中显示</p>
                </div>
              </div>
            </el-form-item>
            <el-form-item v-if="isChoiceType" label="选项">
              <qb-stem-editor
                v-model="meta.optionsText"
                :min-height="140"
                :formula-category="formulaCategory"
                placeholder="每行一个选项，如 A. xxx"
              />
            </el-form-item>
            <el-form-item label="答案" prop="correctAnswer">
              <el-input v-model="meta.correctAnswer" placeholder="单选 A；判断 true/false" />
            </el-form-item>
            <el-form-item label="解析">
              <qb-stem-editor
                v-model="meta.analysis"
                :min-height="120"
                :formula-category="formulaCategory"
                placeholder="请输入解析，点击工具栏「Σ」可插入公式"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="committing" @click="handleCommit">确认入库</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      title="框选题目区域"
      :visible.sync="cropOpen"
      width="860px"
      append-to-body
      @opened="onCropDialogOpened"
      @close="onCropDialogClose"
    >
      <el-alert
        title="请拖动选框，只保留题干和 A/B/C/D 选项文字，尽量排除页面标签、插图和按钮，可显著提升识别率"
        type="info"
        :closable="false"
        show-icon
        class="crop-hint"
      />
      <div v-if="cropVisible" class="crop-wrap">
        <vue-cropper
          ref="cropper"
          :img="cropImg"
          :info="true"
          :auto-crop="true"
          :fixed="false"
          :fixed-box="false"
          :can-move-box="true"
          :output-type="'jpeg'"
          :output-size="1"
          :full="false"
          :high="true"
        />
      </div>
      <div slot="footer">
        <el-button @click="cropOpen = false">取消</el-button>
        <el-button icon="el-icon-refresh-left" @click="rotateCrop(-1)">左旋</el-button>
        <el-button icon="el-icon-refresh-right" @click="rotateCrop(1)">右旋</el-button>
        <el-button type="primary" :loading="uploading" @click="submitCrop">开始识别</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="手动截取题目插图"
      :visible.sync="figureCropOpen"
      width="860px"
      append-to-body
      @opened="onFigureCropDialogOpened"
      @close="onFigureCropDialogClose"
    >
      <el-alert
        title="拖动选框框选不倒翁、几何图形等示意图（尽量只保留插图，不要包含选项文字）"
        type="info"
        :closable="false"
        show-icon
        class="crop-hint"
      />
      <div v-if="figureCropVisible" class="crop-wrap">
        <vue-cropper
          ref="figureCropper"
          :img="figureCropImg"
          :info="true"
          :auto-crop="true"
          :fixed="false"
          :fixed-box="false"
          :can-move-box="true"
          :output-type="'png'"
          :output-size="1"
          :full="false"
          :high="true"
        />
      </div>
      <div slot="footer">
        <el-button @click="figureCropOpen = false">取消</el-button>
        <el-button icon="el-icon-refresh-left" @click="rotateFigureCrop(-1)">左旋</el-button>
        <el-button icon="el-icon-refresh-right" @click="rotateFigureCrop(1)">右旋</el-button>
        <el-button type="primary" :loading="figureUploading" @click="submitFigureCrop">插入插图</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { VueCropper } from 'vue-cropper'
import { getToken } from '@/utils/auth'
import { listSubject } from '@/api/education/subject'
import { recognizeOcr, commitOcr, getOcrDraft, uploadFigureImage, saveDraftFigure } from '@/api/education/questionOcr'
import { listTextbookVersions, listTextbooks, getTextbookChapterTree } from '@/api/education/textbook'
import { matchImportChapters } from '@/api/education/questionImport'
import { normalizeOcrLatex } from '@/utils/questionFormula'
import { buildMultiAnswerJson } from '@/utils/questionAnswer'
import { getFormulaCategoryForSubject } from '@/utils/mathliveLocale'
import QbStemEditor from '../components/QbStemEditor'

import { getQuestionTypeLabel, isChoiceQuestionType, loadQuestionTypeOptions, getDefaultQuestionType, getAnswerModeForType } from '@/utils/questionTypes'

export default {
  name: 'OcrImport',
  components: { VueCropper, QbStemEditor },
  data() {
    const validateChapterPath = (rule, value, callback) => {
      if (!value || !value.length) {
        callback(new Error('请选择章节'))
      } else {
        callback()
      }
    }
    return {
      validateChapterPath,
      subjectOptions: [],
      subjectId: null,
      schoolStage: '\u9ad8\u4e2d',
      versions: [],
      textbooks: [],
      chapterTree: [],
      chapterOptions: [],
      loadingVersions: false,
      loadingTextbooks: false,
      loadingChapters: false,
      matchingChapter: false,
      cascaderProps: { expandTrigger: 'hover' },
      cropOpen: false,
      cropVisible: false,
      cropImg: '',
      figureCropOpen: false,
      figureCropVisible: false,
      figureCropImg: '',
      figureCropObjectUrl: '',
      figureImageUrl: '',
      figureUploading: false,
      pendingFileName: 'ocr.jpg',
      uploading: false,
      committing: false,
      draftId: null,
      imageUrl: '',
      provider: 'stub',
      stubWarning: '',
      qualityHint: '',
      lines: [],
      confidenceThreshold: 0.8,
      avgConfidence: 0,
      predictedType: '',
      questionTypeOptions: [],
      meta: {
        questionType: 'single',
        difficulty: 0.5,
        versionId: undefined,
        textbookId: undefined,
        chapterPath: [],
        chapterId: undefined,
        chapterText: '',
        knowledgeTags: [],
        content: '',
        optionsText: '',
        correctAnswer: 'A',
        analysis: ''
      },
      metaRules: {
        questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
        versionId: [{ required: true, message: '请选择版本', trigger: 'change' }],
        textbookId: [{ required: true, message: '请选择教材', trigger: 'change' }],
        chapterPath: [{ validator: validateChapterPath, trigger: 'change' }],
        knowledgeTags: [{ type: 'array', required: true, min: 1, message: '请添加知识点', trigger: 'change' }],
        content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
        correctAnswer: [{ required: true, message: '请输入答案', trigger: 'blur' }]
      },
      lineEditorMinHeight: 72
    }
  },
  computed: {
    imagePreviewUrl() {
      if (!this.imageUrl) return ''
      if (this.imageUrl.startsWith('http')) return this.imageUrl
      return process.env.VUE_APP_BASE_API + this.imageUrl
    },
    figurePreviewUrl() {
      if (!this.figureImageUrl) return ''
      if (this.figureImageUrl.startsWith('http')) return this.figureImageUrl
      return process.env.VUE_APP_BASE_API + this.figureImageUrl
    },
    isChoiceType() {
      return isChoiceQuestionType(this.meta.questionType)
    },
    avgConfidenceText() {
      return (this.avgConfidence * 100).toFixed(1) + '%'
    },
    confidenceTagType() {
      return this.avgConfidence >= this.confidenceThreshold ? 'success' : 'warning'
    },
    stemOutOfSync() {
      if (!this.lines.length) return false
      const optionRe = /^[A-Da-d][\.\uFF0E\u3001\)\uFF09:]\s*/
      const stem = this.lines.map(l => (l.text || '').trim()).filter(t => t && !optionRe.test(t)).join('\n')
      return stem && stem !== (this.meta.content || '').trim()
    },
    formulaCategory() {
      const subject = (this.subjectOptions || []).find(s => s.subjectId === this.subjectId)
      return getFormulaCategoryForSubject(subject && subject.subjectName)
    }
  },
  watch: {
    subjectId(val, oldVal) {
      if (val !== oldVal) {
        this.resetCatalog()
        if (val) this.loadVersions(false)
      }
    }
  },
  created() {
    const q = this.$route.query || {}
    if (q.schoolStage) this.schoolStage = q.schoolStage
    if (q.subjectId) this.subjectId = Number(q.subjectId) || q.subjectId
    this.loadDraftFromQuery()
    listSubject({ pageNum: 1, pageSize: 200 }).then(res => {
      this.subjectOptions = res.rows || []
      if (this.subjectId) this.loadVersions(false)
    })
    loadQuestionTypeOptions().then(options => {
      this.questionTypeOptions = options
      if (!options.some(item => item.value === this.meta.questionType)) {
        this.meta.questionType = options.length ? options[0].value : getDefaultQuestionType()
      }
    })
  },
  methods: {
    loadDraftFromQuery() {
      const draftId = this.$route.query.draftId
      if (!draftId) return
      getOcrDraft(draftId).then(res => {
        this.applyRecognizeResult(res.data || {})
        if (res.data && res.data.subjectId) {
          this.subjectId = res.data.subjectId
          this.$nextTick(() => this.loadVersions(false))
        }
      })
    },
    resetCatalog() {
      this.meta.versionId = undefined
      this.meta.textbookId = undefined
      this.clearChapter()
      this.versions = []
      this.textbooks = []
    },
    onVersionChange() {
      this.meta.textbookId = undefined
      this.clearChapter()
      this.loadTextbooks(false)
    },
    onTextbookChange() {
      this.clearChapter()
      this.loadChapterTree(false)
    },
    clearChapter() {
      this.meta.chapterPath = []
      this.meta.chapterId = undefined
      this.meta.chapterText = ''
      this.chapterOptions = []
      this.chapterTree = []
    },
    loadVersions(keepSelection) {
      if (!this.subjectId) {
        this.versions = []
        return Promise.resolve()
      }
      this.loadingVersions = true
      return listTextbookVersions(this.subjectId, this.schoolStage).then(res => {
        this.versions = res.data || []
        if (!keepSelection || !this.versions.some(v => v.versionId === this.meta.versionId)) {
          this.meta.versionId = this.versions[0] && this.versions[0].versionId
        }
        return this.loadTextbooks(keepSelection)
      }).finally(() => { this.loadingVersions = false })
    },
    loadTextbooks(keepSelection) {
      if (!this.meta.versionId) {
        this.textbooks = []
        return Promise.resolve()
      }
      this.loadingTextbooks = true
      return listTextbooks(this.meta.versionId).then(res => {
        this.textbooks = res.data || []
        if (!keepSelection || !this.textbooks.some(t => t.textbookId === this.meta.textbookId)) {
          this.meta.textbookId = this.textbooks[0] && this.textbooks[0].textbookId
        }
        if (this.meta.textbookId) {
          return this.loadChapterTree(keepSelection)
        }
        return null
      }).finally(() => { this.loadingTextbooks = false })
    },
    loadChapterTree(keepSelection) {
      if (!this.meta.textbookId || !this.subjectId) {
        this.chapterOptions = []
        return Promise.resolve()
      }
      this.loadingChapters = true
      return getTextbookChapterTree(this.meta.textbookId, this.subjectId).then(res => {
        this.chapterTree = res.data || []
        this.chapterOptions = this.buildCascaderOptions(this.chapterTree)
        if (keepSelection && this.meta.chapterId) {
          const path = this.findChapterPath(this.chapterTree, this.meta.chapterId)
          if (path) {
            this.meta.chapterPath = path
            this.applyChapterFromPath(path)
          }
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
        this.meta.chapterId = undefined
        this.meta.chapterText = ''
        return
      }
      this.applyChapterFromPath(path)
    },
    applyChapterFromPath(path) {
      const chapterId = path[path.length - 1]
      this.meta.chapterId = chapterId
      if (path.length === 1) {
        const node = this.findNodeById(this.chapterTree, chapterId)
        this.meta.chapterText = node ? node.label : ''
        return
      }
      const parent = this.findNodeById(this.chapterTree, path[0])
      const leaf = parent && parent.children
        ? parent.children.find(c => String(c.id) === String(chapterId))
        : this.findNodeById(this.chapterTree, chapterId)
      const parentLabel = parent ? parent.label : ''
      const leafLabel = leaf ? leaf.label : ''
      this.meta.chapterText = parentLabel && leafLabel ? parentLabel + ' > ' + leafLabel : (leafLabel || parentLabel)
    },
    buildOcrChapterHints() {
      const hints = []
      if (this.meta.chapterText) hints.push(this.meta.chapterText)
      const content = (this.meta.content || '').replace(/\s+/g, ' ').trim()
      if (content) {
        const firstLine = content.split(/[\n。]/)[0].trim()
        if (firstLine && firstLine.length <= 40) hints.push(firstLine)
      }
      return hints.filter((h, i, arr) => h && arr.indexOf(h) === i)
    },
    autoMatchOcrChapter() {
      if (!this.meta.textbookId) return
      const hints = this.buildOcrChapterHints()
      if (!hints.length) {
        this.$modal.msgWarning('请先填写题干或章节关键词')
        return
      }
      this.matchingChapter = true
      matchImportChapters({ textbookId: this.meta.textbookId, hints }).then(res => {
        const hit = (res.data || []).find(r => r.matched && r.chapterId)
        if (!hit) {
          this.$modal.msgWarning('未能自动匹配章节，请手动选择')
          return
        }
        const path = this.findChapterPath(this.chapterTree, hit.chapterId)
        if (path) {
          this.meta.chapterPath = path
          this.applyChapterFromPath(path)
        } else {
          this.meta.chapterId = hit.chapterId
          this.meta.chapterText = hit.chapterText || hit.chapterName || ''
        }
        this.$modal.msgSuccess('已匹配章节：' + (hit.chapterText || hit.chapterName))
      }).finally(() => { this.matchingChapter = false })
    },
    goBack() {
      this.$router.push({ path: '/admin/question-bank/question-create' })
    },
    openCropDialog(file) {
      if (!file.type.startsWith('image/')) {
        this.$modal.msgError('请上传图片文件')
        return false
      }
      this.pendingFileName = file.name || 'ocr.jpg'
      const reader = new FileReader()
      reader.onload = e => {
        this.cropImg = e.target.result
        this.cropOpen = true
      }
      reader.readAsDataURL(file)
      return false
    },
    onCropDialogOpened() {
      this.cropVisible = true
      this.$nextTick(() => {
        if (this.$refs.cropper) this.$refs.cropper.refresh()
      })
    },
    onCropDialogClose() {
      this.cropVisible = false
      this.cropImg = ''
    },
    rotateCrop(step) {
      if (!this.$refs.cropper) return
      if (step < 0) this.$refs.cropper.rotateLeft()
      else this.$refs.cropper.rotateRight()
    },
    submitCrop() {
      if (!this.$refs.cropper) return
      this.uploading = true
      this.$refs.cropper.getCropBlob(blob => {
        const formData = new FormData()
        formData.append('file', blob, this.pendingFileName.replace(/\.\w+$/, '') + '.jpg')
        if (this.subjectId) formData.append('subjectId', this.subjectId)
        recognizeOcr(formData).then(res => {
          this.cropOpen = false
          if (res.code !== 200) {
            this.$modal.msgError(res.msg || '识别失败')
            return
          }
          this.applyRecognizeResult(res.data || {})
        }).catch(() => {
          this.$modal.msgError('上传失败')
        }).finally(() => {
          this.uploading = false
        })
      }, 'image/jpeg', 0.92)
    },
    openFigureCropDialog() {
      if (!this.imagePreviewUrl) {
        this.$modal.msgError('请先上传并识别图片')
        return
      }
      fetch(this.imagePreviewUrl, {
        headers: { Authorization: 'Bearer ' + getToken() }
      }).then(resp => {
        if (!resp.ok) throw new Error('load image failed')
        return resp.blob()
      }).then(blob => {
        if (this.figureCropObjectUrl) {
          URL.revokeObjectURL(this.figureCropObjectUrl)
        }
        this.figureCropObjectUrl = URL.createObjectURL(blob)
        this.figureCropImg = this.figureCropObjectUrl
        this.figureCropOpen = true
      }).catch(() => {
        this.$modal.msgError('加载原图失败，请重试')
      })
    },
    onFigureCropDialogOpened() {
      this.figureCropVisible = true
      this.$nextTick(() => {
        if (this.$refs.figureCropper) this.$refs.figureCropper.refresh()
      })
    },
    onFigureCropDialogClose() {
      this.figureCropVisible = false
      this.figureCropImg = ''
      if (this.figureCropObjectUrl) {
        URL.revokeObjectURL(this.figureCropObjectUrl)
        this.figureCropObjectUrl = ''
      }
    },
    rotateFigureCrop(step) {
      if (!this.$refs.figureCropper) return
      if (step < 0) this.$refs.figureCropper.rotateLeft()
      else this.$refs.figureCropper.rotateRight()
    },
    submitFigureCrop() {
      if (!this.$refs.figureCropper) return
      this.figureUploading = true
      this.$refs.figureCropper.getCropBlob(blob => {
        const formData = new FormData()
        formData.append('file', blob, 'figure-' + Date.now() + '.png')
        uploadFigureImage(formData).then(res => {
          const path = res.fileName || (res.url ? res.url.replace(/^https?:\/\/[^/]+/, '') : '')
          if (!path) {
            this.$modal.msgError('插图上传失败：未返回文件路径')
            return
          }
          this.figureImageUrl = path
          this.cacheFigurePath()
          const saveDraft = this.draftId
            ? saveDraftFigure(this.draftId, path).catch(() => {
              this.$modal.msgWarning('插图已上传，但草稿保存失败，请重新截取或联系管理员')
            })
            : Promise.resolve()
          return saveDraft.then(() => {
            this.figureCropOpen = false
            this.$modal.msgSuccess('插图已截取，确认入库后将显示截取图（非整页原图）')
          })
        }).catch(() => {
          this.$modal.msgError('插图上传失败')
        }).finally(() => {
          this.figureUploading = false
        })
      }, 'image/png', 1)
    },
    cacheFigurePath() {
      if (this.draftId && this.figureImageUrl) {
        sessionStorage.setItem('ocr_figure_' + this.draftId, this.figureImageUrl)
      }
    },
    restoreFigurePath() {
      if (!this.draftId || this.figureImageUrl) return
      const cached = sessionStorage.getItem('ocr_figure_' + this.draftId)
      if (cached) {
        this.figureImageUrl = cached
        return
      }
      const fromDraft = this.$route.query.figurePath
      if (fromDraft) this.figureImageUrl = fromDraft
    },
    clearFigureImage() {
      this.figureImageUrl = ''
      if (this.draftId) sessionStorage.removeItem('ocr_figure_' + this.draftId)
    },
    applyRecognizeResult(data) {
      this.figureImageUrl = ''
      this.draftId = data.draftId
      this.imageUrl = data.imageUrl || data.imagePath || ''
      this.provider = data.provider || 'stub'
      const isStubLike = (data.lines || []).some(l => (l.text || '').includes('\u9009\u9879\u4e00'))
      this.stubWarning = data.stubWarning || (this.provider === 'stub' || isStubLike
        ? '\u5f53\u524d\u4e3a\u5360\u4f4d OCR \u6216\u672a\u8fde\u63a5\u771f\u5b9e\u5f15\u64ce\uff08\u663e\u793a\u201c\u9009\u9879\u4e00\u201d\u7b49\u793a\u4f8b\u6587\u672c\uff09\u3002\u8bf7\u786e\u8ba4 PaddleOCR \u670d\u52a1\u5df2\u542f\u52a8\uff08http://127.0.0.1:8867\uff09\u5e76\u91cd\u542f\u540e\u7aef\u3002'
        : '')
      this.qualityHint = data.qualityHint || ''
      if ((data.ocrWarnings || []).some(w => String(w).includes('formula_ocr_unavailable'))) {
        this.qualityHint = '未启用公式 OCR（Pix2Text），分数/公式可能识别不准。请在 paddleocr-service 执行 pip install -r requirements-formula.txt 并重启 OCR 服务。'
      }
      this.lines = (data.lines || []).map(l => ({
        text: normalizeOcrLatex(l.text || ''),
        confidence: Number(l.confidence || 0)
      }))
      this.avgConfidence = Number(data.confidence || 0)
      this.confidenceThreshold = Number(data.confidenceThreshold || 0.8)
      this.predictedType = data.predictedType || ''
      this.meta.questionType = data.predictedType || getDefaultQuestionType()
      this.meta.difficulty = Number(data.predictedDifficulty || 0.5)
      this.applyStructuredMeta(data)
      if (data.figurePath || data.figureUrl) {
        this.figureImageUrl = data.figurePath || data.figureUrl
        this.cacheFigurePath()
      } else {
        this.restoreFigurePath()
      }
    },
    applyStructuredMeta(data) {
      const optionRe = /^[A-Da-d][\.．、\)）:]\s*/
      const cleaned = this.filterDiagramNoise(this.lines)
      this.lines = cleaned
      const stemLines = []
      const optionLines = []
      cleaned.forEach(l => {
        const t = (l.text || '').trim()
        if (!t) return
        if (optionRe.test(t)) optionLines.push(t)
        else stemLines.push(t)
      })
      this.meta.content = stemLines.length ? stemLines.join('\n') : (data.predictedContent || cleaned.map(l => l.text).join('\n'))
      if (optionLines.length) {
        this.meta.optionsText = optionLines.join('\n')
      } else if (data.predictedOptions) {
        try {
          const opts = JSON.parse(data.predictedOptions)
          this.meta.optionsText = Array.isArray(opts) ? opts.join('\n') : ''
        } catch (e) { /* ignore */ }
      }
    },
    syncStemFromLines() {
      const optionRe = /^[A-Da-d][\.\uFF0E\u3001\)\uFF09:]\s*/
      const cleaned = this.filterDiagramNoise(this.lines)
      const stemLines = cleaned.map(l => (l.text || '').trim()).filter(t => t && !optionRe.test(t))
      const optionLines = cleaned.map(l => (l.text || '').trim()).filter(t => t && optionRe.test(t))
      this.meta.content = normalizeOcrLatex(stemLines.join('\n'))
      if (optionLines.length) this.meta.optionsText = normalizeOcrLatex(optionLines.join('\n'))
      this.$modal.msgSuccess('题干已从校对结果同步')
    },
    filterDiagramNoise(lines) {
      const optionRe = /^[A-Da-d][\.．、\)）:]\s*/
      const stemTexts = lines
        .map(l => (l.text || '').trim())
        .filter(t => t && !optionRe.test(t))
      const fullStem = stemTexts.join('')
      const orphanLabelRe = /^[A-Za-z]{1,2}$/
      return lines.filter(l => {
        const t = (l.text || '').trim()
        if (!t || optionRe.test(t)) return !!t
        if (orphanLabelRe.test(t) && stemTexts.length > 1) {
          return false
        }
        if (orphanLabelRe.test(t) && fullStem.includes(t)) {
          return false
        }
        return true
      })
    },
    isLowConfidence(conf) {
      return Number(conf) < this.confidenceThreshold
    },
    formatConfidence(conf) {
      return (Number(conf) * 100).toFixed(0) + '%'
    },
    typeLabel(type) {
      return getQuestionTypeLabel(type)
    },
    buildOptionsJson() {
      if (!this.isChoiceType) return null
      const arr = this.meta.optionsText.split('\n').map(s => s.trim()).filter(Boolean)
      return arr.length ? JSON.stringify(arr) : null
    },
    buildAnswerJson() {
      const t = this.meta.questionType
      const raw = (this.meta.correctAnswer || '').trim()
      const mode = getAnswerModeForType(t)
      if (mode === 'multi') {
        return buildMultiAnswerJson(raw)
      }
      if (mode === 'judge') {
        return JSON.stringify(raw === 'false' ? 'false' : 'true')
      }
      return JSON.stringify(raw)
    },
    buildImagesJson() {
      if (this.figureImageUrl) {
        return JSON.stringify([this.figureImageUrl])
      }
      return null
    },
    handleCommit() {
      if (!this.draftId) {
        this.$modal.msgError('请先上传图片并识别')
        return
      }
      if (!this.subjectId) {
        this.$modal.msgError('请选择学科')
        return
      }
      if (this.isStubOcr()) {
        this.$modal.confirm(
          '当前为占位 OCR 结果（未连接真实识别引擎），入库将产生无效题目。是否仍要继续？',
          'OCR 引擎未就绪',
          { confirmButtonText: '仍要入库', cancelButtonText: '取消', type: 'warning' }
        ).then(() => this.proceedCommit(true)).catch(() => {})
        return
      }
      this.proceedCommit(false)
    },
    isStubOcr() {
      return this.provider === 'stub' || !!this.stubWarning
    },
    proceedCommit(forceStub) {
      if (!this.figureImageUrl) {
        this.$modal.confirm('尚未截取题目插图，入库后将显示整页 OCR 原图。是否继续？').then(() => {
          this.doCommit(forceStub)
        }).catch(() => {})
        return
      }
      this.doCommit(forceStub)
    },
    doCommit(forceStub) {
        this._forceStubCommit = forceStub
        this.$refs.metaForm.validate(valid => {
        if (!valid) return
        this.committing = true
        commitOcr({
          draftId: this.draftId,
          subjectId: this.subjectId,
          chapterId: this.meta.chapterId,
          chapterText: this.meta.chapterText,
          knowledgePoints: JSON.stringify(this.meta.knowledgeTags),
          difficulty: this.meta.difficulty,
          questionType: this.meta.questionType,
          content: this.meta.content,
          options: this.buildOptionsJson(),
          correctAnswer: this.buildAnswerJson(),
          analysis: this.meta.analysis,
          images: this.buildImagesJson(),
          forceStub: !!this._forceStubCommit
        }).then(res => {
          if (this.draftId) sessionStorage.removeItem('ocr_figure_' + this.draftId)
          this.$modal.msgSuccess('入库成功，题目 ID：' + res.data)
          this.goBack()
        }).catch(err => {
          const msg = (err && err.response && err.response.data && err.response.data.msg) || (err && err.message) || '入库失败'
          this.$modal.msgError(msg)
        }).finally(() => { this.committing = false })
      })
    }
  }
}
</script>

<style scoped lang="scss">
.stub-alert,
.quality-alert {
  margin-bottom: 16px;
}
.education-page {
  background: #f4f8fd;
  min-height: calc(100vh - 84px);
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #22324d;
}
.page-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #7d8ea8;
}
.upload-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
.crop-hint {
  margin-bottom: 12px;
}
.crop-wrap {
  height: 420px;
  background: #1a1a1a;
}
.figure-saved-tag {
  margin-top: 8px;
}
.stem-figure-box {
  margin-top: 10px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e4e9f2;
  border-radius: 8px;
}
.stem-figure-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.stem-figure-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}
.stem-figure-placeholder {
  text-align: center;
  p {
    margin: 8px 0 0;
    font-size: 12px;
    color: #909399;
  }
}
.stem-figure-dim {
  max-width: 100%;
  max-height: 160px;
  opacity: 0.45;
  border-radius: 6px;
}
.stem-figure-img {
  max-width: 100%;
  max-height: 240px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  background: #fff;
}
.preview-box {
  margin-top: 12px;
}
.preview-img {
  max-width: 100%;
  max-height: 320px;
  border-radius: 8px;
  border: 1px solid #e4e9f2;
}
.meta-row {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
.line-list {
  max-height: 520px;
  overflow-y: auto;
}
.line-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px;
  border-radius: 6px;
  &.low {
    background: #fff7e6;
    border: 1px solid #ffd591;
  }
}
.line-body {
  flex: 1;
  min-width: 0;

  ::v-deep .qb-stem-editor {
    width: 100%;
  }
}
.conf {
  width: 42px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  padding-top: 38px;
}
.meta-card {
  margin-top: 16px;
}
.stem-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.stem-sync-hint {
  font-size: 12px;
  color: #e6a23c;
}
.content-formula-preview {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e4e9f2;
  border-radius: 8px;
}
.content-formula-preview .preview-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.hint {
  font-size: 12px;
  color: #e6a23c;
}
</style>
