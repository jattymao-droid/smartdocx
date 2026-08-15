<template>
  <el-dialog
    :title="t.dialogTitle"
    :visible.sync="visible"
    width="920px"
    append-to-body
    :close-on-click-modal="false"
    @open="handleOpen"
    @close="handleClose"
  >
    <div class="toolbar">
      <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">{{ t.addType }}</el-button>
      <el-button plain icon="el-icon-refresh" size="mini" @click="loadList">{{ t.refresh }}</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border size="small" max-height="420">
      <el-table-column :label="t.colOrder" prop="orderNum" width="70" align="center" />
      <el-table-column :label="t.colCode" prop="typeCode" width="130" />
      <el-table-column :label="t.colName" prop="typeName" width="110" />
      <el-table-column :label="t.colAnswerMode" width="100" align="center">
        <template slot-scope="scope">
          {{ answerModeLabel(scope.row.answerMode) }}
        </template>
      </el-table-column>
      <el-table-column :label="t.colContentMax" width="100" align="center">
        <template slot-scope="scope">
          {{ scope.row.contentMaxLen || t.defaultLabel }}
        </template>
      </el-table-column>
      <el-table-column :label="t.colStatus" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">
            {{ scope.row.status === '0' ? t.enabled : t.disabled }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t.colBuiltin" width="70" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.builtin === '1'" size="mini" type="warning">{{ t.yes }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column :label="t.colRemark" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column :label="t.colAction" width="140" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="handleEdit(scope.row)">{{ t.edit }}</el-button>
          <el-button
            v-if="scope.row.builtin !== '1'"
            type="text"
            size="mini"
            class="danger-text"
            @click="handleDelete(scope.row)"
          >{{ t.remove }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :title="formTitle"
      :visible.sync="formVisible"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" size="small">
        <el-form-item :label="t.fieldCode" prop="typeCode">
          <el-input
            v-model="form.typeCode"
            :disabled="!!form.typeId && form.builtin === '1'"
            :placeholder="t.codePlaceholder"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item :label="t.fieldName" prop="typeName">
          <el-input v-model="form.typeName" maxlength="50" />
        </el-form-item>
        <el-form-item :label="t.fieldAnswerMode" prop="answerMode">
          <el-select
            v-model="form.answerMode"
            :disabled="!!form.typeId && form.builtin === '1'"
            style="width: 100%"
          >
            <el-option
              v-for="item in answerModeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t.fieldContentMax">
          <el-input-number
            v-model="form.contentMaxLen"
            :min="100"
            :max="50000"
            :step="100"
            controls-position="right"
            :placeholder="t.contentMaxPlaceholder"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t.fieldOrder" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t.fieldStatus" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">{{ t.enabled }}</el-radio>
            <el-radio label="1">{{ t.disabled }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t.fieldRemark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="formVisible = false">{{ t.cancel }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">{{ t.confirm }}</el-button>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import {
  listQuestionType,
  addQuestionType,
  updateQuestionType,
  delQuestionType
} from '@/api/education/questionType'
import { ANSWER_MODE_OPTIONS, ANSWER_MODE_LABELS, clearQuestionTypeCache } from '@/utils/questionTypes'

const T = {
  dialogTitle: '\u9898\u578b\u7ba1\u7406',
  addType: '\u65b0\u589e\u9898\u578b',
  refresh: '\u5237\u65b0',
  colOrder: '\u6392\u5e8f',
  colCode: '\u7f16\u7801',
  colName: '\u540d\u79f0',
  colAnswerMode: '\u7b54\u9898\u6a21\u5f0f',
  colContentMax: '\u9898\u5e72\u4e0a\u9650',
  defaultLabel: '\u9ed8\u8ba4',
  colStatus: '\u72b6\u6001',
  enabled: '\u542f\u7528',
  disabled: '\u505c\u7528',
  colBuiltin: '\u5185\u7f6e',
  yes: '\u662f',
  colRemark: '\u5907\u6ce8',
  colAction: '\u64cd\u4f5c',
  edit: '\u7f16\u8f91',
  remove: '\u5220\u9664',
  fieldCode: '\u9898\u578b\u7f16\u7801',
  codePlaceholder: '\u5c0f\u5199\u5b57\u6bcd\u5f00\u5934\uff0c\u5982 custom_type',
  fieldName: '\u9898\u578b\u540d\u79f0',
  fieldAnswerMode: '\u7b54\u9898\u6a21\u5f0f',
  fieldContentMax: '\u9898\u5e72\u4e0a\u9650',
  contentMaxPlaceholder: '\u7559\u7a7a\u7528\u9ed8\u8ba4',
  fieldOrder: '\u6392\u5e8f',
  fieldStatus: '\u72b6\u6001',
  fieldRemark: '\u5907\u6ce8',
  cancel: '\u53d6\u6d88',
  confirm: '\u786e\u5b9a',
  addFormTitle: '\u65b0\u589e\u9898\u578b',
  editFormTitle: '\u7f16\u8f91\u9898\u578b',
  updateSuccess: '\u4fee\u6539\u6210\u529f',
  addSuccess: '\u65b0\u589e\u6210\u529f',
  deleteSuccess: '\u5220\u9664\u6210\u529f',
  deleteConfirmPrefix: '\u786e\u8ba4\u5220\u9664\u9898\u578b\u300c',
  deleteConfirmSuffix: '\u300d\uff1f',
  ruleCodeRequired: '\u8bf7\u8f93\u5165\u9898\u578b\u7f16\u7801',
  ruleCodePattern: '\u7f16\u7801\u683c\u5f0f\u4e0d\u6b63\u786e',
  ruleNameRequired: '\u8bf7\u8f93\u5165\u9898\u578b\u540d\u79f0',
  ruleAnswerModeRequired: '\u8bf7\u9009\u62e9\u7b54\u9898\u6a21\u5f0f',
  ruleOrderRequired: '\u8bf7\u8f93\u5165\u6392\u5e8f',
  ruleStatusRequired: '\u8bf7\u9009\u62e9\u72b6\u6001'
}

export default {
  name: 'QuestionTypeManageDialog',
  data() {
    return {
      t: T,
      visible: false,
      loading: false,
      submitLoading: false,
      list: [],
      formVisible: false,
      formTitle: '',
      answerModeOptions: ANSWER_MODE_OPTIONS,
      form: this.emptyForm(),
      rules: {
        typeCode: [
          { required: true, message: T.ruleCodeRequired, trigger: 'blur' },
          { pattern: /^[a-z][a-z0-9_]{0,31}$/, message: T.ruleCodePattern, trigger: 'blur' }
        ],
        typeName: [{ required: true, message: T.ruleNameRequired, trigger: 'blur' }],
        answerMode: [{ required: true, message: T.ruleAnswerModeRequired, trigger: 'change' }],
        orderNum: [{ required: true, message: T.ruleOrderRequired, trigger: 'blur' }],
        status: [{ required: true, message: T.ruleStatusRequired, trigger: 'change' }]
      }
    }
  },
  methods: {
    open() {
      this.visible = true
    },
    handleOpen() {
      this.loadList()
    },
    handleClose() {
      this.formVisible = false
    },
    emptyForm() {
      return {
        typeId: undefined,
        typeCode: '',
        typeName: '',
        answerMode: 'subjective',
        contentMaxLen: undefined,
        orderNum: 99,
        status: '0',
        builtin: '0',
        remark: ''
      }
    },
    answerModeLabel(mode) {
      return ANSWER_MODE_LABELS[mode] || mode || '-'
    },
    loadList() {
      this.loading = true
      listQuestionType({ pageNum: 1, pageSize: 200 }).then(res => {
        this.list = res.rows || []
      }).finally(() => {
        this.loading = false
      })
    },
    handleAdd() {
      this.formTitle = T.addFormTitle
      this.form = this.emptyForm()
      this.formVisible = true
      this.$nextTick(() => {
        if (this.$refs.formRef) this.$refs.formRef.clearValidate()
      })
    },
    handleEdit(row) {
      this.formTitle = T.editFormTitle
      this.form = {
        typeId: row.typeId,
        typeCode: row.typeCode,
        typeName: row.typeName,
        answerMode: row.answerMode,
        contentMaxLen: row.contentMaxLen || undefined,
        orderNum: row.orderNum != null ? row.orderNum : 0,
        status: row.status || '0',
        builtin: row.builtin,
        remark: row.remark || ''
      }
      this.formVisible = true
      this.$nextTick(() => {
        if (this.$refs.formRef) this.$refs.formRef.clearValidate()
      })
    },
    resetForm() {
      this.form = this.emptyForm()
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const payload = { ...this.form }
        if (!payload.contentMaxLen) {
          payload.contentMaxLen = null
        }
        const req = payload.typeId ? updateQuestionType(payload) : addQuestionType(payload)
        req.then(() => {
          this.$modal.msgSuccess(payload.typeId ? T.updateSuccess : T.addSuccess)
          this.formVisible = false
          clearQuestionTypeCache()
          this.loadList()
          this.$emit('updated')
        }).finally(() => {
          this.submitLoading = false
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm(T.deleteConfirmPrefix + row.typeName + T.deleteConfirmSuffix).then(() => {
        return delQuestionType(row.typeId)
      }).then(() => {
        this.$modal.msgSuccess(T.deleteSuccess)
        clearQuestionTypeCache()
        this.loadList()
        this.$emit('updated')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
.toolbar {
  margin-bottom: 12px;
}
.danger-text {
  color: #f56c6c;
}
</style>
