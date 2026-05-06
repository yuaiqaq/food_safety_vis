import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: [
      'yuaiqaq.gnway.cc',
      '.gnway.cc',
    ],
    proxy: {
      '/api': {
        // 动态判断：如果是通过域名访问，用外网地址；否则用本地
        target: (req) => {
          const host = req.headers.host
          if (host && host.includes('yuaiqaq.gnway.cc')) {
            return 'http://yuaiqaq.gnway.cc:80'
          }
          return 'http://localhost:8080'
        },
        changeOrigin: true,
      }
    }
  }
})