<template>
  <div class="library-doc-cover">
    <div v-if="showArchiveDefault" class="library-doc-cover__archive" aria-hidden="true">
      <span class="library-doc-cover__archive-icon">&#128230;</span>
      <span class="library-doc-cover__archive-text">{{ archiveLabel }}</span>
    </div>
    <img
      v-else-if="coverSrc"
      class="library-doc-cover__img"
      :src="coverSrc"
      alt=""
      loading="lazy"
      decoding="async"
      @error="onImgError"
    >
    <div v-else class="library-doc-cover__fallback" :class="extCoverClass">
      <span>{{ extLabel }}</span>
    </div>
  </div>
</template>

<script>
import {
  formatFileExtLabel,
  getFileExtCoverClass,
  isArchiveExt,
  resolveLibraryDocumentCover,
  shouldUseArchiveDefaultCover
} from '@/utils/libraryFileExt'
import { resolvePortalMediaUrl } from '@/utils/portalBanner'

export default {
  name: 'LibraryDocCover',
  props: {
    coverUrl: { type: String, default: '' },
    fileExt: { type: String, default: '' }
  },
  data() {
    return {
      imgFailed: false
    }
  },
  computed: {
    showArchiveDefault() {
      if (shouldUseArchiveDefaultCover(this.coverUrl, this.fileExt)) {
        return true
      }
      return this.imgFailed && isArchiveExt(this.fileExt)
    },
    coverSrc() {
      if (shouldUseArchiveDefaultCover(this.coverUrl, this.fileExt) || this.imgFailed) {
        return ''
      }
      return resolveLibraryDocumentCover(this.coverUrl, this.fileExt, resolvePortalMediaUrl)
    },
    extCoverClass() {
      return getFileExtCoverClass(this.fileExt)
    },
    extLabel() {
      return formatFileExtLabel(this.fileExt)
    },
    archiveLabel() {
      return '\u538b\u7f29\u5305'
    }
  },
  watch: {
    coverUrl() {
      this.imgFailed = false
    },
    fileExt() {
      this.imgFailed = false
    }
  },
  methods: {
    onImgError() {
      this.imgFailed = true
    }
  }
}
</script>

<style scoped lang="scss">
.library-doc-cover {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #f5f5f5;
}

.library-doc-cover__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.library-doc-cover__archive {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(145deg, #ede9fe 0%, #ddd6fe 42%, #a78bfa 100%);
  color: #5b21b6;
}

.library-doc-cover__archive-icon {
  font-size: 32px;
  line-height: 1;
  filter: drop-shadow(0 2px 6px rgba(91, 33, 182, 0.18));
}

.library-doc-cover__archive-text {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.library-doc-cover__fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;

  &.ext-pdf { background: linear-gradient(135deg, #ef5350, #c62828); }
  &.ext-docx, &.ext-doc { background: linear-gradient(135deg, #42a5f5, #1565c0); }
  &.ext-pptx, &.ext-ppt { background: linear-gradient(135deg, #ff7043, #d84315); }
  &.ext-xlsx, &.ext-xls { background: linear-gradient(135deg, #66bb6a, #2e7d32); }
  &.ext-txt { background: linear-gradient(135deg, #90a4ae, #546e7a); }
  &.ext-zip, &.ext-rar, &.ext-7z { background: linear-gradient(135deg, #a78bfa, #6366f1); }
  &.ext-file { background: linear-gradient(135deg, #78909c, #455a64); }
}
</style>
