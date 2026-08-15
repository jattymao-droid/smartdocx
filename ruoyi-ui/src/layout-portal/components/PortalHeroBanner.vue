<template>
  <div class="hero-banner-bg" aria-hidden="true">
    <!-- ?????????? / ??? -->
    <template v-if="hasMedia">
      <video
        v-if="banner.mode === 'video' && videoUrl"
        :key="videoUrl"
        class="hero-banner-media"
        :src="videoUrl"
        :poster="posterUrl || undefined"
        :style="{ objectFit: banner.objectFit || 'cover' }"
        autoplay
        muted
        loop
        playsinline
        preload="metadata"
      />
      <div
        v-else-if="banner.mode === 'image' && imageUrl"
        class="hero-banner-media hero-banner-media--image"
        :style="{
          backgroundImage: 'url(' + imageUrl + ')',
          backgroundSize: banner.objectFit || 'cover',
          backgroundPosition: 'center'
        }"
      />
    </template>

    <!-- ????????????? -->
    <el-carousel
      v-else
      class="hero-banner-carousel"
      height="100%"
      :interval="6000"
      arrow="never"
      indicator-position="inside"
    >
      <el-carousel-item v-for="(slide, i) in defaultSlides" :key="i">
        <div class="hero-banner-slide" :style="{ background: slide.bg }" />
      </el-carousel-item>
    </el-carousel>

    <div
      class="hero-banner-shade"
      :style="shadeStyle"
    />
  </div>
</template>

<script>
import {
  DEFAULT_PORTAL_BANNER,
  bannerHasMedia,
  loadPortalBannerConfig,
  resolvePortalMediaUrl
} from '@/utils/portalBanner'

export default {
  name: 'PortalHeroBanner',
  data() {
    return {
      banner: { ...DEFAULT_PORTAL_BANNER },
      defaultSlides: [
        { bg: 'linear-gradient(125deg, #1E3A8A 0%, #2563EB 42%, #3B82F6 100%)' },
        { bg: 'linear-gradient(125deg, #0F766E 0%, #0D9488 48%, #14B8A6 100%)' },
        { bg: 'linear-gradient(125deg, #5B21B6 0%, #7C3AED 48%, #8B5CF6 100%)' }
      ]
    }
  },
  computed: {
    hasMedia() {
      return bannerHasMedia(this.banner)
    },
    imageUrl() {
      return resolvePortalMediaUrl(this.banner.imageUrl)
    },
    videoUrl() {
      return resolvePortalMediaUrl(this.banner.videoUrl)
    },
    posterUrl() {
      return resolvePortalMediaUrl(this.banner.videoPoster)
    },
    shadeStyle() {
      const overlay = this.hasMedia && typeof this.banner.overlay === 'number'
        ? this.banner.overlay
        : 0.52
      return { opacity: overlay }
    }
  },
  created() {
    this.loadConfig()
  },
  methods: {
    loadConfig() {
      loadPortalBannerConfig().then(cfg => {
        this.banner = cfg
        this.$emit('ready', cfg)
      }).catch(() => {
        this.$emit('ready', this.banner)
      })
    }
  }
}
</script>

<style scoped lang="scss">
.hero-banner-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.hero-banner-media {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.hero-banner-media--image {
  height: 100%;
  background-repeat: no-repeat;
}

.hero-banner-carousel {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;

  ::v-deep .el-carousel__container {
    height: 100% !important;
  }

  ::v-deep .el-carousel__indicators--inside {
    bottom: 18px;

    .el-carousel__button {
      width: 18px;
      height: 3px;
      border-radius: 2px;
      background: rgba(255, 255, 255, 0.45);
      opacity: 1;
    }

    .is-active .el-carousel__button {
      background: #fff;
      width: 26px;
    }
  }
}

.hero-banner-slide {
  width: 100%;
  height: 100%;
}

.hero-banner-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    rgba(15, 23, 42, 0.72) 0%,
    rgba(15, 23, 42, 0.45) 45%,
    rgba(15, 23, 42, 0.28) 100%
  );
  pointer-events: none;
}
</style>
