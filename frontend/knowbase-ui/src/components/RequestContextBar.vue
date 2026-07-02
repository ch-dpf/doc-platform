<template>
  <div class="request-context-bar">
    <span class="request-context-bar__label">请求上下文</span>
    <el-input v-model="requestContext.tenantId" size="small" placeholder="租户 ID" style="width: 110px" />
    <el-input v-model="requestContext.userId" size="small" placeholder="用户 ID" style="width: 110px" />
    <el-input v-model="requestContext.roles" size="small" placeholder="角色，逗号分隔" style="width: 160px" />
  </div>
</template>

<script setup>
import { requestContext } from '../context';
</script>

<style scoped>
.request-context-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.request-context-bar__label {
  font-size: 12px;
  color: var(--dp-text-secondary);
  white-space: nowrap;
}
</style>
