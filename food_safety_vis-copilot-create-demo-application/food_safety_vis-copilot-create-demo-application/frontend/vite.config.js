import { defineConfig } from 'vite'
import vue from '@plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',       // 允许外网访问 ✅
    port: 5173,            // 你的端口 ✅
    allowedHosts: [
      'yuaiqaq.gnway.cc',
      '.gnway.cc'          // 允许域名访问 ✅
    ],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 本地后端
        changeOrigin: true,               // 必须开启
        ws: false                         // 关闭 websocket 冲突
      }
    }
  }
})