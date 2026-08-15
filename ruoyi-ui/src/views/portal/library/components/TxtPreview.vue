<template>
  <div v-loading="loading" class="txt-preview">
    <pre v-if="text">{{ text }}</pre>
    <div v-else-if="!loading" class="txt-empty">{{ emptyLabel }}</div>
  </div>
</template>

<script>
import { resolvePortalMediaUrl } from '@/utils/portalBanner'

export default {
  name: 'TxtPreview',
  props: {
    url: { type: String, default: '' }
  },
  computed: {
    emptyLabel() {
      return '\u65e0\u6cd5\u8bfb\u53d6\u6587\u672c\u5185\u5bb9'
    }
  },
  data() {
    return {
      loading: false,
      text: ''
    }
  },
  watch: {
    url: {
      immediate: true,
      handler() { this.loadText() }
    }
  },
  methods: {
    loadText() {
      const src = resolvePortalMediaUrl(this.url)
      if (!src) {
        this.text = ''
        return
      }
      this.loading = true
      fetch(src)
        .then(res => res.arrayBuffer())
        .then(buf => {
          this.text = this.decodeBuffer(buf)
        })
        .catch(() => {
          this.text = ''
        })
        .finally(() => {
          this.loading = false
        })
    },
    decodeBuffer(buf) {
      const utf8 = new TextDecoder('utf-8', { fatal: false }).decode(buf)
      if (!utf8.includes('\uFFFD')) return utf8
      try {
        return new TextDecoder('gbk').decode(buf)
      } catch (e) {
        return utf8
      }
    }
  }
}
</script>

<style scoped lang="scss">
.txt-preview {
  min-height: 360px;
  padding: 24px 32px;
  background: #fff;
  border-top: 1px solid #eef2f7;

  pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
    font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
    font-size: 14px;
    line-height: 1.75;
    color: #334155;
  }
}

.txt-empty {
  padding: 48px;
  text-align: center;
  color: #94a3b8;
}
</style>
