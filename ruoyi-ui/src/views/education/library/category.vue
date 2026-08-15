<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="80px">
      <el-form-item :label="L.name" prop="categoryName">
        <el-input v-model="queryParams.categoryName" :placeholder="L.namePh" clearable @keyup.enter.native="handleQuery" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['education:library:category']">{{ L.add }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['education:library:category']">{{ L.edit }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['education:library:category']">{{ L.remove }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="el-icon-back" size="mini" @click="goBack">{{ L.back }}</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column :label="L.id" align="center" prop="categoryId" width="80" />
      <el-table-column :label="L.name" align="center" prop="categoryName" min-width="140" />
      <el-table-column :label="L.docCount" align="center" prop="docCount" width="90" />
      <el-table-column :label="L.order" align="center" prop="orderNum" width="80" />
      <el-table-column :label="L.status" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column :label="L.action" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['education:library:category']">{{ L.edit }}</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['education:library:category']">{{ L.remove }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="480px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item :label="L.name" prop="categoryName">
          <el-input v-model="form.categoryName" :placeholder="L.namePh" maxlength="100" />
        </el-form-item>
        <el-form-item :label="L.order" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item :label="L.status" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">{{ L.confirm }}</el-button>
        <el-button @click="cancel">{{ L.cancel }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listAdminLibraryCategories,
  getLibraryCategory,
  addLibraryCategory,
  updateLibraryCategory,
  delLibraryCategory
} from '@/api/education/library'

const L = {
  name: '\u5206\u7c7b\u540d\u79f0',
  namePh: '\u8bf7\u8f93\u5165\u5206\u7c7b\u540d\u79f0',
  status: '\u72b6\u6001',
  search: '\u641c\u7d22',
  reset: '\u91cd\u7f6e',
  add: '\u65b0\u589e',
  edit: '\u4fee\u6539',
  remove: '\u5220\u9664',
  back: '\u8fd4\u56de\u6587\u6863',
  id: '\u7f16\u53f7',
  docCount: '\u6587\u6863\u6570',
  order: '\u6392\u5e8f',
  action: '\u64cd\u4f5c',
  confirm: '\u786e \u5b9a',
  cancel: '\u53d6 \u6d88',
  addTitle: '\u65b0\u589e\u5206\u7c7b',
  editTitle: '\u4fee\u6539\u5206\u7c7b',
  nameRequired: '\u5206\u7c7b\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a',
  saveOk: '\u4fdd\u5b58\u6210\u529f',
  addOk: '\u65b0\u589e\u6210\u529f',
  delOk: '\u5220\u9664\u6210\u529f',
  delConfirm: '\u662f\u5426\u786e\u8ba4\u5220\u9664\u9009\u4e2d\u5206\u7c7b\uff1f\u5df2\u6709\u6587\u6863\u7684\u5206\u7c7b\u65e0\u6cd5\u5220\u9664\u3002'
}

export default {
  name: 'EduLibraryCategory',
  dicts: ['sys_normal_disable'],
  data() {
    return {
      L,
      loading: false,
      showSearch: true,
      ids: [],
      single: true,
      multiple: true,
      total: 0,
      list: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        categoryName: undefined,
        status: undefined
      },
      form: {},
      rules: {
        categoryName: [{ required: true, message: L.nameRequired, trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    goBack() {
      this.$router.push({ path: '/admin/question-bank-center/library' })
    },
    getList() {
      this.loading = true
      listAdminLibraryCategories(this.queryParams).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        categoryId: undefined,
        categoryName: undefined,
        orderNum: 0,
        status: '0'
      }
      this.resetForm('form')
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
      this.ids = selection.map(item => item.categoryId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = L.addTitle
    },
    handleUpdate(row) {
      this.reset()
      const categoryId = row.categoryId || this.ids[0]
      getLibraryCategory(categoryId).then(res => {
        this.form = res.data || {}
        this.open = true
        this.title = L.editTitle
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.categoryId ? updateLibraryCategory(this.form) : addLibraryCategory(this.form)
        req.then(() => {
          this.$modal.msgSuccess(this.form.categoryId ? L.saveOk : L.addOk)
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const categoryIds = row.categoryId || this.ids.join(',')
      this.$modal.confirm(L.delConfirm).then(() => delLibraryCategory(categoryIds)).then(() => {
        this.getList()
        this.$modal.msgSuccess(L.delOk)
      }).catch(() => {})
    }
  }
}
</script>
