<template>
  <div class="filter-bar">
    <div class="filter-row">
      <span class="filter-label">题型</span>
      <div class="filter-tags">
        <span class="filter-tag" :class="{ active: !questionType }" @click="setField('questionType', undefined)">全部</span>
        <span
          v-for="item in resolvedQuestionTypeOptions"
          :key="item.value"
          class="filter-tag"
          :class="{ active: questionType === item.value }"
          @click="setField('questionType', item.value)"
        >{{ item.label }}</span>
      </div>
    </div>
    <div class="filter-row filter-row-diff">
      <span class="filter-label">难度</span>
      <div class="filter-tags">
        <span class="filter-tag" :class="{ active: difficultyBand === 'all' }" @click="setDifficulty('all')">全部</span>
        <span class="filter-tag" :class="{ active: difficultyBand === 'easy' }" @click="setDifficulty('easy')">容易</span>
        <span class="filter-tag" :class="{ active: difficultyBand === 'medium' }" @click="setDifficulty('medium')">适中</span>
        <span class="filter-tag" :class="{ active: difficultyBand === 'hard' }" @click="setDifficulty('hard')">困难</span>
      </div>
    </div>
  </div>
</template>

<script>
import dynamicQuestionTypes from '@/mixins/dynamicQuestionTypes'
import { getCachedQuestionTypeOptions } from '@/utils/questionTypes'

export default {
  name: 'QuestionFilterBar',
  mixins: [dynamicQuestionTypes],
  props: {
    questionType: { type: String, default: undefined },
    difficultyMin: { type: Number, default: undefined },
    difficultyMax: { type: Number, default: undefined },
    questionTypeOptions: { type: Array, default: () => [] }
  },
  computed: {
    resolvedQuestionTypeOptions() {
      if (this.questionTypeOptions && this.questionTypeOptions.length) {
        return this.questionTypeOptions
      }
      return this.dynamicQuestionTypeOptions.length
        ? this.dynamicQuestionTypeOptions
        : getCachedQuestionTypeOptions()
    },
    difficultyBand() {
      if (this.difficultyMin == null && this.difficultyMax == null) return 'all'
      if (this.difficultyMin === 0.1 && this.difficultyMax === 0.35) return 'easy'
      if (this.difficultyMin === 0.36 && this.difficultyMax === 0.74) return 'medium'
      if (this.difficultyMin === 0.75 && this.difficultyMax === 1) return 'hard'
      return 'custom'
    }
  },
  methods: {
    setField(field, value) {
      this.$emit('change', { field, value })
    },
    setDifficulty(band) {
      let min
      let max
      if (band === 'easy') {
        min = 0.1
        max = 0.35
      } else if (band === 'medium') {
        min = 0.36
        max = 0.74
      } else if (band === 'hard') {
        min = 0.75
        max = 1
      }
      this.$emit('change', { field: 'difficulty', value: { min, max } })
    }
  }
}
</script>

<style scoped lang="scss">
.filter-bar {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 8px;
  padding: 12px 16px 8px;
  margin-bottom: 12px;
}
.filter-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;
  font-size: 13px;
}
.filter-row-diff {
  align-items: center;
  margin-bottom: 4px;
}
.filter-label {
  flex-shrink: 0;
  width: 42px;
  line-height: 28px;
  color: #909399;
}
.filter-tags {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
}
.filter-tag {
  display: inline-block;
  padding: 4px 10px;
  line-height: 20px;
  border-radius: 14px;
  color: #303133;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  &:hover { color: #409eff; }
  &.active { color: #fff; background: #409eff; }
}
</style>
