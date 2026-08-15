<template>
  <div class="app-container portal-banner-admin">
    <el-card v-loading="loading" shadow="never" class="section-card">
      <div slot="header" class="card-header">
        <span>门户首页 Hero Banner</span>
        <el-button type="text" icon="el-icon-view" @click="openPortal">预览门户首页</el-button>
      </div>

      <el-form ref="homeForm" :model="form" :rules="homeRules" label-width="120px" size="small">
        <el-form-item label="展示模式" prop="mode">
          <el-radio-group v-model="form.mode">
            <el-radio :label="'none'">默认渐变背景</el-radio>
            <el-radio :label="'image'">背景图片</el-radio>
            <el-radio :label="'video'">背景视频</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.mode === 'image'" label="背景图片" prop="imageUrl">
          <image-upload v-model="form.imageUrl" :limit="1" :file-size="10" />
          <el-input v-model="form.imageUrl" clearable placeholder="或填写图片 URL" style="margin-top: 8px" @blur="trimField('imageUrl')" />
        </el-form-item>

        <template v-if="form.mode === 'video'">
          <el-form-item label="背景视频" prop="videoUrl">
            <file-upload v-model="form.videoUrl" :limit="1" :file-size="100" :file-type="['mp4', 'webm']" />
            <el-input v-model="form.videoUrl" clearable placeholder="或填写视频 URL" style="margin-top: 8px" @blur="trimField('videoUrl')" />
          </el-form-item>
          <el-form-item label="视频封面" prop="videoPoster">
            <image-upload v-model="form.videoPoster" :limit="1" :file-size="10" />
          </el-form-item>
        </template>

        <el-form-item label="遮罩浓度" prop="overlay">
          <el-slider v-model="form.overlay" :min="0" :max="1" :step="0.01" show-input :show-input-controls="false" style="max-width: 420px" />
        </el-form-item>

        <template v-if="form.mode === 'image' || form.mode === 'video'">
          <el-form-item label="主标题" prop="heroTitle">
            <el-input v-model="form.heroTitle" maxlength="40" show-word-limit placeholder="Hero 区域主标题，留空使用默认文案" />
          </el-form-item>
          <el-form-item label="副标题" prop="heroDesc">
            <el-input v-model="form.heroDesc" type="textarea" :rows="2" maxlength="120" show-word-limit placeholder="Hero 区域描述文案" />
          </el-form-item>
        </template>

        <template v-if="form.mode === 'none'">
          <el-divider content-position="left">轮播幻灯（最多 8 张）</el-divider>
          <div v-for="(slide, index) in form.slides" :key="index" class="slide-editor">
            <div class="slide-editor-head">
              <span>幻灯 {{ index + 1 }}</span>
              <el-button v-if="form.slides.length > 1" type="text" icon="el-icon-delete" @click="removeSlide(index)">删除</el-button>
            </div>
            <el-form-item label="标题">
              <el-input v-model="slide.title" maxlength="40" placeholder="幻灯标题" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="slide.desc" type="textarea" :rows="2" maxlength="120" placeholder="幻灯描述" />
            </el-form-item>
            <el-form-item label="背景类型">
              <el-radio-group v-model="slide.bgType" size="mini" @change="onSlideBgTypeChange(slide)">
                <el-radio-button label="gradient">渐变</el-radio-button>
                <el-radio-button label="image">图片</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="slide.bgType === 'gradient'" label="渐变 CSS">
              <el-input v-model="slide.bg" placeholder="linear-gradient(125deg, #115E59, #14B8A6)" />
            </el-form-item>
            <el-form-item v-else label="背景图片">
              <image-upload v-model="slide.imageUrl" :limit="1" :file-size="10" />
              <el-input v-model="slide.imageUrl" clearable placeholder="或填写图片 URL" style="margin-top: 8px" />
            </el-form-item>
          </div>
          <el-button v-if="form.slides.length < 8" type="dashed" icon="el-icon-plus" @click="addSlide">添加幻灯</el-button>
        </template>
      </el-form>
    </el-card>

    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">
        <span>门户顶栏背景（Logo / 搜索 / 菜单区域）</span>
      </div>

      <el-form ref="headerForm" :model="form" :rules="headerRules" label-width="120px" size="small">
        <el-form-item label="顶栏背景" prop="headerMode">
          <el-radio-group v-model="form.headerMode">
            <el-radio :label="'none'">默认（无背景图）</el-radio>
            <el-radio :label="'image'">背景图片</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.headerMode === 'image'" label="顶栏图片" prop="headerImageUrl">
          <image-upload v-model="form.headerImageUrl" :limit="1" :file-size="10" />
          <el-input v-model="form.headerImageUrl" clearable placeholder="或填写图片 URL" style="margin-top: 8px" @blur="trimField('headerImageUrl')" />
          <div class="form-tip">建议宽图（如 1920×140），高度将自动适配顶栏与导航区域</div>
        </el-form-item>

        <el-form-item v-if="form.headerMode === 'image'" label="顶栏遮罩" prop="headerOverlay">
          <el-slider v-model="form.headerOverlay" :min="0" :max="1" :step="0.01" show-input :show-input-controls="false" style="max-width: 420px" />
          <div class="form-tip">用于保证 Logo、搜索框与菜单文字清晰可读，建议 0.3 – 0.55</div>
        </el-form-item>

        <div v-if="form.headerMode === 'image' && form.headerImageUrl" class="header-preview">
          <img class="header-preview-bg" :src="resolveUrl(form.headerImageUrl)" alt="">
          <div class="header-preview-shade" :style="{ opacity: form.headerOverlay }" />
          <div class="header-preview-mock">
            <span class="mock-logo">东陆智能教学库</span>
            <span class="mock-search">搜索框</span>
            <span class="mock-nav">首页 · 章节选题 · 组卷</span>
          </div>
        </div>
      </el-form>
    </el-card>

    <div class="form-actions">
      <el-button type="primary" :loading="saving" @click="submitForm" v-hasPermi="['education:portal:banner:edit']">保存配置</el-button>
      <el-button @click="loadConfig">重新加载</el-button>
    </div>
  </div>
</template>

<script>
import { getPortalBannerAdminConfig, updatePortalBannerAdminConfig } from '@/api/education/portalBanner'
import { DEFAULT_HERO_SLIDES, resolvePortalMediaUrl } from '@/utils/portalBanner'

function createSlide(source) {
  const item = source || {}
  const imageUrl = item.imageUrl || ''
  return {
    title: item.title || '',
    desc: item.desc || '',
    bg: item.bg || 'linear-gradient(125deg, #115E59 0%, #0F766E 55%, #14B8A6 100%)',
    imageUrl,
    bgType: imageUrl ? 'image' : 'gradient'
  }
}

const MEDIA_URL_PATTERN = /^(https?:\/\/|\/\/|\/|data:|blob:)/i

export default {
  name: 'PortalBannerAdmin',
  data() {
    return {
      loading: false,
      saving: false,
      form: {
        mode: 'none',
        imageUrl: '',
        videoUrl: '',
        videoPoster: '',
        overlay: 0.42,
        heroTitle: '',
        heroDesc: '',
        slides: DEFAULT_HERO_SLIDES.map(s => createSlide(s)),
        headerMode: 'none',
        headerImageUrl: '',
        headerOverlay: 0.4
      },
      homeRules: {
        mode: [{ required: true, message: '请选择展示模式', trigger: 'change' }]
      },
      headerRules: {
        headerMode: [{ required: true, message: '请选择顶栏背景模式', trigger: 'change' }],
        headerImageUrl: [{
          validator: (rule, value, callback) => {
            if (this.form.headerMode !== 'image') {
              callback()
              return
            }
            if (!value) {
              callback(new Error('请上传或填写顶栏背景图片'))
              return
            }
            if (!this.isValidMediaUrl(value)) {
              callback(new Error('请填写有效的图片地址'))
              return
            }
            callback()
          },
          trigger: ['blur', 'change']
        }]
      }
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
          mode: this.normalizeMode(data.mode),
          imageUrl: data.imageUrl || '',
          videoUrl: data.videoUrl || '',
          videoPoster: data.videoPoster || '',
          overlay: typeof data.overlay === 'number' ? data.overlay : 0.42,
          heroTitle: data.heroTitle || '',
          heroDesc: data.heroDesc || '',
          slides: this.normalizeSlides(data.slides),
          headerMode: data.headerMode === 'image' ? 'image' : 'none',
          headerImageUrl: data.headerImageUrl || '',
          headerOverlay: typeof data.headerOverlay === 'number' ? data.headerOverlay : 0.4
        }
        this.$nextTick(() => {
          this.$refs.homeForm && this.$refs.homeForm.clearValidate()
          this.$refs.headerForm && this.$refs.headerForm.clearValidate()
        })
      }).finally(() => {
        this.loading = false
      })
    },
    buildSavePayload() {
      const mode = this.normalizeMode(this.form.mode)
      const headerMode = this.form.headerMode === 'image' ? 'image' : 'none'
      const slides = mode === 'none'
        ? this.form.slides.map(slide => ({
          title: (slide.title || '').trim(),
          desc: (slide.desc || '').trim(),
          bg: slide.bgType === 'gradient' ? (slide.bg || '').trim() : '',
          imageUrl: slide.bgType === 'image' ? (slide.imageUrl || '').trim() : ''
        })).filter(slide => slide.bg || slide.imageUrl)
        : []
      return {
        mode,
        overlay: this.form.overlay,
        heroTitle: (this.form.heroTitle || '').trim(),
        heroDesc: (this.form.heroDesc || '').trim(),
        slides,
        headerMode,
        headerOverlay: this.form.headerOverlay,
        imageUrl: mode === 'image' ? (this.form.imageUrl || '').trim() : '',
        videoUrl: mode === 'video' ? (this.form.videoUrl || '').trim() : '',
        videoPoster: mode === 'video' ? (this.form.videoPoster || '').trim() : '',
        headerImageUrl: headerMode === 'image' ? (this.form.headerImageUrl || '').trim() : ''
      }
    },
    normalizeSlides(raw) {
      const list = Array.isArray(raw) ? raw : []
      const slides = list.map(item => createSlide(item)).filter(slide => slide.bg || slide.imageUrl)
      return slides.length ? slides : DEFAULT_HERO_SLIDES.map(s => createSlide(s))
    },
    addSlide() {
      if (this.form.slides.length >= 8) return
      this.form.slides.push(createSlide())
    },
    removeSlide(index) {
      if (this.form.slides.length <= 1) return
      this.form.slides.splice(index, 1)
    },
    onSlideBgTypeChange(slide) {
      if (slide.bgType === 'gradient') {
        slide.imageUrl = ''
        if (!slide.bg) {
          slide.bg = 'linear-gradient(125deg, #115E59 0%, #0F766E 55%, #14B8A6 100%)'
        }
      } else {
        slide.bg = ''
      }
    },
    normalizeMode(mode) {
      return mode === 'image' || mode === 'video' ? mode : 'none'
    },
    submitForm() {
      const tasks = [
        new Promise((resolve, reject) => {
          this.$refs.homeForm.validate(valid => {
            if (valid) resolve()
            else reject(new Error('请检查首页 Banner 配置'))
          })
        })
      ]
      if (this.$refs.headerForm) {
        tasks.push(new Promise((resolve, reject) => {
          this.$refs.headerForm.validate(valid => {
            if (valid) resolve()
            else reject(new Error('请检查顶栏背景配置'))
          })
        }))
      }
      Promise.all(tasks).then(() => {
        this.saving = true
        return updatePortalBannerAdminConfig(this.buildSavePayload())
      }).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.loadConfig()
      }).catch((err) => {
        if (err && err.message && err.message !== 'cancel') {
          this.$modal.msgError(err.message || '保存失败，请检查表单')
        }
      }).finally(() => {
        this.saving = false
      })
    },
    openPortal() {
      window.open('/', '_blank')
    },
    trimField(field) {
      if (this.form[field]) {
        this.form[field] = String(this.form[field]).trim()
      }
    },
    isValidMediaUrl(value) {
      const raw = String(value || '').trim()
      return raw && (MEDIA_URL_PATTERN.test(raw) || !/[\s\u0000]/.test(raw))
    },
    resolveUrl(url) {
      return resolvePortalMediaUrl(url)
    }
  }
}
</script>

<style scoped>
.portal-banner-admin .section-card {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}
.form-actions {
  padding: 8px 0 24px;
}
.slide-editor {
  margin: 0 0 16px 120px;
  max-width: 720px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fafbfc;
}
.slide-editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
  color: #334155;
}
.header-preview {
  position: relative;
  height: 148px;
  border-radius: 12px;
  overflow: hidden;
  margin: 8px 0 0 120px;
  max-width: 720px;
  border: 1px solid #e2e8f0;
}
.header-preview-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  display: block;
}
.header-preview-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.55), rgba(15, 23, 42, 0.35));
}
.header-preview-mock {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 20px;
  color: #fff;
  font-size: 13px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
}
.mock-logo { font-weight: 700; font-size: 15px; }
.mock-search {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  color: #64748b;
  width: fit-content;
  text-shadow: none;
}
.mock-nav { opacity: 0.92; font-size: 12px; }
</style>
