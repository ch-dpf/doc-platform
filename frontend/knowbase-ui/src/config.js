function trimUrl(url) {
  return url ? url.replace(/\/$/, '') : '';
}

const DEFAULT_BACKEND_PORT = 8080;

export function resolveBackendUrl() {
  const fromEnv = trimUrl(import.meta.env.VITE_BACKEND_URL);
  if (fromEnv) {
    return fromEnv;
  }
  if (import.meta.env.DEV && typeof window !== 'undefined') {
    return window.location.origin;
  }
  if (typeof window !== 'undefined') {
    return `${window.location.protocol}//${window.location.hostname}:${DEFAULT_BACKEND_PORT}`;
  }
  return `http://localhost:${DEFAULT_BACKEND_PORT}`;
}

export const backendUrl = resolveBackendUrl();
export const knife4jUrl = import.meta.env.DEV
  ? `${typeof window !== 'undefined' ? window.location.origin : ''}/doc.html`
  : `${trimUrl(import.meta.env.VITE_BACKEND_URL) || backendUrl}/doc.html`;
