<template>
  <div v-loading="loading" class="docx-preview-wrap">
    <div ref="host" class="docx-host" />
    <div v-if="!loading && failed" class="docx-empty">{{ failLabel }}</div>
  </div>
</template>

<script>
import { renderAsync } from 'docx-preview'
import { resolvePortalMediaUrl } from '@/utils/portalBanner'

export default {
  name: 'DocxPreview',
  props: {
    url: { type: String, default: '' }
  },
  computed: {
    failLabel() {
      return '\u6587\u6863\u9884\u89c8\u5931\u8d25'
    }
  },
  data() {
    return {
      loading: false,
      failed: false
    }
  },
  watch: {
    url: {
      immediate: true,
      handler() { this.renderDoc() }
    }
  },
  methods: {
    renderDoc() {
      const host = this.$refs.host
      if (!host) return
      host.innerHTML = ''
      const src = resolvePortalMediaUrl(this.url)
      if (!src) {
        this.failed = true
        return
      }
      this.loading = true
      this.failed = false
      fetch(src)
        .then(res => {
          if (!res.ok) throw new Error('fetch failed')
          return res.blob()
        })
        .then(blob => renderAsync(blob, host, null, {
          className: 'library-docx-page',
          inWrapper: true,
          ignoreWidth: false,
          ignoreHeight: false
        }))
        .catch(() => {
          this.failed = true
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style scoped lang="scss">
.docx-preview-wrap {
  min-height: 360px;
  max-height: 75vh;
  overflow: auto;
  padding: 16px;
  background: #fff;
}

.docx-host ::v-deep .library-docx-page {
  padding: 12px;
}

.docx-empty {
  padding: 48px;
  text-align: center;
  color: #94a3b8;
}
</style>
