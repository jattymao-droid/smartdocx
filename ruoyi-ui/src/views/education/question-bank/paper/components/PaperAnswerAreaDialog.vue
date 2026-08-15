<template>
  <el-dialog title="插入作答区" :visible.sync="visible" width="440px" append-to-body @open="onOpen">
    <el-form label-width="88px" size="small">
      <el-form-item label="作答样式">
        <el-radio-group v-model="form.style">
          <el-radio label="ruled">横线作答区</el-radio>
          <el-radio label="blank">空白作答区</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="行数">
        <el-slider v-model="form.lines" :min="1" :max="20" show-input />
      </el-form-item>
      <el-form-item v-if="existing" label="当前">
        <el-tag size="small" closable @close="clearArea">
          {{ existing.style === 'blank' ? '空白' : '横线' }} {{ existing.lines }} 行
        </el-tag>
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button v-if="existing" type="danger" plain @click="clearArea">移除作答区</el-button>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="confirm">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { suggestAnswerAreaLines } from '@/utils/paperAnswerArea'

export default {
  name: 'PaperAnswerAreaDialog',
  props: {
    value: { type: Boolean, default: false },
    question: { type: Object, default: null },
    existing: { type: Object, default: null }
  },
  data() {
    return {
      form: { style: 'ruled', lines: 4 }
    }
  },
  computed: {
    visible: {
      get() { return this.value },
      set(val) { this.$emit('input', val) }
    }
  },
  methods: {
    onOpen() {
      if (this.existing) {
        this.form = { style: this.existing.style || 'ruled', lines: this.existing.lines || 4 }
      } else if (this.question) {
        this.form = { style: 'ruled', lines: suggestAnswerAreaLines(this.question.questionType) }
      }
    },
    clearArea() {
      this.$emit('clear')
      this.visible = false
    },
    confirm() {
      this.$emit('confirm', { style: this.form.style, lines: this.form.lines })
      this.visible = false
    }
  }
}
</script>
