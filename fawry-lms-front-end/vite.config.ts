import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// Dev proxy: browser hits same-origin /api (no CORS); Vite forwards to the Spring Boot host.
// VITE_API_BASE_URL stays empty in .env.development so apiFetch builds relative /api/... paths.
const API_TARGET = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: API_TARGET,
        changeOrigin: true,
      },
    },
  },
})
