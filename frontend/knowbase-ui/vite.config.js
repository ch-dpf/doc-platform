import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080'

  return {
    plugins: [vue()],
    test: {
      environment: 'node'
    },
    server: {
      host: true,
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true
        },
        '/doc.html': { target: backendTarget, changeOrigin: true },
        '/swagger-ui.html': { target: backendTarget, changeOrigin: true },
        '/v3/api-docs': { target: backendTarget, changeOrigin: true },
        '/webjars': { target: backendTarget, changeOrigin: true }
      }
    },
    preview: {
      host: true,
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': { target: backendTarget, changeOrigin: true },
        '/doc.html': { target: backendTarget, changeOrigin: true },
        '/swagger-ui.html': { target: backendTarget, changeOrigin: true },
        '/v3/api-docs': { target: backendTarget, changeOrigin: true },
        '/webjars': { target: backendTarget, changeOrigin: true }
      }
    }
  }
})
