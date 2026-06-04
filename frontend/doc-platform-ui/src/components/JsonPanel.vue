<template>
  <div class="json-panel">
    <div v-if="title" class="json-panel__head">
      <span class="json-panel__title">{{ title }}</span>
      <el-button v-if="copyable && data != null" link type="primary" size="small" @click="copy">
        复制
      </el-button>
    </div>
    <pre v-if="data != null" class="json-panel__code">{{ formatted }}</pre>
    <div v-else class="json-panel__empty">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  title: { type: String, default: '' },
  data: { type: [Object, Array, String, Number, Boolean], default: null },
  copyable: { type: Boolean, default: true },
  emptyText: { type: String, default: '暂无数据' }
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
.json-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.json-panel__title {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.json-panel__code {
  margin: 0;
  max-height: 440px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.55;
  font-family: ui-monospace, 'Cascadia Code', Consolas, monospace;
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  color: #e2e8f0;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #334155;
}

.json-panel__empty {
  padding: 32px 16px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 10px;
}
</style>
