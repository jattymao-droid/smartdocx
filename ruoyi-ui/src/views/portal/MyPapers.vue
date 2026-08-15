<template>
  <div class="portal-my-papers portal-page">
    <div class="portal-container papers-wrap">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">{{ labels.home }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ labels.breadcrumb }}</span>
      </nav>

      <div v-if="!token" class="login-hint portal-card portal-login-hint">
        <i class="el-icon-info" />
        <span>{{ labels.loginRequired }}</span>
        <el-button type="primary" size="small" @click="goLogin">{{ labels.goLogin }}</el-button>
      </div>

      <template v-else>
        <header class="papers-hero portal-card portal-page-hero portal-page-hero--amber">
          <div class="hero-text">
            <div class="hero-title-row">
              <span class="hero-icon portal-page-hero__icon portal-page-hero__icon--amber"><i class="el-icon-folder-opened" /></span>
              <div>
                <h1>{{ labels.title }}</h1>
                <p>{{ labels.subtitle }}</p>
              </div>
            </div>
          </div>
          <div class="hero-toolbar">
            <el-input
              v-model="query.paperTitle"
              size="small"
              clearable
              prefix-icon="el-icon-search"
              :placeholder="labels.searchPh"
              class="search-input"
              @keyup.enter.native="loadList"
              @clear="loadList"
            />
            <el-button size="small" type="primary" icon="el-icon-search" @click="loadList">{{ labels.search }}</el-button>
            <el-button size="small" plain icon="el-icon-refresh" @click="resetQuery">{{ labels.refresh }}</el-button>
          </div>
        </header>

        <div v-loading="loading" class="papers-table-wrap portal-card portal-card-accent">
          <div v-if="list.length" class="table-head">
            <span class="table-count" v-html="tableCountText" />
            <span class="table-hint">{{ labels.tableHint }}</span>
          </div>

          <el-table
            v-if="list.length"
            :data="list"
            class="portal-table"
            size="small"
          >
            <el-table-column prop="paperTitle" :label="labels.colTitle" min-width="260" show-overflow-tooltip>
              <template slot-scope="scope">
                <button type="button" class="paper-title-link" @click="openPaper(scope.row)">
                  <i class="el-icon-document" />
                  <span>{{ scope.row.paperTitle }}</span>
                </button>
              </template>
            </el-table-column>
            <el-table-column prop="itemCount" :label="labels.colCount" width="100" align="center">
              <template slot-scope="scope">
                <span class="meta-pill">{{ scope.row.itemCount || 0 }} {{ labels.countUnit }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="totalScore" :label="labels.colScore" width="100" align="center">
              <template slot-scope="scope">
                <span class="meta-pill meta-pill--score">{{ scope.row.totalScore || 0 }} {{ labels.scoreUnit }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" :label="labels.colTime" width="180" align="center">
              <template slot-scope="scope">
                <span class="time-text">{{ scope.row.createTime || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="labels.colAction" width="180" align="center" fixed="right">
              <template slot-scope="scope">
                <div class="row-actions">
                  <el-button size="mini" plain type="primary" @click="openPaper(scope.row)">{{ labels.open }}</el-button>
                  <el-button size="mini" plain type="danger" @click="removePaper(scope.row)">{{ labels.delete }}</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div v-else class="portal-empty">
            <div class="portal-empty-icon"><i class="el-icon-document" /></div>
            <h3>{{ labels.empty }}</h3>
            <p>{{ labels.emptyHint }}</p>
            <el-button type="primary" size="small" round icon="el-icon-search" @click="$router.push('/chapter')">{{ labels.goPick }}</el-button>
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
import { listMyPapers, getMyPaper, deleteMyPaper } from '@/api/education/paper'
import { goPortalLogin } from '@/utils/portalLogin'
import { savePaperDraft } from '@/utils/questionBasketPrefs'
import { myPaperLabels } from './portal-mypapers-labels'

export default {
  name: 'PortalMyPapers',
  data() {
    return {
      labels: myPaperLabels,
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
          this.$modal.msgWarning(this.labels.loadFail)
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
        this.$modal.msgSuccess(this.labels.loadOk)
        this.$router.push({ path: '/paper/preview', query: { draft: '1', paperId: data.paperId } })
      }).catch(() => {
        this.$modal.msgError(this.labels.loadFail)
      }).finally(() => { this.loading = false })
    },
    removePaper(row) {
      if (!row || !row.paperId) return
      this.$confirm(this.labels.deleteConfirm, this.labels.tip, {
        confirmButtonText: this.labels.confirm,
        cancelButtonText: this.labels.cancel,
        type: 'warning'
      }).then(() => deleteMyPaper(row.paperId)).then(() => {
        this.$modal.msgSuccess(this.labels.deleteOk)
        this.loadList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #2563EB;
$violet: #7C3AED;
$amber: #D97706;
$emerald: #059669;
$ink: #0F172A;
$muted: #64748B;
$border: #E2E8F0;

.papers-wrap { padding-bottom: 48px; }

.login-hint {
  i { font-size: 20px; color: $primary; }
  .el-button { margin-left: auto; }
}

.papers-hero {
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
    letter-spacing: -0.01em;
  }
  p { margin: 0; font-size: 13px; color: $muted; }
}

.hero-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.search-input {
  width: 260px;

  ::v-deep .el-input__inner {
    border-radius: 10px;
    border-color: $border;
    &:focus { border-color: $primary; }
  }
}

.papers-table-wrap {
  padding: 0;
  min-height: 320px;
  overflow: hidden;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  background: linear-gradient(90deg, #FFFBEB 0%, #F8FAFF 50%, #FAF5FF 100%);
  border-bottom: 1px solid #EEF2F6;
}

.table-count {
  font-size: 13px;
  color: $muted;

  ::v-deep b {
    font-size: 17px;
    font-weight: 800;
    margin: 0 3px;
    background: linear-gradient(135deg, $primary, $violet);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.table-hint {
  font-size: 12px;
  color: #94A3B8;
}

.paper-title-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  color: $ink;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
  transition: color 0.15s;

  i {
    color: $primary;
    font-size: 15px;
    flex-shrink: 0;
  }

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &:hover { color: $primary; }
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  background: linear-gradient(135deg, #F1F5F9, #E2E8F0);
  border: 1px solid #E2E8F0;

  &--score {
    color: $violet;
    background: linear-gradient(135deg, #FAF5FF, #EDE9FE);
    border-color: rgba(124, 58, 237, 0.2);
  }
}

.time-text {
  font-size: 12px;
  color: #64748B;
  font-variant-numeric: tabular-nums;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

@media (max-width: 768px) {
  .papers-hero { align-items: flex-start; }
  .hero-toolbar { width: 100%; }
  .search-input { flex: 1; min-width: 0; width: auto; }
  .table-head { flex-direction: column; align-items: flex-start; }
}
</style>
