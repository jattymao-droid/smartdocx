<template>
  <span class="ext-badge" :class="[badgeClass, sizeClass]" :title="label">{{ label }}</span>
</template>

<script>
import { formatFileExtLabel, getFileExtBadgeClass } from '@/utils/libraryFileExt'

export default {
  name: 'FileExtBadge',
  props: {
    ext: { type: String, default: '' },
    size: {
      type: String,
      default: 'sm',
      validator: v => ['xs', 'sm', 'md'].includes(v)
    }
  },
  computed: {
    label() {
      return formatFileExtLabel(this.ext)
    },
    badgeClass() {
      return getFileExtBadgeClass(this.ext)
    },
    sizeClass() {
      return 'ext-badge--' + this.size
    }
  }
}
</script>

<style scoped lang="scss">
.ext-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 700;
  color: #fff;
  border-radius: 4px;
  line-height: 1.2;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.ext-badge--xs {
  padding: 0 4px;
  font-size: 9px;
  border-radius: 3px;
}

.ext-badge--sm {
  padding: 1px 5px;
  font-size: 10px;
}

.ext-badge--md {
  padding: 2px 7px;
  font-size: 11px;
  border-radius: 4px;
}

.ext-badge--pdf {
  background: linear-gradient(135deg, #ef5350, #c62828);
}

.ext-badge--word {
  background: linear-gradient(135deg, #42a5f5, #1565c0);
}

.ext-badge--ppt {
  background: linear-gradient(135deg, #ff7043, #d84315);
}

.ext-badge--excel {
  background: linear-gradient(135deg, #66bb6a, #2e7d32);
}

.ext-badge--txt {
  background: linear-gradient(135deg, #90a4ae, #546e7a);
}

.ext-badge--archive {
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
}

.ext-badge--file {
  background: linear-gradient(135deg, #78909c, #455a64);
}
</style>
