<template>
  <div class="library-onboarding-strip" role="status">
    <span class="library-onboarding-strip__badge">新建</span>
    <nav class="library-onboarding-strip__steps" aria-label="新建知识库引导">
      <span
        class="library-onboarding-strip__step"
        :class="{
          'library-onboarding-strip__step--done': configSaved,
          'library-onboarding-strip__step--active': !configSaved
        }"
      >
        确认配置
      </span>
      <span class="library-onboarding-strip__arrow" aria-hidden="true">→</span>
      <span
        class="library-onboarding-strip__step"
        :class="{
          'library-onboarding-strip__step--done': hasDocuments,
          'library-onboarding-strip__step--active': configSaved && !hasDocuments,
          'library-onboarding-strip__step--disabled': !configSaved
        }"
      >
        上传文档
      </span>
      <span class="library-onboarding-strip__arrow" aria-hidden="true">→</span>
      <button
        v-if="hasDocuments"
        type="button"
        class="library-onboarding-strip__step library-onboarding-strip__step--clickable library-onboarding-strip__step--active"
        @click="emit('verify-retrieval')"
      >
        验证检索
      </button>
      <span
        v-else
        class="library-onboarding-strip__step library-onboarding-strip__step--disabled"
      >
        验证检索
      </span>
    </nav>
    <p v-if="!configSaved" class="library-onboarding-strip__hint">
      请在下方调整并保存配置
    </p>
    <p v-else-if="!hasDocuments" class="library-onboarding-strip__hint">
      配置已保存，可上传文档
    </p>
    <p v-else class="library-onboarding-strip__hint">
      文档已入库，点击「验证检索」前往检索测试
    </p>
    <el-button link type="info" class="library-onboarding-strip__skip" @click="emit('dismiss')">
      跳过
    </el-button>
  </div>
</template>

<script setup>
defineProps({
  configSaved: { type: Boolean, default: false },
  hasDocuments: { type: Boolean, default: false }
})

const emit = defineEmits(['dismiss', 'verify-retrieval'])
</script>
