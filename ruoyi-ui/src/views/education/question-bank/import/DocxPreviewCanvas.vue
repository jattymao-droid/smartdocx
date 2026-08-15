<template>
  <div class="docx-preview-canvas">
    <div class="canvas-toolbar">
      <el-button size="mini" icon="el-icon-full-screen" @click="addBox">新建选框</el-button>
      <el-button size="mini" type="primary" plain icon="el-icon-crop" @click="captureActiveBox">框选添加题目</el-button>
      <el-button size="mini" @click="clearBoxes">清除选框</el-button>
      <span class="canvas-tip">拖拽/缩放选框覆盖段落后点「框选添加题目」</span>
    </div>
    <div ref="canvasInner" class="canvas-inner" :class="{ 'is-expanded': expanded }" @click="onBlockClick">
      <div ref="previewContent" class="preview-content"></div>
      <div
        v-for="box in boxes"
        :key="box.id"
        class="docx-selection-box"
        :class="{ active: box.id === activeBoxId }"
        :data-box-id="box.id"
        :style="{ width: box.w + 'px', height: box.h + 'px' }"
        @mousedown.stop="activeBoxId = box.id"
      />
    </div>
  </div>
</template>

<script>
import interact from 'interactjs'
import { resolveImageUrl } from '@/utils/paperExportCommon'

export default {
  name: 'DocxPreviewCanvas',
  props: {
    previewHtml: { type: String, default: '' },
    usedBlockIds: { type: Array, default: () => [] },
    markedBlockIds: { type: Array, default: () => [] },
    excludedBlockIds: { type: Array, default: () => [] },
    interactiveMarks: { type: Boolean, default: false },
    expanded: { type: Boolean, default: false }
  },
  data() {
    return {
      boxes: [],
      boxSeq: 0,
      activeBoxId: null
    }
  },
  watch: {
    previewHtml() {
      this.$nextTick(() => this.renderPreview())
    },
    usedBlockIds: {
      handler() { this.applyBlockHighlights() },
      deep: true
    },
    excludedBlockIds: {
      handler() { this.applyBlockHighlights() },
      deep: true
    },
    markedBlockIds: {
      handler() { this.applyBlockHighlights() },
      deep: true
    },
    interactiveMarks() { this.applyBlockHighlights() }
  },
  mounted() {
    this.renderPreview()
  },
  beforeDestroy() {
    this.destroyInteract()
  },
  methods: {
    renderPreview() {
      const el = this.$refs.previewContent
      if (!el) return
      el.innerHTML = this.previewHtml || ''
      el.querySelectorAll('img').forEach(img => {
        const src = img.getAttribute('src')
        if (src) img.setAttribute('src', resolveImageUrl(src))
      })
      this.applyBlockHighlights()
    },
    usedSet() {
      return new Set(this.usedBlockIds || [])
    },
    addBox() {
      const id = ++this.boxSeq
      this.boxes.push({ id, w: 280, h: 140 })
      this.activeBoxId = id
      this.$nextTick(() => this.setupInteract(id))
    },
    clearBoxes() {
      this.destroyInteract()
      this.boxes = []
      this.activeBoxId = null
    },
    setupInteract(boxId) {
      const selector = `.docx-selection-box[data-box-id="${boxId}"]`
      interact(selector)
        .draggable({
          listeners: {
            move: event => {
              const target = event.target
              const x = (parseFloat(target.getAttribute('data-x')) || 0) + event.dx
              const y = (parseFloat(target.getAttribute('data-y')) || 0) + event.dy
              target.style.transform = `translate(${x}px, ${y}px)`
              target.setAttribute('data-x', x)
              target.setAttribute('data-y', y)
            }
          },
          modifiers: [
            interact.modifiers.restrictRect({
              restriction: this.$refs.canvasInner,
              endOnly: true
            })
          ]
        })
        .resizable({
          edges: { left: true, right: true, bottom: true, top: true },
          listeners: {
            move: event => {
              const target = event.target
              let x = parseFloat(target.getAttribute('data-x')) || 0
              let y = parseFloat(target.getAttribute('data-y')) || 0
              target.style.width = `${event.rect.width}px`
              target.style.height = `${event.rect.height}px`
              x += event.deltaRect.left
              y += event.deltaRect.top
              target.style.transform = `translate(${x}px, ${y}px)`
              target.setAttribute('data-x', x)
              target.setAttribute('data-y', y)
            }
          },
          modifiers: [
            interact.modifiers.restrictSize({ min: { width: 60, height: 40 } }),
            interact.modifiers.restrictEdges({
              outer: this.$refs.canvasInner,
              endOnly: true
            })
          ]
        })
    },
    destroyInteract() {
      interact('.docx-selection-box').unset()
    },
    relativeRect(node) {
      const canvas = this.$refs.canvasInner
      if (!canvas || !node) return null
      const canvasRect = canvas.getBoundingClientRect()
      const rect = node.getBoundingClientRect()
      return {
        left: rect.left - canvasRect.left,
        top: rect.top - canvasRect.top,
        right: rect.right - canvasRect.left,
        bottom: rect.bottom - canvasRect.top
      }
    },
    rectsIntersect(a, b) {
      return !(a.right < b.left || a.left > b.right || a.bottom < b.top || a.top > b.bottom)
    },
    collectBlockIdsInActiveBox() {
      const box = this.boxes.find(item => item.id === this.activeBoxId)
      if (!box) return []
      const boxEl = this.$el.querySelector(`.docx-selection-box[data-box-id="${box.id}"]`)
      if (!boxEl) return []
      const sel = this.relativeRect(boxEl)
      const used = this.usedSet()
      const ids = []
      const root = this.$refs.previewContent
      if (!root) return ids
      root.querySelectorAll('.qb-docx-block').forEach(node => {
        const blockRect = this.relativeRect(node)
        if (blockRect && sel && this.rectsIntersect(sel, blockRect)) {
          const id = parseInt(node.getAttribute('data-block-id'), 10)
          if (!Number.isNaN(id) && !used.has(id)) {
            ids.push(id)
          }
        }
      })
      return ids.sort((a, b) => a - b)
    },
    captureActiveBox() {
      if (!this.activeBoxId) {
        this.$modal.msgWarning('请先新建选框')
        return
      }
      const ids = this.collectBlockIdsInActiveBox()
      if (!ids.length) {
        this.$modal.msgWarning('选框内没有可用段落')
        return
      }
      this.$emit('capture', ids)
    },
    applyBlockHighlights() {
      const root = this.$refs.previewContent
      if (!root) return
      const used = this.usedSet()
      const marked = new Set(this.markedBlockIds || [])
      const excluded = new Set(this.excludedBlockIds || [])
      root.querySelectorAll('.qb-docx-block').forEach(node => {
        const id = parseInt(node.getAttribute('data-block-id'), 10)
        node.classList.toggle('is-used', used.has(id))
        node.classList.toggle('is-marked', marked.has(id))
        node.classList.toggle('is-excluded', excluded.has(id))
      })
      if (root.parentElement) {
        root.classList.toggle('interactive', this.interactiveMarks)
      }
    },
    onBlockClick(event) {
      if (!this.interactiveMarks) return
      const block = event.target.closest('.qb-docx-block')
      if (!block) return
      const id = parseInt(block.getAttribute('data-block-id'), 10)
      if (Number.isNaN(id)) return
      this.$emit('toggle-block', id)
    }
  }
}
</script>

<style scoped lang="scss">
.docx-preview-canvas {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.canvas-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.canvas-tip {
  color: #909399;
  font-size: 12px;
}
.canvas-inner {
  position: relative;
  min-height: 480px;
  max-height: 560px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
}
.canvas-inner.is-expanded {
  min-height: 520px;
  max-height: calc(72vh - 200px);
}
.preview-content {
  position: relative;
  z-index: 1;
  padding: 12px;
}
.preview-content ::v-deep .qb-docx-preview {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
}
.preview-content ::v-deep .qb-docx-block {
  display: flex;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 8px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  background: #fafafa;
}
.preview-content ::v-deep .qb-docx-block.is-marked {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.35);
}
.preview-content ::v-deep .qb-docx-block.is-excluded {
  border-color: #f56c6c;
  background: #fef0f0;
  opacity: 0.75;
}
.preview-content ::v-deep .qb-docx-block {
  cursor: default;
}
.preview-content.interactive ::v-deep .qb-docx-block {
  cursor: pointer;
}
.preview-content ::v-deep .qb-docx-block.is-used {
  opacity: 0.45;
}
.preview-content ::v-deep .qb-block-no {
  width: 24px;
  color: #909399;
  flex-shrink: 0;
}
.preview-content ::v-deep .qb-block-text {
  flex: 1;
  word-break: break-word;
}
.preview-content ::v-deep .qb-block-body {
  flex: 1;
  min-width: 0;
}
.preview-content ::v-deep .qb-block-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
}
.preview-content ::v-deep .qb-block-image {
  max-width: 100%;
  max-height: 220px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
}
.docx-selection-box {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 3;
  border: 2px dashed #e6a23c;
  background: rgba(230, 162, 60, 0.12);
  box-sizing: border-box;
  touch-action: none;
  cursor: move;
}
.docx-selection-box.active {
  border: 2px dashed #409eff;
  background: rgba(64, 158, 255, 0.12);
}
</style>
