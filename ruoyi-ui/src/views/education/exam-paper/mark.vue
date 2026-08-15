<template>
  <div class="app-container education-page exam-paper-mark-page">
    <div class="page-header">
      <div>
        <div class="page-title">智能标记题目</div>
        <div class="page-desc">上传 DOCX 试卷，在原文档上标记题目并设置分类后发布到门户</div>
      </div>
      <div class="header-actions">
        <el-button icon="el-icon-back" size="small" @click="goBack">返回列表</el-button>
        <el-button v-if="parsed" size="small" @click="resetMark">重新上传</el-button>
        <el-button v-if="parsed" type="primary" :loading="committing" @click="submitCommit">保存为草稿</el-button>
      </div>
    </div>

    <el-card v-if="!parsed" shadow="never" class="upload-card">
      <el-form label-width="88px" size="small">
        <el-form-item label="学科" required>
          <el-select v-model="uploadForm.subjectId" filterable placeholder="请选择学科" style="width:220px">
            <el-option v-for="s in subjectOptions" :key="s.subjectId" :label="s.subjectName" :value="s.subjectId" />
          </el-select>
        </el-form-item>
        <el-form-item label="DOCX" required>
          <el-upload drag action="#" :auto-upload="false" :limit="1" accept=".docx" :on-change="onFileChange" :file-list="fileList">
            <i class="el-icon-upload" />
            <div class="el-upload__text">拖拽或点击上传试卷 DOCX</div>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="parsing" icon="el-icon-magic-stick" @click="parseUpload">解析并标记</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <exam-paper-mark-workspace
      v-else
      full-page
      :local-file="uploadForm.file"
      :source-file="commitForm.sourceFile"
      :preview-html="previewHtml"
      :marked-items.sync="markedItems"
      :commit-form="commitForm"
      :category-options="categoryOptions"
      :visual-key="visualKey"
      :subject-id="uploadForm.subjectId"
      :subject-options="subjectOptions"
    />
  </div>
</template>

<script>
import { listSubject } from '@/api/education/subject'
import { uploadExamPaper, commitExamPaper } from '@/api/education/examPaper'
import ExamPaperMarkWorkspace from './ExamPaperMarkWorkspace'

const CATEGORY_OPTIONS = [
  { value: 'gaokao', label: '\u9ad8\u8003\u771f\u9898' },
  { value: 'mock', label: '\u6a21\u62df\u8bd5\u5377' },
  { value: 'unit', label: '\u5355\u5143\u6d4b\u9a8c' },
  { value: 'mid', label: '\u671f\u4e2d\u8003\u8bd5' },
  { value: 'final', label: '\u671f\u672b\u8003\u8bd5' },
  { value: 'monthly', label: '\u6708\u8003\u8bd5\u5377' },
  { value: 'school', label: '\u6821\u5185\u8bd5\u5377' },
  { value: 'sync', label: '\u540c\u6b65\u8bd5\u5377' }
]

export default {
  name: 'ExamPaperMark',
  components: { ExamPaperMarkWorkspace },
  data() {
    return {
      categoryOptions: CATEGORY_OPTIONS,
      subjectOptions: [],
      parsing: false,
      committing: false,
      parsed: false,
      fileList: [],
      uploadForm: { subjectId: undefined, file: null },
      commitForm: { paperTitle: '', examCategory: 'mock', examYear: '', region: '', sourceFile: '', publishStatus: '1' },
      markedItems: [],
      previewHtml: '',
      visualKey: 0
    }
  },
  created() {
    this.loadSubjects()
  },
  methods: {
    loadSubjects() {
      listSubject({ pageNum: 1, pageSize: 100, status: '0' }).then(res => {
        this.subjectOptions = res.rows || []
      })
    },
    goBack() {
      this.$router.push({ path: '/admin/question-bank-center/exam-paper' })
    },
    resetMark() {
      this.$confirm('\u91cd\u65b0\u4e0a\u4f20\u5c06\u6e05\u7a7a\u5f53\u524d\u6807\u8bb0\u7ed3\u679c\uff0c\u662f\u5426\u7ee7\u7eed\uff1f', '\u63d0\u793a', { type: 'warning' }).then(() => {
        this.parsed = false
        this.fileList = []
        this.uploadForm.file = null
        this.markedItems = []
        this.previewHtml = ''
        this.visualKey = 0
        this.commitForm = { paperTitle: '', examCategory: 'mock', examYear: '', region: '', sourceFile: '', publishStatus: '1' }
      }).catch(() => {})
    },
    onFileChange(file, fileList) {
      this.fileList = fileList.slice(-1)
      this.uploadForm.file = file.raw
      if (!this.commitForm.paperTitle && file.name) {
        this.commitForm.paperTitle = file.name.replace(/\.docx$/i, '')
      }
    },
    parseUpload() {
      if (!this.uploadForm.subjectId) {
        this.$message.warning('\u8bf7\u9009\u62e9\u5b66\u79d1')
        return
      }
      if (!this.uploadForm.file) {
        this.$message.warning('\u8bf7\u9009\u62e9 DOCX \u6587\u4ef6')
        return
      }
      const fd = new FormData()
      fd.append('file', this.uploadForm.file)
      fd.append('subjectId', this.uploadForm.subjectId)
      this.parsing = true
      uploadExamPaper(fd).then(res => {
        const data = res.data || {}
        this.commitForm.sourceFile = data.sourceFile || ''
        this.previewHtml = data.previewHtml || ''
        this.markedItems = (data.markedItems || []).map(item => ({
          ...item,
          scoreValue: item.scoreValue != null ? Number(item.scoreValue) : 5,
          included: item.included !== false
        }))
        if (!this.commitForm.paperTitle && data.fileName) {
          this.commitForm.paperTitle = String(data.fileName).replace(/\.docx$/i, '')
        }
        this.parsed = true
      }).catch(err => {
        const msg = (err && err.response && err.response.data && err.response.data.msg) || (err && err.message) || '\u89e3\u6790\u5931\u8d25'
        this.$message.error(msg)
      }).finally(() => { this.parsing = false })
    },
    submitCommit() {
      if (!this.commitForm.paperTitle) {
        this.$message.warning('\u8bf7\u8f93\u5165\u8bd5\u5377\u6807\u9898')
        return
      }
      this.committing = true
      commitExamPaper({
        paperTitle: this.commitForm.paperTitle,
        subjectId: this.uploadForm.subjectId,
        examCategory: this.commitForm.examCategory,
        examYear: this.commitForm.examYear,
        region: this.commitForm.region,
        sourceFile: this.commitForm.sourceFile,
        publishStatus: '1',
        items: this.markedItems
      }).then(() => {
        this.$message.success('试卷已保存为草稿，可在列表中发布到门户')
        this.goBack()
      }).catch(err => {
        const msg = (err && err.response && err.response.data && err.response.data.msg) || (err && err.message) || '保存失败'
        this.$message.error(msg)
      }).finally(() => { this.committing = false })
    }
  }
}
</script>

<style scoped lang="scss">
.exam-paper-mark-page {
  .page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
  }
  .page-title { font-size: 20px; font-weight: 700; color: #1e293b; }
  .page-desc { margin-top: 6px; font-size: 13px; color: #64748b; }
  .header-actions { display: flex; gap: 8px; flex-shrink: 0; }
  .upload-card { max-width: 640px; }
}
</style>
