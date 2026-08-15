<template>
  <div class="document-catalog-fields">
    <el-form-item v-if="!hideStage" :label="L.stage" prop="schoolStage">
      <el-select
        v-model="inner.schoolStage"
        clearable
        :placeholder="L.stagePh"
        style="width: 100%"
        @change="onStageChange"
      >
        <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </el-form-item>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item :label="L.version" prop="versionId">
          <el-select
            v-model="inner.versionId"
            clearable
            filterable
            :placeholder="L.versionPh"
            style="width: 100%"
            :loading="loadingVersions"
            :disabled="!subjectId"
            @change="onVersionChange"
          >
            <el-option v-for="v in versions" :key="v.versionId" :label="v.versionName" :value="v.versionId" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item :label="L.textbook" prop="textbookId">
          <el-select
            v-model="inner.textbookId"
            clearable
            filterable
            :placeholder="L.textbookPh"
            style="width: 100%"
            :loading="loadingTextbooks"
            :disabled="!inner.versionId"
            @change="onTextbookChange"
          >
            <el-option v-for="t in textbooks" :key="t.textbookId" :label="t.textbookName" :value="t.textbookId" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item :label="L.chapter" prop="chapterPath">
      <el-cascader
        v-model="inner.chapterPath"
        :options="chapterOptions"
        :props="cascaderProps"
        filterable
        clearable
        style="width: 100%"
        :placeholder="L.chapterPh"
        :disabled="!inner.textbookId"
        @change="onChapterChange"
      />
    </el-form-item>
  </div>
</template>

<script>
import { listTextbookVersions, listTextbooks, getTextbookChapterTree } from '@/api/education/textbook'
import {
  getVersionCache,
  setVersionCache,
  getTextbookCache,
  setTextbookCache,
  getChapterTreeCache,
  setChapterTreeCache
} from '@/utils/catalogCache'

function emptyCatalog() {
  return {
    schoolStage: '\u9ad8\u4e2d',
    versionId: undefined,
    textbookId: undefined,
    chapterPath: [],
    chapterId: undefined,
    chapterText: ''
  }
}

const L = {
  stage: '\u5b66\u6bb5',
  stagePh: '\u8bf7\u9009\u62e9\u5b66\u6bb5',
  version: '\u7248\u672c',
  versionPh: '\u8bf7\u9009\u62e9\u7248\u672c',
  textbook: '\u6559\u6750',
  textbookPh: '\u8bf7\u9009\u62e9\u6559\u6750',
  chapter: '\u7ae0\u8282',
  chapterPh: '\u8bf7\u9009\u62e9\u7ae0\u8282'
}

export default {
  name: 'DocumentCatalogFields',
  props: {
    value: {
      type: Object,
      default: () => emptyCatalog()
    },
    subjectId: {
      type: [Number, String],
      default: undefined
    },
    hideStage: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      L,
      inner: emptyCatalog(),
      stageOptions: [
        { label: '\u521d\u4e2d', value: '\u521d\u4e2d' },
        { label: '\u9ad8\u4e2d', value: '\u9ad8\u4e2d' }
      ],
      versions: [],
      textbooks: [],
      chapterTree: [],
      chapterOptions: [],
      loadingVersions: false,
      loadingTextbooks: false,
      loadingChapters: false,
      syncing: false,
      cascaderProps: { expandTrigger: 'hover' }
    }
  },
  watch: {
    value: {
      deep: true,
      immediate: true,
      handler(val) {
        const prevSubjectId = this._lastSubjectId
        const prevStage = this._lastStage
        this.syncing = true
        const next = Object.assign(emptyCatalog(), val || {})
        Object.keys(next).forEach(key => {
          this.$set(this.inner, key, next[key])
        })
        this.$nextTick(() => {
          this.syncing = false
          const subjectChanged = this.subjectId && String(this.subjectId) !== String(prevSubjectId)
          const stageChanged = String(next.schoolStage || '') !== String(prevStage || '')
          this._lastSubjectId = this.subjectId
          this._lastStage = next.schoolStage
          if (subjectChanged || stageChanged || (this.subjectId && !this.versions.length)) {
            this.ensureCatalogLoaded()
          }
        })
      }
    },
    subjectId(val, oldVal) {
      this._lastSubjectId = val
      if (String(val) === String(oldVal)) return
      this.resetDownstream()
      if (val) this.loadVersions(false)
    },
    inner: {
      deep: true,
      handler(val) {
        if (this.syncing) return
        this.$emit('input', Object.assign({}, val))
      }
    }
  },
  created() {
    this._lastSubjectId = this.subjectId
    this._lastStage = this.inner.schoolStage
    if (this.subjectId) this.ensureCatalogLoaded()
  },
  methods: {
    ensureCatalogLoaded() {
      if (!this.subjectId) return
      this.loadVersions(true).then(() => {
        if (this.inner.textbookId) return this.loadChapterTree(true)
        return null
      })
    },
    emitInner() {
      if (this.syncing) return
      this.$emit('input', Object.assign({}, this.inner))
    },
    onStageChange() {
      this.inner.versionId = undefined
      this.inner.textbookId = undefined
      this.clearChapter()
      this.loadVersions(false)
      this.emitInner()
    },
    onVersionChange() {
      this.inner.textbookId = undefined
      this.clearChapter()
      this.loadTextbooks(false)
      this.emitInner()
    },
    onTextbookChange() {
      this.clearChapter()
      this.loadChapterTree(false)
      this.emitInner()
    },
    resetDownstream() {
      this.inner.versionId = undefined
      this.inner.textbookId = undefined
      this.clearChapter()
      this.versions = []
      this.textbooks = []
    },
    clearChapter() {
      this.inner.chapterPath = []
      this.inner.chapterId = undefined
      this.inner.chapterText = ''
      this.chapterOptions = []
      this.chapterTree = []
    },
    loadVersions(keepSelection) {
      if (!this.subjectId) {
        this.versions = []
        return Promise.resolve()
      }
      const stage = this.inner.schoolStage || '\u9ad8\u4e2d'
      const cached = getVersionCache(this.subjectId, stage)
      if (cached) {
        this.versions = cached
        this.syncing = true
        if (!keepSelection || !this.versions.some(v => v.versionId === this.inner.versionId)) {
          this.$set(this.inner, 'versionId', undefined)
        }
        this.syncing = false
        this.emitInner()
        if (this.inner.versionId) return this.loadTextbooks(keepSelection)
        this.textbooks = []
        return Promise.resolve()
      }
      this.loadingVersions = true
      return listTextbookVersions(this.subjectId, stage).then(res => {
        this.versions = res.data || []
        setVersionCache(this.subjectId, stage, this.versions)
        this.syncing = true
        if (!keepSelection || !this.versions.some(v => v.versionId === this.inner.versionId)) {
          this.$set(this.inner, 'versionId', undefined)
        }
        this.syncing = false
        this.emitInner()
        if (this.inner.versionId) return this.loadTextbooks(keepSelection)
        this.textbooks = []
        return null
      }).catch(() => {
        this.versions = []
      }).finally(() => { this.loadingVersions = false })
    },
    loadTextbooks(keepSelection) {
      if (!this.inner.versionId) {
        this.textbooks = []
        return Promise.resolve()
      }
      const cached = getTextbookCache(this.inner.versionId)
      if (cached) {
        this.textbooks = cached
        this.syncing = true
        if (!keepSelection || !this.textbooks.some(t => t.textbookId === this.inner.textbookId)) {
          this.$set(this.inner, 'textbookId', undefined)
        }
        this.syncing = false
        this.emitInner()
        if (this.inner.textbookId) return this.loadChapterTree(keepSelection)
        this.clearChapter()
        return Promise.resolve()
      }
      this.loadingTextbooks = true
      return listTextbooks(this.inner.versionId).then(res => {
        this.textbooks = res.data || []
        setTextbookCache(this.inner.versionId, this.textbooks)
        this.syncing = true
        if (!keepSelection || !this.textbooks.some(t => t.textbookId === this.inner.textbookId)) {
          this.$set(this.inner, 'textbookId', undefined)
        }
        this.syncing = false
        this.emitInner()
        if (this.inner.textbookId) return this.loadChapterTree(keepSelection)
        this.clearChapter()
        return null
      }).catch(() => {
        this.textbooks = []
      }).finally(() => { this.loadingTextbooks = false })
    },
    loadChapterTree(keepSelection) {
      if (!this.inner.textbookId || !this.subjectId) {
        this.chapterOptions = []
        return Promise.resolve()
      }
      const cached = getChapterTreeCache(this.inner.textbookId, this.subjectId)
      if (cached) {
        this.chapterTree = cached
        this.chapterOptions = this.buildCascaderOptions(this.chapterTree)
        if (keepSelection && this.inner.chapterId) {
          const path = this.findChapterPath(this.chapterTree, this.inner.chapterId)
          if (path) {
            this.inner.chapterPath = path
            this.applyChapterFromPath(path)
          }
        }
        return Promise.resolve()
      }
      this.loadingChapters = true
      return getTextbookChapterTree(this.inner.textbookId, this.subjectId).then(res => {
        this.chapterTree = res.data || []
        setChapterTreeCache(this.inner.textbookId, this.subjectId, this.chapterTree)
        this.chapterOptions = this.buildCascaderOptions(this.chapterTree)
        if (keepSelection && this.inner.chapterId) {
          const path = this.findChapterPath(this.chapterTree, this.inner.chapterId)
          if (path) {
            this.inner.chapterPath = path
            this.applyChapterFromPath(path)
          }
        }
      }).catch(() => {
        this.chapterTree = []
        this.chapterOptions = []
      }).finally(() => { this.loadingChapters = false })
    },
    buildCascaderOptions(nodes) {
      return (nodes || [])
        .filter(n => n.id !== 'all')
        .map(n => {
          const children = (n.children || []).filter(c => c.id !== 'all')
          const item = { value: n.id, label: n.label }
          if (children.length) item.children = children.map(c => ({ value: c.id, label: c.label }))
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
        this.inner.chapterId = undefined
        this.inner.chapterText = ''
        this.emitInner()
        return
      }
      this.applyChapterFromPath(path)
      this.emitInner()
    },
    applyChapterFromPath(path) {
      const chapterId = path[path.length - 1]
      this.inner.chapterId = chapterId
      if (path.length === 1) {
        const node = this.findNodeById(this.chapterTree, chapterId)
        this.inner.chapterText = node ? node.label : ''
        return
      }
      const parent = this.findNodeById(this.chapterTree, path[0])
      const leaf = parent && parent.children
        ? parent.children.find(c => String(c.id) === String(chapterId))
        : this.findNodeById(this.chapterTree, chapterId)
      const parentLabel = parent ? parent.label : ''
      const leafLabel = leaf ? leaf.label : ''
      this.inner.chapterText = parentLabel && leafLabel ? parentLabel + ' > ' + leafLabel : (leafLabel || parentLabel)
    }
  }
}
</script>
