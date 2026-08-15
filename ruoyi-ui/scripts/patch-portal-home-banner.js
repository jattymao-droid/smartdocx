/**
 * Add configurable banner image/video to portal Home.vue
 * Run: node scripts/patch-portal-home-banner.js
 */
const fs = require('fs')
const path = require('path')

const file = path.join(__dirname, '../src/views/portal/Home.vue')
let text = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n')

const templateOld = `    <section class="home-hero">
      <div class="hero-mesh" aria-hidden="true" />
      <div class="hero-bg-shape hero-bg-a" aria-hidden="true" />
      <div class="hero-bg-shape hero-bg-b" aria-hidden="true" />
      <div class="portal-container hero-inner">`

const templateNew = `    <section
      class="home-hero"
      :class="{ 'home-hero--media': heroHasMedia, 'home-hero--video': banner.mode === 'video' }"
    >
      <div v-if="banner.mode === 'video' && bannerVideoUrl" class="hero-media hero-media--video">
        <video
          :key="bannerVideoUrl"
          class="hero-media-el"
          :src="bannerVideoUrl"
          :poster="bannerPosterUrl || undefined"
          :style="{ objectFit: banner.objectFit || 'cover' }"
          autoplay
          muted
          loop
          playsinline
          preload="metadata"
        />
      </div>
      <div
        v-else-if="banner.mode === 'image' && bannerImageUrl"
        class="hero-media hero-media--image"
        :style="{
          backgroundImage: 'url(' + bannerImageUrl + ')',
          backgroundSize: banner.objectFit || 'cover',
          backgroundPosition: 'center'
        }"
      />
      <div
        v-if="heroHasMedia && banner.overlay > 0"
        class="hero-overlay"
        :style="{ opacity: banner.overlay }"
        aria-hidden="true"
      />
      <div v-if="!heroHasMedia" class="hero-mesh" aria-hidden="true" />
      <div v-if="!heroHasMedia" class="hero-bg-shape hero-bg-a" aria-hidden="true" />
      <div v-if="!heroHasMedia" class="hero-bg-shape hero-bg-b" aria-hidden="true" />
      <div class="portal-container hero-inner">`

if (!text.includes(templateOld)) {
  console.error('template block not found')
  process.exit(1)
}
text = text.replace(templateOld, templateNew)

const importOld = `import { homeLabels } from './portal-home-labels'`
const importNew = `import { homeLabels } from './portal-home-labels'
import {
  DEFAULT_PORTAL_BANNER,
  bannerHasMedia,
  loadPortalBannerConfig,
  resolvePortalMediaUrl
} from '@/utils/portalBanner'`

if (!text.includes(importOld)) {
  console.error('import block not found')
  process.exit(1)
}
text = text.replace(importOld, importNew)

const dataOld = `      labels: homeLabels,
      tipIndex: 0,`
const dataNew = `      labels: homeLabels,
      banner: { ...DEFAULT_PORTAL_BANNER },
      tipIndex: 0,`
text = text.replace(dataOld, dataNew)

const computedOld = `  computed: {
    ...mapGetters(['token']),
    smartTip() {`
const computedNew = `  computed: {
    ...mapGetters(['token']),
    heroHasMedia() {
      return bannerHasMedia(this.banner)
    },
    bannerImageUrl() {
      return resolvePortalMediaUrl(this.banner.imageUrl)
    },
    bannerVideoUrl() {
      return resolvePortalMediaUrl(this.banner.videoUrl)
    },
    bannerPosterUrl() {
      return resolvePortalMediaUrl(this.banner.videoPoster)
    },
    smartTip() {`
text = text.replace(computedOld, computedNew)

const createdOld = `  created() {
    this.loadStats()`
const createdNew = `  created() {
    this.loadBannerConfig()
    this.loadStats()`
text = text.replace(createdOld, createdNew)

const methodsOld = `  methods: {
    loadStats() {`
const methodsNew = `  methods: {
    loadBannerConfig() {
      loadPortalBannerConfig().then(cfg => {
        this.banner = cfg
      }).catch(() => {})
    },
    loadStats() {`
text = text.replace(methodsOld, methodsNew)

const styleAnchor = `.home-hero {
  position: relative;
  padding: 36px 0 44px;
  margin-bottom: 8px;
  overflow: hidden;
}`

const styleNew = `.home-hero {
  position: relative;
  padding: 36px 0 44px;
  margin-bottom: 8px;
  overflow: hidden;
}

.hero-media {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.hero-media--video .hero-media-el {
  width: 100%;
  height: 100%;
  display: block;
}

.hero-media--image {
  background-repeat: no-repeat;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  z-index: 0;
  background: linear-gradient(125deg, rgba(15, 23, 42, 0.78), rgba(15, 23, 42, 0.35));
  pointer-events: none;
}

.home-hero--media {
  min-height: 380px;

  .hero-inner { z-index: 1; }

  .hero-title {
    color: #fff;
    text-shadow: 0 2px 16px rgba(0, 0, 0, 0.25);
    em { color: #BFDBFE; }
  }

  .hero-desc {
    color: rgba(255, 255, 255, 0.9);
    text-shadow: 0 1px 8px rgba(0, 0, 0, 0.2);
  }

  .hero-badge.smart {
    background: rgba(255, 255, 255, 0.14);
    border-color: rgba(255, 255, 255, 0.28);
    color: #fff;
  }

  .hero-tag {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.22);
    color: rgba(255, 255, 255, 0.92);
    i { color: rgba(255, 255, 255, 0.78); }
  }

  .hero-panel {
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(10px);
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.18);
  }
}`

if (!text.includes(styleAnchor)) {
  console.error('style block not found')
  process.exit(1)
}
text = text.replace(styleAnchor, styleNew)

fs.writeFileSync(file, text, 'utf8')
console.log('Home.vue patched for banner media')
