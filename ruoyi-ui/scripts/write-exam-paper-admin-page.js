const fs = require('fs')
const path = require('path')

const out = path.join(__dirname, '../src/views/education/exam-paper/index.vue')

const content = `<template>
  <div class="app-container education-page exam-paper-page">
    <div class="page-header">
      <div>
        <div class="page-title">${'\u8bd5\u5377\u9009\u9898\u7ba1\u7406'}</div>
        <div class="page-desc">${'\u4e0a\u4f20 DOCX \u8bd5\u5377\uff0c\u667a\u80fd\u6807\u8bb0\u9898\u76ee\u5e76\u53d1\u5e03\u5230\u95e8\u6237'}</div>
      </div>
      <el-button type="primary" icon="el-icon-upload2" @click="openUpload">${'\u4e0a\u4f20\u8bd5\u5377'}</el-button>
    </div>

    <el-form :inline="true" size="small" class="filter-form">
      <el-form-item label="${'\u6807\u9898'}">
        <el-input v-model="queryParams.paperTitle" clearable placeholder="${'\u8bd5\u5377\u6807\u9898'}" @keyup.enter.native="getList" />
      </el-form-item>
      <el-form-item label="${'\u5206\u7c7b'}">
        <el-select v-model="queryParams.examCategory" clearable placeholder="${'\u5168\u90e8'}" style="width:140px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="${'\u72b6\u6001'}">
        <el-select v-model="queryParams.publishStatus" clearable placeholder="${'\u5168\u90e8'}" style="width:120px">
          <el-option label="${'\u5df2\u53d1\u5e03'}" value="0" />
          <el-option label="${'\u8349\u7a3f'}" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="getList">${'\u641c\u7d22'}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="paperList" border>
      <el-table-column prop="paperTitle" label="${'\u8bd5\u5377\u6807\u9898'}" min-width="220" show-overflow-tooltip />
      <el-table-column prop="examCategory" label="${'\u5206\u7c7b'}" width="110">
        <template slot-scope="scope">{{ categoryLabel(scope.row.examCategory) }}</template>
      </el-table-column>
      <el-table-column prop="examYear" label="${'\u5e74\u4efd'}" width="90" />
      <el-table-column prop="itemCount" label="${'\u9898\u6570'}" width="70" align="center" />
      <el-table-column prop="totalScore" label="${'\u603b\u5206'}" width="80" align="center" />
      <el-table-column prop="publishStatus" label="${'\u72b6\u6001'}" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.publishStatus === '0' ? 'success' : 'info'" size="mini">
            {{ scope.row.publishStatus === '0' ? '${'\u5df2\u53d1\u5e03'}' : '${'\u8349\u7a3f'}' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="${'\u4e0a\u4f20\u65f6\u95f4'}" width="160" />
      <el-table-column label="${'\u64cd\u4f5c'}" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="viewDetail(scope.row)">${'\u67e5\u770b'}</el-button>
          <el-button v-if="scope.row.publishStatus !== '0'" type="text" size="mini" @click="handlePublish(scope.row, '0')">${'\u53d1\u5e03'}</el-button>
          <el-button v-else type="text" size="mini" @click="handlePublish(scope.row, '1')">${'\u4e0b\u67b6'}</el-button>
          <el-button type="text" size="mini" class="danger-text" @click="handleDelete(scope.row)">${'\u5220\u9664'}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="uploadStep === 1 ? '${'\u4e0a\u4f20\u8bd5\u5377'}' : '${'\u667a\u80fd\u6807\u8bb0\u9898\u76ee'}'" :visible.sync="uploadVisible" width="960px" top="5vh" @closed="resetUpload">
      <template v-if="uploadStep === 1">
        <el-form label-width="88px" size="small">
          <el-form-item label="${'\u5b66\u79d1'}" required>
            <el-select v-model="uploadForm.subjectId" filterable placeholder="${'\u8bf7\u9009\u62e9\u5b66\u79d1'}" style="width:220px">
              <el-option v-for="s in subjectOptions" :key="s.subjectId" :label="s.subjectName" :value="s.subjectId" />
            </el-select>
          </el-form-item>
          <el-form-item label="DOCX" required>
            <el-upload drag action="#" :auto-upload="false" :limit="1" accept=".docx" :on-change="onFileChange" :file-list="fileList">
              <i class="el-icon-upload" />
              <div class="el-upload__text">${'\u62d6\u62fd\u6216\u70b9\u51fb\u4e0a\u4f20\u8bd5\u5377 DOCX'}</div>
            </el-upload>
          </el-form-item>
        </el-form>
      </template>
      <template v-else>
        <el-form :inline="true" size="small" class="meta-form">
          <el-form-item label="${'\u6807\u9898'}" required>
            <el-input v-model="commitForm.paperTitle" placeholder="${'\u8bd5\u5377\u6807\u9898'}" style="width:220px" />
          </el-form-item>
          <el-form-item label="${'\u5206\u7c7b'}" required>
            <el-select v-model="commitForm.examCategory" style="width:140px">
              <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="${'\u5e74\u4efd'}">
            <el-input v-model="commitForm.examYear" placeholder="2024" style="width:100px" />
          </el-form-item>
          <el-form-item label="${'\u5730\u533a'}">
            <el-input v-model="commitForm.region" placeholder="${'\u7701\u5e02'}" style="width:120px" />
          </el-form-item>
        </el-form>
        <div class="mark-summary">${'\u5df2\u8bc6\u522b'} <b>{{ questionMarkCount }}</b> ${'\u9053\u9898\u76ee'}</div>
        <el-table :data="markedItems" border max-height="420" size="small">
          <el-table-column label="#" width="50" align="center">
            <template slot-scope="scope">{{ scope.row.orderNum || '-' }}</template>
          </el-table-column>
          <el-table-column label="${'\u7c7b\u578b'}" width="90">
            <template slot-scope="scope">
              <el-tag v-if="!scope.row.question" size="mini" type="info">${'\u5206\u7ec4'}</el-tag>
              <el-select v-else v-model="scope.row.questionType" size="mini" style="width:78px">
                <el-option label="${'\u5355\u9009'}" value="single" />
                <el-option label="${'\u5224\u65ad'}" value="judge" />
                <el-option label="${'\u586b\u7a7a'}" value="fill" />
                <el-option label="${'\u7b80\u7b54'}" value="short" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="${'\u5206\u503c'}" width="80" align="center">
            <template slot-scope="scope">
              <el-input-number v-if="scope.row.question" v-model="scope.row.scoreValue" :min="1" :max="30" size="mini" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="${'\u5339\u914d'}" width="90" align="center">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.question && scope.row.matchStatus === 'existing'" size="mini" type="success">${'\u5df2\u6709'}</el-tag>
              <el-tag v-else-if="scope.row.question" size="mini" type="warning">${'\u65b0\u9898'}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="${'\u5185\u5bb9'}" min-width="280" show-overflow-tooltip prop="content" />
          <el-table-column label="${'\u5bfc\u5165'}" width="70" align="center">
            <template slot-scope="scope">
              <el-checkbox v-if="scope.row.question" v-model="scope.row.included" />
            </template>
          </el-table-column>
        </el-table>
      </template>
      <div slot="footer">
        <el-button @click="uploadVisible = false">${'\u53d6\u6d88'}</el-button>
        <el-button v-if="uploadStep === 1" type="primary" :loading="parsing" @click="parseUpload">${'\u89e3\u6790\u6807\u8bb0'}</el-button>
        <el-button v-else type="primary" :loading="committing" @click="submitCommit">${'\u4fdd\u5b58\u5e76\u53d1\u5e03'}</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="detailTitle" :visible.sync="detailVisible" width="800px">
      <el-table :data="detailQuestions" border size="small" max-height="480">
        <el-table-column label="#" width="50" align="center" prop="orderNum" />
        <el-table-column label="${'\u9898\u578b'}" width="80" prop="questionType" />
        <el-table-column label="${'\u5206\u503c'}" width="70" prop="scoreValue" />
        <el-table-column label="${'\u9898\u5e72'}" min-width="260" show-overflow-tooltip prop="content" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listSubject } from '@/api/education/subject'
import { listExamPaper, getExamPaper, uploadExamPaper, commitExamPaper, publishExamPaper, delExamPaper } from '@/api/education/examPaper'

const CATEGORY_OPTIONS = [
  { value: 'gaokao', label: '${'\u9ad8\u8003\u771f\u9898'}' },
  { value: 'mock', label: '${'\u6a21\u62df\u8bd5\u5377'}' },
  { value: 'unit', label: '${'\u5355\u5143\u6d4b\u9a8c'}' },
  { value: 'mid', label: '${'\u671f\u4e2d\u8003\u8bd5'}' },
  { value: 'final', label: '${'\u671f\u672b\u8003\u8bd5'}' },
  { value: 'monthly', label: '${'\u6708\u8003\u8bd5\u5377'}' },
  { value: 'school', label: '${'\u6821\u5185\u8bd5\u5377'}' },
  { value: 'sync', label: '${'\u540c\u6b65\u8bd5\u5377'}' }
]

export default {
  name: 'ExamPaperAdmin',
  data() {
    return {
      loading: false,
      paperList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, paperTitle: undefined, examCategory: undefined, publishStatus: undefined },
      categoryOptions: CATEGORY_OPTIONS,
      subjectOptions: [],
      uploadVisible: false,
      uploadStep: 1,
      parsing: false,
      committing: false,
      fileList: [],
      uploadForm: { subjectId: undefined, file: null },
      commitForm: { paperTitle: '', examCategory: 'mock', examYear: '', region: '', sourceFile: '', publishStatus: '0' },
      markedItems: [],
      detailVisible: false,
      detailTitle: '',
      detailQuestions: []
    }
  },
  computed: {
    questionMarkCount() {
      return (this.markedItems || []).filter(i => i.question && i.included).length
    }
  },
  created() {
    this.loadSubjects()
    this.getList()
  },
  methods: {
    categoryLabel(code) {
      const hit = CATEGORY_OPTIONS.find(c => c.value === code)
      return hit ? hit.label : code || '-'
    },
    loadSubjects() {
      listSubject({ pageNum: 1, pageSize: 100, status: '0' }).then(res => {
        this.subjectOptions = res.rows || []
      })
    },
    getList() {
      this.loading = true
      listExamPaper(this.queryParams).then(res => {
        this.paperList = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    openUpload() {
      this.uploadVisible = true
      this.uploadStep = 1
    },
    resetUpload() {
      this.uploadStep = 1
      this.fileList = []
      this.uploadForm = { subjectId: undefined, file: null }
      this.markedItems = []
    },
    onFileChange(file, fileList) {
      this.fileList = fileList.slice(-1)
      this.uploadForm.file = file.raw
      if (!this.commitForm.paperTitle && file.name) {
        this.commitForm.paperTitle = file.name.replace(/\\.docx$/i, '')
      }
    },
    parseUpload() {
      if (!this.uploadForm.subjectId) {
        this.$message.warning('${'\u8bf7\u9009\u62e9\u5b66\u79d1'}')
        return
      }
      if (!this.uploadForm.file) {
        this.$message.warning('${'\u8bf7\u9009\u62e9 DOCX \u6587\u4ef6'}')
        return
      }
      const fd = new FormData()
      fd.append('file', this.uploadForm.file)
      fd.append('subjectId', this.uploadForm.subjectId)
      this.parsing = true
      uploadExamPaper(fd).then(res => {
        const data = res.data || {}
        this.commitForm.sourceFile = data.sourceFile || ''
        this.markedItems = (data.markedItems || []).map(item => ({
          ...item,
          scoreValue: item.scoreValue != null ? Number(item.scoreValue) : 5,
          included: item.included !== false
        }))
        if (!this.commitForm.paperTitle && data.fileName) {
          this.commitForm.paperTitle = String(data.fileName).replace(/\\.docx$/i, '')
        }
        this.uploadStep = 2
      }).catch(() => {
        this.$message.error('${'\u89e3\u6790\u5931\u8d25'}')
      }).finally(() => { this.parsing = false })
    },
    submitCommit() {
      if (!this.commitForm.paperTitle) {
        this.$message.warning('${'\u8bf7\u8f93\u5165\u8bd5\u5377\u6807\u9898'}')
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
        publishStatus: '0',
        items: this.markedItems
      }).then(() => {
        this.$message.success('${'\u8bd5\u5377\u5df2\u4fdd\u5b58\u5e76\u53d1\u5e03'}')
        this.uploadVisible = false
        this.getList()
      }).finally(() => { this.committing = false })
    },
    viewDetail(row) {
      getExamPaper(row.paperId).then(res => {
        const data = res.data || {}
        this.detailTitle = data.paperTitle || row.paperTitle
        this.detailQuestions = data.questions || []
        this.detailVisible = true
      })
    },
    handlePublish(row, status) {
      publishExamPaper(row.paperId, status).then(() => {
        this.$message.success(status === '0' ? '${'\u5df2\u53d1\u5e03'}' : '${'\u5df2\u4e0b\u67b6'}')
        this.getList()
      })
    },
    handleDelete(row) {
      this.$confirm('${'\u786e\u8ba4\u5220\u9664\u8be5\u8bd5\u5377\uff1f'}', '${'\u63d0\u793a'}', { type: 'warning' }).then(() => {
        delExamPaper(row.paperId).then(() => {
          this.$message.success('${'\u5220\u9664\u6210\u529f'}')
          this.getList()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
.exam-paper-page {
  .page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
  .page-title { font-size: 20px; font-weight: 700; color: #1e293b; }
  .page-desc { margin-top: 6px; font-size: 13px; color: #64748b; }
  .filter-form { margin-bottom: 12px; }
  .mark-summary { margin: 0 0 10px; font-size: 13px; color: #475569; b { color: #2563eb; } }
  .meta-form { margin-bottom: 8px; }
  .danger-text { color: #ef4444; }
}
</style>
`

fs.mkdirSync(path.dirname(out), { recursive: true })
fs.writeFileSync(out, content, 'utf8')
console.log('Wrote', out)
