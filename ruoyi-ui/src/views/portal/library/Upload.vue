<template>
  <div class="portal-library-upload portal-page">
    <div class="portal-container upload-wrap">
      <nav class="portal-breadcrumb" aria-label="breadcrumb">
        <router-link to="/">{{ L.home }}</router-link>
        <span class="sep">/</span>
        <router-link to="/library">{{ L.breadcrumb }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ L.uploadTitle }}</span>
      </nav>

      <header class="upload-hero portal-card">
        <div class="upload-hero-main">
          <h1>{{ L.uploadTitle }}</h1>
          <p>{{ L.uploadSubtitle }}</p>
        </div>
      </header>

      <div class="upload-layout">
        <section class="upload-form-card portal-card portal-card-accent">
          <el-form ref="uploadForm" :model="uploadForm" :rules="uploadRules" label-width="96px" size="small">
            <el-form-item :label="L.fieldFile" prop="fileUrl">
              <file-upload
                v-model="uploadForm.fileUrl"
                :limit="1"
                :file-size="50"
                :file-type="extOptions"
                @file-meta="onFileMeta"
              />
            </el-form-item>
            <el-form-item :label="L.fieldTitle" prop="title">
              <el-input v-model="uploadForm.title" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item :label="L.fieldSummary" prop="summary">
              <el-input v-model="uploadForm.summary" type="textarea" :rows="4" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item :label="L.fieldCover" prop="coverUrl">
              <image-upload v-model="uploadForm.coverUrl" :limit="1" :file-size="5" />
              <p v-if="coverGenerating" class="cover-hint cover-hint--loading">
                <i class="el-icon-loading" /> {{ L.coverGenerating }}
              </p>
              <p v-else class="cover-hint">{{ L.coverAutoHint }}</p>
            </el-form-item>
            <el-form-item :label="L.fieldStage" prop="schoolStage">
              <el-select v-model="catalogForm.schoolStage" clearable :placeholder="L.stagePh" style="width: 100%" @change="onStageChange">
                <el-option :label="L.stageJunior" :value="L.stageJunior" />
                <el-option :label="L.stageSenior" :value="L.stageSenior" />
              </el-select>
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="L.fieldSubject" prop="subjectId">
                  <el-select v-model="uploadForm.subjectId" clearable style="width: 100%" @change="onSubjectChange">
                    <el-option v-for="s in subjects" :key="s.subjectId" :label="s.subjectName" :value="s.subjectId" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="L.fieldCategory" prop="categoryId">
                  <el-select v-model="uploadForm.categoryId" clearable style="width: 100%">
                    <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <document-catalog-fields v-model="catalogForm" :subject-id="uploadForm.subjectId" hide-stage />
            <el-form-item :label="L.fieldTags" prop="tagNames">
              <el-input v-model="uploadForm.tagNames" :placeholder="L.tagsPh" maxlength="200" />
            </el-form-item>
            <el-form-item :label="L.fieldVisibility" prop="visibility">
              <el-radio-group v-model="uploadForm.visibility">
                <el-radio label="public">{{ L.visPublic }}</el-radio>
                <el-radio label="school">{{ L.visSchool }}</el-radio>
                <el-radio label="private">{{ L.visPrivate }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item :label="L.fieldDownload" prop="allowDownload">
              <el-switch v-model="uploadForm.allowDownload" active-value="1" inactive-value="0" />
            </el-form-item>
            <el-form-item class="upload-actions">
              <el-button @click="goBack">{{ L.cancel }}</el-button>
              <el-button type="primary" :loading="uploading" @click="submitUpload">{{ L.confirmUpload }}</el-button>
            </el-form-item>
          </el-form>
        </section>

        <upload-guide />
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { fetchSubjectOptionsCached, fetchLibraryCategoriesCached } from '@/utils/metaCache'
import { addPortalLibraryDocument, generateLibraryCover } from '@/api/education/library'
import { goPortalLogin } from '@/utils/portalLogin'
import { LIBRARY_UPLOAD_EXTS, isArchiveExt } from '@/utils/libraryFileExt'
import UploadGuide from './components/UploadGuide'
import DocumentCatalogFields from './components/DocumentCatalogFields'

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
  uploadTitle: '\u4e0a\u4f20\u6559\u5b66\u6587\u6863',
  uploadSubtitle: '\u652f\u6301 PDF\u3001Word\u3001PPT\u3001Excel\u3001ZIP/RAR/7Z \u538b\u7f29\u5305\u7b49\u683c\u5f0f\uff0c\u538b\u7f29\u5305\u53ef\u5728\u7ebf\u9884\u89c8\u5185\u90e8\u6587\u6863',
  fieldFile: '\u6587\u4ef6',
  fieldTitle: '\u6807\u9898',
  fieldSummary: '\u7b80\u4ecb',
  fieldCover: '\u5c01\u9762',
  coverAutoHint: '\u4e0a\u4f20\u6587\u6863\u540e\u5c06\u81ea\u52a8\u622a\u53d6\u9996\u9875\u4f5c\u4e3a\u5c01\u9762\uff0c\u538b\u7f29\u5305\u8bf7\u624b\u52a8\u4e0a\u4f20\u5c01\u9762',
  coverGenerating: '\u6b63\u5728\u751f\u6210\u5c01\u9762\u2026',
  fieldStage: '\u5b66\u6bb5',
  stagePh: '\u8bf7\u9009\u62e9\u5b66\u6bb5',
  stageJunior: '\u521d\u4e2d',
  stageSenior: '\u9ad8\u4e2d',
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
  confirmUpload: '\u63d0\u4ea4\u4e0a\u4f20'
}

export default {
  name: 'PortalLibraryUpload',
  components: { UploadGuide, DocumentCatalogFields },
  data() {
    return {
      L,
      uploading: false,
      subjects: [],
      categories: [],
      catalogForm: emptyCatalogForm(),
      coverGenerating: false,
      coverManual: false,
      autoCoverApplying: false,
      coverRequestToken: 0,
      extOptions: LIBRARY_UPLOAD_EXTS,
      uploadForm: {
        fileUrl: '',
        fileName: '',
        fileExt: '',
        fileSize: 0,
        title: '',
        summary: '',
        coverUrl: '',
        tagNames: '',
        subjectId: undefined,
        categoryId: undefined,
        visibility: 'school',
        allowDownload: '1'
      },
      uploadRules: {
        fileUrl: [{ required: true, message: '\u8bf7\u4e0a\u4f20\u6587\u4ef6', trigger: 'change' }],
        title: [{ required: true, message: '\u8bf7\u586b\u5199\u6807\u9898', trigger: 'blur' }]
      }
    }
  },
  computed: {
    ...mapGetters(['token'])
  },
  watch: {
    'uploadForm.coverUrl'(val, oldVal) {
      if (this.autoCoverApplying) return
      if (val && val !== oldVal) {
        this.coverManual = true
      }
    }
  },
  created() {
    if (!this.token) {
      goPortalLogin(this.$router, '/library/upload', 'login')
      return
    }
    this.loadMeta()
  },
  methods: {
    loadMeta() {
      fetchSubjectOptionsCached().then(res => {
        this.subjects = res.data || []
      }).catch(() => { this.subjects = [] })
      fetchLibraryCategoriesCached().then(res => {
        this.categories = res.data || []
      }).catch(() => { this.categories = [] })
    },
    goBack() {
      this.$router.push('/library')
    },
    onSubjectChange() {
      this.catalogForm = Object.assign(emptyCatalogForm(), { schoolStage: this.catalogForm.schoolStage || '\u9ad8\u4e2d' })
    },
    onStageChange() {
      this.catalogForm = Object.assign(emptyCatalogForm(), { schoolStage: this.catalogForm.schoolStage })
    },
    onFileMeta(meta) {
      if (!meta || !meta.url) return
      const name = meta.name || meta.url.slice(meta.url.lastIndexOf('/') + 1)
      const dot = name.lastIndexOf('.')
      const ext = dot >= 0 ? name.slice(dot + 1).toLowerCase() : ''
      this.uploadForm.fileName = name
      this.uploadForm.fileExt = ext
      this.uploadForm.fileSize = meta.size || 0
      if (!this.uploadForm.title) {
        this.uploadForm.title = dot >= 0 ? name.slice(0, dot) : name
      }
      this.coverManual = false
      this.uploadForm.coverUrl = ''
      this.autoGenerateCover(meta.url, ext)
    },
    autoGenerateCover(fileUrl, fileExt) {
      if (!fileUrl || fileExt === 'txt' || isArchiveExt(fileExt)) return
      const token = ++this.coverRequestToken
      this.coverGenerating = true
      generateLibraryCover({ fileUrl, fileExt }).then(res => {
        if (token !== this.coverRequestToken || this.coverManual) return
        const coverUrl = res.data
        if (coverUrl) {
          this.autoCoverApplying = true
          this.uploadForm.coverUrl = coverUrl
          this.$nextTick(() => {
            this.autoCoverApplying = false
          })
        }
      }).catch(() => {
        // user may upload cover manually
      }).finally(() => {
        if (token === this.coverRequestToken) {
          this.coverGenerating = false
        }
      })
    },
    submitUpload() {
      this.$refs.uploadForm.validate(valid => {
        if (!valid) return
        this.uploading = true
        const payload = {
          ...this.uploadForm,
          schoolStage: this.catalogForm.schoolStage,
          versionId: this.catalogForm.versionId,
          textbookId: this.catalogForm.textbookId,
          chapterId: this.catalogForm.chapterId,
          chapterText: this.catalogForm.chapterText
        }
        addPortalLibraryDocument(payload).then(res => {
          const msg = res.auditPending
            ? '\u4e0a\u4f20\u6210\u529f\uff0c\u5f85\u7ba1\u7406\u5458\u5ba1\u6838\u540e\u516c\u5f00'
            : '\u4e0a\u4f20\u6210\u529f'
          this.$modal.msgSuccess(msg)
          const id = res.data
          if (id && !res.auditPending) {
            this.$router.push('/library/' + id)
            return
          }
          this.$router.push({ path: '/library', query: { tab: 'mine' } })
        }).finally(() => {
          this.uploading = false
        })
      })
    }
  }
}
</script>

<style scoped lang="scss">
.upload-wrap {
  padding-bottom: 40px;
}

.upload-hero {
  margin-bottom: 14px;
  padding: 18px 20px;

  h1 {
    margin: 0 0 6px;
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #64748b;
    line-height: 1.6;
  }
}

.upload-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
  align-items: start;
}

.upload-form-card {
  padding: 24px 28px 12px;
  min-width: 0;
}

.upload-actions {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}

.cover-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;

  &--loading {
    color: #0f766e;
  }

  i {
    margin-right: 4px;
  }
}

@media (max-width: 960px) {
  .upload-layout {
    grid-template-columns: 1fr;
  }
}
</style>
