<template>
  <div class="knowledge-sidebar" :class="{ 'knowledge-sidebar--portal': variant === 'portal' }">
    <div class="sidebar-title">
      <i v-if="variant === 'portal'" class="el-icon-price-tag sidebar-title-icon" aria-hidden="true" />
      <span class="sidebar-title-text">{{ title }}</span>
    </div>
    <div v-if="showSearch" class="sidebar-search">
      <el-input
        v-model="innerKeyword"
        size="small"
        clearable
        :placeholder="searchPlaceholder"
        prefix-icon="el-icon-search"
        @keyup.enter.native="emitSearch"
        @clear="emitSearch"
      />
    </div>
    <div v-loading="loading" class="sidebar-tree-wrap">
      <el-tree
        ref="tree"
        class="knowledge-tree"
        :data="treeData"
        node-key="id"
        :props="treeProps"
        :indent="18"
        highlight-current
        :expand-on-click-node="false"
        default-expand-all
        @node-click="onNodeClick"
      >
        <span slot-scope="{ node, data }" class="tree-node" :class="'tree-node--' + (data.nodeType || 'chapter')">
          <span class="tree-label" :title="node.label">{{ node.label }}</span>
          <span v-if="data.count != null && data.nodeType !== 'knowledge'" class="tree-count">{{ data.count }}</span>
          <span v-else-if="data.count != null && data.nodeType === 'knowledge'" class="tree-count tree-count--tag">{{ data.count }}</span>
        </span>
      </el-tree>
      <el-empty v-if="!loading && !treeData.length" :image-size="56" :description="emptyText" />
    </div>
  </div>
</template>

<script>
import { getKnowledgeTree } from '@/api/education/question'

export default {
  name: 'KnowledgeSidebar',
  props: {
    subjectId: { type: [Number, String], default: undefined },
    textbookId: { type: [Number, String], default: undefined },
    chapterId: { type: [Number, String], default: undefined },
    knowledgePoint: { type: String, default: undefined },
    keyword: { type: String, default: '' },
    variant: { type: String, default: 'default' },
    title: { type: String, default: '\u77e5\u8bc6\u70b9' },
    searchPlaceholder: { type: String, default: '\u641c\u7d22\u77e5\u8bc6\u70b9' },
    emptyText: { type: String, default: '\u6682\u65e0\u77e5\u8bc6\u70b9' },
    showSearch: { type: Boolean, default: true }
  },
  data() {
    return {
      loading: false,
      loadSeq: 0,
      treeData: [],
      innerKeyword: this.keyword || '',
      treeProps: { label: 'label', children: 'children' }
    }
  },
  watch: {
    textbookId: {
      immediate: true,
      handler() { this.loadTree() }
    },
    subjectId() { this.loadTree() },
    keyword(val) {
      if (val !== this.innerKeyword) {
        this.innerKeyword = val || ''
      }
      this.loadTree()
    },
    chapterId() { this.syncCurrentNode() },
    knowledgePoint() { this.syncCurrentNode() }
  },
  methods: {
    emitSearch() {
      this.$emit('search', (this.innerKeyword || '').trim())
    },
    loadTree() {
      if (!this.textbookId || !this.subjectId) {
        this.treeData = []
        return
      }
      const seq = ++this.loadSeq
      this.loading = true
      getKnowledgeTree({
        subjectId: this.subjectId,
        textbookId: this.textbookId,
        keyword: (this.innerKeyword || '').trim() || undefined
      }).then(res => {
        if (seq !== this.loadSeq) return
        this.treeData = res.data || []
        this.$nextTick(() => {
          this.expandAllNodes()
          this.expandPathToCurrent()
          this.syncCurrentNode()
        })
      }).finally(() => {
        if (seq === this.loadSeq) this.loading = false
      })
    },
    expandAllNodes() {
      const tree = this.$refs.tree
      if (!tree || !tree.store) return
      tree.store._getAllNodes().forEach(node => {
        node.expanded = true
      })
    },
    expandPathToCurrent() {
      const tree = this.$refs.tree
      if (!tree) return
      const key = this.resolveCurrentKey()
      if (!key) return
      const node = tree.getNode(key)
      if (!node) return
      let parent = node.parent
      while (parent && parent.data) {
        parent.expanded = true
        parent = parent.parent
      }
    },
    resolveCurrentKey() {
      if (this.knowledgePoint && this.chapterId) {
        const hit = this.findKnowledgeNode(this.treeData, String(this.chapterId), this.knowledgePoint)
        if (hit) return hit.id
      }
      if (this.chapterId) return String(this.chapterId)
      if (!this.knowledgePoint && !this.chapterId) return 'all'
      return null
    },
    findKnowledgeNode(nodes, chapterId, tagName) {
      for (const node of nodes || []) {
        if (node.nodeType === 'knowledge' && String(node.chapterId) === chapterId && node.tagName === tagName) {
          return node
        }
        const child = this.findKnowledgeNode(node.children, chapterId, tagName)
        if (child) return child
      }
      return null
    },
    onNodeClick(data) {
      if (!data) return
      if (data.nodeType === 'knowledge') {
        this.$emit('node-change', {
          chapterId: data.chapterId,
          knowledgePoint: data.tagName
        })
        return
      }
      if (data.id === 'all' || data.nodeType === 'all') {
        this.$emit('node-change', { chapterId: undefined, knowledgePoint: undefined })
        return
      }
      this.$emit('node-change', {
        chapterId: data.chapterId || Number(data.id),
        knowledgePoint: undefined
      })
    },
    syncCurrentNode() {
      const tree = this.$refs.tree
      if (!tree || !this.treeData.length) return
      const key = this.resolveCurrentKey()
      if (key) tree.setCurrentKey(key)
    }
  }
}
</script>

<style scoped lang="scss">
.knowledge-sidebar {
  display: flex;
  flex-direction: column;
  min-height: 200px;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 10px;
}

.sidebar-title-icon {
  color: #2563eb;
}

.sidebar-search {
  margin-bottom: 10px;
}

.sidebar-tree-wrap {
  flex: none;
  overflow: visible;
}

.knowledge-tree {
  background: transparent;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding-right: 6px;
  font-size: 13px;
}

.tree-node--knowledge .tree-label {
  color: #475569;
  font-weight: 400;
}

.tree-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-count {
  flex-shrink: 0;
  min-width: 18px;
  padding: 0 6px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.tree-count--tag {
  background: #eff6ff;
  color: #64748b;
}

.knowledge-sidebar--portal {
  display: flex;
  flex-direction: column;
  overflow: visible;

  .sidebar-title {
    display: none;
  }

  .sidebar-tree-wrap {
    flex: none;
    overflow: visible;
  }
}

::v-deep .el-tree-node__content {
  height: 32px;
  border-radius: 8px;
}

::v-deep .el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content {
  background: #eff6ff;
}
</style>
