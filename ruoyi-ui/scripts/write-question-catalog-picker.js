const fs = require('fs')
const path = require('path')

const out = path.join(__dirname, '../src/views/education/question-bank/components/QuestionCatalogPicker.vue')

const content = `<template>
  <div class="question-catalog-picker" :class="{ compact: compact }">
    <el-form label-width="52px" size="mini" class="catalog-form">
      <el-form-item label="\u79d1\u76ee">
        <el-select
          v-model="inner.subjectId"
          filterable
          placeholder="\u5b66\u79d1"
          style="width:100%"
          :disabled="subjectLocked"
          @change="onSubjectChange"
        >
          <el-option v-for="s in resolvedSubjectOptions" :key="s.subjectId" :label="s.subjectName" :value="s.subjectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="\u7248\u672c">
        <el-select
          v-model="inner.versionId"
          filterable
          placeholder="\u7248\u672c"
          style="width:100%"
          :loading="loadingVersions"
          :disabled="!inner.subjectId"
          @change="onVersionChange"
        >
          <el-option v-for="v in versions" :key="v.versionId" :label="v.versionName" :value="v.versionId" />
        </el-select>
      </el-form-item>
      <el-form-item label="\u6559\u6750">
        <el-select
          v-model="inner.textbookId"
          filterable
          placeholder="\u6559\u6750"
          style="width:100%"
          :loading="loadingTextbooks"
          :disabled="!inner.versionId"
          @change="onTextbookChange"
        >
          <el-option v-for="t in textbooks" :key="t.textbookId" :label="t.textbookName" :value="t.textbookId" />
        </el-select>
      </el-form-item>
      <el-form-item label="\u7ae0\u8282">
        <el-cascader
          v-model="inner.chapterPath"
          :options="chapterOptions"
          :props="cascaderProps"
          filterable
          clearable
          style="width:100%"
          placeholder="\u7ae0\u8282"
          :disabled="!inner.textbookId"
          @change="onChapterChange"
        />
      </el-form-item>
      <el-form-item label="\u77e5\u8bc6\u70b9">
        <el-select
          v-model="inner.knowledgeTags"
          multiple
          filterable
          allow-create
          default-first-option
          collapse-tags
          placeholder="\u56de\u8f66\u6dfb\u52a0"
          style="width:100%"
        >
          <el-option v-for="tag in inner.knowledgeTags" :key="tag" :label="tag" :value="tag" />
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { listSubject } from '@/api/education/subject'
import { listTextbookVersions, listTextbooks, getTextbookChapterTree } from '@/api/education/textbook'

function emptyMeta() {
  return {
    subjectId: undefined,
    versionId: undefined,
    textbookId: undefined,
    chapterPath: [],
    chapterId: undefined,
    chapterText: '',
    knowledgeTags: []
  }
}

export default {
  name: 'QuestionCatalogPicker',
  props: {
    value: { type: Object, default: () => emptyMeta() },
    subjectId: { type: [Number, String], default: undefined },
    subjectOptions: { type: Array, default: () => [] },
    subjectLocked: { type: Boolean, default: false },
    compact: { type: Boolean, default: true },
    schoolStage: { type: String, default: '\u9ad8\u4e2d' }
  },
  data() {
    return {
      inner: emptyMeta(),
      localSubjectOptions: [],
      versions: [],
      textbooks: [],
      chapterOptions: [],
      chapterTree: [],
      loadingVersions: false,
      loadingTextbooks: false,
      loadingChapters: false,
      syncing: false,
      cascaderProps: { expandTrigger: 'hover', checkStrictly: false }
    }
  },
  computed: {
    resolvedSubjectOptions() {
      return (this.subjectOptions && this.subjectOptions.length)
        ? this.subjectOptions
        : this.localSubjectOptions
    }
  },
  watch: {
    value: {
      deep: true,
      immediate: true,
      handler(val) {
        this.syncing = true
        this.inner = Object.assign(emptyMeta(), val || {})
        if (!this.inner.subjectId && this.subjectId) {
          this.inner.subjectId = Number(this.subjectId)
        }
        this.$nextTick(() => {
          this.syncing = false
          this.ensureCatalogLoaded()
        })
      }
    },
    inner: {
      deep: true,
      handler(val) {
        if (this.syncing) return
        this.$emit('input', Object.assign({}, val))
      }
    },
    subjectId(val) {
      if (val && !this.inner.subjectId) {
        this.inner.subjectId = Number(val)
        this.ensureCatalogLoaded()
      }
    }
  },
  created() {
    if (!this.subjectOptions.length) {
      listSubject({ pageNum: 1, pageSize: 200, status: '0' }).then(res => {
        this.localSubjectOptions = res.rows || []
      })
    }
    if (this.subjectId && !this.inner.subjectId) {
      this.inner.subjectId = Number(this.subjectId)
    }
    this.ensureCatalogLoaded()
  },
  methods: {
    ensureCatalogLoaded() {
      if (!this.inner.subjectId) return
      this.loadVersions(true).then(() => {
        if (this.inner.textbookId) return this.loadChapterTree(true)
        return null
      })
    },
    emitInner() {
      if (this.syncing) return
      this.$emit('input', Object.assign({}, this.inner))
    },
    onSubjectChange() {
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
    clearChapter() {
      this.inner.chapterPath = []
      this.inner.chapterId = undefined
      this.inner.chapterText = ''
      this.chapterOptions = []
      this.chapterTree = []
    },
    loadVersions(keepSelection) {
      if (!this.inner.subjectId) {
        this.versions = []
        return Promise.resolve()
      }
      this.loadingVersions = true
      return listTextbookVersions(this.inner.subjectId, this.schoolStage).then(res => {
        this.versions = res.data || []
        if (!keepSelection || !this.versions.some(v => v.versionId === this.inner.versionId)) {
          this.inner.versionId = this.versions[0] && this.versions[0].versionId
        }
        return this.loadTextbooks(keepSelection)
      }).finally(() => { this.loadingVersions = false })
    },
    loadTextbooks(keepSelection) {
      if (!this.inner.versionId) {
        this.textbooks = []
        return Promise.resolve()
      }
      this.loadingTextbooks = true
      return listTextbooks(this.inner.versionId).then(res => {
        this.textbooks = res.data || []
        if (!keepSelection || !this.textbooks.some(t => t.textbookId === this.inner.textbookId)) {
          this.inner.textbookId = this.textbooks[0] && this.textbooks[0].textbookId
        }
        if (this.inner.textbookId) return this.loadChapterTree(keepSelection)
        return null
      }).finally(() => { this.loadingTextbooks = false })
    },
    loadChapterTree(keepSelection) {
      if (!this.inner.textbookId || !this.inner.subjectId) {
        this.chapterOptions = []
        return Promise.resolve()
      }
      this.loadingChapters = true
      return getTextbookChapterTree(this.inner.textbookId, this.inner.subjectId).then(res => {
        this.chapterTree = res.data || []
        this.chapterOptions = this.buildCascaderOptions(this.chapterTree)
        if (keepSelection && this.inner.chapterId) {
          const path = this.findChapterPath(this.chapterTree, this.inner.chapterId)
          if (path) {
            this.inner.chapterPath = path
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

<style scoped lang="scss">
.question-catalog-picker.compact {
  .catalog-form ::v-deep .el-form-item {
    margin-bottom: 6px;
  }
  .catalog-form ::v-deep .el-form-item__label {
    font-size: 12px;
    padding-right: 6px;
  }
}
</style>
`

fs.writeFileSync(out, content, 'utf8')
console.log('Wrote', out)
