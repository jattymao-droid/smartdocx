<template>
  <div class="portal-library-detail portal-page doc-preview-page">
    <div class="portal-container" v-loading="loading">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">{{ L.home }}</router-link>
        <span class="sep">/</span>
        <router-link to="/library">{{ L.breadcrumb }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ (doc && doc.title) || L.detail }}</span>
      </nav>

      <div v-if="doc" class="doc-layout">
        <!-- 左侧主内容 -->
        <main class="doc-main">
          <header class="doc-header portal-card">
            <div class="doc-header-top">
              <h1 class="doc-title">
                <file-type-icon :ext="doc.fileExt" />
                <span class="doc-title-text">{{ doc.title }}</span>
              </h1>
              <button
                v-if="token"
                type="button"
                class="doc-fav-btn"
                :class="{ active: doc.favorited }"
                @click="toggleFavorite"
              >
                <i :class="doc.favorited ? 'el-icon-star-on' : 'el-icon-star-off'" />
              </button>
            </div>
            <div class="doc-meta-bar">
              <span class="meta-item"><i class="el-icon-view" />{{ doc.viewCount || 0 }}</span>
              <span class="meta-item"><i class="el-icon-star-off" />{{ doc.favoriteCount || 0 }}</span>
              <span class="meta-item"><i class="el-icon-download" />{{ doc.downloadCount || 0 }}</span>
              <span class="meta-item"><i class="el-icon-document" />{{ displayPageCount }}{{ L.pageUnit }}</span>
              <span class="meta-item"><i class="el-icon-files" />{{ displaySize }}</span>
              <span class="meta-item"><i class="el-icon-time" />{{ formatDateTime(doc.createTime) }}</span>
              <span v-if="token && currentReadProgress > 0" class="meta-item meta-progress">
                <i class="el-icon-reading" />{{ currentReadProgress }}%
              </span>
            </div>
          </header>

          <section class="preview-panel portal-card">
            <library-preview
              :preview-type="preview.previewType"
              :preview-url="preview.previewUrl"
              :file-url="preview.fileUrl || doc.fileUrl"
              :convert-status="preview.convertStatus"
              :initial-progress="initialReadProgress"
              :allow-download="canDownload"
              :download-label="downloadLabel"
              :watermark="preview.watermark"
              :error="preview.previewError"
              :max-preview-pages="preview.previewPageLimit || 0"
              @download="downloadFile"
              @progress="onReadProgress"
              @page-count="onPreviewPageCount"
            />
          </section>

          <section v-if="canDownload" class="inline-download portal-card">
            <p class="inline-download-hint">{{ L.downloadHint }}</p>
            <div class="inline-download-row">
              <div class="inline-download-price">
                <span class="price-label">{{ priceDisplay }}</span>
                <span class="price-sub">{{ doc.downloadCount || 0 }} {{ L.downloadedBy }}</span>
              </div>
              <button
                type="button"
                class="btn-download-now"
                :class="{ 'is-paid': payAccess.needPay && !payAccess.purchased, 'is-owned': payAccess.purchased && payAccess.needPay }"
                :disabled="downloading"
                @click="downloadFile"
              >
                <i :class="downloading ? 'el-icon-loading' : 'el-icon-download'" />
                {{ downloadLabel }}
              </button>
            </div>
          </section>

          <section v-if="metaTags.length" class="doc-tags portal-card">
            <h3 class="section-title">{{ L.tags }}</h3>
            <div class="tag-cloud">
              <span v-for="tag in metaTags" :key="tag" class="tag-chip">#{{ tag }}</span>
            </div>
          </section>

          <section v-if="doc.summary" class="doc-summary-block portal-card">
            <h3 class="section-title">{{ L.summary }}</h3>
            <p class="doc-summary-text">{{ doc.summary }}</p>
          </section>

          <p class="doc-disclaimer">{{ L.disclaimer }}</p>

          <section v-if="relatedList.length" class="related-grid-section portal-card">
            <div class="section-head">
              <h3 class="section-title">{{ L.related }}</h3>
              <router-link to="/library" class="section-more">{{ L.relatedMore }}</router-link>
            </div>
            <div class="related-grid">
              <button
                v-for="item in relatedList"
                :key="item.documentId"
                type="button"
                class="related-card"
                @click="$router.push('/library/' + item.documentId)"
              >
                <div class="related-thumb">
                  <library-doc-cover :cover-url="item.coverUrl" :file-ext="item.fileExt" />
                </div>
                <p class="related-card-title" :title="item.title">{{ item.title }}</p>
                <p class="related-card-meta">
                  <span>{{ formatDateTime(item.createTime) }}</span>
                  <span><i class="el-icon-view" />{{ item.viewCount || 0 }}</span>
                </p>
              </button>
            </div>
          </section>
        </main>

        <!-- 右侧边栏 -->
        <aside class="doc-sidebar">
          <div class="sidebar-card sidebar-download">
            <div class="sidebar-download-actions">
              <button
                v-if="canDownload"
                type="button"
                class="btn-download-now btn-download-now--block"
                :class="{ 'is-paid': payAccess.needPay && !payAccess.purchased, 'is-owned': payAccess.purchased && payAccess.needPay }"
                :disabled="downloading"
                @click="downloadFile"
              >
                <i :class="downloading ? 'el-icon-loading' : 'el-icon-download'" />
                {{ downloadLabel }}
              </button>
              <button
                v-if="token"
                type="button"
                class="sidebar-fav-btn"
                :class="{ active: doc.favorited }"
                @click="toggleFavorite"
              >
                <i :class="doc.favorited ? 'el-icon-star-on' : 'el-icon-star-off'" />
              </button>
            </div>
            <p v-if="payAccess.needPay && !payAccess.purchased" class="sidebar-pay-tip">{{ L.payTip }}</p>
            <dl class="prop-list">
              <div class="prop-row"><dt>{{ L.uploader }}</dt><dd>{{ doc.createBy || '-' }}</dd></div>
              <div class="prop-row"><dt>{{ L.category }}</dt><dd>{{ doc.categoryName || '-' }}</dd></div>
              <div class="prop-row"><dt>{{ L.price }}</dt><dd class="prop-price">{{ priceDisplay }}</dd></div>
              <div class="prop-row"><dt>{{ L.pages }}</dt><dd>{{ displayPageCount }}{{ L.pageUnit }}</dd></div>
              <div class="prop-row"><dt>{{ L.size }}</dt><dd>{{ displaySize }}</dd></div>
              <div class="prop-row"><dt>{{ L.format }}</dt><dd>{{ extLabel }}</dd></div>
              <div class="prop-row"><dt>{{ L.uploadTime }}</dt><dd>{{ formatDateTime(doc.createTime) }}</dd></div>
            </dl>
          </div>

          <div class="sidebar-card sidebar-vip" :class="{ 'sidebar-vip--active': vipStatus.active }">
            <div class="vip-badge"><i class="el-icon-medal" /></div>
            <h4>{{ vipStatus.active ? L.vipActiveTitle : L.vipTitle }}</h4>
            <p v-if="vipStatus.active">{{ L.vipActiveHint.replace('{n}', vipStatus.remainDays || 0) }}</p>
            <p v-else>{{ L.vipHint }}</p>
            <router-link to="/library/vip" class="vip-link">{{ vipStatus.active ? L.manageVip : L.joinVip }}</router-link>
          </div>

          <div class="sidebar-card sidebar-author">
            <h4 class="sidebar-card-title">{{ L.authorDetail }}</h4>
            <div class="author-profile">
              <span class="author-avatar"><i class="el-icon-user-solid" /></span>
              <div class="author-info">
                <strong>{{ doc.createBy || L.anonymous }}</strong>
                <span>{{ docCatalogLine || L.authorDocs }}</span>
              </div>
            </div>
          </div>

          <div v-if="relatedList.length" class="sidebar-card">
            <h4 class="sidebar-card-title">{{ L.similar }}</h4>
            <ul class="sidebar-related-list">
              <li v-for="item in relatedList.slice(0, 6)" :key="item.documentId">
                <button type="button" class="sidebar-related-item" @click="$router.push('/library/' + item.documentId)">
                  <div class="sidebar-related-thumb">
                    <library-doc-cover :cover-url="item.coverUrl" :file-ext="item.fileExt" />
                  </div>
                  <div class="sidebar-related-main">
                    <p class="sidebar-related-title" :title="item.title">{{ item.title }}</p>
                    <p class="sidebar-related-meta">
                      <span>{{ item.categoryName || item.subjectName || '-' }}</span>
                      <span>{{ formatDateTime(item.createTime) }}</span>
                    </p>
                  </div>
                </button>
              </li>
            </ul>
          </div>

          <div v-if="metaTags.length" class="sidebar-card">
            <h4 class="sidebar-card-title">{{ L.hotTags }}</h4>
            <div class="sidebar-tag-cloud">
              <span v-for="tag in metaTags" :key="'hot-' + tag" class="sidebar-tag">{{ tag }}</span>
            </div>
          </div>

          <div class="sidebar-card sidebar-hot">
            <hot-downloads v-if="showHotDownloads" :exclude-id="documentId" embedded />
          </div>
        </aside>
      </div>

      <el-empty v-else-if="!loading && !doc && needLogin" :description="L.loginRequired">
        <el-button type="primary" size="small" @click="goLogin">{{ L.goLogin }}</el-button>
      </el-empty>
      <el-empty v-else-if="!loading && !doc" :description="L.notFound" />
    </div>
    <pay-dialog ref="payDialog" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import LibraryPreview from './components/LibraryPreview'
import LibraryDocCover from './components/LibraryDocCover'
import FileTypeIcon from './components/FileTypeIcon'
import PayDialog from '@/components/PayDialog'
import { getPayOrderStatus } from '@/api/education/pay'
import {
  buildDownloadButtonLabel,
  downloadLibraryWithPay,
  deriveLibraryPayStatus,
  isPayReturnQuery,
  loadLibraryPayStatus,
  resolvePayReturnOrderNo
} from '@/utils/libraryPayDownload'
import {
  getLibraryDocument,
  getLibraryPreview,
  recordLibraryView,
  favoriteLibraryDocument,
  unfavoriteLibraryDocument,
  listRelatedLibraryDocuments,
  saveLibraryReadProgress
} from '@/api/education/library'
import { getVipStatus } from '@/api/education/vip'
import { goPortalLogin } from '@/utils/portalLogin'

const L = {
  home: '\u9996\u9875',
  breadcrumb: '\u6587\u5e93',
  detail: '\u6587\u6863\u8be6\u60c5',
  subject: '\u5b66\u79d1',
  category: '\u5206\u7c7b',
  format: '\u683c\u5f0f',
  size: '\u5927\u5c0f',
  views: '\u9605\u8bfb\u91cf',
  favorites: '\u6536\u85cf\u6570',
  downloads: '\u4e0b\u8f7d\u91cf',
  readProgress: '\u9605\u8bfb\u8fdb\u5ea6',
  pages: '\u9875\u6570',
  pageUnit: '\u9875',
  sizeUnknown: '\u672a\u77e5',
  uploader: '\u4e0a\u4f20\u8005',
  uploadTime: '\u4e0a\u4f20\u65f6\u95f4',
  favorite: '\u6536\u85cf',
  unfavorite: '\u5df2\u6536\u85cf',
  download: '\u4e0b\u8f7d',
  downloadNow: '\u7acb\u5373\u4e0b\u8f7d',
  downloadPaid: '\u4ed8\u8d39\u4e0b\u8f7d',
  downloadOwned: '\u5df2\u8d2d\u4e70 \u00b7 \u4e0b\u8f7d',
  downloadHint: '\u4e0b\u8f7d\u9ad8\u6e05\u539f\u6587\u4ef6\uff0c\u65b9\u4fbf\u79bb\u7ebf\u4f7f\u7528',
  downloadedBy: '\u4eba\u5df2\u4e0b\u8f7d',
  payTip: '\u4ed8\u8d39\u540e\u53ef\u4e0b\u8f7d\u5b8c\u6574\u6587\u6863',
  payReturnPending: '\u6b63\u5728\u786e\u8ba4\u652f\u4ed8\u7ed3\u679c\u2026',
  payReturnSuccess: '\u652f\u4ed8\u6210\u529f\uff0c\u73b0\u53ef\u4e0b\u8f7d',
  tags: '\u6807\u7b7e',
  summary: '\u6458\u8981',
  disclaimer: '\u672c\u7ad9\u6587\u6863\u4ec5\u4f9b\u6559\u5b66\u4ea4\u6d41\u4f7f\u7528\uff0c\u8bf7\u9075\u5b88\u7248\u6743\u6cd5\u89c4\uff0c\u4e0d\u5f97\u7528\u4e8e\u5546\u4e1a\u76ee\u7684\u3002',
  relatedMore: '\u66f4\u591a',
  price: '\u4ef7\u683c',
  free: '\u514d\u8d39',
  vipTitle: 'VIP \u4e13\u4eab',
  vipActiveTitle: 'VIP \u4f1a\u5458',
  vipHint: '\u52a0\u5165 VIP \u53ef\u514d\u8d39\u4e0b\u8f7d\u5168\u7ad9\u4ed8\u8d39\u6587\u6863',
  vipActiveHint: 'VIP \u751f\u6548\u4e2d\uff0c\u5269\u4f59 {n} \u5929',
  joinVip: '\u7acb\u5373\u52a0\u5165 VIP',
  manageVip: '\u67e5\u770b\u4f1a\u5458\u6743\u76ca',
  authorDetail: '\u4e0a\u4f20\u8005',
  authorDocs: '\u6587\u6863\u4f5c\u8005',
  anonymous: '\u533f\u540d\u7528\u6237',
  similar: '\u540c\u7c7b\u63a8\u8350',
  hotTags: '\u70ed\u95e8\u6807\u7b7e',
  back: '\u8fd4\u56de',
  loginRequired: '\u8bf7\u767b\u5f55\u540e\u67e5\u770b\u6b64\u6587\u6863',
  goLogin: '\u53bb\u767b\u5f55',
  notFound: '\u6587\u6863\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650\u67e5\u770b',
  related: '\u76f8\u5173\u6587\u6863',
  stage: '\u5b66\u6bb5'
}

export default {
  name: 'PortalLibraryDetail',
  components: {
    LibraryPreview,
    LibraryDocCover,
    HotDownloads: () => import('./components/HotDownloads'),
    FileTypeIcon,
    PayDialog
  },
  data() {
    return {
      L,
      loading: false,
      showHotDownloads: false,
      doc: null,
      preview: {},
      relatedList: [],
      needLogin: false,
      lastSavedProgress: 0,
      pendingProgress: 0,
      previewPollTimer: null,
      progressSaveTimer: null,
      currentReadProgress: 0,
      livePageCount: 0,
      downloading: false,
      payAccess: {
        enabled: false,
        needPay: false,
        purchased: false,
        price: 0
      },
      vipStatus: {
        active: false,
        remainDays: 0
      }
    }
  },
  computed: {
    ...mapGetters(['token']),
    documentId() {
      return this.$route.params.documentId
    },
    canDownload() {
      return this.doc && (this.preview.allowDownload === '1' || this.doc.allowDownload === '1')
    },
    downloadPrice() {
      const fromPay = Number(this.payAccess && this.payAccess.price)
      if (Number.isFinite(fromPay) && fromPay > 0) return fromPay
      const fromPreview = Number(this.preview && this.preview.downloadPrice)
      if (Number.isFinite(fromPreview) && fromPreview > 0) return fromPreview
      const n = Number(this.doc && this.doc.downloadPrice)
      return Number.isFinite(n) && n > 0 ? n : 0
    },
    downloadLabel() {
      return buildDownloadButtonLabel({
        price: this.downloadPrice,
        needPay: this.payAccess.needPay,
        purchased: this.payAccess.purchased,
        freeLabel: L.downloadNow,
        paidLabel: L.downloadPaid,
        ownedLabel: L.downloadOwned
      })
    },
    priceDisplay() {
      if (this.payAccess.purchased && this.payAccess.needPay) return L.downloadOwned
      if (this.downloadPrice > 0) return '\u00a5' + this.downloadPrice.toFixed(2)
      return L.free
    },
    initialReadProgress() {
      if (!this.doc) return 0
      return Number(this.doc.readProgress) || 0
    },
    extLabel() {
      return (this.doc && this.doc.fileExt ? this.doc.fileExt : 'file').toUpperCase()
    },
    extClass() {
      const ext = (this.doc && this.doc.fileExt) || 'file'
      return 'ext-' + ext.toLowerCase()
    },
    docCatalogLine() {
      if (!this.doc) return ''
      const parts = []
      if (this.doc.schoolStage) parts.push(this.doc.schoolStage)
      if (this.doc.subjectName) parts.push(this.doc.subjectName)
      if (this.doc.categoryName) parts.push(this.doc.categoryName)
      if (this.doc.versionName) parts.push(this.doc.versionName)
      if (this.doc.textbookName) parts.push(this.doc.textbookName)
      if (this.doc.chapterText) parts.push(this.doc.chapterText)
      return parts.join(' / ')
    },
    catalogTags() {
      if (!this.doc) return []
      const tags = []
      if (this.doc.schoolStage) tags.push({ key: 'stage', label: this.doc.schoolStage, cls: 'is-stage' })
      if (this.doc.subjectName) tags.push({ key: 'subject', label: this.doc.subjectName, cls: 'is-subject' })
      if (this.doc.categoryName) tags.push({ key: 'category', label: this.doc.categoryName, cls: 'is-category' })
      if (this.doc.versionName) tags.push({ key: 'version', label: this.doc.versionName, cls: 'is-muted' })
      if (this.doc.textbookName) tags.push({ key: 'textbook', label: this.doc.textbookName, cls: 'is-muted' })
      return tags
    },
    displaySize() {
      const n = Number(this.doc && this.doc.fileSize) || 0
      return n > 0 ? this.formatSize(n) : L.sizeUnknown
    },
    displayPageCount() {
      const fromDoc = Number(this.doc && this.doc.pageCount) || 0
      return fromDoc > 0 ? fromDoc : this.livePageCount
    },
    metaTags() {
      if (!this.doc) return []
      const tags = []
      if (this.doc.schoolStage) tags.push(this.doc.schoolStage)
      if (this.doc.subjectName) tags.push(this.doc.subjectName)
      if (this.doc.categoryName) tags.push(this.doc.categoryName)
      if (this.doc.versionName) tags.push(this.doc.versionName)
      if (this.doc.textbookName) tags.push(this.doc.textbookName)
      if (this.doc.chapterText) tags.push(this.doc.chapterText)
      if (this.doc.tagNames) {
        this.doc.tagNames.split(/[,，;；\s]+/).filter(Boolean).forEach(t => {
          if (!tags.includes(t)) tags.push(t)
        })
      }
      return tags.slice(0, 8)
    },
    extraTags() {
      if (!this.doc) return []
      return this.metaTags.filter(t => t !== this.doc.categoryName && t !== this.doc.subjectName)
    }
  },
  watch: {
    documentId: {
      immediate: true,
      handler() { this.loadDetail() }
    },
    token(val, oldVal) {
      if (val && val !== oldVal && this.doc) {
        this.refreshPayAccess()
      }
      this.loadVipStatus()
    }
  },
  mounted() {
    this.scheduleVipStatusLoad()
    this.$nextTick(() => this.handlePayReturn())
  },
  beforeDestroy() {
    this.clearPreviewPoll()
    if (this.progressSaveTimer) {
      clearTimeout(this.progressSaveTimer)
      this.progressSaveTimer = null
    }
    this.flushProgress(100)
  },
  methods: {
    goLogin() {
      goPortalLogin(this.$router, this.$route.fullPath)
    },
    loadDetail() {
      if (!this.documentId) return
      this.loading = true
      this.needLogin = false
      this.showHotDownloads = false
      this.doc = null
      this.preview = {}
      this.relatedList = []
      this.lastSavedProgress = 0
      this.pendingProgress = 0
      this.currentReadProgress = 0
      this.livePageCount = 0
      if (this.progressSaveTimer) {
        clearTimeout(this.progressSaveTimer)
        this.progressSaveTimer = null
      }
      getLibraryDocument(this.documentId).then(res => {
        this.doc = res.data || null
        if (!this.doc) {
          return Promise.reject(new Error('not found'))
        }
        const saved = Number(this.doc.readProgress) || 0
        this.lastSavedProgress = saved
        this.pendingProgress = saved
        this.currentReadProgress = saved
        return this.loadPreview()
      }).then(() => {
        return this.refreshPayAccess()
      }).then(() => {
        this.loading = false
        this.scheduleSecondaryDetailLoads()
      }).catch(err => {
        this.doc = null
        const msg = (err && err.message) || ''
        if (msg.includes('401') || msg.includes('\u767b\u5f55') || msg.includes('Login required') || msg.includes('permission')) {
          this.needLogin = true
        }
      }).finally(() => {
        if (this.loading) {
          this.loading = false
        }
      })
    },
    scheduleSecondaryDetailLoads() {
      const run = () => {
        if (!this.documentId) return
        recordLibraryView(this.documentId).catch(() => {})
        listRelatedLibraryDocuments(this.documentId).then(res => {
          this.relatedList = res.data || []
        }).catch(() => { this.relatedList = [] })
        this.showHotDownloads = true
      }
      if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
        window.requestIdleCallback(run, { timeout: 1500 })
      } else {
        setTimeout(run, 80)
      }
    },
    loadPreview() {
      return getLibraryPreview(this.documentId).then(res => {
        this.preview = res.data || {}
        if (this.preview.needPay !== undefined || this.preview.downloadPrice !== undefined) {
          this.payAccess = deriveLibraryPayStatus({
            docPrice: this.preview.downloadPrice != null ? this.preview.downloadPrice : (this.doc && this.doc.downloadPrice),
            payEnabled: this.preview.payEnabled,
            purchased: this.preview.purchased,
            productName: this.doc && this.doc.title
          })
        }
        if (this.preview.convertStatus === 'pending') {
          this.schedulePreviewPoll()
        } else {
          this.clearPreviewPoll()
        }
      })
    },
    schedulePreviewPoll() {
      this.clearPreviewPoll()
      this.previewPollTimer = setTimeout(() => {
        this.loadPreview().catch(() => {})
      }, 3000)
    },
    clearPreviewPoll() {
      if (this.previewPollTimer) {
        clearTimeout(this.previewPollTimer)
        this.previewPollTimer = null
      }
    },
    formatSize(size) {
      const n = Number(size) || 0
      if (n < 1024) return n + ' B'
      if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
      return (n / 1024 / 1024).toFixed(1) + ' MB'
    },
    formatDateTime(value) {
      if (!value) return '-'
      const text = String(value).trim()
      const match = text.match(/^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2})/)
      if (match) return match[1] + ' ' + match[2]
      return text.length > 16 ? text.slice(0, 16) : text
    },
    loadVipStatus() {
      return getVipStatus().then(res => {
        const data = res.data || {}
        this.vipStatus = {
          active: !!data.active,
          remainDays: Number(data.remainDays) || 0
        }
      }).catch(() => {})
    },
    scheduleVipStatusLoad() {
      const run = () => this.loadVipStatus()
      if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
        window.requestIdleCallback(run, { timeout: 1500 })
      } else {
        setTimeout(run, 80)
      }
    },
    refreshPayAccess() {
      if (!this.documentId) return Promise.resolve()
      return loadLibraryPayStatus(this.documentId, {
        docPrice: this.doc && this.doc.downloadPrice,
        payEnabled: this.preview && this.preview.payEnabled,
        productName: this.doc && this.doc.title
      }).then(status => {
        this.payAccess = status
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
            return this.refreshPayAccess()
          }
          if (left <= 0) return Promise.resolve()
          return new Promise(resolve => setTimeout(resolve, 2000)).then(() => poll(left - 1))
        })
      }
      poll(8).then(() => this.refreshPayAccess()).then(() => {
        if (this.payAccess.purchased && this.payAccess.needPay && this.canDownload) {
          this.downloadFile()
        }
      }).finally(() => {
        loading.close()
        clearQuery()
      })
    },
    downloadFile() {
      if (this.downloading) return
      this.downloading = true
      downloadLibraryWithPay(this, {
        documentId: this.documentId,
        title: this.doc && this.doc.title,
        docPrice: this.downloadPrice,
        payEnabled: this.payAccess.enabled,
        dialogTitle: L.downloadPaid,
        onLogin: () => this.goLogin(),
        onStatus: status => { this.payAccess = status }
      }).then(() => {
        return this.refreshPayAccess()
      }).catch(err => {
        const msg = (err && err.message) || ''
        if (msg === 'login' || msg === 'cancelled') return
        this.$modal.msgError(msg || '\u4e0b\u8f7d\u5931\u8d25')
      }).finally(() => {
        this.downloading = false
      })
    },
    onReadProgress(pct) {
      const n = Number(pct) || 0
      if (n > this.currentReadProgress) {
        this.currentReadProgress = n
      }
      if (!this.token) return
      if (n - this.lastSavedProgress >= 10 || n >= 95) {
        this.flushProgress(n)
      }
    },
    onPreviewPageCount(count) {
      this.livePageCount = Number(count) || 0
    },
    flushProgress(pct) {
      if (!this.token || !this.documentId) return
      const n = Math.min(100, Math.max(0, Number(pct) || 0))
      if (n <= this.lastSavedProgress && n < 100) return
      this.pendingProgress = n
      if (this.progressSaveTimer) {
        clearTimeout(this.progressSaveTimer)
      }
      const delay = n >= 100 ? 0 : 1500
      this.progressSaveTimer = setTimeout(() => {
        this.progressSaveTimer = null
        const toSave = this.pendingProgress
        if (toSave <= this.lastSavedProgress && toSave < 100) return
        this.lastSavedProgress = toSave
        saveLibraryReadProgress(this.documentId, toSave).catch(() => {})
      }, delay)
    },
    toggleFavorite() {
      if (!this.token) {
        this.goLogin()
        return
      }
      const fn = this.doc.favorited ? unfavoriteLibraryDocument : favoriteLibraryDocument
      fn(this.documentId).then(() => {
        this.doc.favorited = !this.doc.favorited
        const delta = this.doc.favorited ? 1 : -1
        this.doc.favoriteCount = Math.max(0, (this.doc.favoriteCount || 0) + delta)
        this.$modal.msgSuccess(this.doc.favorited ? '\u5df2\u6536\u85cf' : '\u5df2\u53d6\u6d88\u6536\u85cf')
      })
    }
  }
}
</script>

<style scoped lang="scss">
$orange: #f97316;
$orange-dark: #ea580c;
$gold: #d97706;
$ink: #1e293b;
$muted: #64748b;
$border: #e2e8f0;
$bg: #f1f5f9;

// Typography scale
$fs-caption: 12px;
$fs-meta: 13px;
$fs-body: 14px;
$fs-subhead: 15px;
$fs-title: 17px;
$fs-price: 18px;

.doc-preview-page {
  background: $bg;
  font-size: $fs-body;
  line-height: 1.6;
  color: $ink;

  ::v-deep .portal-breadcrumb {
    padding: 14px 0 12px;
    font-size: $fs-meta;

    .sep {
      font-size: $fs-caption;
    }

    .current {
      font-weight: 600;
    }
  }
}

.doc-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
}

.doc-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-header {
  padding: 18px 20px;
}

.doc-header-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.doc-title {
  margin: 0;
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: $fs-title;
  font-weight: 700;
  color: $ink;
  line-height: 1.45;
  letter-spacing: -0.01em;
}

.doc-title-text {
  min-width: 0;
  flex: 1;
}

.doc-fav-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border: 1px solid $border;
  border-radius: 8px;
  background: #fff;
  color: #94a3b8;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;

  &.active, &:hover {
    color: $gold;
    border-color: #fde68a;
    background: #fffbeb;
  }
}

.doc-meta-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
  font-size: $fs-meta;
  color: $muted;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;

  i { color: #94a3b8; font-size: $fs-meta; }
}

.meta-progress {
  color: $orange;
  font-weight: 600;
}

.preview-panel {
  padding: 0;
  border: 1px solid $border;
  display: flex;
  flex-direction: column;
}

.inline-download {
  padding: 20px 24px;
  text-align: center;
}

.inline-download-hint {
  margin: 0 0 14px;
  font-size: $fs-body;
  color: $muted;
}

.inline-download-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
}

.inline-download-price {
  text-align: left;
}

.price-label {
  display: block;
  font-size: $fs-price;
  font-weight: 700;
  color: $orange;
}

.price-sub {
  font-size: $fs-meta;
  color: #94a3b8;
}

.btn-download-now {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 140px;
  height: 44px;
  padding: 0 28px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(180deg, #fb923c, $orange);
  color: #fff;
  font-size: $fs-body;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(249, 115, 22, 0.35);
  transition: transform 0.15s, box-shadow 0.2s;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(249, 115, 22, 0.4);
  }

  &.is-paid {
    background: linear-gradient(180deg, #fbbf24, $gold);
    box-shadow: 0 4px 14px rgba(217, 119, 6, 0.3);
  }

  &.is-owned {
    background: linear-gradient(180deg, #4ade80, #16a34a);
    box-shadow: 0 4px 14px rgba(22, 163, 74, 0.28);
  }

  &:disabled {
    opacity: 0.75;
    cursor: wait;
  }

  &--block {
    flex: 1;
    min-width: 0;
  }
}

.section-title {
  margin: 0 0 12px;
  font-size: $fs-subhead;
  font-weight: 600;
  color: $ink;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  .section-title { margin: 0; }
}

.section-more {
  font-size: $fs-meta;
  color: $orange;
  text-decoration: none;
  &:hover { text-decoration: underline; }
}

.doc-tags, .doc-summary-block, .related-grid-section {
  padding: 16px 20px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  padding: 4px 12px;
  border-radius: 4px;
  background: #f8fafc;
  border: 1px solid $border;
  font-size: $fs-meta;
  color: $muted;
}

.doc-summary-text {
  margin: 0;
  font-size: $fs-body;
  color: #475569;
  line-height: 1.7;
}

.doc-disclaimer {
  margin: 0;
  padding: 0 4px;
  font-size: $fs-caption;
  color: #94a3b8;
  line-height: 1.6;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.related-card {
  border: 1px solid $border;
  border-radius: 8px;
  background: #fff;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  text-align: left;
  transition: box-shadow 0.2s, transform 0.15s;

  &:hover {
    box-shadow: 0 6px 20px rgba(15, 23, 42, 0.08);
    transform: translateY(-2px);
  }
}

.related-thumb {
  aspect-ratio: 3/4;
  background: #f8fafc;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .library-doc-cover {
    width: 100%;
    height: 100%;
  }
}

.related-thumb-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 32px;
}

.related-card-title {
  margin: 8px 10px 4px;
  font-size: $fs-meta;
  font-weight: 600;
  color: $ink;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-card-meta {
  margin: 0 10px 10px;
  display: flex;
  justify-content: space-between;
  font-size: $fs-caption;
  color: #94a3b8;
}

.doc-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-card {
  padding: 16px;
  background: #fff;
  border: 1px solid $border;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.sidebar-card-title {
  margin: 0 0 12px;
  font-size: $fs-subhead;
  font-weight: 600;
  color: $ink;
}

.sidebar-download-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.sidebar-fav-btn {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border: 1px solid $border;
  border-radius: 6px;
  background: #fff;
  color: #94a3b8;
  font-size: 20px;
  cursor: pointer;

  &.active, &:hover {
    color: $gold;
    border-color: #fde68a;
    background: #fffbeb;
  }
}

.sidebar-pay-tip {
  margin: 0 0 10px;
  font-size: $fs-meta;
  color: $gold;
  line-height: 1.5;
}

.prop-list {
  margin: 0;
}

.prop-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #f1f5f9;
  font-size: $fs-meta;

  &:last-child { border-bottom: none; }

  dt {
    margin: 0;
    color: #94a3b8;
    flex-shrink: 0;
  }

  dd {
    margin: 0;
    font-size: $fs-body;
    color: $ink;
    text-align: right;
    word-break: break-all;
  }
}

.prop-price {
  color: $orange !important;
  font-weight: 600;
}

.sidebar-vip {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-color: #fde68a;
  text-align: center;

  &.sidebar-vip--active {
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
    border-color: #f59e0b;
  }

  .vip-badge {
    width: 40px;
    height: 40px;
    margin: 0 auto 8px;
    border-radius: 50%;
    background: linear-gradient(135deg, #fbbf24, $gold);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
  }

  h4 {
    margin: 0 0 6px;
    font-size: $fs-subhead;
    font-weight: 600;
    color: #92400e;
  }

  p {
    margin: 0 0 12px;
    font-size: $fs-meta;
    color: #b45309;
    line-height: 1.55;
  }
}

.vip-link {
  display: inline-block;
  padding: 8px 20px;
  border-radius: 6px;
  background: linear-gradient(180deg, #fbbf24, $gold);
  color: #fff;
  font-size: $fs-meta;
  font-weight: 600;
  text-decoration: none;

  &:hover {
    opacity: 0.92;
  }
}

.author-profile {
  display: flex;
  gap: 12px;
  align-items: center;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e0e7ff, #c7d2fe);
  color: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.author-info {
  min-width: 0;

  strong {
    display: block;
    font-size: $fs-body;
    color: $ink;
    margin-bottom: 4px;
  }

  span {
    font-size: $fs-meta;
    color: $muted;
    line-height: 1.45;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.sidebar-related-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.sidebar-related-item {
  width: 100%;
  display: flex;
  gap: 10px;
  padding: 8px 0;
  border: none;
  border-bottom: 1px solid #f8fafc;
  background: none;
  cursor: pointer;
  text-align: left;

  &:last-child { border-bottom: none; }
  &:hover .sidebar-related-title { color: $orange; }
}

.sidebar-related-thumb {
  width: 48px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid $border;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .library-doc-cover {
    width: 100%;
    height: 100%;
  }
}

.sidebar-related-main {
  min-width: 0;
  flex: 1;
}

.sidebar-related-title {
  margin: 0 0 4px;
  font-size: $fs-body;
  font-weight: 600;
  color: $ink;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s;
}

.sidebar-related-meta {
  margin: 0;
  font-size: $fs-caption;
  color: #94a3b8;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sidebar-tag {
  padding: 4px 10px;
  border-radius: 4px;
  background: #f8fafc;
  border: 1px solid $border;
  font-size: $fs-meta;
  color: $muted;
  cursor: default;
}

.sidebar-hot {
  padding: 0;
  overflow: hidden;

  ::v-deep .hot-downloads--embedded {
    padding: 16px;
    border: none;
    border-top: none;
  }

  ::v-deep .hot-head h2::before {
    background: linear-gradient(180deg, #fb923c, #f97316);
  }

  ::v-deep .hot-head h2 {
    font-size: $fs-subhead;
    font-weight: 600;
  }

  ::v-deep .hot-title-link {
    font-size: $fs-meta;
  }

  ::v-deep .hot-meta,
  ::v-deep .hot-empty {
    font-size: $fs-caption;
  }

  ::v-deep .hot-rank {
    font-size: $fs-caption;
  }

  ::v-deep .hot-more {
    font-size: $fs-meta;
    color: #f97316;
    &:hover {
      background: #fff7ed;
      border-color: rgba(249, 115, 22, 0.2);
    }
  }

  ::v-deep .hot-item:hover {
    background: #fff7ed;
    border-color: rgba(249, 115, 22, 0.15);

    .hot-title-link { color: #ea580c; }
  }
}

@media (max-width: 1200px) {
  .related-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .doc-layout {
    grid-template-columns: 1fr;
  }

  .doc-sidebar {
    order: 2;
  }

  .related-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

}

@media (max-width: 600px) {
  .related-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .doc-title {
    font-size: 17px;
  }
}
</style>
