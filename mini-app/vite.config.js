import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5176,
    host: '0.0.0.0',
    // Самый мощный способ разрешить все хосты в Vite 6
    allowedHosts: 'all',
    // Настройки для туннелей, чтобы Vite не пугался смены протоколов
    cors: true,
    hmr: {
      host: 'localhost',
      protocol: 'ws',
    },
  },
  // Отключаем строгую проверку в режиме разработки для туннелей
  preview: {
    allowedHosts: 'all'
  }
})