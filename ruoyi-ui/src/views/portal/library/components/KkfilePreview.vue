<template>
  <div class="kkfile-preview">
    <iframe v-if="previewUrl" :src="previewUrl" title="Document preview" allowfullscreen />
    <div v-else class="kkfile-empty">{{ emptyLabel }}</div>
  </div>
</template>

<script>
export default {
  name: 'KkfilePreview',
  props: {
    url: { type: String, default: '' }
  },
  computed: {
    previewUrl() {
      return this.url || ''
    },
    emptyLabel() {
      return '\u9884\u89c8\u5730\u5740\u4e0d\u53ef\u7528'
    }
  }
}
</script>

<style scoped lang="scss">
.kkfile-preview {
  width: 100%;
  min-height: 70vh;

  iframe {
    width: 100%;
    min-height: 70vh;
    border: 0;
    background: #fff;
  }
}

.kkfile-empty {
  padding: 48px;
  text-align: center;
  color: #94a3b8;
}
</style>
