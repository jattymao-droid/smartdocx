<template>
  <div class="chapter-sidebar" :class="{ 'chapter-sidebar--portal': variant === 'portal' }">
    <div class="sidebar-title">
      <i v-if="variant === 'portal'" class="el-icon-folder-opened sidebar-title-icon" aria-hidden="true" />
      <span class="sidebar-title-text">教材章节</span>
    </div>
    <div v-loading="loading" class="sidebar-tree-wrap">
      <el-tree
        v-if="treeData.length"
        ref="tree"
        class="chapter-tree"
        :data="treeData"
        node-key="id"
        :props="treeProps"
        :indent="20"
        highlight-current
        :expand-on-click-node="false"
        :default-expanded-keys="defaultExpandedKeys"
        @node-click="onNodeClick"
      >
        <span slot-scope="{ node, data }" class="tree-node">
          <span class="tree-label" :title="node.label">{{ node.label }}</span>
          <span v-if="data.count != null" class="tree-count">{{ data.count }}</span>
        </span>
      </el-tree>
      <el-empty v-if="!loading && !treeData.length" :image-size="56" description="暂无章节" />
    </div>
  </div>
</template>

<script>
import { getTextbookChapterTree } from '@/api/education/textbook'
import { getChapterTreeCache, setChapterTreeCache } from '@/utils/catalogCache'

export default {
  name: 'ChapterSidebar',
  props: {
    subjectId: { type: [Number, String], default: undefined },
    textbookId: { type: [Number, String], default: undefined },
    chapterId: { type: [Number, String], default: undefined },
    variant: { type: String, default: 'default' }
  },
  data() {
    return {
      loading: false,
      loadSeq: 0,
      treeData: [],
      defaultExpandedKeys: ['all'],
      treeProps: { label: 'label', children: 'children' }
    }
  },
  watch: {
    textbookId: {
      immediate: true,
      handler() { this.loadTree() }
    },
    subjectId() { this.loadTree() },
    chapterId() { this.syncCurrentNode() }
  },
  methods: {
    loadTree() {
      if (!this.textbookId) {
        this.treeData = []
        this.defaultExpandedKeys = ['all']
        return
      }
      const cached = getChapterTreeCache(this.textbookId, this.subjectId)
      if (cached) {
        this.applyTreeData(cached)
        return
      }
      const seq = ++this.loadSeq
      this.loading = true
      getTextbookChapterTree(this.textbookId, this.subjectId).then(res => {
        if (seq !== this.loadSeq) return
        const data = res.data || []
        setChapterTreeCache(this.textbookId, this.subjectId, data)
        this.applyTreeData(data)
      }).finally(() => {
        if (seq === this.loadSeq) this.loading = false
      })
    },
    applyTreeData(data) {
      this.treeData = data
      this.defaultExpandedKeys = this.buildDefaultExpandedKeys(data)
      this.$nextTick(() => {
        this.expandPathToCurrent()
        this.syncCurrentNode()
      })
    },
    buildDefaultExpandedKeys(data) {
      const keys = ['all']
      ;(data || []).forEach(node => {
        if (node && node.id && node.id !== 'all') {
          keys.push(String(node.id))
        }
      })
      if (this.chapterId) {
        keys.push(String(this.chapterId))
      }
      return keys
    },
    expandPathToCurrent() {
      const tree = this.$refs.tree
      if (!tree || !this.chapterId) return
      const node = tree.getNode(this.chapterId)
      if (!node) return
      let parent = node.parent
      while (parent && parent.data) {
        parent.expanded = true
        parent = parent.parent
      }
    },
    onNodeClick(data, node) {
      if (data.id === 'all') {
        this.$emit('chapter-change', { chapterId: undefined, chapterText: '' })
        return
      }
      let chapterText = data.label
      const parent = node.parent
      if (parent && parent.data && parent.data.id && parent.data.id !== 'all') {
        chapterText = parent.data.label + ' > ' + data.label
      }
      this.$emit('chapter-change', {
        chapterId: data.id,
        chapterText
      })
    },
    syncCurrentNode() {
      const tree = this.$refs.tree
      if (!tree || !this.treeData.length) return
      const key = this.chapterId ? String(this.chapterId) : 'all'
      tree.setCurrentKey(key)
    }
  }
}
</script>

<style scoped lang="scss">
$line-color: #d4d7dc;
$icon-blue: #3B82F6;
$icon-gray: #c5c9cf;

.chapter-sidebar {
  position: sticky;
  top: 16px;
  align-self: flex-start;
  width: 280px;
  flex-shrink: 0;
  max-height: calc(100vh - 120px);
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-title {
  padding: 12px 14px 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #eef2f6;
}
.sidebar-tree-wrap {
  flex: 1;
  min-height: 0;
  max-height: calc(100vh - 160px);
  padding: 10px 10px 14px 8px;
  overflow-x: hidden;
  overflow-y: auto;
}
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 4px;
  font-size: 13px;
  line-height: 1.4;
  color: #303133;
}
.tree-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tree-count {
  flex-shrink: 0;
  margin-left: 6px;
  font-size: 12px;
  color: #c0c4cc;
}

::v-deep .chapter-tree {
  background: transparent;

  .el-tree-node {
    position: relative;
  }

  .el-tree-node__content {
    height: 34px;
    border-radius: 4px;
    background: transparent !important;
    padding-right: 4px;

    &:hover {
      background: #f5f7fa !important;
    }
  }

  .el-tree-node.is-current > .el-tree-node__content {
    background: #EFF6FF !important;

    .tree-label {
      color: #2563EB;
      font-weight: 500;
    }
  }

  .el-tree-node__expand-icon {
    width: 18px;
    height: 18px;
    margin-right: 6px;
    padding: 0;
    border-radius: 50%;
    background: $icon-gray;
    color: #fff;
    font-size: 14px;
    font-weight: 600;
    line-height: 18px;
    text-align: center;
    transform: rotate(0deg) !important;
    flex-shrink: 0;
    transition: background 0.15s;

    &.expanded {
      background: $icon-blue;
    }

    &.is-leaf {
      width: 0;
      height: 0;
      margin: 0;
      padding: 0;
      visibility: hidden;
      overflow: hidden;
    }
  }

  .el-tree-node__expand-icon.el-icon-caret-right::before {
    content: '+';
    font-family: Arial, Helvetica, sans-serif;
  }

  .el-tree-node__expand-icon.expanded.el-icon-caret-right::before {
    content: '\2212';
  }

  .el-tree-node__children {
    position: relative;
    padding-left: 22px;

    > .el-tree-node {
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: -12px;
        top: 0;
        bottom: 0;
        border-left: 1px dashed $line-color;
      }

      &::after {
        content: '';
        position: absolute;
        left: -12px;
        top: 17px;
        width: 12px;
        border-top: 1px dashed $line-color;
      }

      &:last-child::before {
        bottom: auto;
        height: 17px;
      }
    }
  }

  > .el-tree-node > .el-tree-node__content {
    padding-left: 2px !important;
  }
}

.chapter-sidebar--portal {
  position: static;
  width: 100%;
  flex: 1;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid #EEF2F6;
  border-radius: 8px;

  .sidebar-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0;
    padding: 10px 12px;
    font-size: 13px;
    font-weight: 700;
    color: #1E293B;
    background: #F8FAFC;
    border: none;
    border-bottom: 1px solid #EEF2F6;
    border-radius: 0;
    box-shadow: none;
  }

  .sidebar-title-icon {
    flex-shrink: 0;
    width: 28px;
    height: 28px;
    border-radius: 8px;
    background: #EFF6FF;
    color: #2563EB;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .sidebar-title-text {
    flex: 1;
    min-width: 0;
    letter-spacing: 0.02em;
  }

  .sidebar-tree-wrap {
    flex: 1;
    min-height: 0;
    padding: 6px 6px 10px;
    overflow-x: hidden;
    overflow-y: auto;
    background: transparent;
    border: none;
    border-radius: 0;
    box-shadow: none;
  }

  ::v-deep .chapter-tree {
    .el-tree-node.is-current > .el-tree-node__content {
      background: #EFF6FF !important;
      .tree-label { color: #1E293B; font-weight: 600; }
    }

    .el-tree-node__expand-icon.expanded {
      background: #C4B5A8;
    }

    .el-tree-node__expand-icon:not(.is-leaf):not(.expanded) {
      background: #D8D0C8;
    }
  }
}
</style>
