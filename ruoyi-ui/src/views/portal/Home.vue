<template>
  <div class="kms-home">
    <!-- Hero -->
    <section
      class="kms-hero"
      :class="{
        'kms-hero--media': heroHasMedia,
        'kms-hero--video': banner.mode === 'video',
        'kms-hero--single': heroSlideCount <= 1,
        'kms-hero--ref': true
      }"
    >
      <div class="kms-hero-deco" aria-hidden="true">
        <span class="kms-deco-circle kms-deco-circle--1" />
        <span class="kms-deco-circle kms-deco-circle--2" />
        <span class="kms-deco-line kms-deco-line--1" />
        <span class="kms-deco-line kms-deco-line--2" />
      </div>
      <div class="kms-hero-illustration" aria-hidden="true">
        <span class="kms-illus-card kms-illus-card--1" />
        <span class="kms-illus-card kms-illus-card--2" />
        <span class="kms-illus-card kms-illus-card--3" />
        <span class="kms-illus-person" />
      </div>
      <video
        v-if="banner.mode === 'video' && bannerVideoUrl"
        :key="bannerVideoUrl"
        class="kms-hero-video"
        :src="bannerVideoUrl"
        :poster="bannerPosterUrl || undefined"
        :style="{ objectFit: banner.objectFit || 'cover' }"
        autoplay
        muted
        loop
        playsinline
        preload="metadata"
      />
      <el-carousel
        v-else
        ref="heroCarousel"
        class="kms-hero-carousel"
        height="100%"
        :interval="heroSlideCount > 1 ? heroInterval : 0"
        arrow="never"
        indicator-position="none"
        @change="onHeroSlideChange"
      >
        <el-carousel-item v-for="(slide, i) in heroSlides" :key="i">
          <div
            v-if="slideImageStyle(slide)"
            class="kms-hero-slide kms-hero-slide--image"
            :style="slideImageStyle(slide)"
          />
          <div v-else class="kms-hero-slide" :style="{ background: slide.bg }" />
        </el-carousel-item>
      </el-carousel>
      <div class="kms-hero-shade" :style="heroShadeStyle" aria-hidden="true" />
      <div class="portal-container kms-hero-inner">
        <div class="kms-hero-copy">
          <h2 v-if="currentHero.title">{{ currentHero.title }}</h2>
          <p v-if="currentHero.desc">{{ currentHero.desc }}</p>
        </div>
        <div class="kms-search">
          <el-select v-model="searchType" size="medium" class="kms-search-type">
            <el-option label="文档" value="doc" />
            <el-option label="试题" value="question" />
          </el-select>
          <el-input
            v-model="keyword"
            size="medium"
            clearable
            placeholder="输入关键词"
            class="kms-search-input"
            @keyup.enter.native="onSearch"
          />
          <button type="button" class="kms-search-btn" @click="onSearch">
            <i class="el-icon-search" />
          </button>
        </div>
        <div class="kms-hotwords">
          <span class="kms-hotwords-label">热门搜索：</span>
          <button
            v-for="w in hotWords"
            :key="w"
            type="button"
            class="kms-hotword"
            @click="searchWord(w)"
          >{{ w }}</button>
        </div>
        <div v-if="heroSlideCount > 1" class="kms-hero-dots" aria-hidden="true">
          <button
            v-for="(slide, i) in heroSlides"
            :key="i"
            type="button"
            class="dot"
            :class="{ active: heroSlide === i }"
            @click="setHeroSlide(i)"
          />
        </div>
        <button v-if="heroSlideCount > 1" class="kms-hero-arrow kms-hero-arrow--prev" type="button" @click="prevHero">
          <i class="el-icon-arrow-left" />
        </button>
        <button v-if="heroSlideCount > 1" class="kms-hero-arrow kms-hero-arrow--next" type="button" @click="nextHero">
          <i class="el-icon-arrow-right" />
        </button>
      </div>

      <div class="kms-notice kms-notice--in-hero">
        <div class="portal-container kms-notice-inner">
          <div class="kms-notice-left">
            <i class="el-icon-message-solid" />
            <span class="kms-notice-text">教学文库系统已上线，支持在线预览与分享</span>
            <span class="kms-notice-meta">持续更新教学课件、教案与试卷资料</span>
          </div>
          <div class="kms-notice-stats">
            共 <em>{{ docTotal }}</em> 份，今日新增 <em>{{ todayNew }}</em> 份
          </div>
        </div>
      </div>
    </section>

    <!-- Category columns -->
    <section class="kms-cats portal-container">
      <div class="kms-cat-grid">
        <div
          v-for="(col, idx) in categoryColumns"
          :key="col.title"
          class="kms-cat-col"
          :class="'kms-cat-col--' + idx"
        >
          <div class="kms-cat-head">
            <span class="kms-cat-icon">
              <i :class="col.icon" />
            </span>
            <h3>{{ col.title }}</h3>
          </div>
          <div class="kms-cat-links">
            <button
              v-for="link in col.links"
              :key="link.label"
              type="button"
              class="kms-cat-link"
              @click="goCategory(link)"
            >
              <span>{{ link.label }}</span>
              <i class="el-icon-arrow-right kms-cat-link-arrow" />
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Quick entry: 组卷 -->
    <section class="portal-container kms-quick-strip">
      <div class="kms-quick-grid">
        <button
          v-for="item in quickEntries"
          :key="item.label"
          type="button"
          class="kms-quick-item"
          @click="goQuick(item)"
        >
          <span class="kms-quick-icon" :style="{ background: item.bg, color: item.color }">
            <i :class="item.icon" />
          </span>
          <span class="kms-quick-label">{{ item.label }}</span>
          <span v-if="item.smart" class="kms-quick-ai">AI</span>
        </button>
      </div>
    </section>

    <!-- Main two-column -->
    <div class="portal-container kms-main">
      <div class="kms-main-left">
        <!-- Latest docs -->
        <section class="kms-panel">
          <div class="kms-panel-head">
            <h2>最新文档</h2>
            <a class="kms-more" @click="goLibrary()">查看更多 <i class="el-icon-arrow-right" /></a>
          </div>
          <div v-loading="loadingLatest" class="kms-doc-grid">
            <article
              v-for="doc in latestDocs"
              :key="doc.documentId"
              class="kms-doc-card"
              @click="openDoc(doc)"
            >
              <div class="kms-doc-cover">
                <library-doc-cover :cover-url="doc.coverUrl" :file-ext="doc.fileExt" />
                <span v-if="doc.recommendFlag === '1'" class="kms-vip-badge">推荐</span>
                <file-ext-badge :ext="doc.fileExt" size="md" class="kms-ext-tag" />
              </div>
              <h3 :title="doc.title">{{ doc.title }}</h3>
              <p class="kms-doc-meta">
                <span>{{ formatDate(doc.createTime) }}</span>
                <span><i class="el-icon-view" /> {{ doc.viewCount || 0 }}</span>
              </p>
            </article>
            <div v-if="!loadingLatest && !latestDocs.length" class="kms-empty-grid">暂无文档，欢迎上传分享</div>
          </div>
        </section>

        <!-- Recommend + Hot -->
        <div class="kms-rank-row">
          <section class="kms-panel kms-rank-panel">
            <div class="kms-panel-head">
              <h2>推荐文档</h2>
            </div>
            <div v-if="recommendFeatured" class="kms-hot-feature" @click="openDoc(recommendFeatured)">
              <div class="kms-hot-cover">
                <library-doc-cover :cover-url="recommendFeatured.coverUrl" :file-ext="recommendFeatured.fileExt" />
              </div>
              <div class="kms-hot-info">
                <h4>{{ recommendFeatured.title }}</h4>
                <p>{{ recommendFeatured.categoryName || recommendFeatured.subjectName || '教学资料' }}</p>
                <div class="kms-hot-tags">
                  <span>{{ formatDate(recommendFeatured.createTime) }}</span>
                  <span>{{ recommendFeatured.viewCount || 0 }} 阅读</span>
                  <span class="free-tag">免费</span>
                </div>
              </div>
            </div>
            <ol class="kms-rank-list kms-rank-list--compact">
              <li
                v-for="(doc, idx) in recommendRestDocs"
                :key="doc.documentId"
                class="kms-rank-item"
                @click="openDoc(doc)"
              >
                <span class="rank-num">{{ idx + 2 }}</span>
                <file-ext-badge :ext="doc.fileExt" size="sm" />
                <span class="rank-title" :title="doc.title">{{ doc.title }}</span>
              </li>
            </ol>
          </section>

          <section class="kms-panel kms-rank-panel">
            <div class="kms-panel-head">
              <h2>热门文档</h2>
              <div class="kms-rank-tabs">
                <button
                  v-for="tab in hotTabs"
                  :key="tab.key"
                  type="button"
                  class="kms-rank-tab"
                  :class="{ active: hotTab === tab.key }"
                  @click="hotTab = tab.key"
                >{{ tab.label }}</button>
              </div>
            </div>
            <div v-if="hotFeatured" class="kms-hot-feature" @click="openDoc(hotFeatured)">
              <div class="kms-hot-cover">
                <library-doc-cover :cover-url="hotFeatured.coverUrl" :file-ext="hotFeatured.fileExt" />
              </div>
              <div class="kms-hot-info">
                <h4>{{ hotFeatured.title }}</h4>
                <p>{{ hotFeatured.categoryName || hotFeatured.subjectName || '教学资料' }}</p>
                <div class="kms-hot-tags">
                  <span>{{ formatDate(hotFeatured.createTime) }}</span>
                  <span>{{ hotFeatured.viewCount || 0 }} 阅读</span>
                  <span class="free-tag">免费</span>
                </div>
              </div>
            </div>
            <ol class="kms-rank-list kms-rank-list--compact">
              <li
                v-for="(doc, idx) in hotRestDocs"
                :key="doc.documentId"
                class="kms-rank-item"
                @click="openDoc(doc)"
              >
                <span class="rank-num">{{ idx + 2 }}</span>
                <file-ext-badge :ext="doc.fileExt" size="sm" />
                <span class="rank-title" :title="doc.title">{{ doc.title }}</span>
              </li>
            </ol>
          </section>
        </div>
      </div>

      <aside class="kms-main-right">
        <!-- User card -->
        <section class="kms-side-card kms-user-card">
          <template v-if="token">
            <div class="kms-user-head">
              <img v-if="avatar" :src="avatar" class="kms-user-avatar" alt="">
              <span v-else class="kms-user-avatar kms-user-avatar--fallback"><i class="el-icon-user-solid" /></span>
              <div>
                <h4>{{ nickName || name }}</h4>
                <p>欢迎回来</p>
              </div>
            </div>
            <div class="kms-user-stats">
              <div><b>{{ myDocCount }}</b><span>文档</span></div>
              <div><b>{{ favoriteCount }}</b><span>收藏</span></div>
              <div><b>{{ continueCount }}</b><span>在读</span></div>
            </div>
            <div class="kms-user-privs">
              <span><i class="el-icon-download" /> 高速下载</span>
              <span><i class="el-icon-star-on" /> 收藏文档</span>
              <span><i class="el-icon-reading" /> 继续阅读</span>
              <span><i class="el-icon-upload2" /> 上传分享</span>
            </div>
            <el-button type="warning" class="kms-login-btn" size="small" round @click="$router.push('/library')">进入文库</el-button>
          </template>
          <template v-else>
            <h4>登录享以下特权</h4>
            <div class="kms-user-stats">
              <div><b>0</b><span>文档</span></div>
              <div><b>0</b><span>收藏</span></div>
              <div><b>0</b><span>在读</span></div>
            </div>
            <div class="kms-user-privs">
              <span><i class="el-icon-download" /> 高速下载</span>
              <span><i class="el-icon-star-on" /> 收藏文档</span>
              <span><i class="el-icon-reading" /> 继续阅读</span>
              <span><i class="el-icon-upload2" /> 上传分享</span>
            </div>
            <el-button type="warning" class="kms-login-btn" size="small" round @click="goLogin">立即登录</el-button>
          </template>
        </section>

        <!-- Activity feed -->
        <section class="kms-side-card">
          <div class="kms-side-head">
            <h3>用户动态</h3>
          </div>
          <ul class="kms-activity-list">
            <li v-for="(act, i) in activityFeed" :key="i">
              <span class="act-avatar"><i class="el-icon-user" /></span>
              <div class="act-body">
                <p><em>{{ act.user }}</em> {{ act.action }}《{{ act.title }}》</p>
                <span>{{ act.time }}</span>
              </div>
            </li>
            <li v-if="!activityFeed.length" class="kms-activity-empty">暂无动态</li>
          </ul>
        </section>

        <!-- Creator ranking -->
        <section class="kms-side-card">
          <div class="kms-side-head">
            <h3>创作排行</h3>
          </div>
          <ol class="kms-creator-list">
            <li v-for="(c, idx) in creatorRank" :key="c.name">
              <span class="creator-rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
              <span class="creator-avatar"><i class="el-icon-user-solid" /></span>
              <div class="creator-info">
                <b>{{ c.name }}</b>
                <span>上传 {{ c.count }} 份文档</span>
              </div>
            </li>
            <li v-if="!creatorRank.length" class="kms-activity-empty">暂无数据</li>
          </ol>
        </section>
      </aside>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import {
  listLibraryDocuments,
  listContinueReading,
  listLibraryFavorites,
  listMyLibraryDocuments,
  listLibraryTopics
} from '@/api/education/library'
import { fetchSubjectOptionsCached, fetchLibraryCategoriesCached } from '@/utils/metaCache'
import {
  DEFAULT_PORTAL_BANNER,
  DEFAULT_HERO_SLIDES,
  bannerHasMedia,
  buildHeroSlidesFromBanner,
  loadPortalBannerConfig,
  resolvePortalMediaUrl
} from '@/utils/portalBanner'
import { goPortalLogin } from '@/utils/portalLogin'
import FileExtBadge from './library/components/FileExtBadge'
import LibraryDocCover from './library/components/LibraryDocCover'
import { openLibraryDocument } from '@/utils/libraryNavigation'

export default {
  name: 'PortalHome',
  components: { FileExtBadge, LibraryDocCover },
  data() {
    return {
      keyword: '',
      searchType: 'doc',
      hotWords: ['考研真题', '课件PPT', '教案模板', '期中试卷', '实验报告'],
      docTotal: 0,
      todayNew: 0,
      loadingLatest: false,
      latestDocs: [],
      recommendDocs: [],
      hotDocs: [],
      hotTab: 'week',
      hotTabs: [
        { key: 'week', label: '周榜' },
        { key: 'month', label: '月榜' },
        { key: 'season', label: '季榜' }
      ],
      subjects: [],
      categories: [],
      hotTopics: [],
      myDocCount: 0,
      favoriteCount: 0,
      continueCount: 0,
      categoryColumns: [],
      banner: { ...DEFAULT_PORTAL_BANNER },
      heroSlide: 0,
      heroInterval: 5000,
      heroSlides: DEFAULT_HERO_SLIDES.map(s => ({ ...s })),
      quickEntries: [
        { label: '章节选题', icon: 'el-icon-folder-opened', bg: '#ECFEFF', color: '#0F766E', smart: false, path: '/chapter' },
        { label: '知识点选题', icon: 'el-icon-price-tag', bg: '#D1FAE5', color: '#047857', smart: false, path: '/knowledge' },
        { label: '试卷选题', icon: 'el-icon-document', bg: '#CFFAFE', color: '#0E7490', smart: false, path: '/exam' },
        { label: '智能组卷', icon: 'el-icon-cpu', bg: '#CCFBF1', color: '#115E59', smart: true, path: '/paper' },
        { label: '教学文库', icon: 'el-icon-reading', bg: '#F0FDFA', color: '#0F766E', smart: false, path: '/library' },
        { label: '我的试卷', icon: 'el-icon-folder', bg: '#F1F5F9', color: '#475569', smart: false, path: '/my-papers' }
      ],
      rankPool: []
    }
  },
  computed: {
    ...mapGetters(['token', 'name', 'nickName', 'avatar']),
    heroHasMedia() {
      return bannerHasMedia(this.banner)
    },
    heroSlideCount() {
      return this.banner.mode === 'video' ? 1 : this.heroSlides.length
    },
    bannerVideoUrl() {
      return resolvePortalMediaUrl(this.banner.videoUrl)
    },
    bannerPosterUrl() {
      return resolvePortalMediaUrl(this.banner.videoPoster)
    },
    heroShadeStyle() {
      const overlay = this.heroHasMedia && typeof this.banner.overlay === 'number'
        ? this.banner.overlay
        : (this.banner.mode === 'video' ? 0.52 : null)
      if (overlay == null) return {}
      return { opacity: overlay }
    },
    currentHero() {
      return this.heroSlides[this.heroSlide] || this.heroSlides[0] || { title: '', desc: '', chips: [], primaryText: '进入文库' }
    },
    hotFeatured() {
      return this.displayHotDocs[0] || null
    },
    hotRestDocs() {
      return this.displayHotDocs.slice(1, 10)
    },
    recommendFeatured() {
      return this.displayRecommendDocs[0] || null
    },
    recommendRestDocs() {
      return this.displayRecommendDocs.slice(1, 10)
    },
    displayRecommendDocs() {
      if (this.recommendDocs.length) return this.recommendDocs
      return this.latestDocs.slice(0, 10)
    },
    displayHotDocs() {
      if (this.hotDocs.length) return this.hotDocs
      return this.latestDocs.slice(0, 10)
    },
    activityFeed() {
      return this.latestDocs.slice(0, 6).map(doc => ({
        user: doc.createBy || '用户',
        action: '上传了',
        title: this.truncate(doc.title, 12),
        time: this.formatRelative(doc.createTime)
      }))
    },
    creatorRank() {
      const map = {}
      const all = this.rankPool.length
        ? this.rankPool
        : [...this.latestDocs, ...this.recommendDocs, ...this.hotDocs]
      all.forEach(doc => {
        const name = doc.createBy || '匿名用户'
        if (!map[name]) map[name] = 0
        map[name] += 1
      })
      return Object.keys(map)
        .map(name => ({ name, count: map[name] }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 8)
    }
  },
  watch: {
    hotTab() {
      this.loadHotDocs()
    },
    token() {
      this.loadUserStats()
    }
  },
  created() {
    this.loadBannerConfig()
    this.loadDocBootstrap()
    this.loadFilters()
    this.scheduleSecondaryLoads()
  },
  methods: {
    loadBannerConfig() {
      loadPortalBannerConfig().then(cfg => {
        this.banner = cfg
        this.heroSlides = buildHeroSlidesFromBanner(cfg)
        this.heroSlide = 0
        this.$nextTick(() => {
          const carousel = this.$refs.heroCarousel
          if (carousel && typeof carousel.setActiveItem === 'function') {
            carousel.setActiveItem(0)
          }
        })
      }).catch(() => {})
    },
    slideImageStyle(slide) {
      if (!slide || !slide.imageUrl) return null
      const url = resolvePortalMediaUrl(slide.imageUrl)
      if (!url) return null
      return {
        backgroundImage: `url(${url})`,
        backgroundSize: this.banner.objectFit || 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat'
      }
    },
    onHeroSlideChange(index) {
      this.heroSlide = index
    },
    setHeroSlide(index) {
      this.heroSlide = index
      const carousel = this.$refs.heroCarousel
      if (carousel && typeof carousel.setActiveItem === 'function') {
        carousel.setActiveItem(index)
      }
    },
    prevHero() {
      const next = this.heroSlide > 0 ? this.heroSlide - 1 : this.heroSlides.length - 1
      this.setHeroSlide(next)
    },
    nextHero() {
      const next = (this.heroSlide + 1) % this.heroSlides.length
      this.setHeroSlide(next)
    },
    goQuick(item) {
      if (item && item.path) this.$router.push(item.path)
    },
    loadDocBootstrap() {
      this.loadingLatest = true
      listLibraryDocuments({ portal: true, pageNum: 1, pageSize: 50 }).then(res => {
        const rows = res.rows || []
        this.docTotal = res.total || 0
        const today = new Date().toDateString()
        this.todayNew = rows.filter(r => r.createTime && new Date(r.createTime).toDateString() === today).length
        this.latestDocs = rows.slice(0, 10)
        this.rankPool = rows
      }).catch(() => {
        this.latestDocs = []
        this.rankPool = []
      }).finally(() => {
        this.loadingLatest = false
      })
    },
    scheduleSecondaryLoads() {
      const run = () => {
        this.loadRecommendDocs()
        this.loadHotDocs()
        this.loadUserStats()
      }
      if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
        window.requestIdleCallback(run, { timeout: 2000 })
      } else {
        setTimeout(run, 120)
      }
    },
    loadRecommendDocs() {
      listLibraryDocuments({
        portal: true,
        recommendFlag: '1',
        pageNum: 1,
        pageSize: 10,
        orderBy: 'recommend'
      }).then(res => {
        this.recommendDocs = res.rows || []
      }).catch(() => { this.recommendDocs = [] })
    },
    loadHotDocs() {
      const orderBy = this.hotTab === 'week' ? 'download' : (this.hotTab === 'month' ? 'view' : 'recommend')
      listLibraryDocuments({
        portal: true,
        pageNum: 1,
        pageSize: 10,
        orderBy
      }).then(res => {
        this.hotDocs = res.rows || []
      }).catch(() => { this.hotDocs = [] })
    },
    loadFilters() {
      fetchSubjectOptionsCached().then(res => {
        this.subjects = res.data || []
        this.buildCategoryColumns()
      }).catch(() => { this.subjects = []; this.buildCategoryColumns() })
      fetchLibraryCategoriesCached().then(res => {
        this.categories = res.data || []
        this.buildCategoryColumns()
      }).catch(() => { this.categories = []; this.buildCategoryColumns() })
      listLibraryTopics({ portal: true, pageNum: 1, pageSize: 6 }).then(res => {
        this.hotTopics = res.rows || []
        this.buildCategoryColumns()
      }).catch(() => { this.hotTopics = []; this.buildCategoryColumns() })
    },
    buildCategoryColumns() {
      const subjectLinks = this.subjects.slice(0, 6).map(s => ({
        label: s.subjectName,
        type: 'subject',
        id: s.subjectId
      }))
      const categoryLinks = this.categories.slice(0, 6).map(c => ({
        label: c.categoryName,
        type: 'category',
        id: c.categoryId
      }))
      const topicLinks = this.hotTopics.slice(0, 6).map(t => ({
        label: t.title,
        type: 'topic',
        topicId: t.topicId
      }))
      this.categoryColumns = [
        {
          title: '学科资料',
          icon: 'el-icon-reading',
          bg: '#ECFEFF',
          color: '#0F766E',
          links: subjectLinks.length ? subjectLinks : [
            { label: '语文', type: 'keyword', keyword: '语文' },
            { label: '数学', type: 'keyword', keyword: '数学' },
            { label: '英语', type: 'keyword', keyword: '英语' },
            { label: '物理', type: 'keyword', keyword: '物理' },
            { label: '化学', type: 'keyword', keyword: '化学' },
            { label: '生物', type: 'keyword', keyword: '生物' }
          ]
        },
        {
          title: '教学文档',
          icon: 'el-icon-folder-opened',
          bg: '#D1FAE5',
          color: '#047857',
          links: categoryLinks.length ? categoryLinks : [
            { label: '课件', type: 'keyword', keyword: '课件' },
            { label: '教案', type: 'keyword', keyword: '教案' },
            { label: '试卷', type: 'keyword', keyword: '试卷' },
            { label: '讲义', type: 'keyword', keyword: '讲义' },
            { label: '实验', type: 'keyword', keyword: '实验' },
            { label: '真题', type: 'keyword', keyword: '真题' }
          ]
        },
        {
          title: '热门专题',
          icon: 'el-icon-star-on',
          bg: '#FEF3C7',
          color: '#D97706',
          links: topicLinks.length ? topicLinks : [
            { label: '进入文库', type: 'library' }
          ]
        },
        {
          title: '文件格式',
          icon: 'el-icon-document',
          bg: '#CFFAFE',
          color: '#0E7490',
          links: [
            { label: 'PDF文档', type: 'ext', ext: 'pdf' },
            { label: 'Word文档', type: 'ext', ext: 'docx' },
            { label: 'PPT课件', type: 'ext', ext: 'pptx' },
            { label: 'Excel表格', type: 'ext', ext: 'xlsx' },
            { label: 'TXT文本', type: 'ext', ext: 'txt' },
            { label: '全部格式', type: 'library' }
          ]
        }
      ]
    },
    loadUserStats() {
      if (!this.token) {
        this.myDocCount = 0
        this.favoriteCount = 0
        this.continueCount = 0
        return
      }
      listMyLibraryDocuments({ pageNum: 1, pageSize: 1 }).then(res => {
        this.myDocCount = res.total || 0
      }).catch(() => {})
      listLibraryFavorites({ pageNum: 1, pageSize: 1 }).then(res => {
        this.favoriteCount = res.total || 0
      }).catch(() => {})
      listContinueReading(5).then(res => {
        const list = res.data || res.rows || []
        this.continueCount = list.length
      }).catch(() => {})
    },
    onSearch() {
      const kw = (this.keyword || '').trim()
      if (this.searchType === 'question') {
        this.$router.push({ path: '/chapter', query: kw ? { keyword: kw } : {} })
        return
      }
      this.goLibrary(kw ? { keyword: kw } : {})
    },
    searchWord(w) {
      this.keyword = w
      this.searchType = 'doc'
      this.onSearch()
    },
    goCategory(link) {
      if (link.type === 'subject') {
        this.goLibrary({ subjectId: link.id })
      } else if (link.type === 'category') {
        this.goLibrary({ categoryId: link.id })
      } else if (link.type === 'ext') {
        this.goLibrary({ fileExtFilter: link.ext })
      } else if (link.type === 'topic') {
        this.$router.push('/library/topic/' + link.topicId)
      } else if (link.type === 'keyword') {
        this.goLibrary({ keyword: link.keyword })
      } else {
        this.goLibrary()
      }
    },
    goLibrary(query = {}) {
      this.$router.push({ path: '/library', query })
    },
    openDoc(doc) {
      openLibraryDocument(this.$router, doc)
    },
    goLogin() {
      goPortalLogin(this.$router, '/')
    },
    formatDate(val) {
      if (!val) return '-'
      const d = new Date(val)
      if (Number.isNaN(d.getTime())) return String(val).slice(0, 10)
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    },
    formatRelative(val) {
      if (!val) return ''
      const diff = Date.now() - new Date(val).getTime()
      const mins = Math.floor(diff / 60000)
      if (mins < 1) return '刚刚'
      if (mins < 60) return `${mins}分钟前`
      const hours = Math.floor(mins / 60)
      if (hours < 24) return `${hours}小时前`
      const days = Math.floor(hours / 24)
      if (days < 7) return `${days}天前`
      return this.formatDate(val)
    },
    truncate(str, len) {
      if (!str) return ''
      return str.length > len ? str.slice(0, len) + '…' : str
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #0F766E;
$primary-dark: #115E59;
$primary-light: #14B8A6;
$cyan: #0E7490;
$accent: #D97706;
$accent-light: #F59E0B;
$ink: #0F172A;
$muted: #64748B;
$border: #E2E8F0;
$page-bg: #F1F5F9;

.kms-home {
  background: $page-bg;
  background-image:
    radial-gradient(ellipse 70% 45% at 8% -5%, rgba(15, 118, 110, 0.05), transparent),
    radial-gradient(ellipse 65% 40% at 95% 0%, rgba(14, 116, 144, 0.05), transparent);
  padding-bottom: 40px;
}

/* ---- Hero ---- */
.kms-hero {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  min-height: 320px;
  background: linear-gradient(180deg, #1e4d8c 0%, #2563b8 42%, #1d4f91 100%);
}

.kms-hero--media,
.kms-hero--video {
  min-height: 400px;
}

.kms-hero--ref {
  margin-top: 0;
}

.kms-hero-deco {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}

.kms-deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);

  &--1 {
    width: 280px;
    height: 280px;
    right: 8%;
    top: 18%;
  }

  &--2 {
    width: 120px;
    height: 120px;
    left: 6%;
    bottom: 22%;
  }
}

.kms-deco-line {
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(147, 197, 253, 0.25), transparent);

  &--1 {
    width: 220px;
    height: 1px;
    right: 12%;
    top: 34%;
    transform: rotate(-18deg);
  }

  &--2 {
    width: 160px;
    height: 1px;
    left: 10%;
    top: 58%;
    transform: rotate(12deg);
  }
}

.kms-hero-illustration {
  position: absolute;
  right: 6%;
  bottom: 72px;
  width: 320px;
  height: 200px;
  z-index: 1;
  pointer-events: none;
  opacity: 0.92;
}

.kms-illus-card {
  position: absolute;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12);

  &--1 {
    width: 88px;
    height: 56px;
    left: 24px;
    top: 36px;
    transform: rotate(-8deg);
    background: linear-gradient(135deg, #fff, #eff6ff);
  }

  &--2 {
    width: 72px;
    height: 48px;
    left: 120px;
    top: 18px;
    transform: rotate(6deg);
    background: linear-gradient(135deg, #fff, #fef3c7);
  }

  &--3 {
    width: 96px;
    height: 62px;
    left: 168px;
    top: 72px;
    transform: rotate(-4deg);
    background: linear-gradient(135deg, #fff, #ecfeff);
  }
}

.kms-illus-person {
  position: absolute;
  left: 88px;
  bottom: 8px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fde68a, #fb923c);
  box-shadow: 0 8px 20px rgba(251, 146, 60, 0.35);

  &::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: -18px;
    width: 72px;
    height: 28px;
    margin-left: -36px;
    border-radius: 50%;
    background: rgba(15, 23, 42, 0.12);
  }
}

.kms-hero-carousel {
  position: absolute !important;
  inset: 0;
  z-index: 0;

  ::v-deep .el-carousel__container {
    height: 100% !important;
  }
}

.kms-hero-slide {
  width: 100%;
  height: 100%;
  min-height: 320px;
}

.kms-hero--media .kms-hero-slide,
.kms-hero--video .kms-hero-slide {
  min-height: 400px;
}

.kms-hero-slide--image {
  background-color: #1e4d8c;
}

.kms-hero-video {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.kms-hero--media .kms-hero-shade {
  background: linear-gradient(
    105deg,
    rgba(26, 68, 128, 0.82) 0%,
    rgba(37, 99, 235, 0.62) 45%,
    rgba(29, 78, 145, 0.48) 100%
  );
}

.kms-hero-shade {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(180deg, rgba(26, 68, 128, 0.15) 0%, rgba(29, 78, 145, 0.45) 100%);
  pointer-events: none;
}

.kms-hero-bg {
  display: none;
}

.kms-hero-inner {
  position: relative;
  z-index: 2;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
  padding: 24px 0 12px;
}

.kms-hero-copy {
  max-width: 760px;
  margin: 0 auto 16px;
  text-align: center;
  color: #fff;

  h2 {
    margin: 0 0 8px;
    font-size: 28px;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-shadow: 0 2px 16px rgba(15, 23, 42, 0.18);
  }

  p {
    margin: 0;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.82);
  }
}

.kms-search {
  display: flex;
  align-items: stretch;
  width: min(760px, 92%);
  max-width: 760px;
  margin: 0 auto;
  background: #fff;
  border-radius: 999px;
  overflow: hidden;
  box-shadow: 0 10px 36px rgba(15, 23, 42, 0.16);
  border: 2px solid rgba(255, 255, 255, 0.95);
}

.kms-search-type {
  width: 96px;
  flex-shrink: 0;

  ::v-deep .el-input__inner {
    border: none;
    border-right: 1px solid $border;
    border-radius: 0;
    height: 46px;
    background: #f8fafc;
    color: #334155;
    font-weight: 600;
  }
}

.kms-search-input {
  flex: 1;

  ::v-deep .el-input__inner {
    border: none;
    border-radius: 0;
    height: 46px;
    font-size: 14px;
    background: #fff;
  }
}

.kms-search-btn {
  width: 52px;
  margin: 4px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  transition: filter 0.15s, transform 0.15s;

  &:hover {
    filter: brightness(1.06);
    transform: scale(1.02);
  }
}

.kms-hotwords {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.78);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

.kms-hotwords-label {
  margin-right: 2px;
}

.kms-hotword {
  margin: 0;
  padding: 4px 12px;
  border: none;
  background: rgba(59, 130, 246, 0.85);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  border-radius: 999px;
  transition: background 0.15s, transform 0.15s;

  &:hover {
    background: rgba(37, 99, 235, 0.95);
    transform: translateY(-1px);
  }
}

.kms-hero-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;

  .dot {
    width: 8px;
    height: 8px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.35);
    cursor: pointer;
    transition: background 0.2s, transform 0.2s;

    &.active {
      background: #fff;
      transform: scale(1.15);
    }
  }
}

.kms-hero-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.45);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  z-index: 3;
  transition: background 0.15s, border-color 0.15s;

  &:hover {
    background: rgba(255, 255, 255, 0.24);
    border-color: rgba(255, 255, 255, 0.7);
  }

  &--prev { left: 8px; }
  &--next { right: 8px; }
}

/* ---- Quick strip ---- */
.kms-quick-strip {
  margin-top: 16px;
}

.kms-quick-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0;
  background: #fff;
  border: 1px solid $border;
  border-radius: 14px;
}

.kms-quick-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: none;
  border-right: 1px solid #f0f0f0;
  background: #fff;
  cursor: pointer;
  transition: background 0.15s;

  &:last-child { border-right: none; }
  &:hover { background: #fafafa; }
}

.kms-quick-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.kms-quick-label {
  font-size: 13px;
  color: $ink;
  font-weight: 500;
}

.kms-quick-ai {
  position: absolute;
  top: 10px;
  right: 12px;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 9px;
  font-weight: 800;
  color: $cyan;
  background: #ECFEFF;
}

/* ---- Notice ---- */
.kms-notice {
  background: #fff;
  border-top: 1px solid #e8edf3;
}

.kms-notice--in-hero {
  position: relative;
  z-index: 3;
  flex-shrink: 0;
  margin-top: auto;
  width: 100%;
  box-shadow: 0 -4px 20px rgba(15, 23, 42, 0.06);
}

.kms-notice-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 46px;
  padding: 10px 0;
  font-size: 13px;
  flex-wrap: wrap;
}

.kms-notice-left {
  display: flex;
  align-items: center;
  gap: 8px;
  color: $muted;
  min-width: 0;
  flex-wrap: wrap;

  i { color: #2563eb; font-size: 16px; flex-shrink: 0; }
}

.kms-notice-text { color: $ink; font-weight: 600; }

.kms-notice-meta {
  color: $muted;
  font-size: 12px;
  &::before { content: '|'; margin: 0 8px; color: #e0e0e0; }
}

.kms-notice-stats {
  color: $muted;
  white-space: nowrap;
  font-size: 13px;

  em {
    font-style: normal;
    color: #2563eb;
    font-weight: 700;
  }
}

/* ---- Categories ---- */
.kms-cats {
  padding: 20px 24px 0;
}

.kms-cat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.kms-cat-col {
  position: relative;
  padding: 12px 14px 10px;
  background: #fff;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04), 0 2px 8px rgba(15, 23, 42, 0.03);
  overflow: hidden;
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, $primary, $primary-light);
  }

  &--1::before {
    background: linear-gradient(90deg, #047857, #34D399);
  }

  &--2::before {
    background: linear-gradient(90deg, #D97706, #FBBF24);
  }

  &--3::before {
    background: linear-gradient(90deg, $cyan, #22D3EE);
  }

  &:hover {
    box-shadow: 0 6px 20px rgba(15, 118, 110, 0.1);
    transform: translateY(-1px);
  }
}

.kms-cat-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #F1F5F9;

  h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: $ink;
    line-height: 1.3;
  }
}

.kms-cat-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #ECFEFF, #CCFBF1);
  color: $primary;
  box-shadow: 0 2px 8px rgba(15, 118, 110, 0.12);
}

.kms-cat-col--1 .kms-cat-icon {
  background: linear-gradient(135deg, #D1FAE5, #A7F3D0);
  color: #047857;
  box-shadow: 0 2px 8px rgba(4, 120, 87, 0.12);
}

.kms-cat-col--2 .kms-cat-icon {
  background: linear-gradient(135deg, #FEF3C7, #FDE68A);
  color: #B45309;
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.12);
}

.kms-cat-col--3 .kms-cat-icon {
  background: linear-gradient(135deg, #CFFAFE, #A5F3FC);
  color: $cyan;
  box-shadow: 0 2px 8px rgba(14, 116, 144, 0.12);
}

.kms-cat-links {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 2px 6px;
}

.kms-cat-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2px;
  padding: 4px 6px;
  border: none;
  border-radius: 6px;
  background: transparent;
  text-align: left;
  font-size: 12px;
  line-height: 1.35;
  color: #64748B;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .kms-cat-link-arrow {
    flex-shrink: 0;
    font-size: 11px;
    opacity: 0;
    transform: translateX(-4px);
    transition: opacity 0.15s, transform 0.15s;
    color: $primary;
  }

  &:hover {
    color: $primary;
    background: #F0FDFA;

    .kms-cat-link-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }
}

/* ---- Main layout ---- */
.kms-main {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 12px;
  margin-top: 12px;
  align-items: start;
}

.kms-main-right {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kms-panel {
  background: #fff;
  border: 1px solid $border;
  border-radius: 14px;
  padding: 16px 18px 18px;
  margin-bottom: 16px;
}

.kms-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 2px solid $primary;

  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: $ink;
    padding-left: 8px;
    border-left: 4px solid $primary;
    line-height: 1.2;
  }
}

.kms-more {
  font-size: 13px;
  color: $muted;
  cursor: pointer;
  text-decoration: none;

  &:hover { color: $primary; }
}

/* ---- Doc grid ---- */
.kms-doc-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  min-height: 80px;
}

.kms-doc-card {
  cursor: pointer;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  overflow: hidden;
  transition: box-shadow 0.2s, transform 0.15s;
  background: #fff;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  h3 {
    margin: 8px 10px 4px;
    font-size: 13px;
    font-weight: 600;
    color: $ink;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 36px;
  }
}

.kms-doc-cover {
  position: relative;
  height: 110px;
  background: #f5f5f5;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .library-doc-cover {
    width: 100%;
    height: 100%;
  }
}

.kms-doc-cover-fallback {
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

.kms-vip-badge {
  position: absolute;
  top: 0;
  right: 0;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, $accent-light, $accent);
  border-radius: 0 0 0 6px;
}

.kms-ext-tag {
  position: absolute;
  left: 6px;
  bottom: 6px;
}

.kms-doc-meta {
  display: flex;
  justify-content: space-between;
  margin: 0 10px 10px;
  font-size: 11px;
  color: #bdbdbd;

  i { margin-right: 2px; }
}

.kms-empty-grid {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: $muted;
  font-size: 14px;
}

/* ---- Rank lists ---- */
.kms-rank-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.kms-rank-panel .kms-panel-head {
  border-bottom-color: #e0e0e0;

  h2 { border-left-color: $accent; }
}

.kms-rank-tabs {
  display: flex;
  gap: 4px;
}

.kms-rank-tab {
  padding: 2px 10px;
  border: 1px solid $border;
  border-radius: 12px;
  background: #fafafa;
  font-size: 12px;
  color: $muted;
  cursor: pointer;

  &.active {
    background: #ECFEFF;
    border-color: #99F6E4;
    color: $primary;
    font-weight: 600;
  }
}

.kms-hot-feature {
  display: flex;
  gap: 12px;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: #fafafa; }
}

.kms-hot-cover {
  width: 100px;
  height: 72px;
  flex-shrink: 0;
  border-radius: 14px;
  overflow: hidden;

  img { width: 100%; height: 100%; object-fit: cover; }

  .library-doc-cover {
    width: 100%;
    height: 100%;
  }
}

.kms-hot-info {
  flex: 1;
  min-width: 0;

  h4 {
    margin: 0 0 4px;
    font-size: 14px;
    color: $ink;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  p { margin: 0 0 6px; font-size: 12px; color: $muted; }
}

.kms-hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: #bdbdbd;

  .free-tag {
    color: $primary;
    font-weight: 600;
  }
}

.kms-rank-list {
  list-style: none;
  margin: 0;
  padding: 0;

  &--compact .kms-rank-item { padding: 6px 0; }
}

.kms-rank-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
  cursor: pointer;
  font-size: 13px;

  &:last-child { border-bottom: none; }
  &:hover .rank-title { color: $primary; }
}

.rank-num {
  width: 20px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: #bdbdbd;
  flex-shrink: 0;

  &.top { color: $accent; }
}

.rank-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: $ink;
  transition: color 0.15s;
}

/* ---- Sidebar ---- */
.kms-side-card {
  background: #fff;
  border: 1px solid $border;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 0;
}

.kms-side-head {
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;

  h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: $ink;
  }
}

.kms-user-card {
  h4 {
    margin: 0 0 8px;
    font-size: 14px;
    font-weight: 700;
    color: $ink;
    text-align: center;
  }
}

.kms-user-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;

  h4 { margin: 0; text-align: left; font-size: 14px; }
  p { margin: 1px 0 0; font-size: 11px; color: $muted; }
}

.kms-user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #CCFBF1;

  &--fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    background: #ECFEFF;
    color: $primary;
    font-size: 18px;
  }
}

.kms-user-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin-bottom: 10px;
  text-align: center;

  b {
    display: block;
    font-size: 15px;
    line-height: 1.2;
    color: $ink;
  }

  span {
    font-size: 10px;
    color: $muted;
  }
}

.kms-user-privs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px 6px;
  margin-bottom: 10px;
  font-size: 10px;
  color: $muted;

  span {
    display: flex;
    align-items: center;
    gap: 3px;
    line-height: 1.3;
  }

  i { color: $accent; font-size: 12px; }
}

.kms-login-btn {
  width: 100%;
  background: linear-gradient(135deg, $primary-light, $primary) !important;
  border-color: $primary !important;
  color: #fff !important;
  font-weight: 600;
  font-size: 13px;
  padding: 8px 0 !important;
}

.kms-activity-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 168px;
  overflow-y: auto;

  li {
    display: flex;
    gap: 8px;
    padding: 6px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child { border-bottom: none; }
  }
}

.act-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bdbdbd;
  flex-shrink: 0;
  font-size: 12px;
}

.act-body {
  flex: 1;
  min-width: 0;

  p {
    margin: 0;
    font-size: 11px;
    color: $ink;
    line-height: 1.35;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;

    em {
      font-style: normal;
      color: $primary;
      font-weight: 600;
    }
  }

  span { font-size: 10px; color: #bdbdbd; }
}

.kms-activity-empty {
  text-align: center;
  color: $muted;
  font-size: 12px;
  padding: 10px 0;
}

.kms-creator-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 220px;
  overflow-y: auto;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 5px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child { border-bottom: none; }
  }
}

.creator-rank {
  width: 16px;
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  color: #bdbdbd;

  &.top { color: $accent; }
}

.creator-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #ECFEFF;
  color: $primary;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 13px;
}

.creator-info {
  flex: 1;
  min-width: 0;

  b {
    display: block;
    font-size: 12px;
    line-height: 1.3;
    color: $ink;
  }

  span {
    font-size: 10px;
    color: $muted;
  }
}

/* ---- Responsive ---- */
@media (max-width: 1200px) {
  .kms-doc-grid { grid-template-columns: repeat(4, 1fr); }
  .kms-main { grid-template-columns: 1fr 300px; }
}

@media (max-width: 992px) {
  .kms-hero-illustration { display: none; }
  .kms-cat-grid { grid-template-columns: repeat(2, 1fr); }
  .kms-quick-grid { grid-template-columns: repeat(3, 1fr); }
  .kms-main { grid-template-columns: 1fr; }
  .kms-doc-grid { grid-template-columns: repeat(3, 1fr); }
  .kms-rank-row { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .kms-hero-copy h2 { font-size: 22px; }
  .kms-hero-copy p { font-size: 13px; }
  .kms-hero-arrow { display: none; }
  .kms-quick-grid { grid-template-columns: repeat(2, 1fr); }
  .kms-quick-item { border-right: none !important; border-bottom: 1px solid #f0f0f0; }
  .kms-quick-item:nth-child(2n) { border-right: none; }
  .kms-doc-grid { grid-template-columns: repeat(2, 1fr); }
  .kms-cat-grid { grid-template-columns: 1fr; }
  .kms-notice-meta { display: none; }
}
</style>
