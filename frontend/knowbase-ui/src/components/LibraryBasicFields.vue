<template>
  <div :class="['library-basic-fields', `library-basic-fields--${layout}`]">
    <el-form-item :label="LIBRARY_NAME_LABEL" required>
      <el-input
        v-model="form.name"
        :placeholder="LIBRARY_NAME_PLACEHOLDER"
        maxlength="128"
        show-word-limit
      />
    </el-form-item>
    <el-form-item :label="LIBRARY_DESCRIPTION_LABEL">
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="layout === 'top' ? 3 : 2"
        :placeholder="LIBRARY_DESCRIPTION_PLACEHOLDER"
      />
    </el-form-item>
    <el-form-item label="标签">
      <el-select
        v-model="form.tags"
        multiple
        filterable
        allow-create
        default-first-option
        :placeholder="LIBRARY_TAGS_PLACEHOLDER"
        class="full-width"
      />
    </el-form-item>
  </div>
</template>

<script setup>
import {
  LIBRARY_DESCRIPTION_LABEL,
  LIBRARY_DESCRIPTION_PLACEHOLDER,
  LIBRARY_NAME_LABEL,
  LIBRARY_NAME_PLACEHOLDER,
  LIBRARY_TAGS_PLACEHOLDER
} from '../utils/libraryFormLabels'

defineProps({
  form: { type: Object, required: true },
  /** top：快速创建；inline：库配置 Tab 内 */
  layout: { type: String, default: 'inline' }
})
</script>

<style scoped>
.library-basic-fields--top :deep(.el-form-item) {
  margin-bottom: 18px;
}

.library-basic-fields--top :deep(.el-form-item__label) {
  padding-bottom: 6px;
  line-height: 1.4;
  font-weight: 500;
  color: var(--dp-text);
}

.library-basic-fields--top :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.full-width {
  width: 100%;
}
</style>
