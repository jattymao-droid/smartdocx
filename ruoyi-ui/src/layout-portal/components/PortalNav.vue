<template>
  <div class="portal-nav" :class="{ 'nav--on-media': headerMedia, 'nav--embedded': embedded, 'nav--home-theme': homeTheme }">
    <div class="nav-inner" :class="{ 'portal-container': !embedded }">
      <nav class="nav-links" ref="navLinks">
        <template v-for="item in navItems">
          <router-link
            v-if="!item.children && !item.mega"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ 'nav-link--smart': item.smart, active: isNavItemActive(item) }"
            active-class=""
            :exact="item.exact"
          >
            <span class="nav-link-icon"><i :class="item.icon" /></span>
            <span class="nav-link-text">{{ item.label }}</span>
            <span v-if="item.smart" class="nav-smart-dot" title="AI">AI</span>
          </router-link>
          <el-dropdown
            v-else-if="item.mega"
            :key="item.label + '-mega'"
            trigger="hover"
            placement="bottom-start"
            :show-timeout="60"
            :hide-timeout="180"
            popper-class="portal-nav-mega-popper portal-nav-mega"
            @visible-change="visible => onMegaDropdownVisible(item.label, visible)"
          >
            <span
              class="nav-link nav-link--group"
              :class="{
                active: isMegaActive(item),
                'is-open': megaDropdownOpen === item.label
              }"
            >
              <span class="nav-link-icon"><i :class="item.icon" /></span>
              <span class="nav-link-text">{{ item.label }}</span>
              <i class="el-icon-arrow-down nav-group-arrow" />
            </span>
            <el-dropdown-menu slot="dropdown" class="portal-nav-mega">
              <li class="mega-wrap" role="presentation" @click.stop>
                <div class="mega-panel">
                  <div
                    v-for="(section, idx) in libraryMegaSections"
                    :key="section.title"
                    class="mega-section"
                    :class="{ 'mega-section--last': idx === libraryMegaSections.length - 1 }"
                  >
                    <h4 class="mega-section-title">{{ section.title }}</h4>
                    <div
                      class="mega-section-links"
                      :class="'mega-section-links--' + (section.layout || 'list')"
                    >
                      <a
                        v-for="link in section.links"
                        :key="section.title + '-' + link.label"
                        href="javascript:;"
                        class="mega-link"
                        @click.prevent="onMegaLink(link)"
                      >{{ link.label }}</a>
                    </div>
                  </div>
                </div>
                <div class="mega-foot">
                  <a href="javascript:;" class="mega-foot-link" @click.prevent="onMegaLink({ type: 'path', path: '/library' })">
                    进入文库 <i class="el-icon-arrow-right" />
                  </a>
                </div>
              </li>
            </el-dropdown-menu>
          </el-dropdown>
          <el-dropdown
            v-else-if="item.children"
            :key="item.label"
            trigger="hover"
            placement="bottom-start"
            :show-timeout="60"
            :hide-timeout="180"
            popper-class="portal-nav-mega-popper portal-nav-mega portal-nav-group-mega"
            @visible-change="onGroupDropdownVisible"
          >
            <span
              class="nav-link nav-link--group"
              :class="{
                active: isGroupActive(item),
                'is-open': groupDropdownOpen
              }"
            >
              <span class="nav-link-icon"><i :class="item.icon" /></span>
              <span class="nav-link-text">{{ item.label }}</span>
              <i class="el-icon-arrow-down nav-group-arrow" />
            </span>
            <el-dropdown-menu slot="dropdown" class="portal-nav-mega">
              <li class="mega-wrap" role="presentation" @click.stop>
                <div class="mega-panel">
                  <div class="mega-section mega-section--last">
                    <h4 class="mega-section-title">组卷中心</h4>
                    <div class="mega-section-links mega-section-links--grid mega-section-links--group">
                      <a
                        v-for="child in item.children"
                        :key="child.path"
                        href="javascript:;"
                        class="mega-link"
                        :class="{ 'is-active': isChildActive(child.path) }"
                        @click.prevent="onGroupLink(child)"
                      >
                        <span class="mega-link-label">{{ child.label }}</span>
                        <span v-if="child.smart" class="mega-ai-tag">AI</span>
                      </a>
                    </div>
                  </div>
                </div>
                <div class="mega-foot">
                  <a href="javascript:;" class="mega-foot-link" @click.prevent="onGroupLink({ path: '/paper' })">
                    一键组卷 <i class="el-icon-arrow-right" />
                  </a>
                </div>
              </li>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </nav>
    </div>
  </div>
</template>

<script>
import { fetchSubjectOptionsCached, fetchLibraryCategoriesCached } from '@/utils/metaCache'

const DEFAULT_MEGA_SECTIONS = [
  {
    title: '教育专区',
    layout: 'inline',
    links: [
      { label: '初中', type: 'library', query: { schoolStage: '\u521d\u4e2d' } },
      { label: '高中', type: 'library', query: { schoolStage: '\u9ad8\u4e2d' } },
      { label: '全学段', type: 'path', path: '/library' }
    ]
  },
  {
    title: '教学文档',
    layout: 'grid',
    links: [
      { label: '课件', type: 'library', query: { keyword: '\u8bfe\u4ef6' } },
      { label: '教案', type: 'library', query: { keyword: '\u6559\u6848' } },
      { label: '试卷', type: 'library', query: { keyword: '\u8bd5\u5377' } },
      { label: '讲义', type: 'library', query: { keyword: '\u8bb2\u4e49' } },
      { label: '素材', type: 'library', query: { keyword: '\u7d20\u6750' } },
      { label: '其他', type: 'library', query: { keyword: '\u5176\u4ed6' } }
    ]
  },
  {
    title: '实用文档',
    layout: 'inline',
    links: [
      { label: 'PPT', type: 'library', query: { fileExtFilter: 'pptx' } },
      { label: 'Word', type: 'library', query: { fileExtFilter: 'docx' } },
      { label: 'PDF', type: 'library', query: { fileExtFilter: 'pdf' } }
    ]
  },
  {
    title: '学科资料',
    layout: 'grid',
    links: [
      { label: '语文', type: 'library', query: { keyword: '\u8bed\u6587' } },
      { label: '数学', type: 'library', query: { keyword: '\u6570\u5b66' } },
      { label: '英语', type: 'library', query: { keyword: '\u82f1\u8bed' } },
      { label: '物理', type: 'library', query: { keyword: '\u7269\u7406' } },
      { label: '化学', type: 'library', query: { keyword: '\u5316\u5b66' } },
      { label: '生物', type: 'library', query: { keyword: '\u751f\u7269' } },
      { label: '政治', type: 'library', query: { keyword: '\u653f\u6cbb' } },
      { label: '地理', type: 'library', query: { keyword: '\u5730\u7406' } },
      { label: '历史', type: 'library', query: { keyword: '\u5386\u53f2' } }
    ]
  }
]

export default {
  name: 'PortalNav',
  props: {
    headerMedia: { type: Boolean, default: false },
    embedded: { type: Boolean, default: false },
    homeTheme: { type: Boolean, default: false }
  },
  data() {
    return {
      groupDropdownOpen: false,
      megaDropdownOpen: null,
      megaMenuLoaded: false,
      libraryMegaSections: DEFAULT_MEGA_SECTIONS.map(section => ({
        ...section,
        links: section.links.map(link => ({ ...link }))
      })),
      navItems: [
        { label: '首页', path: '/', icon: 'el-icon-s-home', exact: true },
        { label: '教学文档', icon: 'el-icon-reading', mega: true, path: '/library' },
        { label: '\u70ed\u95e8\u4e13\u9898', path: '/library/topics', icon: 'el-icon-star-on', exact: false },
        {
          label: '组卷',
          icon: 'el-icon-collection',
          children: [
            { label: '章节选题', path: '/chapter', icon: 'el-icon-folder-opened', desc: '按教材目录筛选', smart: false, bg: '#ECFEFF', color: '#0F766E' },
            { label: '知识点选题', path: '/knowledge', icon: 'el-icon-price-tag', desc: '按知识点树定位', smart: false, bg: '#D1FAE5', color: '#047857' },
            { label: '试卷选题', path: '/exam', icon: 'el-icon-document', desc: '从真题试卷选题', smart: false, bg: '#CFFAFE', color: '#0E7490' },
            { label: '\u667a\u80fd\u7ec4\u5377', path: '/paper', icon: 'el-icon-cpu', desc: '试题篮一键出卷', smart: true, bg: '#CCFBF1', color: '#115E59' }
          ]
        },
        { label: '\u6211\u7684\u8bd5\u5377', path: '/my-papers', icon: 'el-icon-folder', exact: false }
      ]
    }
  },
  methods: {
    loadLibraryMegaSections() {
      if (this.megaMenuLoaded) return
      this.megaMenuLoaded = true
      Promise.all([
        fetchLibraryCategoriesCached().catch(() => ({ data: [] })),
        fetchSubjectOptionsCached().catch(() => ({ data: [] }))
      ]).then(([catRes, subRes]) => {
        const categories = catRes.data || []
        const subjects = subRes.data || []
        this.libraryMegaSections = this.buildLibraryMegaSections(categories, subjects)
      })
    },
    buildLibraryMegaSections(categories, subjects) {
      const categoryLinks = categories.slice(0, 9).map(c => ({
        label: this.shortLabel(c.categoryName, 6),
        type: 'library',
        query: { categoryId: String(c.categoryId) }
      }))
      const subjectLinks = subjects.slice(0, 9).map(s => ({
        label: this.shortLabel(s.subjectName, 6),
        type: 'library',
        query: { subjectId: String(s.subjectId) }
      }))
      const sections = DEFAULT_MEGA_SECTIONS.map(section => ({
        ...section,
        links: section.links.map(link => ({ ...link }))
      }))
      if (categoryLinks.length) {
        const idx = sections.findIndex(s => s.title === '教学文档')
        if (idx >= 0) sections[idx].links = categoryLinks
      }
      if (subjectLinks.length) {
        const idx = sections.findIndex(s => s.title === '\u5b66\u79d1\u8d44\u6599')
        if (idx >= 0) sections[idx].links = subjectLinks
      }
      return sections
    },
    shortLabel(text, maxLen = 6) {
      const value = String(text || '').trim()
      if (!value) return ''
      return value.length > maxLen ? value.slice(0, maxLen) : value
    },
    isNavItemActive(item) {
      if (!item || !item.path) return false
      const path = this.$route.path
      if (item.path === '/library/topics') {
        return path === '/library/topics' || path.startsWith('/library/topic/')
      }
      if (item.exact) return path === item.path
      return path === item.path || path.startsWith(item.path + '/')
    },
    onGroupDropdownVisible(visible) {
      this.groupDropdownOpen = visible
    },
    onMegaDropdownVisible(label, visible) {
      this.megaDropdownOpen = visible ? label : null
      if (visible) {
        this.loadLibraryMegaSections()
      }
    },
    onGroupLink(child) {
      const path = child && child.path
      if (path && path !== this.$route.path) {
        this.$router.push(path)
      }
    },
    onMegaLink(link) {
      if (!link) return
      if (link.type === 'path' && link.path) {
        if (this.$route.path !== link.path) {
          this.$router.push(link.path)
        }
        return
      }
      if (link.type === 'library') {
        this.$router.push({ path: '/library', query: link.query || {} })
        return
      }
      if (link.type === 'topic' && link.topicId) {
        this.$router.push('/library/topic/' + link.topicId)
      }
    },
    isGroupActive(item) {
      if (!item || !Array.isArray(item.children)) return false
      return item.children.some(child => this.isChildActive(child.path))
    },
    isMegaActive(item) {
      if (!item || !item.path) return false
      return this.$route.path === item.path || this.$route.path.startsWith(item.path + '/')
    },
    isChildActive(path) {
      if (!path) return false
      return this.$route.path === path || this.$route.path.startsWith(path + '/')
    }
  }
}
</script>

<style scoped lang="scss">
$primary: #0F766E;
$primary-light: #14B8A6;
$accent: #0E7490;

.portal-nav {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 251, 255, 0.95) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid #E2E8F0;
  position: relative;
  transition: background 0.25s ease;

  &.nav--on-media {
    background: rgba(255, 255, 255, 0.55);
    border-bottom-color: rgba(255, 255, 255, 0.3);
  }

  &::before {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    height: 2px;
    background: linear-gradient(90deg, #0F766E, #14B8A6, #0E7490, #0891B2, #0F766E);
    background-size: 200% 100%;
    opacity: 0.75;
    animation: portal-rainbow-shift 10s linear infinite;
  }

  &.nav--embedded {
    flex: 1;
    min-width: 0;
    background: transparent;
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
    border-bottom: none;

    &::before {
      display: none;
    }
  }

  &.nav--home-theme {
    background: transparent;
    border-bottom: none;

    &::before { display: none; }

    .nav-link {
      padding: 14px 16px;
      border-radius: 0;
      border: none;
      color: rgba(255, 255, 255, 0.88);
      font-size: 14px;
      font-weight: 500;

      &:hover:not(.active) {
        color: #fff;
        background: rgba(255, 255, 255, 0.08);
        transform: none;
        border: none;
        box-shadow: none;

        .nav-link-icon {
          background: transparent;
          color: rgba(255, 255, 255, 0.88);
        }
      }

      &.active {
        color: #fff;
        font-weight: 600;
        background: rgba(255, 255, 255, 0.1);
        border: none;
        box-shadow: none;

        .nav-link-icon {
          background: transparent;
          color: #fff;
        }
      }
    }

    .nav-link-icon {
      display: none;
    }

    .nav-link--group {
      &.is-open,
      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.1);
        border: none;
        box-shadow: none;
        transform: none;

        .nav-group-arrow { color: #fff; }
      }
    }

    .nav-group-arrow {
      color: rgba(255, 255, 255, 0.72);
      font-size: 11px;
    }

    .nav-smart-dot { display: none; }
  }
}

@keyframes portal-rainbow-shift {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}

.nav-inner {
  display: flex;
  align-items: center;
  min-height: 50px;
  gap: 14px;
  padding: 8px 0 10px;
}

.nav--embedded .nav-inner {
  min-height: 44px;
  padding: 0;
}

.nav-links {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 6px;
  overflow-x: auto;
  padding: 2px 4px;
  scrollbar-width: none;
  -ms-overflow-style: none;

  &::-webkit-scrollbar { display: none; }
}

.nav--embedded .nav-links {
  justify-content: flex-start;
  gap: 2px;
  padding: 0;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  white-space: nowrap;
  border-radius: 20px;
  position: relative;
  border: 1px solid transparent;
  transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease, transform 0.15s ease;

  &:hover:not(.active) {
    color: $primary;
    background: linear-gradient(135deg, #F0FDFA, #ECFEFF);
    border-color: rgba(15, 118, 110, 0.16);
    transform: translateY(-1px);

    .nav-link-icon {
      background: linear-gradient(135deg, #ECFEFF, #CCFBF1);
      color: $primary;
    }
  }

  &.active {
    color: $primary;
    font-weight: 600;
    background: #ECFEFF;
    border-color: rgba(15, 118, 110, 0.24);
    box-shadow: 0 2px 10px rgba(15, 118, 110, 0.1);

    .nav-link-icon {
      background: #CCFBF1;
      color: $primary;
    }

    .nav-smart-dot {
      background: #CCFBF1;
      color: $primary;
    }
  }
}

.nav--embedded .nav-link {
  padding: 6px 12px;
  border: none;
  border-radius: 16px;
  background: transparent;
  box-shadow: none;
  font-size: 13px;

  &:hover:not(.active) {
    transform: none;
    background: #f3f4f6;
    border: none;

    .nav-link-icon {
      background: transparent;
      color: $primary;
    }
  }

  &.active {
    background: transparent;
    border: none;
    box-shadow: none;
    color: $primary;
    font-weight: 700;

    &::after {
      content: '';
      position: absolute;
      left: 10px;
      right: 10px;
      bottom: -3px;
      height: 2px;
      border-radius: 2px;
      background: linear-gradient(90deg, #0F766E, #0E7490);
    }

    .nav-link-icon {
      background: transparent;
    }
  }
}

.nav-link--group {
  cursor: pointer;
  outline: none;
  user-select: none;

  &.is-open {
    color: $primary;
    background: linear-gradient(135deg, #F0FDFA, #ECFEFF);
    border-color: rgba(15, 118, 110, 0.2);
    box-shadow: 0 4px 14px rgba(15, 118, 110, 0.12);
    transform: translateY(-1px);

    .nav-link-icon {
      background: linear-gradient(135deg, #ECFEFF, #CCFBF1);
      color: $primary;
    }

    .nav-group-arrow {
      color: $primary;
      transform: rotate(180deg);
    }
  }
}

.nav-group-arrow {
  margin-left: 2px;
  font-size: 12px;
  color: #94A3B8;
  transition: transform 0.22s ease, color 0.2s ease;
}

.nav-link--group.active .nav-group-arrow,
.nav-link--group:hover .nav-group-arrow {
  color: $primary;
}

.nav-link--group:hover:not(.is-open) .nav-group-arrow {
  transform: translateY(1px);
}

.nav-link-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #F1F5F9;
  color: #94A3B8;
  transition: background 0.2s, color 0.2s;

  i { font-size: 14px; }
}

.nav--embedded .nav-link-icon {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: transparent;
  color: #94A3B8;

  i { font-size: 13px; }
}

.nav-link-text { line-height: 1; }

.nav-smart-dot {
  padding: 1px 5px;
  border-radius: 6px;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.04em;
  line-height: 1.4;
  color: $accent;
  background: linear-gradient(135deg, #CFFAFE, #CCFBF1);
  flex-shrink: 0;
}

.nav--embedded .nav-smart-dot {
  transform: scale(0.9);
}

.nav-link--smart:not(.active):hover .nav-smart-dot {
  background: linear-gradient(135deg, #99F6E4, #A5F3FC);
}

@media (max-width: 1100px) {
  .nav-link-text { display: none; }
  .nav-link { padding: 7px 10px; }
  .nav-smart-dot { display: none; }
}

@media (max-width: 768px) {
  .nav-inner { gap: 8px; }
}
</style>

<style lang="scss">
$primary: #0F766E;

.el-dropdown-menu.el-popper.portal-nav-mega-popper,
.el-dropdown-menu.portal-nav-mega {
  margin-top: 8px !important;
  padding: 0 !important;
  min-width: 720px !important;
  max-width: 820px !important;
  width: 720px !important;
  border: 1px solid #e2e8f0 !important;
  border-radius: 8px !important;
  background: #fff !important;
  box-shadow: 0 10px 32px rgba(15, 23, 42, 0.12) !important;
  overflow: hidden;

  > li.mega-wrap {
    list-style: none;
    padding: 0;
    margin: 0;
    line-height: normal;
  }

  &[x-placement^='bottom'] .popper__arrow {
    display: none;
  }

  .mega-wrap {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    pointer-events: auto;
  }

  .mega-panel {
    padding: 10px 16px 6px;
  }

  .mega-section {
    padding: 0 0 8px;
    margin: 0 0 8px;
    border-bottom: 1px solid #f1f5f9;

    &--last {
      margin-bottom: 0;
      padding-bottom: 0;
      border-bottom: none;
    }
  }

  .mega-section-title {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0 0 5px;
    padding: 0;
    font-size: 13px;
    font-weight: 600;
    color: #1e3a5f;
    line-height: 1.3;

    &::before {
      content: '';
      flex-shrink: 0;
      width: 3px;
      height: 12px;
      border-radius: 2px;
      background: #2563eb;
    }
  }

  .mega-section-links {
    padding: 0 0 0 9px;

    &--list {
      display: flex;
      flex-direction: column;
      gap: 0;
    }

    &--inline {
      display: flex;
      flex-wrap: wrap;
      gap: 2px 24px;
    }

    &--grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 2px 20px;
    }
  }

  .mega-link {
    display: block;
    padding: 3px 6px;
    font-size: 13px;
    color: #475569;
    text-decoration: none;
    line-height: 1.35;
    border-radius: 4px;
    transition: background 0.12s ease, color 0.12s ease;
    white-space: nowrap;

    &:hover {
      background: #f8fafc;
      color: #2563eb;
    }

    &.is-active {
      background: #eff6ff;
      color: #2563eb;
      font-weight: 600;
    }
  }

  .mega-link-label {
    vertical-align: middle;
  }

  .mega-ai-tag {
    display: inline-block;
    margin-left: 4px;
    padding: 0 5px;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 700;
    color: #2563eb;
    background: #eff6ff;
    line-height: 1.6;
    vertical-align: middle;
  }

  .mega-section-links--group {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 2px 12px;
  }

  .mega-section-links--list .mega-link {
    padding: 3px 6px 3px 0;
  }

  .mega-foot {
    margin: 0;
    padding: 6px 16px 8px;
    border-top: 1px solid #f1f5f9;
    text-align: right;
    background: #fff;
    flex-shrink: 0;
  }

  .mega-foot-link {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #2563eb;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.12s ease;

    &:hover {
      color: #1d4ed8;
    }

    i {
      font-size: 12px;
    }
  }
}

@media (max-width: 900px) {
  .el-dropdown-menu.el-popper.portal-nav-mega-popper,
  .el-dropdown-menu.portal-nav-mega {
    min-width: min(720px, 94vw) !important;
    max-width: 94vw !important;
    width: min(720px, 94vw) !important;

    .mega-section-links--grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .mega-section-links--group {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
}

.el-dropdown-menu.el-popper.portal-nav-group-mega {
  min-width: 520px !important;
  max-width: 560px !important;
  width: 520px !important;
}

@media (max-width: 900px) {
  .el-dropdown-menu.el-popper.portal-nav-group-mega {
    min-width: min(520px, 94vw) !important;
    max-width: 94vw !important;
    width: min(520px, 94vw) !important;
  }
}

.el-dropdown-menu.el-popper.portal-nav-topic-mega {
  min-width: 420px !important;
  max-width: 480px !important;
  width: 420px !important;

  .mega-section-links--topic {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .mega-topic-empty {
    margin: 0;
    padding: 8px 0 4px;
    font-size: 13px;
    color: #94A3B8;
  }
}

@media (max-width: 900px) {
  .el-dropdown-menu.el-popper.portal-nav-topic-mega {
    min-width: min(420px, 94vw) !important;
    max-width: 94vw !important;
    width: min(420px, 94vw) !important;

    .mega-section-links--topic {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
}
</style>
