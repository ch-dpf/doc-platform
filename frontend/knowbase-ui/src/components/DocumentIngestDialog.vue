<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="920px"
    top="4vh"
    destroy-on-close
    class="document-ingest-dialog"
    @close="emit('close')"
  >
    <DocumentIngestWizard
      v-if="libraryId"
      :library-id="libraryId"
      embedded
      @completed="handleCompleted"
      @message="(text, type) => emit('message', text, type)"
    />
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue';
import DocumentIngestWizard from './DocumentIngestWizard.vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  libraryId: { type: String, default: '' },
  libraryName: { type: String, default: '' }
});

const emit = defineEmits(['close', 'completed', 'message']);

const title = computed(() => `${props.libraryName || '知识库'} · 上传文档`);

function handleCompleted(run) {
  emit('completed', run);
  emit('close');
}
</script>

<style scoped>
.document-ingest-dialog :deep(.el-dialog__body) {
  max-height: calc(92vh - 120px);
  overflow-y: auto;
}
</style>
