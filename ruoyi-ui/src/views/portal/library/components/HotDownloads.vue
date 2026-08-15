<template>
  <aside class="hot-downloads" :class="{ 'hot-downloads--embedded': embedded }" v-loading="loading">
    <div class="hot-head">
      <h2>{{ title }}</h2>
      <router-link to="/library" class="hot-more">{{ moreLabel }}</router-link>
    </div>
    <ul v-if="items.length" class="hot-list">
      <li
        v-for="(item, index) in items"
        :key="item.documentId"
        class="hot-item"
        :class="{ current: isCurrent(item) }"
      >
        <span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
        <div class="hot-main">
          <div class="hot-title-row">
            <file-ext-badge :ext="item.fileExt" size="xs" />
            <router-link
              :to="detailPath(item)"
              class="hot-title-link"
              :class="{ 'is-current': isCurrent(item) }"
              :title="item.title"
            >{{ item.title }}</router-link>
            <span class="hot-stat"><i class="el-icon-download" />{{ item.downloadCount || 0 }}</span>
            <span v-if="isCurrent(item)" class="hot-current-tag">{{ currentLabel }}</span>
          </div>
        </div>
      </li>
    </ul>
    <p v-else-if="!loading" class="hot-empty">{{ emptyLabel }}</p>
  </aside>
</template>

<script>
import { fetchHotDownloadsCached } from '@/utils/metaCache'
import { resolveLibraryDocumentPath } from '@/utils/libraryNavigation'
import FileExtBadge from './FileExtBadge'

export default {
  name: 'HotDownloads',
  components: { FileExtBadge },
  props: {
    excludeId: { type: [String, Number], default: null },
    limit: { type: Number, default: 5 },
    embedded: { type: Boolean, default: false }
  },
  data() {
    return {
      loading: false,
      items: []
    }
  },
  computed: {
    title() {
      return '\u70ed\u95e8\u4e0b\u8f7d'
    },
    moreLabel() {
      return '\u66f4\u591a'
    },
    emptyLabel() {
      return '\u6682\u65e0\u70ed\u95e8\u6587\u6863'
    },
    currentLabel() {
      return '\u5f53\u524d'
    }
  },
  watch: {
    excludeId: {
      immediate: true,
      handler() {
        this.scheduleLoad()
      }
    }
  },
  methods: {
    detailPath(item) {
      return resolveLibraryDocumentPath(item)
    },
    isCurrent(item) {
      if (!item || this.excludeId == null) return false
      return String(item.documentId) === String(this.excludeId)
    },
    scheduleLoad() {
      const run = () => this.loadList()
      if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
        window.requestIdleCallback(run, { timeout: 1200 })
      } else {
        setTimeout(run, 60)
      }
    },
    loadList() {
      this.loading = true
      const pageSize = Math.max(this.limit + 1, 6)
      fetchHotDownloadsCached(pageSize).then(res => {
        const rows = res.rows || []
        const exclude = this.excludeId != null ? String(this.excludeId) : ''
        let filtered = rows.filter(item => String(item.documentId) !== exclude)
        if (!filtered.length) {
          filtered = rows
        }
        this.items = filtered.slice(0, this.limit)
      }).catch(() => {
        this.items = []
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped lang="scss">
.hot-downloads {
  padding: 12px 14px 10px;
  min-height: 72px;
}

.hot-downloads--embedded {
  padding: 14px 14px 12px;
  min-height: 0;
  background: transparent;
  border: none;
  border-radius: 0;
  box-shadow: none;
  border-top: 1px solid #eef2f7;
}

.hot-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e8edf3;

  h2 {
    margin: 0;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 700;
    color: #1e293b;

    &::before {
      content: '';
      width: 3px;
      height: 12px;
      border-radius: 999px;
      background: linear-gradient(180deg, #0f766e, #2dd4bf);
    }
  }
}

.hot-more {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #0f766e;
  text-decoration: none;
  border: 1px solid transparent;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

  &:hover {
    background: #ecfeff;
    border-color: rgba(15, 118, 110, 0.14);
    text-decoration: none;
  }

  &:active {
    background: #ccfbf1;
  }
}

.hot-empty {
  margin: 0;
  padding: 12px 0 4px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

.hot-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hot-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 8px;
  border-radius: 10px;
  border: 1px solid transparent;
  border-bottom: none;
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    background: linear-gradient(135deg, #f0fdfa 0%, #f8fafc 100%);
    border-color: rgba(15, 118, 110, 0.16);
    box-shadow: 0 4px 14px rgba(15, 118, 110, 0.08);
    transform: translateX(3px);

    .hot-title-link {
      color: #0f766e;
    }

    .hot-rank {
      transform: scale(1.06);
    }
  }

  &:active {
    transform: translateX(1px);
    background: #ecfeff;
    border-color: rgba(15, 118, 110, 0.28);
  }

  &.current {
    background: linear-gradient(135deg, #ecfeff 0%, #f0fdfa 100%);
    border-color: rgba(15, 118, 110, 0.22);
    box-shadow: inset 3px 0 0 #0f766e;
  }

  &:last-child {
    padding-bottom: 8px;
  }
}

.hot-rank {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  margin-top: 1px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
  transition: transform 0.2s ease, background 0.2s ease;

  &.top {
    background: linear-gradient(135deg, #fef3c7, #fde68a);
    color: #b45309;
    box-shadow: 0 1px 4px rgba(180, 83, 9, 0.12);
  }
}

.hot-main {
  min-width: 0;
  flex: 1;
}

.hot-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.hot-title-link {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  line-height: 1.5;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;

  &:hover {
    color: #0f766e;
    text-decoration: none;
  }

  &:active {
    color: #115e59;
  }

  &.is-current {
    color: #0f766e;
    font-weight: 700;
  }
}

.hot-current-tag {
  flex-shrink: 0;
  margin-top: 1px;
  padding: 1px 6px;
  border-radius: 999px;
  background: #ecfeff;
  border: 1px solid rgba(15, 118, 110, 0.14);
  color: #0f766e;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
}

.hot-stat {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;

  i {
    font-size: 12px;
  }
}
</style>
