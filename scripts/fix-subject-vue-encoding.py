#!/usr/bin/env python3
"""Rewrite subject/index.vue with correct UTF-8 Chinese text."""

from pathlib import Path

CONTENT = """<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="\u5b66\u79d1\u540d\u79f0" prop="subjectName">
        <el-input v-model="queryParams.subjectName" placeholder="\u8bf7\u8f93\u5165\u5b66\u79d1\u540d\u79f0" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="\u72b6\u6001" prop="status">
        <el-select v-model="queryParams.status" placeholder="\u5b66\u79d1\u72b6\u6001" clearable>
          <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">\u641c\u7d22</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">\u91cd\u7f6e</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['education:subject:add']">\u65b0\u589e</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['education:subject:edit']">\u4fee\u6539</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['education:subject:remove']">\u5220\u9664</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="\u5b66\u79d1\u7f16\u53f7" align="center" prop="subjectId" width="90" />
      <el-table-column label="\u5b66\u79d1\u540d\u79f0" align="center" prop="subjectName" />
      <el-table-column label="\u6ee1\u5206" align="center" prop="fullScore" width="100" />
      <el-table-column label="\u6392\u5e8f" align="center" prop="orderNum" width="80" />
      <el-table-column label="\u72b6\u6001" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="\u64cd\u4f5c" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['education:subject:edit']">\u4fee\u6539</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['education:subject:remove']">\u5220\u9664</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="\u5b66\u79d1\u540d\u79f0" prop="subjectName">
          <el-input v-model="form.subjectName" placeholder="\u8bf7\u8f93\u5165\u5b66\u79d1\u540d\u79f0" />
        </el-form-item>
        <el-form-item label="\u6ee1\u5206" prop="fullScore">
          <el-input-number v-model="form.fullScore" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="\u6392\u5e8f" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="\u72b6\u6001" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="\u5907\u6ce8" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="\u8bf7\u8f93\u5165\u5907\u6ce8" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">\u786e \u5b9a</el-button>
        <el-button @click="cancel">\u53d6 \u6d88</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSubject, getSubject, addSubject, updateSubject, delSubject } from '@/api/education/subject'

export default {
  name: 'EduSubject',
  dicts: ['sys_normal_disable'],
  data() {
    return {
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
        subjectName: undefined,
        status: undefined
      },
      form: {},
      rules: {
        subjectName: [{ required: true, message: '\u5b66\u79d1\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listSubject(this.queryParams).then(res => {
        this.list = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        subjectId: undefined,
        subjectName: undefined,
        fullScore: 100,
        orderNum: 0,
        status: '0',
        remark: undefined
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
      this.ids = selection.map(item => item.subjectId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '\u65b0\u589e\u5b66\u79d1'
    },
    handleUpdate(row) {
      this.reset()
      const subjectId = row.subjectId || this.ids[0]
      getSubject(subjectId).then(res => {
        this.form = res.data
        this.open = true
        this.title = '\u4fee\u6539\u5b66\u79d1'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.subjectId ? updateSubject(this.form) : addSubject(this.form)
        req.then(() => {
          this.$modal.msgSuccess(this.form.subjectId ? '\u4fee\u6539\u6210\u529f' : '\u65b0\u589e\u6210\u529f')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const subjectIds = row.subjectId || this.ids.join(',')
      this.$modal.confirm('\u662f\u5426\u786e\u8ba4\u5220\u9664\u9009\u4e2d\u5b66\u79d1\uff1f').then(() => delSubject(subjectIds)).then(() => {
        this.getList()
        this.$modal.msgSuccess('\u5220\u9664\u6210\u529f')
      }).catch(() => {})
    }
  }
}
</script>
"""

path = Path(__file__).resolve().parents[1] / "ruoyi-ui" / "src" / "views" / "education" / "subject" / "index.vue"
path.write_text(CONTENT.encode("utf-8").decode("utf-8"), encoding="utf-8", newline="\n")
text = path.read_text(encoding="utf-8")
assert "\u5b66\u79d1\u540d\u79f0" in text, "encoding check failed"
print(f"fixed: {path}")
