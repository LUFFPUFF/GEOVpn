import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // ВАЖНО: разрешает Vite слушать все сетевые интерфейсы
    port: 3000,
    allowedHosts: true, // Разрешает туннели Cloudflare/ngrok

    // Настройка для работы WebSocket (HMR) через HTTPS туннель
    hmr: {
      protocol: 'wss', // Используем защищенный протокол для вебсокетов
      clientPort: 443  // Туннель работает на 443 порту (HTTPS)
    },

    proxy: {
      '/api/users': { target: 'http://localhost:8082', changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, '/api/v1') },
      '/api/devices': { target: 'http://localhost:8082', changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, '/api/v1') },
      '/api/configs': { target: 'http://localhost:8083', changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, '/api/v1') },
      '/api/servers': { target: 'http://localhost:8083', changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, '/api/v1') },
    }
  }
})