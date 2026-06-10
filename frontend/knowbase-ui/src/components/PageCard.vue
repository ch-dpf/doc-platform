<template>
  <el-card class="page-card" shadow="never">
    <template v-if="title" #header>
      <div class="page-card__header">
        <div>
          <h2 class="page-card__title">{{ title }}</h2>
          <p v-if="subtitle" class="page-card__subtitle">{{ subtitle }}</p>
        </div>
        <div v-if="$slots.actions" class="page-card__actions">
          <slot name="actions" />
        </div>
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' }
})
</script>
