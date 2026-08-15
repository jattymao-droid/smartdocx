<template>
  <div class="app-container education-page docx-import-page">
    <div class="page-header">
      <div>
        <div class="page-title">DOCX 导入</div>
        <div class="page-desc">上传后按 Word 原样预览，拖动选框框选每道题（含插图与选项），添加到右侧队列后导入</div>
      </div>
      <el-button icon="el-icon-back" size="small" @click="goBack">返回新增试题</el-button>
    </div>

    <el-card shadow="never" class="upload-card">
      <el-form :inline="true" size="small">
        <el-form-item label="学科">
          <el-select v-model="subjectId" filterable placeholder="请选择学科" style="width:160px">
            <el-option v-for="item in subjectOptions" :key="item.subjectId" :label="item.subjectName" :value="item.subjectId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="uploadData"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            accept=".docx"
          >
            <el-button type="primary" icon="el-icon-upload2" :loading="uploading">上传 DOCX</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="fileName">
          <span class="file-tag"><i class="el-icon-document" /> {{ fileName }}</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row v-if="taskId || localDocxFile" :gutter="16" class="main-row">
      <el-col :span="14">
        <el-card shadow="never" class="left-card">
          <el-tabs v-model="selectMode">
            <el-tab-pane label="文档原样" name="visual">
              <docx-visual-canvas
                :key="visualRenderKey"
                :file-path="docxFilePath"
                :local-file="localDocxFile"
                @capture="onVisualCapture"
              />
            </el-tab-pane>
            <el-tab-pane label="段落列表（备用）" name="list">
              <div class="card-header list-header">
                <span>解析段落 {{ blocks.length }} 条</span>
                <div>
                  <el-button size="mini" @click="selectAll">全选</el-button>
                  <el-button size="mini" @click="clearSelection">清除</el-button>
                  <el-button size="mini" type="primary" plain :disabled="!selectedIds.length" @click="mergeSelected">合并为一题</el-button>
                </div>
              </div>
              <div class="block-list">
                <div
                  v-for="block in blocks"
                  :key="block.blockId"
                  class="block-item"
                  :class="{
                    selected: selectedIds.includes(block.blockId),
                    used: usedBlockIds.has(block.blockId),
                    heading: isHeadingBlock(block)
                  }"
                  @click="onBlockClick(block)"
                >
                  <el-checkbox :value="selectedIds.includes(block.blockId)" @click.native.stop @change="toggleBlock(block.blockId)" />
                  <span class="block-no">{{ block.orderNum }}</span>
                  <div class="block-body">
                    <span v-if="block.text" class="block-text">{{ block.text }}</span>
                    <div v-if="block.imageUrls && block.imageUrls.length" class="block-images">
                      <img
                        v-for="(url, i) in block.imageUrls"
                        :key="i"
                        :src="resolveImageUrl(url)"
                        class="block-thumb"
                        alt="figure"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <div slot="header">导入设置</div>
          <el-form ref="metaForm" :model="meta" :rules="metaRules" label-width="80px" size="small">
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
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :disabled="!meta.textbookId || !chapterHeadings.length"
                  :loading="matchingChapter"
                  @click="autoMatchChapter"
                >自动匹配</el-button>
              </div>
              <p v-if="chapterHeadings.length" class="chapter-hint">
                检测到 {{ chapterHeadings.length }} 个章节标题：{{ chapterHeadings.slice(-2).join('、') }}
                <span v-if="chapterMatchScore">（匹配度 {{ (chapterMatchScore * 100).toFixed(0) }}%）</span>
              </p>
            </el-form-item>
            <el-form-item label="知识点" prop="knowledgeTags">
              <el-select v-model="meta.knowledgeTags" multiple filterable allow-create default-first-option placeholder="回车添加" style="width:100%">
                <el-option v-for="tag in meta.knowledgeTags" :key="tag" :label="tag" :value="tag" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度" prop="difficulty">
              <el-slider v-model="meta.difficulty" :min="0.1" :max="1" :step="0.1" show-input />
            </el-form-item>
            <el-form-item label="题型" prop="questionType">
              <el-select v-model="meta.questionType" style="width:100%">
                <el-option v-for="item in questionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="import-queue">
            <div class="queue-title">待导入题目（{{ importItems.length }} 道）</div>
            <el-empty v-if="!importItems.length" description="在左侧框选题目后添加到队列" :image-size="60" />
            <div v-for="(item, idx) in importItems" :key="idx" class="queue-item">
              <div class="queue-body">
                <div class="queue-item-head">
                  <span>第 {{ idx + 1 }} 题</span>
                  <el-select v-model="item.questionType" size="mini" style="width:100px;margin-left:8px" @change="onItemTypeChange(item)">
                    <el-option v-for="opt in questionTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </div>
                <el-input
                  v-model="item.content"
                  type="textarea"
                  :rows="5"
                  placeholder="题干"
                  size="mini"
                  class="queue-field queue-stem"
                />
                <el-input
                  v-if="isItemChoiceType(item)"
                  v-model="item.optionsText"
                  type="textarea"
                  :rows="6"
                  placeholder="每行一个选项，如 A.选项内容"
                  size="mini"
                  class="queue-field queue-options"
                />
                <el-input
                  v-if="isItemChoiceType(item)"
                  v-model="item.correctAnswer"
                  placeholder="答案，如 A"
                  size="mini"
                  class="queue-field queue-answer"
                />
                <el-input
                  v-model="item.analysis"
                  type="textarea"
                  :rows="3"
                  placeholder="题目解析"
                  size="mini"
                  class="queue-field queue-analysis"
                />
                <div v-if="item.imageUrls && item.imageUrls.length" class="queue-images">
                  <img
                    v-for="(url, i) in item.imageUrls"
                    :key="i"
                    :src="resolveImageUrl(url)"
                    class="queue-thumb"
                    alt="figure"
                  />
                </div>
              </div>
              <el-button type="text" icon="el-icon-delete" @click="removeItem(idx)" />
            </div>
          </div>
          <div class="import-actions">
            <el-button v-if="selectMode === 'list'" type="primary" :disabled="!selectedIds.length" @click="addSelectedAsItems">添加所选段落</el-button>
            <el-button type="success" :disabled="!importItems.length" :loading="committing" @click="submitImport">确认导入</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'
import { listSubject } from '@/api/education/subject'
import { resolveImageUrl } from '@/utils/paperExportCommon'
import { commitImport, getImportTask, matchImportChapters } from '@/api/education/questionImport'
import { listTextbookVersions, listTextbooks, getTextbookChapterTree } from '@/api/education/textbook'
import DocxVisualCanvas from './DocxVisualCanvas'
import { stripLeadingQuestionNo } from '@/utils/questionContent'
import { buildMultiAnswerJson } from '@/utils/questionAnswer'
import { isChoiceQuestionType, loadQuestionTypeOptions, getDefaultQuestionType, getAnswerModeForType } from '@/utils/questionTypes'

export default {
  name: 'DocxImport',
  components: { DocxVisualCanvas },
  data() {
    const validateChapterPath = (rule, value, callback) => {
      if (!value || !value.length) callback(new Error('请选择章节'))
      else callback()
    }
    const validateKnowledge = (rule, value, callback) => {
      if (!value || !value.length) callback(new Error('请至少添加一个知识点'))
      else callback()
    }
    return {
      uploading: false,
      committing: false,
      selectMode: 'visual',
      subjectId: undefined,
      schoolStage: '高中',
      versions: [],
      textbooks: [],
      chapterTree: [],
      chapterOptions: [],
      loadingVersions: false,
      loadingTextbooks: false,
      loadingChapters: false,
      cascaderProps: { expandTrigger: 'hover' },
      subjectOptions: [],
      taskId: undefined,
      docxFilePath: '',
      localDocxFile: null,
      visualRenderKey: 0,
      fileName: '',
      blocks: [],
      chapterHeadings: [],
      chapterMatchScore: null,
      matchingChapter: false,
      selectedIds: [],
      importItems: [],
      meta: {
        versionId: undefined,
        textbookId: undefined,
        chapterPath: [],
        chapterId: undefined,
        chapterText: '',
        knowledgeTags: [],
        difficulty: 0.5,
        questionType: 'single'
      },
      metaRules: {
        versionId: [{ required: true, message: '请选择版本', trigger: 'change' }],
        textbookId: [{ required: true, message: '请选择教材', trigger: 'change' }],
        chapterPath: [{ validator: validateChapterPath, trigger: 'change' }],
        knowledgeTags: [{ validator: validateKnowledge, trigger: 'change' }],
        questionType: [{ required: true, message: '请选择题型', trigger: 'change' }]
      },
      questionTypeOptions: []
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
  computed: {
    uploadUrl() {
      return process.env.VUE_APP_BASE_API + '/education/question/import/docx'
    },
    uploadHeaders() {
      return { Authorization: 'Bearer ' + getToken() }
    },
    uploadData() {
      return { subjectId: this.subjectId }
    },
    usedBlockIds() {
      const set = new Set()
      this.importItems.forEach(item => {
        (item.blockIds || []).forEach(id => set.add(id))
      })
      return set
    },
    blockMap() {
      const map = {}
      this.blocks.forEach(b => { map[b.blockId] = b })
      return map
    },
    isChoiceType() {
      return isChoiceQuestionType(this.meta.questionType)
    }
  },
  created() {
    const q = this.$route.query || {}
    if (q.schoolStage) this.schoolStage = q.schoolStage
    if (q.subjectId) this.subjectId = Number(q.subjectId) || q.subjectId
    this.loadSubjects()
    this.loadTaskFromQuery()
    loadQuestionTypeOptions().then(options => {
      this.questionTypeOptions = options
      if (!options.some(item => item.value === this.meta.questionType)) {
        this.meta.questionType = options.length ? options[0].value : getDefaultQuestionType()
      }
    })
  },
  methods: {
    resolveImageUrl,
    isItemChoiceType(item) {
      const t = (item && item.questionType) || this.meta.questionType
      return isChoiceQuestionType(t)
    },
    onItemTypeChange(item) {
      if (!this.isItemChoiceType(item)) {
        item.optionsText = ''
        item.correctAnswer = 'A'
      }
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
        } else if (this.chapterHeadings.length) {
          this.autoMatchChapter(false)
        }
      }).finally(() => { this.loadingChapters = false })
    },
    isHeadingBlock(block) {
      return block && (block.blockKind === 'heading' || this.isHeadingText(block.text))
    },
    isHeadingText(text) {
      const t = (text || '').trim()
      if (!t || t.length > 80 || t.includes('？') || t.includes('?')) return false
      return /^第[0-9一二三四五六七八九十百千]+[章节编课]/.test(t)
        || /^[0-9]+(\.[0-9]+){0,2}\s+\S+/.test(t)
        || /^[一二三四五六七八九十]+[、．.]/.test(t)
    },
    extractChapterHeadingsFromBlocks() {
      const hints = []
      ;(this.blocks || []).forEach(block => {
        if (!this.isHeadingBlock(block)) return
        const raw = (block.text || '').trim()
        if (!raw || hints.includes(raw)) return
        hints.push(raw)
      })
      return hints
    },
    autoMatchChapter(showToast = true) {
      if (!this.meta.textbookId || !this.chapterHeadings.length) return Promise.resolve()
      this.matchingChapter = true
      return matchImportChapters({
        textbookId: this.meta.textbookId,
        hints: this.chapterHeadings
      }).then(res => {
        const rows = res.data || []
        const hit = [...rows].reverse().find(r => r.matched && r.chapterId)
          || rows.find(r => r.matched && r.chapterId)
        if (!hit) {
          if (showToast) this.$modal.msgWarning('未能自动匹配章节，请手动选择')
          return
        }
        this.applyChapterMatch(hit, showToast)
      }).finally(() => { this.matchingChapter = false })
    },
    applyChapterMatch(hit, showToast = true) {
      if (!hit || !hit.chapterId) return
      const path = this.findChapterPath(this.chapterTree, hit.chapterId)
      if (path) {
        this.meta.chapterPath = path
        this.applyChapterFromPath(path)
      } else {
        this.meta.chapterId = hit.chapterId
        this.meta.chapterText = hit.chapterText || hit.chapterName || ''
      }
      this.chapterMatchScore = hit.score || null
      if (showToast) {
        this.$modal.msgSuccess('已匹配章节：' + (hit.chapterText || hit.chapterName))
      }
    },
    onBlockClick(block) {
      if (this.isHeadingBlock(block)) {
        const hint = (block.text || '').trim()
        if (this.meta.textbookId && hint) {
          matchImportChapters({ textbookId: this.meta.textbookId, hints: [hint] }).then(res => {
            const hit = (res.data || [])[0]
            if (hit && hit.matched) this.applyChapterMatch(hit)
            else this.$modal.msgWarning('未匹配到对应章节')
          })
        }
        return
      }
      this.toggleBlock(block.blockId)
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
    applyParsedPayload(data) {
      this.taskId = data.taskId
      this.fileName = data.fileName || ''
      this.docxFilePath = data.filePath || ''
      this.blocks = data.blocks || []
      this.chapterHeadings = data.chapterHeadings || this.extractChapterHeadingsFromBlocks()
      this.chapterMatchScore = null
      this.selectedIds = []
      this.importItems = []
      this.selectMode = 'visual'
    },
    loadTaskFromQuery() {
      const taskId = this.$route.query.taskId
      if (!taskId) return
      getImportTask(taskId).then(res => {
        const data = res.data || {}
        const task = data.task || {}
        this.subjectId = task.subjectId
        this.applyParsedPayload({
          taskId: task.taskId,
          fileName: task.fileName,
          filePath: task.filePath,
          blocks: data.blocks || []
        })
      })
    },
    loadSubjects() {
      listSubject({ pageNum: 1, pageSize: 500 }).then(res => {
        this.subjectOptions = res.rows || []
      })
    },
    goBack() {
      this.$router.push({ path: '/admin/question-bank/question-create' })
    },
    beforeUpload(file) {
      if (!this.subjectId) {
        this.$modal.msgWarning('请先选择学科')
        return false
      }
      const isDocx = file.name.toLowerCase().endsWith('.docx')
      if (!isDocx) {
        this.$modal.msgError('仅支持 .docx 格式')
        return false
      }
      if (file.size / 1024 / 1024 > 20) {
        this.$modal.msgError('文件不能超过 20MB')
        return false
      }
      this.localDocxFile = file
      this.visualRenderKey += 1
      this.uploading = true
      return true
    },
    onUploadSuccess(res) {
      this.uploading = false
      const payload = (res && res.data) ? res.data : (res || {})
      if (res && res.code !== undefined && res.code !== 200) {
        this.$modal.msgError(res.msg || '解析失败')
        return
      }
      this.applyParsedPayload(payload)
      if (!this.docxFilePath && payload.taskId) {
        getImportTask(payload.taskId).then(r => {
          const task = (r.data && r.data.task) ? r.data.task : {}
          if (task.filePath) this.docxFilePath = task.filePath
        }).catch(() => {})
      }
      this.$modal.msgSuccess('文档已上传，请在原样预览中框选题目')
    },
    onUploadError() {
      this.uploading = false
      this.$modal.msgError('上传失败')
    },
    onVisualCapture(payload) {
      const raw = (payload.content || '').trim()
      const imageUrls = payload.imageUrls || []
      if (!raw && !imageUrls.length) return
      const parsed = this.parseCapturedContent(raw)
      if (this.isDuplicateItem(parsed.content, imageUrls)) {
        this.$modal.msgWarning('与队列中已有题目重复，已跳过。请调整选框覆盖整道题后重试')
        return
      }
      this.importItems.push({
        content: parsed.content,
        optionsText: parsed.optionsText,
        correctAnswer: parsed.correctAnswer,
        analysis: '',
        imageUrls,
        blockIds: [],
        chapterId: this.meta.chapterId,
        chapterText: this.meta.chapterText
      })
      this.$modal.msgSuccess('已添加到待导入队列')
    },
    parseCapturedContent(raw) {
      const text = (raw || '').replace(/\r/g, '').trim()
      if (!text) {
        return { content: '', optionsText: '', correctAnswer: 'A', analysis: '' }
      }
      const firstOpt = text.search(/[A-Da-d][\.．、\)）:：]/)
      if (firstOpt < 0) {
        return { content: stripLeadingQuestionNo(text), optionsText: '', correctAnswer: 'A', analysis: '' }
      }
      const stem = text.slice(0, firstOpt).trim()
      const optionPart = text.slice(firstOpt).trim()
      const optionLines = optionPart.split(/(?=[A-Da-d][\.．、\)）:：])/).map(s => s.trim()).filter(Boolean)
      return {
        content: stripLeadingQuestionNo(stem || text),
        optionsText: optionLines.join('\n'),
        correctAnswer: 'A',
        analysis: ''
      }
    },
    prepareCommitItem(item) {
      const merged = [item.content, item.optionsText].filter(Boolean).join('\n')
      const parsed = this.parseCapturedContent(merged)
      const optionsText = parsed.optionsText || item.optionsText || ''
      const itemType = item.questionType || this.meta.questionType
      return {
        content: parsed.content || item.content || '',
        blockIds: item.blockIds || [],
        images: item.imageUrls && item.imageUrls.length ? JSON.stringify(item.imageUrls) : null,
        options: this.buildItemOptionsJson(optionsText, itemType),
        optionsText,
        correctAnswer: this.buildItemAnswerJson(item.correctAnswer, itemType),
        analysis: (item.analysis || '').trim() || undefined,
        questionType: itemType,
        chapterId: item.chapterId || this.meta.chapterId,
        chapterText: item.chapterText || this.meta.chapterText
      }
    },
    isDuplicateItem(content, imageUrls) {
      const text = (content || '').trim()
      if (!text) return false
      return this.importItems.some(item => {
        const existing = (item.content || '').trim()
        if (!existing) return false
        if (existing === text) return true
        if (existing.length > 20 && text.length > 20) {
          if (existing.includes(text) || text.includes(existing)) return true
        }
        return false
      })
    },
    buildImportItem(rawContent, imageUrls, blockIds) {
      const parsed = this.parseCapturedContent(rawContent)
      return {
        content: parsed.content,
        optionsText: parsed.optionsText,
        correctAnswer: parsed.correctAnswer,
        analysis: '',
        questionType: this.meta.questionType,
        imageUrls: imageUrls || [],
        blockIds: blockIds || [],
        chapterId: this.meta.chapterId,
        chapterText: this.meta.chapterText
      }
    },
    buildItemOptionsJson(optionsText, questionType) {
      const t = questionType || this.meta.questionType
      if (!isChoiceQuestionType(t)) return null
      const arr = (optionsText || '').split('\n').map(s => s.trim()).filter(Boolean)
      return arr.length ? JSON.stringify(arr) : null
    },
    buildItemAnswerJson(answer, questionType) {
      const raw = (answer || 'A').trim()
      const t = questionType || this.meta.questionType
      const mode = getAnswerModeForType(t)
      if (mode === 'multi') {
        return buildMultiAnswerJson(raw)
      }
      if (mode === 'judge') {
        return JSON.stringify(raw === 'false' ? 'false' : 'true')
      }
      return JSON.stringify(raw || 'A')
    },
    validateImportForm() {
      return new Promise((resolve, reject) => {
        this.$refs.metaForm.validate(valid => {
          if (!valid) {
            reject(new Error('请完善导入设置（章节、知识点、题型）'))
            return
          }
          if (!this.taskId || !this.importItems.length) {
            reject(new Error('请先添加待导入题目'))
            return
          }
          const badIdx = this.importItems.findIndex(item => {
              if (!this.isItemChoiceType(item)) return false
              const prepared = this.prepareCommitItem(item)
              return !prepared.options
            })
            if (badIdx >= 0) {
              reject(new Error(`第 ${badIdx + 1} 题缺少选项，请在右侧填写或删除该题`))
              return
            }
          resolve()
        })
      })
    },
    toggleBlock(blockId) {
      if (this.usedBlockIds.has(blockId)) return
      const idx = this.selectedIds.indexOf(blockId)
      if (idx >= 0) this.selectedIds.splice(idx, 1)
      else this.selectedIds.push(blockId)
    },
    selectAll() {
      this.selectedIds = this.blocks.filter(b => !this.usedBlockIds.has(b.blockId)).map(b => b.blockId)
    },
    clearSelection() {
      this.selectedIds = []
    },
    pushImportItem(blockIds) {
      const sorted = [...blockIds].sort((a, b) => a - b)
      const imageUrls = []
      const content = sorted.map(id => {
        const block = this.blockMap[id]
        if (!block) return ''
        if (block.imageUrls && block.imageUrls.length) {
          block.imageUrls.forEach(u => {
            if (u && !imageUrls.includes(u)) imageUrls.push(u)
          })
        }
        const text = block.text === '插图' && block.imageUrls && block.imageUrls.length ? '' : (block.text || '')
        return text
      }).filter(Boolean).join('\n')
      if (!content && !imageUrls.length) return
      const item = this.buildImportItem(content, imageUrls, sorted)
      if (this.isDuplicateItem(item.content, imageUrls)) {
        this.$modal.msgWarning('与队列中已有题目重复，已跳过')
        return
      }
      this.importItems.push(item)
      this.selectedIds = []
    },
    mergeSelected() {
      if (!this.selectedIds.length) return
      this.pushImportItem(this.selectedIds)
      this.$modal.msgSuccess('已添加到待导入队列')
    },
    addSelectedAsItems() {
      if (!this.selectedIds.length) {
        this.$modal.msgWarning('请先勾选段落')
        return
      }
      const sorted = [...this.selectedIds].sort((a, b) => a - b)
      sorted.forEach(id => {
        const block = this.blockMap[id]
        if (block && !this.usedBlockIds.has(id)) {
          const imageUrls = block.imageUrls ? [...block.imageUrls] : []
          const item = this.buildImportItem(block.text || '', imageUrls, [id])
          if (!this.isDuplicateItem(item.content, imageUrls)) {
            this.importItems.push(item)
          }
        }
      })
      this.selectedIds = []
    },
    removeItem(idx) {
      this.importItems.splice(idx, 1)
    },
    submitImport() {
      this.validateImportForm().then(() => {
        this.committing = true
        return commitImport({
          taskId: this.taskId,
          subjectId: this.subjectId,
          chapterId: this.meta.chapterId,
          chapterText: (this.meta.chapterText || '').trim(),
          knowledgePoints: JSON.stringify(this.meta.knowledgeTags),
          difficulty: this.meta.difficulty,
          questionType: this.meta.questionType,
          items: this.importItems.map(item => this.prepareCommitItem(item))
        })
      }).then(res => {
        this.$modal.msgSuccess(`成功导入 ${res.data || this.importItems.length} 道试题`)
        this.$router.push({ path: '/admin/question-bank/question-create' })
      }).catch(err => {
        if (err && err.message && !err.message.includes('请输入选项')) {
          this.$modal.msgWarning(err.message)
        }
      }).finally(() => { this.committing = false })
    }
  }
}
</script>

<style scoped lang="scss">
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
.upload-card {
  margin-bottom: 16px;
}
.file-tag {
  color: #606266;
  font-size: 13px;
}
.left-card {
  min-height: 620px;
  overflow: visible;
}
.docx-import-page ::v-deep .left-card .el-card__body {
  overflow: visible;
}
.docx-import-page ::v-deep .el-tabs__content {
  overflow: visible;
}
.docx-import-page ::v-deep .el-tab-pane {
  overflow: visible;
}
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.block-list {
  max-height: 520px;
  overflow-y: auto;
}
.block-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 8px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  &.selected {
    background: #ecf5ff;
  }
  &.used {
    opacity: 0.45;
    cursor: not-allowed;
  }
  &.heading {
    border-color: #f5dab1;
    background: #fdf6ec;
  }
}
.chapter-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.chapter-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.block-no {
  width: 28px;
  color: #909399;
  flex-shrink: 0;
}
.block-body {
  flex: 1;
  min-width: 0;
}
.block-text {
  display: block;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-all;
}
.block-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}
.block-thumb {
  max-width: 160px;
  max-height: 120px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.import-queue {
  margin-top: 12px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
.queue-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}
.queue-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.queue-body {
  flex: 1;
  min-width: 0;
}
.queue-item-head {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}
.queue-field {
  margin-bottom: 6px;
}
.queue-stem ::v-deep textarea,
.queue-options ::v-deep textarea {
  min-height: 96px;
  line-height: 1.5;
}
.queue-options ::v-deep textarea {
  min-height: 120px;
}
.queue-answer {
  max-width: 120px;
}
.queue-analysis ::v-deep textarea {
  min-height: 72px;
  line-height: 1.5;
}

.queue-content {
  font-size: 12px;
  color: #606266;
  max-height: 72px;
  overflow: hidden;
  white-space: pre-line;
  line-height: 1.5;
}
.queue-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}
.queue-thumb {
  max-width: 100px;
  max-height: 72px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.import-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
