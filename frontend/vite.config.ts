import path from 'node:path'
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    // Single-origin in dev: proxy /api/* to the Quarkus backend (ADR-0007, no CORS).
    // The trailing slash is load-bearing: a bare '/api' prefix would also swallow
    // SPA routes like /apis/:id on hard loads (FEAT-002).
    proxy: {
      '/api/': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
})
