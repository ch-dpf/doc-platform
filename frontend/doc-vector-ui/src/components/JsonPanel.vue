<template>
  <el-card v-if="data" shadow="never" class="json-panel">
    <template #header>
      <span>{{ title }}</span>
      <el-button v-if="copyable" link type="primary" @click="copy">复制</el-button>
    </template>
    <pre>{{ formatted }}</pre>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  title: { type: String, default: '响应结果' },
  data: { type: [Object, Array, String, Number, Boolean], default: null },
  copyable: { type: Boolean, default: true }
})

const formatted = computed(() => {
  if (props.data == null) return ''
  return typeof props.data === 'string'
    ? props.data
    : JSON.stringify(props.data, null, 2)
})

function copy() {
  navigator.clipboard.writeText(formatted.value)
  ElMessage.success('已复制')
}
</script>

<style scoped>
.json-panel pre {
  margin: 0;
  max-height: 420px;
  overflow: auto;
  font-size: 13px;
  background: #0f172a;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 6px;
}
</style>
