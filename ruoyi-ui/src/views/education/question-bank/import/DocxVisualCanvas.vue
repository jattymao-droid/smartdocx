<template>
  <div class="docx-visual-canvas">
    <div class="canvas-toolbar">
      <el-button size="mini" icon="el-icon-full-screen" @click="addBox">新建选框</el-button>
      <el-button
        size="mini"
        :type="smartSelectMode ? 'primary' : 'default'"
        icon="el-icon-magic-stick"
        @click="smartSelectMode = !smartSelectMode"
      >智能框选</el-button>
      <el-button size="mini" type="primary" plain icon="el-icon-crop" :loading="capturing" @click="captureActiveBox">框选添加到队列</el-button>
      <el-button size="mini" @click="clearBoxes">清除选框</el-button>
      <span class="canvas-tip">{{ canvasTipText }}</span>
    </div>
    <div v-if="loadError" class="load-error">
      <el-alert :title="loadError" type="error" show-icon :closable="false" />
    </div>
    <div ref="canvasInner" class="canvas-inner" :class="{ 'is-expanded': expanded, 'smart-select-mode': smartSelectMode }" v-loading="loading">
      <div ref="docxHost" class="docx-host" @click="onDocHostClick"></div>
      <div
        v-for="box in boxes"
        :key="box.id"
        class="docx-selection-box"
        :class="{ active: box.id === activeBoxId }"
        :data-box-id="box.id"
        :style="{ width: box.w + 'px', height: box.h + 'px' }"
        @mousedown.stop="onBoxMouseDown(box.id)"
      />
      <div
        v-if="enableBoxCatalog && activeBoxId && catalogPanelStyle"
        class="box-catalog-panel"
        :style="catalogPanelStyle"
        @mousedown.stop
        @click.stop
      >
        <div class="box-catalog-title">题目分类</div>
        <question-catalog-picker
          :key="'catalog-' + activeBoxId"
          :value="activeBoxMeta"
          :subject-id="defaultSubjectId"
          :subject-options="subjectOptions"
          :subject-locked="subjectLocked"
          compact
          @input="onActiveBoxMetaInput"
        />
        <div class="box-catalog-actions">
          <el-button size="mini" type="primary" :loading="capturing" @click="captureActiveBox">添加题目</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import interact from 'interactjs'
import { renderAsync } from 'docx-preview'
import { getToken } from '@/utils/auth'
import { uploadImportImage } from '@/api/education/questionImport'
import QuestionCatalogPicker from '@/views/education/question-bank/components/QuestionCatalogPicker'
import { detectQuestionRectAtPoint, detectQuestionRectByText } from '@/utils/docxQuestionFrame'
import {
  buildCaptureContent,
  collectTextLineRows,
  mediaInSelection,
  queryMediaNodes,
  rasterizeSvgToBlob
} from '@/utils/docxCaptureSupport'

function createBoxMeta(defaultSubjectId) {
  return {
    subjectId: defaultSubjectId || undefined,
    versionId: undefined,
    textbookId: undefined,
    chapterPath: [],
    chapterId: undefined,
    chapterText: '',
    knowledgeTags: []
  }
}

export default {
  name: 'DocxVisualCanvas',
  components: { QuestionCatalogPicker },
  props: {
    filePath: { type: String, default: '' },
    localFile: { type: File, default: null },
    expanded: { type: Boolean, default: false },
    enableBoxCatalog: { type: Boolean, default: false },
    smartSelectDefault: { type: Boolean, default: true },
    defaultSubjectId: { type: [Number, String], default: undefined },
    subjectOptions: { type: Array, default: () => [] },
    subjectLocked: { type: Boolean, default: false }
  },
  data() {
    return {
      loading: false,
      capturing: false,
      loadError: '',
      boxes: [],
      boxSeq: 0,
      activeBoxId: null,
      rendered: false,
      renderToken: 0,
      styleContainer: null,
      catalogPanelStyle: null,
      smartSelectMode: true
    }
  },
  computed: {
    canvasTipText() {
      if (this.smartSelectMode) {
        return this.enableBoxCatalog
          ? '点击题目自动框选整题，设置分类后点「添加题目」'
          : '点击题目自动框选整题，可微调选框后点「框选添加到队列」'
      }
      return this.enableBoxCatalog
        ? '拖动选框覆盖题目，在选框右上角设置分类后点「添加题目」'
        : '在原文档上拖动选框，覆盖题干、插图与选项后点「框选添加到队列」'
    },
    activeBoxMeta() {
      const box = this.boxes.find(b => b.id === this.activeBoxId)
      if (!box || !box.meta) {
        return createBoxMeta(this.defaultSubjectId ? Number(this.defaultSubjectId) : undefined)
      }
      return box.meta
    }
  },
  watch: {
    localFile(file) {
      if (file) this.renderFromFile(file)
    },
    filePath(val) {
      if (val && !this.localFile) this.loadFromServer(val)
    },
    activeBoxId() {
      this.updateCatalogPanelPosition()
    },
    defaultSubjectId(val) {
      if (!val) return
      this.boxes.forEach(box => {
        if (box.meta && !box.meta.subjectId) {
          this.$set(box.meta, 'subjectId', Number(val))
        }
      })
    }
  },
  mounted() {
    this.smartSelectMode = this.smartSelectDefault !== false
    this.$nextTick(() => {
      if (this.localFile) {
        this.renderFromFile(this.localFile)
      } else if (this.filePath) {
        this.loadFromServer(this.filePath)
      }
    })
  },
  beforeDestroy() {
    this.destroyInteract()
    this.cleanupStyleContainer()
  },
  methods: {
    onBoxMouseDown(boxId) {
      this.activeBoxId = boxId
      this.updateCatalogPanelPosition()
    },
    onActiveBoxMetaInput(meta) {
      const box = this.boxes.find(b => b.id === this.activeBoxId)
      if (!box) return
      this.$set(box, 'meta', Object.assign(createBoxMeta(this.defaultSubjectId ? Number(this.defaultSubjectId) : undefined), meta || {}))
    },
    updateCatalogPanelPosition() {
      this.$nextTick(() => {
        if (!this.enableBoxCatalog || !this.activeBoxId) {
          this.catalogPanelStyle = null
          return
        }
        const el = this.$el.querySelector(`.docx-selection-box[data-box-id="${this.activeBoxId}"]`)
        const canvas = this.$refs.canvasInner
        const box = this.boxes.find(b => b.id === this.activeBoxId)
        if (!el || !canvas || !box) {
          this.catalogPanelStyle = null
          return
        }
        const x = parseFloat(el.getAttribute('data-x')) || 0
        const y = parseFloat(el.getAttribute('data-y')) || 0
        const panelWidth = 288
        const top = Math.max(8, 12 + y - 8)
        let left = 12 + x + box.w + 10
        if (left + panelWidth > canvas.clientWidth - 8) {
          left = 12 + x - panelWidth - 10
        }
        left = Math.max(8, left)
        this.catalogPanelStyle = {
          top: top + 'px',
          left: left + 'px',
          width: panelWidth + 'px'
        }
      })
    },
    resolveFileUrl(path) {
      if (!path) return ''
      if (/^https?:\/\//i.test(path)) return path
      return process.env.VUE_APP_BASE_API + path
    },
    async waitForHost(maxAttempts = 60) {
      for (let i = 0; i < maxAttempts; i++) {
        if (this._isDestroyed) {
          throw new Error('preview host destroyed')
        }
        await this.$nextTick()
        if (this.$refs.docxHost) return this.$refs.docxHost
        await new Promise(resolve => requestAnimationFrame(resolve))
      }
      throw new Error('preview host missing')
    },
    async renderFromFile(file) {
      if (!file) return
      this.loadError = ''
      this.loading = true
      this.rendered = false
      this.clearBoxes()
      const token = ++this.renderToken
      try {
        const blob = file instanceof Blob ? file : new Blob([file])
        await this.renderBlob(blob, token)
      } catch (e) {
        if (e && e.message === 'preview host destroyed') return
        console.error('docx render failed', e)
        this.cleanupStyleContainer()
        this.loadError = '文档预览失败，请确认文件为 .docx 格式'
      } finally {
        if (token === this.renderToken) this.loading = false
      }
    },
    async loadFromServer(filePath) {
      this.loadError = ''
      this.loading = true
      this.rendered = false
      this.clearBoxes()
      const token = ++this.renderToken
      try {
        const resp = await fetch(this.resolveFileUrl(filePath), {
          headers: { Authorization: 'Bearer ' + getToken() }
        })
        if (!resp.ok) throw new Error('http ' + resp.status)
        const blob = await resp.blob()
        await this.renderBlob(blob, token)
      } catch (e) {
        console.error('docx fetch failed', e)
        this.cleanupStyleContainer()
        this.loadError = '无法加载服务器上的文档，请重新上传'
      } finally {
        if (token === this.renderToken) this.loading = false
      }
    },
    cleanupStyleContainer() {
      if (this.styleContainer && this.styleContainer.parentNode) {
        this.styleContainer.parentNode.removeChild(this.styleContainer)
      }
      this.styleContainer = null
    },
    async renderBlob(blob, token) {
      const host = await this.waitForHost()
      host.innerHTML = ''
      this.cleanupStyleContainer()
      const styleRoot = document.createElement('div')
      styleRoot.setAttribute('data-docx-visual-styles', String(token))
      styleRoot.style.display = 'none'
      document.head.appendChild(styleRoot)
      this.styleContainer = styleRoot
      await renderAsync(blob, host, styleRoot, {
        className: 'docx-visual',
        inWrapper: true,
        ignoreWidth: false,
        ignoreHeight: false,
        ignoreFonts: false,
        breakPages: true,
        ignoreLastRenderedPageBreak: false,
        useBase64URL: true,
        experimental: true,
        renderHeaders: true,
        renderFooters: true
      })
      if (token !== this.renderToken) {
        this.cleanupStyleContainer()
        return
      }
      this.rendered = true
      this.$emit('rendered')
      this.updateCatalogPanelPosition()
    },
    onDocHostClick(event) {
      if (!this.smartSelectMode || !this.rendered) return
      if (event.target.closest('.docx-selection-box, .box-catalog-panel')) return
      this.smartFrameAtPoint(event.clientX, event.clientY)
    },
    ensureActiveBox() {
      if (!this.boxes.length) {
        this.addBox(false)
      } else if (!this.activeBoxId) {
        this.activeBoxId = this.boxes[0].id
      }
      return this.activeBoxId
    },
    applyBoxRect(boxId, rect) {
      if (!rect || !boxId) return false
      const boxEl = this.$el.querySelector(`.docx-selection-box[data-box-id="${boxId}"]`)
      const box = this.boxes.find(b => b.id === boxId)
      if (!boxEl || !box) return false
      const width = Math.max(80, Math.round(rect.width))
      const height = Math.max(60, Math.round(rect.height))
      const x = Math.max(0, Math.round(rect.left))
      const y = Math.max(0, Math.round(rect.top))
      box.w = width
      box.h = height
      boxEl.style.width = `${width}px`
      boxEl.style.height = `${height}px`
      boxEl.style.transform = `translate(${x}px, ${y}px)`
      boxEl.setAttribute('data-x', x)
      boxEl.setAttribute('data-y', y)
      this.activeBoxId = boxId
      this.updateCatalogPanelPosition()
      return true
    },
    smartFrameAtPoint(clientX, clientY) {
      const root = this.$refs.docxHost
      const canvas = this.$refs.canvasInner
      if (!root || !canvas) return false
      const rect = detectQuestionRectAtPoint(root, canvas, clientX, clientY)
      if (!rect) {
        this.$modal.msgWarning('未识别到题目，请点击题干或选项区域')
        return false
      }
      const boxId = this.ensureActiveBox()
      return this.applyBoxRect(boxId, rect)
    },
    smartFrameByContent(content) {
      const root = this.$refs.docxHost
      const canvas = this.$refs.canvasInner
      if (!root || !canvas || !content) return false
      const rect = detectQuestionRectByText(root, canvas, content)
      if (!rect) return false
      const boxId = this.ensureActiveBox()
      const ok = this.applyBoxRect(boxId, rect)
      if (ok) {
        canvas.scrollTo({
          top: Math.max(0, rect.top - 48),
          behavior: 'smooth'
        })
      }
      return ok
    },
    addBox(focusNew = true) {
      const id = ++this.boxSeq
      this.boxes.push({
        id,
        w: 320,
        h: 180,
        meta: createBoxMeta(this.defaultSubjectId ? Number(this.defaultSubjectId) : undefined)
      })
      if (focusNew) {
        this.activeBoxId = id
      }
      this.$nextTick(() => {
        this.setupInteract(id)
        if (focusNew) this.updateCatalogPanelPosition()
      })
    },
    clearBoxes() {
      this.destroyInteract()
      this.boxes = []
      this.activeBoxId = null
      this.catalogPanelStyle = null
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
              if (parseInt(target.getAttribute('data-box-id'), 10) === this.activeBoxId) {
                this.updateCatalogPanelPosition()
              }
            },
            end: event => {
              if (this.smartSelectMode) {
                this.smartSnapBox(parseInt(event.target.getAttribute('data-box-id'), 10))
              }
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
              const currentId = parseInt(target.getAttribute('data-box-id'), 10)
              const box = this.boxes.find(b => b.id === currentId)
              if (box) {
                box.w = event.rect.width
                box.h = event.rect.height
              }
              if (currentId === this.activeBoxId) {
                this.updateCatalogPanelPosition()
              }
            },
            end: event => {
              if (this.smartSelectMode) {
                this.smartSnapBox(parseInt(event.target.getAttribute('data-box-id'), 10))
              }
            }
          },
          modifiers: [
            interact.modifiers.restrictSize({ min: { width: 80, height: 60 } }),
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
    smartSnapBox(boxId) {
      const boxEl = this.$el.querySelector(`.docx-selection-box[data-box-id="${boxId}"]`)
      const canvas = this.$refs.canvasInner
      const root = this.$refs.docxHost
      if (!boxEl || !canvas || !root) return
      const rect = boxEl.getBoundingClientRect()
      const cx = rect.left + rect.width / 2
      const cy = rect.top + rect.height / 2
      const frame = detectQuestionRectAtPoint(root, canvas, cx, cy)
      if (frame) {
        this.applyBoxRect(boxId, frame)
      }
    },
    relativeRect(node) {
      const canvas = this.$refs.canvasInner
      if (!canvas || !node) return null
      const canvasRect = canvas.getBoundingClientRect()
      const rect = node.getBoundingClientRect()
      return {
        left: rect.left - canvasRect.left + canvas.scrollLeft,
        top: rect.top - canvasRect.top + canvas.scrollTop,
        right: rect.right - canvasRect.left + canvas.scrollLeft,
        bottom: rect.bottom - canvasRect.top + canvas.scrollTop,
        width: rect.width,
        height: rect.height
      }
    },
    rectsIntersect(a, b) {
      if (!a || !b) return false
      return !(a.right < b.left || a.left > b.right || a.bottom < b.top || a.top > b.bottom)
    },
    overlapArea(a, b) {
      const x = Math.max(0, Math.min(a.right, b.right) - Math.max(a.left, b.left))
      const y = Math.max(0, Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top))
      return x * y
    },
    elementInSelection(el, sel) {
      const rect = this.relativeRect(el)
      if (!rect || rect.width < 1 || rect.height < 1) return false
      const elArea = rect.width * rect.height
      const overlap = this.overlapArea(sel, rect)
      if (overlap <= 0) return false
      return overlap / elArea >= 0.35 || this.rectsIntersect(sel, rect)
    },
    imageInSelection(node, sel) {
      return mediaInSelection(node, sel, this.overlapArea.bind(this), this.relativeRect.bind(this))
    },
    collectTextLines(root, sel) {
      return collectTextLineRows(root, this.elementInSelection.bind(this), sel)
    },
    async uploadImageSrc(src) {
      if (!src) return null
      if (src.startsWith('/profile/')) return src
      if (/^https?:\/\//i.test(src) && src.includes('/profile/')) {
        const idx = src.indexOf('/profile/')
        return idx >= 0 ? src.slice(idx) : null
      }
      let blob
      if (src.startsWith('data:') || src.startsWith('blob:')) {
        const res = await fetch(src)
        blob = await res.blob()
      } else {
        return null
      }
      return this.uploadImageBlob(blob)
    },
    async uploadImageBlob(blob) {
      if (!blob) return null
      const formData = new FormData()
      const ext = (blob.type || '').includes('png') ? 'png' : 'jpg'
      formData.append('file', blob, 'docx-figure-' + Date.now() + '.' + ext)
      const uploadRes = await uploadImportImage(formData)
      return uploadRes.fileName || null
    },
    async uploadMediaNode(node) {
      if (!node) return null
      const tag = (node.tagName || '').toUpperCase()
      if (tag === 'SVG') {
        try {
          const blob = await rasterizeSvgToBlob(node)
          return await this.uploadImageBlob(blob)
        } catch (e) {
          return null
        }
      }
      return this.uploadImageSrc(node.src)
    },
    async collectImageUrls(root, sel) {
      const maxImages = 5
      const candidates = []
      const seenKey = new Set()
      for (const node of queryMediaNodes(root)) {
        if (!this.imageInSelection(node, sel)) continue
        const tag = (node.tagName || '').toUpperCase()
        const key = tag === 'SVG' ? 'svg:' + (node.outerHTML || '').slice(0, 120) : (node.src || '')
        if (!key || seenKey.has(key)) continue
        seenKey.add(key)
        const rect = node.getBoundingClientRect()
        const overlap = this.overlapArea(sel, this.relativeRect(node))
        candidates.push({
          node,
          area: rect.width * rect.height,
          overlap
        })
      }
      candidates.sort((a, b) => b.overlap - a.overlap || b.area - a.area)
      const urls = []
      for (const item of candidates.slice(0, maxImages)) {
        const path = await this.uploadMediaNode(item.node)
        if (path && !urls.includes(path)) urls.push(path)
      }
      return { urls, truncated: candidates.length > maxImages }
    },
    async captureActiveBox() {
      if (!this.activeBoxId) {
        this.$modal.msgWarning('请先新建选框')
        return
      }
      if (!this.rendered) {
        this.$modal.msgWarning('文档尚未加载完成')
        return
      }
      const boxEl = this.$el.querySelector(`.docx-selection-box[data-box-id="${this.activeBoxId}"]`)
      const root = this.$refs.docxHost
      if (!boxEl || !root) return
      const sel = this.relativeRect(boxEl)
      const lines = this.collectTextLines(root, sel)
      this.capturing = true
      try {
        const imageResult = await this.collectImageUrls(root, sel)
        const imageUrls = imageResult.urls || []
        const content = buildCaptureContent(lines)
        if (!content && !imageUrls.length) {
          this.$modal.msgWarning('选框内未识别到内容，请调整选框')
          return
        }
        if (imageResult.truncated) {
          this.$modal.msgWarning('选框内图片较多，已自动保留最重要的 5 张')
        }
        const activeBox = this.boxes.find(b => b.id === this.activeBoxId)
        const meta = activeBox && activeBox.meta ? Object.assign({}, activeBox.meta) : null
        this.$emit('capture', { content, imageUrls, meta })
      } finally {
        this.capturing = false
      }
    }
  }
}
</script>

<style scoped lang="scss">
.docx-visual-canvas {
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
.load-error {
  margin-bottom: 8px;
}
.canvas-inner {
  position: relative;
  min-height: 520px;
  max-height: 640px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #e8eaed;
}
.canvas-inner.is-expanded {
  min-height: 520px;
  max-height: calc(72vh - 200px);
}
.docx-host {
  position: relative;
  z-index: 1;
  padding: 16px;
  min-height: 400px;
  overflow: visible;
}
.smart-select-mode .docx-host {
  cursor: crosshair;
}
/* docx-preview layout: preserve page width and floating objects */
.docx-host ::v-deep .docx-visual-wrapper {
  background: transparent !important;
  padding: 0 !important;
  display: block !important;
}
.docx-host ::v-deep .docx-visual-wrapper > section.docx-visual {
  background: #fff;
  margin: 0 auto 24px !important;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.1);
  overflow: visible !important;
  display: block !important;
  flex: none !important;
  box-sizing: border-box;
}
.docx-host ::v-deep section.docx-visual > article {
  position: relative;
  overflow: visible !important;
  display: block;
  width: 100%;
}
.docx-host ::v-deep section.docx-visual p,
.docx-host ::v-deep section.docx-visual table {
  overflow: visible;
}
.docx-host ::v-deep section.docx-visual img {
  max-width: none;
  height: auto;
}
.docx-host ::v-deep section.docx-visual svg {
  overflow: visible;
}
.docx-host ::v-deep section.docx-visual span {
  white-space: pre-wrap;
}
.docx-selection-box {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 3;
  overflow: visible;
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
.box-catalog-panel {
  position: absolute;
  width: 288px;
  padding: 8px 10px 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #dcdfe6;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.14);
  z-index: 20;
  cursor: default;
  pointer-events: auto;
}
.box-catalog-panel ::v-deep .question-catalog-picker {
  display: block;
  min-height: 180px;
}
.box-catalog-title {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #303133;
}
.box-catalog-actions {
  margin-top: 4px;
  text-align: right;
}
</style>
