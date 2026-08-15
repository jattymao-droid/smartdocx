<template>
  <div class="portal-library portal-page">
    <div class="portal-container library-wrap">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">{{ L.home }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ L.breadcrumb }}</span>
      </nav>

      <header class="library-hero-v2 portal-card">
        <div class="hero-v2-main">
          <h1>{{ pageTitle }}</h1>
          <p>{{ pageSubtitle }}</p>
        </div>
        <div class="hero-v2-search">
          <el-input
            v-model="query.keyword"
            size="small"
            clearable
            prefix-icon="el-icon-search"
            :placeholder="L.searchPh"
            class="search-input"
            @keyup.enter.native="handleSearch"
            @clear="handleSearch"
          />
          <el-button size="small" type="primary" icon="el-icon-search" @click="handleSearch">{{ L.search }}</el-button>
          <el-button v-if="token" size="small" plain icon="el-icon-upload2" @click="openUpload">{{ L.upload }}</el-button>
        </div>
      </header>

      <div v-if="hasActiveFilters" class="search-summary portal-card">
        <div class="search-summary-main">
          <span class="search-summary-count">{{ L.resultPrefix }} <em>{{ total }}</em> {{ L.resultSuffix }}</span>
          <div class="search-chips">
            <span v-if="query.keyword" class="search-chip">
              {{ L.keywordLabel }}：{{ query.keyword }}
              <button type="button" class="chip-close" @click="clearFilter('keyword')"><i class="el-icon-close" /></button>
            </span>
            <span v-if="query.schoolStage" class="search-chip">
              {{ L.stagePh }}：{{ query.schoolStage }}
              <button type="button" class="chip-close" @click="clearFilter('schoolStage')"><i class="el-icon-close" /></button>
            </span>
            <span v-if="query.subjectId" class="search-chip">
              {{ L.subjectPh }}：{{ subjectName(query.subjectId) }}
              <button type="button" class="chip-close" @click="clearFilter('subjectId')"><i class="el-icon-close" /></button>
            </span>
            <span v-if="query.categoryId" class="search-chip">
              {{ L.categoryPh }}：{{ categoryName(query.categoryId) }}
              <button type="button" class="chip-close" @click="clearFilter('categoryId')"><i class="el-icon-close" /></button>
            </span>
            <span v-if="query.fileExtFilter" class="search-chip">
              {{ L.formatPh }}：{{ query.fileExtFilter.toUpperCase() }}
              <button type="button" class="chip-close" @click="clearFilter('fileExtFilter')"><i class="el-icon-close" /></button>
            </span>
          </div>
        </div>
        <el-button type="text" size="small" @click="clearAllFilters">{{ L.clearFilters }}</el-button>
      </div>

      <div class="library-layout-v2">
        <aside class="library-side-v2 portal-card">
          <section class="side-section">
            <h3>{{ L.stagePh }}</h3>
            <div class="side-tags">
              <button
                type="button"
                class="side-tag"
                :class="{ active: !query.schoolStage }"
                @click="query.schoolStage = undefined; handleSearch()"
              >{{ L.all }}</button>
              <button
                type="button"
                class="side-tag"
                :class="{ active: query.schoolStage === L.stageJunior }"
                @click="query.schoolStage = L.stageJunior; handleSearch()"
              >{{ L.stageJunior }}</button>
              <button
                type="button"
                class="side-tag"
                :class="{ active: query.schoolStage === L.stageSenior }"
                @click="query.schoolStage = L.stageSenior; handleSearch()"
              >{{ L.stageSenior }}</button>
            </div>
          </section>

          <section class="side-section">
            <h3>{{ L.subjectPh }}</h3>
            <div class="side-tags">
              <button
                type="button"
                class="side-tag"
                :class="{ active: !query.subjectId }"
                @click="query.subjectId = undefined; handleSearch()"
              >{{ L.all }}</button>
              <button
                v-for="s in subjects"
                :key="s.subjectId"
                type="button"
                class="side-tag"
                :class="{ active: query.subjectId === s.subjectId }"
                @click="query.subjectId = s.subjectId; handleSearch()"
              >{{ s.subjectName }}</button>
            </div>
          </section>

          <section class="side-section">
            <h3>{{ L.categoryPh }}</h3>
            <div class="side-tags">
              <button
                type="button"
                class="side-tag"
                :class="{ active: !query.categoryId }"
                @click="query.categoryId = undefined; handleSearch()"
              >{{ L.all }}</button>
              <button
                v-for="c in categories"
                :key="c.categoryId"
                type="button"
                class="side-tag"
                :class="{ active: query.categoryId === c.categoryId }"
                @click="query.categoryId = c.categoryId; handleSearch()"
              >{{ c.categoryName }}</button>
            </div>
          </section>

          <section v-if="(token && continueList.length) || sideHotList.length" class="side-section side-section--list">
            <h3>{{ token && continueList.length ? L.continueTitle : L.tabHot }}</h3>
            <button
              v-for="item in (token && continueList.length ? continueList.slice(0, 6) : sideHotList)"
              :key="item.documentId"
              type="button"
              class="side-doc-item"
              @click="openDetail(item)"
            >
              <span class="side-doc-title">{{ item.title }}</span>
              <span class="side-doc-meta">{{ (item.fileExt || 'file').toUpperCase() }} · {{ item.viewCount || 0 }} {{ L.reads }}</span>
            </button>
          </section>
        </aside>

        <section class="library-main-v2">
          <div class="library-filters-v2 portal-card">
            <el-tabs v-model="activeTab" @tab-click="onTabChange">
              <el-tab-pane :label="L.tabAll" name="all" />
              <el-tab-pane :label="L.tabRecommend" name="recommend" />
              <el-tab-pane :label="L.tabHot" name="hot" />
              <el-tab-pane v-if="token" :label="L.tabMine" name="mine" />
              <el-tab-pane v-if="token" :label="L.tabFavorite" name="favorite" />
            </el-tabs>
            <div class="filter-row">
              <el-select v-model="query.fileExtFilter" size="small" clearable :placeholder="L.formatPh" @change="handleSearch">
                <el-option v-for="ext in extOptions" :key="ext" :label="ext.toUpperCase()" :value="ext" />
              </el-select>
              <el-select
                v-if="showSortSelect"
                v-model="query.orderBy"
                size="small"
                :placeholder="L.sortPh"
                @change="handleSearch"
              >
                <el-option :label="L.sortLatest" value="" />
                <el-option :label="L.sortView" value="view" />
                <el-option :label="L.sortDownload" value="download" />
              </el-select>
              <span v-if="!loading && list.length" class="filter-result-hint">{{ L.showing }} {{ list.length }} / {{ total }}</span>
            </div>
          </div>

          <div v-loading="loading" class="library-list-wrap portal-card portal-card-accent">
            <div v-if="list.length" class="library-list">
              <article
                v-for="item in list"
                :key="item.documentId"
                class="doc-row"
                @click="openDetail(item)"
              >
                <div class="doc-thumb" :class="extCoverClass(item.fileExt)">
                  <library-doc-cover :cover-url="item.coverUrl" :file-ext="item.fileExt" />
                  <span v-if="item.recommendFlag === '1'" class="doc-recommend">{{ L.recommendBadge }}</span>
                </div>
                <div class="doc-content">
                  <div class="doc-content-main">
                    <h3 class="doc-title" :title="item.title" v-html="highlightKeyword(item.title)" />
                    <p v-if="item.summary" class="doc-summary">{{ item.summary }}</p>
                    <div class="doc-meta">
                      <span v-if="item.schoolStage" class="meta-tag">{{ item.schoolStage }}</span>
                      <span v-if="item.subjectName" class="meta-tag">{{ item.subjectName }}</span>
                      <span v-if="item.categoryName" class="meta-tag">{{ item.categoryName }}</span>
                      <span v-if="item.versionName" class="meta-tag meta-tag--muted">{{ item.versionName }}</span>
                      <span v-if="item.textbookName" class="meta-tag meta-tag--muted">{{ item.textbookName }}</span>
                      <span v-if="item.chapterText" class="meta-tag meta-tag--chapter" :title="item.chapterText">{{ item.chapterText }}</span>
                      <span v-if="formatListPrice(item.downloadPrice)" class="meta-tag meta-tag--price">{{ formatListPrice(item.downloadPrice) }}</span>
                      <span class="meta-tag meta-tag--muted">{{ formatSize(item.fileSize) }}</span>
                      <span class="meta-tag meta-tag--muted">{{ formatDate(item.createTime) }}</span>
                    </div>
                  </div>
                  <div class="doc-stats">
                    <span v-if="activeTab === 'mine' && item.auditStatus === '0'" class="audit-pending">{{ L.auditPending }}</span>
                    <span><i class="el-icon-view" /> {{ item.viewCount || 0 }}</span>
                    <span><i class="el-icon-star-off" /> {{ item.favoriteCount || 0 }}</span>
                    <el-button
                      v-if="activeTab === 'mine'"
                      type="text"
                      size="mini"
                      icon="el-icon-edit"
                      class="doc-edit"
                      @click.stop="openEdit(item)"
                    />
                  </div>
                </div>
              </article>
            </div>
            <div v-else class="portal-empty">
              <div class="portal-empty-icon"><i class="el-icon-reading" /></div>
              <h3>{{ hasActiveFilters ? L.emptySearch : L.empty }}</h3>
              <p>{{ hasActiveFilters ? L.emptySearchHint : L.emptyHint }}</p>
              <div v-if="hasActiveFilters" class="empty-actions">
                <el-button size="small" @click="clearAllFilters">{{ L.clearFilters }}</el-button>
                <el-button v-if="token" size="small" type="primary" plain icon="el-icon-upload2" @click="openUpload">{{ L.upload }}</el-button>
              </div>
            </div>
            <pagination
              v-show="total > 0"
              class="portal-pager"
              :total="total"
              :page.sync="query.pageNum"
              :limit.sync="query.pageSize"
              @pagination="onPagination"
            />
          </div>
        </section>
      </div>
    </div>

    <el-dialog :title="L.editTitle" :visible.sync="editOpen" width="640px" append-to-body>
      <el-form ref="editForm" :model="editForm" label-width="96px" size="small">
        <el-form-item :label="L.fieldTitle">
          <el-input v-model="editForm.title" maxlength="200" />
        </el-form-item>
        <el-form-item :label="L.fieldSummary">
          <el-input v-model="editForm.summary" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
        <el-form-item :label="L.fieldCover">
          <image-upload v-model="editForm.coverUrl" :limit="1" :file-size="5" />
        </el-form-item>
        <el-form-item :label="L.stagePh">
          <el-select v-model="editCatalogForm.schoolStage" clearable :placeholder="L.stagePh" style="width: 100%" @change="onEditStageChange">
            <el-option :label="L.stageJunior" :value="L.stageJunior" />
            <el-option :label="L.stageSenior" :value="L.stageSenior" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="L.fieldSubject">
              <el-select v-model="editForm.subjectId" clearable style="width: 100%" @change="onEditSubjectChange">
                <el-option v-for="s in subjects" :key="s.subjectId" :label="s.subjectName" :value="s.subjectId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="L.fieldCategory">
              <el-select v-model="editForm.categoryId" clearable style="width: 100%">
                <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <document-catalog-fields v-model="editCatalogForm" :subject-id="editForm.subjectId" hide-stage />
        <el-form-item :label="L.fieldVisibility">
          <el-radio-group v-model="editForm.visibility">
            <el-radio label="public">{{ L.visPublic }}</el-radio>
            <el-radio label="school">{{ L.visSchool }}</el-radio>
            <el-radio label="private">{{ L.visPrivate }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="L.fieldDownload">
          <el-switch v-model="editForm.allowDownload" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="editOpen = false">{{ L.cancel }}</el-button>
        <el-button type="primary" :loading="editing" @click="submitEdit">{{ L.save }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { debounce } from '@/utils'
import { fetchSubjectOptionsCached, fetchLibraryCategoriesCached } from '@/utils/metaCache'
import {
  listLibraryDocuments,
  listMyLibraryDocuments,
  listLibraryFavorites,
  updatePortalLibraryDocument,
  listContinueReading
} from '@/api/education/library'
import { goPortalLogin } from '@/utils/portalLogin'
import { resolvePortalMediaUrl } from '@/utils/portalBanner'
import { formatListPriceLabel } from '@/utils/libraryPayDownload'
import DocumentCatalogFields from './components/DocumentCatalogFields'
import FileExtBadge from './components/FileExtBadge'
import LibraryDocCover from './components/LibraryDocCover'
import { getFileExtCoverClass, LIBRARY_UPLOAD_EXTS } from '@/utils/libraryFileExt'
import { openLibraryDocument } from '@/utils/libraryNavigation'

function emptyCatalogForm() {
  return {
    schoolStage: '\u9ad8\u4e2d',
    versionId: undefined,
    textbookId: undefined,
    chapterPath: [],
    chapterId: undefined,
    chapterText: ''
  }
}

const L = {
  home: '\u9996\u9875',
  breadcrumb: '\u6587\u5e93',
  title: '\u6559\u5b66\u6587\u6863\u6587\u5e93',
  subtitle: '\u6d4f\u89c8\u3001\u9884\u89c8\u4e0e\u5206\u4eab\u6559\u5b66\u8d44\u6599',
  searchPh: '\u641c\u7d22\u6807\u9898\u3001\u6807\u7b7e\u6216\u6587\u4ef6\u540d',
  search: '\u641c\u7d22',
  upload: '\u4e0a\u4f20\u6587\u6863',
  tabAll: '\u5168\u90e8',
  tabRecommend: '\u63a8\u8350',
  tabHot: '\u70ed\u95e8',
  tabMine: '\u6211\u7684\u4e0a\u4f20',
  tabFavorite: '\u6211\u7684\u6536\u85cf',
  subjectPh: '\u5b66\u79d1',
  stagePh: '\u5b66\u6bb5',
  stageJunior: '\u521d\u4e2d',
  stageSenior: '\u9ad8\u4e2d',
  categoryPh: '\u5206\u7c7b',
  formatPh: '\u683c\u5f0f',
  empty: '\u6682\u65e0\u6587\u6863',
  emptyHint: '\u8bd5\u8bd5\u6362\u4e2a\u5173\u952e\u8bcd\uff0c\u6216\u767b\u5f55\u540e\u4e0a\u4f20\u8d44\u6599',
  uploadTitle: '\u4e0a\u4f20\u6559\u5b66\u6587\u6863',
  fieldFile: '\u6587\u4ef6',
  fieldTitle: '\u6807\u9898',
  fieldSummary: '\u7b80\u4ecb',
  fieldCover: '\u5c01\u9762',
  fieldSubject: '\u5b66\u79d1',
  fieldCategory: '\u5206\u7c7b',
  fieldTags: '\u6807\u7b7e',
  tagsPh: '\u591a\u4e2a\u6807\u7b7e\u7528\u9017\u53f7\u5206\u9694',
  fieldVisibility: '\u53ef\u89c1\u8303\u56f4',
  fieldDownload: '\u5141\u8bb8\u4e0b\u8f7d',
  visPublic: '\u516c\u5f00',
  visSchool: '\u6821\u5185',
  visPrivate: '\u4ec5\u672c\u4eba',
  cancel: '\u53d6\u6d88',
  confirmUpload: '\u63d0\u4ea4\u4e0a\u4f20',
  editTitle: '\u7f16\u8f91\u6587\u6863',
  auditPending: '\u5f85\u5ba1\u6838',
  save: '\u4fdd\u5b58',
  continueTitle: '\u7ee7\u7eed\u9605\u8bfb',
  continueHint: '\u6700\u8fd1\u6253\u5f00\u7684\u6587\u6863',
  all: '\u5168\u90e8',
  reads: '\u6b21\u9605\u8bfb',
  resultPrefix: '\u5171\u627e\u5230',
  resultSuffix: '\u4efd\u6587\u6863',
  keywordLabel: '\u5173\u952e\u8bcd',
  clearFilters: '\u6e05\u9664\u7b5b\u9009',
  sortPh: '\u6392\u5e8f',
  sortLatest: '\u6700\u65b0\u4e0a\u4f20',
  sortView: '\u6700\u591a\u6d4f\u89c8',
  sortDownload: '\u6700\u591a\u4e0b\u8f7d',
  showing: '\u5f53\u524d\u9875',
  emptySearch: '\u672a\u627e\u5230\u76f8\u5173\u6587\u6863',
  emptySearchHint: '\u8bd5\u8bd5\u6362\u4e2a\u5173\u952e\u8bcd\uff0c\u6216\u8c03\u6574\u5b66\u79d1\u3001\u5206\u7c7b\u7b5b\u9009',
  recommendBadge: '\u63a8\u8350',
  searchResultTitle: '\u641c\u7d22\u7ed3\u679c',
  searchResultSub: '\u6309\u5173\u952e\u8bcd\u3001\u5b66\u79d1\u4e0e\u5206\u7c7b\u7b5b\u9009\u6559\u5b66\u8d44\u6599'
}

export default {
  name: 'PortalLibrary',
  components: { DocumentCatalogFields, FileExtBadge, LibraryDocCover },
  data() {
    return {
      L,
      loading: false,
      list: [],
      total: 0,
      continueList: [],
      subjects: [],
      categories: [],
      activeTab: 'all',
      query: {
        pageNum: 1,
        pageSize: 12,
        keyword: undefined,
        schoolStage: undefined,
        subjectId: undefined,
        categoryId: undefined,
        fileExtFilter: undefined,
        orderBy: '',
        portal: true
      },
      editOpen: false,
      editing: false,
      editForm: {},
      editCatalogForm: emptyCatalogForm(),
      extOptions: LIBRARY_UPLOAD_EXTS,
      listRequestSeq: 0
    }
  },
  computed: {
    ...mapGetters(['token']),
    sideHotList() {
      return (this.list || []).slice(0, 6)
    },
    hasActiveFilters() {
      return !!(this.query.keyword || this.query.schoolStage || this.query.subjectId || this.query.categoryId || this.query.fileExtFilter)
    },
    pageTitle() {
      if (this.query.keyword) return `${this.L.searchResultTitle}：${this.query.keyword}`
      return this.L.title
    },
    pageSubtitle() {
      if (this.hasActiveFilters) return this.L.searchResultSub
      return this.L.subtitle
    },
    showSortSelect() {
      return this.activeTab === 'all' || this.activeTab === 'mine' || this.activeTab === 'favorite'
    }
  },
  created() {
    this.debouncedApplySearch = debounce(() => this.applySearch(), 280)
    this.loadMeta()
    this.syncQueryFromRoute()
    this.loadList()
    this.loadContinue()
  },
  watch: {
    '$route.query': {
      handler() {
        this.syncQueryFromRoute()
        this.loadList()
      },
      deep: true
    },
    token(val) {
      if (val) this.loadContinue()
      else this.continueList = []
    }
  },
  methods: {
    extCoverClass(ext) {
      return getFileExtCoverClass(ext)
    },
    loadMeta() {
      fetchSubjectOptionsCached().then(res => {
        this.subjects = res.data || []
      }).catch(() => { this.subjects = [] })
      fetchLibraryCategoriesCached().then(res => {
        this.categories = res.data || []
      }).catch(() => { this.categories = [] })
    },
    loadContinue() {
      if (!this.token) return
      listContinueReading(6).then(res => {
        this.continueList = res.data || []
      }).catch(() => { this.continueList = [] })
    },
    onTabChange() {
      this.query.pageNum = 1
      this.applySearch()
    },
    handleSearch() {
      this.query.pageNum = 1
      this.debouncedApplySearch()
    },
    applySearch() {
      if (!this.syncRouteFromQuery()) {
        this.loadList()
      }
    },
    syncQueryFromRoute() {
      const q = this.$route.query || {}
      this.query.keyword = q.keyword ? String(q.keyword) : undefined
      this.query.schoolStage = q.schoolStage ? String(q.schoolStage) : undefined
      this.query.subjectId = q.subjectId ? Number(q.subjectId) : undefined
      this.query.categoryId = q.categoryId ? Number(q.categoryId) : undefined
      this.query.fileExtFilter = q.fileExtFilter ? String(q.fileExtFilter) : undefined
      this.query.orderBy = q.orderBy ? String(q.orderBy) : ''
      if (q.tab === 'mine' || q.tab === 'favorite' || q.tab === 'recommend' || q.tab === 'hot' || q.tab === 'all') {
        this.activeTab = q.tab
      }
      if (q.pageNum) this.query.pageNum = Number(q.pageNum) || 1
    },
    syncRouteFromQuery() {
      const next = {}
      if (this.query.keyword) next.keyword = this.query.keyword
      if (this.query.schoolStage) next.schoolStage = this.query.schoolStage
      if (this.query.subjectId) next.subjectId = String(this.query.subjectId)
      if (this.query.categoryId) next.categoryId = String(this.query.categoryId)
      if (this.query.fileExtFilter) next.fileExtFilter = this.query.fileExtFilter
      if (this.query.orderBy) next.orderBy = this.query.orderBy
      if (this.query.pageNum > 1) next.pageNum = String(this.query.pageNum)
      if (this.activeTab && this.activeTab !== 'all') next.tab = this.activeTab
      const cur = this.$route.query || {}
      const same = JSON.stringify(next) === JSON.stringify(
        Object.keys(cur).reduce((acc, key) => {
          if (cur[key] != null && cur[key] !== '') acc[key] = String(cur[key])
          return acc
        }, {})
      )
      if (!same) {
        this.$router.replace({ path: '/library', query: next }).catch(() => {})
      }
      return !same
    },
    clearFilter(field) {
      if (field === 'keyword') this.query.keyword = undefined
      if (field === 'schoolStage') this.query.schoolStage = undefined
      if (field === 'subjectId') this.query.subjectId = undefined
      if (field === 'categoryId') this.query.categoryId = undefined
      if (field === 'fileExtFilter') this.query.fileExtFilter = undefined
      this.handleSearch()
    },
    clearAllFilters() {
      this.query.keyword = undefined
      this.query.schoolStage = undefined
      this.query.subjectId = undefined
      this.query.categoryId = undefined
      this.query.fileExtFilter = undefined
      this.query.orderBy = ''
      this.handleSearch()
    },
    subjectName(id) {
      const item = this.subjects.find(s => s.subjectId === id)
      return item ? item.subjectName : id
    },
    categoryName(id) {
      const item = this.categories.find(c => c.categoryId === id)
      return item ? item.categoryName : id
    },
    onPagination() {
      this.applySearch()
    },
    loadList() {
      const seq = ++this.listRequestSeq
      this.loading = true
      const params = { ...this.query }
      if (this.activeTab === 'recommend') {
        params.recommendFlag = '1'
        params.orderBy = 'recommend'
      } else if (this.activeTab === 'hot') {
        params.orderBy = 'view'
      } else if (!params.orderBy) {
        delete params.orderBy
      }
      let req
      if (this.activeTab === 'mine') {
        req = listMyLibraryDocuments(params)
      } else if (this.activeTab === 'favorite') {
        req = listLibraryFavorites(params)
      } else {
        req = listLibraryDocuments(params)
      }
      req.then(res => {
        if (seq !== this.listRequestSeq) return
        this.list = res.rows || []
        this.total = res.total || 0
      }).catch(() => {
        if (seq !== this.listRequestSeq) return
        this.list = []
        this.total = 0
      }).finally(() => {
        if (seq !== this.listRequestSeq) return
        this.loading = false
      })
    },
    openDetail(item) {
      openLibraryDocument(this.$router, item)
    },
    formatSize(size) {
      const n = Number(size) || 0
      if (n < 1024) return n + ' B'
      if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
      return (n / 1024 / 1024).toFixed(1) + ' MB'
    },
    formatListPrice(downloadPrice) {
      return formatListPriceLabel(downloadPrice)
    },
    formatDate(val) {
      if (!val) return ''
      const d = new Date(val)
      if (Number.isNaN(d.getTime())) return String(val).slice(0, 10)
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    },
    escapeHtml(text) {
      return String(text || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
    },
    highlightKeyword(text) {
      const raw = String(text || '')
      const kw = (this.query.keyword || '').trim()
      const escaped = this.escapeHtml(raw)
      if (!kw) return escaped
      const pattern = new RegExp(`(${kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
      return escaped.replace(pattern, '<mark class="kw-hit">$1</mark>')
    },
    openUpload() {
      if (!this.token) {
        goPortalLogin(this.$router, '/library/upload')
        return
      }
      this.$router.push('/library/upload')
    },
    openEdit(item) {
      this.editForm = {
        documentId: item.documentId,
        title: item.title,
        summary: item.summary,
        coverUrl: item.coverUrl || '',
        subjectId: item.subjectId,
        categoryId: item.categoryId,
        visibility: item.visibility || 'school',
        allowDownload: item.allowDownload || '1'
      }
      this.editCatalogForm = {
        schoolStage: item.schoolStage || '\u9ad8\u4e2d',
        versionId: item.versionId,
        textbookId: item.textbookId,
        chapterPath: [],
        chapterId: item.chapterId,
        chapterText: item.chapterText || ''
      }
      this.editOpen = true
    },
    onEditSubjectChange() {
      this.editCatalogForm = Object.assign(emptyCatalogForm(), { schoolStage: this.editCatalogForm.schoolStage || '\u9ad8\u4e2d' })
    },
    onEditStageChange() {
      this.editCatalogForm = Object.assign(emptyCatalogForm(), { schoolStage: this.editCatalogForm.schoolStage })
    },
    submitEdit() {
      this.editing = true
      updatePortalLibraryDocument({
        ...this.editForm,
        schoolStage: this.editCatalogForm.schoolStage,
        versionId: this.editCatalogForm.versionId,
        textbookId: this.editCatalogForm.textbookId,
        chapterId: this.editCatalogForm.chapterId,
        chapterText: this.editCatalogForm.chapterText
      }).then(() => {
        this.$modal.msgSuccess('\u4fdd\u5b58\u6210\u529f')
        this.editOpen = false
        this.loadList()
      }).finally(() => {
        this.editing = false
      })
    }
  }
}
</script>

<style scoped lang="scss">
.library-wrap {
  padding-bottom: 32px;
}

.library-hero-v2 {
  margin-bottom: 14px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.hero-v2-main {
  min-width: 0;

  h1 {
    margin: 0 0 4px;
    font-size: 20px;
    color: #0f172a;
    font-weight: 700;
  }

  p {
    margin: 0;
    font-size: 12px;
    color: #64748b;
  }
}

.hero-v2-search {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;

  .search-input {
    width: 320px;
  }
}

.library-layout-v2 {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.library-side-v2 {
  padding: 12px 12px 8px;
}

.side-section {
  & + & {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #eef2f7;
  }

  h3 {
    margin: 0 0 8px;
    font-size: 13px;
    font-weight: 700;
    color: #1e293b;
  }
}

.side-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.side-tag {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  border-radius: 14px;
  padding: 3px 10px;
  font-size: 12px;
  cursor: pointer;

  &.active,
  &:hover {
    color: #0f766e;
    border-color: rgba(15, 118, 110, 0.28);
    background: #f0fdfa;
  }
}

.side-section--list {
  .side-doc-item {
    width: 100%;
    border: none;
    background: transparent;
    text-align: left;
    display: block;
    padding: 6px 0;
    cursor: pointer;
  }
}

.side-doc-title {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.side-doc-meta {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
}

.library-main-v2 {
  min-width: 0;
}

.library-filters-v2 {
  margin-bottom: 12px;
  padding: 10px 14px 0;

  .filter-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    padding: 0 0 10px;
  }
}

.filter-result-hint {
  margin-left: auto;
  font-size: 12px;
  color: #94a3b8;
}

.search-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding: 12px 16px;
}

.search-summary-main {
  min-width: 0;
}

.search-summary-count {
  font-size: 13px;
  color: #64748b;

  em {
    font-style: normal;
    font-weight: 800;
    color: #0f766e;
    font-size: 16px;
    margin: 0 2px;
  }
}

.search-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.search-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
  color: #0f766e;
  background: #ecfeff;
  border: 1px solid rgba(15, 118, 110, 0.18);
}

.chip-close {
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  padding: 0;
  line-height: 1;

  &:hover { color: #0f766e; }
}

.empty-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 8px;
}

.doc-recommend {
  position: absolute;
  top: 4px;
  right: 4px;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 9px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  box-shadow: 0 2px 6px rgba(217, 119, 6, 0.28);
}

::v-deep .kw-hit {
  padding: 0 2px;
  border-radius: 3px;
  background: #fef3c7;
  color: #b45309;
  font-weight: 700;
}

.continue-section {
  margin-bottom: 16px;
  padding: 14px 16px 16px;
}

.continue-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;

  h2 {
    margin: 0;
    font-size: 15px;
    font-weight: 700;
    color: #1e293b;
  }
}

.continue-hint {
  font-size: 12px;
  color: #94a3b8;
}

.continue-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.continue-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  min-width: 180px;
  max-width: 240px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 14px rgba(15, 118, 110, 0.1);
  }
}

.continue-ext {
  font-size: 10px;
  font-weight: 700;
  color: #0f766e;
  background: #ecfeff;
  padding: 2px 6px;
  border-radius: 4px;
}

.continue-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.continue-progress {
  width: 100%;
}

.library-hero {
  margin-bottom: 16px;

  .search-input {
    width: 280px;
  }
}

.library-filters {
  margin-bottom: 16px;
  padding: 12px 16px 4px;

  .filter-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    padding-bottom: 8px;
  }
}

@media (max-width: 980px) {
  .library-layout-v2 {
    grid-template-columns: 1fr;
  }

  .hero-v2-search {
    flex-shrink: 1;
    width: 100%;

    .search-input {
      width: auto;
      flex: 1;
    }
  }

  .library-hero-v2 {
    flex-direction: column;
    align-items: stretch;
  }
}

.library-list {
  padding: 4px 0;
}

.doc-row {
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.15s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f8fafc;
  }
}

.doc-thumb {
  flex-shrink: 0;
  width: 108px;
  height: 76px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  background: linear-gradient(135deg, #ecfeff, #f0fdf4);
  display: flex;
  align-items: center;
  justify-content: center;

  &.ext-pdf { background: linear-gradient(135deg, #fef2f2, #fee2e2); }
  &.ext-docx, &.ext-doc { background: linear-gradient(135deg, #eff6ff, #dbeafe); }
  &.ext-ppt, &.ext-pptx { background: linear-gradient(135deg, #fff7ed, #ffedd5); }
  &.ext-xls, &.ext-xlsx { background: linear-gradient(135deg, #f0fdf4, #dcfce7); }
  &.ext-txt { background: linear-gradient(135deg, #f8fafc, #f1f5f9); }
  &.ext-zip, &.ext-rar, &.ext-7z { background: linear-gradient(135deg, #faf5ff, #ede9fe); }
  &.ext-file { background: linear-gradient(135deg, #f8fafc, #e2e8f0); }

  .library-doc-cover {
    width: 100%;
    height: 100%;
  }
}

.doc-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.doc-content {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.doc-content-main {
  flex: 1;
  min-width: 0;
}

.doc-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.doc-summary {
  margin: 0 0 6px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.doc-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.meta-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
  color: #0f766e;
  background: #ecfeff;
  border: 1px solid rgba(15, 118, 110, 0.12);

  &--muted {
    color: #94a3b8;
    background: #f8fafc;
    border-color: #e2e8f0;
  }

  &--chapter {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    vertical-align: bottom;
  }

  &--price {
    color: #ea580c;
    background: #fff7ed;
    border-color: rgba(249, 115, 22, 0.22);
    font-weight: 600;
  }
}

.doc-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .audit-pending {
    color: #d97706;
    font-weight: 600;
  }

  .doc-edit {
    margin: 0;
    padding: 0;
  }
}

@media (max-width: 640px) {
  .doc-row {
    gap: 12px;
    padding: 12px;
  }

  .doc-thumb {
    width: 80px;
    height: 60px;
  }

  .doc-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .doc-stats {
    flex-direction: row;
    align-items: center;
    width: 100%;
  }
}
</style>
