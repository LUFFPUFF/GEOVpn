import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig(({ command }) => {
  return {

    base: '/admin/',
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    server: {
      port: 3000,
      host: true,
      proxy: {
        '/api/v1/admin/device-limits': {
          target: 'http://localhost:8083',
          changeOrigin: true,
        },
        '/api/v1/configs': {
          target: 'http://localhost:8083',
          changeOrigin: true,
        },
        '/api/v1/subscription': {
          target: 'http://localhost:8083',
          changeOrigin: true,
        },
        '/api/v1/admin': {
          target: 'http://localhost:8082',
          changeOrigin: true,
        },
        '/api/v1/users': {
          target: 'http://localhost:8082',
          changeOrigin: true,
        },
        '/api/v1/devices': {
          target: 'http://localhost:8082',
          changeOrigin: true,
        },
        '/api/v1/servers': {
          target: 'http://localhost:8084',
          changeOrigin: true,
        },
        '/api/v1': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        }
      }
    },
    build: {
      outDir: 'dist',
      minify: command === 'build',
      sourcemap: false
    }
  }
})