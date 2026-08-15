<template>
  <el-dialog title="试题纠错" :visible.sync="visible" width="520px" append-to-body @open="onOpen">
    <div v-if="question" class="fb-source">
      <span>{{ question.questionCode }}</span>
      <span class="fb-brief">{{ briefContent(question) }}</span>
    </div>
    <el-form ref="form" :model="form" :rules="rules" label-width="80px" size="small">
      <el-form-item label="纠错类型" prop="feedbackType">
        <el-radio-group v-model="form.feedbackType">
          <el-radio v-for="t in feedbackTypes" :key="t.value" :label="t.value">{{ t.label }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="问题描述" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请详细描述题目问题，便于审核修正" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button v-if="canEdit" type="warning" plain @click="goEdit">直接修改题目</el-button>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交纠错</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { submitQuestionFeedback, getQuestion } from '@/api/education/question'
import { stripLeadingQuestionNo } from '@/utils/questionContent'

export default {
  name: 'PaperQuestionFeedbackDialog',
  props: {
    value: { type: Boolean, default: false },
    question: { type: Object, default: null },
    paperTitle: { type: String, default: '' }
  },
  data() {
    return {
      submitting: false,
      canEdit: false,
      form: { feedbackType: 'content', content: '' },
      feedbackTypes: [
        { value: 'content', label: '题干错误' },
        { value: 'answer', label: '答案错误' },
        { value: 'analysis', label: '解析错误' },
        { value: 'image', label: '图片问题' },
        { value: 'typo', label: '其它问题' }
      ],
      rules: {
        feedbackType: [{ required: true, message: '请选择类型', trigger: 'change' }],
        content: [{ required: true, message: '请填写描述', trigger: 'blur' }]
      }
    }
  },
  computed: {
    visible: {
      get() { return this.value },
      set(val) { this.$emit('input', val) }
    }
  },
  methods: {
    briefContent(item) {
      const text = stripLeadingQuestionNo(item.content || item.contentBrief || '')
      return text.length > 80 ? text.slice(0, 80) + '...' : text
    },
    onOpen() {
      this.form = { feedbackType: 'content', content: '' }
      this.canEdit = false
      if (this.question && this.question.questionId) {
        getQuestion(this.question.questionId).then(res => {
          const data = res.data || {}
          this.canEdit = !!(data.params && data.params.canManage)
        }).catch(() => {})
      }
      this.$nextTick(() => { if (this.$refs.form) this.$refs.form.clearValidate() })
    },
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid || !this.question) return
        this.submitting = true
        submitQuestionFeedback({
          questionId: this.question.questionId,
          feedbackType: this.form.feedbackType,
          content: this.form.content,
          paperTitle: this.paperTitle
        }).then(() => {
          this.$modal.msgSuccess('纠错已提交，感谢反馈')
          this.visible = false
        }).finally(() => { this.submitting = false })
      })
    },
    goEdit() {
      if (!this.question) return
      this.visible = false
      this.$router.push({ path: '/admin/question-bank', query: { editId: this.question.questionId } })
    }
  }
}
</script>

<style scoped lang="scss">
.fb-source {
  font-size: 13px;
  color: #606266;
  margin-bottom: 14px;
  padding: 8px 10px;
  background: #fafafa;
  border-radius: 4px;
  .fb-brief { display: block; margin-top: 4px; color: #303133; line-height: 1.5; }
}
::v-deep .el-radio { display: block; margin: 6px 0; }
</style>
