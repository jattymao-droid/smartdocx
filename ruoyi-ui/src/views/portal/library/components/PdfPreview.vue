<template>
  <div v-loading="loading" class="pdf-preview wenku-preview" :class="{ 'wenku-preview--boxed': !embedInPage }" @keydown="onKeydown" tabindex="0" ref="root">
    <div v-if="loadError" class="pdf-empty">{{ loadError }}</div>
    <template v-else-if="pdfDoc">
      <div class="pdf-chrome" :class="{ 'pdf-chrome--sticky': embedInPage }">
        <div class="pdf-chrome-left">
          <span class="pdf-chrome-title">{{ chromeTitle }}</span>
          <span class="pdf-chrome-page">{{ currentPage }} / {{ pageCount }}<template v-if="isTruncated"> ({{ totalPageLabel }})</template></span>
        </div>
        <div class="pdf-chrome-actions">
          <button type="button" class="chrome-btn" :title="zoomOutLabel" @click="zoomOut">
            <i class="el-icon-zoom-out" />
          </button>
          <span class="chrome-zoom">{{ zoomPercent }}%</span>
          <button type="button" class="chrome-btn" :title="zoomInLabel" @click="zoomIn">
            <i class="el-icon-zoom-in" />
          </button>
          <button type="button" class="chrome-btn chrome-btn--text" @click="resetFit">{{ fitLabel }}</button>
          <button type="button" class="chrome-btn" :title="fullscreenLabel" @click="$emit('fullscreen')">
            <i class="el-icon-full-screen" />
          </button>
        </div>
      </div>
      <div ref="scrollWrap" class="pdf-scroll-wrap" :class="{ 'pdf-scroll-wrap--inline': embedInPage }" @scroll="onContainerScroll">
        <div class="pdf-pages">
          <div
            v-for="n in pageCount"
            :key="n"
            :ref="'page-' + n"
            class="pdf-page"
            :class="{ 'is-active': n === currentPage, 'is-landscape': pageIsLandscape }"
            :data-page="n"
            :style="pageShellStyle(n)"
          >
            <div v-if="!isPageRendered(n)" class="page-skeleton">
              <i v-if="isPageQueued(n)" class="el-icon-loading" />
            </div>
            <div v-show="isPageRendered(n)" class="pdf-page-body">
              <canvas :ref="'canvas-' + n" />
            </div>
            <div class="page-footer">{{ n }}</div>
          </div>
        </div>
        <div v-if="isTruncated" class="preview-limit-banner">
          <p>{{ truncationHint }}</p>
          <button v-if="allowDownload" type="button" class="preview-limit-download" @click="$emit('download')">
            {{ effectiveDownloadLabel }}
          </button>
        </div>
      </div>
    </template>
    <div v-else-if="!loading" class="pdf-empty">{{ loadingLabel }}</div>
  </div>
</template>

<script>
import * as pdfjsLib from 'pdfjs-dist/legacy/build/pdf'
import { buildPortalPdfFetchUrl } from '@/utils/portalBanner'

const PDF_CMAP_URL = `${process.env.BASE_URL}cmaps/`
const PDF_STANDARD_FONT_URL = `${process.env.BASE_URL}standard_fonts/`
const MAX_CANVAS_SIDE = 8192
const MAX_CANVAS_PIXELS = 16777216
const RENDER_BUFFER_PX = 480
const MAX_CONCURRENT_RENDERS = 2

if (pdfjsLib.GlobalWorkerOptions) {
  pdfjsLib.GlobalWorkerOptions.workerSrc = `${process.env.BASE_URL}pdf.worker.min.js`
}

function buildPdfFetchUrl(url) {
  return buildPortalPdfFetchUrl(url)
}

export default {
  name: 'PdfPreview',
  props: {
    url: { type: String, default: '' },
    initialProgress: { type: Number, default: 0 },
    allowDownload: { type: Boolean, default: false },
    downloadLabel: { type: String, default: '' },
    maxPreviewPages: { type: Number, default: 0 },
    embedInPage: { type: Boolean, default: true }
  },
  data() {
    return {
      loading: false,
      loadError: '',
      pdfDoc: null,
      pageCount: 0,
      totalPageCount: 0,
      zoomFactor: 1,
      currentPage: 1,
      jumpPage: '1',
      scrollProgress: 0,
      renderedPages: {},
      renderTasks: {},
      pageDisplayHeights: {},
      pageDisplayWidths: {},
      defaultPageRatio: 1.414,
      pageIsLandscape: false,
      scrollTicking: false,
      layoutReady: false,
      resumeApplied: false,
      renderQueue: [],
      activeRenders: 0,
      resizeObserver: null,
      resizeTimer: null
    }
  },
  computed: {
    loadingLabel() {
      return '\u52a0\u8f7d\u4e2d\u2026'
    },
    chromeTitle() {
      return '\u6587\u6863\u9884\u89c8'
    },
    zoomInLabel() {
      return '\u653e\u5927'
    },
    zoomOutLabel() {
      return '\u7f29\u5c0f'
    },
    fitLabel() {
      return '\u9002\u5e94\u5bbd\u5ea6'
    },
    fullscreenLabel() {
      return '\u5168\u5c4f'
    },
    zoomPercent() {
      return Math.round((this.zoomFactor || 1) * 100)
    },
    readProgress() {
      return this.scrollProgress
    },
    isTruncated() {
      return this.totalPageCount > this.pageCount
    },
    totalPageLabel() {
      return '\u5171 ' + this.totalPageCount + ' \u9875'
    },
    truncationHint() {
      const remaining = Math.max(0, this.totalPageCount - this.pageCount)
      return '\u6587\u6863\u5171 ' + this.totalPageCount + ' \u9875\uff0c\u4ec5\u9884\u89c8\u524d ' + this.pageCount + ' \u9875\uff0c\u8fd8\u6709 ' + remaining + ' \u9875\u672a\u663e\u793a\uff0c\u8bf7\u4e0b\u8f7d\u6587\u6863\u67e5\u770b\u5b8c\u6574\u5185\u5bb9'
    },
    effectiveDownloadLabel() {
      return this.downloadLabel || '\u4e0b\u8f7d\u6587\u6863'
    },
    renderKey() {
      return `${this.getTargetPageWidth()}:${this.zoomFactor}`
    }
  },
  watch: {
    url: {
      immediate: true,
      handler() {
        this.loadPdf()
      }
    },
    zoomFactor() {
      if (!this.layoutReady || this.loading) return
      this.pageDisplayHeights = {}
      this.pageDisplayWidths = {}
      this.renderedPages = {}
      this.measureAllPageLayouts().then(() => this.rerenderAll())
    },
    embedInPage(val) {
      if (val) {
        window.addEventListener('scroll', this.onPageScroll, { passive: true })
        window.addEventListener('resize', this.onPageScroll, { passive: true })
      } else {
        window.removeEventListener('scroll', this.onPageScroll)
        window.removeEventListener('resize', this.onPageScroll)
      }
      this.$nextTick(() => {
        this.syncReadState()
        this.renderVisiblePages()
      })
    }
  },
  mounted() {
    this.initResizeObserver()
    if (this.embedInPage) {
      window.addEventListener('scroll', this.onPageScroll, { passive: true })
      window.addEventListener('resize', this.onPageScroll, { passive: true })
    }
  },
  beforeDestroy() {
    if (this.embedInPage) {
      window.removeEventListener('scroll', this.onPageScroll)
      window.removeEventListener('resize', this.onPageScroll)
    }
    this.destroyResizeObserver()
    this.destroyPdf()
  },
  methods: {
    getTargetPageWidth() {
      const wrap = this.$refs.scrollWrap
      const root = this.$refs.root
      const el = (wrap && wrap.clientWidth > 0) ? wrap : root
      return el && el.clientWidth > 0 ? el.clientWidth : 760
    },
    destroyPdf() {
      this.renderQueue = []
      Object.keys(this.renderTasks).forEach(k => {
        const task = this.renderTasks[k]
        if (task) task.cancel()
      })
      this.renderTasks = {}
      this.renderedPages = {}
      this.activeRenders = 0
      if (this.pdfDoc) {
        this.pdfDoc.destroy()
        this.pdfDoc = null
      }
    },
    loadPdf() {
      this.destroyPdf()
      this.loadError = ''
      this.layoutReady = false
      this.resumeApplied = false
      this.zoomFactor = 1
      this.currentPage = 1
      this.jumpPage = '1'
      this.scrollProgress = 0
      this.pageCount = 0
      this.totalPageCount = 0
      this.pageDisplayHeights = {}
      this.pageDisplayWidths = {}
      const src = buildPdfFetchUrl(this.url)
      if (!src) return
      this.loading = true
      fetch(src, { credentials: 'same-origin' })
        .then(res => {
          if (!res.ok) {
            throw new Error('fetch failed')
          }
          const type = String(res.headers.get('content-type') || '').toLowerCase()
          if (type.includes('json') || type.includes('html')) {
            throw new Error('invalid content type')
          }
          return res.arrayBuffer()
        })
        .then(data => {
          if (!data || data.byteLength < 4) {
            throw new Error('empty pdf')
          }
          const sig = String.fromCharCode(...new Uint8Array(data.slice(0, 4)))
          if (sig !== '%PDF') {
            throw new Error('invalid pdf')
          }
          return data
        })
        .then(data => pdfjsLib.getDocument({
          data,
          cMapUrl: PDF_CMAP_URL,
          cMapPacked: true,
          standardFontDataUrl: PDF_STANDARD_FONT_URL,
          isEvalSupported: false
        }).promise)
        .then(doc => {
          this.pdfDoc = doc
          this.totalPageCount = doc.numPages
          const limit = this.maxPreviewPages > 0 ? this.maxPreviewPages : doc.numPages
          this.pageCount = Math.min(doc.numPages, limit)
          this.$emit('page-count', doc.numPages)
          return doc.getPage(1)
        })
        .then(page => {
          const vp = page.getViewport({ scale: 1 })
          this.defaultPageRatio = vp.height / vp.width
          this.pageIsLandscape = vp.width > vp.height
          return this.$nextTick()
        })
        .then(() => new Promise(resolve => requestAnimationFrame(resolve)))
        .then(() => this.measureAllPageLayouts())
        .then(() => {
          this.layoutReady = true
          return this.$nextTick()
        })
        .then(() => this.renderVisiblePages())
        .then(() => {
          this.applyResumePosition()
          this.syncReadState()
          this.bindResizeObserver()
          this.$nextTick(() => {
            if (this.$refs.root) this.$refs.root.focus()
          })
        })
        .catch(() => {
          this.loadError = '\u65e0\u6cd5\u52a0\u8f7d\u6587\u6863\u9884\u89c8'
        })
        .finally(() => {
          this.loading = false
        })
    },
    applyResumePosition() {
      if (this.resumeApplied || !this.pageCount) return
      const pct = Math.min(100, Math.max(0, Number(this.initialProgress) || 0))
      if (pct <= 0) return
      this.resumeApplied = true
      this.$nextTick(() => {
        const page = Math.min(
          this.pageCount,
          Math.max(1, Math.ceil((this.pageCount * pct) / 100))
        )
        if (page > 1) {
          this.scrollToPage(page, false)
        } else {
          this.syncReadState()
          this.emitProgress()
        }
      })
    },
    measureAllPageLayouts() {
      if (!this.pdfDoc || !this.pageCount) return Promise.resolve()
      const targetW = this.getTargetPageWidth()
      const jobs = []
      for (let n = 1; n <= this.pageCount; n++) {
        jobs.push(
          this.pdfDoc.getPage(n).then(page => {
            const vp = page.getViewport({ scale: 1 })
            const displayW = Math.round(targetW * this.zoomFactor)
            const logicalScale = displayW / vp.width
            const displayH = Math.round(vp.height * logicalScale) + 28
            this.$set(this.pageDisplayHeights, n, displayH)
            this.$set(this.pageDisplayWidths, n, displayW)
          })
        )
      }
      return Promise.all(jobs)
    },
    syncLayoutFromDom() {
      for (let n = 1; n <= this.pageCount; n++) {
        const node = this.pageNode(n)
        if (node && node.offsetHeight > 0) {
          this.$set(this.pageDisplayHeights, n, node.offsetHeight)
        }
      }
    },
    isScrolledToBottom(wrap) {
      if (!wrap) return false
      if (this.embedInPage) {
        const rect = wrap.getBoundingClientRect()
        return rect.bottom <= window.innerHeight + 12
      }
      const maxScroll = wrap.scrollHeight - wrap.clientHeight
      return maxScroll <= 12 || wrap.scrollTop >= maxScroll - 12
    },
    detectCurrentPage() {
      const wrap = this.$refs.scrollWrap
      if (!wrap || !this.pageCount) return 1
      if (this.isScrolledToBottom(wrap)) {
        return this.pageCount
      }
      if (this.embedInPage) {
        const wrapRect = wrap.getBoundingClientRect()
        if (wrapRect.top >= -8) {
          return 1
        }
      } else if (wrap.scrollTop <= 8) {
        return 1
      }

      const markerY = this.embedInPage
        ? window.innerHeight * 0.5
        : wrap.getBoundingClientRect().top + wrap.getBoundingClientRect().height * 0.5
      let page = 1
      for (let n = 1; n <= this.pageCount; n++) {
        const node = this.pageNode(n)
        if (!node) continue
        const rect = node.getBoundingClientRect()
        if (rect.top <= markerY && rect.bottom >= markerY) {
          return n
        }
        if (rect.top <= markerY) {
          page = n
        }
      }
      return page
    },
    calcReadProgress() {
      const wrap = this.$refs.scrollWrap
      if (!wrap || !this.pageCount) return 0
      if (this.isScrolledToBottom(wrap)) {
        return 100
      }
      if (this.embedInPage) {
        const rect = wrap.getBoundingClientRect()
        const contentHeight = wrap.offsetHeight
        const viewportHeight = window.innerHeight
        const scrollable = Math.max(0, contentHeight - viewportHeight)
        if (scrollable <= 0) {
          return Math.min(100, Math.round((this.currentPage / this.pageCount) * 100))
        }
        const scrolled = Math.max(0, -rect.top)
        return Math.min(100, Math.max(0, Math.round((scrolled / scrollable) * 100)))
      }
      const maxScroll = Math.max(0, wrap.scrollHeight - wrap.clientHeight)
      if (maxScroll <= 0) {
        return Math.min(100, Math.round((this.currentPage / this.pageCount) * 100))
      }
      return Math.min(100, Math.max(0, Math.round((wrap.scrollTop / maxScroll) * 100)))
    },
    pageNode(pageNum) {
      const el = this.$refs['page-' + pageNum]
      return Array.isArray(el) ? el[0] : el
    },
    calcScrollProgress() {
      return this.calcReadProgress()
    },
    syncReadState() {
      if (!this.pageCount) {
        this.scrollProgress = 0
        return
      }
      this.syncLayoutFromDom()
      this.currentPage = this.detectCurrentPage()
      this.jumpPage = String(this.currentPage)
      this.scrollProgress = this.calcReadProgress()
    },
    isRenderCancelled(err) {
      if (!err) return false
      if (err.name === 'RenderingCancelledException') return true
      return /cancel/i.test(String(err.message || ''))
    },
    clampRenderScale(page, scale) {
      let next = scale
      let viewport = page.getViewport({ scale: next })
      const maxSide = Math.max(viewport.width, viewport.height)
      if (maxSide > MAX_CANVAS_SIDE) {
        next = next * (MAX_CANVAS_SIDE / maxSide)
        viewport = page.getViewport({ scale: next })
      }
      const pixels = viewport.width * viewport.height
      if (pixels > MAX_CANVAS_PIXELS) {
        next = next * Math.sqrt(MAX_CANVAS_PIXELS / pixels)
      }
      return Math.max(0.25, next)
    },
    canvasRef(pageNum) {
      const ref = this.$refs['canvas-' + pageNum]
      return Array.isArray(ref) ? ref[0] : ref
    },
    isPageRendered(pageNum) {
      return this.renderedPages[pageNum] === this.renderKey
    },
    isPageQueued(pageNum) {
      return !!this.renderTasks[pageNum] || this.renderQueue.includes(pageNum)
    },
    pageShellStyle(pageNum) {
      const style = { width: '100%' }
      const h = this.pageDisplayHeights[pageNum]
      if (h) {
        style.minHeight = h + 'px'
      } else {
        const est = Math.round(this.getTargetPageWidth() * this.defaultPageRatio * this.zoomFactor) + 28
        style.minHeight = est + 'px'
      }
      return style
    },
    enqueueRender(pageNum) {
      if (!this.pdfDoc || this.isPageRendered(pageNum)) return
      if (this.renderTasks[pageNum] || this.renderQueue.includes(pageNum)) return
      this.renderQueue.push(pageNum)
      this.drainRenderQueue()
    },
    drainRenderQueue() {
      while (this.activeRenders < MAX_CONCURRENT_RENDERS && this.renderQueue.length) {
        const pageNum = this.renderQueue.shift()
        if (!pageNum || this.isPageRendered(pageNum)) continue
        this.activeRenders += 1
        this.renderPage(pageNum).finally(() => {
          this.activeRenders -= 1
          this.drainRenderQueue()
        })
      }
    },
    renderPage(pageNum) {
      if (!this.pdfDoc || this.isPageRendered(pageNum)) {
        return Promise.resolve()
      }
      if (this.renderTasks[pageNum]) {
        this.renderTasks[pageNum].cancel()
      }
      return this.$nextTick().then(() => {
        const canvas = this.canvasRef(pageNum)
        if (!canvas) return Promise.resolve()
        const targetW = this.getTargetPageWidth()
        const dpr = Math.min(window.devicePixelRatio || 1, 2)
        return this.pdfDoc.getPage(pageNum).then(page => {
          const baseVp = page.getViewport({ scale: 1 })
          const displayW = Math.round(targetW * this.zoomFactor)
          const logicalScale = displayW / baseVp.width
          const safeScale = this.clampRenderScale(page, logicalScale * dpr)
          const viewport = page.getViewport({ scale: safeScale })
          const displayH = Math.round(baseVp.height * logicalScale)
          canvas.width = Math.floor(viewport.width)
          canvas.height = Math.floor(viewport.height)
          canvas.style.width = '100%'
          canvas.style.height = displayH + 'px'
          canvas.style.maxWidth = '100%'
          canvas.style.maxHeight = 'none'
          const ctx = canvas.getContext('2d')
          const task = page.render({ canvasContext: ctx, viewport })
          this.renderTasks[pageNum] = task
          return task.promise.then(() => {
            this.$set(this.renderedPages, pageNum, this.renderKey)
            this.$set(this.pageDisplayWidths, pageNum, displayW)
            this.$set(this.pageDisplayHeights, pageNum, displayH + 28)
            delete this.renderTasks[pageNum]
            this.$nextTick(() => {
              this.syncLayoutFromDom()
              requestAnimationFrame(() => this.syncReadState())
            })
          })
        }).catch(err => {
          delete this.renderTasks[pageNum]
          if (this.isRenderCancelled(err)) return
          this.loadError = '\u9875\u9762\u6e32\u67d3\u5931\u8d25'
        })
      })
    },
    collectVisiblePages() {
      const wrap = this.$refs.scrollWrap
      if (!wrap || !this.pageCount) return []
      const viewportTop = this.embedInPage ? -RENDER_BUFFER_PX : wrap.getBoundingClientRect().top - RENDER_BUFFER_PX
      const viewportBottom = this.embedInPage
        ? window.innerHeight + RENDER_BUFFER_PX
        : wrap.getBoundingClientRect().bottom + RENDER_BUFFER_PX
      const pages = []
      for (let n = 1; n <= this.pageCount; n++) {
        const node = this.pageNode(n)
        if (!node) continue
        const rect = node.getBoundingClientRect()
        if (rect.bottom >= viewportTop && rect.top <= viewportBottom) {
          pages.push(n)
        }
      }
      return pages
    },
    renderVisiblePages() {
      const pages = this.collectVisiblePages()
      pages.forEach(n => this.enqueueRender(n))
      return Promise.resolve()
    },
    rerenderAll() {
      this.renderedPages = {}
      this.renderQueue = []
      return this.$nextTick().then(() => this.renderVisiblePages()).then(() => {
        this.syncReadState()
      })
    },
    onContainerScroll() {
      if (this.embedInPage) return
      this.handleScroll()
    },
    onPageScroll() {
      if (!this.embedInPage) return
      this.handleScroll()
    },
    handleScroll() {
      if (this.scrollTicking) return
      this.scrollTicking = true
      requestAnimationFrame(() => {
        this.scrollTicking = false
        this.syncReadState()
        this.renderVisiblePages()
        this.emitProgress()
      })
    },
    updateCurrentPage() {
      this.syncReadState()
    },
    scrollToPage(pageNum, smooth) {
      const n = Math.min(this.pageCount, Math.max(1, pageNum))
      this.jumpPage = String(n)
      const node = this.pageNode(n)
      const wrap = this.$refs.scrollWrap
      if (node && wrap) {
        if (this.embedInPage) {
          const top = Math.max(0, window.scrollY + node.getBoundingClientRect().top - 72)
          window.scrollTo({
            top,
            behavior: smooth ? 'smooth' : 'auto'
          })
        } else {
          const wrapRect = wrap.getBoundingClientRect()
          const nodeRect = node.getBoundingClientRect()
          wrap.scrollTo({
            top: Math.max(0, wrap.scrollTop + (nodeRect.top - wrapRect.top)),
            behavior: smooth ? 'smooth' : 'auto'
          })
        }
      }
      this.renderVisiblePages()
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.syncReadState()
          this.emitProgress()
        })
      })
    },
    onKeydown(e) {
      if (!this.pdfDoc) return
      if (e.key === 'ArrowDown' || e.key === 'PageDown') {
        e.preventDefault()
        this.scrollToPage(this.currentPage + 1, true)
      } else if (e.key === 'ArrowUp' || e.key === 'PageUp') {
        e.preventDefault()
        this.scrollToPage(this.currentPage - 1, true)
      } else if (e.key === 'Home') {
        e.preventDefault()
        this.scrollToPage(1, true)
      } else if (e.key === 'End') {
        e.preventDefault()
        this.scrollToPage(this.pageCount, true)
      }
    },
    emitProgress() {
      this.$emit('progress', this.scrollProgress)
    },
    resetFit() {
      this.zoomFactor = 1
      this.pageDisplayHeights = {}
      this.pageDisplayWidths = {}
      this.renderedPages = {}
      return this.measureAllPageLayouts().then(() => this.rerenderAll())
    },
    zoomIn() {
      this.zoomFactor = Math.min(3, +(this.zoomFactor * 1.12).toFixed(3))
    },
    zoomOut() {
      this.zoomFactor = Math.max(0.5, +(this.zoomFactor / 1.12).toFixed(3))
    },
    initResizeObserver() {
      if (typeof ResizeObserver === 'undefined') return
      this.resizeObserver = new ResizeObserver(() => {
        if (!this.layoutReady || this.loading || !this.pdfDoc) return
        if (this.resizeTimer) clearTimeout(this.resizeTimer)
        this.resizeTimer = setTimeout(() => {
          this.pageDisplayHeights = {}
          this.pageDisplayWidths = {}
          this.renderedPages = {}
          this.measureAllPageLayouts().then(() => this.rerenderAll())
        }, 120)
      })
    },
    bindResizeObserver() {
      if (!this.resizeObserver) return
      this.resizeObserver.disconnect()
      if (this.$refs.root) {
        this.resizeObserver.observe(this.$refs.root)
      }
      if (this.$refs.scrollWrap) {
        this.resizeObserver.observe(this.$refs.scrollWrap)
      }
    },
    destroyResizeObserver() {
      if (this.resizeTimer) {
        clearTimeout(this.resizeTimer)
        this.resizeTimer = null
      }
      if (this.resizeObserver) {
        this.resizeObserver.disconnect()
        this.resizeObserver = null
      }
    }
  }
}
</script>

<style scoped lang="scss">
.wenku-preview {
  width: 100%;
  background: #e8ebf0;
  display: flex;
  flex-direction: column;
  outline: none;
}

.wenku-preview:not(.wenku-preview--boxed) {
  height: auto;
}

.wenku-preview--boxed {
  flex: 1;
  min-height: 0;
  height: 100%;
}

.pdf-chrome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e2e8f0;
  backdrop-filter: blur(8px);
  z-index: 3;
}

.pdf-chrome-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.pdf-chrome-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.pdf-chrome-page {
  padding: 2px 8px;
  border-radius: 999px;
  background: #f1f5f9;
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}

.pdf-chrome-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.chrome-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;

  i {
    font-size: 14px;
  }

  &:hover {
    color: #0f766e;
    border-color: #99f6e4;
    background: #f0fdfa;
  }

  &--text {
    width: auto;
    padding: 0 10px;
    font-size: 13px;
    line-height: 26px;
  }
}

.chrome-zoom {
  min-width: 42px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.pdf-chrome--sticky {
  position: sticky;
  top: 0;
  z-index: 5;
}

.pdf-scroll-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  max-height: none;
  padding: 0 0 24px;
  background: #e8ebf0;
  position: relative;
  scroll-behavior: smooth;
}

.pdf-scroll-wrap--inline {
  flex: none;
  min-height: auto;
  overflow: visible;
  scroll-behavior: auto;
}

.pdf-pages {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
  width: 100%;
}

.pdf-page {
  position: relative;
  width: 100%;
  max-width: 100%;
  margin: 0;
  background: #fff;
  border: none;
  border-radius: 0;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08);
  padding: 0;
  line-height: 0;
  transition: box-shadow 0.2s ease;
  overflow: hidden;

  &.is-active {
    box-shadow: 0 4px 16px rgba(15, 118, 110, 0.14);
  }

  &.is-landscape {
    max-width: 100%;
  }
}

.pdf-page-body {
  display: block;
  width: 100%;
  background: #fff;
}

.pdf-page canvas {
  display: block;
  width: 100%;
  height: auto;
  margin: 0;
  max-width: 100%;
  image-rendering: auto;
}

.page-skeleton {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  background: linear-gradient(110deg, #f8fafc 8%, #eef2f7 18%, #f8fafc 33%);
  background-size: 200% 100%;
  animation: page-shimmer 1.2s ease-in-out infinite;
  color: #94a3b8;

  i {
    font-size: 24px;
  }
}

@keyframes page-shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

.page-footer {
  text-align: center;
  font-size: 12px;
  color: #cbd5e1;
  padding: 4px 0 6px;
  line-height: 1;
  background: #fafbfc;
  border-top: 1px solid #f1f5f9;
}

.pdf-empty {
  padding: 48px;
  text-align: center;
  color: #94a3b8;
}

.preview-limit-banner {
  margin: 16px 16px 0;
  padding: 14px 16px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fff7ed, #fffbeb);
  border: 1px solid #fed7aa;
  color: #9a3412;
  text-align: center;

  p {
    margin: 0 0 10px;
    font-size: 14px;
    line-height: 1.6;
  }
}

.preview-limit-download {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 108px;
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  background: #0f766e;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #0d9488;
  }
}
</style>
