import request from '@/utils/request'

export const DEFAULT_PORTAL_BANNER = {
  mode: 'none',
  imageUrl: '',
  videoUrl: '',
  videoPoster: '',
  overlay: 0.42,
  objectFit: 'cover',
  headerMode: 'none',
  headerImageUrl: '',
  headerOverlay: 0.4,
  heroTitle: '',
  heroDesc: '',
  slides: []
}

export const DEFAULT_HERO_SLIDES = [
  {
    title: '',
    desc: '覆盖课件、教案、试卷、讲义，支持在线预览与分类筛选。',
    bg: 'linear-gradient(180deg, #1e4d8c 0%, #2563b8 42%, #1d4f91 100%)',
    imageUrl: ''
  },
  {
    title: '教学资源一站式检索',
    desc: '按学科、分类、格式筛选，快速定位教学场景所需内容。',
    bg: 'linear-gradient(180deg, #1a4480 0%, #1d4ed8 50%, #2563eb 100%)',
    imageUrl: ''
  },
  {
    title: '资源共享与协作共建',
    desc: '支持上传分享、收藏续读和热门排行，打造校内文库生态。',
    bg: 'linear-gradient(180deg, #1e3a8a 0%, #2563eb 55%, #3b82f6 100%)',
    imageUrl: ''
  }
]

function normalizeSlide(item) {
  if (!item || typeof item !== 'object') return null
  const title = String(item.title || '').trim()
  const desc = String(item.desc || '').trim()
  const bg = String(item.bg || '').trim()
  const imageUrl = String(item.imageUrl || '').trim()
  if (!bg && !imageUrl) return null
  return { title, desc, bg, imageUrl }
}

export function normalizeHeroSlides(raw) {
  const list = Array.isArray(raw) ? raw : []
  const slides = list.map(normalizeSlide).filter(Boolean)
  return slides.length ? slides : DEFAULT_HERO_SLIDES.map(s => ({ ...s }))
}

export function buildHeroSlidesFromBanner(banner) {
  const cfg = banner || {}
  const mode = cfg.mode || 'none'
  const heroTitle = String(cfg.heroTitle || '').trim()
  const heroDesc = String(cfg.heroDesc || '').trim()

  if (mode === 'image' && cfg.imageUrl) {
    return [{
      title: heroTitle || DEFAULT_HERO_SLIDES[0].title,
      desc: heroDesc || DEFAULT_HERO_SLIDES[0].desc,
      bg: '',
      imageUrl: cfg.imageUrl
    }]
  }

  if (mode === 'video') {
    return [{
      title: heroTitle || DEFAULT_HERO_SLIDES[0].title,
      desc: heroDesc || DEFAULT_HERO_SLIDES[0].desc,
      bg: DEFAULT_HERO_SLIDES[0].bg,
      imageUrl: ''
    }]
  }

  return normalizeHeroSlides(cfg.slides)
}

const LOCAL_MEDIA_MARKERS = ['/statics/', '/profile/upload/', '/profile/']
const LOCAL_FILE_HOST_RE = /^https?:\/\/(?:127\.0\.0\.1|localhost)(?::\d+)?/i

function fileServiceBase() {
  const raw = process.env.VUE_APP_FILE_BASE
  return raw ? String(raw).replace(/\/$/, '') : ''
}

/** Strip file-service host, keep /statics/... or /profile/upload/... */
export function normalizePortalMediaPath(url) {
  if (!url) return ''
  let src = String(url).trim()
  if (!src) return ''

  if (LOCAL_FILE_HOST_RE.test(src)) {
    try {
      const parsed = new URL(src)
      src = parsed.pathname + (parsed.search || '')
    } catch (e) {
      // keep original
    }
  }

  for (const marker of LOCAL_MEDIA_MARKERS) {
    const idx = src.indexOf(marker)
    if (idx >= 0) {
      return src.slice(idx)
    }
  }

  if (src.startsWith('/') && !src.startsWith('//')) {
    return src
  }

  return src
}

export function isLocalPortalMediaUrl(url) {
  const path = normalizePortalMediaPath(url)
  return LOCAL_MEDIA_MARKERS.some(marker => path.startsWith(marker))
}

export function bannerHasMedia(banner) {
  if (!banner) return false
  if (banner.mode === 'image') return !!banner.imageUrl
  if (banner.mode === 'video') return !!banner.videoUrl
  return false
}

export function headerBannerHasImage(banner) {
  if (!banner) return false
  return banner.headerMode === 'image' && !!banner.headerImageUrl
}

export function resolvePortalMediaUrl(url, apiBase = process.env.VUE_APP_BASE_API) {
  if (!url) return ''
  const src = String(url).trim()
  const base = (apiBase || '').replace(/\/$/, '')

  if (src.includes('/system/portal/banner/media?')) {
    if (src.startsWith('http://') || src.startsWith('https://')) {
      return src
    }
    if (base && src.startsWith(base + '/')) {
      return src
    }
    if (src.startsWith('/')) {
      return base + src
    }
  }

  const localPath = normalizePortalMediaPath(src)

  if (localPath.startsWith('/profile/')) {
    // In RuoYi-Cloud, /profile/** is served by ruoyi-system behind /system/**.
    return base + '/system' + localPath
  }

  if (LOCAL_MEDIA_MARKERS.some(marker => localPath.startsWith(marker))) {
    const fileBase = fileServiceBase()
    if (fileBase) {
      return fileBase + localPath
    }
    return base + localPath
  }

  if (src.startsWith('/') && !src.startsWith('//')) {
    return base + src
  }

  if (/^https?:\/\//i.test(src)) {
    return base + '/system/portal/banner/media?url=' + encodeURIComponent(src)
  }

  if (src.startsWith('data:') || src.startsWith('blob:')) {
    return src
  }

  return base + '/' + src.replace(/^\/+/, '')
}

export function buildPortalPdfFetchUrl(url, apiBase = process.env.VUE_APP_BASE_API) {
  const src = resolvePortalMediaUrl(url, apiBase)
  if (!src) return ''
  const q = src.indexOf('?')
  const path = q >= 0 ? src.slice(0, q) : src
  const query = q >= 0 ? src.slice(q) : ''
  return path.replace(/\+/g, '%2B') + query
}

let bannerCache = null

export function clearPortalBannerCache() {
  bannerCache = null
}

export function loadPortalBannerConfig() {
  if (bannerCache) {
    return bannerCache
  }
  bannerCache = request({
    url: '/system/portal/banner',
    method: 'get',
    headers: { isToken: false }
  }).then(res => {
    const data = (res && res.data) || {}
    return {
      mode: data.mode || DEFAULT_PORTAL_BANNER.mode,
      imageUrl: data.imageUrl || '',
      videoUrl: data.videoUrl || '',
      videoPoster: data.videoPoster || '',
      overlay: typeof data.overlay === 'number' ? data.overlay : DEFAULT_PORTAL_BANNER.overlay,
      objectFit: data.objectFit || DEFAULT_PORTAL_BANNER.objectFit,
      headerMode: data.headerMode === 'image' ? 'image' : 'none',
      headerImageUrl: data.headerImageUrl || '',
      headerOverlay: typeof data.headerOverlay === 'number' ? data.headerOverlay : DEFAULT_PORTAL_BANNER.headerOverlay,
      heroTitle: data.heroTitle || '',
      heroDesc: data.heroDesc || '',
      slides: normalizeHeroSlides(data.slides)
    }
  }).catch(() => ({
    ...DEFAULT_PORTAL_BANNER,
    slides: DEFAULT_HERO_SLIDES.map(s => ({ ...s }))
  }))
  return bannerCache
}
