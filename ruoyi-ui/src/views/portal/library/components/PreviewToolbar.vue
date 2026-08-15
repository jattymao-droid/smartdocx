<template>
  <div class="preview-toolbar">
    <el-button-group>
      <el-button size="mini" icon="el-icon-full-screen" @click="$emit('fullscreen')">{{ fullscreenLabel }}</el-button>
      <el-button v-if="allowDownload" size="mini" icon="el-icon-download" @click="$emit('download')">{{ effectiveDownloadLabel }}</el-button>
    </el-button-group>
  </div>
</template>

<script>
export default {
  name: 'PreviewToolbar',
  props: {
    allowDownload: { type: Boolean, default: false },
    downloadLabel: { type: String, default: '' }
  },
  computed: {
    fullscreenLabel() {
      return '\u5168\u5c4f'
    },
    effectiveDownloadLabel() {
      return this.downloadLabel || '\u4e0b\u8f7d'
    }
  }
}
</script>

<style scoped lang="scss">
.preview-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
}
</style>
