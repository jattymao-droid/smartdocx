<template>
  <div class="qb-formula-text" :class="{ 'is-block': block, 'is-html': isHtmlContent }">
    <div
      v-if="isHtmlContent"
      class="qb-question-html"
      v-html="questionHtml"
    />
    <template v-else-if="contentParts.length > 1 || hasInlineImages">
      <template v-for="(part, i) in contentParts">
        <span
          v-if="part.type === 'text' && part.content"
          :key="'t-' + i"
          class="qb-formula-text-segment"
          v-html="renderSegment(part.content)"
        />
        <img
          v-else-if="part.type === 'image' && part.url"
          :key="'i-' + i"
          :src="resolveImageUrl(part.url)"
          class="qb-inline-image"
          alt=""
        >
      </template>
    </template>
    <div v-else v-html="html" />
  </div>
</template>

<script>
import { renderFormulaText, splitContentImageParts } from '@/utils/questionFormula'
import { isQuestionHtml, resolveQuestionHtml, resolveMediaProxyUrl } from '@/utils/questionContent'

export default {
  name: 'QbFormulaText',
  props: {
    text: {
      type: [String, Number],
      default: ''
    },
    images: {
      type: Array,
      default: () => []
    },
    block: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    isHtmlContent() {
      return isQuestionHtml(this.text)
    },
    questionHtml() {
      return resolveQuestionHtml(this.text)
    },
    contentParts() {
      return splitContentImageParts(this.text, this.images)
    },
    hasInlineImages() {
      return this.contentParts.some(part => part.type === 'image')
    },
    html() {
      return renderFormulaText(this.text)
    }
  },
  methods: {
    renderSegment(text) {
      return renderFormulaText(text)
    },
    resolveImageUrl(url) {
      return resolveMediaProxyUrl(url)
    }
  }
}
</script>

<style lang="scss">
.qb-formula-text {
  display: inline;
  line-height: 1.75;
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: normal;
  vertical-align: baseline;

  &.is-block {
    display: block;
    max-width: 100%;
  }

  &.is-html {
    display: block;
  }

  .qb-formula-text-segment {
    display: inline;
  }

  .qb-inline-image {
    display: inline-block;
    vertical-align: middle;
    max-width: min(100%, 320px);
    max-height: 220px;
    margin: 6px 8px 6px 0;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    background: #fafafa;
  }

  .qb-question-html {
    display: block;
    max-width: 100%;
    line-height: 1.75;
    color: #303133;
    word-break: break-word;

    table {
      border-collapse: collapse;
      margin: 8px 0;
      max-width: 100%;
    }

    td,
    th {
      border: 1px solid #333;
      padding: 4px 8px;
      text-align: center;
      vertical-align: middle;
    }

    img {
      display: inline-block;
      vertical-align: middle;
      max-width: 100%;
      height: auto;
      margin: 4px 6px 4px 0;
    }

    p {
      margin: 6px 0;
    }

    .qb-blank {
      display: inline-block;
      min-width: 2em;
      border-bottom: 1px solid #303133;
    }

    .qb-options {
      list-style: none;
      margin: 10px 0 0;
      padding: 0;
    }

    .qb-option-item {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      margin: 0 0 8px;
      padding: 8px 12px;
      border-radius: 10px;
      background: #f8fafc;
      border: 1px solid #eef2f6;
      font-size: 14px;
      line-height: 1.75;
      color: #606266;
    }

    .qb-option-label {
      flex-shrink: 0;
      font-weight: 600;
      line-height: 1.75;
    }

    .qb-option-text {
      flex: 1;
      text-align: justify;
      line-height: 1.75;
    }
  }

  .katex {
    font-size: 1.05em;
  }

  &.is-block .katex {
    max-width: 100%;
  }

  .katex-display {
    display: block;
    margin: 0.45em 0;
    text-align: center;
    max-width: 100%;
    overflow-x: auto;
    overflow-y: hidden;
  }

  .katex-display > .katex {
    display: inline-block;
    text-align: center;
  }
}
</style>
