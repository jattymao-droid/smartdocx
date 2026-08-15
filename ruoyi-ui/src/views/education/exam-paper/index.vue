<template>
  <div class="app-container education-page exam-paper-page">
    <div class="page-header">
      <div>
        <div class="page-title">试卷选题管理</div>
        <div class="page-desc">上传 DOCX 试卷，智能标记题目并发布到门户</div>
      </div>
      <el-button type="primary" icon="el-icon-upload2" @click="openUpload">上传试卷</el-button>
    </div>

    <el-form :inline="true" size="small" class="filter-form">
      <el-form-item label="标题">
        <el-input v-model="queryParams.paperTitle" clearable placeholder="试卷标题" @keyup.enter.native="getList" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="queryParams.examCategory" clearable placeholder="全部" style="width:140px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.publishStatus" clearable placeholder="全部" style="width:120px">
          <el-option label="已发布" value="0" />
          <el-option label="草稿" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="getList">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="paperList" border>
      <el-table-column prop="paperTitle" label="试卷标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="examCategory" label="分类" width="110">
        <template slot-scope="scope">{{ categoryLabel(scope.row.examCategory) }}</template>
      </el-table-column>
      <el-table-column prop="examYear" label="年份" width="90" />
      <el-table-column prop="itemCount" label="题数" width="70" align="center" />
      <el-table-column prop="totalScore" label="总分" width="80" align="center" />
      <el-table-column prop="publishStatus" label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.publishStatus === '0' ? 'success' : 'info'" size="mini">
            {{ scope.row.publishStatus === '0' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="viewDetail(scope.row)">查看</el-button>
          <el-button v-if="scope.row.publishStatus !== '0'" type="text" size="mini" @click="handlePublish(scope.row, '0')">发布</el-button>
          <el-button v-else type="text" size="mini" @click="handlePublish(scope.row, '1')">下架</el-button>
          <el-button type="text" size="mini" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="detailTitle" :visible.sync="detailVisible" width="800px">
      <el-table :data="detailQuestions" border size="small" max-height="480">
        <el-table-column label="#" width="50" align="center" prop="orderNum" />
        <el-table-column label="题型" width="80" prop="questionType" />
        <el-table-column label="分值" width="70" prop="scoreValue" />
        <el-table-column label="题干" min-width="260" show-overflow-tooltip prop="content" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listExamPaper, getExamPaper, publishExamPaper, delExamPaper } from '@/api/education/examPaper'

const CATEGORY_OPTIONS = [
  { value: 'gaokao', label: '高考真题' },
  { value: 'mock', label: '模拟试卷' },
  { value: 'unit', label: '单元测验' },
  { value: 'mid', label: '期中考试' },
  { value: 'final', label: '期末考试' },
  { value: 'monthly', label: '月考试卷' },
  { value: 'school', label: '校内试卷' },
  { value: 'sync', label: '同步试卷' }
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
      detailVisible: false,
      detailTitle: '',
      detailQuestions: []
    }
  },
  created() {
    this.getList()
  },
  activated() {
    this.getList()
  },
  methods: {
    categoryLabel(code) {
      const hit = CATEGORY_OPTIONS.find(c => c.value === code)
      return hit ? hit.label : code || '-'
    },
    getList() {
      this.loading = true
      listExamPaper(this.queryParams).then(res => {
        this.paperList = res.rows || []
        this.total = res.total || 0
      }).catch(err => {
        this.paperList = []
        this.total = 0
        const msg = (err && err.response && err.response.data && err.response.data.msg) || '加载试卷列表失败'
        this.$message.error(msg)
      }).finally(() => { this.loading = false })
    },
    openUpload() {
      this.$router.push({ name: 'ExamPaperMark' })
    },
    viewDetail(row) {
      getExamPaper(row.paperId).then(res => {
        const data = res.data || {}
        this.detailTitle = data.paperTitle || row.paperTitle
        this.detailQuestions = data.questions || []
        this.detailVisible = true
      }).catch(err => {
        const msg = (err && err.response && err.response.data && err.response.data.msg) || '加载试卷详情失败'
        this.$message.error(msg)
      })
    },
    handlePublish(row, status) {
      publishExamPaper(row.paperId, status).then(() => {
        this.$message.success(status === '0' ? '已发布' : '已下架')
        this.getList()
      }).catch(err => {
        const msg = (err && err.response && err.response.data && err.response.data.msg) || '操作失败'
        this.$message.error(msg)
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除该试卷？', '提示', { type: 'warning' }).then(() => {
        delExamPaper(row.paperId).then(() => {
          this.$message.success('删除成功')
          this.getList()
        }).catch(err => {
          const msg = (err && err.response && err.response.data && err.response.data.msg) || '删除失败'
          this.$message.error(msg)
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
  .danger-text { color: #ef4444; }
}
</style>
