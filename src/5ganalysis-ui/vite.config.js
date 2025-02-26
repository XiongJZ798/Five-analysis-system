import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue({
      hmr: {
        overlay: false,
        log: 'error'
      }
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: 3000,
    host: true,
    hmr: {
      protocol: 'ws',
      host: 'localhost',
      port: 3000,
      clientPort: 3000,
      overlay: false
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path
      }
    }
  },
  build: {
    rollupOptions: {
      onwarn(warning, warn) {
        if (warning.code === 'INVALID_URL' && warning.message.includes('mcs.zijieapi.com')) {
          return
        }
        warn(warning)
      }
    }
  }
})
