/**
 * 开发模式默认走 Vite 同源代理（/api → 本机 8080），便于用局域网 IP 访问前端。
 * 生产构建可设置 VITE_API_BASE / VITE_BACKEND_URL 指向实际后端地址。
 */
function trimUrl(url) {
  return url ? url.replace(/\/$/, '') : ''
}

export function resolveBackendUrl() {
  const fromEnv = trimUrl(import.meta.env.VITE_BACKEND_URL)
  if (fromEnv) return fromEnv
  if (import.meta.env.DEV && typeof window !== 'undefined') {
    return window.location.origin
  }
  if (typeof window !== 'undefined') {
    return `${window.location.protocol}//${window.location.hostname}:8080`
  }
  return 'http://localhost:8080'
}

export const backendUrl = resolveBackendUrl()
export const knife4jUrl = import.meta.env.DEV
  ? `${typeof window !== 'undefined' ? window.location.origin : ''}/doc.html`
  : `${trimUrl(import.meta.env.VITE_BACKEND_URL) || backendUrl}/doc.html`
