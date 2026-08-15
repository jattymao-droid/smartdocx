<template>
  <div class="textbook-selector">
    <div class="selector-row selector-row-scroll">
      <span class="row-label">版本</span>
      <div class="row-tags row-tags-scroll">
        <span
          v-for="item in versions"
          :key="item.versionId"
          class="tag-item"
          :class="{ active: item.versionId === innerVersionId }"
          @click="selectVersion(item.versionId)"
        >{{ item.versionName }}</span>
        <span v-if="!loadingVersions && !versions.length" class="tag-empty">暂无版本数据</span>
      </div>
    </div>
    <div class="selector-row">
      <span class="row-label">教材</span>
      <div class="row-tags">
        <span
          v-for="item in textbooks"
          :key="item.textbookId"
          class="tag-item"
          :class="{ active: item.textbookId === innerTextbookId }"
          @click="selectTextbook(item.textbookId)"
        >{{ item.textbookName }}</span>
        <span v-if="!loadingTextbooks && innerVersionId && !textbooks.length" class="tag-empty">暂无教材数据</span>
      </div>
    </div>
  </div>
</template>

<script>
import { listTextbookVersions, listTextbooks } from '@/api/education/textbook'
import { getTextbookCache, getVersionCache, setTextbookCache, setVersionCache } from '@/utils/catalogCache'

export default {
  name: 'TextbookSelectorBar',
  props: {
    subjectId: { type: [Number, String], default: undefined },
    schoolStage: { type: String, default: '高中' },
    versionId: { type: [Number, String], default: undefined },
    textbookId: { type: [Number, String], default: undefined }
  },
  data() {
    return {
      versions: [],
      textbooks: [],
      innerVersionId: this.versionId,
      innerTextbookId: this.textbookId,
      loadingVersions: false,
      loadingTextbooks: false
    }
  },
  watch: {
    subjectId: {
      immediate: true,
      handler() {
        this.loadVersions()
      }
    },
    schoolStage() {
      this.loadVersions()
    },
    versionId(val) {
      this.innerVersionId = val
    },
    textbookId(val) {
      this.innerTextbookId = val
    }
  },
  methods: {
    loadVersions() {
      if (!this.subjectId) {
        this.versions = []
        this.textbooks = []
        this.innerVersionId = undefined
        this.innerTextbookId = undefined
        this.emitChange()
        return
      }
      const cached = getVersionCache(this.subjectId, this.schoolStage)
      if (cached) {
        this.versions = cached
        const keep = this.versions.some(v => v.versionId === this.innerVersionId)
        this.innerVersionId = keep ? this.innerVersionId : (this.versions[0] && this.versions[0].versionId)
        this.loadTextbooks()
        return
      }
      this.loadingVersions = true
      listTextbookVersions(this.subjectId, this.schoolStage).then(res => {
        this.versions = res.data || []
        setVersionCache(this.subjectId, this.schoolStage, this.versions)
        const keep = this.versions.some(v => v.versionId === this.innerVersionId)
        this.innerVersionId = keep ? this.innerVersionId : (this.versions[0] && this.versions[0].versionId)
        this.loadTextbooks()
      }).finally(() => { this.loadingVersions = false })
    },
    loadTextbooks() {
      if (!this.innerVersionId) {
        this.textbooks = []
        this.innerTextbookId = undefined
        this.emitChange()
        return
      }
      const cached = getTextbookCache(this.innerVersionId)
      if (cached) {
        this.textbooks = cached
        const keep = this.textbooks.some(t => t.textbookId === this.innerTextbookId)
        this.innerTextbookId = keep ? this.innerTextbookId : (this.textbooks[0] && this.textbooks[0].textbookId)
        this.emitChange()
        return
      }
      this.loadingTextbooks = true
      listTextbooks(this.innerVersionId).then(res => {
        this.textbooks = res.data || []
        setTextbookCache(this.innerVersionId, this.textbooks)
        const keep = this.textbooks.some(t => t.textbookId === this.innerTextbookId)
        this.innerTextbookId = keep ? this.innerTextbookId : (this.textbooks[0] && this.textbooks[0].textbookId)
        this.emitChange()
      }).finally(() => { this.loadingTextbooks = false })
    },
    selectVersion(id) {
      if (this.innerVersionId === id) return
      this.innerVersionId = id
      this.loadTextbooks()
    },
    selectTextbook(id) {
      if (this.innerTextbookId === id) return
      this.innerTextbookId = id
      this.emitChange()
    },
    emitChange() {
      this.$emit('update:versionId', this.innerVersionId)
      this.$emit('update:textbookId', this.innerTextbookId)
      this.$emit('change', {
        versionId: this.innerVersionId,
        textbookId: this.innerTextbookId
      })
    }
  }
}
</script>

<style scoped lang="scss">
.textbook-selector {
  padding: 0;
}
.selector-row {
  display: flex;
  align-items: flex-start;
  padding: 8px 0;
  font-size: 13px;
  &:not(:last-child) {
    border-bottom: 1px dashed #eef2f6;
  }
}
.selector-row-scroll {
  align-items: center;
}
.row-label {
  flex-shrink: 0;
  width: 36px;
  line-height: 28px;
  color: #909399;
  font-weight: 600;
}
.row-tags {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
}
.row-tags-scroll {
  flex-wrap: nowrap;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 2px;
  scrollbar-width: thin;
  &::-webkit-scrollbar {
    height: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 2px;
  }
}
.tag-item {
  flex-shrink: 0;
  display: inline-block;
  padding: 4px 12px;
  line-height: 20px;
  border-radius: 4px;
  color: #606266;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s;
  &:hover {
    color: #409eff;
    background: #f0f7ff;
  }
  &.active {
    color: #fff;
    background: #409eff;
    font-weight: 600;
  }
}
.tag-empty {
  color: #c0c4cc;
  line-height: 28px;
  font-size: 12px;
  white-space: nowrap;
}
</style>
