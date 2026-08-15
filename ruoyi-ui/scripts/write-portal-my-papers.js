/**
 * Generate Portal MyPapers.vue. Run: node scripts/write-portal-my-papers.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/portal/MyPapers.vue')

const L = {
  home: '\u9996\u9875',
  breadcrumb: '\u6211\u7684\u8bd5\u5377',
  title: '\u6211\u7684\u8bd5\u5377',
  subtitle: '\u7ba1\u7406\u5df2\u4fdd\u5b58\u7684\u8bd5\u5377\uff0c\u53ef\u7ee7\u7eed\u7f16\u8f91\u6216\u5220\u9664',
  loginRequired: '\u767b\u5f55\u540e\u53ef\u67e5\u770b\u4e0e\u7ba1\u7406\u60a8\u7684\u8bd5\u5377',
  goLogin: '\u53bb\u767b\u5f55',
  searchPh: '\u641c\u7d22\u8bd5\u5377\u6807\u9898',
  search: '\u641c\u7d22',
  refresh: '\u5237\u65b0',
  empty: '\u6682\u65e0\u4fdd\u5b58\u7684\u8bd5\u5377',
  goPick: '\u53bb\u9009\u9898\u7ec4\u5377',
  colTitle: '\u8bd5\u5377\u6807\u9898',
  colCount: '\u9898\u91cf',
  colScore: '\u603b\u5206',
  colTime: '\u4fdd\u5b58\u65f6\u95f4',
  colAction: '\u64cd\u4f5c',
  open: '\u6253\u5f00\u7f16\u8f91',
  delete: '\u5220\u9664',
  deleteConfirm: '\u786e\u8ba4\u5220\u9664\u8be5\u8bd5\u5377\uff1f\u5220\u9664\u540e\u4e0d\u53ef\u6062\u590d',
  deleteOk: '\u5220\u9664\u6210\u529f',
  loadOk: '\u5df2\u52a0\u8f7d\u8bd5\u5377',
  loadFail: '\u52a0\u8f7d\u5931\u8d25',
  tip: '\u63d0\u793a',
  confirm: '\u786e\u5b9a',
  cancel: '\u53d6\u6d88'
}

const content = `<template>
  <div class="portal-my-papers portal-page">
    <div class="portal-container papers-wrap">
      <div class="breadcrumb">
        <router-link to="/portal/home">{{ labels.home }}</router-link>
        <span class="sep">&gt;</span>
        <span>{{ labels.breadcrumb }}</span>
      </div>

      <div v-if="!token" class="login-hint portal-card">
        <i class="el-icon-info" />
        <span>{{ labels.loginRequired }}</span>
        <el-button type="primary" size="small" @click="goLogin">{{ labels.goLogin }}</el-button>
      </div>

      <template v-else>
        <header class="papers-hero portal-card">
          <div class="hero-text">
            <h1>{{ labels.title }}</h1>
            <p>{{ labels.subtitle }}</p>
          </div>
          <div class="hero-toolbar">
            <el-input
              v-model="query.paperTitle"
              size="small"
              clearable
              :placeholder="labels.searchPh"
              class="search-input"
              @keyup.enter.native="loadList"
            />
            <el-button size="small" type="primary" icon="el-icon-search" @click="loadList">{{ labels.search }}</el-button>
            <el-button size="small" icon="el-icon-refresh" @click="resetQuery">{{ labels.refresh }}</el-button>
          </div>
        </header>

        <div v-loading="loading" class="papers-table-wrap portal-card">
          <el-table v-if="list.length" :data="list" stripe size="small">
            <el-table-column prop="paperTitle" :label="labels.colTitle" min-width="220" show-overflow-tooltip>
              <template slot-scope="scope">
                <a href="javascript:;" class="paper-title-link" @click.prevent="openPaper(scope.row)">{{ scope.row.paperTitle }}</a>
              </template>
            </el-table-column>
            <el-table-column prop="itemCount" :label="labels.colCount" width="80" align="center" />
            <el-table-column prop="totalScore" :label="labels.colScore" width="80" align="center" />
            <el-table-column prop="createTime" :label="labels.colTime" width="168" align="center" />
            <el-table-column :label="labels.colAction" width="160" align="center" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="openPaper(scope.row)">{{ labels.open }}</el-button>
                <el-button type="text" size="mini" class="danger-text" @click="removePaper(scope.row)">{{ labels.delete }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-block">
            <i class="el-icon-document" />
            <p>{{ labels.empty }}</p>
            <el-button type="primary" size="small" round @click="$router.push('/portal/chapter')">{{ labels.goPick }}</el-button>
          </div>
          <pagination
            v-show="total > 0"
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
import { listMyPapers, getMyPaper, deleteMyPaper } from '@/api/education/paper'
import { goPortalLogin } from '@/utils/portalLogin'
import { savePaperDraft } from '@/utils/questionBasketPrefs'

const labels = {
  home: '${L.home}',
  breadcrumb: '${L.breadcrumb}',
  title: '${L.title}',
  subtitle: '${L.subtitle}',
  loginRequired: '${L.loginRequired}',
  goLogin: '${L.goLogin}',
  searchPh: '${L.searchPh}',
  search: '${L.search}',
  refresh: '${L.refresh}',
  empty: '${L.empty}',
  goPick: '${L.goPick}',
  colTitle: '${L.colTitle}',
  colCount: '${L.colCount}',
  colScore: '${L.colScore}',
  colTime: '${L.colTime}',
  colAction: '${L.colAction}',
  open: '${L.open}',
  delete: '${L.delete}',
  deleteConfirm: '${L.deleteConfirm}',
  deleteOk: '${L.deleteOk}',
  loadOk: '${L.loadOk}',
  loadFail: '${L.loadFail}',
  tip: '${L.tip}',
  confirm: '${L.confirm}',
  cancel: '${L.cancel}'
}

export default {
  name: 'PortalMyPapers',
  data() {
    return {
      labels,
      loading: false,
      list: [],
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 10,
        paperTitle: undefined
      }
    }
  },
  computed: {
    ...mapGetters(['token'])
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
      this.query.paperTitle = undefined
      this.query.pageNum = 1
      this.loadList()
    },
    loadList() {
      this.loading = true
      listMyPapers(this.query).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    openPaper(row) {
      if (!row || !row.paperId) return
      this.loading = true
      getMyPaper(row.paperId).then(res => {
        const data = res.data || {}
        const items = Array.isArray(data.basketItems) ? data.basketItems : []
        if (!items.length) {
          this.$modal.msgWarning('${L.loadFail}')
          return
        }
        this.$store.commit('questionBasket/SET_ITEMS', items)
        const draft = {
          form: {
            paperTitle: data.paperTitle || row.paperTitle,
            header: data.header || {},
            templateCode: data.templateCode || 'A4_1COL',
            sortMode: data.sortMode || 'TYPE_THEN_DIFFICULTY',
            exportMode: data.exportMode || 'student',
            answerLayout: data.answerLayout || 'inline',
            exportConfig: data.exportConfig || {}
          },
          groupTab: data.groupTab || 'type',
          orderRadio: data.orderRadio || 'difficulty',
          paperTemplate: data.paperTemplate || 'homework',
          exportFormat: data.exportFormat || 'pdf',
          pageLayout: data.pageLayout || 'A4',
          answerAreas: data.answerAreas || {},
          paperId: data.paperId,
          items
        }
        savePaperDraft(draft)
        this.$modal.msgSuccess('${L.loadOk}')
        this.$router.push({ path: '/portal/paper/preview', query: { draft: '1', paperId: data.paperId } })
      }).catch(() => {
        this.$modal.msgError('${L.loadFail}')
      }).finally(() => { this.loading = false })
    },
    removePaper(row) {
      if (!row || !row.paperId) return
      this.$confirm('${L.deleteConfirm}', '${L.tip}', {
        confirmButtonText: '${L.confirm}',
        cancelButtonText: '${L.cancel}',
        type: 'warning'
      }).then(() => deleteMyPaper(row.paperId)).then(() => {
        this.$modal.msgSuccess('${L.deleteOk}')
        this.loadList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
.papers-wrap { padding-bottom: 32px; }

.login-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 24px;
  color: #64748b;
  i { font-size: 20px; color: #2563EB; }
}

.papers-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.hero-text {
  h1 { margin: 0 0 6px; font-size: 22px; color: #1e293b; }
  p { margin: 0; font-size: 14px; color: #64748b; }
}

.hero-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.search-input { width: 220px; }

.papers-table-wrap {
  padding: 16px 20px 8px;
  min-height: 280px;
}

.empty-block {
  text-align: center;
  padding: 48px 16px;
  color: #94a3b8;
  i { font-size: 48px; margin-bottom: 12px; display: block; }
  p { margin: 0 0 16px; }
}

.danger-text { color: #ef4444 !important; }

.paper-title-link {
  color: #2563EB;
  text-decoration: none;
  cursor: pointer;
  &:hover { text-decoration: underline; color: #1D4ED8; }
}
</style>
`

fs.writeFileSync(file, content, 'utf8')
console.log('wrote MyPapers.vue')
