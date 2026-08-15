<template>
  <div class="app-container ai-tutor-admin">
    <el-card v-loading="loading" shadow="never">
      <div slot="header" class="card-header">
        <span>AI ????????</span>
      </div>

      <el-alert
        title="?????? DeepSeek ????deepseek-chat?????????��???�� DeepSeek API Key ??��?????????????? AI ??????????"
        type="info"
        :closable="false"
        show-icon
        class="tip-alert"
      />

      <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small">
        <el-form-item label="???????" prop="enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <el-form-item label="API ???" prop="baseUrl">
          <el-input v-model="form.baseUrl" clearable placeholder="https://api.deepseek.com" />
          <div class="form-tip">DeepSeek ?? https://api.deepseek.com?????? OpenAI ??????????????</div>
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model="form.apiKey"
            clearable
            show-password
            :placeholder="apiKeyPlaceholder"
          />
          <div v-if="form.apiKeyConfigured && !form.apiKey" class="form-tip">
            ???????{{ form.apiKeyMasked }}????????????????? Key ??????
          </div>
        </el-form-item>

        <el-form-item label="???" prop="model">
          <el-select v-model="form.model" filterable allow-create default-first-option style="width: 100%">
            <el-option label="deepseek-chat???????" value="deepseek-chat" />
            <el-option label="deepseek-reasoner" value="deepseek-reasoner" />
          </el-select>
        </el-form-item>

        <el-form-item label="???" prop="temperature">
          <el-slider
            v-model="form.temperature"
            :min="0"
            :max="1"
            :step="0.05"
            show-input
            :show-input-controls="false"
            style="max-width: 420px"
          />
          <div class="form-tip">0 ???????1 ???��?????????? 0.6 - 0.8</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submitForm" v-hasPermi="['education:ai-tutor:edit']">????????</el-button>
          <el-button @click="loadConfig">???????</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getAiTutorAdminConfig, updateAiTutorAdminConfig } from '@/api/education/aiTutorAdmin'

export default {
  name: 'AiTutorAdmin',
  data() {
    return {
      loading: false,
      saving: false,
      form: {
        enabled: true,
        baseUrl: 'https://api.deepseek.com',
        apiKey: '',
        apiKeyConfigured: false,
        apiKeyMasked: '',
        model: 'deepseek-chat',
        temperature: 0.7
      },
      rules: {
        baseUrl: [{ required: true, message: '????�� API ???', trigger: 'blur' }],
        model: [{ required: true, message: '????????', trigger: 'change' }]
      }
    }
  },
  computed: {
    apiKeyPlaceholder() {
      return this.form.apiKeyConfigured ? '?????????????????? Key' : '?????? DeepSeek API Key'
    }
  },
  created() {
    this.loadConfig()
  },
  methods: {
    loadConfig() {
      this.loading = true
      getAiTutorAdminConfig().then(res => {
        const data = res.data || {}
        this.form = {
          enabled: data.enabled !== false,
          baseUrl: data.baseUrl || 'https://api.deepseek.com',
          apiKey: '',
          apiKeyConfigured: !!data.apiKeyConfigured,
          apiKeyMasked: data.apiKeyMasked || '',
          model: data.model || 'deepseek-chat',
          temperature: data.temperature != null ? Number(data.temperature) : 0.7
        }
      }).finally(() => {
        this.loading = false
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.saving = true
        updateAiTutorAdminConfig({
          enabled: this.form.enabled,
          baseUrl: this.form.baseUrl,
          apiKey: this.form.apiKey,
          model: this.form.model,
          temperature: this.form.temperature
        }).then(() => {
          this.$modal.msgSuccess('??????')
          this.loadConfig()
        }).finally(() => {
          this.saving = false
        })
      })
    }
  }
}
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tip-alert {
  margin-bottom: 20px;
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
}
</style>
