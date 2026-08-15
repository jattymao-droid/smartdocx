<template>
  <div class="app-container education-page audit-page">
    <div class="page-header">
      <div>
        <div class="page-title">题库审核</div>
        <div class="page-desc">待审核试题批量通过或退回（角标为全局待审数）</div>
      </div>
      <el-button icon="el-icon-back" size="small" @click="goBack">返回新增试题</el-button>
    </div>

    <el-form :inline="true" size="small" class="filter-form">
      <el-form-item label="学科">
        <el-select v-model="queryParams.subjectId" clearable filterable placeholder="学科" style="width:140px">
          <el-option v-for="item in subjectOptions" :key="item.subjectId" :label="item.subjectName" :value="item.subjectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="题型">
        <el-select v-model="queryParams.questionType" clearable placeholder="题型" style="width:120px">
          <el-option v-for="item in resolvedQuestionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" clearable placeholder="题干关键词" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-check" size="mini" :disabled="multiple" @click="handleSubmitApprove">批量通过</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-close" size="mini" :disabled="multiple" @click="openRejectDialog">批量退回</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="questionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" prop="questionCode" width="150" show-overflow-tooltip />
      <el-table-column label="题干" prop="content" min-width="220" show-overflow-tooltip />
      <el-table-column label="学科" prop="subjectName" width="90" align="center" />
      <el-table-column label="题型" prop="questionType" width="80" align="center">
        <template slot-scope="scope">{{ questionTypeLabel(scope.row.questionType) }}</template>
      </el-table-column>
      <el-table-column label="录入人" prop="createBy" width="100" align="center" />
      <el-table-column label="录入时间" prop="createTime" width="160" align="center">
        <template slot-scope="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="approveOne(scope.row)">通过</el-button>
          <el-button type="text" size="mini" class="danger-text" @click="rejectOne(scope.row)">退回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="退回原因" :visible.sync="rejectOpen" width="480px" append-to-body>
      <el-input v-model="rejectRemark" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请填写退回原因" />
      <div slot="footer">
        <el-button @click="rejectOpen = false">取 消</el-button>
        <el-button type="danger" :loading="rejectLoading" @click="confirmReject">确 定退回</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listQuestion, auditQuestions } from '@/api/education/question'
import { listSubject } from '@/api/education/subject'
import dynamicQuestionTypes from '@/mixins/dynamicQuestionTypes'

export default {
  name: 'QuestionAudit',
  mixins: [dynamicQuestionTypes],
  data() {
    return {
      loading: false,
      rejectLoading: false,
      rejectOpen: false,
      rejectRemark: '',
      pendingRejectIds: [],
      total: 0,
      questionList: [],
      subjectOptions: [],
      ids: [],
      multiple: true,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        status: '1',
        subjectId: undefined,
        questionType: undefined,
        keyword: undefined
      }
    }
  },
  created() {
    this.loadSubjects()
    this.getList()
  },
  methods: {
    goBack() {
      this.$router.push({ path: '/admin/question-bank/question-create' })
    },
    loadSubjects() {
      listSubject({ pageNum: 1, pageSize: 500 }).then(res => {
        this.subjectOptions = res.rows || []
      }).catch(() => {}).catch(() => {}).catch(() => {}).catch(() => {}).catch(() => {})
    },
    getList() {
      this.loading = true
      listQuestion({ ...this.queryParams }).then(res => {
        this.questionList = res.rows || []
        this.total = res.total || 0
      }).catch(() => {
        this.$modal.msgError('加载审核列表失败')
      }).finally(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        status: '1',
        subjectId: undefined,
        questionType: undefined,
        keyword: undefined
      }
      this.getList()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.questionId)
      this.multiple = !selection.length
    },
    handleSubmitApprove() {
      this.doAudit(this.ids, 'approve')
    },
    approveOne(row) {
      this.doAudit([row.questionId], 'approve')
    },
    rejectOne(row) {
      this.pendingRejectIds = [row.questionId]
      this.rejectRemark = ''
      this.rejectOpen = true
    },
    openRejectDialog() {
      this.pendingRejectIds = [...this.ids]
      this.rejectRemark = ''
      this.rejectOpen = true
    },
    confirmReject() {
      if (!this.rejectRemark || !this.rejectRemark.trim()) {
        this.$modal.msgWarning('请填写退回原因')
        return
      }
      this.rejectLoading = true
      this.doAudit(this.pendingRejectIds, 'reject', this.rejectRemark.trim()).finally(() => {
        this.rejectLoading = false
        this.rejectOpen = false
      })
    },
    doAudit(questionIds, action, remark) {
      if (!questionIds || !questionIds.length) {
        this.$modal.msgWarning('请先选择试题')
        return Promise.reject()
      }
      const label = action === 'approve' ? '通过' : '退回'
      return this.$modal.confirm(`确认对所选试题执行「${label}」操作？`).then(() => {
        return auditQuestions({ questionIds, action, remark }).then(() => {
          this.$modal.msgSuccess('审核成功')
          this.getList()
        })
      }).catch(() => {})
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

.danger-text {
  color: #f56c6c;
}
</style>
