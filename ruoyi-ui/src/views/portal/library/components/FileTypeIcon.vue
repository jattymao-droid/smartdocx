<template>
  <span class="file-type-icon" :class="typeInfo.cls" :title="typeInfo.label" aria-hidden="true">
    <span v-if="typeInfo.letter" class="file-type-letter">{{ typeInfo.letter }}</span>
    <i v-else :class="typeInfo.icon" />
  </span>
</template>

<script>
const TYPE_MAP = {
  ppt: { cls: 'is-ppt', letter: 'P', label: 'PPT' },
  pptx: { cls: 'is-ppt', letter: 'P', label: 'PPT' },
  doc: { cls: 'is-word', letter: 'W', label: 'Word' },
  docx: { cls: 'is-word', letter: 'W', label: 'Word' },
  xls: { cls: 'is-excel', icon: 'el-icon-s-grid', label: 'Excel' },
  xlsx: { cls: 'is-excel', icon: 'el-icon-s-grid', label: 'Excel' },
  pdf: { cls: 'is-pdf', icon: 'el-icon-document', label: 'PDF' },
  txt: { cls: 'is-txt', icon: 'el-icon-tickets', label: 'TXT' }
}

export default {
  name: 'FileTypeIcon',
  props: {
    ext: { type: String, default: '' }
  },
  computed: {
    typeInfo() {
      const key = String(this.ext || '').toLowerCase()
      return TYPE_MAP[key] || { cls: 'is-file', icon: 'el-icon-document', label: key || 'FILE' }
    }
  }
}
</script>

<style scoped lang="scss">
.file-type-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 5px;
  font-size: 11px;
  line-height: 1;

  i {
    font-size: 11px;
    font-weight: 700;
  }

  .file-type-letter {
    font-size: 11px;
    font-weight: 800;
    line-height: 1;
    font-family: 'Segoe UI', system-ui, sans-serif;
  }

  &.is-ppt {
    color: #c2410c;
    background: #fff7ed;
    border: 1px solid rgba(234, 88, 12, 0.2);
  }

  &.is-word {
    color: #1d4ed8;
    background: #eff6ff;
    border: 1px solid rgba(37, 99, 235, 0.2);
  }

  &.is-excel {
    color: #15803d;
    background: #f0fdf4;
    border: 1px solid rgba(22, 163, 74, 0.2);
  }

  &.is-pdf {
    color: #b91c1c;
    background: #fef2f2;
    border: 1px solid rgba(220, 38, 38, 0.2);
  }

  &.is-txt,
  &.is-file {
    color: #64748b;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
  }
}
</style>
