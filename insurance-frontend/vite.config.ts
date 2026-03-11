import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// When running inside Docker (test profile), BACKEND_URL is set to
// http://app:8080 so the proxy reaches the Spring Boot service by its
// Docker network name instead of localhost.
const backendUrl = process.env.BACKEND_URL ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    proxy: {
      '/api/v1': {
        target: backendUrl,
        changeOrigin: true,
      }
    }
  }
})
