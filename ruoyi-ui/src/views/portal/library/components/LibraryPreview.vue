<template>
  <div
    ref="previewRoot"
    class="library-preview"
    :class="previewClasses"
    @contextmenu="onContextMenu"
  >
    <preview-toolbar
      v-if="showOuterToolbar"
      :allow-download="allowDownload && !!fileUrl"
      :download-label="effectiveDownloadLabel"
      @download="downloadFile"
      @fullscreen="toggleFullscreen"
    />
    <div class="preview-body">
      <template v-if="archiveInner">
        <div class="archive-inner-bar">
          <button type="button" class="archive-inner-back" @click="exitArchiveInner">
            &#8592; {{ archiveBackLabel }}
          </button>
          <span class="archive-inner-title">{{ archiveInner.name }}</span>
        </div>
        <div v-if="archiveInnerLoading" class="preview-converting">
          <div class="converting-ring">
            <i class="el-icon-loading" />
          </div>
          <p>{{ convertingHint }}</p>
          <span class="converting-sub">{{ convertingSubHint }}</span>
        </div>
        <div v-else-if="archiveInnerLoadError" class="preview-converting">
          <p>{{ archiveInnerLoadError }}</p>
          <button type="button" class="archive-inner-back" @click="exitArchiveInner">{{ archiveBackLabel }}</button>
        </div>
        <pdf-preview
          v-else-if="archiveInnerMode === 'pdf' && archiveInnerMediaUrl"
          :url="archiveInnerMediaUrl"
          :initial-progress="0"
          :allow-download="false"
          :max-preview-pages="0"
          :embed-in-page="!isFullscreen"
          @fullscreen="toggleFullscreen"
        />
        <txt-preview
          v-else-if="archiveInnerMode === 'txt' && archiveInnerMediaUrl"
          :url="archiveInnerMediaUrl"
        />
      </template>
      <template v-else>
        <div v-if="previewType === 'pdf' && convertStatus === 'pending'" class="preview-converting">
          <div class="converting-ring">
            <i class="el-icon-loading" />
          </div>
          <p>{{ convertingHint }}</p>
          <span class="converting-sub">{{ convertingSubHint }}</span>
        </div>
        <pdf-preview
          v-else-if="previewType === 'pdf'"
          :url="resolvedPdfUrl"
          :initial-progress="initialProgress"
          :allow-download="allowDownload && !!fileUrl"
          :download-label="effectiveDownloadLabel"
          :max-preview-pages="maxPreviewPages"
          :embed-in-page="!isFullscreen"
          @progress="$emit('progress', $event)"
          @page-count="$emit('page-count', $event)"
          @download="downloadFile"
          @fullscreen="toggleFullscreen"
        />
        <txt-preview v-else-if="previewType === 'txt'" :url="resolvedFileUrl" />
        <div v-else-if="previewType === 'kkfileview' && convertStatus === 'pending'" class="preview-converting">
          <div class="converting-ring">
            <i class="el-icon-loading" />
          </div>
          <p>{{ archiveConvertingHint }}</p>
          <span class="converting-sub">{{ archiveConvertingSubHint }}</span>
        </div>
        <archive-file-list
          v-else-if="previewType === 'kkfileview'"
          :preview-url="activeKkfileUrl"
          @open-inner="openArchiveInner"
        />
        <div v-else class="preview-unsupported">
          <i class="el-icon-document" />
          <p>{{ error || unsupportedHint }}</p>
          <p v-if="error && previewType === 'unsupported'" class="preview-hint">{{ previewServiceHint }}</p>
          <el-tag v-if="convertStatus === 'pending'" size="small" type="warning">{{ convertingHint }}</el-tag>
          <el-button v-if="allowDownload && fileUrl" type="primary" size="small" @click="downloadFile">
            {{ effectiveDownloadLabel }}
          </el-button>
        </div>
      </template>
    </div>
    <div v-if="watermarkText" class="preview-watermark" aria-hidden="true">
      <span v-for="n in 12" :key="n">{{ watermarkText }}</span>
    </div>
  </div>
</template>

<script>
import screenfull from 'screenfull'
import PreviewToolbar from './PreviewToolbar'
import {
  buildArchiveInnerFetchUrl,
  buildArchiveInnerFileUrl,
  isTrustedArchiveMessage,
  resolveArchiveInnerPreview,
  triggerArchiveWarmup
} from '@/utils/libraryArchivePreview'

export default {
  name: 'LibraryPreview',
  components: {
    PdfPreview: () => import('./PdfPreview'),
    TxtPreview: () => import('./TxtPreview'),
    ArchiveFileList: () => import('./ArchiveFileList'),
    PreviewToolbar
  },
  props: {
    previewType: { type: String, default: '' },
    previewUrl: { type: String, default: '' },
    fileUrl: { type: String, default: '' },
    allowDownload: { type: Boolean, default: false },
    downloadLabel: { type: String, default: '' },
    watermark: { type: String, default: '' },
    error: { type: String, default: '' },
    convertStatus: { type: String, default: '' },
    initialProgress: { type: Number, default: 0 },
    maxPreviewPages: { type: Number, default: 0 }
  },
  data() {
    return {
      isFullscreen: false,
      archiveInner: null,
      archiveInnerMode: '',
      archiveInnerMediaUrl: '',
      archiveInnerLoading: false,
      archiveInnerLoadError: '',
      activeKkfileUrl: this.previewUrl || '',
      warmupTimer: null,
      archiveWarmupTriggered: false
    }
  },
  computed: {
    resolvedFileUrl() {
      return this.fileUrl
    },
    resolvedPdfUrl() {
      return this.previewUrl || this.fileUrl
    },
    watermarkText() {
      return this.watermark || ''
    },
    previewClasses() {
      return {
        'has-watermark': !!this.watermarkText,
        'preview-protected': !this.allowDownload,
        'is-fullscreen': this.isFullscreen,
        'is-archive-inner': !!this.archiveInner
      }
    },
    showOuterToolbar() {
      return this.previewType !== 'pdf' && !this.archiveInner
    },
    archiveBackLabel() {
      return '\u8fd4\u56de\u538b\u7f29\u5305\u5217\u8868'
    },
    unsupportedHint() {
      return '\u6682\u4e0d\u652f\u6301\u5728\u7ebf\u9884\u89c8\u6b64\u683c\u5f0f'
    },
    previewServiceHint() {
      return 'Office \u6587\u6863\u9700\u8981\u672c\u5730 LibreOffice \u8f6c\u6362\uff0c\u8bf7\u68c0\u67e5\u914d\u7f6e\u6216\u8054\u7cfb\u7ba1\u7406\u5458'
    },
    convertingHint() {
      return '\u6587\u6863\u8f6c\u6362\u4e2d\uff0c\u8bf7\u7a0d\u5019\u2026'
    },
    convertingSubHint() {
      return 'Office \u6587\u6863\u6b63\u5728\u8f6c\u4e3a PDF\uff0c\u5b8c\u6210\u540e\u53ef\u7eb5\u5411\u9605\u8bfb'
    },
    archiveConvertingHint() {
      return '\u538b\u7f29\u5305\u5185\u6587\u6863\u8f6c\u6362\u4e2d\uff0c\u8bf7\u7a0d\u5019\u2026'
    },
    archiveConvertingSubHint() {
      return '\u6b63\u5728\u9884\u5148\u8f6c\u6362\u538b\u7f29\u5305\u5185\u7684 Office \u6587\u6863\uff0c\u5b8c\u6210\u540e\u6253\u5f00\u5373\u53ef\u9884\u89c8'
    },
    effectiveDownloadLabel() {
      return this.downloadLabel || '\u4e0b\u8f7d\u6587\u6863'
    }
  },
  watch: {
    previewUrl(val) {
      if (!this.archiveInner) {
        this.activeKkfileUrl = val || ''
      }
    }
  },
  mounted() {
    if (screenfull.isEnabled) {
      screenfull.on('change', this.onFullscreenChange)
    }
    window.addEventListener('message', this.onArchiveMessage)
  },
  beforeDestroy() {
    if (screenfull.isEnabled) {
      screenfull.off('change', this.onFullscreenChange)
    }
    window.removeEventListener('message', this.onArchiveMessage)
    this.clearWarmupTimer()
  },
  methods: {
    onArchiveMessage(event) {
      if (!event || !event.data || event.data.type !== 'LIBRARY_ARCHIVE_INNER') return
      if (!isTrustedArchiveMessage(event.origin)) return
      this.openArchiveInner(event.data)
    },
    openArchiveInner(payload) {
      const spec = resolveArchiveInnerPreview(payload.ext)
      if (!spec.mode) {
        if (this.$message) {
          this.$message.warning('\u6682\u4e0d\u652f\u6301\u5728\u7ebf\u9884\u89c8\u6b64\u683c\u5f0f')
        }
        return
      }

      this.clearWarmupTimer()
      this.archiveInner = {
        name: payload.name || '',
        ext: payload.ext || '',
        archiveUrl: payload.archiveUrl || this.activeKkfileUrl,
        kkBase: payload.kkBase || '',
        innerPath: payload.innerPath || '',
        fileKey: payload.fileKey || ''
      }
      this.archiveInnerMode = spec.mode
      this.archiveInnerMediaUrl = ''
      this.archiveInnerLoadError = ''
      this.clearWarmupTimer()

      if (spec.mode === 'pdf') {
        if (spec.needsWarmup) {
          this.archiveInnerLoading = true
          this.archiveWarmupTriggered = false
          this.resolveArchivePdfAfterWarmup(0)
        } else {
          this.archiveInnerMediaUrl = buildArchiveInnerFileUrl(payload.kkBase, payload.innerPath, payload.ext)
        }
        return
      }

      if (spec.mode === 'txt') {
        this.archiveInnerMediaUrl = buildArchiveInnerFileUrl(payload.kkBase, payload.innerPath, payload.ext)
      }
    },
    resolveArchivePdfAfterWarmup(attempt) {
      if (!this.archiveInnerLoading || !this.archiveInner) return
      const fetchUrl = buildArchiveInnerFetchUrl(
        this.archiveInner.kkBase,
        this.archiveInner.innerPath,
        this.archiveInner.ext
      )
      const fileUrl = buildArchiveInnerFileUrl(
        this.archiveInner.kkBase,
        this.archiveInner.innerPath,
        this.archiveInner.ext
      )
      const delay = attempt === 0 ? 200 : 800
      this.warmupTimer = window.setTimeout(() => {
        this.probeArchivePdf(fetchUrl).then(ok => {
          if (!this.archiveInnerLoading || !this.archiveInner) return
          if (ok) {
            this.archiveInnerMediaUrl = fileUrl
            this.archiveInnerLoading = false
            this.archiveInnerLoadError = ''
            return
          }
          if (!this.archiveWarmupTriggered) {
            this.archiveWarmupTriggered = true
            triggerArchiveWarmup(
              this.archiveInner.kkBase,
              this.archiveInner.innerPath,
              this.archiveInner.fileKey
            ).finally(() => {
              if (this.archiveInnerLoading && this.archiveInner) {
                this.resolveArchivePdfAfterWarmup(attempt + 1)
              }
            })
            return
          }
          if (attempt < 45) {
            this.resolveArchivePdfAfterWarmup(attempt + 1)
            return
          }
          this.archiveInnerLoading = false
          this.archiveInnerLoadError = '\u6587\u6863\u8f6c\u6362\u8d85\u65f6\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u6216\u4e0b\u8f7d\u539f\u6587\u4ef6'
        })
      }, delay)
    },
    probeArchivePdf(fetchUrl) {
      if (!fetchUrl) return Promise.resolve(false)
      return fetch(fetchUrl, { credentials: 'include', headers: { Range: 'bytes=0-4' } })
        .then(res => {
          if (!res.ok && res.status !== 206) return false
          const type = String(res.headers.get('content-type') || '').toLowerCase()
          if (type.includes('json') || type.includes('html')) return false
          return res.arrayBuffer().then(buf => {
            if (!buf || buf.byteLength < 4) return false
            const sig = String.fromCharCode(...new Uint8Array(buf.slice(0, 4)))
            return sig === '%PDF'
          })
        })
        .catch(() => false)
    },
    exitArchiveInner() {
      this.clearWarmupTimer()
      const archiveUrl = (this.archiveInner && this.archiveInner.archiveUrl) || this.previewUrl
      this.archiveInner = null
      this.archiveInnerMode = ''
      this.archiveInnerMediaUrl = ''
      this.archiveInnerLoading = false
      this.archiveInnerLoadError = ''
      this.archiveWarmupTriggered = false
      this.activeKkfileUrl = archiveUrl || this.previewUrl
    },
    clearWarmupTimer() {
      if (this.warmupTimer) {
        window.clearTimeout(this.warmupTimer)
        this.warmupTimer = null
      }
    },
    downloadFile() {
      this.$emit('download')
    },
    toggleFullscreen() {
      const el = this.$refs.previewRoot
      if (screenfull.isEnabled && el) {
        screenfull.toggle(el)
      }
    },
    onFullscreenChange() {
      this.isFullscreen = screenfull.isEnabled && screenfull.element === this.$refs.previewRoot
    },
    onContextMenu(e) {
      if (!this.allowDownload) {
        e.preventDefault()
      }
    }
  }
}
</script>

<style scoped lang="scss">
.library-preview {
  position: relative;
  background: #e8ebf0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.library-preview:not(.is-fullscreen) {
  height: auto;
  overflow: visible;
}

.preview-body {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.archive-inner-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.archive-inner-back {
  border: 0;
  border-radius: 8px;
  padding: 8px 12px;
  background: #0f172a;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.archive-inner-back:hover {
  background: #0f766e;
}

.archive-inner-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.library-preview.is-fullscreen {
  flex: 1;
  min-height: 0;
  height: 100%;
  overflow: hidden;

  .preview-body {
    flex: 1;
    min-height: 0;
  }
}

.preview-converting {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  color: #64748b;
  gap: 12px;

  .converting-ring {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: rgba(15, 118, 110, 0.08);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  i { font-size: 28px; color: #0f766e; }
  p { margin: 0; font-size: 14px; font-weight: 600; color: #334155; }
}

.converting-sub {
  font-size: 13px;
  color: #94a3b8;
}

.library-preview.is-fullscreen {
  border-radius: 0;
  min-height: 100vh;
  background: #dfe3ea;

  .preview-body,
  .pdf-preview,
  .pdf-scroll-wrap {
    max-height: none !important;
    min-height: calc(100vh - 44px);
  }

  .pdf-scroll-wrap {
    height: calc(100vh - 44px);
  }
}

.preview-protected .preview-body {
  user-select: none;
}

.preview-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  color: #64748b;
  gap: 12px;

  i { font-size: 48px; color: #94a3b8; }
  p { margin: 0; font-size: 14px; max-width: 420px; text-align: center; }
}

.preview-hint {
  margin-top: 8px !important;
  font-size: 12px !important;
  color: #94a3b8 !important;
  line-height: 1.5;
}

.preview-watermark {
  pointer-events: none;
  position: absolute;
  inset: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(4, 1fr);
  gap: 24px;
  padding: 24px;
  overflow: hidden;
  opacity: 0.08;
  font-size: 13px;
  font-weight: 600;
  color: #0f766e;
  transform: rotate(-24deg);

  span {
    display: flex;
    align-items: center;
    justify-content: center;
    white-space: nowrap;
  }
}

.has-watermark.preview-protected .preview-watermark {
  opacity: 0.1;
}
</style>
