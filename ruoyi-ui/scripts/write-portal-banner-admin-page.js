/**
 * Create portal banner admin page (education/portal/banner/index.vue)
 * Run: node scripts/write-portal-banner-admin-page.js
 */
const fs = require('fs')
const path = require('path')

const outDir = path.join(__dirname, '../src/views/education/portal/banner')
const outFile = path.join(outDir, 'index.vue')

const content = `<template>
  <div class="app-container portal-banner-admin">
    <el-card v-loading="loading" shadow="never">
      <div slot="header" class="card-header">
        <span>${'\u95e8\u6237\u9996\u9875 Banner \u914d\u7f6e'}</span>
        <el-button type="text" icon="el-icon-view" @click="openPortal">${'\u9884\u89c8\u95e8\u6237\u9996\u9875'}</el-button>
      </div>

      <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small">
        <el-form-item label="${'\u5c55\u793a\u6a21\u5f0f'}" prop="mode">
          <el-radio-group v-model="form.mode">
            <el-radio label="none">${'\u9ed8\u8ba4\u6e10\u53d8\u80cc\u666f'}</el-radio>
            <el-radio label="image">${'\u80cc\u666f\u56fe\u7247'}</el-radio>
            <el-radio label="video">${'\u80cc\u666f\u89c6\u9891'}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.mode === 'image'" label="${'\u80cc\u666f\u56fe\u7247'}" prop="imageUrl">
          <div class="media-source">
            <div class="media-source__block">
              <div class="media-source__label">${'\u672c\u5730\u4e0a\u4f20'}</div>
              <image-upload v-model="form.imageUrl" :limit="1" :file-size="10" />
            </div>
            <div class="media-source__block media-source__block--url">
              <div class="media-source__label">${'\u6216\u586b\u5199\u7f51\u5740'}</div>
              <el-input
                v-model="form.imageUrl"
                clearable
                :placeholder="urlPlaceholder.image"
                @blur="trimField('imageUrl')"
              >
                <template slot="prepend">URL</template>
              </el-input>
              <div class="form-tip">${'\u652f\u6301 https \u94fe\u63a5\u3001\u7ad9\u5185\u8def\u5f84\uff08\u5982 /profile/upload/...\uff09'}</div>
            </div>
          </div>
        </el-form-item>

        <template v-if="form.mode === 'video'">
          <el-form-item label="${'\u80cc\u666f\u89c6\u9891'}" prop="videoUrl">
            <div class="media-source">
              <div class="media-source__block">
                <div class="media-source__label">${'\u672c\u5730\u4e0a\u4f20'}</div>
                <file-upload v-model="form.videoUrl" :limit="1" :file-size="100" :file-type="['mp4', 'webm']" />
                <div class="form-tip">${'\u5efa\u8bae mp4 \u683c\u5f0f\uff0c\u6587\u4ef6\u8f83\u5927\u65f6\u4e0a\u4f20\u53ef\u80fd\u9700\u8981\u7b49\u5f85\u4e00\u4f1a\u513f'}</div>
              </div>
              <div class="media-source__block media-source__block--url">
                <div class="media-source__label">${'\u6216\u586b\u5199\u7f51\u5740'}</div>
                <el-input
                  v-model="form.videoUrl"
                  clearable
                  :placeholder="urlPlaceholder.video"
                  @blur="trimField('videoUrl')"
                >
                  <template slot="prepend">URL</template>
                </el-input>
                <div class="form-tip">${'\u8bf7\u586b\u5199\u53ef\u76f4\u63a5\u8bbf\u95ee\u7684 .mp4 \u76f4\u94fe\uff08\u4ee5 https://...\/.mp4 \u7ed3\u5c3e\uff09\uff1b\u50cf Pixabay \u201c\u4e0b\u8f7d\u9875\u201d\u94fe\u63a5\u901a\u5e38\u65e0\u6cd5\u64ad\u653e\uff0c\u5efa\u8bae\u672c\u5730\u4e0a\u4f20'}</div>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="${'\u89c6\u9891\u5c01\u9762'}" prop="videoPoster">
            <div class="media-source">
              <div class="media-source__block">
                <div class="media-source__label">${'\u672c\u5730\u4e0a\u4f20'}</div>
                <image-upload v-model="form.videoPoster" :limit="1" :file-size="10" />
              </div>
              <div class="media-source__block media-source__block--url">
                <div class="media-source__label">${'\u6216\u586b\u5199\u7f51\u5740'}</div>
                <el-input
                  v-model="form.videoPoster"
                  clearable
                  :placeholder="urlPlaceholder.poster"
                  @blur="trimField('videoPoster')"
                >
                  <template slot="prepend">URL</template>
                </el-input>
                <div class="form-tip">${'\u53ef\u9009\uff1b\u7559\u7a7a\u5219\u4f7f\u7528\u89c6\u9891\u9996\u5e27\u6216\u9ed8\u8ba4\u80cc\u666f'}</div>
              </div>
            </div>
          </el-form-item>
        </template>

        <el-form-item label="${'\u906e\u7f69\u6d53\u5ea6'}" prop="overlay">
          <el-slider v-model="form.overlay" :min="0" :max="1" :step="0.01" show-input :show-input-controls="false" style="max-width: 420px" />
          <div class="form-tip">${'\u7528\u4e8e\u8c03\u6574\u80cc\u666f\u4e0a\u6587\u5b57\u53ef\u8bfb\u6027\uff0c\u5efa\u8bae 0.3 \u2013 0.55'}</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submitForm" v-hasPermi="['education:portal:banner:edit']">${'\u4fdd\u5b58\u914d\u7f6e'}</el-button>
          <el-button @click="loadConfig">${'\u91cd\u65b0\u52a0\u8f7d'}</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">${'\u6548\u679c\u9884\u89c8'}</el-divider>
      <div class="preview-box" :class="{ 'preview-box--media': previewHasMedia }">
        <video
          v-if="form.mode === 'video' && form.videoUrl"
          class="preview-media"
          :src="resolveUrl(form.videoUrl)"
          :poster="form.videoPoster ? resolveUrl(form.videoPoster) : undefined"
          muted
          loop
          autoplay
          playsinline
        />
        <div
          v-else-if="form.mode === 'image' && form.imageUrl"
          class="preview-media preview-media--image"
          :style="{ backgroundImage: 'url(' + resolveUrl(form.imageUrl) + ')' }"
        />
        <div v-if="previewHasMedia && form.overlay > 0" class="preview-overlay" :style="{ opacity: form.overlay }" />
        <div class="preview-content">
          <h3>${'\u6559\u5e08\u95e8\u6237\u9996\u9875\u9884\u89c8'}</h3>
          <p>${'\u4fdd\u5b58\u540e\u5237\u65b0\u95e8\u6237\u9996\u9875\u5373\u53ef\u770b\u5230\u6548\u679c'}</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getPortalBannerAdminConfig, updatePortalBannerAdminConfig } from '@/api/education/portalBanner'
import { resolvePortalMediaUrl } from '@/utils/portalBanner'

const MEDIA_URL_PATTERN = /^(https?:\\/\\/|\\/\\/|\\/|data:|blob:)/i

export default {
  name: 'PortalBannerAdmin',
  data() {
    return {
      loading: false,
      saving: false,
      urlPlaceholder: {
        image: 'https://example.com/banner.jpg \u6216 /profile/upload/2026/01/01/banner.png',
        video: 'https://example.com/intro.mp4 \u6216 /profile/upload/2026/01/01/intro.mp4',
        poster: 'https://example.com/poster.jpg \u6216 /profile/upload/2026/01/01/poster.png'
      },
      form: {
        mode: 'none',
        imageUrl: '',
        videoUrl: '',
        videoPoster: '',
        overlay: 0.42
      },
      rules: {
        mode: [{ required: true, message: '${'\u8bf7\u9009\u62e9\u5c55\u793a\u6a21\u5f0f'}', trigger: 'change' }],
        imageUrl: [{
          validator: (rule, value, callback) => {
            if (this.form.mode !== 'image') {
              callback()
              return
            }
            if (!value) {
              callback(new Error('${'\u8bf7\u4e0a\u4f20\u6216\u586b\u5199\u80cc\u666f\u56fe\u7247\u5730\u5740'}'))
              return
            }
            if (!this.isValidMediaUrl(value)) {
              callback(new Error('${'\u8bf7\u586b\u5199\u6709\u6548\u7684\u56fe\u7247\u5730\u5740\uff08http/https \u6216 / \u5f00\u5934\u7684\u8def\u5f84\uff09'}'))
              return
            }
            callback()
          },
          trigger: 'blur'
        }],
        videoUrl: [{
          validator: (rule, value, callback) => {
            if (this.form.mode !== 'video') {
              callback()
              return
            }
            if (!value) {
              callback(new Error('${'\u8bf7\u4e0a\u4f20\u6216\u586b\u5199\u80cc\u666f\u89c6\u9891\u5730\u5740'}'))
              return
            }
            if (!this.isValidMediaUrl(value)) {
              callback(new Error('${'\u8bf7\u586b\u5199\u6709\u6548\u7684\u89c6\u9891\u5730\u5740\uff08http/https \u6216 / \u5f00\u5934\u7684\u8def\u5f84\uff09'}'))
              return
            }
            callback()
          },
          trigger: 'blur'
        }],
        videoPoster: [{
          validator: (rule, value, callback) => {
            if (this.form.mode !== 'video' || !value) {
              callback()
              return
            }
            if (!this.isValidMediaUrl(value)) {
              callback(new Error('${'\u8bf7\u586b\u5199\u6709\u6548\u7684\u5c01\u9762\u5730\u5740\uff08http/https \u6216 / \u5f00\u5934\u7684\u8def\u5f84\uff09'}'))
              return
            }
            callback()
          },
          trigger: 'blur'
        }]
      }
    }
  },
  computed: {
    previewHasMedia() {
      if (this.form.mode === 'image') {
        return !!this.form.imageUrl
      }
      if (this.form.mode === 'video') {
        return !!this.form.videoUrl
      }
      return false
    }
  },
  created() {
    this.loadConfig()
  },
  methods: {
    loadConfig() {
      this.loading = true
      getPortalBannerAdminConfig().then(res => {
        const data = res.data || {}
        this.form = {
          mode: data.mode || 'none',
          imageUrl: data.imageUrl || '',
          videoUrl: data.videoUrl || '',
          videoPoster: data.videoPoster || '',
          overlay: typeof data.overlay === 'number' ? data.overlay : 0.42
        }
      }).finally(() => {
        this.loading = false
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.saving = true
        updatePortalBannerAdminConfig({
          ...this.form,
          imageUrl: (this.form.imageUrl || '').trim(),
          videoUrl: (this.form.videoUrl || '').trim(),
          videoPoster: (this.form.videoPoster || '').trim()
        }).then(() => {
          this.$modal.msgSuccess('${'\u4fdd\u5b58\u6210\u529f'}')
          this.loadConfig()
        }).finally(() => {
          this.saving = false
        })
      })
    },
    openPortal() {
      window.open('/portal/home', '_blank')
    },
    trimField(field) {
      if (!this.form[field]) {
        return
      }
      this.form[field] = String(this.form[field]).trim()
    },
    isValidMediaUrl(value) {
      const raw = String(value || '').trim()
      if (!raw) {
        return false
      }
      if (MEDIA_URL_PATTERN.test(raw)) {
        return true
      }
      return !/[\\s\u0000]/.test(raw)
    },
    resolveUrl(url) {
      return resolvePortalMediaUrl(url)
    }
  }
}
</script>

<style scoped>
.portal-banner-admin .card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.media-source {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  width: 100%;
}
.media-source__block {
  flex: 1 1 280px;
  min-width: 260px;
}
.media-source__block--url {
  max-width: 520px;
}
.media-source__label {
  color: #606266;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}
.preview-box {
  position: relative;
  min-height: 220px;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 50%, #bfdbfe 100%);
}
.preview-box--media {
  background: #0f172a;
}
.preview-media {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.preview-media--image {
  background-size: cover;
  background-position: center;
}
.preview-overlay {
  position: absolute;
  inset: 0;
  background: #0f172a;
  pointer-events: none;
}
.preview-content {
  position: relative;
  z-index: 1;
  padding: 48px 32px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}
.preview-content h3 {
  margin: 0 0 8px;
  font-size: 22px;
}
.preview-content p {
  margin: 0;
  opacity: 0.92;
}
</style>
`

fs.mkdirSync(outDir, { recursive: true })
fs.writeFileSync(outFile, content, 'utf8')
console.log('Wrote', outFile)
