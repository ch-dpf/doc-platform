/** 后端服务地址（与 doc-ingest-service 一致，默认 8081） */
export const backendUrl = (import.meta.env.VITE_BACKEND_URL || 'http://localhost:8081').replace(
  /\/$/,
  ''
)

export const knife4jUrl = `${backendUrl}/doc.html`
