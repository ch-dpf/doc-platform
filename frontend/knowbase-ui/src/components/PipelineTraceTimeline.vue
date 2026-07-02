<template>
  <div v-if="items.length || variant === 'stepper'" class="pipeline-trace" :class="`pipeline-trace--${variant}`">
    <template v-if="variant === 'stepper'">
      <div class="pipeline-stepper">
        <div class="pipeline-stepper__summary">
          <div class="pipeline-stepper__summary-left">
            <span class="pipeline-stepper__count">{{ completedCount }}</span>
            <span class="pipeline-stepper__count-sep">/</span>
            <span class="pipeline-stepper__total">{{ steps.length }}</span>
            <span class="pipeline-stepper__count-label">阶段完成</span>
          </div>
          <div v-if="totalDurationMs > 0" class="pipeline-stepper__duration">
            总耗时 <strong>{{ totalDurationMs }}</strong> ms
          </div>
        </div>

        <div class="pipeline-stepper__progress">
          <div class="pipeline-stepper__progress-fill" :style="{ width: `${progressPercent}%` }" />
        </div>

        <div class="pipeline-stepper__track">
          <div
            v-for="(step, index) in steps"
            :key="step.key"
            class="pipeline-stepper__step"
            :class="`pipeline-stepper__step--${step.status}`"
          >
            <div
              v-if="index > 0"
              class="pipeline-stepper__connector"
              :class="connectorClass(index)"
            />
            <div class="pipeline-stepper__node" :title="step.label">
              <el-icon v-if="step.status === 'succeeded'" class="pipeline-stepper__icon"><Check /></el-icon>
              <el-icon v-else-if="step.status === 'failed'" class="pipeline-stepper__icon"><Close /></el-icon>
              <span v-else-if="step.status === 'started'" class="pipeline-stepper__pulse" />
              <span v-else class="pipeline-stepper__index">{{ index + 1 }}</span>
            </div>
            <span class="pipeline-stepper__label">{{ step.label }}</span>
            <span v-if="step.durationMs != null" class="pipeline-stepper__step-duration">{{ step.durationMs }} ms</span>
          </div>
        </div>

        <div v-if="activeSteps.length" class="pipeline-stepper__details">
          <div
            v-for="step in activeSteps"
            :key="`detail-${step.key}`"
            class="pipeline-stepper__card"
            :class="`pipeline-stepper__card--${step.status}`"
          >
            <div class="pipeline-stepper__card-head">
              <span class="pipeline-stepper__card-title">{{ step.label }}</span>
              <el-tag size="small" :type="pipelineSpanStatusType(step.span?.status)">{{ step.span?.status || '—' }}</el-tag>
              <span v-if="step.durationMs != null" class="pipeline-stepper__card-duration">{{ step.durationMs }} ms</span>
            </div>
            <p v-if="step.summary" class="pipeline-stepper__card-summary muted">{{ step.summary }}</p>
            <p v-if="showTimestamps && step.startedAt" class="pipeline-stepper__card-time muted">
              {{ formatDateTime(step.startedAt) }}
            </p>
          </div>
        </div>
        <el-empty v-else :description="emptyText" />
      </div>
    </template>

    <template v-else>
      <div class="pipeline-trace-timeline">
        <div
          v-for="(span, index) in items"
          :key="span.spanId || `${span.stage}-${index}`"
          class="pipeline-trace-item"
        >
          <div class="pipeline-trace-item__rail">
            <span class="pipeline-trace-item__dot" :class="`pipeline-trace-item__dot--${pipelineSpanStatusType(span.status)}`" />
            <span v-if="index < items.length - 1" class="pipeline-trace-item__line" />
          </div>
          <div class="pipeline-trace-item__body">
            <div class="pipeline-trace-item__head">
              <strong>{{ formatPipelineStageLabel(span.stage) }}</strong>
              <el-tag size="small" :type="pipelineSpanStatusType(span.status)">{{ span.status || '—' }}</el-tag>
              <span class="muted">{{ span.durationMs ?? '--' }} ms</span>
            </div>
            <p v-if="formatPipelineSpanSummary(span.attributes)" class="pipeline-trace-item__summary muted">
              {{ formatPipelineSpanSummary(span.attributes) }}
            </p>
            <p v-if="showTimestamps && span.startedAt" class="pipeline-trace-item__time muted">
              {{ formatDateTime(span.startedAt) }}
            </p>
          </div>
        </div>
      </div>
    </template>
  </div>
  <el-empty v-else :description="emptyText" />
</template>

<script setup>
import { computed } from 'vue';
import { Check, Close } from '@element-plus/icons-vue';
import {
  DOCUMENT_PIPELINE_STAGES,
  formatDateTime,
  formatPipelineSpanSummary,
  formatPipelineStageLabel,
  pipelineSpanStatusType,
  sortPipelineSpans
} from '../format';

const props = defineProps({
  spans: { type: Array, default: () => [] },
  stages: { type: Array, default: null },
  emptyText: { type: String, default: '暂无 Pipeline Span' },
  showTimestamps: { type: Boolean, default: false },
  variant: { type: String, default: 'timeline' }
});

const stageKeys = computed(() => {
  if (Array.isArray(props.stages) && props.stages.length) {
    return props.stages;
  }
  return props.variant === 'stepper' ? DOCUMENT_PIPELINE_STAGES : [];
});

const items = computed(() => sortPipelineSpans(props.spans));

function normalizeSpanStatus(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'SUCCEEDED') {
    return 'succeeded';
  }
  if (normalized === 'FAILED') {
    return 'failed';
  }
  if (normalized === 'STARTED') {
    return 'started';
  }
  return 'pending';
}

const spanByStage = computed(() => {
  const map = new Map();
  for (const span of items.value) {
    if (span?.stage) {
      map.set(span.stage, span);
    }
  }
  return map;
});

const steps = computed(() => {
  const stages = stageKeys.value;
  let lastSeenIndex = -1;
  stages.forEach((key, index) => {
    if (spanByStage.value.has(key)) {
      lastSeenIndex = index;
    }
  });

  return stages.map((key, index) => {
    const span = spanByStage.value.get(key);
    let status = 'pending';
    if (span) {
      status = normalizeSpanStatus(span.status);
    } else if (lastSeenIndex >= 0 && index < lastSeenIndex) {
      status = 'skipped';
    }

    return {
      key,
      label: formatPipelineStageLabel(key),
      status,
      span,
      durationMs: span?.durationMs,
      summary: span ? formatPipelineSpanSummary(span.attributes) : '',
      startedAt: span?.startedAt
    };
  });
});

const activeSteps = computed(() => steps.value.filter((step) => step.span));

const completedCount = computed(() =>
  steps.value.filter((step) => step.status === 'succeeded').length
);

const totalDurationMs = computed(() =>
  activeSteps.value.reduce((sum, step) => sum + (Number(step.durationMs) || 0), 0)
);

const progressPercent = computed(() => {
  if (!steps.value.length) {
    return 0;
  }
  const startedIndex = steps.value.findIndex((step) => step.status === 'started');
  if (startedIndex >= 0) {
    return ((startedIndex + 0.5) / steps.value.length) * 100;
  }
  const failedIndex = steps.value.findIndex((step) => step.status === 'failed');
  if (failedIndex >= 0) {
    return ((failedIndex + 1) / steps.value.length) * 100;
  }
  if (completedCount.value > 0) {
    return (completedCount.value / steps.value.length) * 100;
  }
  return 0;
});

function connectorClass(index) {
  const previous = steps.value[index - 1];
  if (!previous) {
    return '';
  }
  if (previous.status === 'succeeded') {
    return 'pipeline-stepper__connector--done';
  }
  if (previous.status === 'failed') {
    return 'pipeline-stepper__connector--failed';
  }
  return '';
}
</script>

<style scoped>
.pipeline-trace-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.pipeline-trace-item {
  display: grid;
  grid-template-columns: 20px 1fr;
  gap: 12px;
}

.pipeline-trace-item__rail {
  position: relative;
  display: flex;
  justify-content: center;
}

.pipeline-trace-item__dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 999px;
  background: #94a3b8;
}

.pipeline-trace-item__dot--success {
  background: #16a34a;
}

.pipeline-trace-item__dot--danger {
  background: #dc2626;
}

.pipeline-trace-item__dot--info {
  background: #2563eb;
}

.pipeline-trace-item__line {
  position: absolute;
  top: 18px;
  bottom: -6px;
  width: 2px;
  background: #e2e8f0;
}

.pipeline-trace-item__body {
  padding-bottom: 16px;
}

.pipeline-trace-item__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.pipeline-trace-item__summary,
.pipeline-trace-item__time {
  margin: 6px 0 0;
  font-size: 13px;
}

/* —— Stepper variant —— */
.pipeline-stepper {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.pipeline-stepper__summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.pipeline-stepper__summary-left {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.pipeline-stepper__count {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  color: var(--dp-primary);
}

.pipeline-stepper__count-sep,
.pipeline-stepper__total {
  font-size: 18px;
  font-weight: 600;
  color: var(--dp-text-secondary);
}

.pipeline-stepper__count-label {
  margin-left: 8px;
  font-size: 13px;
  color: var(--dp-text-secondary);
}

.pipeline-stepper__duration {
  font-size: 13px;
  color: var(--dp-text-secondary);
}

.pipeline-stepper__duration strong {
  color: var(--dp-text);
  font-weight: 600;
}

.pipeline-stepper__progress {
  height: 6px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.pipeline-stepper__progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--dp-primary) 0%, var(--dp-accent) 100%);
  transition: width 0.35s ease;
}

.pipeline-stepper__track {
  display: flex;
  gap: 0;
  overflow-x: auto;
  padding: 4px 0 8px;
  scrollbar-width: thin;
}

.pipeline-stepper__step {
  position: relative;
  flex: 1 0 88px;
  min-width: 72px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;
}

.pipeline-stepper__connector {
  position: absolute;
  top: 15px;
  right: 50%;
  left: -50%;
  height: 2px;
  background: #e2e8f0;
  z-index: 0;
}

.pipeline-stepper__connector--done {
  background: var(--dp-accent);
}

.pipeline-stepper__connector--failed {
  background: #fca5a5;
}

.pipeline-stepper__node {
  position: relative;
  z-index: 1;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 2px solid #cbd5e1;
  background: var(--dp-surface);
  font-size: 12px;
  font-weight: 600;
  color: var(--dp-text-secondary);
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.pipeline-stepper__step--succeeded .pipeline-stepper__node {
  border-color: var(--dp-accent);
  background: #ecfdf5;
  color: var(--dp-accent);
}

.pipeline-stepper__step--failed .pipeline-stepper__node {
  border-color: #ef4444;
  background: #fef2f2;
  color: #ef4444;
}

.pipeline-stepper__step--started .pipeline-stepper__node {
  border-color: var(--dp-primary);
  background: #e0f2fe;
}

.pipeline-stepper__step--skipped .pipeline-stepper__node {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #cbd5e1;
}

.pipeline-stepper__icon {
  font-size: 16px;
}

.pipeline-stepper__pulse {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--dp-primary);
  animation: pipeline-stepper-pulse 1.2s ease-in-out infinite;
}

@keyframes pipeline-stepper-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.85); }
}

.pipeline-stepper__label {
  font-size: 12px;
  font-weight: 500;
  color: var(--dp-text);
  line-height: 1.3;
  max-width: 80px;
}

.pipeline-stepper__step--pending .pipeline-stepper__label,
.pipeline-stepper__step--skipped .pipeline-stepper__label {
  color: var(--dp-text-secondary);
}

.pipeline-stepper__step-duration {
  font-size: 11px;
  color: var(--dp-text-secondary);
}

.pipeline-stepper__details {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pipeline-stepper__card {
  padding: 12px 14px;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  background: var(--dp-surface);
  border-left-width: 3px;
  border-left-color: #cbd5e1;
}

.pipeline-stepper__card--succeeded {
  border-left-color: var(--dp-accent);
}

.pipeline-stepper__card--failed {
  border-left-color: #ef4444;
  background: #fffbfb;
}

.pipeline-stepper__card--started {
  border-left-color: var(--dp-primary);
  background: #f8fcff;
}

.pipeline-stepper__card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.pipeline-stepper__card-title {
  font-weight: 600;
  font-size: 14px;
}

.pipeline-stepper__card-duration {
  margin-left: auto;
  font-size: 12px;
  color: var(--dp-text-secondary);
}

.pipeline-stepper__card-summary,
.pipeline-stepper__card-time {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.5;
}
</style>
