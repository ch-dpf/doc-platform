import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget =
    env.KNOWBASE_API_BASE_URL ||
    env.VITE_DEV_PROXY_TARGET ||
    'http://127.0.0.1:8088';

  console.log(`[knowbase-ui] API proxy target: ${apiTarget}`);

  return {
    plugins: [vue()],
    resolve: {
      alias: [
        {
          find: /^element-plus$/,
          replacement: fileURLToPath(new URL('./node_modules/element-plus/lib/index.js', import.meta.url))
        }
      ]
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true
        },
        '/v3/api-docs': {
          target: apiTarget,
          changeOrigin: true
        },
        '/doc.html': {
          target: apiTarget,
          changeOrigin: true
        },
        '/swagger-ui.html': {
          target: apiTarget,
          changeOrigin: true
        },
        '/webjars': {
          target: apiTarget,
          changeOrigin: true
        }
      }
    }
  };
});
