<template>
  <div class="app-container education-page import-history-page">
    <div class="page-header">
      <div>
        <div class="page-title">导入记录</div>
        <div class="page-desc">DOCX 导入任务与 OCR 草稿历史（P3）</div>
      </div>
      <el-button icon="el-icon-back" size="small" @click="goBack">返回新增试题</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-click="onTabChange">
      <el-tab-pane label="DOCX 导入" name="docx">
        <el-form :inline="true" size="small" class="filter-form">
          <el-form-item label="文件名">
            <el-input v-model="docxQuery.fileName" clearable placeholder="模糊搜索" @keyup.enter.native="loadDocx" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="docxQuery.status" clearable placeholder="全部" style="width:120px">
              <el-option v-for="item in docxStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadDocx">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="docxLoading" :data="docxList" size="small">
          <el-table-column label="ID" prop="taskId" width="70" />
          <el-table-column label="文件" prop="fileName" min-width="180" show-overflow-tooltip />
          <el-table-column label="学科" prop="subjectName" width="90" />
          <el-table-column label="状态" width="90" align="center">
            <template slot-scope="scope">
              <el-tag size="mini" :type="docxStatusType(scope.row.status)">{{ docxStatusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="段落" prop="blockCount" width="70" align="center" />
          <el-table-column label="已导入" prop="importedCount" width="80" align="center" />
          <el-table-column label="操作人" prop="createBy" width="100" />
          <el-table-column label="时间" prop="createTime" width="160">
            <template slot-scope="scope">{{ parseTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button
                v-if="scope.row.status === 'parsed' || scope.row.status === 'failed'"
                type="text"
                size="mini"
                @click="resumeDocx(scope.row)"
              >{{ scope.row.status === 'failed' ? '重试导入' : '继续导入' }}</el-button>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="docxTotal > 0"
          :total="docxTotal"
          :page.sync="docxQuery.pageNum"
          :limit.sync="docxQuery.pageSize"
          @pagination="loadDocx"
        />
      </el-tab-pane>

      <el-tab-pane label="OCR 草稿" name="ocr">
        <el-form :inline="true" size="small" class="filter-form">
          <el-form-item label="状态">
            <el-select v-model="ocrQuery.status" clearable placeholder="全部" style="width:120px">
              <el-option v-for="item in ocrStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadOcr">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="ocrLoading" :data="ocrList" size="small">
          <el-table-column label="ID" prop="draftId" width="70" />
          <el-table-column label="预览文本" prop="ocrText" min-width="200" show-overflow-tooltip />
          <el-table-column label="学科" prop="subjectName" width="90" />
          <el-table-column label="预判题型" prop="predictedType" width="90" align="center" />
          <el-table-column label="置信度" prop="confidence" width="80" align="center">
            <template slot-scope="scope">{{ formatConf(scope.row.confidence) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.status === 'committed' ? 'success' : 'warning'">
                {{ scope.row.status === 'committed' ? '已入库' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="题目ID" prop="questionId" width="80" align="center" />
          <el-table-column label="时间" prop="createTime" width="160">
            <template slot-scope="scope">{{ parseTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button
                v-if="scope.row.status === 'draft'"
                type="text"
                size="mini"
                @click="resumeOcr(scope.row)"
              >继续校对</el-button>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="ocrTotal > 0"
          :total="ocrTotal"
          :page.sync="ocrQuery.pageNum"
          :limit.sync="ocrQuery.pageSize"
          @pagination="loadOcr"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { listImportTasks } from '@/api/education/questionImport'
import { listOcrDrafts } from '@/api/education/questionOcr'

export default {
  name: 'ImportHistory',
  data() {
    return {
      activeTab: 'docx',
      docxLoading: false,
      ocrLoading: false,
      docxList: [],
      ocrList: [],
      docxTotal: 0,
      ocrTotal: 0,
      docxQuery: { pageNum: 1, pageSize: 10, fileName: undefined, status: undefined },
      ocrQuery: { pageNum: 1, pageSize: 10, status: undefined },
      docxStatusOptions: [
        { label: '已解析', value: 'parsed' },
        { label: '导入中', value: 'importing' },
        { label: '已完成', value: 'done' },
        { label: '失败', value: 'failed' }
      ],
      ocrStatusOptions: [
        { label: '草稿', value: 'draft' },
        { label: '已入库', value: 'committed' }
      ]
    }
  },
  created() {
    this.loadDocx()
  },
  methods: {
    goBack() {
      this.$router.push({ path: '/admin/question-bank/question-create' })
    },
    onTabChange(tab) {
      if (tab.name === 'ocr' && !this.ocrList.length) this.loadOcr()
    },
    loadDocx() {
      this.docxLoading = true
      listImportTasks(this.docxQuery).then(res => {
        this.docxList = res.rows || []
        this.docxTotal = res.total || 0
      }).catch(() => {
        this.$modal.msgError('加载 DOCX 导入记录失败')
      }).finally(() => { this.docxLoading = false })
    },
    loadOcr() {
      this.ocrLoading = true
      listOcrDrafts(this.ocrQuery).then(res => {
        this.ocrList = res.rows || []
        this.ocrTotal = res.total || 0
      }).catch(() => {
        this.$modal.msgError('加载 OCR 草稿记录失败')
      }).finally(() => { this.ocrLoading = false })
    },
    docxStatusLabel(s) {
      const m = { parsed: '已解析', importing: '导入中', done: '已完成', failed: '失败' }
      return m[s] || s
    },
    docxStatusType(s) {
      if (s === 'done') return 'success'
      if (s === 'failed') return 'danger'
      if (s === 'importing') return 'warning'
      return 'info'
    },
    formatConf(v) {
      if (v == null) return '-'
      return (Number(v) * 100).toFixed(0) + '%'
    },
    resumeDocx(row) {
      this.$router.push({ path: '/admin/question-bank-center/question-create/docx', query: { taskId: row.taskId } })
    },
    resumeOcr(row) {
      this.$router.push({ path: '/admin/question-bank-center/question-create/ocr', query: { draftId: row.draftId } })
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
.filter-form {
  margin-bottom: 12px;
}
.muted {
  color: #c0c4cc;
}
</style>
