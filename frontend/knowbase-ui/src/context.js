import { reactive, watch } from 'vue';

const STORAGE_KEY = 'knowbase-request-context';

function loadContext() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      return JSON.parse(raw);
    }
  } catch {
    // ignore
  }
  return {
    tenantId: 'default',
    userId: 'demo',
    roles: 'admin'
  };
}

export const requestContext = reactive(loadContext());

watch(
  requestContext,
  value => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      tenantId: value.tenantId,
      userId: value.userId,
      roles: value.roles
    }));
  },
  { deep: true }
);

export function applyRequestHeaders(config) {
  config.headers = config.headers || {};
  if (requestContext.tenantId) {
    config.headers['X-Knowbase-Tenant-Id'] = requestContext.tenantId;
  }
  if (requestContext.userId) {
    config.headers['X-Knowbase-User-Id'] = requestContext.userId;
  }
  if (requestContext.roles) {
    config.headers['X-Knowbase-Roles'] = requestContext.roles;
  }
  return config;
}
