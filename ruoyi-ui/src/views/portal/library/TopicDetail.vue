<template>
  <div class="portal-library-topic portal-page">
    <div class="portal-container" v-loading="loading">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">{{ L.home }}</router-link>
        <span class="sep">/</span>
        <router-link to="/library">{{ L.breadcrumb }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ (topic && topic.title) || L.detail }}</span>
      </nav>

      <div v-if="!loading && !topic" class="topic-missing portal-card">
        <i class="el-icon-warning-outline" />
        <p>{{ L.notFound }}</p>
        <router-link to="/library/topics" class="topic-missing-link">{{ L.backTopics }}</router-link>
      </div>

      <div v-if="topic" class="topic-layout">
        <header class="topic-header portal-card">
          <div class="topic-cover" v-if="topic.coverUrl">
            <img :src="resolveCover(topic.coverUrl)" :alt="topic.title" loading="lazy" decoding="async">
          </div>
          <div class="topic-main">
            <h1 class="topic-title">{{ topic.title }}</h1>
            <p v-if="topic.summary" class="topic-summary">{{ topic.summary }}</p>
            <div class="topic-meta">
              <span><i class="el-icon-document" />{{ docList.length }} {{ L.docUnit }}</span>
              <span><i class="el-icon-download" />{{ topic.downloadCount || 0 }} {{ L.downloadUnit }}</span>
            </div>
            <div class="topic-download-row">
              <span class="topic-price">{{ priceDisplay }}</span>
              <button
                type="button"
                class="btn-download-bundle"
                :class="{ 'is-paid': payAccess.needPay && !payAccess.purchased }"
                :disabled="downloading || !docList.length"
                @click="downloadBundle"
              >
                <i :class="downloading ? 'el-icon-loading' : 'el-icon-folder-opened'" />
                {{ downloadLabel }}
              </button>
            </div>
          </div>
        </header>

        <section class="topic-docs portal-card">
          <h2 class="section-title">{{ L.docList }}</h2>
          <ul class="topic-doc-list">
            <li
              v-for="(doc, idx) in docList"
              :key="doc.documentId"
              class="topic-doc-item"
              @click="openDoc(doc)"
            >
              <span class="doc-index">{{ idx + 1 }}</span>
              <file-ext-badge :ext="doc.fileExt" size="sm" />
              <div class="doc-info">
                <p class="doc-title" :title="doc.title">{{ doc.title }}</p>
                <p class="doc-meta">
                  <span v-if="doc.categoryName">{{ doc.categoryName }}</span>
                  <span>{{ doc.viewCount || 0 }} {{ L.views }}</span>
                </p>
              </div>
              <i class="el-icon-arrow-right doc-arrow" />
            </li>
          </ul>
          <p v-if="!docList.length" class="topic-empty">{{ L.empty }}</p>
        </section>
      </div>
    </div>
    <pay-dialog ref="payDialog" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import PayDialog from '@/components/PayDialog'
import FileExtBadge from './components/FileExtBadge'
import { getPayOrderStatus } from '@/api/education/pay'
import { getLibraryTopic } from '@/api/education/library'
import { resolvePortalMediaUrl } from '@/utils/portalBanner'
import { goPortalLogin } from '@/utils/portalLogin'
import {
  buildDownloadButtonLabel,
  loadTopicPayStatus,
  downloadTopicWithPay
} from '@/utils/libraryTopicPayDownload'
import {
  isPayReturnQuery,
  resolvePayReturnOrderNo
} from '@/utils/libraryPayDownload'

const L = {
  home: '\u9996\u9875',
  breadcrumb: '\u6587\u5e93',
  detail: '\u4e13\u9898\u8be6\u60c5',
  docUnit: '\u4efd\u6587\u6863',
  downloadUnit: '\u6b21\u4e0b\u8f7d',
  docList: '\u5305\u542b\u6587\u6863',
  views: '\u9605\u8bfb',
  empty: '\u6682\u65e0\u6587\u6863',
  free: '\u514d\u8d39',
  downloadHint: '\u4e00\u952e\u6253\u5305\u4e0b\u8f7d\u5168\u90e8\u6587\u6863',
  payReturnPending: '\u786e\u8ba4\u652f\u4ed8\u7ed3\u679c\u2026',
  payReturnSuccess: '\u652f\u4ed8\u6210\u529f',
  notFound: '\u4e13\u9898\u4e0d\u5b58\u5728\u6216\u5df2\u4e0b\u67b6',
  backTopics: '\u8fd4\u56de\u4e13\u9898\u5217\u8868'
}

export default {
  name: 'PortalLibraryTopic',
  components: { PayDialog, FileExtBadge },
  data() {
    return {
      L,
      loading: false,
      downloading: false,
      topic: null,
      payAccess: {
        enabled: true,
        needPay: false,
        purchased: true,
        price: 0
      }
    }
  },
  computed: {
    ...mapGetters(['token']),
    topicId() {
      return Number(this.$route.params.topicId)
    },
    docList() {
      return (this.topic && this.topic.documents) || []
    },
    priceDisplay() {
      const price = this.payAccess.price || 0
      if (!this.payAccess.needPay || price <= 0) return L.free
      if (this.payAccess.purchased) return '\u5df2\u8d2d\u4e70'
      return '\u00a5' + price.toFixed(2)
    },
    downloadLabel() {
      return buildDownloadButtonLabel({
        price: this.payAccess.price,
        needPay: this.payAccess.needPay,
        purchased: this.payAccess.purchased,
        freeLabel: '\u6253\u5305\u4e0b\u8f7d',
        paidLabel: '\u4ed8\u8d39\u6253\u5305\u4e0b\u8f7d',
        ownedLabel: '\u5df2\u8d2d\u4e70 \u00b7 \u6253\u5305\u4e0b\u8f7d'
      })
    }
  },
  watch: {
    topicId: {
      immediate: true,
      handler() {
        this.loadTopic()
      }
    },
    '$route.query'() {
      this.handlePayReturn()
    }
  },
  created() {
    this.handlePayReturn()
  },
  methods: {
    loadTopic() {
      if (!this.topicId) return
      this.loading = true
      getLibraryTopic(this.topicId, { portal: true }).then(res => {
        this.topic = res.data || null
        return this.refreshPayStatus()
      }).catch(() => {
        this.topic = null
      }).finally(() => {
        this.loading = false
      })
    },
    refreshPayStatus() {
      if (!this.topic) return Promise.resolve()
      return loadTopicPayStatus(this.topicId, {
        bundlePrice: this.topic.bundlePrice,
        productName: this.topic.title
      }).then(status => {
        this.payAccess = status
      })
    },
    resolveCover(url) {
      return resolvePortalMediaUrl(url)
    },
    openDoc(doc) {
      if (doc && doc.documentId) {
        this.$router.push('/library/' + doc.documentId)
      }
    },
    downloadBundle() {
      if (!this.token) {
        goPortalLogin(this.$router, this.$route.fullPath)
        return
      }
      if (!this.docList.length) return
      this.downloading = true
      downloadTopicWithPay(this, {
        topicId: this.topicId,
        title: this.topic.title,
        bundlePrice: this.topic.bundlePrice,
        onStatus: status => { this.payAccess = status },
        onLogin: () => goPortalLogin(this.$router, this.$route.fullPath)
      }).then(() => {
        if (this.topic) {
          this.topic.downloadCount = (this.topic.downloadCount || 0) + 1
        }
      }).catch(err => {
        if (err && err.message !== 'login') {
          // download plugin shows errors
        }
      }).finally(() => {
        this.downloading = false
      })
    },
    handlePayReturn() {
      if (!isPayReturnQuery(this.$route.query)) return
      const orderNo = resolvePayReturnOrderNo(this.$route.query)
      if (!orderNo) return
      const loading = this.$loading({
        lock: true,
        text: L.payReturnPending,
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.35)'
      })
      const clearQuery = () => {
        const query = { ...this.$route.query }
        let changed = false
        ;['payReturn', 'payOrder', 'payBiz', 'payBizId', 'out_trade_no'].forEach(key => {
          if (query[key] !== undefined) {
            delete query[key]
            changed = true
          }
        })
        if (changed) {
          this.$router.replace({ path: this.$route.path, query }).catch(() => {})
        }
      }
      const poll = (left) => {
        return getPayOrderStatus(orderNo).then(res => {
          const order = res.data || {}
          if (order.status === 'paid') {
            this.$modal.msgSuccess(L.payReturnSuccess)
            return this.refreshPayStatus()
          }
          if (left <= 0) return Promise.resolve()
          return new Promise(resolve => setTimeout(resolve, 2000)).then(() => poll(left - 1))
        })
      }
      poll(8).then(() => this.refreshPayStatus()).then(() => {
        if (this.payAccess.purchased && this.payAccess.needPay && this.docList.length) {
          this.downloadBundle()
        }
      }).finally(() => {
        loading.close()
        clearQuery()
      })
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #0F766E;
$ink: #0F172A;
$muted: #64748B;
$border: #E2E8F0;

.portal-library-topic {
  padding-bottom: 40px;
}

.topic-layout {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.topic-header {
  display: flex;
  gap: 24px;
  padding: 24px;
}

.topic-cover {
  flex: 0 0 200px;
  height: 140px;
  border-radius: 8px;
  overflow: hidden;
  background: #F1F5F9;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.topic-main {
  flex: 1;
  min-width: 0;
}

.topic-title {
  margin: 0 0 8px;
  font-size: 24px;
  color: $ink;
}

.topic-summary {
  margin: 0 0 12px;
  color: $muted;
  line-height: 1.6;
}

.topic-meta {
  display: flex;
  gap: 16px;
  color: $muted;
  font-size: 13px;
  margin-bottom: 16px;

  i { margin-right: 4px; }
}

.topic-download-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.topic-price {
  font-size: 20px;
  font-weight: 600;
  color: $primary;
}

.btn-download-bundle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  background: $primary;
  color: #fff;
  font-size: 14px;
  cursor: pointer;

  &:hover:not(:disabled) { opacity: 0.92; }
  &:disabled { opacity: 0.6; cursor: not-allowed; }
  &.is-paid { background: #D97706; }
}

.topic-docs {
  padding: 20px 24px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 18px;
  color: $ink;
}

.topic-doc-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.topic-doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  border-bottom: 1px solid $border;
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: #F8FAFC; }
  &:last-child { border-bottom: none; }
}

.doc-index {
  width: 24px;
  text-align: center;
  color: $muted;
  font-size: 13px;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-title {
  margin: 0;
  font-size: 14px;
  color: $ink;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: $muted;
  display: flex;
  gap: 12px;
}

.doc-arrow {
  color: #CBD5E1;
}

.topic-empty {
  text-align: center;
  color: $muted;
  padding: 24px 0;
}

.topic-missing {
  text-align: center;
  padding: 48px 24px;
  color: $muted;

  i {
    font-size: 40px;
    color: #F59E0B;
    display: block;
    margin-bottom: 12px;
  }

  p { margin: 0 0 12px; }
}

.topic-missing-link {
  color: $primary;
  text-decoration: none;

  &:hover { text-decoration: underline; }
}

@media (max-width: 640px) {
  .topic-header {
    flex-direction: column;
  }
  .topic-cover {
    flex: none;
    width: 100%;
  }
}
</style>
