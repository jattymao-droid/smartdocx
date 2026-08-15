<template>
  <div class="portal-library-topics portal-page">
    <div class="portal-container" v-loading="loading">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">{{ L.home }}</router-link>
        <span class="sep">/</span>
        <router-link to="/library">{{ L.library }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ L.title }}</span>
      </nav>

      <header class="topics-hero portal-card">
        <h1>{{ L.title }}</h1>
        <p>{{ L.subtitle }}</p>
      </header>

      <div v-if="list.length" class="topics-grid">
        <article
          v-for="topic in list"
          :key="topic.topicId"
          class="topic-card portal-card"
          @click="openTopic(topic)"
        >
          <div class="topic-card-cover">
            <img v-if="topic.coverUrl" :src="resolveCover(topic.coverUrl)" :alt="topic.title" loading="lazy" decoding="async">
            <div v-else class="topic-card-cover-fallback">
              <i class="el-icon-folder-opened" />
            </div>
          </div>
          <div class="topic-card-body">
            <h3 class="topic-card-title" :title="topic.title">{{ topic.title }}</h3>
            <p v-if="topic.summary" class="topic-card-summary">{{ topic.summary }}</p>
            <div class="topic-card-meta">
              <span><i class="el-icon-document" />{{ topic.docCount || 0 }}{{ L.docUnit }}</span>
              <span><i class="el-icon-download" />{{ topic.downloadCount || 0 }}{{ L.downloadUnit }}</span>
              <span class="topic-price">{{ formatPrice(topic.bundlePrice) }}</span>
            </div>
          </div>
        </article>
      </div>

      <div v-else-if="!loading" class="topics-empty portal-card">
        <i class="el-icon-folder-opened" />
        <p>{{ L.empty }}</p>
        <router-link to="/library" class="topics-empty-link">{{ L.goLibrary }}</router-link>
      </div>

      <pagination
        v-show="total > query.pageSize"
        :total="total"
        :page.sync="query.pageNum"
        :limit.sync="query.pageSize"
        @pagination="loadList"
      />
    </div>
  </div>
</template>

<script>
import { listLibraryTopics } from '@/api/education/library'
import { resolvePortalMediaUrl } from '@/utils/portalBanner'

const L = {
  home: '\u9996\u9875',
  library: '\u6587\u5e93',
  title: '\u70ed\u95e8\u4e13\u9898',
  subtitle: '\u7cbe\u9009\u6587\u6863\u4e13\u9898\uff0c\u652f\u6301\u4e00\u952e\u6253\u5305\u4e0b\u8f7d',
  empty: '\u6682\u65e0\u4e13\u9898\uff0c\u656c\u8bf7\u671f\u5f85',
  goLibrary: '\u8fd4\u56de\u6587\u5e93',
  docUnit: '\u4efd',
  downloadUnit: '\u6b21'
}

export default {
  name: 'PortalLibraryTopicList',
  data() {
    return {
      L,
      loading: false,
      list: [],
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 12
      }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      listLibraryTopics({
        portal: true,
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize
      }).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).catch(() => {
        this.list = []
        this.total = 0
      }).finally(() => {
        this.loading = false
      })
    },
    openTopic(topic) {
      if (topic && topic.topicId) {
        this.$router.push('/library/topic/' + topic.topicId)
      }
    },
    resolveCover(url) {
      return resolvePortalMediaUrl(url)
    },
    formatPrice(val) {
      const n = Number(val)
      if (!Number.isFinite(n) || n <= 0) return '\u514d\u8d39'
      return '\u00a5' + n.toFixed(2)
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #0F766E;
$ink: #0F172A;
$muted: #64748B;
$border: #E2E8F0;

.portal-library-topics {
  padding-bottom: 40px;
}

.topics-hero {
  padding: 24px 28px;
  margin-bottom: 20px;

  h1 {
    margin: 0 0 8px;
    font-size: 26px;
    color: $ink;
  }

  p {
    margin: 0;
    color: $muted;
  }
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.topic-card {
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.15s, box-shadow 0.15s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  }
}

.topic-card-cover {
  height: 120px;
  background: #F1F5F9;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.topic-card-cover-fallback {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $primary;
  font-size: 36px;
}

.topic-card-body {
  padding: 16px;
}

.topic-card-title {
  margin: 0 0 6px;
  font-size: 16px;
  color: $ink;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-card-summary {
  margin: 0 0 10px;
  font-size: 13px;
  color: $muted;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.topic-card-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: $muted;

  i { margin-right: 3px; }

  .topic-price {
    margin-left: auto;
    color: $primary;
    font-weight: 600;
  }
}

.topics-empty {
  text-align: center;
  padding: 48px 24px;
  color: $muted;

  i {
    font-size: 40px;
    color: #CBD5E1;
    display: block;
    margin-bottom: 12px;
  }

  p { margin: 0 0 12px; }
}

.topics-empty-link {
  color: $primary;
  text-decoration: none;

  &:hover { text-decoration: underline; }
}
</style>
