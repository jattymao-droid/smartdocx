<template>
  <div class="app-container education-page qb-create-page">
    <div class="page-header">
      <div>
        <div class="page-title">新增试题</div>
        <div class="page-desc">DOCX/OCR 导入、导入记录、审核与手动录入均在本功能内完成</div>
      </div>
      <div class="page-header-actions">
        <el-button
          v-hasPermi="['education:question:edit']"
          icon="el-icon-setting"
          size="small"
          @click="openQuestionTypeManage"
        >题型管理</el-button>
        <el-button icon="el-icon-back" size="small" @click="goBack">返回题库</el-button>
      </div>
    </div>

    <div class="entry-toolbar">
      <span class="entry-toolbar-label">录入方式</span>
      <el-button v-hasPermi="['education:question:import']" type="info" plain icon="el-icon-upload" size="small" @click="handleDocxImport">DOCX导入</el-button>
      <el-button v-hasPermi="['education:question:import']" type="warning" plain icon="el-icon-camera" size="small" @click="handleOcrImport">OCR导入</el-button>
      <el-button v-hasPermi="['education:question:import']" plain icon="el-icon-time" size="small" @click="handleImportHistory">导入记录</el-button>
      <el-badge :value="pendingAuditCount" :hidden="!pendingAuditCount" class="audit-badge" title="全局待审数量">
        <el-button v-hasPermi="['education:question:audit']" type="primary" plain icon="el-icon-s-check" size="small" @click="handleQuestionAudit">题库审核</el-button>
      </el-badge>
    </div>

    <el-card shadow="never" class="form-card" v-loading="pageLoading">
      <div class="form-section-title">手动录入</div>
      <question-form ref="questionForm" :subject-options="subjectOptions" :question-type-options="questionTypeOptions" />
      <div class="form-footer">
        <el-button @click="goBack">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保 存</el-button>
      </div>
    </el-card>

    <question-type-manage-dialog ref="questionTypeDialog" @updated="reloadQuestionTypes" />
  </div>
</template>

<script>
import { listSubject } from '@/api/education/subject'
import { getPendingAuditCount } from '@/api/education/question'
import { loadQuestionTypeOptions } from '@/utils/questionTypes'
import QuestionForm from './QuestionForm'
import QuestionTypeManageDialog from './components/QuestionTypeManageDialog'

function parseQueryNum(val) {
  if (val == null || val === '') return undefined
  const n = Number(val)
  return Number.isFinite(n) ? n : undefined
}

export default {
  name: 'QuestionCreate',
  components: { QuestionForm, QuestionTypeManageDialog },
  data() {
    return {
      pageLoading: false,
      submitLoading: false,
      pendingAuditCount: 0,
      subjectOptions: [],
      questionTypeOptions: []
    }
  },
  created() {
    this.loadSubjects()
    this.loadQuestionTypes()
    this.loadPendingAuditCount()
  },
  methods: {
    loadQuestionTypes() {
      return loadQuestionTypeOptions().then(options => {
        this.questionTypeOptions = options
      }).catch(() => {})
    },
    reloadQuestionTypes() {
      this.loadQuestionTypes().then(() => {
        const form = this.$refs.questionForm
        if (form && form.form && form.form.questionType) {
          const stillValid = this.questionTypeOptions.some(t => t.value === form.form.questionType)
          if (!stillValid && this.questionTypeOptions.length) {
            form.form.questionType = this.questionTypeOptions[0].value
          }
        }
      })
    },
    openQuestionTypeManage() {
      this.$refs.questionTypeDialog.open()
    },
    loadSubjects() {
      this.pageLoading = true
      listSubject({ pageNum: 1, pageSize: 500 }).then(res => {
        this.subjectOptions = (res.rows || []).filter(s => s.subjectName !== '总分')
        this.initFormDefaults()
      }).finally(() => {
        this.pageLoading = false
      })
    },
    loadPendingAuditCount() {
      if (!this.$auth.hasPermi('education:question:audit')) return
      getPendingAuditCount().then(res => {
        this.pendingAuditCount = (res.data != null) ? res.data : 0
      }).catch(() => {})
    },
    initFormDefaults() {
      const q = this.$route.query || {}
      this.$nextTick(() => {
        this.$refs.questionForm.reset({
          subjectId: parseQueryNum(q.subjectId),
          schoolStage: q.schoolStage || undefined,
          versionId: parseQueryNum(q.versionId),
          textbookId: parseQueryNum(q.textbookId),
          chapterId: parseQueryNum(q.chapterId)
        })
      })
    },
    getImportQuery() {
      const form = this.$refs.questionForm && this.$refs.questionForm.form
      const q = this.$route.query || {}
      return {
        schoolStage: (form && form.schoolStage) || q.schoolStage || undefined,
        subjectId: (form && form.subjectId) || parseQueryNum(q.subjectId) || undefined
      }
    },
    handleDocxImport() {
      this.$router.push({
        path: '/admin/question-bank-center/question-create/docx',
        query: this.getImportQuery()
      })
    },
    handleOcrImport() {
      this.$router.push({
        path: '/admin/question-bank-center/question-create/ocr',
        query: this.getImportQuery()
      })
    },
    handleImportHistory() {
      this.$router.push({ path: '/admin/question-bank-center/question-create/history' })
    },
    handleQuestionAudit() {
      this.$router.push({ path: '/admin/question-bank-center/question-create/audit' })
    },
    goBack() {
      this.$router.push({ path: '/admin/question-bank' })
    },
    submitForm() {
      this.submitLoading = true
      this.$refs.questionForm.submit().then(() => {
        this.$modal.msgSuccess('保存成功')
        this.goBack()
      }).catch(() => {}).finally(() => {
        this.submitLoading = false
      })
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
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.page-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}
.entry-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}
.entry-toolbar-label {
  font-size: 13px;
  color: #606266;
  margin-right: 4px;
}
.audit-badge {
  line-height: 1;
}
.form-card {
  border-radius: 8px;
}
.form-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}
.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
