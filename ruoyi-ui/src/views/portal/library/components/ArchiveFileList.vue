<template>
  <div class="archive-file-list">
    <div v-if="loading" class="archive-state">
      <i class="el-icon-loading" />
      <span>{{ loadingLabel }}</span>
    </div>
    <div v-else-if="error" class="archive-state archive-state--error">
      <p>{{ error }}</p>
      <button type="button" class="archive-retry" @click="loadManifest(previewUrl)">{{ retryLabel }}</button>
    </div>
    <template v-else>
      <div class="archive-card">
        <div class="archive-header">
          <div class="archive-icon">&#128230;</div>
          <div>
            <div class="archive-title">{{ archiveTitle }}</div>
            <div class="archive-subtitle">{{ subtitleLabel }}</div>
          </div>
        </div>
        <div class="archive-toolbar">
          <span class="archive-count">{{ fileCountLabel }}</span>
          <span class="archive-hint">{{ clickHint }}</span>
        </div>
        <div class="archive-files">
          <button
            v-for="file in files"
            :key="file.id"
            type="button"
            class="archive-file"
            @click="openFile(file)"
          >
            <span class="file-badge" :class="'file-badge--' + badgeClass(file.ext)">{{ badgeText(file.ext) }}</span>
            <span class="file-name">{{ file.name }}</span>
          </button>
        </div>
        <div class="archive-footnote">{{ footnote }}</div>
      </div>
    </template>
  </div>
</template>

<script>
import { archiveBadgeClass, fetchArchiveManifest, flattenArchiveNodes } from '@/utils/libraryArchivePreview'

export default {
  name: 'ArchiveFileList',
  props: {
    previewUrl: { type: String, default: '' }
  },
  data() {
    return {
      loading: false,
      error: '',
      manifest: null,
      files: []
    }
  },
  computed: {
    loadingLabel() {
      return '\u538b\u7f29\u5305\u89e3\u6790\u4e2d\u2026'
    },
    subtitleLabel() {
      return '\u70b9\u51fb\u6587\u4ef6\u540d\u53ef\u5728\u7ebf\u9884\u89c8'
    },
    clickHint() {
      return '\u6587\u4ef6\u5217\u8868'
    },
    footnote() {
      return '\u5df2\u81ea\u52a8\u9690\u85cf\u9884\u89c8\u65f6\u751f\u6210\u7684 PDF\u3001JPG \u53ca\u5185\u5d4c\u56fe\u7247'
    },
    retryLabel() {
      return '\u91cd\u8bd5'
    },
    archiveTitle() {
      const key = (this.manifest && this.manifest.fileKey) || ''
      return key.replace(/_\d{14}A\d{3}\.zip_?$/i, '').replace(/\.(zip|rar|7z)_?$/i, '') || '\u538b\u7f29\u5305'
    },
    fileCountLabel() {
      return this.files.length + ' \u4e2a\u6587\u4ef6'
    }
  },
  watch: {
    previewUrl: {
      immediate: true,
      handler(val) {
        this.loadManifest(val)
      }
    }
  },
  methods: {
    loadManifest(previewUrl) {
      if (!previewUrl) {
        this.error = '\u9884\u89c8\u5730\u5740\u4e0d\u53ef\u7528'
        return
      }
      this.loading = true
      this.error = ''
      this.manifest = null
      this.files = []
      fetchArchiveManifest(previewUrl)
        .then(data => {
          this.manifest = data
          this.files = flattenArchiveNodes(data.nodes || [])
        })
        .catch(err => {
          this.error = (err && err.message) || '\u538b\u7f29\u5305\u5217\u8868\u52a0\u8f7d\u5931\u8d25'
        })
        .finally(() => {
          this.loading = false
        })
    },
    badgeClass(ext) {
      return archiveBadgeClass(ext)
    },
    badgeText(ext) {
      const key = String(ext || '').toLowerCase()
      if (key) return key.toUpperCase()
      return 'FILE'
    },
    openFile(file) {
      if (!file || !this.manifest) return
      this.$emit('open-inner', {
        type: 'LIBRARY_ARCHIVE_INNER',
        ext: file.ext || '',
        name: file.name,
        innerPath: file.id,
        archiveUrl: this.manifest.archiveUrl || this.previewUrl,
        kkBase: this.manifest.kkBase,
        fileKey: this.manifest.fileKey
      })
    }
  }
}
</script>

<style scoped lang="scss">
.archive-file-list {
  width: 100%;
  min-height: 360px;
}

.archive-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 360px;
  color: #64748b;
  font-size: 14px;
}

.archive-state--error p {
  margin: 0 0 12px;
  color: #ef4444;
}

.archive-retry {
  border: 0;
  border-radius: 8px;
  padding: 8px 16px;
  background: #0f766e;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.archive-retry:hover {
  background: #0f172a;
}

.archive-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
}

.archive-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 22px;
  border-bottom: 1px solid #eef2f7;
  background: linear-gradient(180deg, #fafbfd 0%, #fff 100%);
}

.archive-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.archive-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.archive-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}

.archive-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 22px;
  background: #f8fafc;
  border-bottom: 1px solid #eef2f7;
  font-size: 13px;
  color: #64748b;
}

.archive-count {
  font-weight: 600;
  color: #334155;
}

.archive-files {
  padding: 10px 14px 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.archive-file {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  border: 0;
  border-radius: 8px;
  padding: 10px 12px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
}

.archive-file:hover {
  background: #f1f5f9;
}

.file-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.file-badge--doc { background: #2563eb; }
.file-badge--xls { background: #059669; }
.file-badge--ppt { background: #ea580c; }
.file-badge--pdf { background: #dc2626; }
.file-badge--img { background: #7c3aed; }
.file-badge--archive { background: #475569; }
.file-badge--media { background: #db2777; }
.file-badge--file { background: #64748b; }

.file-name {
  font-size: 14px;
  color: #1e293b;
  line-height: 1.4;
  word-break: break-all;
}

.archive-footnote {
  padding: 10px 22px 16px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
