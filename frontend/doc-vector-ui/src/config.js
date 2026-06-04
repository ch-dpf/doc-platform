/** 后端服务地址（与 vector-index-service 一致，默认 8082） */
export const backendUrl = (import.meta.env.VITE_BACKEND_URL || 'http://localhost:8082').replace(
  /\/$/,
  ''
)

export const knife4jUrl = `${backendUrl}/doc.html`
