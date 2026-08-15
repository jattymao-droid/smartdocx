<template>
  <div class="portal-my-purchases portal-page">
    <div class="portal-container purchases-wrap">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">{{ labels.home }}</router-link>
        <span class="sep">/</span>
        <router-link to="/profile">{{ labels.profile }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ labels.breadcrumb }}</span>
      </nav>

      <div v-if="!token" class="login-hint portal-card portal-login-hint">
        <i class="el-icon-info" />
        <span>{{ labels.loginRequired }}</span>
        <el-button type="primary" size="small" @click="goLogin">{{ labels.goLogin }}</el-button>
      </div>

      <template v-else>
        <header class="purchases-hero portal-card portal-page-hero portal-page-hero--emerald">
          <div class="hero-text">
            <div class="hero-title-row">
              <span class="hero-icon portal-page-hero__icon portal-page-hero__icon--emerald"><i class="el-icon-shopping-bag-1" /></span>
              <div>
                <h1>{{ labels.title }}</h1>
                <p>{{ labels.subtitle }}</p>
              </div>
            </div>
          </div>
          <div class="hero-toolbar">
            <el-input
              v-model="query.keyword"
              size="small"
              clearable
              prefix-icon="el-icon-search"
              :placeholder="labels.searchPh"
              class="search-input"
              @keyup.enter.native="loadList"
              @clear="loadList"
            />
            <el-select v-model="query.status" size="small" clearable :placeholder="labels.filterAll" class="filter-select" @change="loadList">
              <el-option :label="labels.filterPaid" value="paid" />
              <el-option :label="labels.filterPending" value="pending" />
            </el-select>
            <el-select v-model="query.bizType" size="small" clearable :placeholder="labels.filterAllType" class="filter-select" @change="loadList">
              <el-option :label="labels.typeLibrary" :value="PAY_BIZ.LIBRARY_DOCUMENT" />
              <el-option :label="labels.typePaper" :value="PAY_BIZ.PAPER_EXPORT" />
            </el-select>
            <el-button size="small" type="primary" icon="el-icon-search" @click="loadList">{{ labels.search }}</el-button>
            <el-button size="small" plain icon="el-icon-refresh" @click="resetQuery">{{ labels.refresh }}</el-button>
          </div>
        </header>

        <div v-loading="loading" class="purchases-table-wrap portal-card portal-card-accent">
          <div v-if="list.length" class="table-head">
            <span class="table-count" v-html="tableCountText" />
            <span class="table-hint">{{ labels.tableHint }}</span>
          </div>

          <el-table v-if="list.length" :data="list" class="portal-table" size="small">
            <el-table-column prop="productName" :label="labels.colProduct" min-width="220" show-overflow-tooltip />
            <el-table-column :label="labels.colType" width="110" align="center">
              <template slot-scope="scope">
                <span class="meta-pill">{{ bizTypeLabel(scope.row.bizType) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="labels.colAmount" width="100" align="center">
              <template slot-scope="scope">
                <span class="amount-text">&yen;{{ formatAmount(scope.row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="labels.colPayType" width="90" align="center">
              <template slot-scope="scope">
                <span>{{ payTypeLabel(scope.row.payType) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="labels.colStatus" width="90" align="center">
              <template slot-scope="scope">
                <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="labels.colPayTime" width="170" align="center">
              <template slot-scope="scope">
                <span class="time-text">{{ displayTime(scope.row) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="labels.colOrderNo" min-width="180" show-overflow-tooltip>
              <template slot-scope="scope">
                <button type="button" class="order-no-link" :title="scope.row.orderNo" @click="copyOrderNo(scope.row.orderNo)">
                  {{ scope.row.orderNo }}
                </button>
              </template>
            </el-table-column>
            <el-table-column :label="labels.colAction" width="120" align="center" fixed="right">
              <template slot-scope="scope">
                <el-button
                  v-if="scope.row.status === 'paid'"
                  size="mini"
                  plain
                  type="primary"
                  @click="usePurchase(scope.row)"
                >{{ labels.use }}</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else class="portal-empty">
            <div class="portal-empty-icon"><i class="el-icon-shopping-bag-1" /></div>
            <h3>{{ labels.empty }}</h3>
            <p>{{ labels.emptyHint }}</p>
            <el-button type="primary" size="small" round icon="el-icon-reading" @click="$router.push('/library')">{{ labels.goLibrary }}</el-button>
          </div>

          <pagination
            v-show="total > 0"
            class="portal-pager"
            :total="total"
            :page.sync="query.pageNum"
            :limit.sync="query.pageSize"
            @pagination="loadList"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listMyPayOrders, PAY_BIZ } from '@/api/education/pay'
import { goPortalLogin } from '@/utils/portalLogin'
import { myPurchaseLabels } from './portal-mypurchases-labels'

export default {
  name: 'PortalMyPurchases',
  data() {
    return {
      labels: { ...myPurchaseLabels, profile: '\u4e2a\u4eba\u4e2d\u5fc3' },
      PAY_BIZ,
      loading: false,
      list: [],
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined,
        status: undefined,
        bizType: undefined
      }
    }
  },
  computed: {
    ...mapGetters(['token']),
    tableCountText() {
      const n = this.total || 0
      return this.labels.tableCount.replace('{n}', `<b>${n}</b>`)
    }
  },
  watch: {
    token(val) {
      if (val) this.loadList()
    }
  },
  created() {
    if (this.token) this.loadList()
  },
  methods: {
    goLogin() {
      goPortalLogin(this.$router, this.$route.fullPath)
    },
    resetQuery() {
      this.query = {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined,
        status: undefined,
        bizType: undefined
      }
      this.loadList()
    },
    loadList() {
      this.loading = true
      listMyPayOrders(this.query).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    formatAmount(value) {
      const n = Number(value)
      return Number.isFinite(n) ? n.toFixed(2) : '0.00'
    },
    bizTypeLabel(type) {
      if (type === PAY_BIZ.LIBRARY_DOCUMENT) return this.labels.typeLibrary
      if (type === PAY_BIZ.PAPER_EXPORT) return this.labels.typePaper
      return type || '-'
    },
    payTypeLabel(type) {
      if (type === 'wxpay') return this.labels.payWx
      if (type === 'alipay') return this.labels.payAlipay
      return type || '-'
    },
    statusLabel(status) {
      if (status === 'paid') return this.labels.statusPaid
      if (status === 'pending') return this.labels.statusPending
      if (status === 'failed') return this.labels.statusFailed
      return status || '-'
    },
    statusTagType(status) {
      if (status === 'paid') return 'success'
      if (status === 'pending') return 'warning'
      return 'info'
    },
    displayTime(row) {
      return row.payTime || row.createTime || '-'
    },
    copyOrderNo(orderNo) {
      if (!orderNo) return
      const done = () => this.$message.success(this.labels.copyOk)
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(orderNo).then(done).catch(() => {})
      }
    },
    usePurchase(row) {
      if (!row) return
      if (row.bizType === PAY_BIZ.LIBRARY_DOCUMENT && row.bizId) {
        this.$router.push('/library/' + row.bizId)
        return
      }
      if (row.bizType === PAY_BIZ.PAPER_EXPORT) {
        if (row.bizId) {
          this.$router.push({ path: '/paper/preview', query: { paperId: row.bizId } })
        } else {
          this.$router.push('/paper')
        }
      }
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #2563EB;
$emerald: #059669;
$ink: #0F172A;
$muted: #64748B;
$border: #E2E8F0;

.purchases-wrap { padding-bottom: 48px; }

.login-hint {
  i { font-size: 20px; color: $primary; }
  .el-button { margin-left: auto; }
}

.purchases-hero {
  margin-bottom: 16px;
}

.hero-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-text {
  h1 {
    margin: 0 0 4px;
    font-size: 20px;
    font-weight: 800;
    color: $ink;
  }
  p { margin: 0; font-size: 13px; color: $muted; }
}

.hero-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.search-input { width: 220px; }
.filter-select { width: 120px; }

.purchases-table-wrap {
  padding: 0;
  overflow: hidden;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid $border;
  font-size: 13px;
  color: $muted;
}

.table-count b { color: $emerald; font-size: 15px; }

.meta-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
}

.amount-text {
  font-weight: 700;
  color: #d97706;
}

.time-text {
  font-size: 12px;
  color: #64748b;
}

.order-no-link {
  border: none;
  background: none;
  padding: 0;
  color: $primary;
  font-size: 12px;
  cursor: pointer;
  text-align: left;
  &:hover { text-decoration: underline; }
}

@media (max-width: 960px) {
  .hero-toolbar { width: 100%; }
  .search-input, .filter-select { flex: 1; min-width: 140px; }
}
</style>
