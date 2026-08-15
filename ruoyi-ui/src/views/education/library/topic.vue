<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="80px">
      <el-form-item :label="L.title" prop="title">
        <el-input v-model="queryParams.title" :placeholder="L.titlePh" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item :label="L.status" prop="status">
        <el-select v-model="queryParams.status" :placeholder="L.status" clearable>
          <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">{{ L.search }}</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">{{ L.reset }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['education:library:topic:add']">{{ L.add }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['education:library:topic:edit']">{{ L.edit }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['education:library:topic:remove']">{{ L.remove }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="el-icon-back" size="mini" @click="goBack">{{ L.back }}</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column :label="L.id" align="center" prop="topicId" width="80" />
      <el-table-column :label="L.title" align="left" prop="title" min-width="160" show-overflow-tooltip />
      <el-table-column :label="L.docCount" align="center" prop="docCount" width="90" />
      <el-table-column :label="L.price" align="center" prop="bundlePrice" width="100">
        <template slot-scope="scope">
          <span>{{ formatPrice(scope.row.bundlePrice) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="L.downloads" align="center" prop="downloadCount" width="90" />
      <el-table-column :label="L.order" align="center" prop="orderNum" width="80" />
      <el-table-column :label="L.status" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column :label="L.action" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['education:library:topic:edit']">{{ L.edit }}</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['education:library:topic:remove']">{{ L.remove }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body @close="cancel">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="L.title" prop="title">
          <el-input v-model="form.title" :placeholder="L.titlePh" maxlength="200" />
        </el-form-item>
        <el-form-item :label="L.summary" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="1000" :placeholder="L.summaryPh" />
        </el-form-item>
        <el-form-item :label="L.cover" prop="coverUrl">
          <image-upload v-model="form.coverUrl" :limit="1" :file-size="5" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="L.price" prop="bundlePrice">
              <el-input-number v-model="form.bundlePrice" :min="0" :precision="2" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="L.order" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="L.status" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="L.documents" prop="documentIds">
          <div class="topic-doc-toolbar">
            <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="openDocPicker">{{ L.pickDocs }}</el-button>
            <span class="topic-doc-hint">{{ L.selectedCount.replace('{n}', selectedDocs.length) }}</span>
          </div>
          <el-table v-if="selectedDocs.length" :data="selectedDocs" size="mini" max-height="240" class="topic-doc-table">
            <el-table-column :label="L.docTitle" prop="title" min-width="200" show-overflow-tooltip />
            <el-table-column :label="L.format" prop="fileExt" width="80" align="center" />
            <el-table-column :label="L.action" width="80" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="removeDoc(scope.row.documentId)">{{ L.removeDoc }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">{{ L.confirm }}</el-button>
        <el-button @click="cancel">{{ L.cancel }}</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="L.pickDocs" :visible.sync="docPickerOpen" width="800px" append-to-body>
      <el-form :model="docQuery" size="small" :inline="true">
        <el-form-item>
          <el-input v-model="docQuery.keyword" :placeholder="L.docSearchPh" clearable @keyup.enter.native="searchDocs" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="searchDocs">{{ L.search }}</el-button>
        </el-form-item>
      </el-form>
      <el-table
        v-loading="docLoading"
        :data="docPickerList"
        size="small"
        max-height="360"
        @selection-change="onDocPickerSelect"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column :label="L.docTitle" prop="title" min-width="220" show-overflow-tooltip />
        <el-table-column :label="L.format" prop="fileExt" width="70" align="center" />
        <el-table-column :label="L.uploader" prop="createBy" width="100" />
      </el-table>
      <pagination
        v-show="docTotal > 0"
        small
        :total="docTotal"
        :page.sync="docQuery.pageNum"
        :limit.sync="docQuery.pageSize"
        @pagination="searchDocs"
      />
      <div slot="footer">
        <el-button type="primary" @click="confirmDocPicker">{{ L.confirm }}</el-button>
        <el-button @click="docPickerOpen = false">{{ L.cancel }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listAdminLibraryTopics,
  getAdminLibraryTopic,
  addLibraryTopic,
  updateLibraryTopic,
  delLibraryTopic,
  listLibraryDocuments
} from '@/api/education/library'

const L = {
  title: '\u4e13\u9898\u6807\u9898',
  titlePh: '\u8bf7\u8f93\u5165\u4e13\u9898\u6807\u9898',
  summary: '\u7b80\u4ecb',
  summaryPh: '\u53ef\u9009\uff0c\u5c55\u793a\u5728\u95e8\u6237\u4e13\u9898\u9875',
  cover: '\u5c01\u9762\u56fe',
  coverPh: '\u652f\u6301 jpg/png\uff0c\u5355\u5f20\u4e0d\u8d85\u8fc7 5MB',
  status: '\u72b6\u6001',
  search: '\u641c\u7d22',
  reset: '\u91cd\u7f6e',
  add: '\u65b0\u589e',
  edit: '\u4fee\u6539',
  remove: '\u5220\u9664',
  back: '\u8fd4\u56de\u6587\u6863',
  id: '\u7f16\u53f7',
  docCount: '\u6587\u6863\u6570',
  price: '\u6253\u5305\u4ef7',
  downloads: '\u4e0b\u8f7d\u6b21\u6570',
  order: '\u6392\u5e8f',
  action: '\u64cd\u4f5c',
  confirm: '\u786e \u5b9a',
  cancel: '\u53d6 \u6d88',
  documents: '\u5305\u542b\u6587\u6863',
  pickDocs: '\u9009\u62e9\u6587\u6863',
  selectedCount: '\u5df2\u9009 {n} \u4efd',
  docTitle: '\u6587\u6863\u6807\u9898',
  format: '\u683c\u5f0f',
  removeDoc: '\u79fb\u9664',
  docSearchPh: '\u641c\u7d22\u6587\u6863\u6807\u9898',
  uploader: '\u4e0a\u4f20\u4eba'
}

export default {
  name: 'LibraryTopicAdmin',
  dicts: ['sys_normal_disable'],
  data() {
    return {
      L,
      loading: false,
      showSearch: true,
      total: 0,
      list: [],
      ids: [],
      single: true,
      multiple: true,
      open: false,
      title: '',
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        status: undefined
      },
      form: {},
      rules: {
        title: [{ required: true, message: L.titlePh, trigger: 'blur' }],
        documentIds: [{
          validator: (rule, value, callback) => {
            if (!value || !value.length) callback(new Error('\u8bf7\u81f3\u5c11\u9009\u62e9\u4e00\u4e2a\u6587\u6863'))
            else callback()
          },
          trigger: 'change'
        }]
      },
      selectedDocs: [],
      docPickerOpen: false,
      docLoading: false,
      docPickerList: [],
      docPickerSelection: [],
      docTotal: 0,
      docQuery: { pageNum: 1, pageSize: 10, keyword: undefined }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    formatPrice(val) {
      const n = Number(val)
      if (!Number.isFinite(n) || n <= 0) return '\u514d\u8d39'
      return '\u00a5' + n.toFixed(2)
    },
    getList() {
      this.loading = true
      listAdminLibraryTopics(this.queryParams).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.topicId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    resetFormData() {
      this.form = {
        topicId: undefined,
        title: '',
        summary: '',
        coverUrl: '',
        bundlePrice: 0,
        orderNum: 0,
        status: '0',
        documentIds: []
      }
      this.selectedDocs = []
    },
    handleAdd() {
      this.resetFormData()
      this.open = true
      this.title = L.add
    },
    handleUpdate(row) {
      const topicId = row.topicId || this.ids[0]
      getAdminLibraryTopic(topicId).then(res => {
        const data = res.data || {}
        this.form = {
          topicId: data.topicId,
          title: data.title,
          summary: data.summary,
          coverUrl: data.coverUrl,
          bundlePrice: Number(data.bundlePrice) || 0,
          orderNum: data.orderNum || 0,
          status: data.status || '0',
          documentIds: data.documentIds || []
        }
        this.selectedDocs = (data.documents || []).map(d => ({ ...d }))
        this.open = true
        this.title = L.edit
      })
    },
    cancel() {
      this.open = false
      this.resetFormData()
    },
    submitForm() {
      this.form.documentIds = this.selectedDocs.map(d => d.documentId)
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.topicId ? updateLibraryTopic(this.form) : addLibraryTopic(this.form)
        req.then(() => {
          this.$modal.msgSuccess(this.form.topicId ? '\u4fee\u6539\u6210\u529f' : '\u65b0\u589e\u6210\u529f')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const topicIds = row.topicId ? [row.topicId] : this.ids
      this.$modal.confirm('\u786e\u8ba4\u5220\u9664\u9009\u4e2d\u4e13\u9898\uff1f').then(() => {
        return delLibraryTopic(topicIds.join(','))
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('\u5220\u9664\u6210\u529f')
      }).catch(() => {})
    },
    goBack() {
      this.$router.push({ path: '/admin/question-bank-center/library' })
    },
    openDocPicker() {
      this.docPickerOpen = true
      this.docPickerSelection = []
      this.searchDocs()
    },
    searchDocs() {
      this.docLoading = true
      listLibraryDocuments({
        pageNum: this.docQuery.pageNum,
        pageSize: this.docQuery.pageSize,
        keyword: this.docQuery.keyword,
        auditStatus: '1',
        status: '0'
      }).then(res => {
        this.docPickerList = res.rows || []
        this.docTotal = res.total || 0
      }).finally(() => {
        this.docLoading = false
      })
    },
    onDocPickerSelect(rows) {
      this.docPickerSelection = rows
    },
    confirmDocPicker() {
      const map = {}
      this.selectedDocs.forEach(d => { map[d.documentId] = d })
      this.docPickerSelection.forEach(d => { map[d.documentId] = d })
      this.selectedDocs = Object.values(map)
      this.form.documentIds = this.selectedDocs.map(d => d.documentId)
      this.docPickerOpen = false
      this.$refs.form.validateField('documentIds')
    },
    removeDoc(documentId) {
      this.selectedDocs = this.selectedDocs.filter(d => d.documentId !== documentId)
      this.form.documentIds = this.selectedDocs.map(d => d.documentId)
      this.$refs.form.validateField('documentIds')
    }
  }
}
</script>

<style scoped>
.topic-doc-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.topic-doc-hint {
  color: #909399;
  font-size: 12px;
}
.topic-doc-table {
  width: 100%;
}
</style>
