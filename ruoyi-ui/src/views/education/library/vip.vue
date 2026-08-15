<template>
  <div class="app-container library-vip-admin">
    <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="80px">
      <el-form-item :label="L.username" prop="keyword">
        <el-input v-model="queryParams.keyword" :placeholder="L.usernamePh" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item :label="L.status" prop="status">
        <el-select v-model="queryParams.status" :placeholder="L.status" clearable>
          <el-option :label="L.statusActive" value="0" />
          <el-option :label="L.statusExpired" value="1" />
          <el-option :label="L.statusDisabled" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">{{ L.search }}</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">{{ L.reset }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openGrant" v-hasPermi="['education:library:vip']">{{ L.grant }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="el-icon-setting" size="mini" @click="openConfig" v-hasPermi="['education:library:vip']">{{ L.config }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="el-icon-back" size="mini" @click="goBack">{{ L.back }}</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column :label="L.username" prop="username" min-width="120" />
      <el-table-column :label="L.plan" prop="planCode" width="100" />
      <el-table-column :label="L.status" prop="status" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row)" size="mini">{{ statusLabel(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="L.startTime" prop="startTime" width="160" />
      <el-table-column :label="L.expireTime" prop="expireTime" width="160" />
      <el-table-column :label="L.source" prop="source" width="90" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.source === 'pay' ? L.sourcePay : L.sourceAdmin }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="L.remark" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column :label="L.action" width="180" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-time" @click="openExtend(scope.row)" v-hasPermi="['education:library:vip']">{{ L.extend }}</el-button>
          <el-button size="mini" type="text" icon="el-icon-close" @click="handleDisable(scope.row)" v-hasPermi="['education:library:vip']">{{ L.disable }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="L.configTitle" :visible.sync="configOpen" width="520px" append-to-body>
      <el-form label-width="130px" size="small">
        <el-form-item :label="L.configEnabled">
          <el-switch v-model="configForm.enabled" />
        </el-form-item>
        <el-form-item :label="L.configPrice">
          <el-input-number v-model="configForm.price" :min="0" :max="9999" :precision="2" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item :label="L.configDuration">
          <el-input-number v-model="configForm.durationDays" :min="1" :max="3650" controls-position="right" />
        </el-form-item>
        <el-form-item :label="L.configFreeDownload">
          <el-switch v-model="configForm.freeDownload" />
        </el-form-item>
        <el-form-item :label="L.configPreviewPages">
          <el-input-number v-model="configForm.previewPages" :min="0" :max="100" controls-position="right" />
          <p class="form-hint">{{ L.configPreviewHint }}</p>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="configOpen = false">{{ L.cancel }}</el-button>
        <el-button type="primary" :loading="configSaving" @click="saveConfig">{{ L.confirm }}</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="grantTitle" :visible.sync="grantOpen" width="460px" append-to-body>
      <el-form ref="grantForm" :model="grantForm" :rules="grantRules" label-width="100px">
        <el-form-item :label="L.username" prop="username">
          <el-input v-model="grantForm.username" :placeholder="L.usernamePh" :disabled="grantMode === 'extend'" />
        </el-form-item>
        <el-form-item :label="L.durationDays" prop="durationDays">
          <el-input-number v-model="grantForm.durationDays" :min="1" :max="3650" controls-position="right" />
        </el-form-item>
        <el-form-item :label="L.remark" prop="remark">
          <el-input v-model="grantForm.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="grantOpen = false">{{ L.cancel }}</el-button>
        <el-button type="primary" :loading="grantSaving" @click="submitGrant">{{ L.confirm }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listVipMembers,
  getVipAdminConfig,
  updateVipAdminConfig,
  grantVipMember,
  extendVipMember,
  disableVipMember
} from '@/api/education/vip'

const L = {
  username: '\u7528\u6237\u540d',
  usernamePh: '\u8bf7\u8f93\u5165\u7528\u6237\u540d',
  status: '\u72b6\u6001',
  statusActive: '\u751f\u6548\u4e2d',
  statusExpired: '\u5df2\u8fc7\u671f',
  statusDisabled: '\u5df2\u505c\u7528',
  search: '\u641c\u7d22',
  reset: '\u91cd\u7f6e',
  grant: '\u6388\u4e88 VIP',
  extend: '\u7eed\u671f',
  disable: '\u505c\u7528',
  config: 'VIP \u8bbe\u7f6e',
  back: '\u8fd4\u56de\u6587\u6863',
  plan: '\u5957\u9910',
  startTime: '\u5f00\u901a\u65f6\u95f4',
  expireTime: '\u5230\u671f\u65f6\u95f4',
  source: '\u6765\u6e90',
  sourcePay: '\u652f\u4ed8\u8d2d\u4e70',
  sourceAdmin: '\u7ba1\u7406\u5458\u6388\u4e88',
  remark: '\u5907\u6ce8',
  action: '\u64cd\u4f5c',
  cancel: '\u53d6 \u6d88',
  confirm: '\u786e \u5b9a',
  configTitle: 'VIP \u4f1a\u5458\u8bbe\u7f6e',
  configEnabled: '\u5f00\u542f VIP',
  configPrice: 'VIP \u4ef7\u683c\uff08\u5143\uff09',
  configDuration: '\u6709\u6548\u5929\u6570',
  configFreeDownload: '\u514d\u8d39\u4e0b\u8f7d\u4ed8\u8d39\u6587\u6863',
  configPreviewPages: 'VIP \u9884\u89c8\u9875\u6570',
  configPreviewHint: '0 \u8868\u793a\u4f7f\u7528\u9ed8\u8ba4\u9884\u89c8\u9875\u6570\u4e0a\u9650',
  grantTitle: '\u6388\u4e88 VIP \u4f1a\u5458',
  extendTitle: 'VIP \u7eed\u671f',
  durationDays: '\u7eed\u671f\u5929\u6570',
  usernameRequired: '\u8bf7\u8f93\u5165\u7528\u6237\u540d',
  saveOk: '\u4fdd\u5b58\u6210\u529f',
  grantOk: '\u6388\u4e88\u6210\u529f',
  extendOk: '\u7eed\u671f\u6210\u529f',
  disableOk: '\u5df2\u505c\u7528',
  disableConfirm: '\u786e\u8ba4\u505c\u7528\u8be5\u7528\u6237\u7684 VIP \u4f1a\u5458\u5417\uff1f'
}

export default {
  name: 'EduLibraryVip',
  data() {
    return {
      L,
      loading: false,
      showSearch: true,
      total: 0,
      list: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined,
        status: undefined
      },
      configOpen: false,
      configSaving: false,
      configForm: {
        enabled: false,
        price: 29,
        durationDays: 30,
        freeDownload: true,
        previewPages: 0
      },
      grantOpen: false,
      grantSaving: false,
      grantMode: 'grant',
      grantForm: {
        username: '',
        durationDays: 30,
        remark: ''
      },
      grantRules: {
        username: [{ required: true, message: L.usernameRequired, trigger: 'blur' }]
      }
    }
  },
  computed: {
    grantTitle() {
      return this.grantMode === 'extend' ? L.extendTitle : L.grantTitle
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listVipMembers(this.queryParams).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    goBack() {
      this.$router.push({ path: '/admin/question-bank-center/library' })
    },
    openConfig() {
      getVipAdminConfig().then(res => {
        const data = res.data || {}
        this.configForm = {
          enabled: !!data.enabled,
          price: Number(data.price) || 0,
          durationDays: Number(data.durationDays) || 30,
          freeDownload: data.freeDownload !== false,
          previewPages: Number(data.previewPages) || 0
        }
        this.configOpen = true
      })
    },
    saveConfig() {
      this.configSaving = true
      updateVipAdminConfig(this.configForm).then(() => {
        this.$modal.msgSuccess(L.saveOk)
        this.configOpen = false
      }).finally(() => { this.configSaving = false })
    },
    openGrant() {
      this.grantMode = 'grant'
      this.grantForm = { username: '', durationDays: this.configForm.durationDays || 30, remark: '' }
      this.grantOpen = true
    },
    openExtend(row) {
      this.grantMode = 'extend'
      this.grantForm = { username: row.username, durationDays: 30, remark: '' }
      this.grantOpen = true
    },
    submitGrant() {
      this.$refs.grantForm.validate(valid => {
        if (!valid) return
        this.grantSaving = true
        const req = this.grantMode === 'extend' ? extendVipMember : grantVipMember
        req(this.grantForm).then(() => {
          this.$modal.msgSuccess(this.grantMode === 'extend' ? L.extendOk : L.grantOk)
          this.grantOpen = false
          this.getList()
        }).finally(() => { this.grantSaving = false })
      })
    },
    handleDisable(row) {
      this.$modal.confirm(L.disableConfirm).then(() => {
        return disableVipMember(row.username)
      }).then(() => {
        this.$modal.msgSuccess(L.disableOk)
        this.getList()
      }).catch(() => {})
    },
    statusLabel(row) {
      if (row.status === '2') return L.statusDisabled
      if (row.status === '1') return L.statusExpired
      const expire = row.expireTime ? new Date(row.expireTime.replace(/-/g, '/')) : null
      if (expire && expire.getTime() < Date.now()) return L.statusExpired
      return L.statusActive
    },
    statusTagType(row) {
      const label = this.statusLabel(row)
      if (label === L.statusActive) return 'success'
      if (label === L.statusDisabled) return 'info'
      return 'warning'
    }
  }
}
</script>

<style scoped>
.form-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
}
</style>
