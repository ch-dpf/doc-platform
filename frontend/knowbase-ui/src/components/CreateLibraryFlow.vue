<template>
  <CreateLibraryDialog
    v-model="visible"
    :tenant-id="tenantId"
    @created="(lib) => emit('created', lib)"
  />
</template>

<script setup>
import { computed } from 'vue'
import CreateLibraryDialog from './CreateLibraryDialog.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  tenantId: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'created'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
</script>
