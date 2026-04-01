import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/users': { target: 'http://localhost:8082', changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, '/api/v1') },
      '/api/devices': { target: 'http://localhost:8082', changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, '/api/v1') },
      '/api/configs': { target: 'http://localhost:8083', changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, '/api/v1') },
      '/api/servers': { target: 'http://localhost:8083', changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, '/api/v1') },
    }
  }
})