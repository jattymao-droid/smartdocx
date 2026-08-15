/**
 * Rewrite PortalNav.vue (ASCII-safe). Run: node scripts/write-portal-nav.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/layout-portal/components/PortalNav.vue')

const L = {
  pickSubject: '\u9009\u62e9\u5b66\u79d1',
  home: '\u9996\u9875',
  chapter: '\u7ae0\u8282\u9009\u9898',
  knowledge: '\u77e5\u8bc6\u70b9\u9009\u9898',
  exam: '\u8bd5\u5377\u9009\u9898',
  paper: '\u667a\u80fd\u7ec4\u5377',
  myPapers: '\u6211\u7684\u8bd5\u5377',
  primary: '\u5c0f\u5b66',
  junior: '\u521d\u4e2d',
  high: '\u9ad8\u4e2d',
  vocational: '\u4e2d\u804c'
}

const content = `<template>
  <div class="portal-nav">
    <div class="portal-container nav-inner">
      <div class="subject-dropdown" @mouseenter="panelOpen = true" @mouseleave="panelOpen = false">
        <button type="button" class="subject-btn">
          {{ currentSubjectLabel }}
          <i class="el-icon-arrow-down" />
        </button>
        <div v-show="panelOpen" class="subject-panel portal-card">
          <div class="stage-tabs">
            <span
              v-for="s in stages"
              :key="s"
              :class="['stage-tab', { active: schoolStage === s }]"
              @click="pickStage(s)"
            >{{ s }}</span>
          </div>
          <div class="subject-grid">
            <span
              v-for="item in subjectOptions"
              :key="item.subjectId"
              :class="['subject-item', { active: subjectId === item.subjectId }]"
              @click="pickSubject(item)"
            >{{ item.subjectName }}</span>
          </div>
        </div>
      </div>
      <nav class="nav-links">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          active-class="active"
          exact
        >
          <i :class="item.icon" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </div>
  </div>
</template>

<script>
import { listSubject } from '@/api/education/subject'

export default {
  name: 'PortalNav',
  props: {
    subjectId: { type: [Number, String], default: null },
    schoolStage: { type: String, default: '${L.high}' }
  },
  data() {
    return {
      panelOpen: false,
      subjectOptions: [],
      stages: ['${L.primary}', '${L.junior}', '${L.high}', '${L.vocational}'],
      navItems: [
        { label: '${L.home}', path: '/portal/home', icon: 'el-icon-s-home' },
        { label: '${L.chapter}', path: '/portal/chapter', icon: 'el-icon-folder-opened' },
        { label: '${L.knowledge}', path: '/portal/knowledge', icon: 'el-icon-price-tag' },
        { label: '${L.exam}', path: '/portal/exam', icon: 'el-icon-document' },
        { label: '${L.paper}', path: '/portal/paper', icon: 'el-icon-cpu' },
        { label: '${L.myPapers}', path: '/portal/my-papers', icon: 'el-icon-folder' }
      ]
    }
  },
  computed: {
    currentSubjectLabel() {
      const hit = this.subjectOptions.find(s => s.subjectId === this.subjectId)
      return hit ? hit.subjectName : '${L.pickSubject}'
    }
  },
  created() {
    this.loadSubjects()
  },
  methods: {
    loadSubjects() {
      listSubject({ pageNum: 1, pageSize: 100, status: '0' }).then(res => {
        this.subjectOptions = res.rows || []
        if (!this.subjectId && this.subjectOptions.length) {
          const physics = this.subjectOptions.find(s => (s.subjectName || '').includes('\u7269\u7406'))
          this.$emit('update:subjectId', (physics || this.subjectOptions[0]).subjectId)
        }
      })
    },
    pickStage(stage) {
      this.$emit('update:schoolStage', stage)
    },
    pickSubject(item) {
      this.$emit('update:subjectId', item.subjectId)
      this.panelOpen = false
      const path = this.$route.path
      let target = '/portal/chapter'
      if (path === '/portal/knowledge') target = '/portal/knowledge'
      else if (path === '/portal/exam') target = '/portal/exam'
      this.$router.push({
        path: target,
        query: { ...this.$route.query, subjectId: item.subjectId }
      })
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #2563EB;
$primary-light: #3B82F6;

.portal-nav {
  background: #fff;
  border-bottom: 1px solid #E2E8F0;
  box-shadow: none;
}

.nav-inner {
  display: flex;
  align-items: center;
  min-height: 52px;
  gap: 8px;
  padding: 8px 0;
}

.subject-dropdown { position: relative; flex-shrink: 0; }

.subject-btn {
  height: 40px;
  min-width: 120px;
  padding: 0 16px;
  border: 1.5px solid $primary;
  border-radius: 10px;
  background: #fff;
  color: $primary;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: none;
  transition: background 0.15s, color 0.15s;
  &:hover { background: #EFF6FF; }
}

.subject-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 2000;
  width: 480px;
  padding: 14px 16px 16px;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(37, 99, 235, 0.16);
}

.stage-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
}

.stage-tab {
  padding: 5px 14px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  border-radius: 20px;
  &.active { color: #1D4ED8; background: #DBEAFE; font-weight: 600; }
}

.subject-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.subject-item {
  padding: 8px;
  font-size: 13px;
  text-align: center;
  border-radius: 8px;
  cursor: pointer;
  color: #334155;
  &:hover { background: #EFF6FF; color: $primary; }
  &.active { background: $primary; color: #fff; }
}

.nav-links {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 4px;
  overflow-x: auto;
  padding-left: 8px;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  color: #64748b;
  font-size: 14px;
  text-decoration: none;
  white-space: nowrap;
  border-radius: 22px;
  transition: all 0.2s ease;
  i { font-size: 15px; opacity: 0.85; }
  &:hover {
    color: $primary;
    background: #EFF6FF;
    transform: translateY(-1px);
  }
  &.active {
    color: $primary;
    font-weight: 600;
    background: #EFF6FF;
    border: 1px solid rgba(37, 99, 235, 0.28);
    box-shadow: none;
    i { opacity: 1; color: $primary; }
  }
}
</style>
`

fs.writeFileSync(file, content, 'utf8')
console.log('wrote PortalNav.vue')
